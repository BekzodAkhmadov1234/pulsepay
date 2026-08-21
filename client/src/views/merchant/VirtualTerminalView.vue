<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMerchantAuthStore } from '@/stores/merchantAuth';
import { chargeCustomer, searchCustomer } from '@/lib/api/virtualTerminal';
import type { ChargeResponse, CustomerSearchResponse } from '@/lib/api/virtualTerminal';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const merchantAuth = useMerchantAuthStore();

// ── Step state ─────────────────────────────────────────────────────────────
type Step = 'phone' | 'card' | 'amount' | 'done';
const step = ref<Step>('phone');

// Phone lookup
const phone = ref('');
const phoneLoading = ref(false);
const phoneError = ref('');
const recipient = ref<CustomerSearchResponse | null>(null);

// Card selection
const selectedInstrumentId = ref('');
const selectedNetwork = ref('');

// Amount
const amountUzs = ref<number | null>(null);
const chargeLoading = ref(false);
const chargeError = ref('');
const result = ref<ChargeResponse | null>(null);

async function searchPhone() {
  phoneError.value = '';
  phoneLoading.value = true;
  try {
    recipient.value = await searchCustomer(phone.value);
    step.value = 'card';
  } catch (e) {
    phoneError.value = e instanceof ApiError ? e.message : 'Foydalanuvchi topilmadi';
  } finally {
    phoneLoading.value = false;
  }
}

function selectCard(instrumentId: string, network: string) {
  selectedInstrumentId.value = instrumentId;
  selectedNetwork.value = network;
  step.value = 'amount';
}

async function charge() {
  if (!amountUzs.value || amountUzs.value <= 0) return;
  chargeError.value = '';
  chargeLoading.value = true;
  try {
    result.value = await chargeCustomer({
      customerPhone: phone.value,
      customerInstrumentId: selectedInstrumentId.value,
      cardNetwork: selectedNetwork.value,
      amountUzs: amountUzs.value,
      purposeCodeId: 1,
    });
    step.value = 'done';
  } catch (e) {
    chargeError.value = e instanceof ApiError ? e.message : 'Xatolik yuz berdi';
  } finally {
    chargeLoading.value = false;
  }
}

function reset() {
  step.value = 'phone';
  phone.value = '';
  recipient.value = null;
  selectedInstrumentId.value = '';
  selectedNetwork.value = '';
  amountUzs.value = null;
  result.value = null;
  chargeError.value = '';
  phoneError.value = '';
}

function handleLogout() {
  merchantAuth.logout();
  router.push('/merchant/login');
}

function formatUzs(tiyin: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 0 }).format(tiyin / 100);
}
</script>

<template>
  <div class="page">
    <!-- Header -->
    <header class="m-header">
      <div class="m-header-inner">
        <div class="m-logo">
          <div class="m-logo-icon">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#0E211C"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 14h4l2.5-7 3 12 2.5-9 2 4h2"></path>
            </svg>
          </div>
          <span>Pulse<span style="color: #29be8c">Pay</span></span>
        </div>
        <nav class="m-nav">
          <button class="m-nav-btn" @click="router.push('/merchant/dashboard')">
            Boshqaruv paneli
          </button>
          <button class="m-nav-btn active">Terminal</button>
          <button class="m-nav-btn" @click="router.push('/merchant/settlements')">
            Hisob-kitob
          </button>
        </nav>
        <div class="m-header-right">
          <span class="m-merchant-name">{{ merchantAuth.merchant?.email }}</span>
          <button class="m-logout-btn" @click="handleLogout">Chiqish</button>
        </div>
      </div>
    </header>

    <main class="pp-main">
      <div class="terminal-wrap">
        <div class="terminal-head">
          <div class="terminal-icon">
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#29BE8C"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="2" y="5" width="20" height="14" rx="2"></rect>
              <path d="M2 10h20"></path>
            </svg>
          </div>
          <h1 class="terminal-title">Virtual terminal</h1>
        </div>

        <!-- Step indicators -->
        <div class="steps">
          <div
            :class="[
              'step',
              step === 'phone' || step === 'card' || step === 'amount' || step === 'done'
                ? 'done'
                : '',
            ]"
          >
            1
          </div>
          <div class="step-line"></div>
          <div
            :class="['step', step === 'card' || step === 'amount' || step === 'done' ? 'done' : '']"
          >
            2
          </div>
          <div class="step-line"></div>
          <div :class="['step', step === 'amount' || step === 'done' ? 'done' : '']">3</div>
          <div class="step-line"></div>
          <div :class="['step', step === 'done' ? 'done' : '']">4</div>
        </div>

        <!-- Step 1: Phone -->
        <div v-if="step === 'phone'" class="terminal-card">
          <h2 class="step-title">Mijoz telefon raqami</h2>
          <p class="step-sub">PulsePay foydalanuvchisining telefon raqamini kiriting</p>
          <div style="display: flex; gap: 10px; margin-top: 20px">
            <input
              v-model="phone"
              type="tel"
              placeholder="+998901234567"
              class="t-input"
              @keyup.enter="searchPhone"
            />
            <button class="pp-btn-primary" :disabled="!phone || phoneLoading" @click="searchPhone">
              {{ phoneLoading ? '...' : 'Qidirish' }}
            </button>
          </div>
          <div v-if="phoneError" class="err-msg">{{ phoneError }}</div>
        </div>

        <!-- Step 2: Card selection -->
        <div v-else-if="step === 'card'" class="terminal-card">
          <h2 class="step-title">Karta tanlash</h2>
          <p class="step-sub">
            Mijoz: <strong>{{ recipient?.fullName ?? phone }}</strong>
          </p>
          <div class="card-list">
            <button
              v-for="card in recipient?.cards ?? []"
              :key="card.instrumentId"
              class="card-option"
              @click="selectCard(card.instrumentId, card.cardNetwork)"
            >
              <div class="card-pan">{{ card.maskedPan }}</div>
              <div class="card-net">{{ card.cardNetwork }}</div>
            </button>
            <div
              v-if="!recipient?.cards?.length"
              style="color: rgba(247, 244, 237, 0.45); padding: 20px 0"
            >
              Faol karta topilmadi
            </div>
          </div>
          <button class="pp-btn-ghost" style="margin-top: 16px" @click="step = 'phone'">
            ← Orqaga
          </button>
        </div>

        <!-- Step 3: Amount -->
        <div v-else-if="step === 'amount'" class="terminal-card">
          <h2 class="step-title">To'lov summasi</h2>
          <p class="step-sub">
            Karta:
            <strong>{{
              recipient?.cards?.find((c) => c.instrumentId === selectedInstrumentId)?.maskedPan ??
              '—'
            }}</strong>
          </p>
          <div style="margin-top: 20px">
            <label class="t-label">Summa (UZS)</label>
            <input
              v-model.number="amountUzs"
              type="number"
              min="1"
              placeholder="0"
              class="t-input t-amount"
              @keyup.enter="charge"
            />
          </div>
          <div v-if="chargeError" class="err-msg">{{ chargeError }}</div>
          <div style="display: flex; gap: 10px; margin-top: 20px">
            <button class="pp-btn-ghost" @click="step = 'card'">← Orqaga</button>
            <button
              class="pp-btn-primary"
              :disabled="!amountUzs || chargeLoading"
              style="flex: 1"
              @click="charge"
            >
              {{ chargeLoading ? '...' : "To'lovni amalga oshirish" }}
            </button>
          </div>
        </div>

        <!-- Step 4: Done -->
        <div v-else-if="step === 'done' && result" class="terminal-card success-card">
          <div class="success-icon">
            <svg
              width="28"
              height="28"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#29BE8C"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
          <h2 class="step-title" style="color: #6fd8a8">To'lov muvaffaqiyatli!</h2>
          <div class="result-rows">
            <div class="result-row">
              <span class="result-label">Tranzaksiya ID</span>
              <span class="result-val" style="font-size: 12px; word-break: break-all">{{
                result.transferId
              }}</span>
            </div>
            <div class="result-row">
              <span class="result-label">Summa</span>
              <span class="result-val">{{ formatUzs(result.amountTiyin) }} UZS</span>
            </div>
            <div class="result-row">
              <span class="result-label">Komissiya</span>
              <span class="result-val">{{ formatUzs(result.feeTiyin) }} UZS</span>
            </div>
            <div class="result-row">
              <span class="result-label">Status</span>
              <span class="result-val" style="color: #6fd8a8">Bajarildi</span>
            </div>
          </div>
          <button class="pp-btn-primary" style="width: 100%; margin-top: 20px" @click="reset">
            Yangi to'lov
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #0e211c;
  color: #f7f4ed;
  font-family: Manrope, system-ui, sans-serif;
}
.m-header {
  background: rgba(14, 33, 28, 0.95);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}
.m-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px clamp(20px, 4vw, 48px);
}
.m-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}
.m-logo-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #29be8c;
  display: flex;
  align-items: center;
  justify-content: center;
}
.m-nav {
  flex: 1;
  display: flex;
  gap: 4px;
}
.m-nav-btn {
  border: none;
  background: transparent;
  color: rgba(247, 244, 237, 0.55);
  font-family: Manrope, sans-serif;
  font-size: 14px;
  font-weight: 600;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.m-nav-btn:hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}
.m-nav-btn.active {
  background: rgba(247, 244, 237, 0.1);
  color: #f7f4ed;
}
.m-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.m-merchant-name {
  font-size: 14px;
  font-weight: 600;
  color: #f7f4ed;
}
.m-logout-btn {
  background: none;
  border: 1px solid rgba(247, 244, 237, 0.16);
  border-radius: 999px;
  padding: 7px 14px;
  font-family: Manrope, sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.65);
  cursor: pointer;
}
.m-logout-btn:hover {
  background: rgba(247, 244, 237, 0.07);
}

.terminal-wrap {
  max-width: 520px;
  margin: 0 auto;
}
.terminal-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}
.terminal-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(41, 190, 140, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.terminal-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(20px, 3vw, 28px);
  font-weight: 600;
  letter-spacing: -0.02em;
  margin: 0;
}

.steps {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 28px;
}
.step {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(247, 244, 237, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: rgba(247, 244, 237, 0.4);
  flex-shrink: 0;
  transition:
    background 0.2s,
    border-color 0.2s,
    color 0.2s;
}
.step.done {
  border-color: #29be8c;
  background: rgba(41, 190, 140, 0.15);
  color: #6fd8a8;
}
.step-line {
  flex: 1;
  height: 2px;
  background: rgba(247, 244, 237, 0.1);
}

.terminal-card {
  background: rgba(247, 244, 237, 0.04);
  border: 1px solid rgba(247, 244, 237, 0.09);
  border-radius: 20px;
  padding: clamp(22px, 3vw, 30px);
}
.success-card {
  border-color: rgba(41, 190, 140, 0.25);
  text-align: center;
}
.success-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(41, 190, 140, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.step-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px;
}
.step-sub {
  font-size: 14px;
  color: rgba(247, 244, 237, 0.55);
  margin: 0;
}

.t-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.55);
  margin-bottom: 8px;
}
.t-input {
  flex: 1;
  height: 50px;
  padding: 0 16px;
  background: rgba(14, 33, 28, 0.6);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  font-family: Manrope, sans-serif;
  font-size: 15px;
  color: #f7f4ed;
  box-sizing: border-box;
}
.t-input:focus {
  outline: none;
  border-color: #29be8c;
}
.t-input::placeholder {
  color: rgba(247, 244, 237, 0.3);
}
.t-amount {
  width: 100%;
  font-size: 22px;
  font-weight: 600;
  height: 56px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}
.card-option {
  background: rgba(247, 244, 237, 0.05);
  border: 1px solid rgba(247, 244, 237, 0.12);
  border-radius: 12px;
  padding: 14px 18px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition:
    border-color 0.15s,
    background 0.15s;
  text-align: left;
}
.card-option:hover {
  border-color: #29be8c;
  background: rgba(41, 190, 140, 0.06);
}
.card-pan {
  font-weight: 600;
  font-size: 15px;
  color: #f7f4ed;
}
.card-net {
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.5);
  text-transform: uppercase;
}

.result-rows {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}
.result-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
}
.result-label {
  color: rgba(247, 244, 237, 0.5);
}
.result-val {
  font-weight: 600;
}

.err-msg {
  color: #ff9c82;
  font-size: 13px;
  margin-top: 10px;
}
</style>
