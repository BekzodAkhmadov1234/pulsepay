import { ApiError } from './client';

const ADMIN_BASE =
  (import.meta.env.VITE_ADMIN_API_URL as string | undefined) ?? 'http://localhost:8080/admin/v1';

const ADMIN_TOKEN_KEY = 'pp_admin_token';

export function getAdminToken(): string | null {
  return localStorage.getItem(ADMIN_TOKEN_KEY);
}
export function setAdminToken(token: string): void {
  localStorage.setItem(ADMIN_TOKEN_KEY, token);
}
export function clearAdminToken(): void {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
}

async function adminRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAdminToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  };

  const res = await fetch(`${ADMIN_BASE}${path}`, { ...options, headers });

  if (res.status === 204) return undefined as T;

  const body: unknown = await res.json().catch(() => ({}));

  if (!res.ok) {
    const err = body as { message?: string; error?: string };
    const message = err.message ?? err.error ?? `Request failed with status ${res.status}`;
    if (res.status === 401) {
      clearAdminToken();
      window.location.href = '/admin/login';
    }
    throw new ApiError(res.status, String(res.status), message);
  }

  return body as T;
}

export const adminClient = {
  get: <T>(path: string) => adminRequest<T>(path),
  post: <T>(path: string, data: unknown) =>
    adminRequest<T>(path, { method: 'POST', body: JSON.stringify(data) }),
  put: <T>(path: string, data: unknown) =>
    adminRequest<T>(path, { method: 'PUT', body: JSON.stringify(data) }),
  patch: <T>(path: string, data?: unknown) =>
    adminRequest<T>(path, {
      method: 'PATCH',
      body: data != null ? JSON.stringify(data) : undefined,
    }),
  delete: <T>(path: string) => adminRequest<T>(path, { method: 'DELETE' }),
};
