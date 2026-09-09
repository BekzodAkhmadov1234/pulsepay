import { apiClient } from './client';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginPayload {
  phoneE164: string;
}

// ── Registration (two-step OTP flow) ──────────────────────────────────────

/** Step 1 — create pending user and send REGISTRATION OTP */
export function registerOtp(phoneE164: string): Promise<void> {
  return apiClient.post<void>('/auth/register/otp', { phoneE164 });
}

/** Step 2 — verify OTP, set full name, activate account, get JWT */
export function registerConfirm(
  phoneE164: string,
  code: string,
  fullName: string
): Promise<TokenResponse> {
  return apiClient.post<TokenResponse>('/auth/register/confirm', {
    phoneE164,
    code,
    fullName,
    platform: 'web',
  });
}

// ── Login (existing OTP flow) ──────────────────────────────────────────────

/** Ask backend to dispatch a LOGIN OTP to the phone */
export function requestOtp(phoneE164: string): Promise<void> {
  return apiClient.post<void>('/auth/otp', { phoneE164 });
}

/** (dev only) retrieve the OTP code the backend just generated */
export function getDevOtp(phoneE164: string): Promise<{ code: string }> {
  return apiClient.get<{ code: string }>(`/dev/otp/${encodeURIComponent(phoneE164)}`);
}

/** Verify login OTP and receive a JWT */
export function verifyOtp(phoneE164: string, code: string): Promise<TokenResponse> {
  return apiClient.post<TokenResponse>('/auth/verify', {
    phoneE164,
    code,
    deviceFingerprint: 'web-mock-device',
    platform: 'web',
  });
}

// ── Account self-deletion (auth required) ─────────────────────────────────

/** Step 1 — send ACCOUNT_DELETE OTP to the authenticated user's phone */
export function closeAccountOtp(): Promise<void> {
  return apiClient.post<void>('/user/close/otp', {});
}

/** Step 2 — verify OTP and permanently close the account */
export function closeAccountConfirm(code: string): Promise<void> {
  return apiClient.post<void>('/user/close/confirm', { code });
}
