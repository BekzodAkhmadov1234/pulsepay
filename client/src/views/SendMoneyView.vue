<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useCardsStore } from '@/stores/cards';
import { useTransfersStore } from '@/stores/transfers';
import { searchUserByPhone, searchUserByCard } from '@/lib/api/users';
import { ApiError } from '@/lib/api/client';
import type { RecipientDto } from '@/lib/api/users';

const cardsStore = useCardsStore();
const transfersStore = useTransfersStore();
const router = useRouter();

onMounted(async () => {
  if (cardsStore.cards.length === 0) await cardsStore.fetchCards();
  // Auto-select first verified card immediately so the user sees it upfront
  if (!senderCardId.value && verifiedCards.value.length > 0) {
    senderCardId.value = verifiedCards.value[0].id;
  }
});

// Sender card (always visible)
const senderCardId = ref('');

const verifiedCards = computed(() => cardsStore.cards.filter((c) => c.status === 'VERIFIED'));

// Recipient lookup
const lookupMode = ref<'phone' | 'card'>('phone');
const phoneDigits = ref('');
const recipientPhone = computed(() => '+998' + phoneDigits.value);
const cardNumber = ref('');
const recipient = ref<RecipientDto | null>(null);
const lookupError = ref('');
const isLooking = ref(false);

// Transfer details
const recipientCardId = ref('');
const amountUzs = ref<number | null>(null);
const sendError = ref('');
const fieldErrors = ref<Record<string, string>>({});

watch(lookupMode, () => {
  recipient.value = null;
  lookupError.value = '';
  recipientCardId.value = '';
});

function onPhoneInput(val: string | number | null) {
  phoneDigits.value = String(val ?? '')
    .replace(/\D/g, '')
    .slice(0, 9);
}

async function handleLookup() {
  lookupError.value = '';
  recipient.value = null;
  recipientCardId.value = '';

  if (lookupMode.value === 'phone') {
    if (phoneDigits.value.length !== 9) {
      lookupError.value = 'Enter 9 digits after +998.';
      return;
    }
    isLooking.value = true;
    try {
      recipient.value = await searchUserByPhone(recipientPhone.value);
      const defaultCard =
        recipient.value.cards.find((c) => c.isDefault) ?? recipient.value.cards[0];
      recipientCardId.value = defaultCard?.id ?? '';
    } catch (err) {
      lookupError.value =
        err instanceof ApiError && err.status === 404
          ? 'No account found for that phone number.'
          : 'Lookup failed. Please try again.';
    } finally {
      isLooking.value = false;
    }
  } else {
    const digits = cardNumber.value.replace(/\D/g, '');
    if (digits.length !== 16) {
      lookupError.value = 'Enter a 16-digit card number.';
      return;
    }
    isLooking.value = true;
    try {
      recipient.value = await searchUserByCard(digits);
      const first6 = digits.slice(0, 6);
      const last4 = digits.slice(-4);
      const matched = recipient.value.cards.find(
        (c) => c.maskedPan.startsWith(first6) && c.maskedPan.endsWith(last4)
      );
      recipientCardId.value = matched?.id ?? recipient.value.cards[0]?.id ?? '';
    } catch (err) {
      lookupError.value =
        err instanceof ApiError && err.status === 404
          ? 'No card found for that number.'
          : 'Lookup failed. Please try again.';
    } finally {
      isLooking.value = false;
    }
  }
}

function validate(): boolean {
  fieldErrors.value = {};
  if (!senderCardId.value) fieldErrors.value.senderCard = 'Select your card.';
  if (!recipientCardId.value) fieldErrors.value.recipientCard = 'Select recipient card.';
  if (!amountUzs.value || amountUzs.value <= 0) fieldErrors.value.amount = 'Enter a valid amount.';
  else if (amountUzs.value > 30_000_000)
    fieldErrors.value.amount = 'Maximum 30 000 000 UZS per transfer.';
  return Object.keys(fieldErrors.value).length === 0;
}

async function handleSend() {
  sendError.value = '';
  if (!validate() || !recipient.value) return;

  const senderCard = verifiedCards.value.find((c) => c.id === senderCardId.value)!;
  const recipientCard = recipient.value.cards.find((c) => c.id === recipientCardId.value)!;

  try {
    await transfersStore.sendMoney({
      senderInstrumentId: senderCard.id,
      senderCardNetwork: senderCard.cardNetwork ?? 'uzcard',
      recipientId: recipient.value.id,
      recipientInstrumentId: recipientCard.id,
      recipientCardNetwork: recipientCard.cardNetwork ?? 'uzcard',
      amountUzs: amountUzs.value!,
      transferTypeId: 1,
      channel: 'mobile_app',
      idempotencyKey: crypto.randomUUID(),
    });
    router.push('/transfers');
  } catch (err) {
    sendError.value = err instanceof ApiError ? err.message : 'Transfer failed. Please try again.';
  }
}

function networkLabel(network: string | null) {
  if (network === 'uzcard') return 'UzCard';
  if (network === 'humo') return 'HUMO';
  return 'Unknown';
}
</script>

<template>
  <q-page class="q-pa-lg">
    <div style="max-width: 600px; margin: 0 auto" class="column q-gutter-y-lg">
      <div>
        <h1 class="text-h5 text-weight-bold q-my-none">Send Money</h1>
        <p class="text-caption text-grey-6 q-mt-xs q-mb-none">P2P transfer to any PulsePay user</p>
      </div>

      <!-- Step 1: Your card — always visible so users with multiple cards can choose upfront -->
      <q-card flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">Your card</h2>

        <q-select
          v-model="senderCardId"
          :options="
            verifiedCards.map((c) => ({
              label: `${networkLabel(c.cardNetwork)}  ${c.maskedPan}`,
              value: c.id,
            }))
          "
          label="Pay from"
          outlined
          emit-value
          map-options
          :error="!!fieldErrors.senderCard"
          :error-message="fieldErrors.senderCard"
        >
          <template v-if="verifiedCards.length === 0" #no-option>
            <q-item>
              <q-item-section class="text-grey">
                No verified cards — <router-link to="/cards">add a card</router-link> first.
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card>

      <!-- Step 2: Recipient lookup -->
      <q-card flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">
          Transfer by phone number or card
        </h2>

        <q-btn-toggle
          v-model="lookupMode"
          :options="[
            { label: 'By phone', value: 'phone' },
            { label: 'By card', value: 'card' },
          ]"
          unelevated
          no-caps
          dense
          toggle-color="primary"
          class="q-mb-md"
        />

        <div class="row q-gutter-sm items-start">
          <div class="col">
            <!-- By Phone input -->
            <q-input
              v-if="lookupMode === 'phone'"
              :model-value="phoneDigits"
              type="tel"
              inputmode="tel"
              label="Recipient phone"
              outlined
              maxlength="9"
              :error="!!lookupError"
              :error-message="lookupError"
              @update:model-value="onPhoneInput"
              @keyup.enter="handleLookup"
            >
              <template #prepend>
                <span class="text-body2 text-grey-7">+998</span>
              </template>
            </q-input>

            <!-- By Card input -->
            <q-input
              v-else
              v-model="cardNumber"
              label="Card number"
              placeholder="8600 xxxx xxxx xxxx"
              outlined
              maxlength="16"
              type="tel"
              inputmode="numeric"
              :error="!!lookupError"
              :error-message="lookupError"
              @keyup.enter="handleLookup"
            />
          </div>
          <div class="col-auto q-mt-xs">
            <q-btn
              unelevated
              color="primary"
              no-caps
              label="Find"
              :loading="isLooking"
              @click="handleLookup"
            />
          </div>
        </div>

        <!-- Recipient found -->
        <div v-if="recipient" class="q-mt-md">
          <q-banner dense rounded class="bg-green-1 text-positive q-mb-md">
            <template #avatar><q-icon name="check_circle" color="positive" /></template>
            {{ recipient.fullName }} ({{ recipient.phoneE164 }})
          </q-banner>

          <q-select
            v-model="recipientCardId"
            :options="
              recipient.cards.map((c) => ({
                label: `${networkLabel(c.cardNetwork)}  ${c.maskedPan}${c.isDefault ? '  ★' : ''}`,
                value: c.id,
              }))
            "
            label="Recipient card"
            outlined
            emit-value
            map-options
            :error="!!fieldErrors.recipientCard"
            :error-message="fieldErrors.recipientCard"
          />
        </div>
      </q-card>

      <!-- Step 3: Amount + send (shown after recipient found) -->
      <q-card v-if="recipient" flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">Amount</h2>

        <q-banner v-if="sendError" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ sendError }}
        </q-banner>

        <div class="column q-gutter-y-md">
          <q-input
            v-model.number="amountUzs"
            label="Amount (UZS)"
            type="number"
            outlined
            :min="1"
            :max="30000000"
            :error="!!fieldErrors.amount"
            :error-message="fieldErrors.amount"
          />

          <div class="row q-gutter-sm">
            <q-btn
              unelevated
              color="primary"
              no-caps
              label="Send"
              :loading="transfersStore.isLoading"
              :disable="verifiedCards.length === 0"
              @click="handleSend"
            />
            <q-btn flat no-caps label="Cancel" to="/" />
          </div>
        </div>
      </q-card>
    </div>
  </q-page>
</template>
