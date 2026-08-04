<script setup lang="ts">
import { useCounterStore } from '@/stores/counter';

const counter = useCounterStore();
</script>

<template>
  <div class="space-y-8">
    <section>
      <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Welcome to PulsePay</h1>
      <p class="mt-2 text-gray-500">Uzbekistan's P2P · C2B · B2B/B2C payment platform.</p>
    </section>

    <!-- Pinia counter demo -->
    <section class="rounded-xl border border-gray-200 p-6 space-y-4 max-w-sm">
      <h2 class="text-lg font-semibold text-gray-800">Pinia Counter Demo</h2>
      <p class="text-4xl font-mono font-bold text-purple-600">{{ counter.count }}</p>
      <p class="text-sm text-gray-500">Double: {{ counter.doubleCount }}</p>
      <div class="flex gap-3">
        <button
          class="px-4 py-2 rounded-lg bg-purple-600 text-white hover:bg-purple-700 transition-colors"
          @click="counter.increment"
        >
          +
        </button>
        <button
          class="px-4 py-2 rounded-lg bg-gray-200 text-gray-800 hover:bg-gray-300 transition-colors"
          @click="counter.decrement"
        >
          −
        </button>
        <button
          class="px-4 py-2 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
          @click="counter.reset"
        >
          Reset
        </button>
      </div>
    </section>
  </div>
</template>
