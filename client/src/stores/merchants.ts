import { ref } from 'vue';
import { defineStore } from 'pinia';
import type { MerchantDto, OnboardMerchantPayload } from '@/lib/api/merchants';
import {
  listMerchants,
  onboardMerchant,
  approveMerchant,
  rejectMerchant,
  suspendMerchant,
} from '@/lib/api/merchants';

export const useMerchantsStore = defineStore('merchants', () => {
  const merchants = ref<MerchantDto[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  async function load(): Promise<void> {
    isLoading.value = true;
    error.value = null;
    try {
      merchants.value = await listMerchants();
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load merchants';
    } finally {
      isLoading.value = false;
    }
  }

  async function create(payload: OnboardMerchantPayload): Promise<MerchantDto> {
    const merchant = await onboardMerchant(payload);
    merchants.value = [merchant, ...merchants.value];
    return merchant;
  }

  async function approve(id: string): Promise<void> {
    const updated = await approveMerchant(id);
    _replace(updated);
  }

  async function reject(id: string, reason?: string): Promise<void> {
    const updated = await rejectMerchant(id, reason);
    _replace(updated);
  }

  async function suspend(id: string, reason?: string): Promise<void> {
    const updated = await suspendMerchant(id, reason);
    _replace(updated);
  }

  function _replace(updated: MerchantDto): void {
    merchants.value = merchants.value.map((m) => (m.id === updated.id ? updated : m));
  }

  return { merchants, isLoading, error, load, create, approve, reject, suspend };
});
