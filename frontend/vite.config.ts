import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 开发环境代理到服务器后端（改完前端代码可在本地秒级热更新预览，接口走真实后端）
      '/api': {
        target: 'http://192.168.2.136:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://192.168.2.136:8080',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})