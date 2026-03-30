import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite' // Bunu ekledik

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), // Bunu ekledik
  ],
})