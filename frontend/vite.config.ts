import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * WHY proxy in vite.config?
 * Avoids CORS issues in local dev. The frontend calls /api/* which Vite
 * proxies to the appropriate backend service. In production, Nginx/Kong does this.
 * No CORS headers needed on the backend during development.
 */
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',  // sockjs-client expects Node's `global` — polyfill with browser's globalThis
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
})
