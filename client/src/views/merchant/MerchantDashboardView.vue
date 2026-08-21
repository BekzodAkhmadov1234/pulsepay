<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useMerchantAuthStore } from '@/stores/merchantAuth';
import {
  getMerchantProfile,
  getMerchantAccount,
  listMerchantTransfers,
  listMerchantSettlements,
} from '@/lib/api/virtualTerminal';
import type {
  MerchantProfileDto,
  MerchantAccountDto,
  MerchantTransferSummary,
  SettlementBatchDto,
} from '@/lib/api/virtualTerminal';

const router = useRouter();
const merchantAuth = useMerchantAuthStore();

const profile = ref<MerchantProfileDto | null>(null);
const account = ref<MerchantAccountDto | null>(null);
const transfers = ref<MerchantTransferSummary[]>([]);
const settlements = ref<SettlementBatchDto[]>([]);
const isLoading = ref(true);

onMounted(async () => {
  try {
    [profile.value, account.value, transfers.value, settlements.value] = await Promise.all([
      getMerchantProfile(),
      getMerchantAccount(),
      listMerchantTransfers(),
      listMerchantSettlements(),
    ]);
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

const openBatch = () => settlements.value.find((s) => s.status === 'open');
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
          <button class="m-nav-btn active">Boshqaruv paneli</button>
          <button class="m-nav-btn" @click="router.push('/merchant/terminal')">Terminal</button>
          <button class="m-nav-btn" @click="router.push('/merchant/settlements')">
            Hisob-kitob
          </button>
        </nav>
        <div class="m-header-right">
          <span class="m-merchant-name">{{ profile?.legalTradeName ?? '...' }}</span>
          <button class="m-logout-btn" @click="handleLogout">Chiqish</button>
        </div>
      </div>
    </header>

    <main v-if="!isLoading" class="pp-main">
      <!-- Stats row -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-label">Jami o'tkazmalar</div>
          <div class="stat-val">{{ transfers.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Ochiq hisob-kitob</div>
          <div class="stat-val">{{ openBatch() ? formatUzs(openBatch()!.totalAmount) : '—' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Hisob holati</div>
          <div
            class="stat-val"
            :style="{ color: account?.status === 'active' ? '#6FD8A8' : '#FF9C82' }"
          >
            {{ account?.status === 'active' ? 'Faol' : (account?.status ?? '—') }}
          </div>
        </div>
      </div>

      <!-- Recent transfers -->
      <div style="margin-top: 32px">
        <h2 class="section-title">So'nggi to'lovlar</h2>
        <div class="pp-section-card" style="margin-top: 12px">
          <div
            v-if="transfers.length === 0"
            style="padding: 36px; text-align: center; color: rgba(247, 244, 237, 0.4)"
          >
            Hali to'lov yo'q
          </div>
          <table v-else class="tx-table">
            <thead>
              <tr>
                <th>Sana</th>
                <th>Mijoz</th>
                <th>Karta</th>
                <th>Summa</th>
                <th>Komissiya</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in transfers.slice(0, 10)" :key="t.id">
                <td>{{ formatDate(t.initiatedAt) }}</td>
                <td>{{ t.senderName ?? '—' }}</td>
                <td style="font-size: 13px; color: rgba(247, 244, 237, 0.6)">
                  {{ t.senderMaskedPan ?? '—' }}
                </td>
                <td class="amount-cell">{{ formatUzs(t.amount.tiyin) }}</td>
                <td style="color: rgba(247, 244, 237, 0.5)">{{ formatUzs(t.feeAmount.tiyin) }}</td>
                <td>
                  <span :class="['status-badge', t.status === 'completed' ? 'green' : 'yellow']">
                    {{ t.status === 'completed' ? 'Bajarildi' : t.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Open settlement banner -->
      <div v-if="openBatch()" class="settlement-banner" style="margin-top: 28px">
        <div>
          <div class="stat-label">Ochiq hisob-kitob summasi</div>
          <div class="settlement-amount">{{ formatUzs(openBatch()!.totalAmount) }}</div>
        </div>
        <button class="pp-btn-primary" @click="router.push('/merchant/settlements')">
          Hisob-kitob ko'rish
        </button>
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
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.stat-card {
  background: rgba(247, 244, 237, 0.04);
  border: 1px solid rgba(247, 244, 237, 0.09);
  border-radius: 16px;
  padding: 20px 24px;
}
.stat-label {
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.45);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.stat-val {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 24px;
  font-weight: 700;
  margin-top: 8px;
}
.section-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
.tx-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.tx-table th {
  padding: 12px 20px;
  text-align: left;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.4);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}
.tx-table td {
  padding: 13px 20px;
  border-bottom: 1px solid rgba(247, 244, 237, 0.06);
}
.tx-table tr:last-child td {
  border-bottom: none;
}
.amount-cell {
  font-weight: 700;
}
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.status-badge.green {
  background: rgba(41, 190, 140, 0.15);
  color: #6fd8a8;
}
.status-badge.yellow {
  background: rgba(242, 178, 62, 0.15);
  color: #f2b23e;
}
.settlement-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(41, 190, 140, 0.08);
  border: 1px solid rgba(41, 190, 140, 0.2);
  border-radius: 16px;
  padding: 20px 24px;
  gap: 20px;
  flex-wrap: wrap;
}
.settlement-amount {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: #6fd8a8;
  margin-top: 6px;
}
</style>
