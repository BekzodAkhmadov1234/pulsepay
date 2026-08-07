<script setup lang="ts">
import { RouterView, useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

function handleLogout() {
  auth.logout();
  router.push('/login');
}
</script>

<template>
  <q-layout view="hHh lpR fFf">
    <q-header
      v-if="!route.meta.authLayout && !route.meta.requiresAdmin && !route.meta.adminGuestOnly"
      elevated
      class="bg-white text-dark"
    >
      <q-toolbar style="max-width: 1024px; margin: 0 auto; width: 100%">
        <RouterLink
          to="/"
          class="text-weight-black text-dark q-mr-md"
          style="font-size: 1.125rem; text-decoration: none"
        >
          Pulse<span class="text-primary">Pay</span>
        </RouterLink>

        <q-btn
          flat
          dense
          no-caps
          label="Bosh sahifa"
          to="/"
          exact
          active-class="text-primary text-weight-medium"
        />
        <q-btn
          flat
          dense
          no-caps
          label="Kartalar"
          to="/cards"
          class="q-ml-xs"
          active-class="text-primary text-weight-medium"
        />
        <q-btn
          flat
          dense
          no-caps
          label="O'tkazmalar"
          to="/transfers"
          class="q-ml-xs"
          active-class="text-primary text-weight-medium"
        />
        <q-space />

        <template v-if="auth.isAuthenticated">
          <span class="text-body2 text-grey-6 q-mr-sm gt-sm">{{
            auth.user?.fullName || auth.user?.phoneE164
          }}</span>
          <q-btn flat dense no-caps icon="logout" label="Chiqish" @click="handleLogout" />
        </template>
        <template v-else>
          <q-btn flat dense no-caps label="Kirish" to="/login" class="q-mr-sm" />
          <q-btn unelevated color="primary" no-caps label="Boshlash" to="/register" />
        </template>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <RouterView />
    </q-page-container>
  </q-layout>
</template>
