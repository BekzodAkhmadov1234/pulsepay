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
  cardsStore.cards.length === 0 ? '— UZS' : formatAmount(totalBalanceUzs.value) + ' UZS'
);

function isCurrentMonth(tx: TransferDto): boolean {
  const iso = tx.processedAt ?? tx.initiatedAt;
  if (!iso) return false;
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  if (isNaN(d.getTime())) return false;
  const now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
}

const monthlyIncome = computed(() =>
  transfersStore.transfers
    .filter((t) => t.direction === 'credit' && isCurrentMonth(t))
    .reduce((sum, t) => sum + t.amountUzs, 0)
);

const monthlyExpenses = computed(() =>
  transfersStore.transfers
    .filter((t) => t.direction === 'debit' && isCurrentMonth(t))
    .reduce((sum, t) => sum + t.amountUzs, 0)
);

function formatAmount(uzs: number) {
  return new Intl.NumberFormat('uz-UZ', { style: 'decimal', maximumFractionDigits: 0 }).format(uzs);
}
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- ── Top row: balance + monthly stats ── -->
      <div style="display: flex; flex-wrap: wrap; gap: 16px; align-items: stretch">
        <!-- Balance card -->
        <div
          style="
            position: relative;
            overflow: hidden;
            flex: 3 1 420px;
            min-width: 0;
            background: linear-gradient(
              135deg,
              rgba(41, 190, 140, 0.14) 0%,
              rgba(247, 244, 237, 0.05) 100%
            );
            border: 1px solid rgba(41, 190, 140, 0.28);
            border-radius: 22px;
            padding: clamp(24px, 3vw, 34px);
          "
        >
          <div
            style="
              position: absolute;
              width: 420px;
              height: 420px;
              right: -160px;
              top: -220px;
              border-radius: 50%;
              background: radial-gradient(
                circle,
                rgba(41, 190, 140, 0.22) 0%,
                rgba(41, 190, 140, 0) 68%
              );
              pointer-events: none;
            "
          ></div>

          <div
            style="
              position: relative;
              display: flex;
              align-items: center;
              justify-content: space-between;
              gap: 16px;
            "
          >
            <div
              style="
                font-size: 12px;
                font-weight: 700;
                letter-spacing: 0.16em;
                text-transform: uppercase;
                color: #6fd8a8;
              "
            >
              Umumiy balans
            </div>
            <button
              style="
                display: flex;
                align-items: center;
                gap: 8px;
                background: none;
                border: 1px solid rgba(247, 244, 237, 0.16);
                border-radius: 999px;
                padding: 7px 12px;
                font-family: Manrope, sans-serif;
                font-size: 12.5px;
                font-weight: 600;
                color: rgba(247, 244, 237, 0.7);
                cursor: pointer;
                transition:
                  background 0.15s,
                  color 0.15s;
              "
              @click="balanceVisible = !balanceVisible"
            >
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
                <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              {{ balanceVisible ? 'Yashirish' : "Ko'rsatish" }}
            </button>
          </div>

          <div
            style="
              position: relative;
              margin-top: 14px;
              font-family: 'Space Grotesk', sans-serif;
              font-size: clamp(34px, 4.6vw, 52px);
              font-weight: 600;
              letter-spacing: -0.03em;
              line-height: 1;
            "
          >
            {{ balanceVisible ? balanceDisplay : '•••••••• UZS' }}
          </div>

          <div
            style="
              position: relative;
              display: flex;
              align-items: center;
              gap: 12px;
              margin-top: 22px;
              padding-top: 20px;
              border-top: 1px solid rgba(247, 244, 237, 0.12);
            "
          >
            <div
              style="
                flex: none;
                width: 38px;
                height: 38px;
                border-radius: 12px;
                background: rgba(41, 190, 140, 0.16);
                display: flex;
                align-items: center;
                justify-content: center;
              "
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#29BE8C"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2"></path>
                <path d="M3 8v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3h-4a2 2 0 0 1 0-4h4"></path>
              </svg>
            </div>
            <div style="flex: 1; min-width: 0">
              <div style="font-size: 14.5px; font-weight: 600">Mening hamyonim</div>
              <div style="font-size: 12.5px; color: rgba(247, 244, 237, 0.5); margin-top: 2px">
                PulsePay hisobi
              </div>
            </div>
            <div
              style="
                flex: none;
                font-family: 'Space Grotesk', sans-serif;
                font-size: 19px;
                font-weight: 600;
                letter-spacing: -0.015em;
              "
            >
              {{ balanceVisible ? '— UZS' : '•••••• UZS' }}
            </div>
          </div>
        </div>

        <!-- Monthly stats card -->
        <div
          style="
            flex: 1 1 260px;
            min-width: 0;
            background: rgba(247, 244, 237, 0.045);
            border: 1px solid rgba(247, 244, 237, 0.1);
            border-radius: 22px;
            padding: clamp(20px, 2.4vw, 28px);
            display: flex;
            flex-direction: column;
            gap: 20px;
          "
        >
          <div
            style="
              font-size: 12px;
              font-weight: 700;
              letter-spacing: 0.16em;
              text-transform: uppercase;
              color: rgba(247, 244, 237, 0.45);
            "
          >
            Bu oyda
          </div>
          <div style="display: flex; flex-direction: column; gap: 6px">
            <div style="font-size: 13.5px; color: rgba(247, 244, 237, 0.6)">Chiqim</div>
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 24px;
                font-weight: 600;
                letter-spacing: -0.02em;
                color: #ff9c82;
              "
            >
              {{ monthlyExpenses > 0 ? '−' + formatAmount(monthlyExpenses) + ' UZS' : '0 UZS' }}
            </div>
          </div>
          <div style="height: 1px; background: rgba(247, 244, 237, 0.1)"></div>
          <div style="display: flex; flex-direction: column; gap: 6px">
            <div style="font-size: 13.5px; color: rgba(247, 244, 237, 0.6)">Kirim</div>
            <div
              style="
                font-family: 'Space Grotesk', sans-serif;
                font-size: 24px;
                font-weight: 600;
                letter-spacing: -0.02em;
                color: #29be8c;
              "
            >
              {{ monthlyIncome > 0 ? '+' + formatAmount(monthlyIncome) + ' UZS' : '0 UZS' }}
            </div>
          </div>
        </div>
      </div>

      <!-- ── Quick actions ── -->
      <div style="margin-top: 24px; display: flex; flex-wrap: wrap; gap: 10px">
        <button
          class="pp-btn-ghost"
          style="font-size: 13.5px; padding: 11px 18px"
          @click="router.push('/send')"
        >
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
            <path d="M8 21V5m0 16-3.5-3.5M8 5l3.5 3.5M16 3v16m0 0 3.5-3.5M16 19l-3.5-3.5"></path>
          </svg>
          O'tkazma
        </button>
        <button
          class="pp-btn-ghost"
          style="font-size: 13.5px; padding: 11px 18px"
          @click="router.push('/send/bank')"
        >
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
            <path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <path d="M3 11h18"></path>
          </svg>
          Bankka o'tkazma
        </button>
        <button
          class="pp-btn-ghost"
          style="font-size: 13.5px; padding: 11px 18px"
          @click="router.push('/top-up')"
        >
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
            <path d="M12 5v14M5 12l7-7 7 7"></path>
          </svg>
          Karta to'ldirish
        </button>
        <button
          class="pp-btn-ghost"
          style="font-size: 13.5px; padding: 11px 18px"
          @click="router.push('/pay/utility')"
        >
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
            <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"></path>
          </svg>
          Kommunal to'lovlar
        </button>
      </div>
    </div>
  </q-page>
</template>
