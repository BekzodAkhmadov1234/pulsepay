<script setup lang="ts">
import { ref, computed } from 'vue';
import { RouterLink, useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const phoneDigits = ref('');
const phone = computed(() => '+998' + phoneDigits.value);
const phoneError = ref('');
const serverError = ref('');

const PHONE_RE = /^\+998\d{9}$/;

const features = [
  'Instant P2P transfers across Uzbekistan',
  'Merchant payments via UzCard & Humo',
  'Bank-grade security, CBU licensed',
];

function validatePhone(): boolean {
  phoneError.value = '';
  if (!phoneDigits.value) {
    phoneError.value = 'Phone number is required.';
    return false;
  }
  if (!PHONE_RE.test(phone.value)) {
    phoneError.value = 'Enter a valid Uzbekistan number (+998XXXXXXXXX).';
    return false;
  }
  return true;
}

async function handleLogin() {
  serverError.value = '';
  if (!validatePhone()) return;

  try {
    await auth.login({ phoneE164: phone.value });
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
    await router.push(redirect);
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 404) {
        phoneError.value = 'No account found for this number.';
      } else if (err.status === 403) {
        serverError.value = 'Account is inactive. Please contact support.';
      } else {
        serverError.value = err.message;
      }
    } else {
      serverError.value = 'Something went wrong. Please try again.';
    }
  }
}

function onPhoneInput(val: string | number | null) {
  phoneDigits.value = String(val ?? '')
    .replace(/\D/g, '')
    .slice(0, 9);
}
</script>

<template>
  <q-page class="row no-wrap">
    <!-- ── Brand panel (lg+) ─────────────────────────────────── -->
    <aside class="brand-panel gt-md col-5 column justify-between q-pa-xl">
      <RouterLink to="/" class="brand-logo text-weight-black">
        Pulse<span class="brand-accent">Pay</span>
      </RouterLink>

      <div class="column q-gutter-y-lg">
        <div class="column q-gutter-y-sm">
          <p class="brand-tagline">Uzbekistan's payment platform</p>
          <h1 class="text-h4 text-weight-bold text-white q-my-none">
            Send money<br />anywhere,<br />instantly.
          </h1>
          <p class="text-body1 text-white q-my-none" style="opacity: 0.72">
            P2P · C2B · B2B/B2C — all payment rails in one account.
          </p>
        </div>

        <q-list dense class="q-pa-none">
          <q-item v-for="f in features" :key="f" class="q-px-none q-py-xs" style="min-height: 0">
            <q-item-section avatar style="min-width: 28px">
              <q-icon name="check_circle" size="18px" class="feature-icon" />
            </q-item-section>
            <q-item-section class="text-white text-body2">{{ f }}</q-item-section>
          </q-item>
        </q-list>
      </div>

      <p class="brand-copyright q-mb-none">© 2026 PulsePay · CBU Licensed</p>
    </aside>

    <!-- ── Form panel ────────────────────────────────────────── -->
    <main class="col column items-center justify-center bg-white q-pa-xl">
      <!-- Mobile logo -->
      <div class="lt-lg q-mb-xl text-h5 text-weight-black text-dark">
        Pulse<span class="text-primary">Pay</span>
      </div>

      <div class="form-inner">
        <div class="q-mb-lg">
          <h2 class="text-h5 text-weight-bold text-dark q-my-none">Welcome back</h2>
          <p class="text-body2 text-grey-6 q-mt-xs q-mb-none">
            Enter your registered phone number to log in.
          </p>
        </div>

        <q-form class="column q-gutter-y-md" @submit.prevent="handleLogin">
          <q-banner v-if="serverError" dense rounded class="bg-red-1 text-negative">
            <template #avatar>
              <q-icon name="warning" color="negative" />
            </template>
            {{ serverError }}
          </q-banner>

          <q-input
            :model-value="phoneDigits"
            type="tel"
            inputmode="tel"
            label="Mobile number"
            outlined
            maxlength="9"
            :error="!!phoneError"
            :error-message="phoneError"
            :disable="auth.isLoading"
            @update:model-value="onPhoneInput"
          >
            <template #prepend>
              <span class="text-body2 text-grey-7">+998</span>
            </template>
          </q-input>

          <q-btn
            type="submit"
            label="Log in"
            color="primary"
            unelevated
            class="full-width"
            size="md"
            padding="12px"
            :loading="auth.isLoading"
          />
        </q-form>

        <!-- Footer -->
        <p class="text-center text-body2 text-grey-6 q-mt-xl q-mb-none">
          Don't have an account?
          <RouterLink to="/register" class="text-primary text-weight-semibold">
            Create one
          </RouterLink>
        </p>
      </div>
    </main>
  </q-page>
</template>

<style scoped>
/* ── Brand panel ─────────────────────────────────────────────── */
.brand-panel {
  background: linear-gradient(150deg, oklch(0.259 0.111 277) 0%, oklch(0.354 0.161 277) 100%);
  position: relative;
  overflow: hidden;
}

.brand-panel::before,
.brand-panel::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.brand-panel::before {
  width: 400px;
  height: 400px;
  background: oklch(0.421 0.199 277 / 0.25);
  filter: blur(80px);
  top: -120px;
  left: -120px;
}

.brand-panel::after {
  width: 320px;
  height: 320px;
  background: oklch(0.496 0.235 277 / 0.2);
  filter: blur(60px);
  bottom: -80px;
  right: -20px;
}

.brand-logo {
  font-size: 1.25rem;
  color: #fff;
  text-decoration: none;
  position: relative;
  z-index: 1;
}

.brand-accent {
  color: oklch(0.702 0.189 277);
}

.brand-tagline {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: oklch(0.702 0.189 277);
  margin: 0;
  position: relative;
  z-index: 1;
}

.brand-copyright {
  font-size: 11px;
  color: oklch(0.421 0.199 277);
  position: relative;
  z-index: 1;
}

.feature-icon {
  color: oklch(0.702 0.189 277);
}

/* Keep list items above the blobs */
.brand-panel .q-list,
.brand-panel h1,
.brand-panel p {
  position: relative;
  z-index: 1;
}

/* ── Form panel ──────────────────────────────────────────────── */
.form-inner {
  width: 100%;
  max-width: 360px;
}
</style>
