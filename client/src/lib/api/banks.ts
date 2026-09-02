import { apiClient } from './client';

export interface BankDto {
  id: string;
  mfoCode: string;
  name: string;
}

export function listBanks(): Promise<BankDto[]> {
  return apiClient.get<BankDto[]>('/banks');
}
