import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
  listRoutes,
  createRoute,
  activateRoute,
  deactivateRoute,
  updateProcessor,
} from '@/lib/api/routes';
import type { RouteResponse, CreateRouteRequest } from '@/lib/api/routes';

export const useRoutesStore = defineStore('routes', () => {
  const routes = ref<RouteResponse[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  async function load(): Promise<void> {
    isLoading.value = true;
    error.value = null;
    try {
      routes.value = await listRoutes();
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : "Yo'nalishlarni yuklashda xatolik";
    } finally {
      isLoading.value = false;
    }
  }

  async function create(data: CreateRouteRequest): Promise<RouteResponse> {
    const route = await createRoute(data);
    routes.value.unshift(route);
    return route;
  }

  async function activate(id: string): Promise<void> {
    const updated = await activateRoute(id);
    patch(updated);
  }

  async function deactivate(id: string): Promise<void> {
    const updated = await deactivateRoute(id);
    patch(updated);
  }

  async function changeProcessor(id: string, processorName: string): Promise<void> {
    const updated = await updateProcessor(id, processorName);
    patch(updated);
  }

  function patch(updated: RouteResponse): void {
    const idx = routes.value.findIndex((r) => r.id === updated.id);
    if (idx !== -1) routes.value[idx] = updated;
  }

  return { routes, isLoading, error, load, create, activate, deactivate, changeProcessor };
});
