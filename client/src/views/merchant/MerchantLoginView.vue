<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useMerchantAuthStore } from '@/stores/merchantAuth';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const route = useRoute();
const merchantAuth = useMerchantAuthStore();

const email = ref('');
const password = ref('');
const serverError = ref('');
const showPassword = ref(false);

async function handleLogin() {
  serverError.value = '';
  try {
    await merchantAuth.login(email.value, password.value);
    const redirect =
      typeof route.query.redirect === 'string' ? route.query.redirect : '/merchant/dashboard';
    await router.push(redirect);
  } catch (err) {
    if (err instanceof ApiError) {
      serverError.value = err.status === 401 ? "Noto'g'ri email yoki parol." : err.message;
    } else {
      serverError.value = "Xatolik yuz berdi. Iltimos, qayta urinib ko'ring.";
    }
  }
}
</script>

<template>
  <div class="page">
    <div class="radial-glow"></div>
    <div class="grid-overlay"></div>

    <header class="header">
      <div class="logo">
        <div class="logo-icon">
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
        <span class="logo-text">Pulse<span class="logo-accent">Pay</span></span>
        <span class="merchant-badge">Savdogar</span>
      </div>
      <div class="security-pill">
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
        Savdogar portali
      </div>
    </header>

    <main class="main">
      <div class="form-wrapper">
        <div class="form-heading">
          <h1 class="heading">Savdogar portaliga kirish</h1>
          <p class="subheading">Savdogar hisob ma'lumotlarini kiriting</p>
        </div>

        <form class="card" @submit.prevent="handleLogin">
          <label class="field-label">Email</label>
          <input
            v-model="email"
            type="email"
            placeholder="merchant@company.uz"
            class="input"
            autocomplete="email"
            :disabled="merchantAuth.isLoading"
          />

          <label class="field-label mt-20">Parol</label>
          <div class="password-wrap">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="••••••••"
              class="input password-input"
              autocomplete="off"
              :disabled="merchantAuth.isLoading"
            />
            <button type="button" class="reveal-btn" @click="showPassword = !showPassword">
              <svg
                v-if="!showPassword"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg
                v-else
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
                ></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
            </button>
          </div>

          <div v-if="serverError" class="error-msg">{{ serverError }}</div>

          <button type="submit" class="submit-btn" :disabled="merchantAuth.isLoading">
            {{ merchantAuth.isLoading ? '...' : 'Kirish' }}
          </button>
        </form>
      </div>
    </main>

    <footer class="footer">© 2026 PulsePay · Savdogar portali</footer>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: Manrope, system-ui, sans-serif;
  background: #0e211c;
  color: #f7f4ed;
  position: relative;
  overflow: hidden;
}
.radial-glow {
  position: absolute;
  width: 820px;
  height: 820px;
  left: 50%;
  top: -520px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(41, 190, 140, 0.18) 0%, rgba(41, 190, 140, 0) 66%);
  pointer-events: none;
}
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(247, 244, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(247, 244, 237, 0.035) 1px, transparent 1px);
  background-size: 80px 80px;
  pointer-events: none;
}
.header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px clamp(20px, 4vw, 48px);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #29be8c;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.logo-accent {
  color: #29be8c;
}
.merchant-badge {
  margin-left: 4px;
  padding: 4px 9px;
  border-radius: 7px;
  background: rgba(41, 190, 140, 0.15);
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6fd8a8;
}
.security-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.72);
}
.main {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(36px, 6vw, 76px) clamp(20px, 4vw, 48px);
}
.form-wrapper {
  width: 100%;
  max-width: 420px;
}
.form-heading {
  text-align: center;
}
.heading {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(28px, 3.4vw, 38px);
  font-weight: 600;
  letter-spacing: -0.03em;
  margin: 0;
  color: #f7f4ed;
}
.subheading {
  font-size: 14.5px;
  line-height: 1.5;
  color: rgba(247, 244, 237, 0.55);
  margin: 12px 0 0;
}
.card {
  margin-top: 30px;
  background: rgba(247, 244, 237, 0.045);
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 20px;
  padding: clamp(22px, 3vw, 30px);
  backdrop-filter: blur(6px);
}
.field-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.62);
}
.mt-20 {
  margin-top: 20px;
}
.input {
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
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.input::placeholder {
  color: rgba(247, 244, 237, 0.35);
}
.input:focus {
  outline: none;
  border-color: #29be8c;
}
.input:disabled {
  opacity: 0.5;
}
.password-wrap {
  position: relative;
  margin-top: 8px;
}
.password-input {
  margin-top: 0;
  padding-right: 54px;
}
.reveal-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(247, 244, 237, 0.55);
  cursor: pointer;
  transition: background 0.15s;
}
.reveal-btn:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}
.error-msg {
  margin-top: 12px;
  font-size: 13px;
  color: #ff9c82;
}
.submit-btn {
  width: 100%;
  margin-top: 24px;
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
  transition: background 0.15s;
}
.submit-btn:hover:not(:disabled) {
  background: #4fd3a6;
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.footer {
  position: relative;
  padding: 18px clamp(20px, 4vw, 48px);
  border-top: 1px solid rgba(247, 244, 237, 0.08);
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.4);
}
</style>
