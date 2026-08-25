import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  // The app is served under /ui by the Apache proxy (see proxy/httpd-app.conf),
  // so built asset URLs have to be prefixed with it -- otherwise index.html asks
  // for /assets/... at the server root, which Apache does not serve. Matches the
  // BrowserRouter basename in src/main.tsx.
  base: '/ui/',
  plugins: [react()],
  server: {
    proxy: {
      '/auth-api': { target: 'http://localhost:8080', changeOrigin: true },
      '/student-api': { target: 'http://localhost:8080', changeOrigin: true },
      '/faculty-api': { target: 'http://localhost:8080', changeOrigin: true },
      '/classes-api': { target: 'http://localhost:8080', changeOrigin: true },
      '/schedule-api': { target: 'http://localhost:8080', changeOrigin: true },
      '/register-api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
