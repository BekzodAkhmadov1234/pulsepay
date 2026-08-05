<script setup lang="ts">
import { TriangleAlert } from '@lucide/vue';

defineProps<{
  message: string;
}>();
</script>

<template>
  <div
    role="alert"
    class="flex items-start gap-3 rounded-lg border border-destructive-border bg-destructive-subtle px-4 py-3 text-sm text-destructive"
  >
    <TriangleAlert class="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
    <span>{{ message }}</span>
  </div>
</template>
