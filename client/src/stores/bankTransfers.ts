import { ref } from 'vue';
import { defineStore } from 'pinia';
import { initiateBankTransfer } from '@/lib/api/bankTransfers';
import { confirmTransfer } from '@/lib/api/transfers';
import type { InitiateBankTransferPayload } from '@/lib/api/bankTransfers';
import type { TransferDto } from '@/lib/api/transfers';

export const useBankTransfersStore = defineStore('bankTransfers', () => {
  const isLoading = ref(false);

  async function sendToBank(payload: InitiateBankTransferPayload): Promise<TransferDto> {
    isLoading.value = true;
    try {
      return await initiateBankTransfer(payload);
    } finally {
      isLoading.value = false;
    }
  }

  async function confirmOtp(transferId: string, code: string): Promise<TransferDto> {
    isLoading.value = true;
    try {
      return await confirmTransfer(transferId, code);
    } finally {
      isLoading.value = false;
    }
  }

  return { isLoading, sendToBank, confirmOtp };
});
