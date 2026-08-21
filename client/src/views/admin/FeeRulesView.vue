<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { useFeeRulesStore } from '@/stores/feeRules';
import type { FeeRuleResponse, CreateFeeRuleRequest } from '@/lib/api/feeRules';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const route = useRoute();
const adminAuth = useAdminAuthStore();
const store = useFeeRulesStore();

onMounted(() => store.loadRules());

// ── Display helpers ────────────────────────────────────────────────────────

function formatUzs(tiyin: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 0 }).format(tiyin / 100);
}

function scopeOf(r: FeeRuleResponse) {
  return `${r.sourceNetwork ?? '*'} → ${r.destinationNetwork ?? '*'}`;
}

function feeOf(r: FeeRuleResponse) {
  if (r.feeType === 'FIXED') return formatUzs(r.fixedAmount ?? 0) + ' UZS';
  if (r.feeType === 'PERCENTAGE') return ((r.percentageBps ?? 0) / 100).toFixed(2) + '%';
  return r.tiers.length + ' bosqich';
}

const TYPE_INK: Record<string, string> = {
  PERCENTAGE: '#6FD8A8',
  TIERED: '#F2B23E',
  FIXED: 'rgba(247,244,237,0.6)',
};
const TYPE_BORDER: Record<string, string> = {
  PERCENTAGE: 'rgba(41,190,140,0.45)',
  TIERED: 'rgba(242,178,62,0.45)',
  FIXED: 'rgba(247,244,237,0.2)',
};
const TYPE_LABEL: Record<string, string> = {
  FIXED: 'Belgilangan',
  PERCENTAGE: 'Foizli',
  TIERED: 'Bosqichli',
};
const PAYER_LABEL: Record<string, string> = {
  SENDER: "Jo'natuvchi",
  RECIPIENT: 'Qabul qiluvchi',
};

// ── Filter / search ────────────────────────────────────────────────────────

const query = ref('');
const typeFilter = ref('Barchasi');
const chips = [
  { label: 'Barchasi', value: 'Barchasi' },
  { label: 'Foizli', value: 'PERCENTAGE' },
  { label: 'Bosqichli', value: 'TIERED' },
  { label: 'Belgilangan', value: 'FIXED' },
];

const filteredRules = computed(() => {
  let rules = store.rules;
  if (typeFilter.value !== 'Barchasi') {
    rules = rules.filter((r) => r.feeType === typeFilter.value);
  }
  if (query.value.trim()) {
    const q = query.value.trim().toLowerCase();
    rules = rules.filter(
      (r) => r.name.toLowerCase().includes(q) || scopeOf(r).toLowerCase().includes(q)
    );
  }
  return rules;
});

// ── Toggle active/inactive ─────────────────────────────────────────────────

const toggleError = ref('');

async function toggleActive(rule: FeeRuleResponse) {
  toggleError.value = '';
  try {
    if (rule.isActive) {
      await store.deactivateRule(rule.id);
    } else {
      await store.activateRule(rule.id);
    }
  } catch (e) {
    toggleError.value = e instanceof ApiError ? e.message : "Holat o'zgartirishda xatolik";
  }
}

// ── Create dialog ──────────────────────────────────────────────────────────

const showCreate = ref(false);
const createError = ref('');
const createLoading = ref(false);

const fName = ref('');
const fSource = ref('*');
const fDest = ref('*');
const fMin = ref('0');
const fMax = ref('');
const fPriority = ref('100');
const fType = ref<'FIXED' | 'PERCENTAGE' | 'TIERED'>('FIXED');
const fFixed = ref('');
const fBps = ref('');
const fMinFee = ref('');
const fMaxFee = ref('');
const fPayer = ref('SENDER');
const fTypeId = ref('1');
const fCurrency = ref('UZS');

function openCreate() {
  fName.value = '';
  fSource.value = '*';
  fDest.value = '*';
  fMin.value = '0';
  fMax.value = '';
  fPriority.value = '100';
  fType.value = 'FIXED';
  fFixed.value = '';
  fBps.value = '';
  fMinFee.value = '';
  fMaxFee.value = '';
  fPayer.value = 'SENDER';
  fTypeId.value = '1';
  fCurrency.value = 'UZS';
  createError.value = '';
  showCreate.value = true;
}

async function submitCreate() {
  createError.value = '';
  if (!fName.value.trim()) {
    createError.value = 'Qoida nomini kiriting.';
    return;
  }
  createLoading.value = true;
  try {
    const payload: CreateFeeRuleRequest = {
      name: fName.value.trim(),
      sourceNetwork: fSource.value === '*' ? null : fSource.value,
      destinationNetwork: fDest.value === '*' ? null : fDest.value,
      minAmount: parseInt(fMin.value) || 0,
      maxAmount: fMax.value ? parseInt(fMax.value) : null,
      feeType: fType.value,
      fixedAmount: fType.value === 'FIXED' && fFixed.value ? parseInt(fFixed.value) : null,
      percentageBps: fType.value === 'PERCENTAGE' && fBps.value ? parseInt(fBps.value) : null,
      minFeeAmount: fType.value === 'PERCENTAGE' && fMinFee.value ? parseInt(fMinFee.value) : null,
      maxFeeAmount: fType.value === 'PERCENTAGE' && fMaxFee.value ? parseInt(fMaxFee.value) : null,
      currencyCode: fCurrency.value || 'UZS',
      priority: parseInt(fPriority.value) || 100,
      effectiveFrom: null,
      effectiveTo: null,
      transferTypeId: fTypeId.value ? parseInt(fTypeId.value) : null,
      feePayer: fPayer.value as 'SENDER' | 'RECIPIENT',
      feeRecipient: 'PLATFORM',
      tiers: null,
    };
    await store.createRule(payload);
    showCreate.value = false;
  } catch (e) {
    createError.value = e instanceof ApiError ? e.message : 'Qoida yaratishda xatolik';
  } finally {
    createLoading.value = false;
  }
}

// ── Tiers dialog ───────────────────────────────────────────────────────────

const showTiers = ref(false);
const tiersRuleId = ref<string | null>(null);
const addTierLoading = ref(false);
const tierError = ref('');

const tiersRule = computed(() => store.rules.find((r) => r.id === tiersRuleId.value) ?? null);

const tMin = ref('0');
const tMax = ref('');
const tFixed = ref('');
const tPercent = ref('');

function openTiers(rule: FeeRuleResponse) {
  tiersRuleId.value = rule.id;
  tMin.value = '0';
  tMax.value = '';
  tFixed.value = '';
  tPercent.value = '';
  tierError.value = '';
  showTiers.value = true;
}

async function submitAddTier() {
  if (!tiersRuleId.value) return;
  if (!tFixed.value && !tPercent.value) {
    tierError.value = 'Belgilangan summa yoki foizni kiriting.';
    return;
  }
  tierError.value = '';
  addTierLoading.value = true;
  try {
    await store.addTier(tiersRuleId.value, {
      tierMinAmount: Math.round((parseFloat(tMin.value) || 0) * 100),
      tierMaxAmount: tMax.value ? Math.round(parseFloat(tMax.value) * 100) : null,
      fixedAmount: tFixed.value ? Math.round(parseFloat(tFixed.value) * 100) : null,
      percentageBps: tPercent.value ? Math.round(parseFloat(tPercent.value) * 100) : null,
    });
    tMin.value = '0';
    tMax.value = '';
    tFixed.value = '';
    tPercent.value = '';
  } catch (e) {
    tierError.value = e instanceof ApiError ? e.message : "Bosqich qo'shishda xatolik";
  } finally {
    addTierLoading.value = false;
  }
}

async function removeTier(tierId: string) {
  if (!tiersRuleId.value) return;
  try {
    await store.removeTier(tiersRuleId.value, tierId);
  } catch (e) {
    tierError.value = e instanceof ApiError ? e.message : "Bosqich o'chirishda xatolik";
  }
}

function tierRange(t: { tierMinAmount: number; tierMaxAmount: number | null }) {
  const max = t.tierMaxAmount !== null ? formatUzs(t.tierMaxAmount) + ' UZS' : '∞';
  return `${formatUzs(t.tierMinAmount)} UZS — ${max}`;
}

function tierFee(t: { fixedAmount: number | null; percentageBps: number | null }) {
  if (t.percentageBps !== null) return (t.percentageBps / 100).toFixed(2) + '% foiz';
  if (t.fixedAmount !== null) return formatUzs(t.fixedAmount) + ' UZS belgilangan';
  return '—';
}

// ── Supersede dialog ───────────────────────────────────────────────────────

const showSupersede = ref(false);
const supersedeTargetId = ref<string | null>(null);
const supersedeError = ref('');
const supersedeLoading = ref(false);

const sName = ref('');
const sSource = ref('*');
const sDest = ref('*');
const sMin = ref('0');
const sMax = ref('');
const sPriority = ref('100');
const sType = ref<'FIXED' | 'PERCENTAGE' | 'TIERED'>('FIXED');
const sFixed = ref('');
const sBps = ref('');
const sMinFee = ref('');
const sMaxFee = ref('');
const sPayer = ref('SENDER');
const sTypeId = ref('1');
const sCurrency = ref('UZS');

function openSupersede(rule: FeeRuleResponse) {
  supersedeTargetId.value = rule.id;
  sName.value = rule.name;
  sSource.value = rule.sourceNetwork ?? '*';
  sDest.value = rule.destinationNetwork ?? '*';
  sMin.value = String(rule.minAmount);
  sMax.value = rule.maxAmount != null ? String(rule.maxAmount) : '';
  sPriority.value = String(rule.priority);
  sType.value = rule.feeType;
  sFixed.value = rule.fixedAmount != null ? String(rule.fixedAmount) : '';
  sBps.value = rule.percentageBps != null ? String(rule.percentageBps) : '';
  sMinFee.value = rule.minFeeAmount != null ? String(rule.minFeeAmount) : '';
  sMaxFee.value = rule.maxFeeAmount != null ? String(rule.maxFeeAmount) : '';
  sPayer.value = rule.feePayer;
  sTypeId.value = rule.transferTypeId != null ? String(rule.transferTypeId) : '1';
  sCurrency.value = rule.currencyCode || 'UZS';
  supersedeError.value = '';
  showSupersede.value = true;
}

async function submitSupersede() {
  if (!supersedeTargetId.value) return;
  supersedeError.value = '';
  if (!sName.value.trim()) {
    supersedeError.value = 'Qoida nomini kiriting.';
    return;
  }
  supersedeLoading.value = true;
  try {
    const payload: CreateFeeRuleRequest = {
      name: sName.value.trim(),
      sourceNetwork: sSource.value === '*' ? null : sSource.value,
      destinationNetwork: sDest.value === '*' ? null : sDest.value,
      minAmount: parseInt(sMin.value) || 0,
      maxAmount: sMax.value ? parseInt(sMax.value) : null,
      feeType: sType.value,
      fixedAmount: sType.value === 'FIXED' && sFixed.value ? parseInt(sFixed.value) : null,
      percentageBps: sType.value === 'PERCENTAGE' && sBps.value ? parseInt(sBps.value) : null,
      minFeeAmount: sType.value === 'PERCENTAGE' && sMinFee.value ? parseInt(sMinFee.value) : null,
      maxFeeAmount: sType.value === 'PERCENTAGE' && sMaxFee.value ? parseInt(sMaxFee.value) : null,
      currencyCode: sCurrency.value || 'UZS',
      priority: parseInt(sPriority.value) || 100,
      effectiveFrom: null,
      effectiveTo: null,
      transferTypeId: sTypeId.value ? parseInt(sTypeId.value) : null,
      feePayer: sPayer.value as 'SENDER' | 'RECIPIENT',
      feeRecipient: 'PLATFORM',
      tiers: null,
    };
    await store.supersedeRule(supersedeTargetId.value, payload);
    showSupersede.value = false;
  } catch (e) {
    supersedeError.value = e instanceof ApiError ? e.message : 'Yangilashda xatolik';
  } finally {
    supersedeLoading.value = false;
  }
}

// ── Logout ─────────────────────────────────────────────────────────────────

function handleLogout() {
  adminAuth.logout();
  router.push('/admin/login');
}
</script>

<template>
  <div class="admin-page">
    <!-- ── Header ──────────────────────────────────────────────────────── -->
    <header class="admin-header">
      <div style="display: flex; align-items: center; gap: clamp(20px, 3vw, 40px)">
        <div style="display: flex; align-items: center; gap: 10px">
          <div
            style="
              width: 26px;
              height: 26px;
              border-radius: 8px;
              background: #29be8c;
              display: flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;
            "
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#0E211C"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M4 14h4l2.5-7 3 12 2.5-9 2 4h2" />
            </svg>
          </div>
          <span
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: 20px;
              font-weight: 700;
              letter-spacing: -0.01em;
            "
            >Pulse<span style="color: #29be8c">Pay</span></span
          >
          <span
            style="
              padding: 4px 9px;
              border-radius: 7px;
              background: rgba(247, 244, 237, 0.1);
              font-size: 11.5px;
              font-weight: 700;
              letter-spacing: 0.08em;
              text-transform: uppercase;
              color: rgba(247, 244, 237, 0.72);
            "
            >Admin</span
          >
        </div>
        <nav style="display: flex; align-items: center; gap: 4px">
          <button
            class="nav-btn"
            :class="{ 'nav-btn--active': route.path === '/admin/fee-rules' }"
            @click="router.push('/admin/fee-rules')"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="M5 19 19 5M7.5 9a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5zm9 11a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z"
              />
            </svg>
            Komissiya qoidalari
          </button>
          <button
            class="nav-btn"
            :class="{ 'nav-btn--active': route.path === '/admin/merchants' }"
            @click="router.push('/admin/merchants')"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="M3 9h18v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 9l2.45-4.9A2 2 0 0 1 7.24 3h9.52a2 2 0 0 1 1.8 1.1L21 9"
              />
              <path d="M12 3v6" />
            </svg>
            Savdogarlar
          </button>
        </nav>
      </div>
      <div style="display: flex; align-items: center; gap: 14px">
        <span style="font-size: 14.5px; font-weight: 600; color: rgba(247, 244, 237, 0.72)">{{
          adminAuth.admin?.email
        }}</span>
        <button class="logout-btn" @click="handleLogout">
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
          </svg>
          Chiqish
        </button>
      </div>
    </header>

    <!-- ── Main ────────────────────────────────────────────────────────── -->
    <main class="admin-main">
      <!-- Title row -->
      <div
        style="
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: 20px;
          flex-wrap: wrap;
        "
      >
        <div>
          <h1
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: clamp(28px, 3.4vw, 40px);
              font-weight: 600;
              letter-spacing: -0.03em;
              margin: 0;
            "
          >
            Komissiya qoidalari
          </h1>
          <p
            style="
              font-size: 15px;
              line-height: 1.5;
              color: rgba(247, 244, 237, 0.6);
              margin: 10px 0 0;
            "
          >
            O'tkazmalarga qo'llaniladigan komissiya qoidalarini boshqaring.
          </p>
        </div>
        <button class="btn-primary" @click="openCreate">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 5v14M5 12h14" />
          </svg>
          Yangi qoida
        </button>
      </div>

      <!-- Search -->
      <div class="search-bar" style="margin-top: 24px">
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="rgba(247,244,237,0.45)"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
          style="flex: none"
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.6-3.6" />
        </svg>
        <input
          v-model="query"
          type="text"
          placeholder="Qoida nomi yoki doirasi bo'yicha qidirish"
          class="search-input"
        />
      </div>

      <!-- Filter chips -->
      <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px">
        <button
          v-for="chip in chips"
          :key="chip.value"
          class="chip-btn"
          :class="{ 'chip-btn--active': typeFilter === chip.value }"
          @click="typeFilter = chip.value"
        >
          {{ chip.label }}
        </button>
      </div>

      <!-- Error banner -->
      <div v-if="store.error || toggleError" class="error-banner">
        {{ store.error || toggleError }}
      </div>

      <!-- Table -->
      <div class="table-wrap" style="margin-top: 24px">
        <!-- Header -->
        <div class="table-head">
          <div>Nomi</div>
          <div>Turi</div>
          <div>Komissiya</div>
          <div>Doirasi</div>
          <div style="text-align: right">Ustuvorlik</div>
          <div>To'lovchi</div>
          <div>Holati</div>
          <div></div>
        </div>

        <!-- Loading -->
        <div
          v-if="store.isLoading"
          style="
            padding: 48px;
            text-align: center;
            color: rgba(247, 244, 237, 0.45);
            font-size: 15px;
          "
        >
          Yuklanmoqda...
        </div>

        <!-- Empty -->
        <div
          v-else-if="!filteredRules.length"
          style="
            padding: 48px;
            text-align: center;
            color: rgba(247, 244, 237, 0.45);
            font-size: 15px;
          "
        >
          Hozircha komissiya qoidalari yo'q
        </div>

        <!-- Rows -->
        <template v-else>
          <div v-for="rule in filteredRules" :key="rule.id" class="table-row">
            <div class="col-name">{{ rule.name }}</div>
            <div>
              <span
                class="type-badge"
                :style="{ color: TYPE_INK[rule.feeType], borderColor: TYPE_BORDER[rule.feeType] }"
              >
                {{ TYPE_LABEL[rule.feeType] ?? rule.feeType }}
              </span>
            </div>
            <div class="col-fee">{{ feeOf(rule) }}</div>
            <div class="col-scope">{{ scopeOf(rule) }}</div>
            <div class="col-priority">{{ rule.priority }}</div>
            <div class="col-payer">{{ PAYER_LABEL[rule.feePayer] ?? rule.feePayer }}</div>
            <div>
              <button
                class="status-btn"
                :style="{
                  background: rule.isActive ? 'rgba(41,190,140,0.16)' : 'rgba(247,244,237,0.08)',
                  color: rule.isActive ? '#29BE8C' : 'rgba(247,244,237,0.6)',
                }"
                @click="toggleActive(rule)"
              >
                <span
                  class="status-dot"
                  :style="{ background: rule.isActive ? '#29BE8C' : 'rgba(247,244,237,0.4)' }"
                ></span>
                {{ rule.isActive ? 'Faol' : 'Nofaol' }}
              </button>
            </div>
            <div style="display: flex; gap: 6px; align-items: center; justify-content: flex-end">
              <button
                class="supersede-btn"
                title="Yangilash — yangi versiya yaratish"
                @click="openSupersede(rule)"
              >
                <svg
                  width="15"
                  height="15"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
              </button>
              <button
                v-if="rule.feeType === 'TIERED'"
                class="tiers-btn"
                title="Bosqichlarni boshqarish"
                @click="openTiers(rule)"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="m12 3 9 5-9 5-9-5z" />
                  <path d="m3 13 9 5 9-5" />
                </svg>
              </button>
            </div>
          </div>
        </template>

        <!-- Footer -->
        <div class="table-footer">
          <span
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-weight: 600;
              color: rgba(247, 244, 237, 0.72);
            "
          >
            {{ filteredRules.length }} ta qoida
          </span>
        </div>
      </div>
    </main>

    <footer class="admin-footer">© 2026 PulsePay · Admin panel</footer>

    <!-- ── Tiers modal ─────────────────────────────────────────────────── -->
    <div v-if="showTiers" class="overlay" @click.self="showTiers = false">
      <div class="modal">
        <div class="modal-header">
          <h2 class="modal-title">Bosqichlar — {{ tiersRule?.name }}</h2>
          <button class="modal-close" @click="showTiers = false">
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div style="display: flex; flex-direction: column; gap: 8px">
            <div v-for="tier in tiersRule?.tiers" :key="tier.id" class="tier-row">
              <div style="flex: 1; min-width: 0">
                <div
                  style="
                    font-family: 'Space Grotesk', sans-serif;
                    font-size: 15.5px;
                    font-weight: 600;
                    letter-spacing: -0.01em;
                  "
                >
                  {{ tierRange(tier) }}
                </div>
                <div style="font-size: 13px; color: #6fd8a8; margin-top: 4px">
                  {{ tierFee(tier) }}
                </div>
              </div>
              <button class="tier-delete-btn" title="O'chirish" @click="removeTier(tier.id)">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" />
                </svg>
              </button>
            </div>
            <div v-if="!tiersRule?.tiers.length" class="tier-empty">
              Bosqichlar yo'q — pastda qo'shing.
            </div>
          </div>

          <h3
            style="
              font-family: 'Space Grotesk', sans-serif;
              font-size: 16px;
              font-weight: 600;
              letter-spacing: -0.01em;
              margin: 26px 0 0;
            "
          >
            Bosqich qo'shish
          </h3>
          <div style="display: flex; flex-wrap: wrap; gap: 14px; margin-top: 14px">
            <div class="form-field">
              <label class="field-label">Min summa (UZS)</label>
              <input
                v-model="tMin"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Max summa (UZS, bo'sh = ∞)</label>
              <input
                v-model="tMax"
                type="text"
                inputmode="numeric"
                placeholder="∞"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Belgilangan summa (UZS)</label>
              <input
                v-model="tFixed"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Foiz (%)</label>
              <input
                v-model="tPercent"
                type="text"
                inputmode="decimal"
                placeholder="0.50"
                class="field-input"
              />
            </div>
          </div>
          <div v-if="tierError" class="form-error">{{ tierError }}</div>
          <button
            class="btn-primary"
            style="margin-top: 18px"
            :disabled="addTierLoading"
            @click="submitAddTier"
          >
            {{ addTierLoading ? '...' : "Bosqich qo'shish" }}
          </button>
        </div>
        <div class="modal-footer">
          <button class="btn-outline" @click="showTiers = false">Tayyor</button>
        </div>
      </div>
    </div>

    <!-- ── Supersede modal ─────────────────────────────────────────────── -->
    <div v-if="showSupersede" class="overlay" @click.self="showSupersede = false">
      <div class="modal" style="max-width: 720px">
        <div class="modal-header">
          <div>
            <h2 class="modal-title">Yangilash</h2>
            <p style="font-size: 13px; color: rgba(247, 244, 237, 0.5); margin: 4px 0 0">
              Eski qoida yopiladi va yangi versiya darhol kuchga kiradi.
            </p>
          </div>
          <button class="modal-close" @click="showSupersede = false">
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="modal-body" style="display: flex; flex-direction: column; gap: 18px">
          <div>
            <label class="field-label">Nomi *</label>
            <input
              v-model="sName"
              type="text"
              placeholder="Qoida nomi"
              class="field-input"
              style="width: 100%; margin-top: 8px"
            />
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Manba tarmog'i</label>
              <select v-model="sSource" class="field-select">
                <option value="*">Barchasi (*)</option>
                <option value="uzcard">UzCard</option>
                <option value="humo">Humo</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Manzil tarmog'i</label>
              <select v-model="sDest" class="field-select">
                <option value="*">Barchasi (*)</option>
                <option value="uzcard">UzCard</option>
                <option value="humo">Humo</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">Min summa (tiyin)</label>
              <input
                v-model="sMin"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Max summa (tiyin)</label>
              <input
                v-model="sMax"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Ustuvorlik</label>
              <input
                v-model="sPriority"
                type="text"
                inputmode="numeric"
                placeholder="100"
                class="field-input"
              />
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya turi *</label>
              <select v-model="sType" class="field-select">
                <option value="FIXED">Belgilangan</option>
                <option value="PERCENTAGE">Foizli</option>
                <option value="TIERED">Bosqichli</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div v-if="sType === 'FIXED'" class="form-field">
              <label class="field-label">Belgilangan summa (tiyin) *</label>
              <input
                v-model="sFixed"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div v-if="sType === 'PERCENTAGE'" class="form-field">
              <label class="field-label">Foiz (bazis punkt, 100=1%) *</label>
              <input
                v-model="sBps"
                type="text"
                inputmode="numeric"
                placeholder="100"
                class="field-input"
              />
            </div>
          </div>

          <div v-if="sType === 'PERCENTAGE'" style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">Min komissiya (tiyin)</label>
              <input
                v-model="sMinFee"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Max komissiya (tiyin)</label>
              <input
                v-model="sMaxFee"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya to'lovchi</label>
              <select v-model="sPayer" class="field-select">
                <option value="SENDER">Jo'natuvchi</option>
                <option value="RECIPIENT">Qabul qiluvchi</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya qabul qiluvchi</label>
              <select class="field-select" disabled style="opacity: 0.5">
                <option value="PLATFORM">Platforma</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">O'tkazma turi ID (1=P2P)</label>
              <input
                v-model="sTypeId"
                type="text"
                inputmode="numeric"
                placeholder="1"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Valyuta</label>
              <input v-model="sCurrency" type="text" placeholder="UZS" class="field-input" />
            </div>
          </div>

          <div v-if="supersedeError" class="form-error">{{ supersedeError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-outline" @click="showSupersede = false">Bekor qilish</button>
          <button class="btn-primary" :disabled="supersedeLoading" @click="submitSupersede">
            {{ supersedeLoading ? '...' : 'Yangilash' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ── Create modal ────────────────────────────────────────────────── -->
    <div v-if="showCreate" class="overlay" @click.self="showCreate = false">
      <div class="modal" style="max-width: 720px">
        <div class="modal-header">
          <h2 class="modal-title">Yangi komissiya qoidasi</h2>
          <button class="modal-close" @click="showCreate = false">
            <svg
              width="17"
              height="17"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="modal-body" style="display: flex; flex-direction: column; gap: 18px">
          <div>
            <label class="field-label">Nomi *</label>
            <input
              v-model="fName"
              type="text"
              placeholder="Qoida nomi"
              class="field-input"
              style="width: 100%; margin-top: 8px"
            />
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Manba tarmog'i</label>
              <select v-model="fSource" class="field-select">
                <option value="*">Barchasi (*)</option>
                <option value="uzcard">UzCard</option>
                <option value="humo">Humo</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Manzil tarmog'i</label>
              <select v-model="fDest" class="field-select">
                <option value="*">Barchasi (*)</option>
                <option value="uzcard">UzCard</option>
                <option value="humo">Humo</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">Min summa (tiyin)</label>
              <input
                v-model="fMin"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Max summa (tiyin)</label>
              <input
                v-model="fMax"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Ustuvorlik</label>
              <input
                v-model="fPriority"
                type="text"
                inputmode="numeric"
                placeholder="100"
                class="field-input"
              />
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya turi *</label>
              <select v-model="fType" class="field-select">
                <option value="FIXED">Belgilangan</option>
                <option value="PERCENTAGE">Foizli</option>
                <option value="TIERED">Bosqichli</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div v-if="fType === 'FIXED'" class="form-field">
              <label class="field-label">Belgilangan summa (tiyin) *</label>
              <input
                v-model="fFixed"
                type="text"
                inputmode="numeric"
                placeholder="0"
                class="field-input"
              />
            </div>
            <div v-if="fType === 'PERCENTAGE'" class="form-field">
              <label class="field-label">Foiz (bazis punkt, 100=1%) *</label>
              <input
                v-model="fBps"
                type="text"
                inputmode="numeric"
                placeholder="100"
                class="field-input"
              />
            </div>
          </div>

          <div v-if="fType === 'PERCENTAGE'" style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">Min komissiya (tiyin)</label>
              <input
                v-model="fMinFee"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Max komissiya (tiyin)</label>
              <input
                v-model="fMaxFee"
                type="text"
                inputmode="numeric"
                placeholder="—"
                class="field-input"
              />
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya to'lovchi</label>
              <select v-model="fPayer" class="field-select">
                <option value="SENDER">Jo'natuvchi</option>
                <option value="RECIPIENT">Qabul qiluvchi</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
            <div style="flex: 1 1 220px; min-width: 0; position: relative">
              <label class="field-label">Komissiya qabul qiluvchi</label>
              <select class="field-select" disabled style="opacity: 0.5">
                <option value="PLATFORM">Platforma</option>
              </select>
              <svg
                class="select-arrow"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="rgba(247,244,237,0.6)"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 14px">
            <div class="form-field">
              <label class="field-label">O'tkazma turi ID (1=P2P)</label>
              <input
                v-model="fTypeId"
                type="text"
                inputmode="numeric"
                placeholder="1"
                class="field-input"
              />
            </div>
            <div class="form-field">
              <label class="field-label">Valyuta</label>
              <input v-model="fCurrency" type="text" placeholder="UZS" class="field-input" />
            </div>
          </div>

          <div v-if="createError" class="form-error">{{ createError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-outline" @click="showCreate = false">Bekor qilish</button>
          <button class="btn-primary" :disabled="createLoading" @click="submitCreate">
            {{ createLoading ? '...' : 'Yaratish' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');

*,
*::before,
*::after {
  box-sizing: border-box;
}

.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: Manrope, system-ui, sans-serif;
  background: #0e211c;
  color: #f7f4ed;
}

/* ── Header ──────────────────────────────────────────────────────────────── */

.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  padding: 16px clamp(20px, 4vw, 48px);
  background: rgba(14, 33, 28, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(247, 244, 237, 0.08);
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  color: rgba(247, 244, 237, 0.6);
  padding: 9px 16px;
  border-radius: 999px;
  font-family: Manrope, sans-serif;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.nav-btn:hover {
  background: rgba(247, 244, 237, 0.06);
  color: #f7f4ed;
}
.nav-btn--active {
  background: rgba(247, 244, 237, 0.1);
  color: #f7f4ed;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: 1px solid rgba(247, 244, 237, 0.16);
  border-radius: 999px;
  padding: 8px 14px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.72);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.logout-btn:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}

/* ── Main ────────────────────────────────────────────────────────────────── */

.admin-main {
  flex: 1;
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: clamp(28px, 4vw, 48px) clamp(20px, 4vw, 48px) 64px;
}

/* ── Buttons ─────────────────────────────────────────────────────────────── */

.btn-primary {
  display: flex;
  align-items: center;
  gap: 9px;
  border: none;
  border-radius: 999px;
  background: #29be8c;
  color: #0e211c;
  padding: 13px 22px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-primary:hover:not(:disabled) {
  background: #4fd3a6;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  border: 1px solid rgba(247, 244, 237, 0.18);
  border-radius: 999px;
  background: transparent;
  color: rgba(247, 244, 237, 0.75);
  padding: 13px 24px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.btn-outline:hover {
  background: rgba(247, 244, 237, 0.08);
  color: #f7f4ed;
}

/* ── Search ──────────────────────────────────────────────────────────────── */

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 560px;
  background: rgba(247, 244, 237, 0.045);
  border: 1px solid rgba(247, 244, 237, 0.12);
  border-radius: 16px;
  padding: 0 16px;
  height: 52px;
}

.search-input {
  flex: 1;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  font-family: Manrope, sans-serif;
  font-size: 15.5px;
  font-weight: 500;
  color: #f7f4ed;
  outline: none;
}
.search-input::placeholder {
  color: rgba(247, 244, 237, 0.35);
}

/* ── Chips ───────────────────────────────────────────────────────────────── */

.chip-btn {
  height: 40px;
  padding: 0 16px;
  background: rgba(247, 244, 237, 0.045);
  border: 1px solid rgba(247, 244, 237, 0.12);
  border-radius: 999px;
  font-family: Manrope, sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  color: #f7f4ed;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}
.chip-btn--active {
  background: #29be8c;
  border-color: #29be8c;
  color: #0e211c;
}

/* ── Error ───────────────────────────────────────────────────────────────── */

.error-banner {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(255, 100, 100, 0.1);
  border: 1px solid rgba(255, 100, 100, 0.3);
  border-radius: 12px;
  font-size: 13px;
  color: #ff9c82;
}

/* ── Table ───────────────────────────────────────────────────────────────── */

.table-wrap {
  background: rgba(247, 244, 237, 0.04);
  border: 1px solid rgba(247, 244, 237, 0.09);
  border-radius: 20px;
  overflow-x: auto;
}

.table-head,
.table-row {
  min-width: 940px;
  display: grid;
  grid-template-columns: minmax(200px, 2.2fr) 108px 96px minmax(120px, 1fr) 84px 110px 100px 80px;
  align-items: center;
  gap: 12px;
  padding: 15px 18px;
}

.table-head {
  background: rgba(247, 244, 237, 0.04);
  border-bottom: 1px solid rgba(247, 244, 237, 0.09);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(247, 244, 237, 0.45);
}

.table-row {
  border-bottom: 1px solid rgba(247, 244, 237, 0.07);
  transition: background 0.12s;
}
.table-row:hover {
  background: rgba(247, 244, 237, 0.04);
}

.col-name {
  font-size: 15px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.col-fee {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.col-scope {
  font-size: 14px;
  color: rgba(247, 244, 237, 0.62);
  min-width: 0;
}
.col-priority {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15px;
  font-weight: 600;
  text-align: right;
}
.col-payer {
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(247, 244, 237, 0.62);
}

.type-badge {
  display: inline-block;
  padding: 5px 10px;
  border: 1px solid;
  border-radius: 7px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.status-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  border: none;
  border-radius: 999px;
  padding: 6px 12px;
  font-family: Manrope, sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
}
.status-btn:hover {
  opacity: 0.8;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
}

.supersede-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 9px;
  background: rgba(247, 244, 237, 0.07);
  color: rgba(247, 244, 237, 0.55);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.supersede-btn:hover {
  background: rgba(247, 244, 237, 0.14);
  color: #f7f4ed;
}

.tiers-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 9px;
  background: rgba(41, 190, 140, 0.14);
  color: #29be8c;
  cursor: pointer;
  transition: background 0.15s;
}
.tiers-btn:hover {
  background: rgba(41, 190, 140, 0.26);
}

.table-footer {
  min-width: 940px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 16px 22px;
  font-size: 13.5px;
  color: rgba(247, 244, 237, 0.55);
  border-top: 1px solid rgba(247, 244, 237, 0.07);
}

/* ── Footer ──────────────────────────────────────────────────────────────── */

.admin-footer {
  padding: 18px clamp(20px, 4vw, 48px);
  border-top: 1px solid rgba(247, 244, 237, 0.08);
  font-size: 12.5px;
  color: rgba(247, 244, 237, 0.4);
}

/* ── Overlay / Modal ─────────────────────────────────────────────────────── */

.overlay {
  position: fixed;
  inset: 0;
  z-index: 25;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 4vh, 48px) 20px;
  background: rgba(8, 20, 17, 0.72);
  backdrop-filter: blur(6px);
  overflow-y: auto;
}

.modal {
  width: 100%;
  max-width: 640px;
  background: #122923;
  border: 1px solid rgba(247, 244, 237, 0.12);
  border-radius: 22px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.45);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px;
  border-bottom: 1px solid rgba(247, 244, 237, 0.1);
}

.modal-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.02em;
  margin: 0;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(247, 244, 237, 0.5);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.modal-close:hover {
  background: rgba(247, 244, 237, 0.1);
  color: #f7f4ed;
}

.modal-body {
  padding: 26px;
  max-height: 70vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px 26px;
  border-top: 1px solid rgba(247, 244, 237, 0.1);
}

/* ── Tier rows ───────────────────────────────────────────────────────────── */

.tier-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(14, 33, 28, 0.55);
  border: 1px solid rgba(247, 244, 237, 0.1);
  border-radius: 14px;
  padding: 14px 16px;
}

.tier-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(247, 244, 237, 0.42);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.tier-delete-btn:hover {
  background: rgba(255, 156, 130, 0.14);
  color: #ff9c82;
}

.tier-empty {
  background: rgba(14, 33, 28, 0.4);
  border: 1px dashed rgba(247, 244, 237, 0.16);
  border-radius: 14px;
  padding: 22px;
  text-align: center;
  font-size: 14px;
  color: rgba(247, 244, 237, 0.5);
}

/* ── Form elements ───────────────────────────────────────────────────────── */

.form-field {
  flex: 1 1 220px;
  min-width: 0;
}

.field-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(247, 244, 237, 0.62);
}

.field-input {
  width: 100%;
  margin-top: 8px;
  height: 52px;
  padding: 0 16px;
  background: rgba(14, 33, 28, 0.6);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15.5px;
  font-weight: 500;
  color: #f7f4ed;
  transition: border-color 0.15s;
}
.field-input:focus {
  outline: none;
  border-color: #29be8c;
}
.field-input::placeholder {
  color: rgba(247, 244, 237, 0.35);
  font-family: Manrope, sans-serif;
}

.field-select {
  width: 100%;
  appearance: none;
  margin-top: 8px;
  height: 52px;
  padding: 0 40px 0 16px;
  background: rgba(14, 33, 28, 0.6);
  border: 1px solid rgba(247, 244, 237, 0.14);
  border-radius: 12px;
  font-family: Manrope, sans-serif;
  font-size: 15.5px;
  font-weight: 500;
  color: #f7f4ed;
  cursor: pointer;
  transition: border-color 0.15s;
}
.field-select:focus {
  outline: none;
  border-color: #29be8c;
}
.field-select option {
  background: #122923;
  color: #f7f4ed;
}

.select-arrow {
  position: absolute;
  right: 16px;
  bottom: 19px;
  pointer-events: none;
}

.form-error {
  font-size: 13px;
  color: #ff9c82;
}
</style>
