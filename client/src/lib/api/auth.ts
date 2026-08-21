import { apiClient } from './client';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterPayload {
  phoneE164: string;
  fullName: string;
}

export interface LoginPayload {
  phoneE164: string;
}

export function register(payload: RegisterPayload): Promise<TokenResponse> {
  return apiClient.post<TokenResponse>('/auth/register', payload);
}

/** Step 1 – ask backend to dispatch an OTP to the phone */
export function requestOtp(phoneE164: string): Promise<void> {
  return apiClient.post<void>('/auth/otp', { phoneE164 });
}

/** Step 2 (dev only) – retrieve the OTP code the backend just generated */
export function getDevOtp(phoneE164: string): Promise<{ code: string }> {
  return apiClient.get<{ code: string }>(`/dev/otp/${encodeURIComponent(phoneE164)}`);
}

/** Step 3 – verify the OTP and receive a JWT */
export function verifyOtp(phoneE164: string, code: string): Promise<TokenResponse> {
  return apiClient.post<TokenResponse>('/auth/verify', {
    phoneE164,
    code,
    deviceFingerprint: 'web-mock-device',
    platform: 'android',
  });
}
