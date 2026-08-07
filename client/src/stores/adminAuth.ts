import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { adminClient } from '@/lib/api/adminClient';
import { getAdminToken, setAdminToken, clearAdminToken } from '@/lib/api/adminClient';
import { decodeJwtPayload, isTokenExpired } from '@/lib/token';

export interface AdminUser {
  email: string;
  role: string;
}

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const admin = ref<AdminUser | null>(null);
  const isLoading = ref(false);

  const isAuthenticated = computed(() => admin.value !== null);

  function _hydrateAdmin(token: string): void {
    const payload = decodeJwtPayload(token);
    if (payload) {
      admin.value = {
        email: (payload.email ?? payload.sub ?? '') as string,
        role: (payload.role ?? 'ADMIN') as string,
      };
    }
  }

  function fetchCurrentAdmin(): void {
    const token = getAdminToken();
    if (token && !isTokenExpired(token)) {
      _hydrateAdmin(token);
    } else if (token) {
      clearAdminToken();
    }
  }

  async function login(email: string, password: string): Promise<void> {
    isLoading.value = true;
    try {
      const res = await adminClient.post<{ accessToken: string }>('/auth/login', {
        email,
        password,
      });
      setAdminToken(res.accessToken);
      _hydrateAdmin(res.accessToken);
    } finally {
      isLoading.value = false;
    }
  }

  function logout(): void {
    clearAdminToken();
    admin.value = null;
  }

  return { admin, isAuthenticated, isLoading, fetchCurrentAdmin, login, logout };
});
