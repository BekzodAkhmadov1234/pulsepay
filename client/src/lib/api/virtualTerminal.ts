import { merchantClient } from './merchantClient';

export interface MerchantProfileDto {
  id: string;
  legalTradeName: string;
  kybStatus: string;
  status: string;
  email: string;
  createdAt: string;
}

export interface MerchantAccountDto {
  id: string;
  merchantId: string;
  currencyCode: string;
  minPayoutThreshold: number;
  settlementSchedule: string;
  status: string;
  createdAt: string;
}

export interface ChargePayload {
  customerPhone: string;
  customerInstrumentId: string;
  cardNetwork: string;
  amountUzs: number;
  purposeCodeId?: number;
}

export interface ChargeResponse {
  transferId: string;
  status: string;
  amountTiyin: number;
  feeTiyin: number;
  completedAt: string;
}

export interface MerchantTransferSummary {
  id: string;
  amount: { tiyin: number; currency: string };
  feeAmount: { tiyin: number; currency: string };
  status: string;
  channel: string;
  initiatedAt: string;
  completedAt: string | null;
  senderName: string | null;
  senderMaskedPan: string | null;
  direction: string;
}

export interface SettlementBatchDto {
  id: string;
  batchType: string;
  merchantAccountId: string;
  operationalDate: string;
  totalAmount: number;
  status: string;
  generatedAt: string;
  settledAt: string | null;
}

export interface CustomerCardSummary {
  instrumentId: string;
  maskedPan: string;
  cardNetwork: string;
}

export interface CustomerSearchResponse {
  id: string;
  fullName: string;
  phoneE164: string;
  cards: CustomerCardSummary[];
}

export function searchCustomer(phone: string): Promise<CustomerSearchResponse> {
  return merchantClient.get<CustomerSearchResponse>(
    `/customers/search?phone=${encodeURIComponent(phone)}`
  );
}

export function getMerchantProfile(): Promise<MerchantProfileDto> {
  return merchantClient.get<MerchantProfileDto>('/profile');
}

export function getMerchantAccount(): Promise<MerchantAccountDto> {
  return merchantClient.get<MerchantAccountDto>('/account');
}

export function chargeCustomer(payload: ChargePayload): Promise<ChargeResponse> {
  return merchantClient.post<ChargeResponse>('/terminal/charge', payload);
}

export function listMerchantTransfers(): Promise<MerchantTransferSummary[]> {
  return merchantClient.get<MerchantTransferSummary[]>('/transfers');
}

export function listMerchantSettlements(): Promise<SettlementBatchDto[]> {
  return merchantClient.get<SettlementBatchDto[]>('/settlements');
}
