import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite' // Bunu ekledik

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), // Bunu ekledik
  ],
  server: {
    proxy: {
      "/api": {
        target: "http://127.0.0.1:5231",
        changeOrigin: true,
        secure: false,
      },
    },
  },
})