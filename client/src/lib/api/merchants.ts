import { adminClient } from './adminClient';

export interface MerchantDto {
  id: string;
  legalTradeName: string;
  categoryId: number | null;
  acquiringBankId: string | null;
  kybStatus: 'pending' | 'under_review' | 'verified' | 'rejected';
  status: 'pending' | 'active' | 'suspended' | 'closed';
  uzqrEnabled: boolean;
  email: string;
  createdAt: string;
}

export interface OnboardMerchantPayload {
  legalTradeName: string;
  mccCode: string;
  acquiringBankId?: string | null;
  email: string;
  password: string;
}

export interface MerchantCategoryDto {
  id: number;
  mccCode: string;
  nameUz: string;
  riskTier: string;
}

export function listMerchantCategories(): Promise<MerchantCategoryDto[]> {
  return adminClient.get<MerchantCategoryDto[]>('/merchants/categories');
}

export function listMerchants(): Promise<MerchantDto[]> {
  return adminClient.get<MerchantDto[]>('/merchants');
}

export function getMerchant(id: string): Promise<MerchantDto> {
  return adminClient.get<MerchantDto>(`/merchants/${id}`);
}

export function onboardMerchant(payload: OnboardMerchantPayload): Promise<MerchantDto> {
  return adminClient.post<MerchantDto>('/merchants', payload);
}

export function approveMerchant(id: string): Promise<MerchantDto> {
  return adminClient.post<MerchantDto>(`/merchants/${id}/approve`, {});
}

export function rejectMerchant(id: string, reason?: string): Promise<MerchantDto> {
  return adminClient.post<MerchantDto>(`/merchants/${id}/reject`, { reason });
}

export function suspendMerchant(id: string, reason?: string): Promise<MerchantDto> {
  return adminClient.post<MerchantDto>(`/merchants/${id}/suspend`, { reason });
}
