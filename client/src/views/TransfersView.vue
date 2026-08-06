<script setup lang="ts">
import { onMounted } from 'vue';
import { useTransfersStore } from '@/stores/transfers';

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
</script>

<template>
  <q-page class="q-pa-lg">
    <div style="max-width: 800px; margin: 0 auto" class="column q-gutter-y-lg">
      <!-- Header -->
      <div class="row items-center justify-between">
        <div>
          <h1 class="text-h5 text-weight-bold q-my-none">Transfers</h1>
          <p class="text-caption text-grey-6 q-mt-xs q-mb-none">Your outgoing transfer history</p>
        </div>
        <q-btn unelevated color="primary" no-caps icon="send" label="Send money" to="/send" />
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
        <p class="text-body2 text-weight-medium q-mt-md q-mb-xs">No transfers yet</p>
        <p class="text-caption text-grey-6 q-mb-md">
          Send money to another PulsePay user to get started.
        </p>
        <q-btn unelevated color="primary" no-caps label="Send money" to="/send" />
      </q-card>

      <!-- Transfer list -->
      <q-list v-else bordered separator rounded>
        <q-item v-for="tx in store.transfers" :key="tx.id + (tx.direction ?? '')" class="q-py-md">
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
                + {{ formatAmount(tx.feeAmountUzs) }} fee
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
    </div>
  </q-page>
</template>
