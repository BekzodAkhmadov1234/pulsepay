<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCardsStore } from '@/stores/cards';
import { useA2PTransfersStore } from '@/stores/a2pTransfers';
import { useAuthStore } from '@/stores/auth';
import { listBanks } from '@/lib/api/banks';
import { fetchDevOtp, previewFee } from '@/lib/api/transfers';
import { ApiError } from '@/lib/api/client';
import type { BankDto } from '@/lib/api/banks';
import type { TransferDto } from '@/lib/api/transfers';

const router = useRouter();
const cardsStore = useCardsStore();
const a2pStore = useA2PTransfersStore();
const authStore = useAuthStore();
const isDev = import.meta.env.DEV;

// ── Card selection (destination — user's own card) ────────────────────────
const destCardId = ref('');
const verifiedCards = computed(() => cardsStore.cards.filter((c) => c.status === 'VERIFIED'));
const selectedCard = computed(() => cardsStore.cards.find((c) => c.id === destCardId.value));

// ── Form state ────────────────────────────────────────────────────────────
const sourceIban = ref('');
const sourceBankId = ref('');
const sourceHolderName = ref('');
const amountStr = ref('');
const banks = ref<BankDto[]>([]);
const banksLoading = ref(false);

const amountUzs = computed(() => {
  const v = parseFloat(amountStr.value);
  return isNaN(v) ? 0 : v;
});

// ── Real-time fee preview ────────────────────────────────────────────────
const feeAmountUzs = ref<number | null>(null);
const feeLoading = ref(false);
let feeTimer: ReturnType<typeof setTimeout> | null = null;

watch([amountUzs, destCardId], () => {
  if (feeTimer) clearTimeout(feeTimer);
  feeAmountUzs.value = null;
  const amount = amountUzs.value;
  const card = selectedCard.value;
  if (!amount || amount <= 0 || !card) return;
  feeLoading.value = true;
  feeTimer = setTimeout(async () => {
    try {
      const res = await previewFee({
        amountUzs: amount,
        sourceNetwork: 'bank',
        destNetwork: card.cardNetwork.toLowerCase(),
        transferTypeId: 4,
      });
      feeAmountUzs.value = res.feeAmountUzs;
    } catch {
      /* ignore */
    } finally {
      feeLoading.value = false;
    }
  }, 400);
});

// ── Validation ────────────────────────────────────────────────────────────
const fieldErrors = ref<Record<string, string>>({});
const sendError = ref('');

// ── OTP step ──────────────────────────────────────────────────────────────
const isOtpStep = ref(false);
const pendingTransferId = ref('');
const otpCode = ref('');
const otpError = ref('');

// ── Success ───────────────────────────────────────────────────────────────
const completedTransfer = ref<TransferDto | null>(null);

onMounted(async () => {
  if (cardsStore.cards.length === 0) await cardsStore.fetchCards();
  if (verifiedCards.value.length > 0) destCardId.value = verifiedCards.value[0].id;

  banksLoading.value = true;
  try {
    banks.value = await listBanks();
  } catch {
    // ignore
  } finally {
    banksLoading.value = false;
  }
});

function validate(): boolean {
  fieldErrors.value = {};
  if (!sourceIban.value.trim()) fieldErrors.value.iban = 'IBAN kiriting';
  else if (!/^UZ\d{25}$/.test(sourceIban.value.trim()))
    fieldErrors.value.iban = "To'g'ri IBAN kiriting (UZ + 25 raqam)";
  if (!sourceBankId.value) fieldErrors.value.bank = 'Bankni tanlang';
  if (!sourceHolderName.value.trim()) fieldErrors.value.holder = 'Hisob egasining ismini kiriting';
  if (!destCardId.value) fieldErrors.value.card = 'Manzil kartani tanlang';
  if (!amountStr.value || amountUzs.value <= 0) fieldErrors.value.amount = 'Miqdorni kiriting';
  return Object.keys(fieldErrors.value).length === 0;
}

async function handlePull() {
  sendError.value = '';
  if (!validate()) return;

  try {
    const card = selectedCard.value!;
    const transfer = await a2pStore.pullFromBank({
      sourceIban: sourceIban.value.trim(),
      sourceBankId: sourceBankId.value,
      sourceAccountHolderName: sourceHolderName.value.trim(),
      destinationInstrumentId: card.id,
      destinationCardNetwork: card.cardNetwork.toLowerCase(),
      amountUzs: amountUzs.value,
      channel: 'mobile_app',
      idempotencyKey: `a2p-${card.id}-${Date.now()}`,
    });

    pendingTransferId.value = transfer.id;
    isOtpStep.value = true;
    otpCode.value = '';

    if (isDev && authStore.user) {
      try {
        const res = await fetchDevOtp(authStore.user.phoneE164);
        otpCode.value = res.code;
      } catch {
        // ignore
      }
    }
  } catch (err) {
    sendError.value =
      err instanceof ApiError ? err.message : "Xato yuz berdi. Qaytadan urinib ko'ring.";
  }
}

async function handleConfirmOtp() {
  otpError.value = '';
  try {
    const result = await a2pStore.confirmOtp(pendingTransferId.value, otpCode.value);
    isOtpStep.value = false;
    completedTransfer.value = result;
  } catch (err) {
    otpError.value = err instanceof ApiError ? err.message : 'OTP tasdiqlanmadi.';
  }
}
</script>

<template>
  <q-page
    style="
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 28px 18px 48px;
      color: #f7f4ed;
      font-family: Manrope, sans-serif;
    "
  >
    <!-- Header -->
    <div
      style="
        width: 100%;
        max-width: 640px;
        display: flex;
        align-items: center;
        gap: 14px;
        margin-bottom: 28px;
      "
    >
      <button
        type="button"
        style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 38px;
          height: 38px;
          border: 1px solid rgba(247, 244, 237, 0.14);
          border-radius: 12px;
          background: transparent;
          color: #f7f4ed;
          cursor: pointer;
        "
        @click="router.back()"
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
          <path d="m15 18-6-6 6-6"></path>
        </svg>
      </button>
      <div>
        <h1
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 22px;
            font-weight: 700;
            letter-spacing: -0.02em;
            margin: 0 0 2px;
          "
        >
          Bankdan kartaga
        </h1>
        <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 0">
          Bank hisobidan kartangizga pul o'tkazing
        </p>
      </div>
    </div>

    <!-- Success screen -->
    <div
      v-if="completedTransfer"
      style="
        width: 100%;
        max-width: 480px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        text-align: center;
        padding-top: 24px;
      "
    >
      <div
        style="
          width: 72px;
          height: 72px;
          border-radius: 50%;
          background: rgba(41, 190, 140, 0.16);
          display: flex;
          align-items: center;
          justify-content: center;
        "
      >
        <svg
          width="34"
          height="34"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#29BE8C"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <path d="m9 11 3 3L22 4"></path>
        </svg>
      </div>
      <div>
        <div
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 26px;
            font-weight: 700;
            letter-spacing: -0.025em;
            margin-bottom: 6px;
          "
        >
          Karta to'ldirildi
        </div>
        <div style="font-size: 14px; color: rgba(247, 244, 237, 0.55)">
          {{ completedTransfer.amountUzs.toLocaleString() }} UZS kartangizga o'tkazildi
        </div>
        <div
          v-if="(completedTransfer.feeAmountUzs ?? 0) > 0"
          style="font-size: 13px; color: rgba(247, 244, 237, 0.4); margin-top: 4px"
        >
          Komissiya: {{ completedTransfer.feeAmountUzs.toLocaleString() }} UZS
        </div>
      </div>
      <button
        class="pp-btn-primary"
        style="width: 100%; justify-content: center; padding: 14px; margin-top: 8px"
        @click="router.push('/transfers')"
      >
        Tarixni ko'rish
      </button>
      <button
        style="
          background: transparent;
          border: none;
          color: rgba(247, 244, 237, 0.55);
          font-size: 14px;
          cursor: pointer;
          text-decoration: underline;
        "
        @click="
          completedTransfer = null;
          amountStr = '';
          sourceIban = '';
          sourceBankId = '';
          sourceHolderName = '';
          feeAmountUzs = null;
        "
      >
        Yangi to'ldirish
      </button>
    </div>

    <!-- Transfer form -->
    <template v-else>
      <div style="width: 100%; max-width: 640px; display: flex; flex-direction: column; gap: 18px">
        <!-- Source IBAN -->
        <div>
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            Manba IBAN (bank hisobi)
          </label>
          <input
            v-model="sourceIban"
            type="text"
            placeholder="UZ1234567890123456789012345"
            maxlength="27"
            style="
              width: 100%;
              height: 54px;
              padding: 0 16px;
              background: rgba(14, 33, 28, 0.55);
              border: 1px solid rgba(247, 244, 237, 0.14);
              border-radius: 12px;
              font-family: 'Space Grotesk', sans-serif;
              font-size: 15px;
              color: #f7f4ed;
              outline: none;
              letter-spacing: 0.02em;
            "
            :style="fieldErrors.iban ? { borderColor: '#ff9c82' } : {}"
          />
          <p v-if="fieldErrors.iban" style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82">
            {{ fieldErrors.iban }}
          </p>
        </div>

        <!-- Source bank -->
        <div>
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            Bank
          </label>
          <select
            v-model="sourceBankId"
            :disabled="banksLoading"
            style="
              width: 100%;
              height: 54px;
              padding: 0 16px;
              background: rgba(14, 33, 28, 0.55);
              border: 1px solid rgba(247, 244, 237, 0.14);
              border-radius: 12px;
              font-family: Manrope, sans-serif;
              font-size: 15px;
              color: #f7f4ed;
              outline: none;
            "
            :style="fieldErrors.bank ? { borderColor: '#ff9c82' } : {}"
          >
            <option value="" disabled>
              {{ banksLoading ? 'Yuklanmoqda...' : 'Bankni tanlang' }}
            </option>
            <option v-for="bank in banks" :key="bank.id" :value="bank.id">
              {{ bank.name }} ({{ bank.mfoCode }})
            </option>
          </select>
          <p v-if="fieldErrors.bank" style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82">
            {{ fieldErrors.bank }}
          </p>
        </div>

        <!-- Source account holder name -->
        <div>
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            Hisob egasining ismi
          </label>
          <input
            v-model="sourceHolderName"
            type="text"
            placeholder="Ismi Familiyasi"
            style="
              width: 100%;
              height: 54px;
              padding: 0 16px;
              background: rgba(14, 33, 28, 0.55);
              border: 1px solid rgba(247, 244, 237, 0.14);
              border-radius: 12px;
              font-family: Manrope, sans-serif;
              font-size: 15px;
              color: #f7f4ed;
              outline: none;
            "
            :style="fieldErrors.holder ? { borderColor: '#ff9c82' } : {}"
          />
          <p v-if="fieldErrors.holder" style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82">
            {{ fieldErrors.holder }}
          </p>
        </div>

        <!-- Destination card -->
        <div>
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            Manzil karta (to'ldiriladi)
          </label>
          <select
            v-model="destCardId"
            style="
              width: 100%;
              height: 54px;
              padding: 0 16px;
              background: rgba(14, 33, 28, 0.55);
              border: 1px solid rgba(247, 244, 237, 0.14);
              border-radius: 12px;
              font-family: Manrope, sans-serif;
              font-size: 15px;
              color: #f7f4ed;
              outline: none;
            "
            :style="fieldErrors.card ? { borderColor: '#ff9c82' } : {}"
          >
            <option value="" disabled>Kartani tanlang</option>
            <option v-for="card in verifiedCards" :key="card.id" :value="card.id">
              {{ card.maskedPan }} — {{ card.cardNetwork }}
            </option>
          </select>
          <p v-if="fieldErrors.card" style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82">
            {{ fieldErrors.card }}
          </p>
          <p
            v-if="verifiedCards.length === 0"
            style="margin: 6px 0 0; font-size: 12.5px; color: rgba(247, 244, 237, 0.4)"
          >
            Tasdiqlangan karta yo'q
          </p>
        </div>

        <!-- Amount -->
        <div>
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            Miqdor (UZS)
          </label>
          <input
            v-model="amountStr"
            type="number"
            min="1"
            step="1000"
            placeholder="1 000 000"
            style="
              width: 100%;
              height: 54px;
              padding: 0 16px;
              background: rgba(14, 33, 28, 0.55);
              border: 1px solid rgba(247, 244, 237, 0.14);
              border-radius: 12px;
              font-family: 'Space Grotesk', sans-serif;
              font-size: 18px;
              font-weight: 600;
              color: #f7f4ed;
              outline: none;
            "
            :style="fieldErrors.amount ? { borderColor: '#ff9c82' } : {}"
          />
          <p v-if="fieldErrors.amount" style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82">
            {{ fieldErrors.amount }}
          </p>
          <div
            v-if="feeLoading"
            style="margin-top: 8px; font-size: 13px; color: rgba(247, 244, 237, 0.4)"
          >
            Komissiya hisoblanmoqda...
          </div>
          <div
            v-else-if="feeAmountUzs !== null && !fieldErrors.amount"
            style="margin-top: 8px; font-size: 13px; color: rgba(247, 244, 237, 0.55)"
          >
            Komissiya:
            <strong style="color: #f7f4ed">{{ feeAmountUzs.toLocaleString() }} UZS</strong>
            &nbsp;·&nbsp; Bank debeti:
            <strong style="color: #f7f4ed"
              >{{ (amountUzs + feeAmountUzs).toLocaleString() }} UZS</strong
            >
          </div>
          <div
            v-else-if="!fieldErrors.amount"
            style="margin-top: 8px; font-size: 13px; color: rgba(247, 244, 237, 0.4)"
          >
            Komissiya qo'llanilishi mumkin (0.5%, min 2 000 UZS)
          </div>
        </div>

        <!-- Error -->
        <div
          v-if="sendError"
          style="
            padding: 12px 16px;
            background: rgba(255, 156, 130, 0.12);
            border: 1px solid rgba(255, 156, 130, 0.3);
            border-radius: 10px;
            font-size: 13.5px;
            color: #ff9c82;
          "
        >
          {{ sendError }}
        </div>

        <!-- Submit -->
        <button
          class="pp-btn-primary"
          style="width: 100%; justify-content: center; padding: 16px; font-size: 16px"
          :disabled="a2pStore.isLoading || verifiedCards.length === 0"
          @click="handlePull"
        >
          {{ a2pStore.isLoading ? 'Yuklanmoqda...' : 'Davom etish' }}
        </button>
      </div>

      <!-- OTP Overlay -->
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
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
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
              :disabled="a2pStore.isLoading"
              @click="handleConfirmOtp"
            >
              {{ a2pStore.isLoading ? 'Tasdiqlanmoqda...' : 'Tasdiqlash' }}
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
    </template>
  </q-page>
</template>
