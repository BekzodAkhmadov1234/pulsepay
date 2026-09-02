import { apiClient } from './client';
import type { TransferDto } from './transfers';

export interface InitiateA2PPayload {
  sourceIban: string;
  sourceBankId: string;
  sourceAccountHolderName: string;
  destinationInstrumentId: string;
  destinationCardNetwork: string;
  amountUzs: number;
  purposeCodeId?: number;
  channel: string;
  idempotencyKey: string;
}

export function initiateA2PTransfer(payload: InitiateA2PPayload): Promise<TransferDto> {
  return apiClient.post<TransferDto>('/a2p-transfers', payload);
}

export function confirmA2PTransfer(id: string, code: string): Promise<TransferDto> {
  return apiClient.patch<TransferDto>(`/a2p-transfers/${id}/otp`, { code });
}

export function getA2PTransfer(id: string): Promise<TransferDto> {
  return apiClient.get<TransferDto>(`/a2p-transfers/${id}`);
}
