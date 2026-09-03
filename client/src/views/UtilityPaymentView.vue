<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCardsStore } from '@/stores/cards';
import { useP2STransfersStore } from '@/stores/p2sTransfers';
import { useAuthStore } from '@/stores/auth';
import { validatePrepayment } from '@/lib/api/p2sTransfers';
import { fetchDevOtp, previewFee } from '@/lib/api/transfers';
import { ApiError } from '@/lib/api/client';
import type { PaynetProviderDto } from '@/lib/api/p2sTransfers';
import type { TransferDto } from '@/lib/api/transfers';

const router = useRouter();
const cardsStore = useCardsStore();
const p2sStore = useP2STransfersStore();
const authStore = useAuthStore();
const isDev = import.meta.env.DEV;

// ── Step state ────────────────────────────────────────────────
const selectedProvider = ref<PaynetProviderDto | null>(null);
const completedTransfer = ref<TransferDto | null>(null);

// ── Card selection ────────────────────────────────────────────
const senderCardId = ref('');
const verifiedCards = computed(() => cardsStore.cards.filter((c) => c.status === 'VERIFIED'));
const selectedCard = computed(() => verifiedCards.value.find((c) => c.id === senderCardId.value));

// ── Dynamic service fields ────────────────────────────────────
const serviceFields = ref<Record<string, string>>({});

function initServiceFields(provider: PaynetProviderDto) {
  const fields: Record<string, string> = {};
  for (const name of provider.fieldNames) fields[name] = '';
  serviceFields.value = fields;
}

// ── Amount ────────────────────────────────────────────────────
const amountStr = ref('');
const amountUzs = computed(() => {
  const v = parseFloat(amountStr.value);
  return isNaN(v) ? 0 : v;
});

// ── Fee preview ───────────────────────────────────────────────
const feeAmountUzs = ref<number | null>(null);
const feeLoading = ref(false);
let feeTimer: ReturnType<typeof setTimeout> | null = null;

watch([amountUzs, senderCardId], () => {
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
        sourceNetwork: card.cardNetwork ?? 'uzcard',
        destNetwork: 'paynet',
        transferTypeId: 7,
      });
      feeAmountUzs.value = res.feeAmountUzs;
    } catch {
      /* ignore */
    } finally {
      feeLoading.value = false;
    }
  }, 400);
});

// ── Validation ────────────────────────────────────────────────
const fieldErrors = ref<Record<string, string>>({});
const sendError = ref('');

// ── OTP step ──────────────────────────────────────────────────
const isOtpStep = ref(false);
const pendingTransferId = ref('');
const otpCode = ref('');
const otpError = ref('');

onMounted(async () => {
  if (cardsStore.cards.length === 0) await cardsStore.fetchCards();
  if (verifiedCards.value.length > 0) senderCardId.value = verifiedCards.value[0].id;
  await p2sStore.fetchProviders();
});

function selectProvider(provider: PaynetProviderDto) {
  selectedProvider.value = provider;
  initServiceFields(provider);
  sendError.value = '';
  fieldErrors.value = {};
  amountStr.value = '';
  feeAmountUzs.value = null;
}

function categoryIcon(category: string): string {
  const c = category.toLowerCase();
  if (c.includes('gas') || c === 'gas') return 'flame';
  if (c.includes('water') || c === 'water') return 'droplet';
  return 'zap';
}

function validate(): boolean {
  fieldErrors.value = {};
  const provider = selectedProvider.value!;
  for (const name of provider.fieldNames) {
    if (!serviceFields.value[name]?.trim()) {
      fieldErrors.value[name] = `${fieldLabel(name)} kiriting`;
    }
  }
  if (!senderCardId.value) fieldErrors.value.card = "To'lov kartasini tanlang";
  if (!amountStr.value || amountUzs.value <= 0)
    fieldErrors.value.amount = "To'lov miqdorini kiriting";
  return Object.keys(fieldErrors.value).length === 0;
}

// Uzbek labels for common Paynet field codes (matches PHP project's titleUz from Paynet API)
const FIELD_LABELS: Record<string, string> = {
  account: 'Shaxsiy hisob raqam',
  account_number: 'Hisob raqami',
  clientid: 'Mijoz ID',
  client_id: 'Mijoz ID',
  abonent_id: 'Abonent ID',
  ls: 'Shaxsiy hisob',
  ls_number: 'Shaxsiy hisob raqami',
  personal_account: 'Shaxsiy hisob',
  phone: 'Telefon raqami',
  amount: 'Miqdor',
  region: 'Hudud',
  address: 'Manzil',
  name: 'Ism',
  number: 'Raqam',
  inn: 'STIR',
  tin: 'STIR',
  pinfl: 'JSHSHIR',
  passport: 'Pasport raqami',
  meter: 'Hisoblagich raqami',
  meter_number: 'Hisoblagich raqami',
  contract: 'Shartnoma raqami',
  contract_number: 'Shartnoma raqami',
};

function fieldLabel(name: string): string {
  return FIELD_LABELS[name.toLowerCase()] ?? name.replace(/_/g, ' ');
}

async function handleContinue() {
  sendError.value = '';
  if (!validate()) return;

  try {
    // Server-side field validation
    await validatePrepayment(selectedProvider.value!.serviceCode, serviceFields.value);
  } catch (err) {
    sendError.value = err instanceof ApiError ? err.message : 'Xizmat vaqtincha mavjud emas.';
    return;
  }

  try {
    const card = selectedCard.value!;
    const transfer = await p2sStore.initiate({
      senderInstrumentId: card.id,
      senderCardNetwork: card.cardNetwork ?? 'uzcard',
      serviceCode: selectedProvider.value!.serviceCode,
      serviceFields: serviceFields.value,
      amountUzs: amountUzs.value,
      channel: 'mobile_app',
      idempotencyKey: crypto.randomUUID(),
    });

    pendingTransferId.value = transfer.id;
    isOtpStep.value = true;
    otpCode.value = '';

    if (isDev && authStore.user) {
      try {
        const res = await fetchDevOtp(authStore.user.phoneE164);
        otpCode.value = res.code;
      } catch {
        /* ignore */
      }
    }
  } catch (err) {
    sendError.value = err instanceof ApiError ? err.message : 'Xizmat vaqtincha mavjud emas.';
  }
}

async function handleConfirmOtp() {
  otpError.value = '';
  try {
    const result = await p2sStore.confirmOtp(pendingTransferId.value, otpCode.value);
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
          flex-shrink: 0;
        "
        @click="selectedProvider ? (selectedProvider = null) : router.back()"
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
          {{ selectedProvider ? selectedProvider.serviceName : "Kommunal to'lovlar" }}
        </h1>
        <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 0">
          {{ selectedProvider ? selectedProvider.category : 'Kommunal xizmatlar' }}
        </p>
      </div>
    </div>

    <!-- Step 4: Success -->
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
          To'lov amalga oshirildi
        </div>
        <div style="font-size: 14px; color: rgba(247, 244, 237, 0.55)">
          {{ selectedProvider?.serviceName }} ·
          {{ completedTransfer.amountUzs.toLocaleString() }} so'm
        </div>
        <div
          v-if="(completedTransfer.feeAmountUzs ?? 0) > 0"
          style="font-size: 13px; color: rgba(247, 244, 237, 0.4); margin-top: 4px"
        >
          Komissiya: {{ completedTransfer.feeAmountUzs.toLocaleString() }} UZS
        </div>
        <div
          style="
            font-size: 12px;
            color: rgba(247, 244, 237, 0.3);
            margin-top: 8px;
            font-family: 'Space Grotesk', monospace;
          "
        >
          ID: {{ completedTransfer.id.slice(0, 8) }}…
        </div>
      </div>
      <div
        style="
          width: 100%;
          background: rgba(247, 244, 237, 0.04);
          border: 1px solid rgba(247, 244, 237, 0.09);
          border-radius: 14px;
          padding: 16px 20px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        "
      >
        <div style="display: flex; justify-content: space-between; font-size: 14px">
          <span style="color: rgba(247, 244, 237, 0.5)">Xizmat ko'rsatuvchi</span>
          <span style="font-weight: 600">{{ selectedProvider?.serviceName }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 14px">
          <span style="color: rgba(247, 244, 237, 0.5)">Summa</span>
          <span style="font-weight: 600"
            >{{ completedTransfer.amountUzs.toLocaleString() }} so'm</span
          >
        </div>
        <div
          v-if="(completedTransfer.feeAmountUzs ?? 0) > 0"
          style="display: flex; justify-content: space-between; font-size: 14px"
        >
          <span style="color: rgba(247, 244, 237, 0.5)">Komissiya</span>
          <span style="font-weight: 600"
            >{{ completedTransfer.feeAmountUzs.toLocaleString() }} so'm</span
          >
        </div>
        <div style="height: 1px; background: rgba(247, 244, 237, 0.08)"></div>
        <div style="display: flex; justify-content: space-between; font-size: 14px">
          <span style="color: rgba(247, 244, 237, 0.5)">Holat</span>
          <span
            style="
              font-weight: 600;
              color: #29be8c;
              background: rgba(41, 190, 140, 0.12);
              padding: 2px 10px;
              border-radius: 999px;
              font-size: 12.5px;
            "
            >{{ completedTransfer.status }}</span
          >
        </div>
      </div>
      <button
        class="pp-btn-primary"
        style="width: 100%; justify-content: center; padding: 14px; margin-top: 4px"
        @click="router.push('/')"
      >
        Tayyor
      </button>
    </div>

    <!-- Steps 1 & 2 -->
    <template v-else>
      <!-- Step 1: Provider selection -->
      <div v-if="!selectedProvider" style="width: 100%; max-width: 640px">
        <div
          v-if="p2sStore.isLoading"
          style="
            text-align: center;
            color: rgba(247, 244, 237, 0.4);
            padding: 40px 0;
            font-size: 14px;
          "
        >
          Yuklanmoqda...
        </div>
        <div
          v-else
          style="
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
            gap: 14px;
          "
        >
          <button
            v-for="provider in p2sStore.providers"
            :key="provider.id"
            type="button"
            style="
              display: flex;
              flex-direction: column;
              align-items: flex-start;
              gap: 12px;
              padding: 20px;
              background: rgba(247, 244, 237, 0.04);
              border: 1px solid rgba(247, 244, 237, 0.1);
              border-radius: 18px;
              cursor: pointer;
              text-align: left;
              transition:
                background 0.15s,
                border-color 0.15s;
              color: #f7f4ed;
              font-family: Manrope, sans-serif;
            "
            @click="selectProvider(provider)"
            @mouseenter="
              ($event.currentTarget as HTMLElement).style.background = 'rgba(247,244,237,0.08)'
            "
            @mouseleave="
              ($event.currentTarget as HTMLElement).style.background = 'rgba(247,244,237,0.04)'
            "
          >
            <!-- Icon -->
            <div
              style="
                width: 44px;
                height: 44px;
                border-radius: 14px;
                background: rgba(41, 190, 140, 0.14);
                display: flex;
                align-items: center;
                justify-content: center;
              "
            >
              <!-- Gas icon -->
              <svg
                v-if="categoryIcon(provider.category) === 'flame'"
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"
                ></path>
              </svg>
              <!-- Water icon -->
              <svg
                v-else-if="categoryIcon(provider.category) === 'droplet'"
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"
                ></path>
              </svg>
              <!-- Electricity / zap icon -->
              <svg
                v-else
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"></path>
              </svg>
            </div>
            <div>
              <div style="font-size: 14.5px; font-weight: 700; margin-bottom: 4px">
                {{ provider.serviceName }}
              </div>
              <div style="font-size: 12px; color: rgba(247, 244, 237, 0.45)">
                {{ provider.category }}
              </div>
            </div>
          </button>
        </div>
      </div>

      <!-- Step 2: Fields + card + amount -->
      <div
        v-else
        style="width: 100%; max-width: 640px; display: flex; flex-direction: column; gap: 18px"
      >
        <!-- Dynamic service fields -->
        <div v-for="fieldName in selectedProvider.fieldNames" :key="fieldName">
          <label
            style="
              display: block;
              font-size: 12.5px;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.62);
              margin-bottom: 8px;
            "
          >
            {{ fieldLabel(fieldName) }}
          </label>
          <input
            v-model="serviceFields[fieldName]"
            type="text"
            :placeholder="fieldLabel(fieldName)"
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
            "
            :style="fieldErrors[fieldName] ? { borderColor: '#ff9c82' } : {}"
          />
          <p
            v-if="fieldErrors[fieldName]"
            style="margin: 6px 0 0; font-size: 12.5px; color: #ff9c82"
          >
            {{ fieldErrors[fieldName] }}
          </p>
        </div>

        <!-- Card selector -->
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
            To'lov kartasi
          </label>
          <select
            v-model="senderCardId"
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
            Faol karta mavjud emas
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
            To'lov summasi (so'm)
          </label>
          <input
            v-model="amountStr"
            type="number"
            min="1"
            step="1000"
            placeholder="50 000"
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
            <strong style="color: #f7f4ed">{{ feeAmountUzs.toLocaleString() }} so'm</strong>
            &nbsp;·&nbsp; Jami:
            <strong style="color: #f7f4ed"
              >{{ (amountUzs + feeAmountUzs).toLocaleString() }} so'm</strong
            >
          </div>
          <div
            v-else-if="!fieldErrors.amount"
            style="margin-top: 8px; font-size: 13px; color: rgba(247, 244, 237, 0.4)"
          >
            Komissiya olinishi mumkin
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

        <!-- Continue button -->
        <button
          class="pp-btn-primary"
          style="width: 100%; justify-content: center; padding: 16px; font-size: 16px"
          :disabled="p2sStore.isLoading || verifiedCards.length === 0"
          @click="handleContinue"
        >
          {{ p2sStore.isLoading ? 'Yuklanmoqda...' : "To'lovni tasdiqlash" }}
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
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
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
                Dev rejim: kod avtomatik to'ldirildi — {{ otpCode }}
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
              :disabled="p2sStore.isLoading"
              @click="handleConfirmOtp"
            >
              {{ p2sStore.isLoading ? 'Tasdiqlanmoqda...' : 'Tasdiqlash' }}
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
