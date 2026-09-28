import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El backend Spring Boot corre en :8080. En desarrollo, Vite proxea /api → backend
// (y le quita el prefijo /api con rewrite). Así el front usa rutas relativas /api/... y
// no hace falta configurar CORS en el backend para el MVP.
// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
