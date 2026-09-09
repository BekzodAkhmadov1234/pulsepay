import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
  listCards as apiListCards,
  addCard as apiAddCard,
  removeCard as apiRemoveCard,
  setDefaultCard as apiSetDefaultCard,
  blockCard as apiBlockCard,
  unblockCard as apiUnblockCard,
  getCardStatement as apiGetStatement,
  getLimitTypes as apiGetLimitTypes,
  getCardLimits as apiGetLimits,
  setCardLimits as apiSetLimits,
  removeCardLimit as apiRemoveLimit,
  setCardPin as apiSetPin,
} from '@/lib/api/cards';
import type {
  CardDto,
  AddCardPayload,
  StatementEntry,
  LimitTypeDto,
  CardLimitDto,
  LimitInput,
} from '@/lib/api/cards';

export const useCardsStore = defineStore('cards', () => {
  const cards = ref<CardDto[]>([]);
  const isLoading = ref(false);

  async function fetchCards(): Promise<void> {
    isLoading.value = true;
    try {
      cards.value = await apiListCards();
    } finally {
      isLoading.value = false;
    }
  }

  async function addCard(payload: AddCardPayload): Promise<CardDto> {
    isLoading.value = true;
    try {
      const card = await apiAddCard(payload);
      cards.value.push(card);
      return card;
    } finally {
      isLoading.value = false;
    }
  }

  async function removeCard(cardId: string): Promise<void> {
    isLoading.value = true;
    try {
      await apiRemoveCard(cardId);
      cards.value = cards.value.filter((c) => c.id !== cardId);
    } finally {
      isLoading.value = false;
    }
  }

  async function setDefault(cardId: string): Promise<void> {
    const updated = await apiSetDefaultCard(cardId);
    cards.value = cards.value.map((c) => (c.id === cardId ? updated : { ...c, isDefault: false }));
  }

  function _replaceCard(updated: CardDto) {
    cards.value = cards.value.map((c) => (c.id === updated.id ? updated : c));
  }

  async function blockCard(cardId: string): Promise<void> {
    _replaceCard(await apiBlockCard(cardId));
  }

  async function unblockCard(cardId: string): Promise<void> {
    _replaceCard(await apiUnblockCard(cardId));
  }

  async function fetchStatement(
    cardId: string,
    params?: { startDate?: string; endDate?: string }
  ): Promise<StatementEntry[]> {
    return apiGetStatement(cardId, params);
  }

  async function fetchLimitTypes(): Promise<LimitTypeDto[]> {
    return apiGetLimitTypes();
  }

  async function fetchLimits(cardId: string): Promise<CardLimitDto[]> {
    return apiGetLimits(cardId);
  }

  async function saveLimits(cardId: string, limits: LimitInput[]): Promise<CardLimitDto[]> {
    return apiSetLimits(cardId, limits);
  }

  async function deleteLimit(cardId: string, limitType: string): Promise<void> {
    return apiRemoveLimit(cardId, limitType);
  }

  async function changePin(cardId: string, network: 'humo' | 'uzcard', pin: string): Promise<void> {
    return apiSetPin(cardId, network, pin);
  }

  return {
    cards,
    isLoading,
    fetchCards,
    addCard,
    removeCard,
    setDefault,
    blockCard,
    unblockCard,
    fetchStatement,
    fetchLimitTypes,
    fetchLimits,
    saveLimits,
    deleteLimit,
    changePin,
  };
});
