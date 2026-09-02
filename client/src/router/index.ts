import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { useMerchantAuthStore } from '@/stores/merchantAuth';

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
      component: () => import('../views/LoginView.vue'),
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
      path: '/send/bank',
      name: 'bank-transfer',
      component: () => import('../views/BankTransferView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/top-up',
      name: 'top-up',
      component: () => import('../views/A2PTransferView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers',
      name: 'transfers',
      component: () => import('../views/TransfersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/reports',
      name: 'reports',
      component: () => import('../views/HisobotlarView.vue'),
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
      path: '/admin/merchants',
      name: 'admin-merchants',
      component: () => import('../views/admin/MerchantsView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/merchant/login',
      name: 'merchant-login',
      component: () => import('../views/merchant/MerchantLoginView.vue'),
      meta: { merchantGuestOnly: true },
    },
    {
      path: '/merchant/dashboard',
      name: 'merchant-dashboard',
      component: () => import('../views/merchant/MerchantDashboardView.vue'),
      meta: { requiresMerchant: true },
    },
    {
      path: '/merchant/terminal',
      name: 'merchant-terminal',
      component: () => import('../views/merchant/VirtualTerminalView.vue'),
      meta: { requiresMerchant: true },
    },
    {
      path: '/merchant/settlements',
      name: 'merchant-settlements',
      component: () => import('../views/merchant/MerchantSettlementsView.vue'),
      meta: { requiresMerchant: true },
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const adminAuth = useAdminAuthStore();
  const merchantAuth = useMerchantAuthStore();

  if (to.meta.requiresAdmin && !adminAuth.isAuthenticated) {
    return { name: 'admin-login', query: { redirect: to.fullPath } };
  }
  if (to.meta.adminGuestOnly && adminAuth.isAuthenticated) {
    return { name: 'admin-fee-rules' };
  }
  if (to.meta.requiresMerchant && !merchantAuth.isAuthenticated) {
    return { name: 'merchant-login', query: { redirect: to.fullPath } };
  }
  if (to.meta.merchantGuestOnly && merchantAuth.isAuthenticated) {
    return { name: 'merchant-dashboard' };
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'home' };
  }
});

export default router;
