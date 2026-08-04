<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router';
</script>

<template>
  <header class="bg-gray-900 text-white px-6 py-4 shadow">
    <nav class="flex items-center gap-6 max-w-5xl mx-auto">
      <span class="font-bold text-lg text-purple-400">PulsePay</span>
      <RouterLink
        to="/"
        class="text-gray-300 hover:text-white transition-colors"
        active-class="text-white font-semibold"
      >
        Home
      </RouterLink>
      <RouterLink
        to="/about"
        class="text-gray-300 hover:text-white transition-colors"
        active-class="text-white font-semibold"
      >
        About
      </RouterLink>
    </nav>
  </header>

  <main class="max-w-5xl mx-auto px-6 py-8">
    <RouterView />
  </main>
</template>
