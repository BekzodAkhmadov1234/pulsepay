import { ref } from 'vue';
import { defineStore } from 'pinia';
import { listTransfers, initiateTransfer, confirmTransfer } from '@/lib/api/transfers';
import type { TransferDto, InitiateTransferPayload } from '@/lib/api/transfers';
import { ApiError } from '@/lib/api/client';

export const useTransfersStore = defineStore('transfers', () => {
  const transfers = ref<TransferDto[]>([]);
  const isLoading = ref(false);
  const fetchError = ref<string | null>(null);

  async function fetchTransfers() {
    isLoading.value = true;
    fetchError.value = null;
    try {
      transfers.value = await listTransfers();
    } catch (err) {
      fetchError.value = err instanceof ApiError ? err.message : 'Failed to load transfers.';
    } finally {
      isLoading.value = false;
    }
  }

  async function sendMoney(payload: InitiateTransferPayload): Promise<TransferDto> {
    isLoading.value = true;
    try {
      const initiated = await initiateTransfer(payload);
      // Auto-confirm: skip OTP in stub phase — backend accepts any 6-digit code
      const completed = await confirmTransfer(initiated.id, '000000');
      transfers.value = [completed, ...transfers.value];
      return completed;
    } finally {
      isLoading.value = false;
    }
  }

  return { transfers, isLoading, fetchError, fetchTransfers, sendMoney };
});
