import { apiClient } from './client';

export interface RecipientCardSummary {
  id: string;
  maskedPan: string;
  cardNetwork: string;
  isDefault: boolean;
}

export interface RecipientDto {
  id: string;
  fullName: string;
  phoneE164: string;
  cards: RecipientCardSummary[];
}

export function searchUserByPhone(phone: string): Promise<RecipientDto> {
  return apiClient.get<RecipientDto>(`/recipients/search?phone=${encodeURIComponent(phone)}`);
}

export function searchUserByCard(cardNumber: string): Promise<RecipientDto> {
  return apiClient.get<RecipientDto>(
    `/recipients/search?cardNumber=${encodeURIComponent(cardNumber)}`
  );
}
