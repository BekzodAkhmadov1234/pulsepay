import { apiClient } from './client';
import type { TransferDto } from './transfers';

export interface PaynetProviderDto {
  id: string;
  serviceCode: string;
  serviceName: string;
  category: string;
  fieldNames: string[];
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

export function listPaynetProviders(): Promise<PaynetProviderDto[]> {
  return apiClient.get<PaynetProviderDto[]>('/paynet/providers');
}

export function validatePrepayment(
  serviceCode: string,
  serviceFields: Record<string, string>
): Promise<PaynetProviderDto> {
  return apiClient.post<PaynetProviderDto>('/paynet/prepayment', { serviceCode, serviceFields });
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
