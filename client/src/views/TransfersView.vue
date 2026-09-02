<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useTransfersStore } from '@/stores/transfers';
import type { TransferDto } from '@/lib/api/transfers';

const store = useTransfersStore();

onMounted(() => {
  store.fetchTransfers();
});

const txFilter = ref<'all' | 'credit' | 'debit'>('all');
const selectedTx = ref<TransferDto | null>(null);

const filters = [
  { label: 'Barchasi', value: 'all' as const },
  { label: 'Kirim', value: 'credit' as const },
  { label: 'Chiqim', value: 'debit' as const },
];

function parseTxDate(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  return isNaN(d.getTime()) ? null : d;
}

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function txDayKey(tx: TransferDto): string {
  const d = parseTxDate(tx.processedAt ?? tx.initiatedAt);
  if (!d) return 'unknown';
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

function txDayLabel(tx: TransferDto): string {
  const d = parseTxDate(tx.processedAt ?? tx.initiatedAt);
  if (!d) return 'Boshqa';
  const today = new Date();
  if (isSameDay(d, today)) return 'Bugun';
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (isSameDay(d, yesterday)) return 'Kecha';
  return d.toLocaleDateString('uz-UZ', { day: 'numeric', month: 'long' });
}

const filteredTransfers = computed(() => {
  if (txFilter.value === 'all') return store.transfers;
  return store.transfers.filter((t) => t.direction === txFilter.value);
});

const groupedTransfers = computed(() => {
  const groups: { label: string; items: TransferDto[]; total: string }[] = [];
  const seen = new Map<string, { label: string; items: TransferDto[]; total: string }>();
  for (const tx of filteredTransfers.value) {
    const key = txDayKey(tx);
    if (!seen.has(key)) {
      const g = { label: txDayLabel(tx), items: [] as TransferDto[], total: '' };
      groups.push(g);
      seen.set(key, g);
    }
    seen.get(key)!.items.push(tx);
  }
  for (const g of groups) {
    const vol = g.items.reduce((s, t) => s + t.amountUzs, 0);
    const fmt = new Intl.NumberFormat('uz-UZ', {
      style: 'decimal',
      maximumFractionDigits: 0,
    }).format(vol);
    g.total = `${g.items.length} ta · aylanma ${fmt} UZS`;
  }
  return groups;
});

function formatAmount(uzs: number) {
  return (
    new Intl.NumberFormat('uz-UZ', { style: 'decimal', maximumFractionDigits: 0 }).format(uzs) +
    ' UZS'
  );
}

function formatTime(iso: string | null | undefined) {
  if (!iso) return '—';
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  if (isNaN(d.getTime())) return '—';
  return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

function formatDateTime(iso: string | null | undefined) {
  if (!iso) return '—';
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  if (isNaN(d.getTime())) return '—';
  return d.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function statusLabel(status: string) {
  if (status === 'completed') return "Muvaffaqiyatli o'tkazma";
  if (status === 'failed') return "Muvaffaqiyatsiz o'tkazma";
  if (status === 'processing') return 'Jarayonda';
  return status;
}

function transferTypeLabel(tx: TransferDto): string {
  if (tx.transferTypeId === 2) return "Bank o'tkazma";
  if (tx.transferTypeId === 3) return "Savdogar to'lovi";
  return "P2P o'tkazma";
}
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- Header -->
      <div
        style="
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: 20px;
          flex-wrap: wrap;
          margin-bottom: 32px;
        "
      >
        <div>
          <h1
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: clamp(28px, 3.4vw, 40px);
              font-weight: 600;
              letter-spacing: -0.03em;
              margin: 0;
              color: #f7f4ed;
            "
          >
            O'tkazmalar
          </h1>
          <p
            style="
              font-size: 15px;
              line-height: 1.5;
              color: rgba(247, 244, 237, 0.6);
              margin: 10px 0 0;
            "
          >
            Sizning to'lov tarixi
          </p>
        </div>
        <RouterLink to="/send" class="pp-btn-primary" style="padding: 13px 22px; font-size: 13.5px">
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 19V5m0 0-6 6m6-6 6 6"></path>
          </svg>
          Pul jo'natish
        </RouterLink>
      </div>

      <!-- Error -->
      <div
        v-if="store.fetchError"
        style="
          margin-bottom: 20px;
          padding: 14px 18px;
          background: rgba(255, 156, 130, 0.12);
          border: 1px solid rgba(255, 156, 130, 0.3);
          border-radius: 12px;
          font-size: 13.5px;
          color: #ff9c82;
        "
      >
        {{ store.fetchError }}
      </div>

      <!-- Filter bar + section title row -->
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          flex-wrap: wrap;
        "
      >
        <h2
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 22px;
            font-weight: 600;
            letter-spacing: -0.02em;
            margin: 0;
            color: #f7f4ed;
          "
        >
          Tranzaksiyalar tarixi
        </h2>
        <div class="pp-filter-bar">
          <button
            v-for="f in filters"
            :key="f.value"
            class="pp-filter-btn"
            :class="{ active: txFilter === f.value }"
            @click="txFilter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div
        v-if="store.isLoading && store.transfers.length === 0"
        style="display: flex; justify-content: center; padding: 64px"
      >
        <div class="pp-spinner"></div>
      </div>

      <!-- Empty state -->
      <div
        v-else-if="groupedTransfers.length === 0"
        style="
          margin-top: 24px;
          background: rgba(247, 244, 237, 0.04);
          border: 1px dashed rgba(247, 244, 237, 0.15);
          border-radius: 18px;
          padding: 56px 24px;
          text-align: center;
        "
      >
        <svg
          width="40"
          height="40"
          viewBox="0 0 24 24"
          fill="none"
          stroke="rgba(247,244,237,0.25)"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          style="margin: 0 auto 16px; display: block"
        >
          <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"></path>
        </svg>
        <p style="font-size: 15px; font-weight: 600; color: #f7f4ed; margin: 0 0 8px">
          Hozircha o'tkazmalar yo'q
        </p>
        <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 0 0 20px">
          Boshlash uchun boshqa PulsePay foydalanuvchisiga pul jo'nating.
        </p>
        <RouterLink to="/send" class="pp-btn-primary">Pul jo'natish</RouterLink>
      </div>

      <!-- Transfer groups -->
      <div v-else style="display: flex; flex-direction: column; gap: 26px; margin-top: 24px">
        <div v-for="group in groupedTransfers" :key="group.label">
          <div
            style="
              display: flex;
              align-items: baseline;
              justify-content: space-between;
              gap: 12px;
              padding-bottom: 10px;
            "
          >
            <div
              style="
                font-size: 13px;
                font-weight: 700;
                letter-spacing: 0.06em;
                text-transform: uppercase;
                color: rgba(247, 244, 237, 0.45);
              "
            >
              {{ group.label }}
            </div>
            <div style="font-size: 13px; font-weight: 600; color: rgba(247, 244, 237, 0.35)">
              {{ group.total }}
            </div>
          </div>
          <div class="pp-section-card">
            <div
              v-for="tx in group.items"
              :key="tx.id + (tx.direction ?? '')"
              class="pp-tx-row"
              @click="selectedTx = tx"
            >
              <div class="pp-tx-icon" :class="tx.direction === 'credit' ? 'credit' : 'debit'">
                <svg
                  width="17"
                  height="17"
                  viewBox="0 0 24 24"
                  fill="none"
                  :stroke="tx.direction === 'credit' ? '#29BE8C' : '#FF9C82'"
                  stroke-width="2.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  :style="tx.direction === 'credit' ? 'transform: rotate(180deg);' : ''"
                >
                  <path d="M12 19V5m0 0-6 6m6-6 6 6"></path>
                </svg>
              </div>
              <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px">
                <div
                  style="
                    font-size: 15px;
                    font-weight: 600;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                  "
                >
                  {{ (tx.direction === 'credit' ? tx.senderName : tx.recipientName) || '—' }}
                </div>
                <div style="font-size: 13px; color: rgba(247, 244, 237, 0.48)">
                  {{ transferTypeLabel(tx) }}
                </div>
              </div>
              <div
                style="
                  flex: none;
                  display: flex;
                  flex-direction: column;
                  align-items: flex-end;
                  gap: 3px;
                "
              >
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 15.5px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                  "
                  :style="{ color: tx.direction === 'credit' ? '#29BE8C' : '#FF9C82' }"
                >
                  {{ tx.direction === 'credit' ? '+' : '−' }}{{ formatAmount(tx.amountUzs) }}
                </div>
                <div style="font-size: 12.5px; color: rgba(247, 244, 237, 0.4)">
                  {{ formatTime(tx.processedAt ?? tx.initiatedAt) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Transaction detail modal ── -->
    <div v-if="selectedTx" class="pp-modal-overlay" @click.self="selectedTx = null">
      <div class="pp-modal">
        <div class="pp-modal-header">
          <div
            class="pp-modal-icon"
            :class="selectedTx.status === 'completed' ? 'success' : 'error'"
          >
            <svg
              v-if="selectedTx.status === 'completed'"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#29BE8C"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 6 9 17l-5-5"></path>
            </svg>
            <svg
              v-else
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#FF9C82"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 8v4m0 4h.01"></path>
            </svg>
          </div>
          <div
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: 30px;
              font-weight: 600;
              letter-spacing: -0.02em;
              margin-bottom: 6px;
            "
          >
            {{ selectedTx.direction === 'credit' ? '+' : '−'
            }}{{ formatAmount(selectedTx.amountUzs) }}
          </div>
          <div style="font-size: 14px; color: rgba(247, 244, 237, 0.55)">
            {{ statusLabel(selectedTx.status) }}
          </div>
        </div>
        <div class="pp-modal-body">
          <div class="pp-modal-row">
            <span class="pp-modal-label">Jo'natuvchi</span>
            <span class="pp-modal-val">{{ selectedTx.senderName || '—' }}</span>
          </div>
          <div class="pp-modal-row">
            <span class="pp-modal-label">Qabul qiluvchi</span>
            <span class="pp-modal-val">{{ selectedTx.recipientName || '—' }}</span>
          </div>
          <div class="pp-modal-row">
            <span class="pp-modal-label">Komissiya</span>
            <span class="pp-modal-val">{{
              selectedTx.feeAmountUzs ? formatAmount(selectedTx.feeAmountUzs) : '0 UZS'
            }}</span>
          </div>
          <div class="pp-modal-row">
            <span class="pp-modal-label">Sana</span>
            <span class="pp-modal-val">{{
              formatDateTime(selectedTx.processedAt ?? selectedTx.initiatedAt)
            }}</span>
          </div>
        </div>
        <div class="pp-modal-footer">
          <button class="pp-modal-close" @click="selectedTx = null">Yopish</button>
        </div>
      </div>
    </div>
  </q-page>
</template>
