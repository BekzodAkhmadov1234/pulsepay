import { apiClient } from './client';

export interface CardDto {
  id: string;
  maskedPan: string;
  cardNetwork: 'uzcard' | 'humo' | null;
  cardHolderName: string;
  expMonth: number;
  expYear: number;
  status: 'UNVERIFIED' | 'VERIFIED' | 'INACTIVE' | 'EXPIRED' | 'BLOCKED';
  isDefault: boolean;
  verifiedAt: string | null;
  balanceUzs: number;
}

export interface AddCardPayload {
  maskedPan: string;
  cardToken: string;
  cardHolderName: string;
  expMonth: number;
  expYear: number;
}

export interface StatementEntry {
  date: string;
  description: string;
  amountUzs: number;
  type: 'debit' | 'credit';
}

export interface LimitTypeDto {
  code: string;
  type: string;
  name: string;
  selectDate: boolean;
}

export interface CardLimitDto {
  type: string;
  name: string;
  valueUzs: number;
  from: string;
  to: string;
}

export interface LimitInput {
  type: string;
  valueTiyin: number;
  dateFrom?: string;
  dateTo?: string;
}

// ── Basic CRUD ─────────────────────────────────────────────────────────────

export function listCards(): Promise<CardDto[]> {
  return apiClient.get<CardDto[]>('/cards');
}

export function addCard(payload: AddCardPayload): Promise<CardDto> {
  return apiClient.post<CardDto>('/cards', payload);
}

export function removeCard(cardId: string): Promise<void> {
  return apiClient.delete<void>(`/cards/${cardId}`);
}

export function setDefaultCard(cardId: string): Promise<CardDto> {
  return apiClient.patch<CardDto>(`/cards/${cardId}/default`, {});
}

// ── Block / Unblock ────────────────────────────────────────────────────────

export function blockCard(cardId: string): Promise<CardDto> {
  return apiClient.post<CardDto>(`/cards/${cardId}/block`, {});
}

export function unblockCard(cardId: string): Promise<CardDto> {
  return apiClient.post<CardDto>(`/cards/${cardId}/unblock`, {});
}

// ── Statement ──────────────────────────────────────────────────────────────

export function getCardStatement(
  cardId: string,
  params?: { startDate?: string; endDate?: string }
): Promise<StatementEntry[]> {
  let path = `/cards/${cardId}/statement`;
  if (params) {
    const q = new URLSearchParams();
    if (params.startDate) q.set('startDate', params.startDate);
    if (params.endDate) q.set('endDate', params.endDate);
    if (q.toString()) path += '?' + q.toString();
  }
  return apiClient.get<StatementEntry[]>(path);
}

// ── Limits ─────────────────────────────────────────────────────────────────

export function getLimitTypes(): Promise<LimitTypeDto[]> {
  return apiClient.get<LimitTypeDto[]>('/cards/limit-types');
}

export function getCardLimits(cardId: string): Promise<CardLimitDto[]> {
  return apiClient.get<CardLimitDto[]>(`/cards/${cardId}/limits`);
}

export function setCardLimits(cardId: string, limits: LimitInput[]): Promise<CardLimitDto[]> {
  return apiClient.post<CardLimitDto[]>(`/cards/${cardId}/limits`, { limits });
}

export function removeCardLimit(cardId: string, limitType: string): Promise<void> {
  return apiClient.delete<void>(`/cards/${cardId}/limits/${limitType}`);
}

// ── PIN change ─────────────────────────────────────────────────────────────

export function setCardPin(cardId: string, network: 'humo' | 'uzcard', pin: string): Promise<void> {
  return apiClient.post<void>(`/cards/${cardId}/pin/${network}`, { pin });
}
