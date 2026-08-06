<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const auth = useAuthStore();

// Phone stored as 9 digits only; combined with prefix on submit
const phoneDigits = ref('');
const phone = computed(() => '+998' + phoneDigits.value);

const form = reactive({ firstName: '', lastName: '' });
const errors = reactive({ phoneE164: '', firstName: '', lastName: '' });
const serverError = ref('');

const PHONE_RE = /^\+998\d{9}$/;

const features = [
  'Instant P2P transfers across Uzbekistan',
  'Merchant payments via UzCard & Humo',
  'Bank-grade security, CBU licensed',
];

function validate(): boolean {
  errors.phoneE164 = '';
  errors.firstName = '';
  errors.lastName = '';

  if (!phoneDigits.value) {
    errors.phoneE164 = 'Phone number is required.';
  } else if (!PHONE_RE.test(phone.value)) {
    errors.phoneE164 = 'Enter a valid Uzbekistan number (+998XXXXXXXXX).';
  }

  const first = form.firstName.trim();
  const last = form.lastName.trim();

  if (!first) {
    errors.firstName = 'First name is required.';
  } else if (first.length < 2) {
    errors.firstName = 'First name must be at least 2 characters.';
  } else if (first.length > 50) {
    errors.firstName = 'First name must be 50 characters or fewer.';
  }

  if (!last) {
    errors.lastName = 'Last name is required.';
  } else if (last.length < 2) {
    errors.lastName = 'Last name must be at least 2 characters.';
  } else if (last.length > 50) {
    errors.lastName = 'Last name must be 50 characters or fewer.';
  }

  return !errors.phoneE164 && !errors.firstName && !errors.lastName;
}

async function handleSubmit() {
  serverError.value = '';
  if (!validate()) return;

  const fullName = `${form.firstName.trim()} ${form.lastName.trim()}`;

  try {
    await auth.register({ phoneE164: phone.value, fullName });
    await router.push('/');
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 409) {
        errors.phoneE164 = 'This phone number is already registered.';
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
          <h2 class="text-h5 text-weight-bold text-dark q-my-none">Create your account</h2>
          <p class="text-body2 text-grey-6 q-mt-xs q-mb-none">
            Join PulsePay — Uzbekistan's payment platform.
          </p>
        </div>

        <q-form class="column q-gutter-y-md" @submit.prevent="handleSubmit">
          <q-banner v-if="serverError" dense rounded class="bg-red-1 text-negative">
            <template #avatar>
              <q-icon name="warning" color="negative" />
            </template>
            {{ serverError }}
          </q-banner>

          <!-- First name + Last name side by side -->
          <div class="row q-col-gutter-md">
            <div class="col-6">
              <q-input
                v-model="form.firstName"
                type="text"
                label="First name"
                placeholder="Jasur"
                outlined
                autocomplete="given-name"
                :error="!!errors.firstName"
                :error-message="errors.firstName"
                :disable="auth.isLoading"
              />
            </div>
            <div class="col-6">
              <q-input
                v-model="form.lastName"
                type="text"
                label="Last name"
                placeholder="Yusupov"
                outlined
                autocomplete="family-name"
                :error="!!errors.lastName"
                :error-message="errors.lastName"
                :disable="auth.isLoading"
              />
            </div>
          </div>

          <!-- Phone -->
          <q-input
            :model-value="phoneDigits"
            type="tel"
            inputmode="tel"
            label="Mobile number"
            outlined
            maxlength="9"
            autocomplete="tel"
            :error="!!errors.phoneE164"
            :error-message="errors.phoneE164"
            :disable="auth.isLoading"
            @update:model-value="onPhoneInput"
          >
            <template #prepend>
              <span class="text-body2 text-grey-7">+998</span>
            </template>
          </q-input>

          <q-btn
            type="submit"
            label="Create account"
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
          Already have an account?
          <RouterLink to="/login" class="text-primary text-weight-semibold"> Log in </RouterLink>
        </p>
      </div>
    </main>
  </q-page>
</template>

<style scoped>
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

.brand-panel .q-list,
.brand-panel h1,
.brand-panel p {
  position: relative;
  z-index: 1;
}

.form-inner {
  width: 100%;
  max-width: 360px;
}
</style>
