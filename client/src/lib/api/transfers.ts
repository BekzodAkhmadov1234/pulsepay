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
  transferTypeId: number | null;
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

export function fetchDevOtp(phoneE164: string): Promise<{ code: string }> {
  return apiClient.get<{ code: string }>(`/dev/otp/${encodeURIComponent(phoneE164)}`);
}

export function previewFee(params: {
  amountUzs: number;
  sourceNetwork: string;
  destNetwork: string;
  transferTypeId?: number;
}): Promise<{ feeAmountUzs: number }> {
  const q = new URLSearchParams({
    amountUzs: String(params.amountUzs),
    sourceNetwork: params.sourceNetwork,
    destNetwork: params.destNetwork,
    transferTypeId: String(params.transferTypeId ?? 1),
  });
  return apiClient.get<{ feeAmountUzs: number }>(`/transfers/fee-preview?${q}`);
}
