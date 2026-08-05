<script setup lang="ts">
import { useAuthStore } from '@/stores/auth';
import { ArrowUpRight, ArrowDownLeft, Store, Activity } from '@lucide/vue';

const auth = useAuthStore();

const stats = [
  { label: 'Available balance', value: '— UZS', sub: 'Connect a card to fund your wallet' },
  { label: 'Transfers this month', value: '0', sub: 'No transfers yet' },
  { label: 'KYC level', value: auth.user?.kycLevel ?? '—', sub: 'Identity verification' },
];

const actions = [
  { label: 'Send money', icon: ArrowUpRight },
  { label: 'Receive', icon: ArrowDownLeft },
  { label: 'Pay merchant', icon: Store },
];
</script>

<template>
  <div class="space-y-8">
    <!-- Welcome -->
    <section>
      <p class="text-sm text-muted-fg">Good day,</p>
      <h1 class="mt-0.5 text-2xl font-bold text-foreground">
        {{ auth.user?.fullName || auth.user?.phoneE164 || 'Dashboard' }}
      </h1>
    </section>

    <!-- Stats -->
    <section class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="rounded-xl border border-border bg-white p-5 shadow-xs"
      >
        <p class="text-xs font-medium text-muted-fg">{{ stat.label }}</p>
        <p class="mt-1 text-2xl font-bold tracking-tight text-foreground">{{ stat.value }}</p>
        <p class="mt-0.5 text-xs text-muted-fg">{{ stat.sub }}</p>
      </div>
    </section>

    <!-- Quick actions -->
    <section>
      <h2 class="mb-3 text-xs font-semibold uppercase tracking-widest text-muted-fg">
        Quick actions
      </h2>
      <div class="grid grid-cols-3 gap-3">
        <button
          v-for="action in actions"
          :key="action.label"
          type="button"
          class="flex flex-col items-center gap-2.5 rounded-xl border border-border bg-white p-5 text-center transition-colors hover:border-brand-200 hover:bg-brand-50"
        >
          <span class="flex h-9 w-9 items-center justify-center rounded-full bg-brand-50">
            <component :is="action.icon" class="h-4 w-4 text-brand-600" />
          </span>
          <span class="text-xs font-medium text-foreground">{{ action.label }}</span>
        </button>
      </div>
    </section>

    <!-- Activity -->
    <section>
      <h2 class="mb-3 text-xs font-semibold uppercase tracking-widest text-muted-fg">
        Recent activity
      </h2>
      <div
        class="flex flex-col items-center justify-center rounded-xl border border-border bg-white px-6 py-14 text-center"
      >
        <Activity class="h-8 w-8 text-muted-fg/30" />
        <p class="mt-3 text-sm font-medium text-foreground">No transactions yet</p>
        <p class="mt-0.5 text-xs text-muted-fg">Your payment history will appear here.</p>
      </div>
    </section>
  </div>
</template>
