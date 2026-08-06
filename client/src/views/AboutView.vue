<script setup lang="ts">
const stack = [
  { label: 'Vue 3', detail: 'Composition API with <script setup>' },
  { label: 'Vite', detail: 'Lightning-fast HMR build tool' },
  { label: 'TypeScript', detail: 'Strict mode enabled' },
  { label: 'Pinia', detail: 'State management (setup store syntax)' },
  { label: 'Vue Router 4', detail: 'Lazy-loaded routes' },
  { label: 'TanStack Query', detail: 'Server-state management' },
  { label: 'Quasar', detail: 'UI component framework' },
  { label: 'Vitest', detail: 'Unit testing' },
];
</script>

<template>
  <q-page class="q-pa-lg">
    <div style="max-width: 1024px; margin: 0 auto">
      <h1 class="text-h4 text-weight-bold q-mb-sm">About</h1>
      <p class="text-body1 text-grey-6 q-mb-lg" style="max-width: 560px">
        PulsePay client — a production-ready Vue 3 front-end scaffolded with a modern stack.
      </p>

      <div class="row q-col-gutter-md" style="max-width: 640px">
        <div v-for="item in stack" :key="item.label" class="col-12 col-sm-6">
          <q-card flat bordered class="q-pa-md">
            <p class="text-body2 text-weight-semibold q-mb-xs">{{ item.label }}</p>
            <p class="text-caption text-grey-6 q-mb-none">{{ item.detail }}</p>
          </q-card>
        </div>
      </div>
    </div>
  </q-page>
</template>
