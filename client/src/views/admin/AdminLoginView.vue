<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { ApiError } from '@/lib/api/client';

const router = useRouter();
const route = useRoute();
const adminAuth = useAdminAuthStore();

const email = ref('');
const password = ref('');
const serverError = ref('');
const showPassword = ref(false);

async function handleLogin() {
  serverError.value = '';
  try {
    await adminAuth.login(email.value, password.value);
    const redirect =
      typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/fee-rules';
    await router.push(redirect);
  } catch (err) {
    if (err instanceof ApiError) {
      serverError.value = err.status === 401 ? 'Invalid email or password.' : err.message;
    } else {
      serverError.value = 'Something went wrong. Please try again.';
    }
  }
}
</script>

<template>
  <q-page class="row items-center justify-center bg-grey-1" style="min-height: 100vh">
    <q-card flat bordered style="width: 100%; max-width: 400px" class="q-pa-lg">
      <div class="text-center q-mb-lg">
        <div class="text-h6 text-weight-black text-dark">
          Pulse<span class="text-primary">Pay</span>
          <span class="text-caption text-grey-6 q-ml-sm">Admin</span>
        </div>
        <p class="text-body2 text-grey-6 q-mt-xs q-mb-none">Sign in to the admin portal</p>
      </div>

      <q-form class="column q-gutter-y-md" @submit.prevent="handleLogin">
        <q-banner v-if="serverError" dense rounded class="bg-red-1 text-negative">
          <template #avatar>
            <q-icon name="warning" color="negative" />
          </template>
          {{ serverError }}
        </q-banner>

        <q-input
          v-model="email"
          type="email"
          label="Email"
          outlined
          autocomplete="email"
          :disable="adminAuth.isLoading"
        />

        <q-input
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          label="Password"
          outlined
          autocomplete="off"
          :disable="adminAuth.isLoading"
        >
          <template #append>
            <q-icon
              :name="showPassword ? 'visibility_off' : 'visibility'"
              class="cursor-pointer"
              @click="showPassword = !showPassword"
            />
          </template>
        </q-input>

        <q-btn
          type="submit"
          label="Sign in"
          color="primary"
          unelevated
          class="full-width"
          size="md"
          padding="12px"
          :loading="adminAuth.isLoading"
        />
      </q-form>
    </q-card>
  </q-page>
</template>
