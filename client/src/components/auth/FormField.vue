<script setup lang="ts">
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

defineProps<{
  id: string;
  label: string;
  type?: string;
  modelValue: string;
  error?: string;
  placeholder?: string;
  autocomplete?: string;
  maxlength?: number;
  inputmode?: 'numeric' | 'text' | 'decimal' | 'tel' | 'email' | 'url' | 'search' | 'none';
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const inputVariants = cva(
  'block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-foreground shadow-xs transition-colors placeholder:text-muted-fg focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:bg-muted/50 disabled:opacity-60',
  {
    variants: {
      hasError: {
        true: 'border-destructive focus:border-destructive focus:ring-destructive/20',
        false: 'border-border focus:border-brand-500 focus:ring-brand-500/20',
      },
    },
    defaultVariants: { hasError: false },
  }
);
</script>

<template>
  <div class="space-y-1.5">
    <label :for="id" class="block text-sm font-medium text-foreground">
      {{ label }}
    </label>
    <input
      :id="id"
      :type="type ?? 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :inputmode="inputmode"
      :aria-invalid="!!error || undefined"
      :aria-describedby="error ? `${id}-error` : undefined"
      :class="cn(inputVariants({ hasError: !!error }))"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <p v-if="error" :id="`${id}-error`" role="alert" class="text-xs text-destructive">
      {{ error }}
    </p>
  </div>
</template>
