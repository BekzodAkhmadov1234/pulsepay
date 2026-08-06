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
