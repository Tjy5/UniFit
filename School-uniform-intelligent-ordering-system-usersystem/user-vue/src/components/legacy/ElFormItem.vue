<template>
  <div class="el-form-item" :class="{ 'is-error': errorMessage }">
    <label v-if="label" class="el-form-item__label" :style="labelStyle">{{ label }}</label>
    <div class="el-form-item__content">
      <slot />
      <p v-if="errorMessage" class="el-form-item__error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useFormContext } from './form-context'

const props = defineProps({
  label: { type: String, default: '' },
  prop: { type: String, default: '' },
})

const form = useFormContext()

onMounted(() => {
  form?.registerField(props.prop)
})

onBeforeUnmount(() => {
  form?.unregisterField(props.prop)
})

const errorMessage = computed(() => form?.errors?.[props.prop] || '')
const labelStyle = computed(() => {
  if (!form?.props?.labelWidth) return {}
  return { width: typeof form.props.labelWidth === 'number' ? `${form.props.labelWidth}px` : form.props.labelWidth }
})
</script>

<style scoped>
.el-form-item {
  margin-bottom: 18px;
}

.el-form-item__label {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--mcm-brown);
  font-weight: 600;
}

.el-form-item__error {
  margin: 6px 0 0;
  color: #c95854;
  font-size: 12px;
}
</style>
