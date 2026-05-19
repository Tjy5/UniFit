<template>
  <DialogRoot :open="visible" @update:open="handleOpenChange">
    <DialogPortal>
      <DialogOverlay class="dialog-overlay" />
      <DialogContent class="dialog-content" :style="{ width: normalizedWidth }">
        <div class="dialog-header">
          <DialogTitle class="dialog-title">{{ title }}</DialogTitle>
          <DialogClose v-if="showClose" class="dialog-close">×</DialogClose>
        </div>
        <div class="dialog-body">
          <slot />
        </div>
        <div v-if="$slots.footer" class="dialog-footer">
          <slot name="footer" />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup>
import { computed } from 'vue'
import { DialogClose, DialogContent, DialogOverlay, DialogPortal, DialogRoot, DialogTitle } from 'radix-vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '480px' },
  showClose: { type: Boolean, default: true },
})

const emit = defineEmits(['update:visible', 'close', 'closed'])
const normalizedWidth = computed(() => props.width || '480px')

function handleOpenChange(nextOpen) {
  emit('update:visible', nextOpen)
  if (!nextOpen) {
    emit('close')
    emit('closed')
  }
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(4px);
}

.dialog-content {
  position: fixed;
  left: 50%;
  top: 50%;
  z-index: 1101;
  max-width: min(92vw, 760px);
  transform: translate(-50%, -50%);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.dialog-title {
  color: var(--text-primary);
  font-weight: 600;
}

.dialog-close {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 24px;
  cursor: pointer;
}

.dialog-body {
  padding: 20px 22px;
  color: var(--text-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 22px 20px;
}
</style>
