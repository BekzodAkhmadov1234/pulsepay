import { defineStore } from 'pinia';
import { ref } from 'vue';
import { initiateA2PTransfer, confirmA2PTransfer } from '@/lib/api/a2pTransfers';
import type { InitiateA2PPayload } from '@/lib/api/a2pTransfers';
import type { TransferDto } from '@/lib/api/transfers';

export const useA2PTransfersStore = defineStore('a2pTransfers', () => {
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  async function pullFromBank(payload: InitiateA2PPayload): Promise<TransferDto> {
    isLoading.value = true;
    error.value = null;
    try {
      return await initiateA2PTransfer(payload);
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Xato yuz berdi';
      throw e;
    } finally {
      isLoading.value = false;
    }
  }

  async function confirmOtp(transferId: string, code: string): Promise<TransferDto> {
    isLoading.value = true;
    error.value = null;
    try {
      return await confirmA2PTransfer(transferId, code);
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'OTP tasdiqlanmadi';
      throw e;
    } finally {
      isLoading.value = false;
    }
  }

  return { isLoading, error, pullFromBank, confirmOtp };
});
