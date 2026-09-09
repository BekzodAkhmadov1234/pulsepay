import { apiClient } from './client';

export interface ExchangeRateDto {
  unit: number;
  min: number;
  code: string;
  rate: number;
  num_code: number;
  max: number;
  /** tiyin string — divide by 100 for UZS */
  buy: string;
  /** tiyin string — divide by 100 for UZS */
  sell: string;
  name: string;
  icon: string;
  buy_prev: string;
  sell_prev: string;
  error_text: string | null;
}

export function getExchangeRates(): Promise<ExchangeRateDto[]> {
  return apiClient.get<ExchangeRateDto[]>('/currency/rates');
}
