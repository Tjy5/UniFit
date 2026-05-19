<template>
  <button
    v-bind="$attrs"
    class="el-button"
    :class="[variantClass, sizeClass, { 'is-loading': loading, 'is-plain': plain, 'is-text': text || type === 'text' }]"
    :disabled="disabled || loading"
    type="button"
    @click="handleClick"
  >
    <span v-if="loading" class="el-button__spinner"></span>
    <span v-if="icon && !loading" class="el-button__icon">{{ iconGlyph }}</span>
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'default' },
  size: { type: String, default: 'default' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  plain: { type: Boolean, default: false },
  text: { type: Boolean, default: false },
  icon: { type: String, default: '' },
})

const emit = defineEmits(['click'])

const iconMap = {
  'el-icon-delete': '🗑',
  'el-icon-edit': '✎',
  'el-icon-search': '⌕',
  'el-icon-plus': '+',
  'el-icon-shopping-cart-2': '🛒',
  'el-icon-star-on': '★',
  'el-icon-star-off': '☆',
}

const variantClass = computed(() => `el-button--${props.type}`)
const sizeClass = computed(() => (props.size !== 'default' ? `el-button--${props.size}` : ''))
const iconGlyph = computed(() => iconMap[props.icon] || '•')

function handleClick(event) {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
.el-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease, opacity 0.16s ease;
}

.el-button:hover:not(:disabled) {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-soft);
}

.el-button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.el-button--primary {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

.el-button--success {
  background: var(--success);
  border-color: var(--success);
  color: #fff;
}

.el-button--warning {
  background: var(--warning);
  border-color: var(--warning);
  color: #fff;
}

.el-button--danger {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}

.el-button--info {
  background: var(--info-soft);
  color: var(--info);
}

.el-button.is-plain {
  background: var(--surface);
  color: var(--brand);
}

.el-button.is-text,
.el-button--text {
  border-color: transparent;
  background: transparent;
  color: var(--brand);
  min-height: auto;
  padding: 0;
}

.el-button--small,
.el-button--mini {
  min-height: 34px;
  padding: 0 14px;
  font-size: 13px;
}

.el-button__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.el-button__icon {
  line-height: 1;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
