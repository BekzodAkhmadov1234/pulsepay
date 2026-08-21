<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { useMerchantsStore } from '@/stores/merchants';
import type { MerchantDto, MerchantCategoryDto } from '@/lib/api/merchants';
import { listMerchantCategories } from '@/lib/api/merchants';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const adminAuth = useAdminAuthStore();
const store = useMerchantsStore();

const categories = ref<MerchantCategoryDto[]>([]);

onMounted(async () => {
  store.load();
  categories.value = await listMerchantCategories();
});

// ── Onboard modal ──────────────────────────────────────────────────────────
const showModal = ref(false);
const form = ref({ legalTradeName: '', mccCode: '', email: '', password: '' });
const formError = ref('');
const formLoading = ref(false);

async function submitOnboard() {
  formError.value = '';
  formLoading.value = true;
  try {
    await store.create({
      legalTradeName: form.value.legalTradeName,
      mccCode: form.value.mccCode,
      email: form.value.email,
      password: form.value.password,
    });
    showModal.value = false;
    form.value = { legalTradeName: '', mccCode: '', email: '', password: '' };
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Xatolik yuz berdi';
  } finally {
    formLoading.value = false;
  }
}

// ── Action handlers ────────────────────────────────────────────────────────
const actionError = ref<Record<string, string>>({});

async function approve(m: MerchantDto) {
  try {
    await store.approve(m.id);
  } catch (e) {
    actionError.value[m.id] = e instanceof ApiError ? e.message : 'Xatolik';
  }
}

async function reject(m: MerchantDto) {
  try {
    await store.reject(m.id, 'KYB rad etildi');
  } catch (e) {
    actionError.value[m.id] = e instanceof ApiError ? e.message : 'Xatolik';
  }
}

async function suspend(m: MerchantDto) {
  try {
    await store.suspend(m.id, "Admin tomonidan to'xtatildi");
  } catch (e) {
    actionError.value[m.id] = e instanceof ApiError ? e.message : 'Xatolik';
  }
}

function handleLogout() {
  adminAuth.logout();
  router.push('/admin/login');
}

// ── Formatting helpers ─────────────────────────────────────────────────────
const KYB_LABEL: Record<string, string> = {
  pending: 'Kutilmoqda',
  under_review: "Ko'rib chiqilmoqda",
  verified: 'Tasdiqlangan',
  rejected: 'Rad etilgan',
};

const STATUS_LABEL: Record<string, string> = {
  pending: 'Kutilmoqda',
  active: 'Faol',
  suspended: "To'xtatilgan",
  closed: 'Yopilgan',
};

function kybClass(status: string) {
  return (
    {
      pending: 'badge-yellow',
      under_review: 'badge-blue',
      verified: 'badge-green',
      rejected: 'badge-red',
    }[status] ?? 'badge-gray'
  );
}

function statusClass(status: string) {
  return (
    {
      pending: 'badge-yellow',
      active: 'badge-green',
      suspended: 'badge-orange',
      closed: 'badge-red',
    }[status] ?? 'badge-gray'
  );
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('uz-UZ');
}
</script>

<template>
  <div class="page">
    <!-- Header -->
    <header class="admin-header">
      <div class="admin-header-inner">
        <div class="admin-logo">
          <div class="admin-logo-icon">
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
          <span>Pulse<span class="admin-accent">Pay</span></span>
          <span class="admin-badge">Admin</span>
        </div>
        <nav class="admin-nav">
          <button class="admin-nav-btn" @click="router.push('/admin/fee-rules')">
            Komissiya qoidalari
          </button>
          <button class="admin-nav-btn active">Savdogarlar</button>
        </nav>
        <button class="logout-btn" @click="handleLogout">
          <svg
            width="14"
            height="14"
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
    </header>

    <!-- Main -->
    <main class="pp-main">
      <div class="section-head">
        <div>
          <h1 class="page-title">Savdogarlar</h1>
          <p class="page-sub">KYB tasdiqlash va savdogar boshqaruvi</p>
        </div>
        <button class="pp-btn-primary" @click="showModal = true">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 5v14M5 12h14"></path>
          </svg>
          Yangi savdogar
        </button>
      </div>

      <!-- Loading / Error -->
      <div v-if="store.isLoading" style="display: flex; justify-content: center; padding: 60px 0">
        <div class="pp-spinner"></div>
      </div>
      <div v-else-if="store.error" style="color: #ff9c82; padding: 20px">{{ store.error }}</div>

      <!-- Table -->
      <div v-else class="pp-section-card" style="margin-top: 28px">
        <div
          v-if="store.merchants.length === 0"
          style="padding: 48px; text-align: center; color: rgba(247, 244, 237, 0.4)"
        >
          Hali savdogar yo'q
        </div>
        <table v-else class="merchant-table">
          <thead>
            <tr>
              <th>Nomi</th>
              <th>Email</th>
              <th>KYB holati</th>
              <th>Status</th>
              <th>Sana</th>
              <th>Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in store.merchants" :key="m.id">
              <td class="name-cell">{{ m.legalTradeName }}</td>
              <td style="color: rgba(247, 244, 237, 0.6); font-size: 13px">{{ m.email }}</td>
              <td>
                <span :class="['badge', kybClass(m.kybStatus)]">{{
                  KYB_LABEL[m.kybStatus] ?? m.kybStatus
                }}</span>
              </td>
              <td>
                <span :class="['badge', statusClass(m.status)]">{{
                  STATUS_LABEL[m.status] ?? m.status
                }}</span>
              </td>
              <td style="color: rgba(247, 244, 237, 0.5); font-size: 13px">
                {{ formatDate(m.createdAt) }}
              </td>
              <td>
                <div class="action-row">
                  <button
                    v-if="m.kybStatus === 'pending' || m.kybStatus === 'under_review'"
                    class="action-btn approve"
                    @click="approve(m)"
                  >
                    Tasdiqlash
                  </button>
                  <button
                    v-if="m.kybStatus === 'pending' || m.kybStatus === 'under_review'"
                    class="action-btn reject"
                    @click="reject(m)"
                  >
                    Rad etish
                  </button>
                  <button
                    v-if="m.status === 'active'"
                    class="action-btn suspend"
                    @click="suspend(m)"
                  >
                    To'xtatish
                  </button>
                  <span v-if="actionError[m.id]" style="color: #ff9c82; font-size: 12px">{{
                    actionError[m.id]
                  }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- Onboard Modal -->
    <div v-if="showModal" class="pp-modal-overlay" @click.self="showModal = false">
      <div class="pp-modal">
        <div class="pp-modal-header">
          <div class="pp-modal-icon success">
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
              <path
                d="M20 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"
              ></path>
              <path d="M16 3H8a2 2 0 0 0-2 2v2h12V5a2 2 0 0 0-2-2z"></path>
            </svg>
          </div>
          <h2 style="margin: 0; font-size: 18px; font-weight: 700">Yangi savdogar</h2>
        </div>
        <form class="pp-modal-body" @submit.prevent="submitOnboard">
          <label class="field-label">Savdogar nomi</label>
          <input
            v-model="form.legalTradeName"
            class="modal-input"
            placeholder="Kompaniya nomi"
            required
          />

          <label class="field-label" style="margin-top: 14px">Toifa (MCC)</label>
          <select v-model="form.mccCode" class="modal-input" required>
            <option value="" disabled>Toifani tanlang</option>
            <option v-for="cat in categories" :key="cat.mccCode" :value="cat.mccCode">
              {{ cat.mccCode }} — {{ cat.nameUz }}
            </option>
          </select>

          <label class="field-label" style="margin-top: 14px">Email</label>
          <input
            v-model="form.email"
            type="email"
            class="modal-input"
            placeholder="merchant@example.com"
            required
          />

          <label class="field-label" style="margin-top: 14px">Parol</label>
          <input
            v-model="form.password"
            type="password"
            class="modal-input"
            placeholder="••••••••"
            required
          />

          <div v-if="formError" style="color: #ff9c82; font-size: 13px; margin-top: 8px">
            {{ formError }}
          </div>

          <div style="display: flex; gap: 10px; margin-top: 20px">
            <button type="button" class="pp-modal-close" @click="showModal = false">
              Bekor qilish
            </button>
            <button type="submit" class="pp-btn-primary" style="flex: 1" :disabled="formLoading">
              {{ formLoading ? '...' : "Ro'yxatdan o'tkazish" }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');

.page {
  min-height: 100vh;
  background: #0e211c;
  color: #f7f4ed;
  font-family: Manrope, system-ui, sans-serif;
}

.admin-header {
  background: rgba(14, 33, 28, 0.95);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.admin-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px clamp(20px, 4vw, 48px);
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 18px;
  font-weight: 700;
}

.admin-logo-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #29be8c;
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-accent {
  color: #29be8c;
}

.admin-badge {
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(247, 244, 237, 0.1);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.6);
}

.admin-nav {
  flex: 1;
  display: flex;
  gap: 4px;
}

.admin-nav-btn {
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

.admin-nav-btn:hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}
.admin-nav-btn.active {
  background: rgba(247, 244, 237, 0.1);
  color: #f7f4ed;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  background: none;
  border: 1px solid rgba(247, 244, 237, 0.16);
  border-radius: 999px;
  padding: 8px 14px;
  font-family: Manrope, sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.65);
  cursor: pointer;
  transition: background 0.15s;
}

.logout-btn:hover {
  background: rgba(247, 244, 237, 0.07);
  color: #f7f4ed;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 600;
  letter-spacing: -0.02em;
  margin: 0;
}

.page-sub {
  margin: 4px 0 0;
  font-size: 14px;
  color: rgba(247, 244, 237, 0.5);
}

.merchant-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.merchant-table th {
  padding: 12px 20px;
  text-align: left;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.45);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}

.merchant-table td {
  padding: 14px 20px;
  border-bottom: 1px solid rgba(247, 244, 237, 0.06);
  vertical-align: middle;
}

.merchant-table tr:last-child td {
  border-bottom: none;
}
.merchant-table tr:hover td {
  background: rgba(247, 244, 237, 0.02);
}

.name-cell {
  font-weight: 600;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge-green {
  background: rgba(41, 190, 140, 0.15);
  color: #6fd8a8;
}
.badge-yellow {
  background: rgba(242, 178, 62, 0.15);
  color: #f2b23e;
}
.badge-orange {
  background: rgba(255, 140, 80, 0.15);
  color: #ff9c60;
}
.badge-red {
  background: rgba(255, 156, 130, 0.15);
  color: #ff9c82;
}
.badge-blue {
  background: rgba(100, 160, 255, 0.15);
  color: #7eb5ff;
}
.badge-gray {
  background: rgba(247, 244, 237, 0.08);
  color: rgba(247, 244, 237, 0.5);
}

.action-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.action-btn {
  border: none;
  border-radius: 999px;
  padding: 6px 14px;
  font-family: Manrope, sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}

.action-btn.approve {
  background: rgba(41, 190, 140, 0.2);
  color: #6fd8a8;
}
.action-btn.reject {
  background: rgba(255, 156, 130, 0.15);
  color: #ff9c82;
}
.action-btn.suspend {
  background: rgba(255, 140, 80, 0.15);
  color: #ff9c60;
}
.action-btn:hover {
  opacity: 0.8;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.55);
}

.modal-input {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  padding: 0 14px;
  background: rgba(14, 33, 28, 0.6);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 10px;
  font-family: Manrope, sans-serif;
  font-size: 14px;
  color: #f7f4ed;
  box-sizing: border-box;
}

.modal-input:focus {
  outline: none;
  border-color: #29be8c;
}
.modal-input::placeholder {
  color: rgba(247, 244, 237, 0.3);
}
select.modal-input {
  appearance: none;
  cursor: pointer;
}
select.modal-input option {
  background: #14302a;
  color: #f7f4ed;
}
</style>
