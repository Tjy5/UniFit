<template>
  <el-button class="export-button" :loading="loading" @click="handleExport">
    <el-icon><Download /></el-icon>
    导出
  </el-button>
</template>

<script setup lang="ts">
import axios from 'axios'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { getToken } from '@/utils/auth'

const props = withDefaults(defineProps<{
  url: string
  filename?: string
  method?: 'get' | 'post'
  payload?: Record<string, unknown>
}>(), {
  filename: 'export.xlsx',
  method: 'post',
  payload: () => ({}),
})

const loading = ref(false)

async function handleExport() {
  loading.value = true
  try {
    const response = await axios.request<Blob>({
      url: `${import.meta.env.VITE_API_BASE_URL ?? '/admin-api'}${props.url}`,
      method: props.method,
      params: props.payload,
      responseType: 'blob',
      headers: {
        Authorization: `Bearer ${getToken()}`,
      },
    })

    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = props.filename
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (error) {
    ElMessage.error('导出失败')
    throw error
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.export-button {
  color: var(--brand);
  background: var(--brand-soft);
  border-color: #b3cee0;
  border-radius: var(--radius-md);
}

.export-button:hover,
.export-button:focus {
  color: var(--brand-hover);
  background: #d5e6f2;
  border-color: #9abbd2;
}
</style>
