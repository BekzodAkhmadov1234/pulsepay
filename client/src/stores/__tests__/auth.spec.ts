import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '../auth';
import * as authApi from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';

vi.mock('@/lib/api/auth');

// Minimal JWT: header.payload.signature
// payload contains phone_e164, kyc_level and a far-future exp
const PAYLOAD = btoa(
  JSON.stringify({
    sub: '+998901234567',
    phone_e164: '+998901234567',
    kyc_level: 'basic',
    exp: 9_999_999_999,
  })
);
const MOCK_TOKEN = `eyJhbGciOiJIUzI1NiJ9.${PAYLOAD}.signature`;

const MOCK_RESPONSE: authApi.TokenResponse = {
  accessToken: MOCK_TOKEN,
  tokenType: 'Bearer',
  expiresIn: 900,
};

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('starts unauthenticated', () => {
    const auth = useAuthStore();
    expect(auth.isAuthenticated).toBe(false);
    expect(auth.user).toBeNull();
  });

  it('register: stores token and hydrates user on success', async () => {
    vi.mocked(authApi.register).mockResolvedValue(MOCK_RESPONSE);

    const auth = useAuthStore();
    await auth.register({ phoneE164: '+998901234567', fullName: 'Test User' });

    expect(auth.isAuthenticated).toBe(true);
    expect(auth.user?.phoneE164).toBe('+998901234567');
    expect(auth.user?.kycLevel).toBe('basic');
    expect(localStorage.getItem('pp_access_token')).toBe(MOCK_TOKEN);
  });

  it('login: stores token and hydrates user on success', async () => {
    vi.mocked(authApi.login).mockResolvedValue(MOCK_RESPONSE);

    const auth = useAuthStore();
    await auth.login({ phoneE164: '+998901234567' });

    expect(auth.isAuthenticated).toBe(true);
    expect(auth.user?.phoneE164).toBe('+998901234567');
    expect(localStorage.getItem('pp_access_token')).toBe(MOCK_TOKEN);
  });

  it('logout: clears user and token', async () => {
    vi.mocked(authApi.login).mockResolvedValue(MOCK_RESPONSE);

    const auth = useAuthStore();
    await auth.login({ phoneE164: '+998901234567' });
    auth.logout();

    expect(auth.isAuthenticated).toBe(false);
    expect(auth.user).toBeNull();
    expect(localStorage.getItem('pp_access_token')).toBeNull();
  });

  it('login: propagates ApiError and leaves user unauthenticated', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(400, '400', 'Invalid or expired OTP'));

    const auth = useAuthStore();
    await expect(auth.login({ phoneE164: '+998901234567' })).rejects.toBeInstanceOf(ApiError);

    expect(auth.isAuthenticated).toBe(false);
    expect(auth.isLoading).toBe(false);
  });

  it('fetchCurrentUser: restores session from valid token in localStorage', () => {
    localStorage.setItem('pp_access_token', MOCK_TOKEN);

    const auth = useAuthStore();
    auth.fetchCurrentUser();

    expect(auth.isAuthenticated).toBe(true);
    expect(auth.user?.phoneE164).toBe('+998901234567');
  });

  it('fetchCurrentUser: clears expired token and stays unauthenticated', () => {
    const expiredPayload = btoa(
      JSON.stringify({ sub: '+998901234567', phone_e164: '+998901234567', exp: 1 })
    );
    const expiredToken = `eyJhbGciOiJIUzI1NiJ9.${expiredPayload}.sig`;
    localStorage.setItem('pp_access_token', expiredToken);

    const auth = useAuthStore();
    auth.fetchCurrentUser();

    expect(auth.isAuthenticated).toBe(false);
    expect(localStorage.getItem('pp_access_token')).toBeNull();
  });
});
