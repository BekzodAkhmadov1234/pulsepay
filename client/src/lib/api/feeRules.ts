import { adminClient } from './adminClient';

export type FeeType = 'FIXED' | 'PERCENTAGE' | 'TIERED';
export type FeePayer = 'SENDER' | 'RECIPIENT' | 'MERCHANT' | 'BUSINESS';
export type FeeRecipient = 'PLATFORM' | 'NETWORK' | 'BANK';

export interface FeeRuleTierResponse {
  id: string;
  feeRuleId: string;
  tierMinAmount: number;
  tierMaxAmount: number | null;
  fixedAmount: number | null;
  percentageBps: number | null;
}

export interface FeeRuleResponse {
  id: string;
  name: string;
  sourceNetwork: string | null;
  destinationNetwork: string | null;
  minAmount: number;
  maxAmount: number | null;
  feeType: FeeType;
  fixedAmount: number | null;
  percentageBps: number | null;
  minFeeAmount: number | null;
  maxFeeAmount: number | null;
  currencyCode: string;
  priority: number;
  isActive: boolean;
  effectiveFrom: string;
  effectiveTo: string | null;
  transferTypeId: number | null;
  feePayer: FeePayer;
  feeRecipient: FeeRecipient;
  tiers: FeeRuleTierResponse[];
}

export interface AddTierRequest {
  tierMinAmount: number;
  tierMaxAmount: number | null;
  fixedAmount: number | null;
  percentageBps: number | null;
}

export interface CreateFeeRuleRequest {
  name: string;
  sourceNetwork: string | null;
  destinationNetwork: string | null;
  minAmount: number;
  maxAmount: number | null;
  feeType: FeeType;
  fixedAmount: number | null;
  percentageBps: number | null;
  minFeeAmount: number | null;
  maxFeeAmount: number | null;
  currencyCode: string;
  priority: number;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  transferTypeId: number | null;
  feePayer: FeePayer;
  feeRecipient: FeeRecipient;
  tiers: AddTierRequest[] | null;
}

export const feeRulesApi = {
  list: () => adminClient.get<FeeRuleResponse[]>('/fee-rules'),
  get: (id: string) => adminClient.get<FeeRuleResponse>(`/fee-rules/${id}`),
  create: (data: CreateFeeRuleRequest) => adminClient.post<FeeRuleResponse>('/fee-rules', data),
  activate: (id: string) => adminClient.patch<FeeRuleResponse>(`/fee-rules/${id}/activate`),
  deactivate: (id: string) => adminClient.patch<FeeRuleResponse>(`/fee-rules/${id}/deactivate`),
  supersede: (id: string, data: CreateFeeRuleRequest) =>
    adminClient.put<FeeRuleResponse>(`/fee-rules/${id}/supersede`, data),
  addTier: (id: string, data: AddTierRequest) =>
    adminClient.post<FeeRuleTierResponse>(`/fee-rules/${id}/tiers`, data),
  removeTier: (id: string, tierId: string) =>
    adminClient.delete<void>(`/fee-rules/${id}/tiers/${tierId}`),
};
