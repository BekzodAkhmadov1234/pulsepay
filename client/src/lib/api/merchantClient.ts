import { ApiError } from './client';

const MERCHANT_BASE =
  (import.meta.env.VITE_MERCHANT_API_URL as string | undefined) ?? '/merchant/v1';

const MERCHANT_TOKEN_KEY = 'pp_merchant_token';

export function getMerchantToken(): string | null {
  return localStorage.getItem(MERCHANT_TOKEN_KEY);
}
export function setMerchantToken(token: string): void {
  localStorage.setItem(MERCHANT_TOKEN_KEY, token);
}
export function clearMerchantToken(): void {
  localStorage.removeItem(MERCHANT_TOKEN_KEY);
}

async function merchantRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getMerchantToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  };

  const res = await fetch(`${MERCHANT_BASE}${path}`, { ...options, headers });

  if (res.status === 204) return undefined as T;

  const body: unknown = await res.json().catch(() => ({}));

  if (!res.ok) {
    const err = body as { message?: string; error?: string };
    const message = err.message ?? err.error ?? `Request failed with status ${res.status}`;
    if (res.status === 401 || res.status === 403) {
      clearMerchantToken();
      window.location.href = '/merchant/login';
    }
    throw new ApiError(res.status, String(res.status), message);
  }

  return body as T;
}

export const merchantClient = {
  get: <T>(path: string) => merchantRequest<T>(path),
  post: <T>(path: string, data: unknown) =>
    merchantRequest<T>(path, { method: 'POST', body: JSON.stringify(data) }),
  patch: <T>(path: string, data?: unknown) =>
    merchantRequest<T>(path, {
      method: 'PATCH',
      body: data != null ? JSON.stringify(data) : undefined,
    }),
};
