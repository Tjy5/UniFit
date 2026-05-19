<template>
  <label class="el-radio" :class="{ 'is-border': border }">
    <input class="el-radio__native" type="radio" :checked="checked" @change="handleChange" />
    <span class="el-radio__inner"></span>
    <span class="el-radio__label"><slot>{{ label }}</slot></span>
  </label>
</template>

<script setup>
import { computed } from 'vue'
import { useRadioGroup } from './radio-context'

const props = defineProps({
  label: { type: null, default: null },
  border: { type: Boolean, default: false },
})

const group = useRadioGroup()
const checked = computed(() => group?.modelValue() === props.label)

function handleChange() {
  group?.update(props.label)
}
</script>

<style scoped>
.el-radio {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.el-radio.is-border {
  padding: 10px 12px;
  border: 1px solid rgba(107, 93, 79, 0.18);
  border-radius: 14px;
  background: rgba(255, 250, 245, 0.92);
}

.el-radio__native {
  display: none;
}

.el-radio__inner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--mcm-orange);
  box-sizing: border-box;
  background: radial-gradient(circle, var(--mcm-orange) 0 45%, transparent 50% 100%);
}
</style>
