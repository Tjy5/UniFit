<template>
  <div class="el-radio-group">
    <slot />
  </div>
</template>

<script setup>
import { provide } from 'vue'
import { radioGroupKey } from './radio-context'

const props = defineProps({
  modelValue: { type: null, default: null },
})

const emit = defineEmits(['update:modelValue'])

provide(radioGroupKey, {
  modelValue: () => props.modelValue,
  update(value) {
    emit('update:modelValue', value)
  },
})
</script>

<style scoped>
.el-radio-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
</style>
