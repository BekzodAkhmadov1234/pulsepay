<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useMerchantAuthStore } from '@/stores/merchantAuth';
import { listMerchantSettlements } from '@/lib/api/virtualTerminal';
import type { SettlementBatchDto } from '@/lib/api/virtualTerminal';

const router = useRouter();
const merchantAuth = useMerchantAuthStore();

const settlements = ref<SettlementBatchDto[]>([]);
const isLoading = ref(true);

onMounted(async () => {
  try {
    settlements.value = await listMerchantSettlements();
  } finally {
    isLoading.value = false;
  }
});

function handleLogout() {
  merchantAuth.logout();
  router.push('/merchant/login');
}

function formatUzs(tiyin: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 0 }).format(tiyin / 100) + ' UZS';
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('uz-UZ');
}

const STATUS_LABEL: Record<string, string> = {
  open: 'Ochiq',
  submitted: 'Yuborilgan',
  settled: 'Hisob-kitob qilingan',
  failed: 'Muvaffaqiyatsiz',
};

const STATUS_CLASS: Record<string, string> = {
  open: 'yellow',
  submitted: 'blue',
  settled: 'green',
  failed: 'red',
};
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
          <button class="m-nav-btn" @click="router.push('/merchant/terminal')">Terminal</button>
          <button class="m-nav-btn active">Hisob-kitob</button>
        </nav>
        <div class="m-header-right">
          <span class="m-merchant-name">{{ merchantAuth.merchant?.email }}</span>
          <button class="m-logout-btn" @click="handleLogout">Chiqish</button>
        </div>
      </div>
    </header>

    <main v-if="!isLoading" class="pp-main">
      <div style="margin-bottom: 28px">
        <h1
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: clamp(22px, 3vw, 32px);
            font-weight: 600;
            letter-spacing: -0.02em;
            margin: 0;
          "
        >
          Hisob-kitob
        </h1>
        <p style="font-size: 14px; color: rgba(247, 244, 237, 0.55); margin: 8px 0 0">
          Savdogar hisob-kitob partiyalarining tarixi
        </p>
      </div>

      <div class="pp-section-card">
        <div
          v-if="settlements.length === 0"
          style="padding: 48px; text-align: center; color: rgba(247, 244, 237, 0.4)"
        >
          Hisob-kitob ma'lumotlari yo'q
        </div>
        <table v-else class="s-table">
          <thead>
            <tr>
              <th>Sana</th>
              <th>Tur</th>
              <th>Jami summa</th>
              <th>Status</th>
              <th>Hisob-kitob sanasi</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in settlements" :key="s.id">
              <td>{{ formatDate(s.generatedAt) }}</td>
              <td
                style="
                  font-size: 12.5px;
                  color: rgba(247, 244, 237, 0.6);
                  text-transform: uppercase;
                  letter-spacing: 0.04em;
                "
              >
                {{ s.batchType.replace('_', ' ') }}
              </td>
              <td style="font-weight: 700">{{ formatUzs(s.totalAmount) }}</td>
              <td>
                <span :class="['s-badge', STATUS_CLASS[s.status] ?? 'yellow']">
                  {{ STATUS_LABEL[s.status] ?? s.status }}
                </span>
              </td>
              <td style="color: rgba(247, 244, 237, 0.55)">
                {{ s.settledAt ? formatDate(s.settledAt) : '—' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <div v-else style="display: flex; justify-content: center; padding: 80px 0">
      <div class="pp-spinner"></div>
    </div>
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

.s-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.s-table th {
  padding: 12px 20px;
  text-align: left;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.4);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}
.s-table td {
  padding: 14px 20px;
  border-bottom: 1px solid rgba(247, 244, 237, 0.06);
}
.s-table tr:last-child td {
  border-bottom: none;
}

.s-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.s-badge.green {
  background: rgba(41, 190, 140, 0.15);
  color: #6fd8a8;
}
.s-badge.yellow {
  background: rgba(242, 178, 62, 0.15);
  color: #f2b23e;
}
.s-badge.blue {
  background: rgba(100, 160, 255, 0.15);
  color: #7bb0ff;
}
.s-badge.red {
  background: rgba(255, 100, 80, 0.15);
  color: #ff8070;
}
</style>
