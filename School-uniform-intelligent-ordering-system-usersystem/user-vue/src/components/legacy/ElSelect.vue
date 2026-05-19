<template>
  <div class="el-select">
    <select class="el-select__inner" :disabled="disabled || loading" :value="selectedIndex" @change="handleChange">
      <option v-if="placeholder || clearable" value="__empty__">{{ placeholder || '请选择' }}</option>
      <option
        v-for="(option, index) in options"
        :key="index"
        :value="String(index)"
        :disabled="option.disabled"
      >
        {{ option.label }}
      </option>
    </select>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

const props = defineProps({
  modelValue: { type: null, default: null },
  placeholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])
const slots = useSlots()

function flatten(nodes, result = []) {
  for (const node of nodes || []) {
    if (Array.isArray(node.children)) {
      flatten(node.children, result)
      continue
    }

    if (node.type?.name === 'ElOption' || node.type?.__name === 'ElOption') {
      result.push({
        label: node.props?.label ?? node.props?.value,
        value: node.props?.value,
        disabled: Boolean(node.props?.disabled),
      })
    }
  }
  return result
}

const options = computed(() => flatten(slots.default?.() || []))
const selectedIndex = computed(() => {
  const index = options.value.findIndex((option) => Object.is(option.value, props.modelValue) || String(option.value) === String(props.modelValue))
  return index >= 0 ? String(index) : '__empty__'
})

function handleChange(event) {
  const { value } = event.target
  if (value === '__empty__') {
    emit('update:modelValue', null)
    emit('change', null)
    return
  }

  const option = options.value[Number(value)]
  emit('update:modelValue', option?.value ?? null)
  emit('change', option?.value ?? null)
}
</script>

<style scoped>
.el-select__inner {
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid rgba(107, 93, 79, 0.35);
  background: rgba(255, 250, 245, 0.94);
  color: var(--mcm-text);
  padding: 12px 14px;
  font: inherit;
}
</style>
