<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCardsStore } from '@/stores/cards';
import { useTransfersStore } from '@/stores/transfers';
import { useAuthStore } from '@/stores/auth';
import { searchUserByPhone, searchUserByCard } from '@/lib/api/users';
import { fetchDevOtp } from '@/lib/api/transfers';
import { ApiError } from '@/lib/api/client';
import type { RecipientDto } from '@/lib/api/users';

const cardsStore = useCardsStore();
const transfersStore = useTransfersStore();
const authStore = useAuthStore();
const router = useRouter();
const isDev = import.meta.env.DEV;

// ── View state ────────────────────────────────────────────────
type View = 'main' | 'between';
const view = ref<View>('main');

// ── Cards ─────────────────────────────────────────────────────
const senderCardId = ref('');
const verifiedCards = computed(() => cardsStore.cards.filter((c) => c.status === 'VERIFIED'));
const ownedCards = computed(() =>
  cardsStore.cards.filter((c) => c.status !== 'BLOCKED' && c.status !== 'EXPIRED')
);

onMounted(async () => {
  if (cardsStore.cards.length === 0) await cardsStore.fetchCards();
  if (transfersStore.transfers.length === 0) await transfersStore.fetchTransfers();
  if (!senderCardId.value && verifiedCards.value.length > 0) {
    senderCardId.value = verifiedCards.value[0].id;
  }
  if (ownedCards.value.length > 1) {
    fromIdx.value = 0;
    toIdx.value = 1;
  }
});

// ── Recipient search ──────────────────────────────────────────
const searchQuery = ref('');
const recipient = ref<RecipientDto | null>(null);
const lookupError = ref('');
const isLooking = ref(false);

let searchTimer: ReturnType<typeof setTimeout> | null = null;

watch(searchQuery, (val) => {
  if (searchTimer) clearTimeout(searchTimer);
  if (!val.trim()) {
    recipient.value = null;
    lookupError.value = '';
    return;
  }
  const digits = val.replace(/\D/g, '');
  let phoneDigits = digits;
  if (phoneDigits.startsWith('998') && phoneDigits.length === 12)
    phoneDigits = phoneDigits.slice(3);
  const isCard = digits.length === 16;
  const isPhone = phoneDigits.length === 9;
  if (!isCard && !isPhone) {
    recipient.value = null;
    lookupError.value = '';
    return;
  }
  searchTimer = setTimeout(() => handleSearch(), 300);
});

async function handleSearch() {
  const q = searchQuery.value.trim();
  if (!q) return;
  lookupError.value = '';
  recipient.value = null;
  recipientCardId.value = '';
  isLooking.value = true;
  try {
    const digits = q.replace(/\D/g, '');
    if (digits.length === 16) {
      recipient.value = await searchUserByCard(digits);
      const first6 = digits.slice(0, 6);
      const last4 = digits.slice(-4);
      const matched = recipient.value.cards.find(
        (c) => c.maskedPan.startsWith(first6) && c.maskedPan.endsWith(last4)
      );
      recipientCardId.value = matched?.id ?? recipient.value.cards[0]?.id ?? '';
    } else {
      let phoneDigits = digits;
      if (phoneDigits.startsWith('998') && phoneDigits.length === 12)
        phoneDigits = phoneDigits.slice(3);
      recipient.value = await searchUserByPhone('+998' + phoneDigits);
      const def = recipient.value.cards.find((c) => c.isDefault) ?? recipient.value.cards[0];
      recipientCardId.value = def?.id ?? '';
    }
    if (!senderCardId.value && verifiedCards.value.length > 0) {
      senderCardId.value = verifiedCards.value[0].id;
    }
  } catch (err) {
    lookupError.value =
      err instanceof ApiError && err.status === 404
        ? 'Foydalanuvchi topilmadi.'
        : "Qidiruv muvaffaqiyatsiz. Qayta urinib ko'ring.";
  } finally {
    isLooking.value = false;
  }
}

// ── Transfer details ──────────────────────────────────────────
const recipientCardId = ref('');
const amountRaw = ref('');
const amountUzs = computed(() => {
  const n = parseInt(amountRaw.value.replace(/\D/g, ''), 10);
  return isNaN(n) ? 0 : n;
});
const sendError = ref('');
const fieldErrors = ref<Record<string, string>>({});

// ── OTP step ──────────────────────────────────────────────────
const isOtpStep = ref(false);
const pendingTransferId = ref('');
const otpCode = ref('');
const otpError = ref('');

function validate(): boolean {
  fieldErrors.value = {};
  if (!senderCardId.value) fieldErrors.value.senderCard = 'Kartangizni tanlang.';
  if (!recipientCardId.value) fieldErrors.value.recipientCard = 'Qabul qiluvchi kartasini tanlang.';
  if (!amountUzs.value || amountUzs.value <= 0)
    fieldErrors.value.amount = "To'g'ri summani kiriting.";
  else if (amountUzs.value > 30_000_000) fieldErrors.value.amount = 'Maksimum 30 000 000 UZS.';
  return Object.keys(fieldErrors.value).length === 0;
}

async function handleSend() {
  sendError.value = '';
  if (!validate() || !recipient.value) return;
  const senderCard = verifiedCards.value.find((c) => c.id === senderCardId.value)!;
  const recipientCard = recipient.value.cards.find((c) => c.id === recipientCardId.value)!;
  try {
    const initiated = await transfersStore.sendMoney({
      senderInstrumentId: senderCard.id,
      senderCardNetwork: senderCard.cardNetwork ?? 'uzcard',
      recipientId: recipient.value.id,
      recipientInstrumentId: recipientCard.id,
      recipientCardNetwork: recipientCard.cardNetwork ?? 'uzcard',
      amountUzs: amountUzs.value,
      transferTypeId: 1,
      channel: 'mobile_app',
      idempotencyKey: crypto.randomUUID(),
    });
    pendingTransferId.value = initiated.id;
    isOtpStep.value = true;
    if (isDev && authStore.user) {
      try {
        const res = await fetchDevOtp(authStore.user.phoneE164);
        otpCode.value = res.code;
      } catch {
        /* ignore */
      }
    }
  } catch (err) {
    sendError.value = err instanceof ApiError ? err.message : "O'tkazma muvaffaqiyatsiz.";
  }
}

async function handleConfirmOtp() {
  otpError.value = '';
  try {
    await transfersStore.confirmOtp(pendingTransferId.value, otpCode.value);
    router.push('/');
  } catch (err) {
    otpError.value = err instanceof ApiError ? err.message : 'OTP tasdiqlanmadi.';
  }
}

// ── Between-cards state ───────────────────────────────────────
const fromIdx = ref(0);
const toIdx = ref(1);
const same = computed(() => fromIdx.value === toIdx.value);
const betweenAmountValue = computed(() => parseInt(betweenAmount.value.replace(/,/g, ''), 10) || 0);

const betweenAccounts = computed(() => {
  return ownedCards.value.map((c) => ({
    label: `${networkLabel(c.cardNetwork)} ···${c.maskedPan?.slice(-4) ?? ''}`,
    balance: c.balanceUzs != null ? Number(c.balanceUzs).toLocaleString('uz-UZ') + ' UZS' : '— UZS',
  }));
});

const betweenAmount = ref('');
const betweenNote = ref('');
const betweenNoteOk = ref(false);

function swapBetween() {
  const tmp = fromIdx.value;
  fromIdx.value = toIdx.value;
  toIdx.value = tmp;
  betweenNote.value = '';
}

function onBetweenAmount(e: Event) {
  const d = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 12);
  betweenAmount.value = d ? Number(d).toLocaleString('en-US') : '';
  betweenNote.value = '';
}

function confirmBetween() {
  const v = parseInt(betweenAmount.value.replace(/,/g, ''), 10);
  if (!v) {
    betweenNote.value = 'Summani kiriting.';
    betweenNoteOk.value = false;
    return;
  }
  betweenNote.value = 'Bu funksiya tez orada ishga tushadi.';
  betweenNoteOk.value = false;
}

// ── Starred contacts (persisted in localStorage) ──────────────
const starredNames = ref<Set<string>>(
  new Set(JSON.parse(localStorage.getItem('pp-starred-contacts') || '[]'))
);

function toggleStar(name: string) {
  const set = new Set(starredNames.value);
  if (set.has(name)) set.delete(name);
  else set.add(name);
  starredNames.value = set;
  localStorage.setItem('pp-starred-contacts', JSON.stringify([...set]));
}

// ── Recent contacts from transfer history ─────────────────────
const recentContacts = computed(() => {
  const seen = new Set<string>();
  const out: { name: string; detail: string; date: string; initials: string }[] = [];
  for (const t of transfersStore.transfers) {
    if (t.direction === 'debit' && t.recipientName && !seen.has(t.recipientName)) {
      seen.add(t.recipientName);
      out.push({
        name: t.recipientName,
        detail: t.recipientMaskedPan || 'PulsePay',
        date: shortDate(t.processedAt ?? t.initiatedAt),
        initials: initials(t.recipientName),
      });
      if (out.length >= 5) break;
    }
  }
  return out;
});

const contactGroups = computed(() => {
  const starred = recentContacts.value.filter((c) => starredNames.value.has(c.name));
  const recent = recentContacts.value.filter((c) => !starredNames.value.has(c.name));
  const groups = [];
  if (starred.length) groups.push({ title: "Saralangan o'tkazmalar", contacts: starred });
  if (recent.length) groups.push({ title: "Oxirgi o'tkazmalar", contacts: recent });
  return groups;
});

function initials(name: string) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
}

function shortDate(iso: string | null | undefined) {
  if (!iso) return '';
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString('uz-UZ', { day: 'numeric', month: 'short' });
}

function networkLabel(network: string | null) {
  if (network === 'uzcard') return 'UzCard';
  if (network === 'humo') return 'HUMO';
  return 'Karta';
}

function fillAllMoney() {
  const card = ownedCards.value[fromIdx.value];
  if (card?.balanceUzs) {
    const d = Math.floor(Number(card.balanceUzs)).toString();
    betweenAmount.value = d ? Number(d).toLocaleString('en-US') : '';
    betweenNote.value = '';
  }
}

function onAmountInput(e: Event) {
  const d = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 12);
  amountRaw.value = d ? Number(d).toLocaleString('en-US') : '';
}
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- ── OTP Overlay ── -->
      <div v-if="isOtpStep" class="pp-modal-overlay" @click.self="() => {}">
        <div class="pp-modal">
          <div class="pp-modal-header">
            <div class="pp-modal-icon success">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"
                ></path>
              </svg>
            </div>
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 22px;
                font-weight: 600;
                letter-spacing: -0.02em;
                margin-bottom: 6px;
              "
            >
              OTP tasdiqlash
            </div>
            <div style="font-size: 14px; color: rgba(247, 244, 237, 0.55)">
              Telefoningizga yuborilgan 6 raqamli kodni kiriting
            </div>
          </div>
          <div class="pp-modal-body">
            <div
              v-if="otpError"
              style="
                padding: 12px 16px;
                background: rgba(255, 156, 130, 0.12);
                border: 1px solid rgba(255, 156, 130, 0.3);
                border-radius: 10px;
                font-size: 13.5px;
                color: #ff9c82;
              "
            >
              {{ otpError }}
            </div>
            <div>
              <label
                style="
                  display: block;
                  font-size: 12.5px;
                  font-weight: 600;
                  color: rgba(247, 244, 237, 0.62);
                  margin-bottom: 8px;
                "
                >OTP kod</label
              >
              <input
                v-model="otpCode"
                type="tel"
                inputmode="numeric"
                maxlength="6"
                placeholder="000000"
                autofocus
                style="
                  width: 100%;
                  height: 54px;
                  padding: 0 16px;
                  background: rgba(14, 33, 28, 0.55);
                  border: 1px solid rgba(247, 244, 237, 0.14);
                  border-radius: 12px;
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 22px;
                  font-weight: 600;
                  letter-spacing: 0.16em;
                  color: #f7f4ed;
                  outline: none;
                  transition: border-color 0.15s;
                  text-align: center;
                "
                @keyup.enter="handleConfirmOtp"
              />
              <p
                v-if="isDev && otpCode"
                style="margin: 8px 0 0; font-size: 12px; color: rgba(247, 244, 237, 0.4)"
              >
                Dev: kod avtomatik to'ldirildi — {{ otpCode }}
              </p>
            </div>
          </div>
          <div
            class="pp-modal-footer"
            style="display: flex; flex-direction: column; gap: 10px; padding: 0 28px 24px"
          >
            <button
              class="pp-btn-primary"
              style="width: 100%; justify-content: center; padding: 14px"
              :disabled="transfersStore.isLoading"
              @click="handleConfirmOtp"
            >
              {{ transfersStore.isLoading ? 'Tasdiqlanmoqda...' : 'Tasdiqlash' }}
            </button>
            <button
              class="pp-modal-close"
              @click="
                isOtpStep = false;
                otpError = '';
              "
            >
              Bekor qilish
            </button>
          </div>
        </div>
      </div>

      <!-- ── Between-cards view ── -->
      <section v-if="view === 'between' && !isOtpStep" style="max-width: 640px; margin: 0 auto">
        <div style="display: flex; align-items: center; gap: 14px">
          <button
            type="button"
            title="Orqaga"
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 38px;
              height: 38px;
              border: 1px solid rgba(247, 244, 237, 0.16);
              border-radius: 12px;
              background: transparent;
              color: rgba(247, 244, 237, 0.7);
              cursor: pointer;
              transition:
                background 0.15s,
                color 0.15s;
            "
            @click="
              view = 'main';
              betweenNote = '';
            "
          >
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M19 12H5m0 0 6-6m-6 6 6 6"></path>
            </svg>
          </button>
          <h1
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: clamp(22px, 2.6vw, 30px);
              font-weight: 600;
              letter-spacing: -0.025em;
              margin: 0;
              color: #f7f4ed;
            "
          >
            Kartalarim orasida o'tkazma
          </h1>
        </div>

        <!-- No cards warning -->
        <div
          v-if="betweenAccounts.length < 2"
          style="
            margin-top: 28px;
            padding: 20px 22px;
            background: rgba(255, 156, 130, 0.08);
            border: 1px solid rgba(255, 156, 130, 0.22);
            border-radius: 16px;
            font-size: 14px;
            color: #ff9c82;
          "
        >
          Kamida 2 ta tasdiqlangan karta bo'lishi kerak.
          <RouterLink to="/cards" style="color: #29be8c">Karta qo'shish →</RouterLink>
        </div>

        <template v-else>
          <div style="position: relative; margin-top: 28px">
            <div
              style="
                font-size: 12px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
              "
            >
              Kartadan
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px">
              <button
                v-for="(acc, i) in betweenAccounts"
                :key="'from-' + i"
                type="button"
                style="
                  flex: 1 1 190px;
                  min-width: 0;
                  text-align: left;
                  border-radius: 16px;
                  padding: 14px 16px;
                  font-family: Manrope, sans-serif;
                  color: #f7f4ed;
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    border-color 0.15s;
                "
                :style="{
                  background: fromIdx === i ? 'rgba(41,190,140,0.12)' : 'rgba(247,244,237,0.045)',
                  border:
                    fromIdx === i
                      ? '1px solid rgba(41,190,140,0.4)'
                      : '1px solid rgba(247,244,237,0.12)',
                }"
                @click="
                  fromIdx = i;
                  betweenNote = '';
                "
              >
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 17px;
                    font-weight: 600;
                    letter-spacing: -0.015em;
                  "
                >
                  {{ acc.balance }}
                </div>
                <div style="font-size: 13px; color: rgba(247, 244, 237, 0.55); margin-top: 4px">
                  {{ acc.label }}
                </div>
              </button>
            </div>

            <div style="display: flex; justify-content: center; margin: 16px 0">
              <button
                type="button"
                title="O'rnini almashtirish"
                style="
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  width: 44px;
                  height: 44px;
                  border: none;
                  border-radius: 50%;
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    transform 0.12s;
                "
                :style="{
                  background: same ? 'rgba(247,244,237,0.1)' : '#29BE8C',
                  color: same ? 'rgba(247,244,237,0.45)' : '#0E211C',
                }"
                @click="swapBetween"
              >
                <svg
                  width="19"
                  height="19"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path
                    d="M8 21V5m0 16-3.5-3.5M8 5l3.5 3.5M16 3v16m0 0 3.5-3.5M16 19l-3.5-3.5"
                  ></path>
                </svg>
              </button>
            </div>

            <div
              style="
                font-size: 12px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
              "
            >
              Kartaga
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px">
              <button
                v-for="(acc, i) in betweenAccounts"
                :key="'to-' + i"
                type="button"
                style="
                  flex: 1 1 190px;
                  min-width: 0;
                  text-align: left;
                  border-radius: 16px;
                  padding: 14px 16px;
                  font-family: Manrope, sans-serif;
                  color: #f7f4ed;
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    border-color 0.15s;
                "
                :style="{
                  background: toIdx === i ? 'rgba(41,190,140,0.12)' : 'rgba(247,244,237,0.045)',
                  border:
                    toIdx === i
                      ? '1px solid rgba(41,190,140,0.4)'
                      : '1px solid rgba(247,244,237,0.12)',
                }"
                @click="
                  toIdx = i;
                  betweenNote = '';
                "
              >
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 17px;
                    font-weight: 600;
                    letter-spacing: -0.015em;
                  "
                >
                  {{ acc.balance }}
                </div>
                <div style="font-size: 13px; color: rgba(247, 244, 237, 0.55); margin-top: 4px">
                  {{ acc.label }}
                </div>
              </button>
            </div>
          </div>

          <!-- Amount + confirm -->
          <div
            style="
              margin-top: 30px;
              padding-top: 24px;
              border-top: 1px solid rgba(247, 244, 237, 0.1);
            "
          >
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >Summani kiriting</label
            >
            <div style="display: flex; align-items: center; gap: 10px; margin-top: 10px">
              <div
                style="
                  display: flex;
                  align-items: center;
                  gap: 10px;
                  flex: 1 1 auto;
                  min-width: 0;
                  height: 62px;
                  padding: 0 8px 0 18px;
                  background: rgba(247, 244, 237, 0.045);
                  border-radius: 16px;
                "
                :style="{ border: '1px solid ' + (same ? '#E4572E' : 'rgba(247,244,237,0.14)') }"
              >
                <input
                  type="tel"
                  inputmode="numeric"
                  placeholder="0"
                  :value="betweenAmount"
                  style="
                    flex: 1;
                    min-width: 0;
                    padding: 0;
                    border: none;
                    background: transparent;
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 22px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                    color: #f7f4ed;
                    outline: none;
                  "
                  @input="onBetweenAmount"
                />
                <span
                  style="
                    flex: none;
                    font-size: 14px;
                    font-weight: 600;
                    color: rgba(247, 244, 237, 0.5);
                  "
                  >UZS</span
                >
                <button
                  type="button"
                  style="
                    flex: none;
                    border: none;
                    border-radius: 12px;
                    background: rgba(41, 190, 140, 0.16);
                    color: #29be8c;
                    padding: 10px 14px;
                    font-family: Manrope, sans-serif;
                    font-size: 12.5px;
                    font-weight: 700;
                    cursor: pointer;
                    transition: background 0.15s;
                    white-space: nowrap;
                  "
                  @click="fillAllMoney"
                >
                  Hamma pulni
                </button>
              </div>
              <button
                type="button"
                title="Davom etish"
                :disabled="same"
                style="
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  flex: none;
                  width: 62px;
                  height: 62px;
                  border: none;
                  border-radius: 16px;
                  transition: background 0.15s;
                "
                :style="{
                  background: same ? 'rgba(247,244,237,0.08)' : '#29BE8C',
                  color: same ? 'rgba(247,244,237,0.35)' : '#0E211C',
                  cursor: same ? 'not-allowed' : 'pointer',
                }"
                @click="confirmBetween"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M5 12h14m0 0-6-6m6 6-6 6"></path>
                </svg>
              </button>
            </div>
            <div
              v-if="!same && betweenAmountValue > 0"
              style="
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 16px;
                flex-wrap: wrap;
                margin-top: 12px;
                font-size: 13px;
              "
            >
              <div style="color: rgba(247, 244, 237, 0.55)">
                Komissiya:
                <strong style="color: #f7f4ed; font-weight: 600"
                  >{{ Math.round(betweenAmountValue * 0.011).toLocaleString('en-US') }} UZS</strong
                >
              </div>
              <div style="color: rgba(247, 244, 237, 0.55)">
                Cashback: <strong style="color: #29be8c; font-weight: 600">0 UZS</strong>
              </div>
            </div>
            <div
              v-if="same || !betweenAmountValue"
              style="margin-top: 12px; font-size: 13px"
              :style="{
                color: same
                  ? '#E4572E'
                  : betweenNote
                    ? betweenNoteOk
                      ? '#29BE8C'
                      : '#FF9C82'
                    : 'rgba(247,244,237,0.45)',
              }"
            >
              {{
                same
                  ? "Xuddi shu kartaga o'tkazish mumkin emas"
                  : betweenNote || "Komissiya qo'llanilishi mumkin"
              }}
            </div>
          </div>
        </template>
      </section>

      <!-- ── Main send view ── -->
      <section
        v-else-if="!isOtpStep"
        style="
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          padding: clamp(20px, 6vh, 48px) 0;
        "
      >
        <h1
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: clamp(28px, 3.4vw, 40px);
            font-weight: 600;
            letter-spacing: -0.03em;
            margin: 0;
            color: #f7f4ed;
          "
        >
          O'tkazma
        </h1>
        <p
          style="
            font-size: 15px;
            line-height: 1.5;
            color: rgba(247, 244, 237, 0.6);
            margin: 10px 0 0;
          "
        >
          Qabul qiluvchining telefon yoki karta raqamini kiriting
        </p>

        <!-- Search bar -->
        <div
          style="
            display: flex;
            align-items: center;
            gap: 12px;
            width: 100%;
            max-width: 640px;
            margin-top: 26px;
            text-align: left;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.14);
            border-radius: 16px;
            padding: 0 16px;
            height: 62px;
          "
        >
          <svg
            v-if="!isLooking"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.5)"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
            style="flex: none"
          >
            <circle cx="11" cy="11" r="7"></circle>
            <path d="m20 20-3.6-3.6"></path>
          </svg>
          <div
            v-else
            style="
              flex: none;
              width: 18px;
              height: 18px;
              border: 2px solid rgba(41, 190, 140, 0.3);
              border-top-color: #29be8c;
              border-radius: 50%;
              animation: pp-spin 0.7s linear infinite;
            "
          ></div>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Telefon (+998XX) yoki karta raqami"
            style="
              flex: 1;
              min-width: 0;
              padding: 0;
              border: none;
              background: transparent;
              font-family: Manrope, sans-serif;
              font-size: 16px;
              font-weight: 500;
              color: #f7f4ed;
              outline: none;
            "
          />
          <button
            v-if="searchQuery"
            type="button"
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 24px;
              height: 24px;
              border: none;
              border-radius: 6px;
              background: transparent;
              color: rgba(247, 244, 237, 0.4);
              cursor: pointer;
            "
            @click="
              searchQuery = '';
              recipient = null;
              lookupError = '';
            "
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <!-- Lookup error -->
        <div
          v-if="lookupError"
          style="
            width: 100%;
            max-width: 640px;
            margin-top: 12px;
            text-align: left;
            font-size: 13px;
            color: #ff9c82;
          "
        >
          {{ lookupError }}
        </div>

        <!-- Recipient found → inline send form -->
        <div
          v-if="recipient"
          style="width: 100%; max-width: 640px; margin-top: 20px; text-align: left"
        >
          <!-- Recipient banner -->
          <div
            style="
              display: flex;
              align-items: center;
              gap: 14px;
              padding: 16px 18px;
              background: rgba(41, 190, 140, 0.08);
              border: 1px solid rgba(41, 190, 140, 0.28);
              border-radius: 16px;
            "
          >
            <div
              style="
                display: flex;
                align-items: center;
                justify-content: center;
                flex: none;
                width: 42px;
                height: 42px;
                border-radius: 50%;
                background: rgba(41, 190, 140, 0.18);
                font-family: 'Space Grotesk', sans-serif;
                font-size: 14px;
                font-weight: 600;
                color: #29be8c;
              "
            >
              {{ initials(recipient.fullName) }}
            </div>
            <div style="flex: 1; min-width: 0">
              <div style="font-size: 15px; font-weight: 600; color: #f7f4ed">
                {{ recipient.fullName }}
              </div>
              <div style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin-top: 2px">
                {{ recipient.phoneE164 }}
              </div>
            </div>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#29BE8C"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 6 9 17l-5-5"></path>
            </svg>
          </div>

          <!-- From card selector -->
          <div style="margin-top: 20px">
            <div
              style="
                font-size: 12px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
                margin-bottom: 10px;
              "
            >
              Sizning kartangiz
            </div>
            <div v-if="verifiedCards.length === 0" style="font-size: 13.5px; color: #ff9c82">
              Tasdiqlangan kartalar yo'q —
              <RouterLink to="/cards" style="color: #29be8c">karta qo'shing</RouterLink>.
            </div>
            <div v-else style="display: flex; flex-wrap: wrap; gap: 8px">
              <button
                v-for="card in verifiedCards"
                :key="card.id"
                type="button"
                style="
                  flex: 1 1 180px;
                  text-align: left;
                  border-radius: 14px;
                  padding: 12px 14px;
                  font-family: Manrope, sans-serif;
                  color: #f7f4ed;
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    border-color 0.15s;
                "
                :style="{
                  background:
                    senderCardId === card.id ? 'rgba(41,190,140,0.12)' : 'rgba(247,244,237,0.045)',
                  border:
                    senderCardId === card.id
                      ? '1px solid rgba(41,190,140,0.4)'
                      : '1px solid rgba(247,244,237,0.12)',
                }"
                @click="senderCardId = card.id"
              >
                <div
                  style="
                    font-size: 11.5px;
                    font-weight: 700;
                    letter-spacing: 0.06em;
                    margin-bottom: 4px;
                    color: rgba(247, 244, 237, 0.6);
                  "
                >
                  {{ networkLabel(card.cardNetwork) }}
                </div>
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 15px;
                    font-weight: 600;
                    letter-spacing: 0.04em;
                  "
                >
                  {{ card.maskedPan }}
                </div>
              </button>
            </div>
            <div
              v-if="fieldErrors.senderCard"
              style="margin-top: 6px; font-size: 12.5px; color: #ff9c82"
            >
              {{ fieldErrors.senderCard }}
            </div>
          </div>

          <!-- Recipient card selector -->
          <div v-if="recipient.cards.length > 1" style="margin-top: 16px">
            <div
              style="
                font-size: 12px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
                margin-bottom: 10px;
              "
            >
              Qabul qiluvchi kartasi
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px">
              <button
                v-for="card in recipient.cards"
                :key="card.id"
                type="button"
                style="
                  flex: 1 1 180px;
                  text-align: left;
                  border-radius: 14px;
                  padding: 12px 14px;
                  font-family: Manrope, sans-serif;
                  color: #f7f4ed;
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    border-color 0.15s;
                "
                :style="{
                  background:
                    recipientCardId === card.id
                      ? 'rgba(41,190,140,0.12)'
                      : 'rgba(247,244,237,0.045)',
                  border:
                    recipientCardId === card.id
                      ? '1px solid rgba(41,190,140,0.4)'
                      : '1px solid rgba(247,244,237,0.12)',
                }"
                @click="recipientCardId = card.id"
              >
                <div
                  style="
                    font-size: 11.5px;
                    font-weight: 700;
                    letter-spacing: 0.06em;
                    margin-bottom: 4px;
                    color: rgba(247, 244, 237, 0.6);
                  "
                >
                  {{ networkLabel(card.cardNetwork) }}{{ card.isDefault ? ' · Asosiy' : '' }}
                </div>
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 15px;
                    font-weight: 600;
                    letter-spacing: 0.04em;
                  "
                >
                  {{ card.maskedPan }}
                </div>
              </button>
            </div>
            <div
              v-if="fieldErrors.recipientCard"
              style="margin-top: 6px; font-size: 12.5px; color: #ff9c82"
            >
              {{ fieldErrors.recipientCard }}
            </div>
          </div>

          <!-- Amount -->
          <div
            style="
              margin-top: 20px;
              padding-top: 20px;
              border-top: 1px solid rgba(247, 244, 237, 0.1);
            "
          >
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
                margin-bottom: 10px;
              "
              >Summani kiriting</label
            >
            <div style="display: flex; align-items: center; gap: 10px">
              <div
                style="
                  display: flex;
                  align-items: center;
                  gap: 10px;
                  flex: 1 1 auto;
                  min-width: 0;
                  height: 62px;
                  padding: 0 8px 0 18px;
                  background: rgba(247, 244, 237, 0.045);
                  border: 1px solid rgba(247, 244, 237, 0.14);
                  border-radius: 16px;
                "
                :style="fieldErrors.amount ? { borderColor: '#FF9C82' } : {}"
              >
                <input
                  type="tel"
                  inputmode="numeric"
                  placeholder="0"
                  :value="amountRaw"
                  style="
                    flex: 1;
                    min-width: 0;
                    padding: 0;
                    border: none;
                    background: transparent;
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 22px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                    color: #f7f4ed;
                    outline: none;
                  "
                  @input="onAmountInput"
                />
                <span
                  style="
                    flex: none;
                    font-size: 14px;
                    font-weight: 600;
                    color: rgba(247, 244, 237, 0.5);
                  "
                  >UZS</span
                >
              </div>
              <button
                type="button"
                :disabled="transfersStore.isLoading || verifiedCards.length === 0"
                style="
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  flex: none;
                  width: 62px;
                  height: 62px;
                  border: none;
                  border-radius: 16px;
                  background: #29be8c;
                  color: #0e211c;
                  cursor: pointer;
                  transition: background 0.15s;
                  font-family: Manrope, sans-serif;
                  font-size: 13px;
                  font-weight: 700;
                "
                @click="handleSend"
              >
                <svg
                  v-if="!transfersStore.isLoading"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M5 12h14m0 0-6-6m6 6-6 6"></path>
                </svg>
                <div
                  v-else
                  class="pp-spinner"
                  style="width: 20px; height: 20px; border-width: 2px"
                ></div>
              </button>
            </div>
            <div v-if="fieldErrors.amount" style="margin-top: 8px; font-size: 13px; color: #ff9c82">
              {{ fieldErrors.amount }}
            </div>
            <div v-if="sendError" style="margin-top: 8px; font-size: 13px; color: #ff9c82">
              {{ sendError }}
            </div>
            <div
              v-if="!fieldErrors.amount && !sendError"
              style="margin-top: 8px; font-size: 13px; color: rgba(247, 244, 237, 0.4)"
            >
              Komissiya qo'llanilishi mumkin
            </div>
          </div>
        </div>

        <!-- Between-cards shortcut -->
        <button
          type="button"
          style="
            display: flex;
            align-items: center;
            gap: 14px;
            width: 100%;
            max-width: 640px;
            margin-top: 14px;
            text-align: left;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.12);
            border-radius: 16px;
            padding: 16px 18px;
            font-family: Manrope, sans-serif;
            color: #f7f4ed;
            cursor: pointer;
            transition:
              background 0.15s,
              border-color 0.15s;
          "
          @click="view = 'between'"
        >
          <span
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 38px;
              height: 38px;
              border-radius: 12px;
              background: rgba(41, 190, 140, 0.16);
              color: #29be8c;
            "
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 8h13m0 0-3.5-3.5M17 8l-3.5 3.5M20 16H7m0 0 3.5-3.5M7 16l3.5 3.5"></path>
            </svg>
          </span>
          <span style="flex: 1; min-width: 0; font-size: 15.5px; font-weight: 600"
            >Kartalarim orasida</span
          >
          <svg
            width="17"
            height="17"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.45)"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
            style="flex: none"
          >
            <path d="m9 6 6 6-6 6"></path>
          </svg>
        </button>

        <!-- Contact groups (starred + recent) from transfer history -->
        <template v-if="contactGroups.length > 0">
          <div
            v-for="group in contactGroups"
            :key="group.title"
            style="width: 100%; max-width: 640px; margin-top: 34px; text-align: left"
          >
            <h2
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 18px;
                font-weight: 600;
                letter-spacing: -0.015em;
                margin: 0 0 14px;
                color: #f7f4ed;
              "
            >
              {{ group.title }}
            </h2>
            <div style="display: flex; flex-direction: column; gap: 8px">
              <div
                v-for="contact in group.contacts"
                :key="contact.name"
                style="
                  display: flex;
                  align-items: center;
                  gap: 14px;
                  background: rgba(247, 244, 237, 0.045);
                  border: 1px solid rgba(247, 244, 237, 0.1);
                  border-radius: 16px;
                  padding: 14px 16px;
                "
              >
                <div
                  style="
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex: none;
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    background: rgba(41, 190, 140, 0.16);
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 13.5px;
                    font-weight: 600;
                    color: #29be8c;
                  "
                >
                  {{ contact.initials }}
                </div>
                <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px">
                  <div
                    style="
                      font-size: 15px;
                      font-weight: 600;
                      white-space: nowrap;
                      overflow: hidden;
                      text-overflow: ellipsis;
                    "
                  >
                    {{ contact.name }}
                  </div>
                  <div style="font-size: 13px; color: rgba(247, 244, 237, 0.5)">
                    {{ contact.detail }}
                  </div>
                </div>
                <div style="flex: none; font-size: 12.5px; color: rgba(247, 244, 237, 0.42)">
                  {{ contact.date }}
                </div>
                <button
                  type="button"
                  title="Saralanganga qo'shish"
                  style="
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex: none;
                    width: 32px;
                    height: 32px;
                    border: none;
                    border-radius: 9px;
                    background: transparent;
                    cursor: pointer;
                    transition: background 0.15s;
                  "
                  :style="{
                    color: starredNames.has(contact.name) ? '#F2B23E' : 'rgba(247,244,237,0.42)',
                  }"
                  @click="toggleStar(contact.name)"
                >
                  <svg
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    :fill="starredNames.has(contact.name) ? '#F2B23E' : 'none'"
                    stroke="currentColor"
                    stroke-width="1.9"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path
                      d="m12 3 2.9 5.9 6.6.9-4.8 4.6 1.2 6.5L12 17.8 6.1 20.9l1.2-6.5L2.5 9.8l6.6-.9z"
                    ></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- Empty state when no history -->
        <div
          v-else-if="!transfersStore.isLoading"
          style="width: 100%; max-width: 640px; margin-top: 34px; text-align: left"
        >
          <p style="font-size: 13.5px; color: rgba(247, 244, 237, 0.4); text-align: center">
            O'tkazma tarixi bu yerda ko'rinadi
          </p>
        </div>
      </section>
    </div>
  </q-page>
</template>
