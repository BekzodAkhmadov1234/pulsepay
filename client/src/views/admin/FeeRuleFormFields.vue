<script setup lang="ts">
import type { CreateFeeRuleRequest, AddTierRequest } from '@/lib/api/feeRules';

const props = defineProps<{ modelValue: CreateFeeRuleRequest }>();
const emit = defineEmits<{ 'update:modelValue': [v: CreateFeeRuleRequest] }>();

function update<K extends keyof CreateFeeRuleRequest>(field: K, val: CreateFeeRuleRequest[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: val });
}

function updateFeeType(v: string) {
  const tiers = v === 'TIERED' ? (props.modelValue.tiers ?? []) : null;
  emit('update:modelValue', {
    ...props.modelValue,
    feeType: v as CreateFeeRuleRequest['feeType'],
    tiers,
  });
}

function addTierRow() {
  const tiers = [
    ...(props.modelValue.tiers ?? []),
    { tierMinAmount: 0, tierMaxAmount: null, fixedAmount: null, percentageBps: 0 },
  ];
  emit('update:modelValue', { ...props.modelValue, tiers });
}

function removeTierRow(i: number) {
  const tiers = (props.modelValue.tiers ?? []).filter((_, idx) => idx !== i);
  emit('update:modelValue', { ...props.modelValue, tiers });
}

function updateTier(i: number, field: keyof AddTierRequest, val: number | null) {
  const tiers = (props.modelValue.tiers ?? []).map((t, idx) =>
    idx === i ? { ...t, [field]: val } : t
  );
  emit('update:modelValue', { ...props.modelValue, tiers });
}
</script>

<template>
  <div class="column q-gutter-y-sm">
    <q-input
      :model-value="modelValue.name"
      label="Nomi *"
      outlined
      dense
      @update:model-value="(v) => update('name', String(v ?? ''))"
    />

    <div class="row q-col-gutter-sm">
      <div class="col-6">
        <q-select
          :model-value="modelValue.sourceNetwork"
          label="Manba tarmog'i"
          outlined
          dense
          clearable
          :options="['uzcard', 'humo']"
          @update:model-value="(v) => update('sourceNetwork', v || null)"
        />
      </div>
      <div class="col-6">
        <q-select
          :model-value="modelValue.destinationNetwork"
          label="Manzil tarmog'i"
          outlined
          dense
          clearable
          :options="['uzcard', 'humo']"
          @update:model-value="(v) => update('destinationNetwork', v || null)"
        />
      </div>
    </div>

    <div class="row q-col-gutter-sm">
      <div class="col-4">
        <q-input
          :model-value="modelValue.minAmount"
          type="number"
          label="Min summa (tiyin)"
          outlined
          dense
          @update:model-value="(v) => update('minAmount', Number(v))"
        />
      </div>
      <div class="col-4">
        <q-input
          :model-value="modelValue.maxAmount"
          type="number"
          label="Max summa (tiyin)"
          outlined
          dense
          clearable
          @update:model-value="
            (v) => update('maxAmount', v !== '' && v !== null ? Number(v) : null)
          "
        />
      </div>
      <div class="col-4">
        <q-input
          :model-value="modelValue.priority"
          type="number"
          label="Ustuvorlik"
          outlined
          dense
          @update:model-value="(v) => update('priority', Number(v))"
        />
      </div>
    </div>

    <q-select
      :model-value="modelValue.feeType"
      label="Komissiya turi *"
      outlined
      dense
      :options="[
        { label: 'Belgilangan', value: 'FIXED' },
        { label: 'Foizli (bps)', value: 'PERCENTAGE' },
        { label: 'Bosqichli', value: 'TIERED' },
      ]"
      emit-value
      map-options
      @update:model-value="updateFeeType"
    />

    <template v-if="modelValue.feeType === 'TIERED'">
      <div class="text-subtitle2 q-mt-xs">Bosqichlar</div>
      <q-markup-table dense flat bordered>
        <thead>
          <tr>
            <th class="text-left">Min (UZS)</th>
            <th class="text-left">Max (UZS, bo'sh = ∞)</th>
            <th class="text-left">Foiz (%)</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(tier, i) in modelValue.tiers ?? []" :key="i">
            <td>
              <q-input
                dense
                borderless
                type="number"
                :model-value="tier.tierMinAmount"
                @update:model-value="
                  (v) => updateTier(i, 'tierMinAmount', v !== '' && v !== null ? Number(v) : 0)
                "
              />
            </td>
            <td>
              <q-input
                dense
                borderless
                type="number"
                :model-value="tier.tierMaxAmount"
                clearable
                @update:model-value="
                  (v) => updateTier(i, 'tierMaxAmount', v !== '' && v !== null ? Number(v) : null)
                "
              />
            </td>
            <td>
              <q-input
                dense
                borderless
                type="number"
                :model-value="tier.percentageBps"
                @update:model-value="
                  (v) => updateTier(i, 'percentageBps', v !== '' && v !== null ? Number(v) : null)
                "
              />
            </td>
            <td class="text-center">
              <q-btn flat dense icon="close" color="negative" size="sm" @click="removeTierRow(i)" />
            </td>
          </tr>
          <tr v-if="!modelValue.tiers?.length">
            <td colspan="4" class="text-caption text-grey-6 text-center">Bosqichlar yo'q</td>
          </tr>
        </tbody>
      </q-markup-table>
      <q-btn
        outline
        dense
        no-caps
        size="sm"
        icon="add"
        label="Bosqich qo'shish"
        color="primary"
        class="q-mt-xs"
        @click="addTierRow"
      />
    </template>

    <q-input
      v-if="modelValue.feeType === 'FIXED'"
      :model-value="modelValue.fixedAmount"
      type="number"
      label="Belgilangan summa (tiyin) *"
      outlined
      dense
      @update:model-value="(v) => update('fixedAmount', v !== '' && v !== null ? Number(v) : null)"
    />

    <template v-if="modelValue.feeType === 'PERCENTAGE'">
      <q-input
        :model-value="modelValue.percentageBps"
        type="number"
        label="Foiz (bps) (masalan: 100 = 1%) *"
        outlined
        dense
        @update:model-value="
          (v) => update('percentageBps', v !== '' && v !== null ? Number(v) : null)
        "
      />
      <div class="row q-col-gutter-sm">
        <div class="col-6">
          <q-input
            :model-value="modelValue.minFeeAmount"
            type="number"
            label="Min komissiya (tiyin)"
            outlined
            dense
            clearable
            @update:model-value="
              (v) => update('minFeeAmount', v !== '' && v !== null ? Number(v) : null)
            "
          />
        </div>
        <div class="col-6">
          <q-input
            :model-value="modelValue.maxFeeAmount"
            type="number"
            label="Max komissiya (tiyin)"
            outlined
            dense
            clearable
            @update:model-value="
              (v) => update('maxFeeAmount', v !== '' && v !== null ? Number(v) : null)
            "
          />
        </div>
      </div>
    </template>

    <div class="row q-col-gutter-sm">
      <div class="col-6">
        <q-select
          :model-value="modelValue.feePayer"
          label="Komissiya to'lovchi"
          outlined
          dense
          :options="[
            { label: 'Jo\'natuvchi', value: 'SENDER' },
            { label: 'Qabul qiluvchi', value: 'RECIPIENT' },
          ]"
          emit-value
          map-options
          @update:model-value="(v) => update('feePayer', v)"
        />
      </div>
      <div class="col-6">
        <q-select
          :model-value="modelValue.feeRecipient"
          label="Komissiya qabul qiluvchi"
          outlined
          dense
          :options="[{ label: 'Platforma', value: 'PLATFORM' }]"
          emit-value
          map-options
          @update:model-value="(v) => update('feeRecipient', v)"
        />
      </div>
    </div>

    <div class="row q-col-gutter-sm">
      <div class="col-6">
        <q-input
          :model-value="modelValue.transferTypeId"
          type="number"
          label="O'tkazma turi ID (1=P2P)"
          outlined
          dense
          @update:model-value="
            (v) => update('transferTypeId', v !== '' && v !== null ? Number(v) : null)
          "
        />
      </div>
      <div class="col-6">
        <q-input
          :model-value="modelValue.currencyCode"
          label="Valyuta"
          outlined
          dense
          @update:model-value="(v) => update('currencyCode', String(v ?? 'UZS'))"
        />
      </div>
    </div>
  </div>
</template>
