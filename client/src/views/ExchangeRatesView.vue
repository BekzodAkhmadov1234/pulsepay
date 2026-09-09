<script setup lang="ts">
import { onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useCurrencyStore } from '@/stores/currency';
import type { ExchangeRateDto } from '@/lib/api/currency';

const { t } = useI18n();
const store = useCurrencyStore();

onMounted(() => store.fetchRates());

const FLAG: Record<string, string> = {
  USD: '🇺🇸',
  EUR: '🇪🇺',
  GBP: '🇬🇧',
  CHF: '🇨🇭',
  JPY: '🇯🇵',
};

function formatRate(tiyinStr: string): string {
  const uzs = parseInt(tiyinStr, 10) / 100;
  return new Intl.NumberFormat('uz-UZ', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(uzs);
}

function trend(rate: ExchangeRateDto): 'up' | 'down' | 'flat' {
  const sell = parseInt(rate.sell, 10);
  const prev = parseInt(rate.sell_prev, 10);
  if (sell > prev) return 'up';
  if (sell < prev) return 'down';
  return 'flat';
}

function diffUzs(rate: ExchangeRateDto): string {
  const sell = parseInt(rate.sell, 10) / 100;
  const prev = parseInt(rate.sell_prev, 10) / 100;
  const d = sell - prev;
  if (d === 0) return '';
  return (d > 0 ? '+' : '') + d.toFixed(2);
}
</script>

<template>
  <q-page class="pp-page">
    <div class="pp-main">
      <!-- Header row -->
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          margin-bottom: 28px;
          flex-wrap: wrap;
        "
      >
        <div>
          <div
            style="
              font-size: 13px;
              font-weight: 700;
              letter-spacing: 0.14em;
              text-transform: uppercase;
              color: rgba(247, 244, 237, 0.45);
              margin-bottom: 6px;
            "
          >
            {{ t('currency.subtitle') }}
          </div>
          <h1
            style="
              margin: 0;
              font-family: 'Space Grotesk', sans-serif;
              font-size: clamp(24px, 3vw, 34px);
              font-weight: 700;
              letter-spacing: -0.02em;
            "
          >
            {{ t('currency.title') }}
          </h1>
        </div>
        <button
          class="pp-btn-ghost"
          :disabled="store.isLoading"
          style="display: flex; align-items: center; gap: 8px"
          @click="store.fetchRates()"
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
            :style="store.isLoading ? 'animation: pp-spin 0.8s linear infinite' : ''"
          >
            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
            <path d="M21 3v5h-5" />
            <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
            <path d="M3 21v-5h5" />
          </svg>
          {{ t('currency.refresh') }}
        </button>
      </div>

      <!-- Loading -->
      <div
        v-if="store.isLoading && store.rates.length === 0"
        style="display: flex; justify-content: center; padding: 64px 0"
      >
        <div class="pp-spinner"></div>
      </div>

      <!-- Error -->
      <div
        v-else-if="store.error && store.rates.length === 0"
        style="text-align: center; padding: 64px 0; color: rgba(247, 244, 237, 0.5)"
      >
        <div style="font-size: 32px; margin-bottom: 12px">⚠️</div>
        <div>{{ store.error }}</div>
        <button class="pp-btn-primary" style="margin-top: 20px" @click="store.fetchRates()">
          {{ t('currency.refresh') }}
        </button>
      </div>

      <!-- Rates table -->
      <div v-else-if="store.rates.length > 0" class="pp-section-card">
        <!-- Table header -->
        <div class="cr-header">
          <div class="cr-cell cr-currency">{{ t('currency.currency') }}</div>
          <div class="cr-cell cr-num">{{ t('currency.buy') }}</div>
          <div class="cr-cell cr-num">{{ t('currency.sell') }}</div>
          <div class="cr-cell cr-num cr-trend-col">{{ t('currency.change') }}</div>
        </div>

        <!-- Rows -->
        <div v-for="rate in store.rates" :key="rate.code" class="cr-row">
          <!-- Currency -->
          <div class="cr-cell cr-currency">
            <span class="cr-flag">{{ FLAG[rate.code] ?? '🏳️' }}</span>
            <div>
              <div class="cr-code">{{ rate.code }}</div>
              <div class="cr-name">{{ rate.name }}</div>
            </div>
          </div>

          <!-- Buy -->
          <div class="cr-cell cr-num">
            <div class="cr-rate-val">{{ formatRate(rate.buy) }}</div>
            <div class="cr-label">{{ t('currency.uzs') }}</div>
          </div>

          <!-- Sell -->
          <div class="cr-cell cr-num">
            <div class="cr-rate-val">{{ formatRate(rate.sell) }}</div>
            <div class="cr-label">{{ t('currency.uzs') }}</div>
          </div>

          <!-- Trend -->
          <div class="cr-cell cr-num cr-trend-col">
            <div
              class="cr-trend"
              :class="{
                'cr-trend--up': trend(rate) === 'up',
                'cr-trend--down': trend(rate) === 'down',
                'cr-trend--flat': trend(rate) === 'flat',
              }"
            >
              <!-- Up arrow -->
              <svg
                v-if="trend(rate) === 'up'"
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 19V5M5 12l7-7 7 7" />
              </svg>
              <!-- Down arrow -->
              <svg
                v-else-if="trend(rate) === 'down'"
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 5v14M19 12l-7 7-7-7" />
              </svg>
              <!-- Flat -->
              <svg
                v-else
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M5 12h14" />
              </svg>
              <span>{{ diffUzs(rate) || '0.00' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Source note -->
      <div
        style="
          margin-top: 20px;
          font-size: 12px;
          color: rgba(247, 244, 237, 0.3);
          text-align: right;
        "
      >
        {{ t('currency.source') }}
      </div>
    </div>
  </q-page>
</template>

<style scoped>
.cr-header {
  display: grid;
  grid-template-columns: 1fr repeat(3, minmax(100px, 140px));
  padding: 12px 20px;
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.4);
}

.cr-row {
  display: grid;
  grid-template-columns: 1fr repeat(3, minmax(100px, 140px));
  padding: 18px 20px;
  align-items: center;
  transition: background 0.12s;
}

.cr-row + .cr-row {
  border-top: 1px solid rgba(247, 244, 237, 0.07);
}

.cr-row:hover {
  background: rgba(247, 244, 237, 0.03);
}

.cr-cell {
  display: flex;
  align-items: center;
}

.cr-currency {
  gap: 14px;
}

.cr-num {
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.cr-flag {
  font-size: 26px;
  line-height: 1;
  flex: none;
}

.cr-code {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.cr-name {
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.5);
  margin-top: 2px;
}

.cr-rate-val {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15px;
  font-weight: 600;
}

.cr-label {
  font-size: 11px;
  color: rgba(247, 244, 237, 0.35);
}

.cr-trend-col {
  align-items: flex-end;
}

.cr-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 13.5px;
  font-weight: 600;
}

.cr-trend--up {
  color: #29be8c;
}
.cr-trend--down {
  color: #ff9c82;
}
.cr-trend--flat {
  color: rgba(247, 244, 237, 0.4);
}
</style>
