import { apiClient } from './client';
import type { TransferDto } from './transfers';

export interface PaynetProviderDto {
  id: string;
  serviceCode: string;
  serviceName: string;
  category: string;
  fieldNames: string[];
}

export interface PaynetCategoryDto {
  category: string;
  displayName: string;
  providerCount: number;
}

export interface InitiateP2SPayload {
  senderInstrumentId: string;
  senderCardNetwork: string;
  serviceCode: string;
  serviceFields: Record<string, string>;
  amountUzs: number;
  purposeCodeId?: number;
  channel: string;
  idempotencyKey: string;
}

export function listPaynetProviders(category?: string): Promise<PaynetProviderDto[]> {
  const params = category ? `?category=${encodeURIComponent(category)}` : '';
  return apiClient.get<PaynetProviderDto[]>(`/paynet/providers${params}`);
}

export function listPaynetCategories(): Promise<PaynetCategoryDto[]> {
  return apiClient.get<PaynetCategoryDto[]>('/paynet/categories');
}

export function searchPaynetProviders(q: string): Promise<PaynetProviderDto[]> {
  return apiClient.get<PaynetProviderDto[]>(`/paynet/providers/search?q=${encodeURIComponent(q)}`);
}

export function popularPaynetProviders(count = 5): Promise<PaynetProviderDto[]> {
  return apiClient.get<PaynetProviderDto[]>(`/paynet/providers/popular?count=${count}`);
}

export function validatePrepayment(
  serviceCode: string,
  serviceFields: Record<string, string>
): Promise<PaynetProviderDto> {
  return apiClient.post<PaynetProviderDto>('/paynet/prepayment', { serviceCode, serviceFields });
}

export function mobileTopUp(phone: string, serviceCode?: string): Promise<PaynetProviderDto> {
  return apiClient.post<PaynetProviderDto>('/paynet/mobile', { phone, serviceCode });
}

export function initiateP2STransfer(payload: InitiateP2SPayload): Promise<TransferDto> {
  return apiClient.post<TransferDto>('/p2s-transfers', payload);
}

export function confirmP2SOtp(transferId: string, code: string): Promise<TransferDto> {
  return apiClient.patch<TransferDto>(`/p2s-transfers/${transferId}/otp`, { code });
}

export function getP2STransfer(id: string): Promise<TransferDto> {
  return apiClient.get<TransferDto>(`/p2s-transfers/${id}`);
}
