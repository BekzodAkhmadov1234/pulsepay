<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useCardsStore } from '@/stores/cards';
import { useAuthStore } from '@/stores/auth';
import { ApiError } from '@/lib/api/client';

const { t } = useI18n();
import type { AddCardPayload, StatementEntry, CardLimitDto, LimitTypeDto } from '@/lib/api/cards';

const store = useCardsStore();
const authStore = useAuthStore();

const showForm = ref(false);
const submitError = ref('');
const fieldErrors = ref<Partial<Record<keyof AddCardPayload, string>>>({});

const rawPan = ref('');
const form = ref<Omit<AddCardPayload, 'cardToken' | 'maskedPan'>>({
  cardHolderName: '',
  expMonth: 1,
  expYear: new Date().getFullYear(),
});

const removingId = ref<string | null>(null);
const settingDefaultId = ref<string | null>(null);
const blockingId = ref<string | null>(null);

// ── Statement panel ────────────────────────────────────────────────────────
const statementCardId = ref<string | null>(null);
const statementEntries = ref<StatementEntry[]>([]);
const statementLoading = ref(false);
const statementError = ref('');

async function openStatement(cardId: string) {
  statementCardId.value = cardId;
  statementEntries.value = [];
  statementError.value = '';
  statementLoading.value = true;
  try {
    statementEntries.value = await store.fetchStatement(cardId);
  } catch (err) {
    statementError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    statementLoading.value = false;
  }
}

function closeStatement() {
  statementCardId.value = null;
}

// ── Limits panel ────────────────────────────────────────────────────────────
const limitsCardId = ref<string | null>(null);
const limitsCardNetwork = ref<string | null>(null);
const cardLimits = ref<CardLimitDto[]>([]);
const limitTypes = ref<LimitTypeDto[]>([]);
const limitsLoading = ref(false);
const limitsError = ref('');
const setLimitType = ref('');
const setLimitValue = ref('');
const setLimitFrom = ref('');
const setLimitTo = ref('');
const savingLimit = ref(false);
const removingLimitType = ref<string | null>(null);

async function openLimits(cardId: string, network: string) {
  limitsCardId.value = cardId;
  limitsCardNetwork.value = network;
  cardLimits.value = [];
  limitsError.value = '';
  setLimitType.value = '';
  setLimitValue.value = '';
  limitsLoading.value = true;
  try {
    [cardLimits.value, limitTypes.value] = await Promise.all([
      store.fetchLimits(cardId),
      store.fetchLimitTypes(),
    ]);
  } catch (err) {
    limitsError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    limitsLoading.value = false;
  }
}

function closeLimits() {
  limitsCardId.value = null;
}

async function handleSetLimit() {
  if (!limitsCardId.value || !setLimitType.value || !setLimitValue.value) return;
  savingLimit.value = true;
  limitsError.value = '';
  try {
    const valueTiyin = Math.round(parseFloat(setLimitValue.value) * 100);
    cardLimits.value = await store.saveLimits(limitsCardId.value, [
      {
        type: setLimitType.value,
        valueTiyin,
        dateFrom: setLimitFrom.value || undefined,
        dateTo: setLimitTo.value || undefined,
      },
    ]);
    setLimitType.value = '';
    setLimitValue.value = '';
    setLimitFrom.value = '';
    setLimitTo.value = '';
  } catch (err) {
    limitsError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    savingLimit.value = false;
  }
}

async function handleRemoveLimit(limitType: string) {
  if (!limitsCardId.value) return;
  removingLimitType.value = limitType;
  try {
    await store.deleteLimit(limitsCardId.value, limitType);
    cardLimits.value = cardLimits.value.filter((l) => l.type !== limitType);
  } catch (err) {
    limitsError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    removingLimitType.value = null;
  }
}

function selectedLimitRequiresDate(): boolean {
  return limitTypes.value.find((t) => t.code === setLimitType.value)?.selectDate ?? false;
}

// ── PIN panel ───────────────────────────────────────────────────────────────
const pinCardId = ref<string | null>(null);
const pinCardNetwork = ref<'humo' | 'uzcard' | null>(null);
const pinNew = ref('');
const pinConfirm = ref('');
const pinError = ref('');
const pinSuccess = ref(false);
const savingPin = ref(false);

function openPin(cardId: string, network: string) {
  if (network !== 'humo' && network !== 'uzcard') return;
  pinCardId.value = cardId;
  pinCardNetwork.value = network as 'humo' | 'uzcard';
  pinNew.value = '';
  pinConfirm.value = '';
  pinError.value = '';
  pinSuccess.value = false;
}

function closePin() {
  pinCardId.value = null;
}

async function handlePinChange() {
  pinError.value = '';
  if (pinNew.value.length !== 4 || !/^\d{4}$/.test(pinNew.value)) {
    pinError.value = t('validation.pin_format');
    return;
  }
  if (pinNew.value !== pinConfirm.value) {
    pinError.value = t('cards.pin_mismatch');
    return;
  }
  if (!pinCardId.value || !pinCardNetwork.value) return;
  savingPin.value = true;
  try {
    await store.changePin(pinCardId.value, pinCardNetwork.value, pinNew.value);
    pinSuccess.value = true;
    pinNew.value = '';
    pinConfirm.value = '';
  } catch (err) {
    pinError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    savingPin.value = false;
  }
}

onMounted(() => {
  store.fetchCards();
});

function onPanInput(e: Event) {
  const digits = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 16);
  rawPan.value = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
}

function onExpiryInput(e: Event) {
  const raw = (e.target as HTMLInputElement).value;
  const d = raw.replace(/\D/g, '').slice(0, 4);
  const formatted = d.length > 2 ? d.slice(0, 2) + '/' + d.slice(2) : d;
  expiryDisplay.value = formatted;
  const m = parseInt(d.slice(0, 2), 10);
  const y = d.length >= 4 ? parseInt('20' + d.slice(2, 4), 10) : 0;
  form.value.expMonth = m || 1;
  form.value.expYear = y || new Date().getFullYear();
}

const expiryDisplay = ref('');

function resetForm() {
  rawPan.value = '';
  expiryDisplay.value = '';
  form.value = { cardHolderName: '', expMonth: 1, expYear: new Date().getFullYear() };
  submitError.value = '';
  fieldErrors.value = {};
  showForm.value = false;
}

function validate(): boolean {
  fieldErrors.value = {};
  const panDigits = rawPan.value.replace(/\D/g, '');
  if (!panDigits) fieldErrors.value.maskedPan = t('validation.card_number_required');
  else if (panDigits.length !== 16)
    fieldErrors.value.maskedPan = t('validation.card_number_length');
  if (!form.value.cardHolderName.trim())
    fieldErrors.value.cardHolderName = t('validation.cardholder_required');
  if (form.value.expMonth < 1 || form.value.expMonth > 12)
    fieldErrors.value.expMonth = t('validation.expiry_month_invalid');
  const now = new Date();
  const expired =
    form.value.expYear < now.getFullYear() ||
    (form.value.expYear === now.getFullYear() && form.value.expMonth < now.getMonth() + 1);
  if (expired) fieldErrors.value.expYear = t('validation.card_expired');
  return Object.keys(fieldErrors.value).length === 0;
}

async function handleAdd() {
  submitError.value = '';
  if (!validate()) return;
  try {
    const panDigits = rawPan.value.replace(/\D/g, '');
    const maskedPan = `${panDigits.slice(0, 6)}${'*'.repeat(6)}${panDigits.slice(-4)}`;
    await store.addCard({ ...form.value, maskedPan, cardToken: `tok_${crypto.randomUUID()}` });
    resetForm();
  } catch (err) {
    submitError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  }
}

async function handleSetDefault(cardId: string) {
  settingDefaultId.value = cardId;
  try {
    await store.setDefault(cardId);
  } catch (err) {
    if (err instanceof ApiError) submitError.value = err.message;
  } finally {
    settingDefaultId.value = null;
  }
}

async function handleRemove(cardId: string) {
  removingId.value = cardId;
  try {
    await store.removeCard(cardId);
  } catch (err) {
    if (err instanceof ApiError) submitError.value = err.message;
  } finally {
    removingId.value = null;
  }
}

async function handleToggleBlock(cardId: string, currentStatus: string) {
  blockingId.value = cardId;
  submitError.value = '';
  try {
    if (currentStatus === 'INACTIVE') {
      await store.unblockCard(cardId);
    } else {
      await store.blockCard(cardId);
    }
  } catch (err) {
    submitError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    blockingId.value = null;
  }
}

function networkLabel(network: string | null) {
  if (network === 'uzcard') return 'UzCard';
  if (network === 'humo') return 'HUMO';
  return 'Karta';
}

function expiry(month: number, year: number) {
  return `${String(month).padStart(2, '0')}/${String(year).slice(-2)}`;
}
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- Header -->
      <div
        style="
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: 20px;
          flex-wrap: wrap;
        "
      >
        <div>
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
            {{ t('cards.title') }}
          </h1>
          <p
            style="
              font-size: 15px;
              line-height: 1.5;
              color: rgba(247, 244, 237, 0.6);
              margin: 10px 0 0;
            "
          >
            {{ t('cards.subtitle') }}
          </p>
        </div>
        <button
          class="pp-btn-primary"
          style="padding: 13px 22px; font-size: 13.5px"
          @click="showForm = !showForm"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 5v14M5 12h14"></path>
          </svg>
          {{ t('cards.add_card') }}
        </button>
      </div>

      <!-- Error banner -->
      <div
        v-if="submitError"
        style="
          margin-top: 20px;
          padding: 14px 18px;
          background: rgba(255, 156, 130, 0.12);
          border: 1px solid rgba(255, 156, 130, 0.3);
          border-radius: 12px;
          font-size: 13.5px;
          color: #ff9c82;
        "
      >
        {{ submitError }}
      </div>

      <!-- Add-card form -->
      <div
        v-if="showForm"
        style="
          margin-top: 26px;
          background: rgba(247, 244, 237, 0.045);
          border: 1px solid rgba(247, 244, 237, 0.12);
          border-radius: 22px;
          padding: clamp(22px, 3vw, 30px);
        "
      >
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 16px">
          <h3
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: 19px;
              font-weight: 600;
              letter-spacing: -0.015em;
              margin: 0;
              color: #f7f4ed;
            "
          >
            {{ t('cards.add_new_card') }}
          </h3>
          <button
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              width: 32px;
              height: 32px;
              border: none;
              border-radius: 9px;
              background: transparent;
              color: rgba(247, 244, 237, 0.5);
              cursor: pointer;
              transition:
                background 0.15s,
                color 0.15s;
            "
            @click="resetForm"
          >
            <svg
              width="16"
              height="16"
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

        <div style="display: flex; flex-wrap: wrap; gap: 16px; margin-top: 22px">
          <div style="flex: 2 1 240px; min-width: 0">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >{{ t('cards.card_number') }}</label
            >
            <input
              type="tel"
              inputmode="numeric"
              placeholder="8600 1234 5678 9012"
              :value="rawPan"
              maxlength="19"
              class="pp-input"
              :class="{ error: fieldErrors.maskedPan }"
              style="font-family: 'Space Grotesk', sans-serif; letter-spacing: 0.08em"
              @input="onPanInput"
            />
            <div v-if="fieldErrors.maskedPan" class="pp-field-error">
              {{ fieldErrors.maskedPan }}
            </div>
          </div>
          <div style="flex: 2 1 240px; min-width: 0">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >{{ t('cards.cardholder_name') }}</label
            >
            <input
              v-model="form.cardHolderName"
              type="text"
              placeholder="ALISHER KARIMOV"
              class="pp-input"
              :class="{ error: fieldErrors.cardHolderName }"
            />
            <div v-if="fieldErrors.cardHolderName" class="pp-field-error">
              {{ fieldErrors.cardHolderName }}
            </div>
          </div>
          <div style="flex: 1 1 150px; max-width: 200px; min-width: 0">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >{{ t('cards.expiry') }}</label
            >
            <input
              type="tel"
              inputmode="numeric"
              placeholder="01/29"
              :value="expiryDisplay"
              maxlength="5"
              class="pp-input"
              :class="{ error: fieldErrors.expMonth || fieldErrors.expYear }"
              style="font-family: 'Space Grotesk', sans-serif; letter-spacing: 0.06em"
              @input="onExpiryInput"
            />
            <div v-if="fieldErrors.expMonth || fieldErrors.expYear" class="pp-field-error">
              {{ fieldErrors.expMonth || fieldErrors.expYear }}
            </div>
          </div>
        </div>

        <div
          style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 22px"
        >
          <button
            class="pp-btn-primary"
            style="padding: 13px 24px"
            :disabled="store.isLoading"
            @click="handleAdd"
          >
            {{ store.isLoading ? t('common.saving') : t('cards.save_card') }}
          </button>
          <button class="pp-btn-ghost" style="padding: 13px 24px" @click="resetForm">
            {{ t('common.cancel') }}
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div
        v-if="store.isLoading && store.cards.length === 0"
        style="display: flex; justify-content: center; padding: 64px"
      >
        <div class="pp-spinner"></div>
      </div>

      <!-- Card grid (wallet tile always first) -->
      <div v-else style="display: flex; flex-wrap: wrap; gap: 18px; margin-top: 28px">
        <!-- Wallet tile -->
        <div
          style="
            flex: 1 1 340px;
            max-width: 420px;
            min-width: 0;
            position: relative;
            overflow: hidden;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.1);
            border-radius: 22px;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 22px;
          "
        >
          <div
            style="
              position: absolute;
              width: 300px;
              height: 300px;
              right: -140px;
              top: -170px;
              border-radius: 50%;
              background: radial-gradient(
                circle,
                rgba(247, 244, 237, 0.07) 0%,
                rgba(247, 244, 237, 0) 68%
              );
              pointer-events: none;
            "
          ></div>
          <div
            style="
              position: relative;
              display: flex;
              align-items: center;
              justify-content: space-between;
              gap: 12px;
            "
          >
            <span
              style="
                padding: 5px 11px;
                border-radius: 7px;
                background: rgba(41, 190, 140, 0.16);
                font-size: 11.5px;
                font-weight: 700;
                letter-spacing: 0.06em;
                color: #29be8c;
              "
              >{{ t('cards.wallet') }}</span
            >
            <div
              style="
                flex: none;
                width: 32px;
                height: 32px;
                border-radius: 9px;
                background: rgba(41, 190, 140, 0.16);
                display: flex;
                align-items: center;
                justify-content: center;
              "
            >
              <svg
                width="17"
                height="17"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2"></path>
                <path d="M3 8v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3h-4a2 2 0 0 1 0-4h4"></path>
              </svg>
            </div>
          </div>
          <div style="position: relative">
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: clamp(19px, 2.2vw, 23px);
                font-weight: 600;
                letter-spacing: -0.01em;
                color: #f7f4ed;
              "
            >
              {{ t('home.my_wallet') }}
            </div>
            <div
              style="
                display: flex;
                align-items: baseline;
                justify-content: space-between;
                gap: 16px;
                margin-top: 14px;
              "
            >
              <div
                style="
                  font-size: 13.5px;
                  font-weight: 500;
                  letter-spacing: 0.04em;
                  color: rgba(247, 244, 237, 0.7);
                  text-transform: uppercase;
                "
              >
                {{ authStore.user?.fullName || '—' }}
              </div>
              <div style="flex: none; text-align: right">
                <div
                  style="
                    font-size: 10.5px;
                    font-weight: 700;
                    letter-spacing: 0.1em;
                    text-transform: uppercase;
                    color: rgba(247, 244, 237, 0.4);
                  "
                >
                  {{ t('cards.account_type') }}
                </div>
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 14.5px;
                    font-weight: 600;
                    margin-top: 3px;
                    color: #f7f4ed;
                  "
                >
                  PulsePay
                </div>
              </div>
            </div>
          </div>
          <div
            style="
              position: relative;
              padding-top: 18px;
              border-top: 1px solid rgba(247, 244, 237, 0.12);
            "
          >
            <div
              style="
                font-size: 11.5px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
              "
            >
              {{ t('cards.balance') }}
            </div>
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 26px;
                font-weight: 600;
                letter-spacing: -0.02em;
                margin-top: 7px;
                color: #f7f4ed;
              "
            >
              — UZS
            </div>
          </div>
        </div>

        <!-- Empty state card (when no physical cards) -->
        <div
          v-if="store.cards.length === 0"
          style="
            flex: 1 1 340px;
            max-width: 420px;
            min-width: 0;
            background: rgba(247, 244, 237, 0.02);
            border: 1px dashed rgba(247, 244, 237, 0.15);
            border-radius: 22px;
            padding: 40px 24px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            gap: 12px;
          "
        >
          <svg
            width="36"
            height="36"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.25)"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="2" y="5" width="20" height="14" rx="3"></rect>
            <path d="M2 10h20"></path>
          </svg>
          <p style="font-size: 14.5px; font-weight: 600; color: #f7f4ed; margin: 0">
            {{ t('cards.empty_title') }}
          </p>
          <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 0">
            {{ t('cards.empty_subtitle') }}
          </p>
        </div>
        <div
          v-for="card in store.cards"
          :key="card.id"
          style="
            flex: 1 1 340px;
            max-width: 420px;
            min-width: 0;
            position: relative;
            overflow: hidden;
            border-radius: 22px;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 22px;
          "
          :style="{
            background: card.isDefault
              ? 'linear-gradient(140deg, rgba(41,190,140,0.20) 0%, rgba(247,244,237,0.05) 100%)'
              : 'rgba(247,244,237,0.045)',
            border: card.isDefault
              ? '1px solid rgba(41,190,140,0.34)'
              : '1px solid rgba(247,244,237,0.1)',
          }"
        >
          <!-- Glow circle -->
          <div
            style="
              position: absolute;
              width: 300px;
              height: 300px;
              right: -140px;
              top: -170px;
              border-radius: 50%;
              pointer-events: none;
            "
            :style="{
              background: card.isDefault
                ? 'radial-gradient(circle, rgba(41,190,140,0.22) 0%, rgba(41,190,140,0) 68%)'
                : 'radial-gradient(circle, rgba(247,244,237,0.07) 0%, rgba(247,244,237,0) 68%)',
            }"
          ></div>

          <!-- Status badge (shown only for non-VERIFIED cards) -->
          <div
            v-if="card.status === 'INACTIVE' || card.status === 'BLOCKED'"
            style="
              position: relative;
              display: flex;
              align-items: center;
              gap: 8px;
              padding: 7px 12px;
              border-radius: 10px;
              margin-bottom: -6px;
            "
            :style="{
              background:
                card.status === 'BLOCKED' ? 'rgba(255,87,87,0.13)' : 'rgba(242,178,62,0.13)',
            }"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              :stroke="card.status === 'BLOCKED' ? '#ff5757' : '#F2B23E'"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
            </svg>
            <span
              style="font-size: 11px; font-weight: 700; letter-spacing: 0.08em"
              :style="{ color: card.status === 'BLOCKED' ? '#ff5757' : '#F2B23E' }"
            >
              {{ card.status === 'BLOCKED' ? t('cards.blocked_label') : t('cards.inactive_label') }}
            </span>
          </div>

          <!-- Brand + actions -->
          <div
            style="
              position: relative;
              display: flex;
              align-items: center;
              justify-content: space-between;
              gap: 12px;
            "
          >
            <span
              style="
                padding: 5px 11px;
                border-radius: 7px;
                background: rgba(247, 244, 237, 0.12);
                font-size: 11.5px;
                font-weight: 700;
                letter-spacing: 0.06em;
                color: #f7f4ed;
              "
            >
              {{ networkLabel(card.cardNetwork) }}
            </span>
            <div style="display: flex; align-items: center; gap: 4px">
              <button
                :disabled="card.isDefault || settingDefaultId === card.id"
                :title="t('cards.set_default')"
                style="
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  width: 32px;
                  height: 32px;
                  border: none;
                  border-radius: 9px;
                  background: transparent;
                  cursor: pointer;
                  transition: background 0.15s;
                "
                :style="{ color: card.isDefault ? '#F2B23E' : 'rgba(247,244,237,0.42)' }"
                @click="handleSetDefault(card.id)"
              >
                <svg
                  width="17"
                  height="17"
                  viewBox="0 0 24 24"
                  :fill="card.isDefault ? '#F2B23E' : 'none'"
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
              <button
                :disabled="removingId === card.id"
                :title="t('common.delete')"
                style="
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  width: 32px;
                  height: 32px;
                  border: none;
                  border-radius: 9px;
                  background: transparent;
                  color: rgba(247, 244, 237, 0.42);
                  cursor: pointer;
                  transition:
                    background 0.15s,
                    color 0.15s;
                "
                @click="handleRemove(card.id)"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"></path>
                </svg>
              </button>
            </div>
          </div>

          <!-- PAN -->
          <div style="position: relative">
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: clamp(19px, 2.2vw, 23px);
                font-weight: 600;
                letter-spacing: 0.08em;
                color: #f7f4ed;
              "
            >
              {{ card.maskedPan }}
            </div>
            <div
              style="
                display: flex;
                align-items: baseline;
                justify-content: space-between;
                gap: 16px;
                margin-top: 14px;
              "
            >
              <div
                style="
                  font-size: 13.5px;
                  font-weight: 500;
                  letter-spacing: 0.04em;
                  color: rgba(247, 244, 237, 0.7);
                  text-transform: uppercase;
                "
              >
                {{ card.cardHolderName }}
              </div>
              <div style="flex: none; text-align: right">
                <div
                  style="
                    font-size: 10.5px;
                    font-weight: 700;
                    letter-spacing: 0.1em;
                    text-transform: uppercase;
                    color: rgba(247, 244, 237, 0.4);
                  "
                >
                  {{ t('cards.valid_thru') }}
                </div>
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 14.5px;
                    font-weight: 600;
                    margin-top: 3px;
                    color: #f7f4ed;
                  "
                >
                  {{ expiry(card.expMonth, card.expYear) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Balance -->
          <div
            style="
              position: relative;
              padding-top: 18px;
              border-top: 1px solid rgba(247, 244, 237, 0.12);
            "
          >
            <div
              style="
                font-size: 11.5px;
                font-weight: 700;
                letter-spacing: 0.14em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
              "
            >
              {{ t('cards.balance') }}
            </div>
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 26px;
                font-weight: 600;
                letter-spacing: -0.02em;
                margin-top: 7px;
                color: #f7f4ed;
              "
            >
              {{
                card.balanceUzs != null
                  ? Number(card.balanceUzs).toLocaleString('uz-UZ') + ' UZS'
                  : '— UZS'
              }}
            </div>
          </div>

          <!-- Card action bar -->
          <div
            style="
              position: relative;
              display: flex;
              align-items: center;
              gap: 6px;
              flex-wrap: wrap;
              padding-top: 16px;
              border-top: 1px solid rgba(247, 244, 237, 0.08);
            "
          >
            <!-- Statement -->
            <button class="pp-card-action-btn" @click="openStatement(card.id)">
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
              {{ t('cards.statement') }}
            </button>

            <!-- Limits (HUMO only) -->
            <button
              v-if="card.cardNetwork === 'humo'"
              class="pp-card-action-btn"
              @click="openLimits(card.id, card.cardNetwork)"
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
              {{ t('cards.limits') }}
            </button>

            <!-- PIN change (HUMO or UzCard only) -->
            <button
              v-if="card.cardNetwork === 'humo' || card.cardNetwork === 'uzcard'"
              class="pp-card-action-btn"
              @click="openPin(card.id, card.cardNetwork)"
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              {{ t('cards.pin_change') }}
            </button>

            <!-- Block / Unblock (only for VERIFIED or INACTIVE cards) -->
            <button
              v-if="card.status === 'VERIFIED' || card.status === 'INACTIVE'"
              class="pp-card-action-btn"
              :class="{ 'pp-card-action-btn--danger': card.status === 'VERIFIED' }"
              :disabled="blockingId === card.id"
              @click="handleToggleBlock(card.id, card.status)"
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle v-if="card.status === 'VERIFIED'" cx="12" cy="12" r="10"></circle>
                <line
                  v-if="card.status === 'VERIFIED'"
                  x1="4.93"
                  y1="4.93"
                  x2="19.07"
                  y2="19.07"
                ></line>
                <path v-else d="M18 8h1a4 4 0 0 1 0 8h-1"></path>
                <path
                  v-if="card.status === 'INACTIVE'"
                  d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"
                ></path>
                <line v-if="card.status === 'INACTIVE'" x1="6" y1="1" x2="6" y2="4"></line>
                <line v-if="card.status === 'INACTIVE'" x1="10" y1="1" x2="10" y2="4"></line>
              </svg>
              {{ card.status === 'INACTIVE' ? t('cards.unblock') : t('cards.block') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Statement drawer ──────────────────────────────────────────────────── -->
    <div v-if="statementCardId" class="pp-drawer-overlay" @click.self="closeStatement">
      <div class="pp-drawer">
        <div class="pp-drawer-header">
          <span class="pp-drawer-title">{{ t('cards.statement') }}</span>
          <button class="pp-drawer-close" @click="closeStatement">
            <svg
              width="16"
              height="16"
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

        <div v-if="statementLoading" class="pp-drawer-empty">
          <div class="pp-spinner"></div>
          <span>{{ t('cards.statement_loading') }}</span>
        </div>
        <div v-else-if="statementError" class="pp-drawer-error">{{ statementError }}</div>
        <div v-else-if="statementEntries.length === 0" class="pp-drawer-empty">
          {{ t('cards.statement_empty') }}
        </div>
        <div v-else class="pp-statement-list">
          <div v-for="(entry, i) in statementEntries" :key="i" class="pp-statement-row">
            <div style="flex: 1; min-width: 0">
              <div style="font-size: 13.5px; font-weight: 600; color: #f7f4ed">
                {{ entry.description }}
              </div>
              <div style="font-size: 12px; color: rgba(247, 244, 237, 0.5); margin-top: 3px">
                {{ new Date(entry.date).toLocaleDateString() }}
              </div>
            </div>
            <div
              style="
                flex: none;
                font-family: 'Space Grotesk', sans-serif;
                font-size: 14px;
                font-weight: 600;
              "
              :style="{ color: entry.amountUzs >= 0 ? '#29be8c' : '#ff9c82' }"
            >
              {{ entry.amountUzs >= 0 ? '+' : ''
              }}{{ Number(entry.amountUzs).toLocaleString('uz-UZ') }} UZS
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Limits drawer ─────────────────────────────────────────────────────── -->
    <div v-if="limitsCardId" class="pp-drawer-overlay" @click.self="closeLimits">
      <div class="pp-drawer">
        <div class="pp-drawer-header">
          <span class="pp-drawer-title">{{ t('cards.limits') }}</span>
          <button class="pp-drawer-close" @click="closeLimits">
            <svg
              width="16"
              height="16"
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

        <div v-if="limitsLoading" class="pp-drawer-empty">
          <div class="pp-spinner"></div>
          <span>{{ t('cards.limits_loading') }}</span>
        </div>
        <div v-else>
          <div v-if="limitsError" class="pp-drawer-error" style="margin-bottom: 16px">
            {{ limitsError }}
          </div>

          <!-- Current limits -->
          <div v-if="cardLimits.length > 0" class="pp-limits-list">
            <div v-for="limit in cardLimits" :key="limit.type" class="pp-limit-row">
              <div style="flex: 1; min-width: 0">
                <div style="font-size: 13px; font-weight: 600; color: #f7f4ed">
                  {{ limit.name }}
                </div>
                <div style="font-size: 12px; color: rgba(247, 244, 237, 0.5); margin-top: 2px">
                  {{ limit.from }} – {{ limit.to }}
                </div>
              </div>
              <div
                style="
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 13.5px;
                  font-weight: 600;
                  color: #f7f4ed;
                  margin-right: 10px;
                "
              >
                {{ Number(limit.valueUzs).toLocaleString('uz-UZ') }} UZS
              </div>
              <button
                class="pp-card-action-btn pp-card-action-btn--danger"
                style="padding: 5px 10px; font-size: 11.5px"
                :disabled="removingLimitType === limit.type"
                @click="handleRemoveLimit(limit.type)"
              >
                {{ t('cards.remove_limit') }}
              </button>
            </div>
          </div>
          <div v-else class="pp-drawer-empty" style="padding: 20px 0">
            {{ t('cards.limits_empty') }}
          </div>

          <!-- Set limit form -->
          <div class="pp-set-limit-form">
            <div
              style="
                font-size: 13px;
                font-weight: 700;
                color: rgba(247, 244, 237, 0.7);
                margin-bottom: 12px;
              "
            >
              {{ t('cards.set_limit') }}
            </div>
            <select
              v-model="setLimitType"
              class="pp-input"
              style="height: 46px; margin-bottom: 10px"
            >
              <option value="" disabled>{{ t('cards.limits') }}</option>
              <option v-for="lt in limitTypes" :key="lt.code" :value="lt.code">
                {{ lt.name }}
              </option>
            </select>
            <input
              v-model="setLimitValue"
              type="number"
              :placeholder="t('cards.limit_value_uzs')"
              class="pp-input"
              style="margin-bottom: 10px"
            />
            <template v-if="selectedLimitRequiresDate()">
              <div style="display: flex; gap: 8px; margin-bottom: 10px">
                <input
                  v-model="setLimitFrom"
                  type="date"
                  class="pp-input"
                  style="flex: 1"
                  :placeholder="t('cards.limit_from')"
                />
                <input
                  v-model="setLimitTo"
                  type="date"
                  class="pp-input"
                  style="flex: 1"
                  :placeholder="t('cards.limit_to')"
                />
              </div>
            </template>
            <button
              class="pp-btn-primary"
              style="width: 100%; padding: 13px"
              :disabled="savingLimit || !setLimitType || !setLimitValue"
              @click="handleSetLimit"
            >
              {{ savingLimit ? t('common.saving') : t('cards.set_limit') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── PIN modal ─────────────────────────────────────────────────────────── -->
    <div v-if="pinCardId" class="pp-modal-overlay" @click.self="closePin">
      <div class="pp-modal">
        <div class="pp-drawer-header">
          <span class="pp-drawer-title">{{ t('cards.pin_change') }}</span>
          <button class="pp-drawer-close" @click="closePin">
            <svg
              width="16"
              height="16"
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

        <div v-if="pinSuccess" style="text-align: center; padding: 24px 0">
          <div style="font-size: 36px; margin-bottom: 12px">✓</div>
          <div style="font-size: 15px; font-weight: 600; color: #29be8c">
            {{ t('cards.pin_changed') }}
          </div>
          <button
            class="pp-btn-primary"
            style="margin-top: 20px; padding: 12px 32px"
            @click="closePin"
          >
            {{ t('common.done') }}
          </button>
        </div>
        <div v-else>
          <div v-if="pinError" class="pp-drawer-error" style="margin-bottom: 16px">
            {{ pinError }}
          </div>

          <div style="margin-bottom: 14px">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
                margin-bottom: 8px;
              "
            >
              {{ t('cards.pin_new') }}
            </label>
            <input
              v-model="pinNew"
              type="password"
              inputmode="numeric"
              maxlength="4"
              :placeholder="'• • • •'"
              class="pp-input"
              style="letter-spacing: 0.3em; font-size: 20px; text-align: center"
            />
          </div>
          <div style="margin-bottom: 20px">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
                margin-bottom: 8px;
              "
            >
              {{ t('cards.pin_confirm') }}
            </label>
            <input
              v-model="pinConfirm"
              type="password"
              inputmode="numeric"
              maxlength="4"
              :placeholder="'• • • •'"
              class="pp-input"
              style="letter-spacing: 0.3em; font-size: 20px; text-align: center"
            />
          </div>
          <button
            class="pp-btn-primary"
            style="width: 100%; padding: 13px"
            :disabled="savingPin"
            @click="handlePinChange"
          >
            {{ savingPin ? t('common.saving') : t('cards.pin_change') }}
          </button>
        </div>
      </div>
    </div>
  </q-page>
</template>

<style scoped>
.pp-input {
  width: 100%;
  margin-top: 8px;
  height: 54px;
  padding: 0 16px;
  background: rgba(14, 33, 28, 0.55);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  font-family: Manrope, sans-serif;
  font-size: 16px;
  font-weight: 500;
  color: #f7f4ed;
  outline: none;
  transition: border-color 0.15s;
}

.pp-input:focus {
  border-color: #29be8c;
}
.pp-input.error {
  border-color: #ff9c82;
}
.pp-input::placeholder {
  color: rgba(247, 244, 237, 0.3);
}

.pp-field-error {
  margin-top: 6px;
  font-size: 12.5px;
  color: #ff9c82;
}

/* ── Card action buttons ─────────────────────────────────────────────── */
.pp-card-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid rgba(247, 244, 237, 0.15);
  border-radius: 8px;
  background: rgba(247, 244, 237, 0.06);
  color: rgba(247, 244, 237, 0.75);
  font-family: Manrope, sans-serif;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
}
.pp-card-action-btn:hover {
  background: rgba(247, 244, 237, 0.12);
  color: #f7f4ed;
}
.pp-card-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pp-card-action-btn--danger {
  border-color: rgba(255, 87, 87, 0.25);
  color: rgba(255, 156, 130, 0.85);
}
.pp-card-action-btn--danger:hover {
  background: rgba(255, 87, 87, 0.1);
  color: #ff9c82;
}

/* ── Drawer overlay ──────────────────────────────────────────────────── */
.pp-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.pp-drawer {
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  overflow-y: auto;
  background: #0e211c;
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 24px 24px 0 0;
  padding: clamp(20px, 3vw, 28px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pp-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pp-drawer-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: #f7f4ed;
}

.pp-drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: rgba(247, 244, 237, 0.5);
  cursor: pointer;
  transition: background 0.15s;
}
.pp-drawer-close:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}

.pp-drawer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 0;
  font-size: 14px;
  color: rgba(247, 244, 237, 0.45);
}

.pp-drawer-error {
  padding: 12px 14px;
  background: rgba(255, 156, 130, 0.1);
  border: 1px solid rgba(255, 156, 130, 0.25);
  border-radius: 10px;
  font-size: 13px;
  color: #ff9c82;
}

/* ── Statement list ──────────────────────────────────────────────────── */
.pp-statement-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.pp-statement-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(247, 244, 237, 0.06);
}

/* ── Limits list ─────────────────────────────────────────────────────── */
.pp-limits-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;
}

.pp-limit-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(247, 244, 237, 0.04);
  border: 1px solid rgba(247, 244, 237, 0.08);
  border-radius: 12px;
}

.pp-set-limit-form {
  border-top: 1px solid rgba(247, 244, 237, 0.08);
  padding-top: 18px;
}

/* ── PIN modal ───────────────────────────────────────────────────────── */
.pp-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.pp-modal {
  width: 100%;
  max-width: 380px;
  background: #0e211c;
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 22px;
  padding: clamp(20px, 4vw, 28px);
  display: flex;
  flex-direction: column;
  gap: 0;
}
</style>
