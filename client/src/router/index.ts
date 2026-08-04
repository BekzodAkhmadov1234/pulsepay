import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      // Eager-loaded — the entry page should be immediately available
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/about',
      name: 'about',
      // Lazy-loaded — split into a separate chunk
      component: () => import('../views/AboutView.vue'),
    },
  ],
});

export default router;
