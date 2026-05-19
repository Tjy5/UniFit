<template>
  <div class="rich-text-editor section-card">
    <Toolbar class="rich-text-editor__toolbar" :default-config="toolbarConfig" :editor="editorRef" mode="default" />
    <Editor
      v-model="htmlValue"
      class="rich-text-editor__content"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, shallowRef, watch } from 'vue'
import { getToken } from '@/utils/auth'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
}>(), {
  placeholder: '请输入内容...',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = shallowRef<IDomEditor>()
const htmlValue = shallowRef(props.modelValue)

const toolbarConfig: Partial<IToolbarConfig> = {}
const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      server: `${import.meta.env.VITE_API_BASE_URL ?? '/admin-api'}/upload`,
      fieldName: 'file',
      headers: {
        Authorization: `Bearer ${getToken()}`,
      },
      customInsert(res: { code: number; msg: string; data: { url: string } }, insertFn: (url: string) => void) {
        if (res.code !== 200) {
          ElMessage.error(res.msg || '图片上传失败')
          return
        }
        insertFn(res.data.url)
      },
    },
  },
}

watch(
  () => props.modelValue,
  (value) => {
    if (value !== htmlValue.value) {
      htmlValue.value = value
    }
  },
)

watch(htmlValue, (value) => {
  emit('update:modelValue', value || '')
})

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped lang="scss">
.rich-text-editor {
  overflow: hidden;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--surface);
}

.rich-text-editor__toolbar {
  border-bottom: 1px solid var(--line);
  background: var(--surface-muted);
}

.rich-text-editor__content {
  min-height: 320px;
}

:deep(.w-e-bar) {
  background: var(--surface-muted);
  border-bottom: 1px solid var(--line);
}

:deep(.w-e-toolbar button:hover) {
  background: var(--brand-soft);
}

:deep(.w-e-text-container) {
  border: 1px solid var(--line) !important;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  background: var(--surface);
}
</style>
