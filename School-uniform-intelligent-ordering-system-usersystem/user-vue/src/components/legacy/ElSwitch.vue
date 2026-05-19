<template>
  <button type="button" class="el-switch" :class="{ 'is-checked': modelValue }" @click="$emit('update:modelValue', !modelValue)">
    <span class="el-switch__core"></span>
  </button>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.el-switch {
  position: relative;
  width: 44px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  background: var(--line-strong);
  cursor: pointer;
}

.el-switch__core {
  position: absolute;
  left: 3px;
  top: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s ease;
}

.el-switch.is-checked {
  background: var(--brand);
}

.el-switch.is-checked .el-switch__core {
  transform: translateX(20px);
}
</style>
