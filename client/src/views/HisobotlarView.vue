<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useCardsStore } from '@/stores/cards';
import { useTransfersStore } from '@/stores/transfers';
import type { TransferDto } from '@/lib/api/transfers';

const cardsStore = useCardsStore();
const transfersStore = useTransfersStore();

onMounted(() => {
  if (cardsStore.cards.length === 0) cardsStore.fetchCards();
  if (transfersStore.transfers.length === 0) transfersStore.fetchTransfers();
});

// ── Filter state ──────────────────────────────────────────────
const query = ref('');
const selectedCardId = ref<string | null>(null);
const txType = ref<'all' | 'credit' | 'debit'>('all');
const dateFrom = ref('');
const dateTo = ref('');
const appliedFrom = ref('');
const appliedTo = ref('');

// ── Panel open state ──────────────────────────────────────────
const cardFilterOpen = ref(false);
const typeOpen = ref(false);
const davrOpen = ref(false);

function toggleCardFilter() {
  cardFilterOpen.value = !cardFilterOpen.value;
  if (cardFilterOpen.value) {
    typeOpen.value = false;
    davrOpen.value = false;
  }
}
function toggleType() {
  typeOpen.value = !typeOpen.value;
  if (typeOpen.value) {
    cardFilterOpen.value = false;
    davrOpen.value = false;
  }
}
function toggleDavr() {
  davrOpen.value = !davrOpen.value;
  if (davrOpen.value) {
    cardFilterOpen.value = false;
    typeOpen.value = false;
  }
}
function clearCardFilter() {
  selectedCardId.value = null;
  cardFilterOpen.value = false;
}
function clearType() {
  txType.value = 'all';
  typeOpen.value = false;
}

// ── Date helpers ──────────────────────────────────────────────
function fmtDate(d: Date) {
  return (
    String(d.getDate()).padStart(2, '0') +
    '.' +
    String(d.getMonth() + 1).padStart(2, '0') +
    '.' +
    d.getFullYear()
  );
}

function parseDmy(v: string): Date | null {
  const p = (v || '').split('.');
  if (p.length < 3 || p[2].length < 4) return null;
  return new Date(+p[2], +p[1] - 1, +p[0]);
}

function fmtDateInput(raw: string) {
  const d = raw.replace(/\D/g, '').slice(0, 8);
  return [d.slice(0, 2), d.slice(2, 4), d.slice(4, 8)].filter(Boolean).join('.');
}

function applyQuickRange(days: number) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - days);
  dateFrom.value = fmtDate(start);
  dateTo.value = fmtDate(end);
}

function applyDavr() {
  appliedFrom.value = dateFrom.value;
  appliedTo.value = dateTo.value;
  davrOpen.value = false;
}

function clearDavr() {
  dateFrom.value = '';
  dateTo.value = '';
  appliedFrom.value = '';
  appliedTo.value = '';
  davrOpen.value = false;
}

// ── Transfer date helpers ─────────────────────────────────────
function parseTxDate(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  return isNaN(d.getTime()) ? null : d;
}

function isSameDay(a: Date, b: Date) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function txDayKey(tx: TransferDto) {
  const d = parseTxDate(tx.processedAt ?? tx.initiatedAt);
  if (!d) return 'unknown';
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

function txDayLabel(tx: TransferDto) {
  const d = parseTxDate(tx.processedAt ?? tx.initiatedAt);
  if (!d) return 'Boshqa';
  const today = new Date();
  if (isSameDay(d, today)) return 'Bugun';
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (isSameDay(d, yesterday)) return 'Kecha';
  return d.toLocaleDateString('uz-UZ', { day: 'numeric', month: 'long' });
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

function statusLabel(status: string) {
  if (status === 'completed') return "Muvaffaqiyatli o'tkazma";
  if (status === 'failed') return "Muvaffaqiyatsiz o'tkazma";
  if (status === 'processing') return 'Jarayonda';
  return status;
}

// ── Filtered & grouped transfers ──────────────────────────────
const filteredTransfers = computed(() => {
  const from = parseDmy(appliedFrom.value);
  const to = parseDmy(appliedTo.value);
  const q = query.value.trim().toLowerCase();

  return transfersStore.transfers.filter((t) => {
    if (txType.value !== 'all' && t.direction !== txType.value) return false;
    if (selectedCardId.value) {
      const card = cardsStore.cards.find((c) => c.id === selectedCardId.value);
      if (card) {
        const last4 = (card.maskedPan ?? '').slice(-4);
        const matchSender = (t.senderMaskedPan ?? '').endsWith(last4);
        const matchRecipient = (t.recipientMaskedPan ?? '').endsWith(last4);
        if (!matchSender && !matchRecipient) return false;
      }
    }
    if (from || to) {
      const d = parseTxDate(t.processedAt ?? t.initiatedAt);
      if (d) {
        if (from && d < from) return false;
        if (to) {
          const te = new Date(to);
          te.setDate(te.getDate() + 1);
          if (d >= te) return false;
        }
      }
    }
    if (q) {
      const name = (t.direction === 'credit' ? t.senderName : t.recipientName) ?? '';
      if (!name.toLowerCase().includes(q)) return false;
    }
    return true;
  });
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
    g.total = `${g.items.length} ta · aylanma ${formatAmount(vol)} UZS`;
  }
  return groups;
});

// ── Chip label helpers ────────────────────────────────────────
const cardFilterActive = computed(() => selectedCardId.value !== null);
const typeActive = computed(() => txType.value !== 'all');
const davrActive = computed(() => !!(appliedFrom.value || appliedTo.value));

const cardsLabel = computed(() => {
  if (!selectedCardId.value) return 'Kartalar';
  const card = cardsStore.cards.find((c) => c.id === selectedCardId.value);
  return card ? `···${card.maskedPan?.slice(-4)}` : 'Kartalar';
});

const typeLabel = computed(() => {
  if (txType.value === 'credit') return 'Tushumlar';
  if (txType.value === 'debit') return 'Sarflangan';
  return 'Operatsiya turi';
});

const davrLabel = computed(() => {
  if (!appliedFrom.value && !appliedTo.value) return 'Davr';
  return `${appliedFrom.value || '…'} – ${appliedTo.value || '…'}`;
});

const totalBalance = computed(() => cardsStore.cards.reduce((s, c) => s + (c.balanceUzs ?? 0), 0));

// ── Transaction modal ─────────────────────────────────────────
const selectedTx = ref<TransferDto | null>(null);
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- ── Header ── -->
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
        Hisobotlar
      </h1>

      <!-- ── Search bar ── -->
      <div
        style="
          display: flex;
          align-items: center;
          gap: 12px;
          max-width: 640px;
          margin-top: 22px;
          background: rgba(247, 244, 237, 0.045);
          border: 1px solid rgba(247, 244, 237, 0.12);
          border-radius: 16px;
          padding: 0 16px;
          height: 54px;
        "
      >
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="rgba(247,244,237,0.45)"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
          style="flex: none"
        >
          <circle cx="11" cy="11" r="7"></circle>
          <path d="m20 20-3.6-3.6"></path>
        </svg>
        <input
          v-model="query"
          type="text"
          placeholder="Qidiruv"
          style="
            flex: 1;
            min-width: 0;
            border: none;
            background: transparent;
            font-family: Manrope, sans-serif;
            font-size: 15.5px;
            font-weight: 500;
            color: #f7f4ed;
            outline: none;
          "
        />
      </div>

      <!-- ── Filter chips ── -->
      <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px">
        <!-- Kartalar chip (active) -->
        <div
          v-if="cardFilterActive"
          style="
            display: flex;
            align-items: center;
            gap: 8px;
            height: 44px;
            padding: 0 8px 0 16px;
            background: #29be8c;
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 700;
            color: #0e211c;
          "
        >
          {{ cardsLabel }}
          <button
            type="button"
            title="Filtrni olib tashlash"
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 26px;
              height: 26px;
              border: none;
              border-radius: 50%;
              background: rgba(14, 33, 28, 0.16);
              color: #0e211c;
              cursor: pointer;
              transition: background 0.15s;
            "
            @click="clearCardFilter"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <!-- Kartalar chip (inactive) -->
        <button
          v-else
          type="button"
          style="
            display: flex;
            align-items: center;
            gap: 10px;
            height: 44px;
            padding: 0 16px;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.12);
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 600;
            color: #f7f4ed;
            cursor: pointer;
            transition:
              background 0.15s,
              border-color 0.15s;
          "
          @click="toggleCardFilter"
        >
          Kartalar
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.6)"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="m6 9 6 6 6-6"></path>
          </svg>
        </button>

        <!-- Operatsiya turi chip (active) -->
        <div
          v-if="typeActive"
          style="
            display: flex;
            align-items: center;
            gap: 8px;
            height: 44px;
            padding: 0 8px 0 16px;
            background: #29be8c;
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 700;
            color: #0e211c;
          "
        >
          {{ typeLabel }}
          <button
            type="button"
            title="Filtrni olib tashlash"
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 26px;
              height: 26px;
              border: none;
              border-radius: 50%;
              background: rgba(14, 33, 28, 0.16);
              color: #0e211c;
              cursor: pointer;
              transition: background 0.15s;
            "
            @click="clearType"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <!-- Operatsiya turi chip (inactive) -->
        <button
          v-else
          type="button"
          style="
            display: flex;
            align-items: center;
            gap: 10px;
            height: 44px;
            padding: 0 16px;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.12);
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 600;
            color: #f7f4ed;
            cursor: pointer;
            transition:
              background 0.15s,
              border-color 0.15s;
          "
          @click="toggleType"
        >
          Operatsiya turi
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.6)"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="m6 9 6 6 6-6"></path>
          </svg>
        </button>

        <!-- Davr chip (active) -->
        <div
          v-if="davrActive"
          style="
            display: flex;
            align-items: center;
            gap: 8px;
            height: 44px;
            padding: 0 8px 0 16px;
            background: #29be8c;
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 700;
            color: #0e211c;
          "
        >
          {{ davrLabel }}
          <button
            type="button"
            title="Filtrni olib tashlash"
            style="
              display: flex;
              align-items: center;
              justify-content: center;
              flex: none;
              width: 26px;
              height: 26px;
              border: none;
              border-radius: 50%;
              background: rgba(14, 33, 28, 0.16);
              color: #0e211c;
              cursor: pointer;
              transition: background 0.15s;
            "
            @click="clearDavr"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <!-- Davr chip (inactive) -->
        <button
          v-else
          type="button"
          style="
            display: flex;
            align-items: center;
            gap: 10px;
            height: 44px;
            padding: 0 16px;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.12);
            border-radius: 999px;
            font-family: Manrope, sans-serif;
            font-size: 14px;
            font-weight: 600;
            color: #f7f4ed;
            cursor: pointer;
            transition:
              background 0.15s,
              border-color 0.15s;
          "
          @click="toggleDavr"
        >
          Davr
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="rgba(247,244,237,0.6)"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="m6 9 6 6 6-6"></path>
          </svg>
        </button>
      </div>

      <!-- ── Operatsiya turi panel ── -->
      <div
        v-if="typeOpen"
        style="
          max-width: 640px;
          margin-top: 16px;
          background: rgba(247, 244, 237, 0.045);
          border: 1px solid rgba(247, 244, 237, 0.12);
          border-radius: 20px;
          padding: 24px;
        "
      >
        <h3
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 19px;
            font-weight: 600;
            letter-spacing: -0.015em;
            margin: 0;
            color: #f7f4ed;
          "
        >
          Operatsiya turi
        </h3>
        <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 16px">
          <button
            v-for="opt in [
              { label: 'Tushumlar', value: 'credit' as const },
              { label: 'Sarflangan', value: 'debit' as const },
            ]"
            :key="opt.value"
            type="button"
            style="
              display: flex;
              align-items: center;
              gap: 14px;
              text-align: left;
              border-radius: 16px;
              padding: 16px;
              font-family: Manrope, sans-serif;
              font-size: 15.5px;
              font-weight: 600;
              color: #f7f4ed;
              cursor: pointer;
              transition:
                background 0.15s,
                border-color 0.15s;
              border: 1px solid;
              width: 100%;
            "
            :style="{
              background: txType === opt.value ? 'rgba(41,190,140,0.12)' : 'rgba(14,33,28,0.5)',
              borderColor: txType === opt.value ? 'rgba(41,190,140,0.4)' : 'rgba(247,244,237,0.1)',
            }"
            @click="
              txType = opt.value;
              typeOpen = false;
            "
          >
            <span style="flex: 1; min-width: 0">{{ opt.label }}</span>
            <span
              style="
                display: flex;
                align-items: center;
                justify-content: center;
                flex: none;
                width: 22px;
                height: 22px;
                border-radius: 50%;
                border: 2px solid;
              "
              :style="{ borderColor: txType === opt.value ? '#29BE8C' : 'rgba(247,244,237,0.3)' }"
            >
              <span
                style="width: 10px; height: 10px; border-radius: 50%"
                :style="{ background: txType === opt.value ? '#29BE8C' : 'transparent' }"
              ></span>
            </span>
          </button>
        </div>
      </div>

      <!-- ── Kartalar panel ── -->
      <div
        v-if="cardFilterOpen"
        style="
          max-width: 640px;
          margin-top: 16px;
          background: rgba(247, 244, 237, 0.045);
          border: 1px solid rgba(247, 244, 237, 0.12);
          border-radius: 20px;
          padding: 24px;
        "
      >
        <h3
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 19px;
            font-weight: 600;
            letter-spacing: -0.015em;
            margin: 0;
            color: #f7f4ed;
          "
        >
          Kartalar
        </h3>
        <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 16px">
          <!-- All accounts -->
          <button
            type="button"
            style="
              display: flex;
              align-items: center;
              gap: 14px;
              text-align: left;
              border-radius: 16px;
              padding: 14px 16px;
              font-family: Manrope, sans-serif;
              color: #f7f4ed;
              cursor: pointer;
              transition:
                background 0.15s,
                border-color 0.15s;
              border: 1px solid;
              width: 100%;
            "
            :style="{
              background: selectedCardId === null ? 'rgba(41,190,140,0.12)' : 'rgba(14,33,28,0.5)',
              borderColor:
                selectedCardId === null ? 'rgba(41,190,140,0.4)' : 'rgba(247,244,237,0.1)',
            }"
            @click="clearCardFilter"
          >
            <span
              style="
                display: flex;
                align-items: center;
                justify-content: center;
                flex: none;
                width: 46px;
                height: 32px;
                border-radius: 8px;
                background: rgba(41, 190, 140, 0.18);
                font-family: 'Space Grotesk', sans-serif;
                font-size: 12.5px;
                font-weight: 600;
                color: #29be8c;
              "
              >ALL</span
            >
            <span style="flex: 1; min-width: 0">
              <span
                style="
                  display: block;
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 17px;
                  font-weight: 600;
                  letter-spacing: -0.015em;
                "
                >{{ formatAmount(totalBalance) }} UZS</span
              >
              <span
                style="
                  display: block;
                  font-size: 13px;
                  color: rgba(247, 244, 237, 0.55);
                  margin-top: 3px;
                "
                >Barcha hisoblar</span
              >
            </span>
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke-width="2.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              style="flex: none"
              :stroke="selectedCardId === null ? '#29BE8C' : 'transparent'"
            >
              <path d="M20 6 9 17l-5-5"></path>
            </svg>
          </button>
          <!-- Each card -->
          <button
            v-for="card in cardsStore.cards"
            :key="card.id"
            type="button"
            style="
              display: flex;
              align-items: center;
              gap: 14px;
              text-align: left;
              border-radius: 16px;
              padding: 14px 16px;
              font-family: Manrope, sans-serif;
              color: #f7f4ed;
              cursor: pointer;
              transition:
                background 0.15s,
                border-color 0.15s;
              border: 1px solid;
              width: 100%;
            "
            :style="{
              background:
                selectedCardId === card.id ? 'rgba(41,190,140,0.12)' : 'rgba(14,33,28,0.5)',
              borderColor:
                selectedCardId === card.id ? 'rgba(41,190,140,0.4)' : 'rgba(247,244,237,0.1)',
            }"
            @click="
              selectedCardId = card.id;
              cardFilterOpen = false;
            "
          >
            <span
              style="
                display: flex;
                align-items: center;
                justify-content: center;
                flex: none;
                width: 46px;
                height: 32px;
                border-radius: 8px;
                background: rgba(41, 190, 140, 0.18);
                font-family: 'Space Grotesk', sans-serif;
                font-size: 12.5px;
                font-weight: 600;
                color: #29be8c;
              "
              >{{ card.maskedPan?.slice(-4) ?? '···' }}</span
            >
            <span style="flex: 1; min-width: 0">
              <span
                style="
                  display: block;
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 17px;
                  font-weight: 600;
                  letter-spacing: -0.015em;
                "
                >{{
                  card.balanceUzs != null ? formatAmount(Number(card.balanceUzs)) + ' UZS' : '— UZS'
                }}</span
              >
              <span
                style="
                  display: block;
                  font-size: 13px;
                  color: rgba(247, 244, 237, 0.55);
                  margin-top: 3px;
                "
                >{{ card.maskedPan }}</span
              >
            </span>
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke-width="2.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              style="flex: none"
              :stroke="selectedCardId === card.id ? '#29BE8C' : 'transparent'"
            >
              <path d="M20 6 9 17l-5-5"></path>
            </svg>
          </button>
        </div>
      </div>

      <!-- ── Davr panel ── -->
      <div
        v-if="davrOpen"
        style="
          max-width: 640px;
          margin-top: 16px;
          background: rgba(247, 244, 237, 0.045);
          border: 1px solid rgba(247, 244, 237, 0.12);
          border-radius: 20px;
          padding: 24px;
        "
      >
        <h3
          style="
            font-family: 'Space Grotesk', sans-serif;
            font-size: 19px;
            font-weight: 600;
            letter-spacing: -0.015em;
            margin: 0;
            color: #f7f4ed;
          "
        >
          Davr
        </h3>

        <div style="display: flex; flex-wrap: wrap; gap: 14px; margin-top: 18px">
          <div style="flex: 1 1 200px; min-width: 0">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >Sanadan</label
            >
            <div
              style="
                display: flex;
                align-items: center;
                gap: 10px;
                margin-top: 8px;
                height: 52px;
                padding: 0 14px;
                background: rgba(14, 33, 28, 0.55);
                border: 1px solid rgba(247, 244, 237, 0.14);
                border-radius: 12px;
              "
            >
              <input
                type="text"
                placeholder="KK.OO.YYYY"
                :value="dateFrom"
                style="
                  flex: 1;
                  min-width: 0;
                  padding: 0;
                  border: none;
                  background: transparent;
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 15.5px;
                  font-weight: 500;
                  letter-spacing: 0.04em;
                  color: #f7f4ed;
                  outline: none;
                "
                @input="dateFrom = fmtDateInput(($event.target as HTMLInputElement).value)"
              />
              <svg
                width="17"
                height="17"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                style="flex: none"
              >
                <rect x="3" y="5" width="18" height="16" rx="3"></rect>
                <path d="M3 10h18M8 3v4M16 3v4"></path>
              </svg>
            </div>
          </div>
          <div style="flex: 1 1 200px; min-width: 0">
            <label
              style="
                display: block;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.62);
              "
              >Sanagacha</label
            >
            <div
              style="
                display: flex;
                align-items: center;
                gap: 10px;
                margin-top: 8px;
                height: 52px;
                padding: 0 14px;
                background: rgba(14, 33, 28, 0.55);
                border: 1px solid rgba(247, 244, 237, 0.14);
                border-radius: 12px;
              "
            >
              <input
                type="text"
                placeholder="KK.OO.YYYY"
                :value="dateTo"
                style="
                  flex: 1;
                  min-width: 0;
                  padding: 0;
                  border: none;
                  background: transparent;
                  font-family: 'Space Grotesk', sans-serif;
                  font-size: 15.5px;
                  font-weight: 500;
                  letter-spacing: 0.04em;
                  color: #f7f4ed;
                  outline: none;
                "
                @input="dateTo = fmtDateInput(($event.target as HTMLInputElement).value)"
              />
              <svg
                width="17"
                height="17"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                style="flex: none"
              >
                <rect x="3" y="5" width="18" height="16" rx="3"></rect>
                <path d="M3 10h18M8 3v4M16 3v4"></path>
              </svg>
            </div>
          </div>
        </div>

        <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px">
          <button
            v-for="r in [
              { label: 'Kecha', days: 1 },
              { label: `O'tgan hafta`, days: 7 },
              { label: `O'tgan oy`, days: 30 },
            ]"
            :key="r.label"
            type="button"
            style="
              height: 40px;
              padding: 0 16px;
              background: rgba(247, 244, 237, 0.06);
              border: 1px solid rgba(247, 244, 237, 0.12);
              border-radius: 999px;
              font-family: Manrope, sans-serif;
              font-size: 13.5px;
              font-weight: 600;
              color: #f7f4ed;
              cursor: pointer;
              transition: background 0.15s;
            "
            @click="applyQuickRange(r.days)"
          >
            {{ r.label }}
          </button>
        </div>

        <div
          style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 22px;
          "
        >
          <button
            type="button"
            style="
              border: none;
              background: none;
              padding: 0;
              font-family: Manrope, sans-serif;
              font-size: 14px;
              font-weight: 700;
              color: #29be8c;
              cursor: pointer;
            "
            @click="clearDavr"
          >
            O'chirish
          </button>
          <button
            type="button"
            style="
              border: none;
              border-radius: 999px;
              background: #29be8c;
              color: #0e211c;
              padding: 13px 26px;
              font-family: Manrope, sans-serif;
              font-size: 13.5px;
              font-weight: 700;
              cursor: pointer;
              transition: background 0.15s;
            "
            @click="applyDavr"
          >
            Ko'rsatish
          </button>
        </div>
      </div>

      <!-- ── Transaction history ── -->
      <section>
        <div
          style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
            margin-top: 24px;
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
            Operatsiyalar
          </h2>
        </div>

        <!-- Loading -->
        <div
          v-if="transfersStore.isLoading && transfersStore.transfers.length === 0"
          style="display: flex; justify-content: center; padding: 48px"
        >
          <div class="pp-spinner"></div>
        </div>

        <!-- Empty -->
        <div
          v-else-if="groupedTransfers.length === 0"
          style="
            margin-top: 24px;
            background: rgba(247, 244, 237, 0.04);
            border: 1px solid rgba(247, 244, 237, 0.09);
            border-radius: 18px;
            padding: 34px 20px;
            text-align: center;
          "
        >
          <div style="font-size: 15px; font-weight: 600; color: #f7f4ed">
            Operatsiyalar topilmadi
          </div>
          <div style="font-size: 13.5px; color: rgba(247, 244, 237, 0.5); margin-top: 6px">
            Qidiruv yoki filtrlarni o'zgartirib ko'ring.
          </div>
        </div>

        <!-- Groups -->
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
                  <div style="font-size: 13px; color: rgba(247, 244, 237, 0.48)">P2P o'tkazma</div>
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
                    {{ tx.direction === 'credit' ? '+' : '−' }}{{ formatAmount(tx.amountUzs) }} UZS
                  </div>
                  <div style="font-size: 12.5px; color: rgba(247, 244, 237, 0.4)">
                    {{ formatTime(tx.processedAt ?? tx.initiatedAt) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
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
            }}{{ formatAmount(selectedTx.amountUzs) }} UZS
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
              selectedTx.feeAmountUzs ? formatAmount(selectedTx.feeAmountUzs) + ' UZS' : '0 UZS'
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
