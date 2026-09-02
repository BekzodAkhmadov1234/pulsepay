import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { register as apiRegister, requestOtp, getDevOtp, verifyOtp } from '@/lib/api/auth';
import type { RegisterPayload, LoginPayload } from '@/lib/api/auth';
import { getToken, setToken, clearToken, decodeJwtPayload, isTokenExpired } from '@/lib/token';

export interface AuthUser {
  id: string;
  phoneE164: string;
  fullName: string;
  kycLevel: string;
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null);
  const isLoading = ref(false);

  const isAuthenticated = computed(() => user.value !== null);

  function _hydrateUser(token: string): void {
    const payload = decodeJwtPayload(token);
    if (payload) {
      user.value = {
        id: (payload.sub ?? '') as string,
        phoneE164: (payload.phone_e164 ?? payload.sub ?? '') as string,
        fullName: (payload.full_name ?? '') as string,
        kycLevel: (payload.kyc_level ?? 'basic') as string,
      };
    }
  }

  /** Restore session from localStorage on app load. Call once before mounting. */
  function fetchCurrentUser(): void {
    const token = getToken();
    if (token && !isTokenExpired(token)) {
      _hydrateUser(token);
    } else if (token) {
      clearToken();
    }
  }

  async function register(payload: RegisterPayload): Promise<void> {
    isLoading.value = true;
    try {
      // Create account, then auto-verify phone via mock OTP (dev flow)
      await apiRegister(payload);
      await requestOtp(payload.phoneE164);
      const { code } = await getDevOtp(payload.phoneE164);
      const res = await verifyOtp(payload.phoneE164, code);
      setToken(res.accessToken);
      _hydrateUser(res.accessToken);
    } finally {
      isLoading.value = false;
    }
  }

  async function login(payload: LoginPayload): Promise<void> {
    isLoading.value = true;
    try {
      // Request OTP, auto-fetch from dev endpoint, then verify — transparent to the user
      await requestOtp(payload.phoneE164);
      const { code } = await getDevOtp(payload.phoneE164);
      const res = await verifyOtp(payload.phoneE164, code);
      setToken(res.accessToken);
      _hydrateUser(res.accessToken);
    } finally {
      isLoading.value = false;
    }
  }

  function logout(): void {
    clearToken();
    user.value = null;
  }

  return { user, isAuthenticated, isLoading, fetchCurrentUser, register, login, logout };
});
