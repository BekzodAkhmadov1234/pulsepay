import { adminClient } from './adminClient';

export interface RouteResponse {
  id: string;
  routeCode: string;
  sourceNetwork: string;
  destinationNetwork: string;
  processorName: string;
  maxAmount: number | null;
  priority: number;
  avgProcessingSeconds: number | null;
  isActive: boolean;
  transferTypeId: number | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

export interface CreateRouteRequest {
  routeCode: string;
  sourceNetwork: string;
  destinationNetwork: string;
  processorName: string;
  maxAmount: number | null;
  priority: number;
  avgProcessingSeconds: number | null;
  transferTypeId: number | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

export function listRoutes(): Promise<RouteResponse[]> {
  return adminClient.get<RouteResponse[]>('/routes');
}

export function createRoute(data: CreateRouteRequest): Promise<RouteResponse> {
  return adminClient.post<RouteResponse>('/routes', data);
}

export function activateRoute(id: string): Promise<RouteResponse> {
  return adminClient.patch<RouteResponse>(`/routes/${id}/activate`);
}

export function deactivateRoute(id: string): Promise<RouteResponse> {
  return adminClient.patch<RouteResponse>(`/routes/${id}/deactivate`);
}

export function updateProcessor(id: string, processorName: string): Promise<RouteResponse> {
  return adminClient.patch<RouteResponse>(`/routes/${id}/processor`, { processorName });
}
