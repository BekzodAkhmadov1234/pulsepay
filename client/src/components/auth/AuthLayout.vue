<script setup lang="ts">
import { RouterLink } from 'vue-router';
import { Check } from '@lucide/vue';

const features = [
  'Instant P2P transfers across Uzbekistan',
  'Merchant payments via UzCard & Humo',
  'Bank-grade security, CBU licensed',
];
</script>

<template>
  <div class="flex min-h-dvh">
    <!-- ── Brand panel (lg+) ────────────────────────────────────── -->
    <aside
      class="relative hidden overflow-hidden bg-brand-950 lg:flex lg:w-[44%] lg:flex-col lg:justify-between lg:p-10 xl:w-2/5"
      aria-hidden="true"
    >
      <!-- Decorative blobs -->
      <div
        class="pointer-events-none absolute -left-24 -top-24 h-80 w-80 rounded-full bg-brand-800/25 blur-3xl"
      />
      <div
        class="pointer-events-none absolute -bottom-20 right-4 h-72 w-72 rounded-full bg-brand-700/20 blur-2xl"
      />

      <RouterLink to="/" class="relative z-10 text-xl font-black tracking-tight text-white">
        Pulse<span class="text-brand-400">Pay</span>
      </RouterLink>

      <div class="relative z-10 space-y-8 text-white">
        <div class="space-y-3">
          <p class="text-[11px] font-semibold uppercase tracking-[0.15em] text-brand-400">
            Uzbekistan's payment platform
          </p>
          <h1 class="text-4xl font-bold leading-tight tracking-tight">
            Send money<br />anywhere,<br />instantly.
          </h1>
          <p class="text-base leading-relaxed text-brand-200/75">
            P2P · C2B · B2B/B2C — all payment rails in one account.
          </p>
        </div>

        <ul class="space-y-3">
          <li v-for="f in features" :key="f" class="flex items-center gap-3 text-sm text-brand-100">
            <span
              class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-600"
            >
              <Check class="h-3 w-3 stroke-[2.5] text-white" />
            </span>
            {{ f }}
          </li>
        </ul>
      </div>

      <p class="relative z-10 text-xs text-brand-800">© 2026 PulsePay · CBU Licensed</p>
    </aside>

    <!-- ── Form panel ───────────────────────────────────────────── -->
    <main class="flex flex-1 flex-col items-center justify-center bg-white px-6 py-12">
      <!-- Mobile logo -->
      <div class="mb-8 lg:hidden">
        <RouterLink to="/" class="text-xl font-black tracking-tight text-foreground">
          Pulse<span class="text-brand-600">Pay</span>
        </RouterLink>
      </div>

      <div class="w-full max-w-sm">
        <slot />
      </div>
    </main>
  </div>
</template>
