<script setup lang="ts">
import { ref, computed, nextTick, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ApiError } from '@/lib/api/client';
import { register as apiRegister, requestOtp, getDevOtp, verifyOtp } from '@/lib/api/auth';
import { setToken } from '@/lib/token';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

// ── Step management ────────────────────────────────────────────────────
const step = ref<'phone' | 'otp'>('phone');

// ── Phone step ─────────────────────────────────────────────────────────
const mode = ref<'login' | 'register'>(route.path.startsWith('/register') ? 'register' : 'login');
const phoneDigits = ref('');
const formattedPhone = computed(() => {
  const d = phoneDigits.value;
  return [d.slice(0, 2), d.slice(2, 5), d.slice(5, 7), d.slice(7, 9)].filter(Boolean).join(' ');
});
const firstName = ref('');
const lastName = ref('');
const agreed = ref(false);

// ── OTP step ───────────────────────────────────────────────────────────
const otpDigits = ref(['', '', '', '', '', '']);
const otpBoxRefs = ref<(HTMLInputElement | null)[]>([]);
const otpAutoFilling = ref(false);

// ── Resend countdown ───────────────────────────────────────────────────
const resendSeconds = ref(0);
let resendTimer: ReturnType<typeof setInterval> | null = null;

function startResendTimer() {
  if (resendTimer) clearInterval(resendTimer);
  resendSeconds.value = 59;
  resendTimer = setInterval(() => {
    resendSeconds.value--;
    if (resendSeconds.value <= 0 && resendTimer) {
      clearInterval(resendTimer);
      resendTimer = null;
    }
  }, 1000);
}

onUnmounted(() => {
  if (resendTimer) clearInterval(resendTimer);
});

// ── Shared UI state ────────────────────────────────────────────────────
const error = ref('');
const isLoading = ref(false);

// ── Computed text ──────────────────────────────────────────────────────
const heading = computed(() => {
  if (step.value === 'otp') return 'Kodni kiriting';
  return mode.value === 'login' ? 'Xush kelibsiz' : 'Hisobingizni yarating';
});
const subheading = computed(() => {
  if (step.value === 'otp') return 'Telefoningizga yuborilgan 6 xonali kodni kiriting.';
  if (mode.value === 'register') return '';
  return 'Telefon raqamingiz orqali hisobingizga kiring.';
});

// ── Mode switcher ──────────────────────────────────────────────────────
function switchMode(m: 'login' | 'register') {
  if (step.value === 'otp') return;
  mode.value = m;
  error.value = '';
  router.replace({ path: m === 'login' ? '/login' : '/register' });
}

// ── Phone input ────────────────────────────────────────────────────────
function onPhoneInput(e: Event) {
  phoneDigits.value = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 9);
  error.value = '';
}

// ── OTP box handlers ───────────────────────────────────────────────────
function setOtpRef(el: unknown, index: number) {
  otpBoxRefs.value[index] = el as HTMLInputElement | null;
}

function onOtpInput(index: number, e: Event) {
  const val = (e.target as HTMLInputElement).value.replace(/\D/g, '');
  otpDigits.value[index] = val.slice(-1);
  error.value = '';
  if (val && index < 5) {
    nextTick(() => otpBoxRefs.value[index + 1]?.focus());
  }
  if (otpDigits.value.every((d) => d !== '') && !otpAutoFilling.value) {
    submitOtp();
  }
}

function onOtpKeydown(index: number, e: KeyboardEvent) {
  if (e.key === 'Backspace' && !otpDigits.value[index] && index > 0) {
    otpDigits.value[index - 1] = '';
    nextTick(() => otpBoxRefs.value[index - 1]?.focus());
  }
}

function onOtpPaste(e: ClipboardEvent) {
  const text = e.clipboardData?.getData('text') ?? '';
  const digits = text.replace(/\D/g, '').slice(0, 6);
  if (!digits) return;
  e.preventDefault();
  otpAutoFilling.value = true;
  digits.split('').forEach((d, i) => {
    otpDigits.value[i] = d;
  });
  otpAutoFilling.value = false;
  if (digits.length === 6) submitOtp();
}

// ── Go back to phone step ──────────────────────────────────────────────
function goBack() {
  step.value = 'phone';
  otpDigits.value = ['', '', '', '', '', ''];
  error.value = '';
  isLoading.value = false;
  if (resendTimer) {
    clearInterval(resendTimer);
    resendTimer = null;
  }
  resendSeconds.value = 0;
}

// ── Auto-fetch OTP code from dev endpoint ──────────────────────────────
async function fetchAndFillOtp() {
  const phoneE164 = '+998' + phoneDigits.value;
  await new Promise((r) => setTimeout(r, 700));
  try {
    const { code } = await getDevOtp(phoneE164);
    otpAutoFilling.value = true;
    code.split('').forEach((d, i) => {
      otpDigits.value[i] = d;
    });
    otpAutoFilling.value = false;
    await nextTick();
    await submitOtp();
  } catch {
    // Dev OTP not available — user enters manually
  }
}

// ── Submit: phone step ─────────────────────────────────────────────────
async function submitPhone() {
  error.value = '';

  if (phoneDigits.value.length < 9) {
    error.value = "Telefon raqamini to'liq kiriting (9 raqam).";
    return;
  }
  if (mode.value === 'register') {
    if (firstName.value.trim().length < 2) {
      error.value = "Ismni to'liq kiriting (kamida 2 belgi).";
      return;
    }
    if (lastName.value.trim().length < 2) {
      error.value = "Familiyani to'liq kiriting (kamida 2 belgi).";
      return;
    }
    if (!agreed.value) {
      error.value = 'Davom etish uchun shartlarga rozilik bildiring.';
      return;
    }
  }

  isLoading.value = true;
  const phoneE164 = '+998' + phoneDigits.value;

  try {
    if (mode.value === 'register') {
      await apiRegister({
        phoneE164,
        fullName: `${firstName.value.trim()} ${lastName.value.trim()}`,
      });
    }
    await requestOtp(phoneE164);
    step.value = 'otp';
    startResendTimer();
    nextTick(() => otpBoxRefs.value[0]?.focus());
    fetchAndFillOtp(); // fire & forget — auto-fills boxes and submits
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 409) {
        error.value = "Bu telefon raqami allaqachon ro'yxatdan o'tgan.";
      } else if (err.status === 404) {
        error.value = "Bu raqam ro'yxatdan o'tmagan. Avval ro'yxatdan o'ting.";
      } else {
        error.value = err.message;
      }
    } else {
      error.value = "Xatolik yuz berdi. Iltimos, qayta urinib ko'ring.";
    }
  } finally {
    isLoading.value = false;
  }
}

// ── Submit: OTP step ───────────────────────────────────────────────────
async function submitOtp() {
  const code = otpDigits.value.join('');
  if (code.length < 6) return;

  error.value = '';
  isLoading.value = true;
  const phoneE164 = '+998' + phoneDigits.value;

  try {
    const res = await verifyOtp(phoneE164, code);
    setToken(res.accessToken);
    auth.fetchCurrentUser();
    const redirect = route.query.redirect as string | undefined;
    router.push(redirect || '/');
  } catch (err) {
    otpDigits.value = ['', '', '', '', '', ''];
    nextTick(() => otpBoxRefs.value[0]?.focus());
    if (err instanceof ApiError) {
      error.value = err.status === 400 ? "Kod noto'g'ri yoki muddati o'tgan." : err.message;
    } else {
      error.value = "Xatolik yuz berdi. Iltimos, qayta urinib ko'ring.";
    }
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="auth-root">
    <div class="auth-glow"></div>
    <div class="auth-grid"></div>

    <!-- Header -->
    <header class="auth-header">
      <div class="auth-logo">
        <div class="auth-logo-icon">
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
      <div class="auth-badge">
        <svg
          width="13"
          height="13"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#29BE8C"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 2 4 6v6c0 5 3.4 8.6 8 10 4.6-1.4 8-5 8-10V6z"></path>
        </svg>
        MBU litsenziyalangan
      </div>
    </header>

    <!-- Main -->
    <main class="auth-main">
      <div class="auth-container">
        <!-- Heading block -->
        <div style="text-align: center">
          <h1 class="auth-heading">{{ heading }}</h1>
          <p v-if="subheading" class="auth-subheading">{{ subheading }}</p>
        </div>

        <!-- Form card -->
        <div class="auth-card">
          <!-- ── PHONE STEP ─────────────────────────────────────── -->
          <template v-if="step === 'phone'">
            <!-- Tab switcher -->
            <div class="auth-tab-bar">
              <button
                class="auth-tab"
                :class="{ active: mode === 'login' }"
                @click="switchMode('login')"
              >
                Kirish
              </button>
              <button
                class="auth-tab"
                :class="{ active: mode === 'register' }"
                @click="switchMode('register')"
              >
                Ro'yxatdan o'tish
              </button>
            </div>

            <!-- Register: name fields -->
            <div v-if="mode === 'register'" class="auth-name-grid">
              <div>
                <label class="auth-label">Ism</label>
                <input
                  v-model="firstName"
                  type="text"
                  placeholder="Aziz"
                  class="auth-input"
                  autocomplete="given-name"
                  @input="error = ''"
                />
              </div>
              <div>
                <label class="auth-label">Familiya</label>
                <input
                  v-model="lastName"
                  type="text"
                  placeholder="Karimov"
                  class="auth-input"
                  autocomplete="family-name"
                  @input="error = ''"
                />
              </div>
            </div>

            <!-- Phone input -->
            <label class="auth-label" style="display: block; margin-top: 20px"
              >Telefon raqami</label
            >
            <div class="auth-phone-wrap">
              <span class="auth-phone-prefix">+998</span>
              <span class="auth-phone-sep"></span>
              <input
                type="tel"
                inputmode="numeric"
                placeholder="90 123 45 67"
                :value="formattedPhone"
                class="auth-phone-input"
                autocomplete="tel"
                @input="onPhoneInput"
                @keydown.enter="submitPhone"
              />
            </div>

            <!-- Terms checkbox (register only) -->
            <label v-if="mode === 'register'" class="auth-checkbox-label">
              <input
                v-model="agreed"
                type="checkbox"
                style="
                  width: 17px;
                  height: 17px;
                  accent-color: #29be8c;
                  flex: none;
                  margin: 2px 0 0;
                "
              />
              <span>Ommaviy oferta va maxfiylik siyosatiga roziman.</span>
            </label>

            <!-- Error -->
            <div v-if="error" class="auth-error">{{ error }}</div>

            <!-- Submit -->
            <button class="auth-submit" :disabled="isLoading" @click="submitPhone">
              <span v-if="isLoading" class="auth-spinner-inline"></span>
              <span v-else>{{ mode === 'login' ? 'KIRISH' : 'HISOB YARATISH' }}</span>
            </button>

            <div class="auth-hint">SMS orqali tasdiqlash kodi yuboriladi</div>
          </template>

          <!-- ── OTP STEP ───────────────────────────────────────── -->
          <template v-else>
            <!-- Back button -->
            <button class="auth-back" @click="goBack">
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
                <path d="M19 12H5m0 0 6-6m-6 6 6 6" />
              </svg>
              Raqamni o'zgartirish
            </button>

            <!-- Phone display -->
            <div class="auth-otp-sent">
              Kod yuborildi: <strong>+998 {{ formattedPhone }}</strong>
            </div>

            <!-- OTP boxes -->
            <div class="auth-otp-boxes">
              <input
                v-for="i in 6"
                :key="i"
                :ref="(el) => setOtpRef(el, i - 1)"
                type="tel"
                inputmode="numeric"
                maxlength="1"
                :value="otpDigits[i - 1]"
                class="auth-otp-box"
                :class="{ filled: otpDigits[i - 1] }"
                autocomplete="one-time-code"
                @input="onOtpInput(i - 1, $event)"
                @keydown="onOtpKeydown(i - 1, $event)"
                @paste="onOtpPaste"
              />
            </div>

            <!-- Error -->
            <div v-if="error" class="auth-error" style="text-align: center">{{ error }}</div>

            <!-- Submit -->
            <button
              class="auth-submit"
              :disabled="isLoading || otpDigits.join('').length < 6"
              style="margin-top: 24px"
              @click="submitOtp"
            >
              <span v-if="isLoading" class="auth-spinner-inline"></span>
              <span v-else>TASDIQLASH</span>
            </button>

            <div class="auth-hint">
              <template v-if="resendSeconds > 0">
                Kodni qayta yuborish — 00:{{ String(resendSeconds).padStart(2, '0') }}
              </template>
              <template v-else>
                Kod kelmadimi?
                <span style="color: #29be8c; cursor: pointer" @click="fetchAndFillOtp"
                  >Qayta yuborish</span
                >
              </template>
            </div>
          </template>
        </div>
      </div>
    </main>

    <footer class="auth-footer">© 2026 PulsePay · MBU litsenziyalangan</footer>
  </div>
</template>

<style scoped>
.auth-root {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: Manrope, system-ui, sans-serif;
  background: #0e211c;
  color: #f7f4ed;
  overflow: hidden;
}

.auth-glow {
  position: absolute;
  width: 900px;
  height: 900px;
  left: 50%;
  top: -560px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(41, 190, 140, 0.2) 0%, rgba(41, 190, 140, 0) 65%);
  pointer-events: none;
}

.auth-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(247, 244, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(247, 244, 237, 0.035) 1px, transparent 1px);
  background-size: 80px 80px;
  pointer-events: none;
}

/* Header */
.auth-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px clamp(20px, 4vw, 48px);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: #f7f4ed;
  text-decoration: none;
}

.auth-logo-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #29be8c;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.auth-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: rgba(247, 244, 237, 0.72);
}

/* Main */
.auth-main {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(36px, 6vw, 76px) clamp(20px, 4vw, 48px);
}

.auth-container {
  width: 100%;
  max-width: 468px;
  display: flex;
  flex-direction: column;
}

/* Heading */
.auth-heading {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.06;
  font-weight: 600;
  letter-spacing: -0.03em;
  margin: 0;
  color: #f7f4ed;
}

.auth-subheading {
  font-size: 15px;
  line-height: 1.55;
  color: rgba(247, 244, 237, 0.6);
  margin: 14px auto 0;
  max-width: 390px;
  text-wrap: pretty;
}

/* Form card */
.auth-card {
  margin-top: 34px;
  background: rgba(247, 244, 237, 0.045);
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 20px;
  padding: clamp(24px, 3vw, 34px);
  backdrop-filter: blur(6px);
}

/* Tab switcher */
.auth-tab-bar {
  display: flex;
  background: rgba(14, 33, 28, 0.6);
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 999px;
  padding: 4px;
  gap: 4px;
}

.auth-tab {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: rgba(247, 244, 237, 0.6);
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}

.auth-tab.active {
  background: #f7f4ed;
  color: #0e211c;
}
.auth-tab:not(.active):hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}

/* Name grid */
.auth-name-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 22px;
}

/* Label */
.auth-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.62);
}

/* Text input */
.auth-input {
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
  box-sizing: border-box;
}

.auth-input:focus {
  border-color: #29be8c;
}
.auth-input::placeholder {
  color: rgba(247, 244, 237, 0.3);
}

/* Phone input group */
.auth-phone-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  background: rgba(14, 33, 28, 0.55);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  padding: 0 16px;
  height: 54px;
  transition: border-color 0.15s;
}

.auth-phone-wrap:focus-within {
  border-color: #29be8c;
}

.auth-phone-prefix {
  font-size: 16px;
  font-weight: 600;
  color: #f7f4ed;
  white-space: nowrap;
}

.auth-phone-sep {
  width: 1px;
  height: 22px;
  background: rgba(247, 244, 237, 0.16);
  flex-shrink: 0;
}

.auth-phone-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-family: Manrope, sans-serif;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: #f7f4ed;
  outline: none;
  padding: 0;
}

.auth-phone-input::placeholder {
  color: rgba(247, 244, 237, 0.3);
}

/* Checkbox */
.auth-checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 18px;
  font-size: 13px;
  line-height: 1.45;
  color: rgba(247, 244, 237, 0.62);
  cursor: pointer;
}

/* Error */
.auth-error {
  margin-top: 12px;
  font-size: 13px;
  color: #ff9c82;
}

/* Submit button */
.auth-submit {
  width: 100%;
  margin-top: 22px;
  height: 54px;
  border: none;
  border-radius: 12px;
  background: #29be8c;
  color: #0e211c;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  transition:
    background 0.15s,
    transform 0.08s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.auth-submit:hover:not(:disabled) {
  background: #4fd3a6;
}
.auth-submit:active:not(:disabled) {
  transform: translateY(1px);
}
.auth-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* Spinner */
.auth-spinner-inline {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(14, 33, 28, 0.3);
  border-top-color: #0e211c;
  border-radius: 50%;
  animation: pp-spin 0.7s linear infinite;
  display: inline-block;
}

/* Hint below button */
.auth-hint {
  margin-top: 14px;
  text-align: center;
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.42);
}

/* ── OTP step styles ───────────────────────────────────────────────── */

/* Back button */
.auth-back {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: rgba(247, 244, 237, 0.55);
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 24px;
  transition: color 0.15s;
}

.auth-back:hover {
  color: #f7f4ed;
}

/* Phone sent line */
.auth-otp-sent {
  font-size: 14.5px;
  color: rgba(247, 244, 237, 0.62);
  margin-bottom: 4px;
}

.auth-otp-sent strong {
  color: #f7f4ed;
  font-weight: 600;
}

/* OTP boxes */
.auth-otp-boxes {
  display: flex;
  gap: 9px;
  margin-top: 20px;
}

.auth-otp-box {
  flex: 1;
  min-width: 0;
  height: 60px;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  background: rgba(14, 33, 28, 0.55);
  color: #f7f4ed;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  outline: none;
  caret-color: #29be8c;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.auth-otp-box:focus {
  border-color: #29be8c;
  background: rgba(41, 190, 140, 0.07);
}

.auth-otp-box.filled {
  border-color: rgba(247, 244, 237, 0.35);
}

/* Footer */
.auth-footer {
  position: relative;
  padding: 18px clamp(20px, 4vw, 48px);
  border-top: 1px solid rgba(247, 244, 237, 0.08);
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.4);
}
</style>
