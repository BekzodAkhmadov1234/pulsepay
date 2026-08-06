import { apiClient } from './client';

export interface TransferDto {
  id: string;
  amountUzs: number;
  feeAmountUzs: number;
  status: string;
  channel: string;
  idempotencyKey: string;
  initiatedAt: string;
  completedAt: string | null;
  senderName: string | null;
  senderMaskedPan: string | null;
  recipientName: string | null;
  recipientMaskedPan: string | null;
  processedAt: string | null;
  direction: 'debit' | 'credit' | null;
}

export interface InitiateTransferPayload {
  senderInstrumentId: string;
  senderCardNetwork: string;
  recipientId: string;
  recipientInstrumentId: string;
  recipientCardNetwork: string;
  amountUzs: number;
  transferTypeId: number;
  purposeCodeId?: number;
  channel: string;
  idempotencyKey: string;
}

export function initiateTransfer(payload: InitiateTransferPayload): Promise<TransferDto> {
  return apiClient.post<TransferDto>('/transfers', payload);
}

export function confirmTransfer(id: string, code: string): Promise<TransferDto> {
  return apiClient.patch<TransferDto>(`/transfers/${id}/otp`, { code });
}

export function listTransfers(): Promise<TransferDto[]> {
  return apiClient.get<TransferDto[]>('/transfers');
}
