<template>
  <div class="image-upload">
    <el-upload
      v-model:file-list="fileList"
      list-type="picture-card"
      :limit="limit"
      :multiple="true"
      :accept="acceptString"
      :http-request="handleUploadRequest"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <el-dialog v-model="previewVisible" title="图片预览" width="720px">
      <img class="image-upload__preview" :src="previewUrl" alt="preview" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type {
  UploadFile,
  UploadFiles,
  UploadProps,
  UploadRequestOptions,
  UploadUserFile,
} from 'element-plus'
import { computed, ref, watch } from 'vue'
import { uploadFile } from '@/api/upload'

const props = withDefaults(defineProps<{
  modelValue: string[]
  limit?: number
  fileSize?: number
  fileTypes?: string[]
}>(), {
  limit: 6,
  fileSize: 5,
  fileTypes: () => ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'],
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const fileList = ref<UploadUserFile[]>([])
const previewVisible = ref(false)
const previewUrl = ref('')

const acceptString = computed(() => props.fileTypes.join(','))

watch(
  () => props.modelValue,
  (value) => {
    fileList.value = value.map((url, index) => ({
      name: `image-${index + 1}`,
      url,
    }))
  },
  { immediate: true },
)

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (!props.fileTypes.includes(rawFile.type)) {
    ElMessage.error('图片格式不支持')
    return false
  }
  if (rawFile.size / 1024 / 1024 > props.fileSize) {
    ElMessage.error(`图片大小不能超过 ${props.fileSize}MB`)
    return false
  }
  return true
}

async function handleUploadRequest(options: UploadRequestOptions) {
  try {
    const { data } = await uploadFile(options.file as File)
    options.onSuccess?.(data)
    syncModelValue([
      ...fileList.value,
      {
        name: data.data.originalFilename,
        url: data.data.url,
      },
    ] as UploadFiles)
  } catch (error) {
    options.onError?.(error as never)
  }
}

function syncModelValue(files: UploadFile[] | UploadFiles) {
  const urls = files
    .map((item) => item.url)
    .filter((value): value is string => Boolean(value))
  emit('update:modelValue', urls)
}

function handleRemove(_file: UploadFile, files: UploadFiles) {
  syncModelValue(files)
}

function handlePreview(file: UploadFile) {
  previewUrl.value = file.url || ''
  previewVisible.value = true
}
</script>

<style scoped lang="scss">
.image-upload {
  width: 100%;
}

:deep(.el-upload--picture-card) {
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  border-radius: var(--radius-md);
  border-color: var(--line);
}

.image-upload__preview {
  display: block;
  max-width: 100%;
  margin: 0 auto;
  border-radius: var(--radius-md);
}
</style>
