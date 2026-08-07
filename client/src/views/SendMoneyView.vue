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
      lookupError.value = '+998 dan keyin 9 ta raqam kiriting.';
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
          ? 'Bu telefon raqami uchun hisob topilmadi.'
          : "Qidiruv muvaffaqiyatsiz. Iltimos, qayta urinib ko'ring.";
    } finally {
      isLooking.value = false;
    }
  } else {
    const digits = cardNumber.value.replace(/\D/g, '');
    if (digits.length !== 16) {
      lookupError.value = '16 ta raqamli karta raqamini kiriting.';
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
          ? 'Bu raqam uchun karta topilmadi.'
          : "Qidiruv muvaffaqiyatsiz. Iltimos, qayta urinib ko'ring.";
    } finally {
      isLooking.value = false;
    }
  }
}

function validate(): boolean {
  fieldErrors.value = {};
  if (!senderCardId.value) fieldErrors.value.senderCard = 'Kartangizni tanlang.';
  if (!recipientCardId.value) fieldErrors.value.recipientCard = 'Qabul qiluvchi kartasini tanlang.';
  if (!amountUzs.value || amountUzs.value <= 0)
    fieldErrors.value.amount = "To'g'ri summani kiriting.";
  else if (amountUzs.value > 30_000_000)
    fieldErrors.value.amount = "O'tkazma uchun maksimum 30 000 000 UZS.";
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
    sendError.value =
      err instanceof ApiError
        ? err.message
        : "O'tkazma muvaffaqiyatsiz. Iltimos, qayta urinib ko'ring.";
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
        <h1 class="text-h5 text-weight-bold q-my-none">Pul o'tkazish</h1>
        <p class="text-caption text-grey-6 q-mt-xs q-mb-none">
          Istalgan PulsePay foydalanuvchisiga P2P o'tkazma
        </p>
      </div>

      <!-- Step 1: Your card — always visible so users with multiple cards can choose upfront -->
      <q-card flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">Sizning kartangiz</h2>

        <q-select
          v-model="senderCardId"
          :options="
            verifiedCards.map((c) => ({
              label: `${networkLabel(c.cardNetwork)}  ${c.maskedPan}`,
              value: c.id,
            }))
          "
          label="Qayerdan to'lash"
          outlined
          emit-value
          map-options
          :error="!!fieldErrors.senderCard"
          :error-message="fieldErrors.senderCard"
        >
          <template v-if="verifiedCards.length === 0" #no-option>
            <q-item>
              <q-item-section class="text-grey">
                Tasdiqlangan kartalar yo'q — avval
                <router-link to="/cards">karta qo'shing</router-link>.
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card>

      <!-- Step 2: Recipient lookup -->
      <q-card flat bordered class="q-pa-md">
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">
          Telefon raqami yoki karta orqali o'tkazma
        </h2>

        <q-btn-toggle
          v-model="lookupMode"
          :options="[
            { label: 'Telefon orqali', value: 'phone' },
            { label: 'Karta orqali', value: 'card' },
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
              label="Qabul qiluvchi telefon raqami"
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
              label="Karta raqami"
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
              label="Topish"
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
            label="Qabul qiluvchi kartasi"
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
        <h2 class="text-subtitle1 text-weight-semibold q-mt-none q-mb-md">Summa</h2>

        <q-banner v-if="sendError" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ sendError }}
        </q-banner>

        <div class="column q-gutter-y-md">
          <q-input
            v-model.number="amountUzs"
            label="Summa (UZS)"
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
              label="Jo'natish"
              :loading="transfersStore.isLoading"
              :disable="verifiedCards.length === 0"
              @click="handleSend"
            />
            <q-btn flat no-caps label="Bekor qilish" to="/" />
          </div>
        </div>
      </q-card>
    </div>
  </q-page>
</template>
