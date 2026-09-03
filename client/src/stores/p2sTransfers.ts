import { ref } from 'vue';
import { defineStore } from 'pinia';
import { listPaynetProviders, initiateP2STransfer, confirmP2SOtp } from '@/lib/api/p2sTransfers';
import type { PaynetProviderDto, InitiateP2SPayload } from '@/lib/api/p2sTransfers';
import type { TransferDto } from '@/lib/api/transfers';

export const useP2STransfersStore = defineStore('p2sTransfers', () => {
  const isLoading = ref(false);
  const providers = ref<PaynetProviderDto[]>([]);

  async function fetchProviders(): Promise<void> {
    if (providers.value.length > 0) return;
    isLoading.value = true;
    try {
      providers.value = await listPaynetProviders();
    } finally {
      isLoading.value = false;
    }
  }

  async function initiate(payload: InitiateP2SPayload): Promise<TransferDto> {
    isLoading.value = true;
    try {
      return await initiateP2STransfer(payload);
    } finally {
      isLoading.value = false;
    }
  }

  async function confirmOtp(transferId: string, code: string): Promise<TransferDto> {
    isLoading.value = true;
    try {
      return await confirmP2SOtp(transferId, code);
    } finally {
      isLoading.value = false;
    }
  }

  return { isLoading, providers, fetchProviders, initiate, confirmOtp };
});
