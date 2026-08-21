import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import {
  merchantClient,
  getMerchantToken,
  setMerchantToken,
  clearMerchantToken,
} from '@/lib/api/merchantClient';
import { decodeJwtPayload, isTokenExpired } from '@/lib/token';

export interface MerchantUser {
  id: string;
  email: string;
}

export const useMerchantAuthStore = defineStore('merchantAuth', () => {
  const merchant = ref<MerchantUser | null>(null);
  const isLoading = ref(false);

  const isAuthenticated = computed(() => merchant.value !== null);

  function _hydrateMerchant(token: string): void {
    const payload = decodeJwtPayload(token);
    if (payload) {
      merchant.value = {
        id: (payload.sub ?? '') as string,
        email: (payload.email ?? '') as string,
      };
    }
  }

  function fetchCurrentMerchant(): void {
    const token = getMerchantToken();
    if (token && !isTokenExpired(token)) {
      _hydrateMerchant(token);
    } else if (token) {
      clearMerchantToken();
    }
  }

  async function login(email: string, password: string): Promise<void> {
    isLoading.value = true;
    try {
      const res = await merchantClient.post<{ accessToken: string }>('/auth/login', {
        email,
        password,
      });
      setMerchantToken(res.accessToken);
      _hydrateMerchant(res.accessToken);
    } finally {
      isLoading.value = false;
    }
  }

  function logout(): void {
    clearMerchantToken();
    merchant.value = null;
  }

  return { merchant, isAuthenticated, isLoading, fetchCurrentMerchant, login, logout };
});
