<script setup lang="ts">
const stack = [
  { label: 'Vue 3', detail: 'Composition API with <script setup>' },
  { label: 'Vite', detail: 'Lightning-fast HMR build tool' },
  { label: 'TypeScript', detail: 'Strict mode enabled' },
  { label: 'Pinia', detail: 'State management (setup store syntax)' },
  { label: 'Vue Router 4', detail: 'Lazy-loaded routes' },
  { label: 'TanStack Query', detail: 'Server-state management' },
  { label: 'Tailwind CSS v4', detail: 'Utility-first styling' },
  { label: 'Vitest', detail: 'Unit testing' },
];
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-3xl font-bold text-gray-900 dark:text-white">About</h1>
    <p class="text-gray-500 max-w-xl">
      PulsePay client — a production-ready Vue 3 front-end scaffolded with a modern stack.
    </p>

    <ul class="grid gap-3 sm:grid-cols-2 max-w-2xl">
      <li
        v-for="item in stack"
        :key="item.label"
        class="rounded-lg border border-gray-200 px-4 py-3"
      >
        <span class="font-semibold text-gray-800">{{ item.label }}</span>
        <span class="block text-sm text-gray-500 mt-0.5">{{ item.detail }}</span>
      </li>
    </ul>
  </div>
</template>
