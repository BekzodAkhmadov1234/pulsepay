import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useAdminAuthStore } from '@/stores/adminAuth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true, authLayout: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guestOnly: true, authLayout: true },
    },
    {
      path: '/cards',
      name: 'cards',
      component: () => import('../views/CardsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/send',
      name: 'send',
      component: () => import('../views/SendMoneyView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers',
      name: 'transfers',
      component: () => import('../views/TransfersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/admin/AdminLoginView.vue'),
      meta: { adminGuestOnly: true },
    },
    {
      path: '/admin/fee-rules',
      name: 'admin-fee-rules',
      component: () => import('../views/admin/FeeRulesView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/routes',
      name: 'admin-routes',
      component: () => import('../views/admin/RoutesView.vue'),
      meta: { requiresAdmin: true },
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const adminAuth = useAdminAuthStore();

  if (to.meta.requiresAdmin && !adminAuth.isAuthenticated) {
    return { name: 'admin-login', query: { redirect: to.fullPath } };
  }
  if (to.meta.adminGuestOnly && adminAuth.isAuthenticated) {
    return { name: 'admin-fee-rules' };
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'home' };
  }
});

export default router;
