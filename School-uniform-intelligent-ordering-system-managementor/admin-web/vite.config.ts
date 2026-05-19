import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function resolveVendorChunk(id: string) {
  const normalizedId = id.replace(/\\/g, '/')

  if (!normalizedId.includes('/node_modules/')) {
    return undefined
  }

  if (
    normalizedId.includes('/vue-router/') ||
    normalizedId.includes('/pinia/') ||
    normalizedId.includes('/vue/') ||
    normalizedId.includes('/element-plus/') ||
    normalizedId.includes('/@element-plus/icons-vue/')
  ) {
    return 'vendor-framework'
  }

  if (normalizedId.includes('/echarts/') || normalizedId.includes('/zrender/')) {
    return 'vendor-echarts'
  }

  if (normalizedId.includes('/@wangeditor/editor-for-vue/')) {
    return 'vendor-editor-vue'
  }

  if (normalizedId.includes('/@wangeditor/editor/')) {
    return 'vendor-editor-core'
  }

  return undefined
}

export default defineConfig({
  plugins: [vue()],
  define: {
    __VUE_OPTIONS_API__: true,
    __VUE_PROD_DEVTOOLS__: false,
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false,
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern',
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 1100,
    rollupOptions: {
      output: {
        manualChunks: resolveVendorChunk,
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    hmr: {
      host: '127.0.0.1',
      protocol: 'ws',
      clientPort: 5173,
    },
    proxy: {
      '/admin-api': {
        target: 'http://localhost:9091',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/admin-api/, ''),
      },
    },
  },
})
