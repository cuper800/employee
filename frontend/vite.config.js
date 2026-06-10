import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 開發伺服器將 /api 代理至後端 Spring Boot (localhost:8088)
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }
})
