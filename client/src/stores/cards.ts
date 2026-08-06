import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
  listCards as apiListCards,
  addCard as apiAddCard,
  removeCard as apiRemoveCard,
  setDefaultCard as apiSetDefaultCard,
} from '@/lib/api/cards';
import type { CardDto, AddCardPayload } from '@/lib/api/cards';

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

  return { cards, isLoading, fetchCards, addCard, removeCard, setDefault };
});
