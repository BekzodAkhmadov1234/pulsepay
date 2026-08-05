<script setup lang="ts">
import { RouterView, RouterLink, useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { LogOut } from '@lucide/vue';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

function handleLogout() {
  auth.logout();
  router.push('/login');
}
</script>

<template>
  <!-- Auth routes: full-page, no global chrome -->
  <RouterView v-if="route.meta.authLayout" />

  <!-- App routes: topbar + constrained content -->
  <template v-else>
    <header
      class="sticky top-0 z-40 flex h-14 items-center border-b border-border bg-background/95 backdrop-blur-sm"
    >
      <nav class="mx-auto flex w-full max-w-5xl items-center gap-6 px-6">
        <!-- Logo -->
        <RouterLink to="/" class="text-lg font-black tracking-tight text-foreground">
          Pulse<span class="text-brand-600">Pay</span>
        </RouterLink>

        <!-- Nav links -->
        <RouterLink
          to="/"
          class="text-sm text-muted-fg transition-colors hover:text-foreground"
          active-class="font-medium text-foreground"
        >
          Dashboard
        </RouterLink>

        <!-- Right controls -->
        <div class="ml-auto flex items-center gap-2">
          <template v-if="auth.isAuthenticated">
            <span class="hidden text-sm text-muted-fg sm:block">
              {{ auth.user?.phoneE164 }}
            </span>
            <button
              class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm text-muted-fg transition-colors hover:bg-muted hover:text-foreground"
              @click="handleLogout"
            >
              <LogOut class="h-4 w-4" />
              <span>Log out</span>
            </button>
          </template>
          <template v-else>
            <RouterLink
              to="/login"
              class="rounded-lg px-3 py-1.5 text-sm text-muted-fg transition-colors hover:text-foreground"
            >
              Log in
            </RouterLink>
            <RouterLink
              to="/register"
              class="rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-700"
            >
              Get started
            </RouterLink>
          </template>
        </div>
      </nav>
    </header>

    <main class="mx-auto max-w-5xl px-6 py-8">
      <RouterView />
    </main>
  </template>
</template>
