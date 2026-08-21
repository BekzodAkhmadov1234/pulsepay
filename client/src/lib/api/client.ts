import { getToken, clearToken } from '../token';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? '/api/v1';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  };

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 204) {
    return undefined as T;
  }
  // 202 may have a body (e.g. transfer initiation), fall through to parse it.

  const body: unknown = await res.json().catch(() => ({}));

  if (!res.ok) {
    const err = body as { message?: string; error?: string };
    const message = err.message ?? err.error ?? `Request failed with status ${res.status}`;
    if (res.status === 401) {
      clearToken();
      window.location.href = '/login';
    }
    throw new ApiError(res.status, String(res.status), message);
  }

  return body as T;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(data) }),
  patch: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
