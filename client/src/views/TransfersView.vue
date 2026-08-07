<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useTransfersStore } from '@/stores/transfers';
import type { TransferDto } from '@/lib/api/transfers';

const store = useTransfersStore();

onMounted(() => {
  store.fetchTransfers();
});

function statusColor(status: string) {
  if (status === 'completed') return 'positive';
  if (status === 'failed') return 'negative';
  if (status === 'processing') return 'warning';
  return 'grey';
}

function formatAmount(uzs: number) {
  return (
    new Intl.NumberFormat('uz-UZ', { style: 'decimal', maximumFractionDigits: 0 }).format(uzs) +
    ' UZS'
  );
}

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

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

const groupedTransfers = computed(() => {
  const groups: { label: string; items: TransferDto[] }[] = [];
  const seen = new Map<string, { label: string; items: TransferDto[] }>();
  for (const tx of store.transfers) {
    const key = txDayKey(tx);
    if (!seen.has(key)) {
      const g = { label: txDayLabel(tx), items: [] as TransferDto[] };
      groups.push(g);
      seen.set(key, g);
    }
    seen.get(key)!.items.push(tx);
  }
  return groups;
});
</script>

<template>
  <q-page class="q-pa-lg">
    <div style="max-width: 800px; margin: 0 auto" class="column q-gutter-y-lg">
      <!-- Header -->
      <div class="row items-center justify-between">
        <div>
          <h1 class="text-h5 text-weight-bold q-my-none">O'tkazmalar</h1>
          <p class="text-caption text-grey-6 q-mt-xs q-mb-none">Sizning o'tkazma tarixi</p>
        </div>
        <q-btn unelevated color="primary" no-caps icon="send" label="Pul jo'natish" to="/send" />
      </div>

      <!-- Error -->
      <q-banner v-if="store.fetchError" dense rounded class="bg-red-1 text-negative">
        <template #avatar><q-icon name="warning" color="negative" /></template>
        {{ store.fetchError }}
      </q-banner>

      <!-- Loading -->
      <div
        v-if="store.isLoading && store.transfers.length === 0"
        class="row justify-center q-pa-xl"
      >
        <q-spinner color="primary" size="40px" />
      </div>

      <!-- Empty state -->
      <q-card
        v-else-if="store.transfers.length === 0"
        flat
        bordered
        class="q-pa-xl text-center"
        style="border-style: dashed"
      >
        <q-icon name="swap_horiz" size="48px" color="grey-4" />
        <p class="text-body2 text-weight-medium q-mt-md q-mb-xs">Hozircha o'tkazmalar yo'q</p>
        <p class="text-caption text-grey-6 q-mb-md">
          Boshlash uchun boshqa PulsePay foydalanuvchisiga pul jo'nating.
        </p>
        <q-btn unelevated color="primary" no-caps label="Pul jo'natish" to="/send" />
      </q-card>

      <!-- Transfer list -->
      <template v-else>
        <template v-for="group in groupedTransfers" :key="group.label">
          <p class="text-caption text-weight-medium text-grey-6 q-mt-md q-mb-sm">
            {{ group.label }}
          </p>
          <q-list bordered separator rounded>
            <q-item v-for="tx in group.items" :key="tx.id + (tx.direction ?? '')" class="q-py-md">
              <q-item-section avatar>
                <q-icon
                  :name="tx.direction === 'credit' ? 'arrow_downward' : 'arrow_upward'"
                  :color="tx.direction === 'credit' ? 'positive' : 'negative'"
                  size="24px"
                />
              </q-item-section>

              <q-item-section>
                <q-item-label
                  class="text-weight-medium"
                  :class="tx.direction === 'credit' ? 'text-positive' : 'text-negative'"
                >
                  {{ tx.direction === 'credit' ? '+' : '-' }}{{ formatAmount(tx.amountUzs) }}
                  <span
                    v-if="tx.direction !== 'credit' && tx.feeAmountUzs > 0"
                    class="text-caption text-grey-6 q-ml-xs"
                  >
                    + {{ formatAmount(tx.feeAmountUzs) }} komissiya
                  </span>
                </q-item-label>
                <q-item-label caption>
                  {{ (tx.direction === 'credit' ? tx.senderName : tx.recipientName) || '—' }}
                </q-item-label>
                <q-item-label caption>{{ formatDate(tx.initiatedAt) }}</q-item-label>
              </q-item-section>

              <q-item-section side>
                <q-badge :color="statusColor(tx.status)" :label="tx.status" />
              </q-item-section>
            </q-item>
          </q-list>
        </template>
      </template>
    </div>
  </q-page>
</template>
