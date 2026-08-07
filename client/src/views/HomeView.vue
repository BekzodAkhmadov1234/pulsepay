<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCardsStore } from '@/stores/cards';
import { useTransfersStore } from '@/stores/transfers';
import type { TransferDto } from '@/lib/api/transfers';

const router = useRouter();
const cardsStore = useCardsStore();
const transfersStore = useTransfersStore();

onMounted(() => {
  cardsStore.fetchCards();
  transfersStore.fetchTransfers();
});

const balanceVisible = ref(true);

const totalBalanceUzs = computed(() =>
  cardsStore.cards.reduce((sum, c) => sum + (c.balanceUzs ?? 0), 0)
);

const balanceDisplay = computed(() =>
  cardsStore.cards.length === 0
    ? '— UZS'
    : Number(totalBalanceUzs.value).toLocaleString('uz-UZ') + ' UZS'
);

// Transaction detail dialog
const selectedTx = ref<TransferDto | null>(null);

function openDetail(tx: TransferDto) {
  selectedTx.value = tx;
}

function closeDetail() {
  selectedTx.value = null;
}

function formatAmount(uzs: number) {
  return new Intl.NumberFormat('uz-UZ', { style: 'decimal', maximumFractionDigits: 0 }).format(uzs);
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

function txDate(tx: TransferDto) {
  return formatDateTime(tx.processedAt ?? tx.completedAt ?? tx.initiatedAt);
}

function statusLabel(status: string) {
  if (status === 'completed') return "Muvaffaqiyatli o'tkazma";
  if (status === 'failed') return "Muvaffaqiyatsiz o'tkazma";
  if (status === 'processing') return 'Jarayonda';
  return status;
}

function statusColor(status: string) {
  if (status === 'completed') return 'positive';
  if (status === 'failed') return 'negative';
  return 'warning';
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
  for (const tx of transfersStore.transfers) {
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
    <div style="max-width: 1024px; margin: 0 auto">
      <!-- Stats -->
      <div class="row q-col-gutter-md q-mb-xl">
        <div class="col-12 col-sm-4">
          <q-card
            flat
            bordered
            class="q-pa-md"
            style="cursor: pointer"
            @click="router.push('/cards')"
          >
            <p class="text-caption text-grey-6 q-mb-xs">Umumiy balans</p>
            <div class="row items-center no-wrap">
              <p class="text-h5 text-weight-bold q-my-none col">
                {{ balanceVisible ? balanceDisplay : '••••• UZS' }}
              </p>
              <q-btn
                flat
                round
                dense
                :icon="balanceVisible ? 'visibility' : 'visibility_off'"
                color="grey-6"
                size="sm"
                @click.stop="balanceVisible = !balanceVisible"
              />
            </div>
          </q-card>
        </div>
      </div>

      <!-- Transaction history -->
      <div>
        <p
          class="text-caption text-weight-bold text-uppercase text-grey-6 q-mb-sm"
          style="letter-spacing: 0.1em"
        >
          Tranzaksiyalar tarixi
        </p>

        <!-- Loading -->
        <div
          v-if="transfersStore.isLoading && transfersStore.transfers.length === 0"
          class="row justify-center q-pa-xl"
        >
          <q-spinner color="primary" size="40px" />
        </div>

        <!-- Empty state -->
        <q-card v-else-if="transfersStore.transfers.length === 0" flat bordered>
          <q-card-section class="column items-center q-pa-xl text-center">
            <q-icon name="show_chart" size="40px" color="grey-4" />
            <p class="text-body2 text-weight-medium q-mt-md q-mb-xs">
              Hozircha tranzaksiyalar yo'q
            </p>
            <p class="text-caption text-grey-6 q-mb-none">To'lov tarixi bu yerda ko'rsatiladi.</p>
          </q-card-section>
        </q-card>

        <!-- Transaction list -->
        <template v-else>
          <template v-for="group in groupedTransfers" :key="group.label">
            <p class="text-caption text-weight-medium text-grey-6 q-mt-md q-mb-sm">
              {{ group.label }}
            </p>
            <q-list bordered separator rounded>
              <q-item
                v-for="tx in group.items"
                :key="tx.id + (tx.direction ?? '')"
                v-ripple
                clickable
                class="q-py-sm"
                @click="openDetail(tx)"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="tx.direction === 'credit' ? 'arrow_downward' : 'arrow_upward'"
                    :color="tx.direction === 'credit' ? 'positive' : 'negative'"
                    size="22px"
                  />
                </q-item-section>

                <q-item-section>
                  <q-item-label
                    class="text-weight-medium"
                    :class="tx.direction === 'credit' ? 'text-positive' : 'text-negative'"
                  >
                    {{ tx.direction === 'credit' ? '+' : '-' }}{{ formatAmount(tx.amountUzs) }} UZS
                  </q-item-label>
                  <q-item-label caption>
                    {{ (tx.direction === 'credit' ? tx.senderName : tx.recipientName) || '—' }}
                  </q-item-label>
                </q-item-section>

                <q-item-section side>
                  <q-item-label caption>{{
                    formatTime(tx.processedAt ?? tx.initiatedAt)
                  }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </template>
        </template>
      </div>
    </div>

    <!-- Transaction detail dialog -->
    <q-dialog :model-value="selectedTx !== null" @update:model-value="closeDetail">
      <q-card style="min-width: 320px; max-width: 480px; width: 100%">
        <q-card-section class="column items-center q-pt-lg q-pb-sm">
          <q-icon
            :name="selectedTx?.status === 'completed' ? 'check_circle' : 'error'"
            :color="statusColor(selectedTx?.status ?? '')"
            size="48px"
          />
          <p class="text-subtitle1 text-weight-bold q-mt-sm q-mb-none">
            {{ statusLabel(selectedTx?.status ?? '') }}
          </p>
          <p class="text-h5 text-weight-bold q-mt-xs q-mb-none">
            {{ formatAmount(selectedTx?.amountUzs ?? 0) }} UZS
          </p>
        </q-card-section>

        <q-separator />

        <q-list dense class="q-py-sm">
          <q-item>
            <q-item-section>
              <q-item-label caption>Sana</q-item-label>
              <q-item-label>{{ selectedTx ? txDate(selectedTx) : '—' }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item>
            <q-item-section>
              <q-item-label caption>Qabul qiluvchi</q-item-label>
              <q-item-label>{{ selectedTx?.recipientName || '—' }}</q-item-label>
              <q-item-label caption class="q-mt-xs" style="font-family: monospace">
                {{ selectedTx?.recipientMaskedPan || '—' }}
              </q-item-label>
            </q-item-section>
          </q-item>

          <q-item>
            <q-item-section>
              <q-item-label caption>Jo'natuvchi</q-item-label>
              <q-item-label>{{ selectedTx?.senderName || '—' }}</q-item-label>
              <q-item-label caption class="q-mt-xs" style="font-family: monospace">
                {{ selectedTx?.senderMaskedPan || '—' }}
              </q-item-label>
            </q-item-section>
          </q-item>

          <q-item>
            <q-item-section>
              <q-item-label caption>Komissiya</q-item-label>
              <q-item-label>
                {{
                  selectedTx?.feeAmountUzs
                    ? formatAmount(selectedTx.feeAmountUzs) + ' UZS'
                    : '0 UZS'
                }}
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>

        <q-card-actions align="right" class="q-pb-md q-pr-md">
          <q-btn flat no-caps label="Yopish" color="primary" @click="closeDetail" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>
