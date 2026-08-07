<script setup lang="ts">
import type { CreateFeeRuleRequest } from '@/lib/api/feeRules';

const props = defineProps<{ modelValue: CreateFeeRuleRequest }>();
const emit = defineEmits<{ 'update:modelValue': [v: CreateFeeRuleRequest] }>();

function update<K extends keyof CreateFeeRuleRequest>(field: K, val: CreateFeeRuleRequest[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: val });
}
</script>

<template>
  <div class="column q-gutter-y-sm">
    <q-input
      :model-value="modelValue.name"
      label="Name *"
      outlined
      dense
      @update:model-value="(v) => update('name', String(v ?? ''))"
    />

    <div class="row q-col-gutter-sm">
      <div class="col-6">
        <q-select
          :model-value="modelValue.sourceNetwork"
          label="Source network"
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
          label="Dest network"
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
          label="Min amount (tiyin)"
          outlined
          dense
          @update:model-value="(v) => update('minAmount', Number(v))"
        />
      </div>
      <div class="col-4">
        <q-input
          :model-value="modelValue.maxAmount"
          type="number"
          label="Max amount (tiyin)"
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
          label="Priority"
          outlined
          dense
          @update:model-value="(v) => update('priority', Number(v))"
        />
      </div>
    </div>

    <q-select
      :model-value="modelValue.feeType"
      label="Fee type *"
      outlined
      dense
      :options="[
        { label: 'Fixed', value: 'FIXED' },
        { label: 'Percentage (bps)', value: 'PERCENTAGE' },
        { label: 'Tiered', value: 'TIERED' },
      ]"
      emit-value
      map-options
      @update:model-value="(v) => update('feeType', v)"
    />

    <q-input
      v-if="modelValue.feeType === 'FIXED'"
      :model-value="modelValue.fixedAmount"
      type="number"
      label="Fixed amount (tiyin) *"
      outlined
      dense
      @update:model-value="(v) => update('fixedAmount', v !== '' && v !== null ? Number(v) : null)"
    />

    <template v-if="modelValue.feeType === 'PERCENTAGE'">
      <q-input
        :model-value="modelValue.percentageBps"
        type="number"
        label="Percentage bps (e.g. 100 = 1%) *"
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
            label="Min fee (tiyin)"
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
            label="Max fee (tiyin)"
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
          label="Fee payer"
          outlined
          dense
          :options="['SENDER', 'RECIPIENT', 'MERCHANT', 'BUSINESS']"
          emit-value
          map-options
          @update:model-value="(v) => update('feePayer', v)"
        />
      </div>
      <div class="col-6">
        <q-select
          :model-value="modelValue.feeRecipient"
          label="Fee recipient"
          outlined
          dense
          :options="['PLATFORM', 'NETWORK', 'BANK']"
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
          label="Transfer type ID (1=P2P)"
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
          label="Currency"
          outlined
          dense
          @update:model-value="(v) => update('currencyCode', String(v ?? 'UZS'))"
        />
      </div>
    </div>
  </div>
</template>
