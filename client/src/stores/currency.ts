import { ref } from 'vue';
import { defineStore } from 'pinia';
import { getExchangeRates } from '@/lib/api/currency';
import type { ExchangeRateDto } from '@/lib/api/currency';

export const useCurrencyStore = defineStore('currency', () => {
  const rates = ref<ExchangeRateDto[]>([]);
  const isLoading = ref(false);
  const error = ref('');

  async function fetchRates(): Promise<void> {
    isLoading.value = true;
    error.value = '';
    try {
      rates.value = await getExchangeRates();
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Failed to load rates';
    } finally {
      isLoading.value = false;
    }
  }

  return { rates, isLoading, error, fetchRates };
});
