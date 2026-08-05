<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60',
  {
    variants: {
      variant: {
        primary:
          'bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 focus-visible:ring-brand-500',
      },
    },
    defaultVariants: { variant: 'primary' },
  }
);

type ButtonVariants = VariantProps<typeof buttonVariants>;

const props = defineProps<{
  loading?: boolean;
  disabled?: boolean;
  variant?: ButtonVariants['variant'];
}>();
</script>

<template>
  <button
    type="submit"
    :disabled="props.loading || props.disabled"
    :class="cn(buttonVariants({ variant: props.variant }))"
  >
    <svg
      v-if="props.loading"
      class="h-4 w-4 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
    <slot />
  </button>
</template>
