<script setup lang="ts">
import { RouterView, useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const navItems = [
  { label: 'Bosh sahifa', to: '/', exact: true, icon: 'M3 10.5 12 3l9 7.5M5.5 9.5V20h13V9.5' },
  {
    label: 'Kartalar',
    to: '/cards',
    exact: false,
    icon: 'M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 11h18',
  },
  {
    label: "O'tkazma",
    to: '/send',
    exact: false,
    icon: 'M8 21V5m0 16-3.5-3.5M8 5l3.5 3.5M16 3v16m0 0 3.5-3.5M16 19l-3.5-3.5',
  },
  { label: 'Hisobotlar', to: '/reports', exact: false, icon: 'M12 3a9 9 0 1 0 9 9h-9z' },
];

function handleLogout() {
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

        <div v-if="auth.isAuthenticated" class="pp-header-right">
          <div class="pp-user">
            <div class="pp-avatar">
              {{ initials(auth.user?.fullName || auth.user?.phoneE164 || '?') }}
            </div>
            <span class="pp-username">{{ auth.user?.fullName || auth.user?.phoneE164 }}</span>
          </div>
          <button class="pp-logout-btn" @click="handleLogout">
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
            Chiqish
          </button>
        </div>
        <div v-else class="pp-header-right">
          <RouterLink to="/login" class="pp-ghost-btn">Kirish</RouterLink>
          <RouterLink to="/register" class="pp-primary-btn">Boshlash</RouterLink>
        </div>
      </div>
    </q-header>

    <q-page-container>
      <RouterView />
    </q-page-container>

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
      © 2026 PulsePay · MBU litsenziyalangan
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
  gap: 14px;
}

.pp-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pp-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(41, 190, 140, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #29be8c;
  flex-shrink: 0;
}

.pp-username {
  font-size: 14.5px;
  font-weight: 600;
  color: #f7f4ed;
}

.pp-logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: 1px solid rgba(247, 244, 237, 0.16);
  border-radius: 999px;
  padding: 8px 14px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.72);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}

.pp-logout-btn:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
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
</style>
