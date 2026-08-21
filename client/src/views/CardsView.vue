<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useCardsStore } from '@/stores/cards';
import { useAuthStore } from '@/stores/auth';
import { ApiError } from '@/lib/api/client';
import type { AddCardPayload } from '@/lib/api/cards';

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
  if (!panDigits) fieldErrors.value.maskedPan = 'Karta raqami kiritilishi shart.';
  else if (panDigits.length !== 16)
    fieldErrors.value.maskedPan = "Karta raqami 16 ta raqamdan iborat bo'lishi kerak.";
  if (!form.value.cardHolderName.trim())
    fieldErrors.value.cardHolderName = 'Karta egasining ismi kiritilishi shart.';
  if (form.value.expMonth < 1 || form.value.expMonth > 12)
    fieldErrors.value.expMonth = "Amal qilish oyi noto'g'ri.";
  const now = new Date();
  const expired =
    form.value.expYear < now.getFullYear() ||
    (form.value.expYear === now.getFullYear() && form.value.expMonth < now.getMonth() + 1);
  if (expired) fieldErrors.value.expYear = "Karta muddati tugagan ko'rinadi.";
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
    submitError.value =
      err instanceof ApiError ? err.message : "Xatolik yuz berdi. Iltimos, qayta urinib ko'ring.";
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
            Mening kartalarim
          </h1>
          <p
            style="
              font-size: 15px;
              line-height: 1.5;
              color: rgba(247, 244, 237, 0.6);
              margin: 10px 0 0;
            "
          >
            UzCard va HUMO to'lov kartalaringizni boshqaring
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
          Karta qo'shish
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
            Yangi karta qo'shish
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
              >Karta raqami</label
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
              >Karta egasining ismi</label
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
              >Amal qilish muddati</label
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
            {{ store.isLoading ? 'Saqlanmoqda...' : 'Kartani saqlash' }}
          </button>
          <button class="pp-btn-ghost" style="padding: 13px 24px" @click="resetForm">
            Bekor qilish
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
              >HAMYON</span
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
              Mening hamyonim
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
                  Hisob turi
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
              Balans
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
            Hozircha kartalar yo'q
          </p>
          <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 0">
            UzCard yoki HUMO kartasi qo'shing.
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
                title="Asosiy karta"
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
                title="O'chirish"
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
                  Amal qilish
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
              Balans
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
</style>
