import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { quasar, transformAssetUrls } from '@quasar/vite-plugin';
import { fileURLToPath, URL } from 'node:url';
import path from 'node:path';

const __dirname = fileURLToPath(new URL('.', import.meta.url));

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: { transformAssetUrls },
    }),
    quasar({
      sassVariables: path.resolve(__dirname, 'src/css/quasar.variables.sass'),
    }),
  ],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/admin/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/merchant/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  test: {
    globals: true,
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
    },
  },
});
