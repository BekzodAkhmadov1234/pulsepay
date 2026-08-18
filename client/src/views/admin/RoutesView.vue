<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { useRoutesStore } from '@/stores/routes';
import type { CreateRouteRequest, RouteResponse } from '@/lib/api/routes';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const adminAuth = useAdminAuthStore();
const store = useRoutesStore();

onMounted(() => store.load());

const KNOWN_PROCESSORS = ['uzcard', 'humo', 'stub'];
const NETWORKS = ['uzcard', 'humo'];

// ── Table columns ──────────────────────────────────────────────────────────

const columns = [
  { name: 'routeCode', label: 'Kod', field: 'routeCode', align: 'left' as const, sortable: true },
  {
    name: 'path',
    label: "Yo'nalish",
    field: (r: RouteResponse) => `${r.sourceNetwork} → ${r.destinationNetwork}`,
    align: 'left' as const,
  },
  {
    name: 'processorName',
    label: 'Vositachi',
    field: 'processorName',
    align: 'left' as const,
  },
  {
    name: 'priority',
    label: 'Ustuvorlik',
    field: 'priority',
    align: 'center' as const,
    sortable: true,
  },
  {
    name: 'maxAmount',
    label: 'Maks. summa',
    field: (r: RouteResponse) =>
      r.maxAmount ? (r.maxAmount / 100).toLocaleString('uz-UZ') + ' UZS' : '—',
    align: 'right' as const,
  },
  { name: 'isActive', label: 'Holati', field: 'isActive', align: 'center' as const },
  { name: 'actions', label: '', field: 'id', align: 'center' as const },
];

// ── Create dialog ──────────────────────────────────────────────────────────

const showCreate = ref(false);
const createError = ref('');
const createLoading = ref(false);

function blankForm(): CreateRouteRequest {
  return {
    routeCode: '',
    sourceNetwork: 'uzcard',
    destinationNetwork: 'uzcard',
    processorName: 'uzcard',
    maxAmount: null,
    priority: 10,
    avgProcessingSeconds: null,
    transferTypeId: 1,
    effectiveFrom: null,
    effectiveTo: null,
  };
}

const createForm = ref<CreateRouteRequest>(blankForm());

function openCreate() {
  createForm.value = blankForm();
  createError.value = '';
  showCreate.value = true;
}

async function submitCreate() {
  createError.value = '';
  createLoading.value = true;
  try {
    await store.create({
      ...createForm.value,
      maxAmount: createForm.value.maxAmount
        ? Math.round(Number(createForm.value.maxAmount) * 100)
        : null,
    });
    showCreate.value = false;
  } catch (e) {
    createError.value = e instanceof ApiError ? e.message : "Yo'nalish yaratishda xatolik";
  } finally {
    createLoading.value = false;
  }
}

// ── Change processor dialog ────────────────────────────────────────────────

const showProcessor = ref(false);
const processorTarget = ref<RouteResponse | null>(null);
const processorName = ref('');
const processorError = ref('');
const processorLoading = ref(false);

function openProcessor(route: RouteResponse) {
  processorTarget.value = route;
  processorName.value = route.processorName;
  processorError.value = '';
  showProcessor.value = true;
}

async function submitProcessor() {
  if (!processorTarget.value) return;
  processorError.value = '';
  processorLoading.value = true;
  try {
    await store.changeProcessor(processorTarget.value.id, processorName.value);
    showProcessor.value = false;
  } catch (e) {
    processorError.value = e instanceof ApiError ? e.message : "Vositachini o'zgartirishda xatolik";
  } finally {
    processorLoading.value = false;
  }
}

// ── Toggle active ──────────────────────────────────────────────────────────

const toggleError = ref('');

async function toggleActive(route: RouteResponse) {
  toggleError.value = '';
  try {
    if (route.isActive) {
      await store.deactivate(route.id);
    } else {
      await store.activate(route.id);
    }
  } catch (e) {
    toggleError.value = e instanceof ApiError ? e.message : "Holat o'zgartirishda xatolik";
  }
}

// ── Logout ─────────────────────────────────────────────────────────────────

function handleLogout() {
  adminAuth.logout();
  router.push('/admin/login');
}
</script>

<template>
  <q-layout view="hHh lpR fFf">
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
          label="Komissiya qoidalari"
          to="/admin/fee-rules"
          active-class="text-primary"
        />
        <q-btn
          flat
          dense
          no-caps
          label="Yo'nalishlar"
          to="/admin/routes"
          active-class="text-primary"
          class="q-ml-sm"
        />
        <q-space />
        <span class="text-caption text-grey-5 q-mr-md gt-sm">{{ adminAuth.admin?.email }}</span>
        <q-btn flat dense no-caps icon="logout" label="Chiqish" @click="handleLogout" />
      </q-toolbar>
    </q-header>

    <q-page-container>
      <q-page style="max-width: 1280px; margin: 0 auto; padding: 24px 16px">
        <div class="row items-center q-mb-lg">
          <div class="col">
            <h1 class="text-h5 text-weight-bold q-my-none">O'tkazma yo'nalishlari</h1>
            <p class="text-body2 text-grey-6 q-mb-none q-mt-xs">
              Har bir yo'nalish uchun vositachi (intermediary) tanlang va boshqaring.
            </p>
          </div>
          <q-btn
            unelevated
            color="primary"
            no-caps
            icon="add"
            label="Yangi yo'nalish"
            @click="openCreate"
          />
        </div>

        <q-banner v-if="store.error" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ store.error }}
        </q-banner>
        <q-banner v-if="toggleError" dense rounded class="bg-red-1 text-negative q-mb-md">
          <template #avatar><q-icon name="warning" color="negative" /></template>
          {{ toggleError }}
        </q-banner>

        <q-table
          :rows="store.routes"
          :columns="columns"
          row-key="id"
          :loading="store.isLoading"
          flat
          bordered
          dense
          :rows-per-page-options="[25, 50, 0]"
          no-data-label="Hozircha yo'nalishlar yo'q"
        >
          <template #body-cell-processorName="props">
            <q-td :props="props">
              <q-badge
                outline
                :color="
                  props.value === 'uzcard' ? 'blue' : props.value === 'humo' ? 'green' : 'grey'
                "
                :label="props.value"
              />
            </q-td>
          </template>

          <template #body-cell-isActive="props">
            <q-td :props="props" class="text-center">
              <q-badge
                :color="props.value ? 'positive' : 'grey-5'"
                :label="props.value ? 'Faol' : 'Nofaol'"
              />
            </q-td>
          </template>

          <template #body-cell-actions="props">
            <q-td :props="props" class="text-center">
              <q-btn-group flat>
                <q-btn
                  flat
                  dense
                  no-caps
                  size="sm"
                  icon="swap_horiz"
                  color="primary"
                  @click="openProcessor(props.row)"
                >
                  <q-tooltip>Vositachini o'zgartirish</q-tooltip>
                </q-btn>
                <q-btn
                  flat
                  dense
                  no-caps
                  size="sm"
                  :icon="props.row.isActive ? 'block' : 'check_circle'"
                  :color="props.row.isActive ? 'negative' : 'positive'"
                  @click="toggleActive(props.row)"
                >
                  <q-tooltip>{{ props.row.isActive ? "O'chirish" : 'Faollashtirish' }}</q-tooltip>
                </q-btn>
              </q-btn-group>
            </q-td>
          </template>
        </q-table>

        <!-- ── Create dialog ──────────────────────────────────── -->
        <q-dialog v-model="showCreate" persistent>
          <q-card style="min-width: 480px; max-width: 580px; width: 100%">
            <q-card-section class="q-pb-sm">
              <div class="text-h6">Yangi yo'nalish</div>
            </q-card-section>
            <q-separator />
            <q-card-section class="q-gutter-y-sm">
              <q-banner v-if="createError" dense rounded class="bg-red-1 text-negative">
                <template #avatar><q-icon name="warning" color="negative" /></template>
                {{ createError }}
              </q-banner>

              <q-input v-model="createForm.routeCode" label="Yo'nalish kodi" dense outlined />

              <div class="row q-col-gutter-sm">
                <div class="col-6">
                  <q-select
                    v-model="createForm.sourceNetwork"
                    :options="NETWORKS"
                    label="Manba tarmog'i"
                    dense
                    outlined
                  />
                </div>
                <div class="col-6">
                  <q-select
                    v-model="createForm.destinationNetwork"
                    :options="NETWORKS"
                    label="Qabul tarmog'i"
                    dense
                    outlined
                  />
                </div>
              </div>

              <q-select
                v-model="createForm.processorName"
                :options="KNOWN_PROCESSORS"
                use-input
                input-debounce="0"
                label="Vositachi (intermediary)"
                dense
                outlined
                hint="Pul qaysi tizim orqali o'tkaziladi"
              />

              <div class="row q-col-gutter-sm">
                <div class="col-6">
                  <q-input
                    v-model.number="createForm.priority"
                    type="number"
                    label="Ustuvorlik"
                    dense
                    outlined
                  />
                </div>
                <div class="col-6">
                  <q-input
                    v-model.number="createForm.maxAmount"
                    type="number"
                    label="Maks. summa (UZS)"
                    dense
                    outlined
                    clearable
                  />
                </div>
              </div>

              <q-input
                v-model.number="createForm.transferTypeId"
                type="number"
                label="Transfer turi ID"
                dense
                outlined
              />
            </q-card-section>
            <q-separator />
            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps label="Bekor qilish" />
              <q-btn
                unelevated
                color="primary"
                no-caps
                label="Yaratish"
                :loading="createLoading"
                @click="submitCreate"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>

        <!-- ── Change processor dialog ────────────────────────── -->
        <q-dialog v-model="showProcessor" persistent>
          <q-card style="min-width: 360px; max-width: 440px; width: 100%">
            <q-card-section class="q-pb-sm">
              <div class="text-h6">Vositachini o'zgartirish</div>
              <div class="text-caption text-grey-6">{{ processorTarget?.routeCode }}</div>
            </q-card-section>
            <q-separator />
            <q-card-section class="q-gutter-y-sm">
              <q-banner v-if="processorError" dense rounded class="bg-red-1 text-negative">
                <template #avatar><q-icon name="warning" color="negative" /></template>
                {{ processorError }}
              </q-banner>
              <q-select
                v-model="processorName"
                :options="KNOWN_PROCESSORS"
                use-input
                input-debounce="0"
                label="Yangi vositachi"
                dense
                outlined
              />
              <p class="text-caption text-grey-6 q-mb-none">
                Pul mablag'i ushbu vositachi (intermediary) orqali jo'natiladi va qabul qilinadi.
              </p>
            </q-card-section>
            <q-separator />
            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps label="Bekor qilish" />
              <q-btn
                unelevated
                color="primary"
                no-caps
                label="Saqlash"
                :loading="processorLoading"
                @click="submitProcessor"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>
      </q-page>
    </q-page-container>
  </q-layout>
</template>
