import '@quasar/extras/material-icons/material-icons.css';
import 'quasar/src/css/index.sass';
import './style.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { Quasar, Notify, Dialog } from 'quasar';
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 1000 * 60 * 5 }, // 5 minutes
  },
});

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(VueQueryPlugin, { queryClient });
app.use(Quasar, {
  plugins: { Notify, Dialog },
  config: {
    brand: {
      primary: '#7c3aed',
      secondary: '#6d28d9',
      accent: '#8b5cf6',
      positive: '#16a34a',
      negative: '#dc2626',
      info: '#0ea5e9',
      warning: '#d97706',
    },
  },
});

// Restore auth session from localStorage before first render
useAuthStore().fetchCurrentUser();

app.mount('#app');
