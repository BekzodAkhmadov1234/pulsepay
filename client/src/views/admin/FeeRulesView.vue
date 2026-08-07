<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { useFeeRulesStore } from '@/stores/feeRules';
import FeeRuleFormFields from './FeeRuleFormFields.vue';
import type { FeeRuleResponse, CreateFeeRuleRequest } from '@/lib/api/feeRules';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const adminAuth = useAdminAuthStore();
const store = useFeeRulesStore();

onMounted(() => store.loadRules());

// ── Helpers ───────────────────────────────────────────────────────────────

function formatAmount(tiyin: number | null): string {
  if (tiyin === null || tiyin === undefined) return '—';
  return (tiyin / 100).toLocaleString('uz-UZ') + ' UZS';
}

function formatBps(bps: number | null): string {
  if (bps === null || bps === undefined) return '—';
  return (bps / 100).toFixed(2) + '%';
}

function feeDisplay(rule: FeeRuleResponse): string {
  if (rule.feeType === 'FIXED') return formatAmount(rule.fixedAmount);
  if (rule.feeType === 'PERCENTAGE') return formatBps(rule.percentageBps);
  return `${rule.tiers.length} tier(s)`;
}

// ── Table columns ─────────────────────────────────────────────────────────

const columns = [
  { name: 'name', label: 'Name', field: 'name', align: 'left' as const, sortable: true },
  { name: 'feeType', label: 'Type', field: 'feeType', align: 'left' as const },
  {
    name: 'fee',
    label: 'Fee',
    field: (r: FeeRuleResponse) => feeDisplay(r),
    align: 'left' as const,
  },
  {
    name: 'scope',
    label: 'Scope',
    field: (r: FeeRuleResponse) => `${r.sourceNetwork ?? '*'} → ${r.destinationNetwork ?? '*'}`,
    align: 'left' as const,
  },
  {
    name: 'priority',
    label: 'Priority',
    field: 'priority',
    align: 'center' as const,
    sortable: true,
  },
  { name: 'feePayer', label: 'Payer', field: 'feePayer', align: 'left' as const },
  { name: 'isActive', label: 'Status', field: 'isActive', align: 'center' as const },
  { name: 'actions', label: '', field: 'id', align: 'center' as const },
];

// ── Blank form factory ────────────────────────────────────────────────────

function blankForm(): CreateFeeRuleRequest {
  return {
    name: '',
    sourceNetwork: null,
    destinationNetwork: null,
    minAmount: 0,
    maxAmount: null,
    feeType: 'FIXED',
    fixedAmount: null,
    percentageBps: null,
    minFeeAmount: null,
    maxFeeAmount: null,
    currencyCode: 'UZS',
    priority: 100,
    effectiveFrom: null,
    effectiveTo: null,
    transferTypeId: 1,
    feePayer: 'SENDER',
    feeRecipient: 'PLATFORM',
    tiers: null,
  };
}

function toPayload(f: CreateFeeRuleRequest): CreateFeeRuleRequest {
  return {
    ...f,
    sourceNetwork: f.sourceNetwork || null,
    destinationNetwork: f.destinationNetwork || null,
    fixedAmount: f.feeType === 'FIXED' && f.fixedAmount !== null ? Number(f.fixedAmount) : null,
    percentageBps:
      f.feeType === 'PERCENTAGE' && f.percentageBps !== null ? Number(f.percentageBps) : null,
    minFeeAmount: f.minFeeAmount !== null ? Number(f.minFeeAmount) : null,
    maxFeeAmount: f.maxFeeAmount !== null ? Number(f.maxFeeAmount) : null,
    maxAmount: f.maxAmount !== null ? Number(f.maxAmount) : null,
    transferTypeId: f.transferTypeId !== null ? Number(f.transferTypeId) : null,
  };
}

// ── Create dialog ─────────────────────────────────────────────────────────

const showCreate = ref(false);
const createForm = ref<CreateFeeRuleRequest>(blankForm());
const createError = ref('');
const createLoading = ref(false);

function openCreate() {
  createForm.value = blankForm();
  createError.value = '';
  showCreate.value = true;
}

async function submitCreate() {
  createError.value = '';
  createLoading.value = true;
  try {
    await store.createRule(toPayload(createForm.value));
    showCreate.value = false;
  } catch (e) {
    createError.value = e instanceof ApiError ? e.message : 'Failed to create rule';
  } finally {
    createLoading.value = false;
  }
}

// ── Supersede dialog ──────────────────────────────────────────────────────

const showSupersede = ref(false);
const supersedeTarget = ref<FeeRuleResponse | null>(null);
const supersedeForm = ref<CreateFeeRuleRequest>(blankForm());
const supersedeError = ref('');
const supersedeLoading = ref(false);

function openSupersede(rule: FeeRuleResponse) {
  supersedeTarget.value = rule;
  supersedeForm.value = {
    name: rule.name + ' (v2)',
    sourceNetwork: rule.sourceNetwork,
    destinationNetwork: rule.destinationNetwork,
    minAmount: rule.minAmount,
    maxAmount: rule.maxAmount,
    feeType: rule.feeType,
    fixedAmount: rule.fixedAmount,
    percentageBps: rule.percentageBps,
    minFeeAmount: rule.minFeeAmount,
    maxFeeAmount: rule.maxFeeAmount,
    currencyCode: rule.currencyCode,
    priority: rule.priority,
    effectiveFrom: null,
    effectiveTo: null,
    transferTypeId: rule.transferTypeId,
    feePayer: rule.feePayer,
    feeRecipient: rule.feeRecipient,
    tiers: null,
  };
  supersedeError.value = '';
  showSupersede.value = true;
}

async function submitSupersede() {
  if (!supersedeTarget.value) return;
  supersedeError.value = '';
  supersedeLoading.value = true;
  try {
    await store.supersedeRule(supersedeTarget.value.id, toPayload(supersedeForm.value));
    showSupersede.value = false;
  } catch (e) {
    supersedeError.value = e instanceof ApiError ? e.message : 'Failed to supersede rule';
  } finally {
    supersedeLoading.value = false;
  }
}

// ── Deactivate ────────────────────────────────────────────────────────────

const deactivateError = ref('');

async function deactivate(rule: FeeRuleResponse) {
  deactivateError.value = '';
  try {
    await store.deactivateRule(rule.id);
  } catch (e) {
    deactivateError.value = e instanceof ApiError ? e.message : 'Failed to deactivate';
  }
}

// ── Tier management dialog ─────────────────────────────────────────────────

const showTiers = ref(false);
const tiersRuleId = ref<string | null>(null);
const addTierLoading = ref(false);
const tierForm = ref({
  tierMinAmount: 0,
  tierMaxAmount: null as number | null,
  fixedAmount: null as number | null,
  percentageBps: null as number | null,
});

const tiersRule = computed(() => store.rules.find((r) => r.id === tiersRuleId.value) ?? null);

function openTiers(rule: FeeRuleResponse) {
  tiersRuleId.value = rule.id;
  tierForm.value = {
    tierMinAmount: 0,
    tierMaxAmount: null,
    fixedAmount: null,
    percentageBps: null,
  };
  showTiers.value = true;
}

async function submitAddTier() {
  if (!tiersRuleId.value) return;
  addTierLoading.value = true;
  try {
    await store.addTier(tiersRuleId.value, {
      tierMinAmount: Number(tierForm.value.tierMinAmount),
      tierMaxAmount:
        tierForm.value.tierMaxAmount !== null ? Number(tierForm.value.tierMaxAmount) : null,
      fixedAmount: tierForm.value.fixedAmount !== null ? Number(tierForm.value.fixedAmount) : null,
      percentageBps:
        tierForm.value.percentageBps !== null ? Number(tierForm.value.percentageBps) : null,
    });
    tierForm.value = {
      tierMinAmount: 0,
      tierMaxAmount: null,
      fixedAmount: null,
      percentageBps: null,
    };
  } catch (e) {
    console.error(e);
  } finally {
    addTierLoading.value = false;
  }
}

async function removeTier(tierId: string) {
  if (!tiersRuleId.value) return;
  try {
    await store.removeTier(tiersRuleId.value, tierId);
  } catch (e) {
    console.error(e);
  }
}

// ── Logout ────────────────────────────────────────────────────────────────

function handleLogout() {
  adminAuth.logout();
  router.push('/admin/login');
}
</script>

<template>
  <q-layout view="hHh lpR fFf">
    <!-- Admin header -->
    <q-header elevated class="bg-grey-9 text-white">
      <q-toolbar style="max-width: 1280px; margin: 0 auto; width: 100%">
        <span class="text-weight-black text-body1 q-mr-lg">
          Pulse<span class="text-primary">Pay</span>
          <span class="text-caption text-grey-5 q-ml-xs">Admin</span>
        </span>
        <q-btn
          flat
          dense
          no-caps
          label="Fee Rules"
          to="/admin/fee-rules"
          active-class="text-primary"
        />
        <q-space />
        <span class="text-caption text-grey-5 q-mr-md gt-sm">{{ adminAuth.admin?.email }}</span>
        <q-btn flat dense no-caps icon="logout" label="Sign out" @click="handleLogout" />
      </q-toolbar>
    </q-header>

    <q-page-container>
      <q-page style="max-width: 1280px; margin: 0 auto; padding: 24px 16px">
        <!-- Page title row -->
        <div class="row items-center q-mb-lg">
          <div class="col">
            <h1 class="text-h5 text-weight-bold q-my-none">Fee Rules</h1>
            <p class="text-body2 text-grey-6 q-mb-none q-mt-xs">
              Manage fee rules applied to transfers.
            </p>
          </div>
          <q-btn
            unelevated
            color="primary"
            no-caps
            icon="add"
            label="New rule"
            @click="openCreate"
          />
        </div>

        <!-- Error banners -->
        <q-banner v-if="store.error" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ store.error }}
        </q-banner>
        <q-banner v-if="deactivateError" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ deactivateError }}
        </q-banner>

        <!-- Rules table -->
        <q-table
          :rows="store.rules"
          :columns="columns"
          row-key="id"
          :loading="store.isLoading"
          flat
          bordered
          dense
          :rows-per-page-options="[25, 50, 0]"
          no-data-label="No fee rules yet"
        >
          <template #body-cell-isActive="props">
            <q-td :props="props" class="text-center">
              <q-badge
                :color="props.value ? 'positive' : 'grey-5'"
                :label="props.value ? 'Active' : 'Inactive'"
              />
            </q-td>
          </template>

          <template #body-cell-feeType="props">
            <q-td :props="props">
              <q-badge
                outline
                :color="
                  props.value === 'TIERED'
                    ? 'deep-purple'
                    : props.value === 'PERCENTAGE'
                      ? 'teal'
                      : 'blue-grey'
                "
                :label="props.value"
              />
            </q-td>
          </template>

          <template #body-cell-actions="props">
            <q-td :props="props" class="text-center">
              <q-btn-group flat>
                <q-btn
                  v-if="props.row.feeType === 'TIERED'"
                  flat
                  dense
                  no-caps
                  size="sm"
                  icon="layers"
                  color="deep-purple"
                  @click="openTiers(props.row)"
                >
                  <q-tooltip>Manage tiers</q-tooltip>
                </q-btn>
                <q-btn
                  v-if="props.row.isActive"
                  flat
                  dense
                  no-caps
                  size="sm"
                  icon="swap_horiz"
                  color="primary"
                  @click="openSupersede(props.row)"
                >
                  <q-tooltip>Supersede (replace)</q-tooltip>
                </q-btn>
                <q-btn
                  v-if="props.row.isActive"
                  flat
                  dense
                  no-caps
                  size="sm"
                  icon="block"
                  color="negative"
                  @click="deactivate(props.row)"
                >
                  <q-tooltip>Deactivate</q-tooltip>
                </q-btn>
              </q-btn-group>
            </q-td>
          </template>
        </q-table>

        <!-- ── Create dialog ──────────────────────────────────────── -->
        <q-dialog v-model="showCreate" persistent>
          <q-card style="min-width: 520px; max-width: 640px; width: 100%">
            <q-card-section class="q-pb-sm">
              <div class="text-h6">New fee rule</div>
            </q-card-section>
            <q-separator />
            <q-card-section class="scroll q-gutter-y-sm" style="max-height: 70vh">
              <q-banner v-if="createError" dense rounded class="bg-red-1 text-negative">
                <template #avatar><q-icon name="warning" color="negative" /></template>
                {{ createError }}
              </q-banner>
              <FeeRuleFormFields v-model="createForm" />
            </q-card-section>
            <q-separator />
            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps label="Cancel" />
              <q-btn
                unelevated
                color="primary"
                no-caps
                label="Create"
                :loading="createLoading"
                @click="submitCreate"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>

        <!-- ── Supersede dialog ───────────────────────────────────── -->
        <q-dialog v-model="showSupersede" persistent>
          <q-card style="min-width: 520px; max-width: 640px; width: 100%">
            <q-card-section class="q-pb-sm">
              <div class="text-h6">Supersede rule</div>
              <div class="text-caption text-grey-6">
                Deactivates "{{ supersedeTarget?.name }}" and creates a replacement effective
                immediately.
              </div>
            </q-card-section>
            <q-separator />
            <q-card-section class="scroll q-gutter-y-sm" style="max-height: 70vh">
              <q-banner v-if="supersedeError" dense rounded class="bg-red-1 text-negative">
                <template #avatar><q-icon name="warning" color="negative" /></template>
                {{ supersedeError }}
              </q-banner>
              <FeeRuleFormFields v-model="supersedeForm" />
            </q-card-section>
            <q-separator />
            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps label="Cancel" />
              <q-btn
                unelevated
                color="primary"
                no-caps
                label="Supersede"
                :loading="supersedeLoading"
                @click="submitSupersede"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>

        <!-- ── Tiers dialog ───────────────────────────────────────── -->
        <q-dialog v-model="showTiers">
          <q-card style="min-width: 480px; max-width: 600px; width: 100%">
            <q-card-section class="q-pb-sm">
              <div class="text-h6">Tiers — {{ tiersRule?.name }}</div>
            </q-card-section>
            <q-separator />
            <q-card-section>
              <!-- Existing tiers -->
              <q-list bordered separator dense class="q-mb-md">
                <q-item v-if="!tiersRule?.tiers.length">
                  <q-item-section class="text-caption text-grey-6">No tiers yet</q-item-section>
                </q-item>
                <q-item v-for="tier in tiersRule?.tiers" :key="tier.id">
                  <q-item-section>
                    <q-item-label class="text-caption text-weight-medium">
                      {{ formatAmount(tier.tierMinAmount) }} –
                      {{ tier.tierMaxAmount !== null ? formatAmount(tier.tierMaxAmount) : '∞' }}
                    </q-item-label>
                    <q-item-label caption>
                      {{
                        tier.fixedAmount !== null
                          ? formatAmount(tier.fixedAmount) + ' fixed'
                          : formatBps(tier.percentageBps) + ' pct'
                      }}
                    </q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-btn
                      flat
                      dense
                      icon="delete"
                      color="negative"
                      size="sm"
                      @click="removeTier(tier.id)"
                    />
                  </q-item-section>
                </q-item>
              </q-list>

              <!-- Add tier form -->
              <div class="text-subtitle2 q-mb-sm">Add tier</div>
              <div class="row q-col-gutter-sm">
                <div class="col-6">
                  <q-input
                    v-model.number="tierForm.tierMinAmount"
                    type="number"
                    label="Min amount (tiyin)"
                    dense
                    outlined
                  />
                </div>
                <div class="col-6">
                  <q-input
                    v-model.number="tierForm.tierMaxAmount"
                    type="number"
                    label="Max amount (tiyin, blank = ∞)"
                    dense
                    outlined
                    clearable
                  />
                </div>
                <div class="col-6">
                  <q-input
                    v-model.number="tierForm.fixedAmount"
                    type="number"
                    label="Fixed amount (tiyin)"
                    dense
                    outlined
                    clearable
                  />
                </div>
                <div class="col-6">
                  <q-input
                    v-model.number="tierForm.percentageBps"
                    type="number"
                    label="Percentage bps"
                    dense
                    outlined
                    clearable
                  />
                </div>
              </div>
              <q-btn
                unelevated
                color="primary"
                no-caps
                label="Add tier"
                class="q-mt-sm"
                :loading="addTierLoading"
                @click="submitAddTier"
              />
            </q-card-section>
            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps label="Done" />
            </q-card-actions>
          </q-card>
        </q-dialog>
      </q-page>
    </q-page-container>
  </q-layout>
</template>
