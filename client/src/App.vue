<script setup lang="ts">
import { ref, computed, nextTick } from 'vue';
import { RouterView, useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '@/stores/auth';
import { useLangStore, LANG_OPTIONS } from '@/stores/lang';
import { closeAccountOtp, closeAccountConfirm } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';

const { t } = useI18n();
const auth = useAuthStore();
const lang = useLangStore();
const router = useRouter();
const route = useRoute();

const langOpen = ref(false);
const userMenuOpen = ref(false);

const navItems = computed(() => [
  { label: t('nav.home'), to: '/', exact: true, icon: 'M3 10.5 12 3l9 7.5M5.5 9.5V20h13V9.5' },
  {
    label: t('nav.cards'),
    to: '/cards',
    exact: false,
    icon: 'M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 11h18',
  },
  {
    label: t('nav.transfer'),
    to: '/send',
    exact: false,
    icon: 'M8 21V5m0 16-3.5-3.5M8 5l3.5 3.5M16 3v16m0 0 3.5-3.5M16 19l-3.5-3.5',
  },
  { label: t('nav.reports'), to: '/reports', exact: false, icon: 'M12 3a9 9 0 1 0 9 9h-9z' },
  {
    label: t('nav.exchange_rates'),
    to: '/exchange-rates',
    exact: false,
    icon: 'M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6',
  },
]);

function handleLogout() {
  userMenuOpen.value = false;
  auth.logout();
  router.push('/login');
}

function initials(name: string) {
  return name
    .split(' ')
    .map((w) => w[0] ?? '')
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function currentLangLabel() {
  return LANG_OPTIONS.find((o) => o.code === lang.lang)?.label ?? 'UZ';
}

function selectLang(code: (typeof LANG_OPTIONS)[0]['code']) {
  lang.setLang(code);
  langOpen.value = false;
}

// ── Delete account modal ──────────────────────────────────────────────────

const deleteModal = ref(false);
// 'warning' → show warning + send-otp btn; 'otp' → enter code; 'done' → success
const deleteStep = ref<'warning' | 'otp' | 'done'>('warning');
const deleteOtpDigits = ref(['', '', '', '', '', '']);
const deleteOtpRefs = ref<(HTMLInputElement | null)[]>([]);
const deleteError = ref('');
const deleteLoading = ref(false);

function openDeleteModal() {
  userMenuOpen.value = false;
  deleteStep.value = 'warning';
  deleteOtpDigits.value = ['', '', '', '', '', ''];
  deleteError.value = '';
  deleteLoading.value = false;
  deleteModal.value = true;
}

function closeDeleteModal() {
  deleteModal.value = false;
}

function setDeleteOtpRef(el: unknown, i: number) {
  deleteOtpRefs.value[i] = el as HTMLInputElement | null;
}

function onDeleteOtpInput(i: number, e: Event) {
  const val = (e.target as HTMLInputElement).value.replace(/\D/g, '');
  deleteOtpDigits.value[i] = val.slice(-1);
  deleteError.value = '';
  if (val && i < 5) nextTick(() => deleteOtpRefs.value[i + 1]?.focus());
  if (deleteOtpDigits.value.every((d) => d !== '')) confirmDelete();
}

function onDeleteOtpKeydown(i: number, e: KeyboardEvent) {
  if (e.key === 'Backspace' && !deleteOtpDigits.value[i] && i > 0) {
    deleteOtpDigits.value[i - 1] = '';
    nextTick(() => deleteOtpRefs.value[i - 1]?.focus());
  }
}

async function sendDeleteOtp() {
  deleteError.value = '';
  deleteLoading.value = true;
  try {
    await closeAccountOtp();
    deleteStep.value = 'otp';
    nextTick(() => deleteOtpRefs.value[0]?.focus());
  } catch (err) {
    deleteError.value = err instanceof ApiError ? err.message : t('common.error_generic');
  } finally {
    deleteLoading.value = false;
  }
}

async function confirmDelete() {
  const code = deleteOtpDigits.value.join('');
  if (code.length < 6) return;
  deleteError.value = '';
  deleteLoading.value = true;
  try {
    await closeAccountConfirm(code);
    deleteStep.value = 'done';
    setTimeout(() => {
      closeDeleteModal();
      auth.logout();
      router.push('/login');
    }, 2000);
  } catch (err) {
    deleteOtpDigits.value = ['', '', '', '', '', ''];
    nextTick(() => deleteOtpRefs.value[0]?.focus());
    deleteError.value =
      err instanceof ApiError
        ? err.status === 400
          ? t('error.otp_invalid')
          : err.message
        : t('common.error_generic');
  } finally {
    deleteLoading.value = false;
  }
}
</script>

<template>
  <q-layout view="hHh lpR fFf">
    <q-header
      v-if="
        !route.meta.authLayout &&
        !route.meta.requiresAdmin &&
        !route.meta.adminGuestOnly &&
        !route.meta.requiresMerchant &&
        !route.meta.merchantGuestOnly
      "
      class="pp-header"
    >
      <div class="pp-header-inner">
        <div class="pp-header-left">
          <RouterLink to="/" class="pp-logo">
            <div class="pp-logo-icon">
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
            <span>Pulse<span class="pp-green">Pay</span></span>
          </RouterLink>
          <nav class="pp-nav">
            <RouterLink
              v-for="item in navItems"
              :key="item.to"
              v-slot="{ isActive, isExactActive, navigate }"
              :to="item.to"
              custom
            >
              <button
                class="pp-nav-btn"
                :class="{ active: item.exact ? isExactActive : isActive }"
                style="display: flex; align-items: center; gap: 8px"
                @click="navigate"
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
                  <path :d="item.icon"></path>
                </svg>
                {{ item.label }}
              </button>
            </RouterLink>
          </nav>
        </div>

        <div class="pp-header-right">
          <!-- Language dropdown -->
          <div class="pp-lang-dropdown">
            <button class="pp-lang-toggle" @click="langOpen = !langOpen">
              {{ currentLangLabel() }}
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6"></path>
              </svg>
            </button>
            <div v-if="langOpen" class="pp-lang-menu">
              <button
                v-for="opt in LANG_OPTIONS"
                :key="opt.code"
                class="pp-lang-menu-item"
                :class="{ active: lang.lang === opt.code }"
                @click="selectLang(opt.code)"
              >
                {{ opt.display }}
              </button>
            </div>
          </div>

          <template v-if="auth.isAuthenticated">
            <!-- User dropdown -->
            <div class="pp-user-menu-wrap">
              <button
                class="pp-avatar"
                :title="auth.user?.fullName || auth.user?.phoneE164 || ''"
                @click="userMenuOpen = !userMenuOpen"
              >
                {{ initials(auth.user?.fullName || auth.user?.phoneE164 || '?') }}
              </button>
              <div v-if="userMenuOpen" class="pp-user-menu">
                <div class="pp-user-menu-name">
                  {{ auth.user?.fullName || auth.user?.phoneE164 }}
                </div>
                <div class="pp-user-menu-divider"></div>
                <button class="pp-user-menu-item" @click="handleLogout">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"></path>
                  </svg>
                  {{ t('nav.logout') }}
                </button>
                <button class="pp-user-menu-item danger" @click="openDeleteModal">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"></path>
                  </svg>
                  {{ t('user.delete_account') }}
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <RouterLink to="/login" class="pp-ghost-btn">{{ t('nav.login') }}</RouterLink>
            <RouterLink to="/register" class="pp-primary-btn">{{ t('nav.start') }}</RouterLink>
          </template>
        </div>
      </div>
    </q-header>

    <q-page-container>
      <RouterView />
    </q-page-container>

    <!-- Delete Account Modal -->
    <Teleport to="body">
      <div v-if="deleteModal" class="pp-modal-overlay" @click.self="closeDeleteModal">
        <div class="pp-modal">
          <!-- Success state -->
          <template v-if="deleteStep === 'done'">
            <div class="pp-modal-icon success">
              <svg
                width="28"
                height="28"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29be8c"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M20 6 9 17l-5-5"></path>
              </svg>
            </div>
            <h3 class="pp-modal-title">{{ t('user.delete_success') }}</h3>
          </template>

          <!-- Warning state -->
          <template v-else-if="deleteStep === 'warning'">
            <div class="pp-modal-icon danger">
              <svg
                width="28"
                height="28"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#ff6b6b"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"></path>
              </svg>
            </div>
            <h3 class="pp-modal-title">{{ t('user.delete_title') }}</h3>
            <p class="pp-modal-desc">{{ t('user.delete_desc') }}</p>
            <div v-if="deleteError" class="pp-modal-error">{{ deleteError }}</div>
            <div class="pp-modal-actions">
              <button class="pp-modal-btn ghost" @click="closeDeleteModal">
                {{ t('common.cancel') }}
              </button>
              <button class="pp-modal-btn danger" :disabled="deleteLoading" @click="sendDeleteOtp">
                <span v-if="deleteLoading" class="pp-spinner-sm"></span>
                <span v-else>{{ t('user.delete_send_otp') }}</span>
              </button>
            </div>
          </template>

          <!-- OTP state -->
          <template v-else>
            <h3 class="pp-modal-title">{{ t('user.delete_account') }}</h3>
            <p class="pp-modal-desc">{{ t('user.delete_otp_hint') }}</p>
            <div class="pp-modal-otp-boxes">
              <input
                v-for="i in 6"
                :key="i"
                :ref="(el) => setDeleteOtpRef(el, i - 1)"
                type="tel"
                inputmode="numeric"
                maxlength="1"
                :value="deleteOtpDigits[i - 1]"
                class="pp-modal-otp-box"
                :class="{ filled: deleteOtpDigits[i - 1] }"
                @input="onDeleteOtpInput(i - 1, $event)"
                @keydown="onDeleteOtpKeydown(i - 1, $event)"
              />
            </div>
            <div v-if="deleteError" class="pp-modal-error">{{ deleteError }}</div>
            <div class="pp-modal-actions">
              <button class="pp-modal-btn ghost" @click="closeDeleteModal">
                {{ t('common.cancel') }}
              </button>
              <button
                class="pp-modal-btn danger"
                :disabled="deleteLoading || deleteOtpDigits.join('').length < 6"
                @click="confirmDelete"
              >
                <span v-if="deleteLoading" class="pp-spinner-sm"></span>
                <span v-else>{{ t('user.delete_confirm_btn') }}</span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </Teleport>

    <footer
      v-if="
        !route.meta.authLayout &&
        !route.meta.requiresAdmin &&
        !route.meta.adminGuestOnly &&
        !route.meta.requiresMerchant &&
        !route.meta.merchantGuestOnly
      "
      style="
        padding: 18px clamp(20px, 4vw, 48px);
        border-top: 1px solid rgba(247, 244, 237, 0.08);
        font-size: 12.5px;
        color: rgba(247, 244, 237, 0.4);
        font-family: Manrope, sans-serif;
      "
    >
      {{ t('footer.copyright') }}
    </footer>
  </q-layout>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');

html,
body {
  margin: 0;
  padding: 0;
  background: #0e211c !important;
  color: #f7f4ed;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

.q-layout,
.q-page-container,
.q-page {
  background: #0e211c !important;
}

a {
  color: #6fd8a8;
  text-decoration: none;
}
a:hover {
  color: #29be8c;
}

@keyframes pp-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Header ─────────────────────────────── */
.pp-header {
  background: rgba(14, 33, 28, 0.92) !important;
  backdrop-filter: blur(10px) !important;
  border-bottom: 1px solid rgba(247, 244, 237, 0.08) !important;
  box-shadow: none !important;
}

.pp-header-inner {
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px clamp(20px, 4vw, 48px);
  flex-wrap: wrap;
}

.pp-header-left {
  display: flex;
  align-items: center;
  gap: clamp(20px, 3vw, 40px);
}

.pp-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none !important;
  color: #f7f4ed !important;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.pp-logo-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #29be8c;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pp-green {
  color: #29be8c;
}

.pp-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pp-nav-btn {
  border: none;
  cursor: pointer;
  border-radius: 999px;
  padding: 9px 16px;
  font-family: Manrope, sans-serif;
  font-size: 14.5px;
  font-weight: 600;
  transition:
    background 0.15s,
    color 0.15s;
  background: transparent;
  color: rgba(247, 244, 237, 0.6);
}

.pp-nav-btn:hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}
.pp-nav-btn.active {
  background: rgba(247, 244, 237, 0.1);
  color: #f7f4ed;
}

.pp-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pp-ghost-btn {
  color: rgba(247, 244, 237, 0.7) !important;
  font-family: Manrope, sans-serif;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none !important;
  padding: 8px 14px;
  border: 1px solid rgba(247, 244, 237, 0.16);
  border-radius: 999px;
  transition:
    background 0.15s,
    color 0.15s;
}

.pp-ghost-btn:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed !important;
}

.pp-primary-btn {
  color: #0e211c !important;
  font-family: Manrope, sans-serif;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none !important;
  padding: 9px 16px;
  background: #29be8c;
  border-radius: 999px;
  transition: background 0.15s;
}

.pp-primary-btn:hover {
  background: #4fd3a6;
}

/* ── Shared view utilities ───────────────── */
.pp-page {
  font-family: Manrope, system-ui, sans-serif;
  color: #f7f4ed;
  background: #0e211c;
  min-height: 100vh;
}

.pp-main {
  max-width: 1120px;
  margin: 0 auto;
  padding: clamp(28px, 4vw, 48px) clamp(20px, 4vw, 48px) 64px;
}

.pp-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: 999px;
  background: #29be8c;
  color: #0e211c;
  padding: 12px 20px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.15s;
}

.pp-btn-primary:hover {
  background: #4fd3a6;
  color: #0e211c !important;
}

.pp-btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(247, 244, 237, 0.18);
  border-radius: 999px;
  background: transparent;
  color: #f7f4ed;
  padding: 12px 20px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.15s;
}

.pp-btn-ghost:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed !important;
}

.pp-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(41, 190, 140, 0.25);
  border-top-color: #29be8c;
  border-radius: 50%;
  animation: pp-spin 0.8s linear infinite;
}

.pp-filter-bar {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(247, 244, 237, 0.05);
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 999px;
}

.pp-filter-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 16px;
  font-family: Manrope, sans-serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
  background: transparent;
  color: rgba(247, 244, 237, 0.6);
}

.pp-filter-btn.active {
  background: #f7f4ed;
  color: #0e211c;
}
.pp-filter-btn:not(.active):hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}

.pp-tx-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.12s;
}

.pp-tx-row:hover {
  background: rgba(247, 244, 237, 0.04);
}

.pp-tx-row + .pp-tx-row {
  border-top: 1px solid rgba(247, 244, 237, 0.07);
}

.pp-tx-icon {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pp-tx-icon.credit {
  background: rgba(41, 190, 140, 0.14);
}
.pp-tx-icon.debit {
  background: rgba(255, 156, 130, 0.14);
}

.pp-section-card {
  background: rgba(247, 244, 237, 0.04);
  border: 1px solid rgba(247, 244, 237, 0.09);
  border-radius: 18px;
  overflow: hidden;
}

/* ── Modal overlay ───────────────────────── */
.pp-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(14, 33, 28, 0.78);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 24px;
}

.pp-modal {
  background: #14302a;
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 22px;
  width: 100%;
  max-width: 420px;
  overflow: hidden;
}

.pp-modal-header {
  padding: 28px 28px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}

.pp-modal-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pp-modal-icon.success {
  background: rgba(41, 190, 140, 0.18);
}
.pp-modal-icon.error {
  background: rgba(255, 156, 130, 0.18);
}

.pp-modal-body {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pp-modal-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16px;
  font-size: 14px;
}

.pp-modal-label {
  color: rgba(247, 244, 237, 0.5);
}
.pp-modal-val {
  font-weight: 600;
  text-align: right;
}

.pp-modal-footer {
  padding: 0 28px 24px;
}

.pp-modal-close {
  width: 100%;
  border: 1px solid rgba(247, 244, 237, 0.18);
  border-radius: 999px;
  background: transparent;
  color: #f7f4ed;
  padding: 13px;
  font-family: Manrope, sans-serif;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}

.pp-modal-close:hover {
  background: rgba(247, 244, 237, 0.08);
}

/* ── Language dropdown ───────────────────── */
.pp-lang-dropdown {
  position: relative;
  flex: none;
}

.pp-lang-toggle {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 12px;
  background: rgba(247, 244, 237, 0.05);
  border: 1px solid rgba(247, 244, 237, 0.12);
  border-radius: 999px;
  font-family: Manrope, sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #f7f4ed;
  cursor: pointer;
  transition: background 0.15s;
}

.pp-lang-toggle:hover {
  background: rgba(247, 244, 237, 0.1);
}

.pp-lang-menu {
  position: absolute;
  top: 42px;
  right: 0;
  z-index: 10;
  min-width: 130px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  background: #122923;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 14px;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.4);
}

.pp-lang-menu-item {
  text-align: left;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(247, 244, 237, 0.8);
  padding: 9px 12px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}

.pp-lang-menu-item:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}

.pp-lang-menu-item.active {
  background: rgba(41, 190, 140, 0.15);
  color: #29be8c;
}

/* ── User menu dropdown ──────────────────── */
.pp-user-menu-wrap {
  position: relative;
  flex: none;
}

.pp-avatar {
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(41, 190, 140, 0.18);
  border: 1px solid rgba(41, 190, 140, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #29be8c;
  cursor: pointer;
  transition: background 0.15s;
}

.pp-avatar:hover {
  background: rgba(41, 190, 140, 0.28);
}

.pp-user-menu {
  position: absolute;
  top: 46px;
  right: 0;
  z-index: 100;
  min-width: 200px;
  background: #122923;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 16px;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.45);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pp-user-menu-name {
  padding: 8px 12px 6px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.55);
  font-family: Manrope, sans-serif;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pp-user-menu-divider {
  height: 1px;
  background: rgba(247, 244, 237, 0.08);
  margin: 2px 0;
}

.pp-user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(247, 244, 237, 0.8);
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.15s,
    color 0.15s;
}

.pp-user-menu-item:hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}

.pp-user-menu-item.danger {
  color: #ff8a8a;
}

.pp-user-menu-item.danger:hover {
  background: rgba(255, 100, 100, 0.1);
  color: #ff6b6b;
}

/* ── Delete account modal ────────────────── */
.pp-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.pp-modal {
  background: #122923;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 24px;
  padding: 36px 32px;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0;
  font-family: Manrope, sans-serif;
}

.pp-modal-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.pp-modal-icon.danger {
  background: rgba(255, 100, 100, 0.12);
}

.pp-modal-icon.success {
  background: rgba(41, 190, 140, 0.12);
}

.pp-modal-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 22px;
  font-weight: 600;
  color: #f7f4ed;
  margin: 0 0 12px;
}

.pp-modal-desc {
  font-size: 14px;
  line-height: 1.55;
  color: rgba(247, 244, 237, 0.6);
  margin: 0 0 24px;
}

.pp-modal-error {
  font-size: 13px;
  color: #ff9c82;
  margin-bottom: 16px;
}

.pp-modal-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-top: 4px;
}

.pp-modal-btn {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.15s,
    opacity 0.15s;
  letter-spacing: 0.04em;
}

.pp-modal-btn.ghost {
  background: rgba(247, 244, 237, 0.07);
  color: rgba(247, 244, 237, 0.7);
}

.pp-modal-btn.ghost:hover {
  background: rgba(247, 244, 237, 0.12);
  color: #f7f4ed;
}

.pp-modal-btn.danger {
  background: #c0392b;
  color: #fff;
}

.pp-modal-btn.danger:hover:not(:disabled) {
  background: #e74c3c;
}

.pp-modal-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.pp-modal-otp-boxes {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  width: 100%;
}

.pp-modal-otp-box {
  flex: 1;
  min-width: 0;
  height: 56px;
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 10px;
  background: rgba(14, 33, 28, 0.55);
  color: #f7f4ed;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
  outline: none;
  caret-color: #29be8c;
  transition: border-color 0.15s;
}

.pp-modal-otp-box:focus {
  border-color: #29be8c;
}

.pp-modal-otp-box.filled {
  border-color: rgba(247, 244, 237, 0.3);
}

.pp-spinner-sm {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: pp-spin 0.7s linear infinite;
  display: inline-block;
}
</style>
