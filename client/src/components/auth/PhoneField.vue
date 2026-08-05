<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const PREFIX = '+998';

const props = defineProps<{
  id: string;
  label: string;
  modelValue: string;
  error?: string;
  autocomplete?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);

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

onMounted(() => {
  if (!props.modelValue.startsWith(PREFIX)) {
    emit('update:modelValue', PREFIX);
  }
});

watch(
  () => props.modelValue,
  (val) => {
    if (!val.startsWith(PREFIX)) {
      emit('update:modelValue', PREFIX);
    }
  }
);

function clampCursor() {
  const el = inputRef.value;
  if (!el) return;
  const s = el.selectionStart ?? 0;
  const e = el.selectionEnd ?? 0;
  if (s < PREFIX.length) {
    el.setSelectionRange(PREFIX.length, Math.max(PREFIX.length, e));
  }
}

function onFocus() {
  requestAnimationFrame(clampCursor);
}

function onClick() {
  requestAnimationFrame(clampCursor);
}

function onKeydown(e: KeyboardEvent) {
  const el = inputRef.value!;
  const start = el.selectionStart ?? 0;
  const end = el.selectionEnd ?? 0;

  if (e.key === 'Backspace' && start <= PREFIX.length && start === end) {
    e.preventDefault();
    return;
  }
  if ((e.key === 'Backspace' || e.key === 'Delete') && start < PREFIX.length) {
    e.preventDefault();
    return;
  }
  if (e.key === 'Home') {
    e.preventDefault();
    el.setSelectionRange(PREFIX.length, e.shiftKey ? end : PREFIX.length);
    return;
  }
}

function onBeforeInput(e: InputEvent) {
  const el = inputRef.value!;
  const start = el.selectionStart ?? 0;
  const end = el.selectionEnd ?? 0;
  const type = e.inputType;

  if (type === 'deleteContentBackward') {
    if (start <= PREFIX.length && start === end) {
      e.preventDefault();
      return;
    }
    if (start < PREFIX.length) {
      e.preventDefault();
      return;
    }
  }
  if (type === 'deleteContentForward' && start < PREFIX.length) {
    e.preventDefault();
    return;
  }
  if ((type === 'deleteByCut' || type === 'deleteByDrag') && start < PREFIX.length) {
    e.preventDefault();
    return;
  }
  if (type.startsWith('insert') && type !== 'insertFromPaste' && start < PREFIX.length) {
    e.preventDefault();
    return;
  }
  if (type === 'insertText') {
    if (!/^\d+$/.test(e.data ?? '')) {
      e.preventDefault();
      return;
    }
    const selectionLen = end - start;
    if (el.value.length - selectionLen + (e.data?.length ?? 0) > 13) {
      e.preventDefault();
      return;
    }
  }
}

function onPaste(e: ClipboardEvent) {
  e.preventDefault();
  const el = inputRef.value!;
  const raw = e.clipboardData?.getData('text') ?? '';

  let digits = raw.replace(/\D/g, '');
  if (digits.startsWith('998')) digits = digits.slice(3);

  const cursorStart = Math.max(PREFIX.length, el.selectionStart ?? PREFIX.length);
  const cursorEnd = Math.max(PREFIX.length, el.selectionEnd ?? PREFIX.length);
  const existingDigits = el.value.slice(PREFIX.length);
  const before = existingDigits.slice(0, cursorStart - PREFIX.length);
  const after = existingDigits.slice(cursorEnd - PREFIX.length);

  const merged = (before + digits + after).slice(0, 9);
  const newValue = PREFIX + merged;
  const newCursor = PREFIX.length + Math.min(before.length + digits.length, 9);

  emit('update:modelValue', newValue);
  requestAnimationFrame(() => {
    if (inputRef.value) {
      inputRef.value.value = newValue;
      inputRef.value.setSelectionRange(newCursor, newCursor);
    }
  });
}

function onInput(e: Event) {
  const el = e.target as HTMLInputElement;
  let val = el.value;

  if (!val.startsWith(PREFIX)) {
    const digits = val.replace(/\D/g, '').replace(/^998/, '').slice(0, 9);
    val = PREFIX + digits;
  } else {
    const afterPrefix = val.slice(PREFIX.length).replace(/\D/g, '').slice(0, 9);
    val = PREFIX + afterPrefix;
  }

  if (el.value !== val) {
    const cursor = el.selectionStart ?? val.length;
    el.value = val;
    const clamped = Math.max(PREFIX.length, Math.min(cursor, val.length));
    el.setSelectionRange(clamped, clamped);
  }

  emit('update:modelValue', val);
}
</script>

<template>
  <div class="space-y-1.5">
    <label :for="id" class="block text-sm font-medium text-foreground">
      {{ label }}
    </label>
    <input
      :id="id"
      ref="inputRef"
      type="tel"
      :value="modelValue.startsWith(PREFIX) ? modelValue : PREFIX"
      :autocomplete="autocomplete ?? 'tel'"
      maxlength="13"
      inputmode="tel"
      :aria-invalid="!!error || undefined"
      :aria-describedby="error ? `${id}-error` : undefined"
      :class="cn(inputVariants({ hasError: !!error }))"
      @focus="onFocus"
      @click="onClick"
      @keydown="onKeydown"
      @beforeinput="onBeforeInput"
      @paste="onPaste"
      @input="onInput"
    />
    <p v-if="error" :id="`${id}-error`" role="alert" class="text-xs text-destructive">
      {{ error }}
    </p>
  </div>
</template>
