<template>
  <div class="el-input-number">
    <button type="button" class="el-input-number__btn" :disabled="disabled || currentValue <= min" @click="stepDown">-</button>
    <input
      class="el-input-number__input"
      type="number"
      :value="currentValue"
      :min="min"
      :max="max"
      :step="step"
      :disabled="disabled"
      @input="handleInput"
      @change="emitChange"
    />
    <button type="button" class="el-input-number__btn" :disabled="disabled || currentValue >= max" @click="stepUp">+</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  min: { type: Number, default: Number.NEGATIVE_INFINITY },
  max: { type: Number, default: Number.POSITIVE_INFINITY },
  step: { type: Number, default: 1 },
  precision: { type: Number, default: undefined },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const currentValue = computed(() => Number(props.modelValue ?? 0))

function normalize(value) {
  let next = Number(value)
  if (Number.isNaN(next)) {
    next = props.min !== Number.NEGATIVE_INFINITY ? props.min : 0
  }
  next = Math.min(props.max, Math.max(props.min, next))
  if (typeof props.precision === 'number') {
    next = Number(next.toFixed(props.precision))
  }
  return next
}

function updateValue(value) {
  const next = normalize(value)
  emit('update:modelValue', next)
  emit('change', next)
}

function stepDown() {
  updateValue(currentValue.value - props.step)
}

function stepUp() {
  updateValue(currentValue.value + props.step)
}

function handleInput(event) {
  emit('update:modelValue', normalize(event.target.value))
}

function emitChange(event) {
  updateValue(event.target.value)
}
</script>

<style scoped>
.el-input-number {
  display: flex;
  align-items: center;
  border-radius: 12px;
  border: 1px solid rgba(107, 93, 79, 0.35);
  background: rgba(255, 250, 245, 0.94);
  overflow: hidden;
}

.el-input-number__btn {
  width: 38px;
  height: 42px;
  border: 0;
  background: rgba(245, 230, 211, 0.72);
  color: var(--mcm-brown);
  cursor: pointer;
}

.el-input-number__input {
  width: 100%;
  border: 0;
  background: transparent;
  text-align: center;
  font: inherit;
  padding: 0 8px;
}

.el-input-number__input:focus {
  outline: none;
}
</style>
