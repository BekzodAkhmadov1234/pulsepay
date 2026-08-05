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

export function login(payload: LoginPayload): Promise<TokenResponse> {
  return apiClient.post<TokenResponse>('/auth/login', payload);
}
