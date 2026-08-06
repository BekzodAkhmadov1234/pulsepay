<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useCardsStore } from '@/stores/cards';
import { ApiError } from '@/lib/api/client';
import type { AddCardPayload } from '@/lib/api/cards';

const store = useCardsStore();

const showForm = ref(false);
const submitError = ref('');
const fieldErrors = ref<Partial<Record<keyof AddCardPayload, string>>>({});

const rawPan = ref('');

const form = ref<Omit<AddCardPayload, 'cardToken' | 'maskedPan'>>({
  cardHolderName: '',
  expMonth: 1,
  expYear: new Date().getFullYear(),
});

const removingId = ref<string | null>(null);
const settingDefaultId = ref<string | null>(null);

onMounted(() => {
  store.fetchCards();
});

function onPanInput(val: string | number | null) {
  const digits = String(val ?? '')
    .replace(/\D/g, '')
    .slice(0, 16);
  rawPan.value = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
}

function resetForm() {
  rawPan.value = '';
  form.value = {
    cardHolderName: '',
    expMonth: 1,
    expYear: new Date().getFullYear(),
  };
  submitError.value = '';
  fieldErrors.value = {};
  showForm.value = false;
}

function validate(): boolean {
  fieldErrors.value = {};
  const panDigits = rawPan.value.replace(/\D/g, '');
  if (!panDigits) fieldErrors.value.maskedPan = 'Card number is required.';
  else if (panDigits.length !== 16) fieldErrors.value.maskedPan = 'Card number must be 16 digits.';
  if (!form.value.cardHolderName.trim())
    fieldErrors.value.cardHolderName = 'Cardholder name is required.';
  if (form.value.expMonth < 1 || form.value.expMonth > 12)
    fieldErrors.value.expMonth = 'Expiry month must be 1–12.';
  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;
  const isExpired =
    form.value.expYear < currentYear ||
    (form.value.expYear === currentYear && form.value.expMonth < currentMonth);
  if (isExpired) fieldErrors.value.expYear = 'Card appears to be expired.';
  return Object.keys(fieldErrors.value).length === 0;
}

async function handleAdd() {
  submitError.value = '';
  if (!validate()) return;
  try {
    const panDigits = rawPan.value.replace(/\D/g, '');
    const maskedPan = `${panDigits.slice(0, 6)}${'*'.repeat(6)}${panDigits.slice(-4)}`;
    await store.addCard({ ...form.value, maskedPan, cardToken: `tok_${crypto.randomUUID()}` });
    resetForm();
  } catch (err) {
    if (err instanceof ApiError) {
      submitError.value = err.message;
    } else {
      submitError.value = 'Something went wrong. Please try again.';
    }
  }
}

async function handleSetDefault(cardId: string) {
  settingDefaultId.value = cardId;
  try {
    await store.setDefault(cardId);
  } catch (err) {
    if (err instanceof ApiError) submitError.value = err.message;
  } finally {
    settingDefaultId.value = null;
  }
}

async function handleRemove(cardId: string) {
  removingId.value = cardId;
  try {
    await store.removeCard(cardId);
  } catch (err) {
    if (err instanceof ApiError) {
      submitError.value = err.message;
    }
  } finally {
    removingId.value = null;
  }
}

function networkLabel(network: string | null) {
  if (network === 'uzcard') return 'UzCard';
  if (network === 'humo') return 'HUMO';
  return 'Unknown';
}

function networkColor(network: string | null) {
  if (network === 'uzcard') return 'blue';
  if (network === 'humo') return 'green';
  return 'grey';
}

function statusColor(status: string) {
  return status === 'VERIFIED' ? 'positive' : 'warning';
}

function expiry(month: number, year: number) {
  return `${String(month).padStart(2, '0')}/${String(year).slice(-2)}`;
}
</script>

<template>
  <q-page class="q-pa-lg">
    <div style="max-width: 1024px; margin: 0 auto" class="column q-gutter-y-lg">
      <!-- Header -->
      <div class="row items-center justify-between">
        <div>
          <h1 class="text-h5 text-weight-bold q-my-none">My Cards</h1>
          <p class="text-caption text-grey-6 q-mt-xs q-mb-none">
            Manage your UzCard and HUMO payment cards
          </p>
        </div>
        <q-btn
          color="primary"
          icon="add"
          label="Add card"
          unelevated
          no-caps
          :disable="store.isLoading"
          @click="showForm = !showForm"
        />
      </div>

      <!-- Error banner -->
      <q-banner v-if="submitError" dense rounded class="bg-red-1 text-negative">
        <template #avatar>
          <q-icon name="warning" color="negative" />
        </template>
        {{ submitError }}
      </q-banner>

      <!-- Add-card form -->
      <q-card v-if="showForm" flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">Add new card</h2>

        <q-form class="row q-col-gutter-md" @submit.prevent="handleAdd">
          <div class="col-12">
            <q-input
              :model-value="rawPan"
              label="Card number"
              placeholder="8600 1234 5678 3456"
              outlined
              maxlength="19"
              inputmode="numeric"
              :error="!!fieldErrors.maskedPan"
              :error-message="fieldErrors.maskedPan"
              @update:model-value="onPanInput"
            />
          </div>

          <div class="col-12">
            <q-input
              v-model="form.cardHolderName"
              label="Cardholder name"
              placeholder="ALISHER KARIMOV"
              outlined
              maxlength="80"
              :error="!!fieldErrors.cardHolderName"
              :error-message="fieldErrors.cardHolderName"
            />
          </div>

          <div class="col-12 col-sm-6">
            <q-input
              v-model.number="form.expMonth"
              label="Expiry month"
              type="number"
              outlined
              :min="1"
              :max="12"
              :error="!!fieldErrors.expMonth"
              :error-message="fieldErrors.expMonth"
            />
          </div>

          <div class="col-12 col-sm-6">
            <q-input
              v-model.number="form.expYear"
              label="Expiry year"
              type="number"
              outlined
              :min="new Date().getFullYear()"
              :max="2040"
              :error="!!fieldErrors.expYear"
              :error-message="fieldErrors.expYear"
            />
          </div>

          <div class="col-12 row q-gutter-sm">
            <q-btn
              type="submit"
              color="primary"
              unelevated
              no-caps
              label="Save card"
              :loading="store.isLoading"
            />
            <q-btn flat no-caps label="Cancel" @click="resetForm" />
          </div>
        </q-form>
      </q-card>

      <!-- Loading -->
      <div v-if="store.isLoading && store.cards.length === 0" class="row justify-center q-pa-xl">
        <q-spinner color="primary" size="40px" />
      </div>

      <!-- Empty state -->
      <q-card
        v-else-if="store.cards.length === 0"
        flat
        bordered
        class="q-pa-xl text-center"
        style="border-style: dashed"
      >
        <q-icon name="credit_card" size="48px" color="grey-4" />
        <p class="text-body2 text-weight-medium q-mt-md q-mb-xs">No cards yet</p>
        <p class="text-caption text-grey-6 q-mb-none">
          Add a UzCard or HUMO card to start making transfers.
        </p>
      </q-card>

      <!-- Card grid -->
      <div v-else class="row q-col-gutter-md">
        <div v-for="card in store.cards" :key="card.id" class="col-12 col-sm-6 col-lg-4">
          <q-card flat bordered class="q-pa-md">
            <!-- Network badge + default star + remove -->
            <div class="row items-center q-gutter-x-xs q-mb-sm">
              <q-badge
                :color="networkColor(card.cardNetwork)"
                :label="networkLabel(card.cardNetwork)"
              />
              <q-btn
                flat
                round
                dense
                size="sm"
                :icon="card.isDefault ? 'star' : 'star_outline'"
                :color="card.isDefault ? 'warning' : 'grey-4'"
                :disable="card.isDefault || store.isLoading"
                :loading="settingDefaultId === card.id"
                @click="handleSetDefault(card.id)"
              />
              <q-space />
              <q-btn
                flat
                round
                dense
                icon="delete_outline"
                color="negative"
                :loading="removingId === card.id"
                :disable="store.isLoading"
                @click="handleRemove(card.id)"
              />
            </div>

            <!-- Masked PAN -->
            <p
              class="text-body1 text-weight-bold q-mb-xs"
              style="font-family: monospace; letter-spacing: 0.12em"
            >
              {{ card.maskedPan }}
            </p>

            <!-- Cardholder + expiry -->
            <p class="text-caption text-grey-6 q-mb-xs">{{ card.cardHolderName }}</p>
            <p class="text-caption text-grey-6 q-mb-sm">
              Expires {{ expiry(card.expMonth, card.expYear) }}
            </p>

            <!-- Balance -->
            <p class="text-body2 text-weight-medium q-mb-sm">
              {{ Number(card.balanceUzs).toLocaleString('uz-UZ') }} UZS
            </p>

            <!-- Status -->
            <q-badge :color="statusColor(card.status)" :label="card.status" />
          </q-card>
        </div>
      </div>
    </div>
  </q-page>
</template>
