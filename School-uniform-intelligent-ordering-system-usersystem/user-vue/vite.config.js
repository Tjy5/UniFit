import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    envPrefix: ['VITE_', 'VUE_APP_'],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      host: '127.0.0.1',
      port: Number(env.USER_VUE_NEXT_PORT || 3001),
      proxy: {
        '/api': {
          target: env.USER_VUE_NEXT_API_TARGET || 'http://localhost:9090',
          changeOrigin: true,
        },
      },
    },
  }
})
