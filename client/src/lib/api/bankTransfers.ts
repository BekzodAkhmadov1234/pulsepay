import { apiClient } from './client';
import type { TransferDto } from './transfers';

export interface InitiateBankTransferPayload {
  senderInstrumentId: string;
  senderCardNetwork: string;
  recipientIban: string;
  recipientBankId: string;
  recipientAccountHolderName: string;
  amountUzs: number;
  purposeCodeId?: number;
  channel: string;
  idempotencyKey: string;
}

export function initiateBankTransfer(payload: InitiateBankTransferPayload): Promise<TransferDto> {
  return apiClient.post<TransferDto>('/bank-transfers', payload);
}

export function getBankTransfer(id: string): Promise<TransferDto> {
  return apiClient.get<TransferDto>(`/bank-transfers/${id}`);
}
