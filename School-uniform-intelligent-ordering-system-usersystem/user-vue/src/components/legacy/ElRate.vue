<template>
  <div class="el-rate">
    <button
      v-for="star in max"
      :key="star"
      type="button"
      class="el-rate__star"
      :class="{ 'is-active': star <= currentValue, 'is-disabled': disabled }"
      @click="updateValue(star)"
    >
      ★
    </button>
    <span v-if="showScore" class="el-rate__text">{{ scoreText }}</span>
    <span v-else-if="showText" class="el-rate__text">{{ textValue }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  max: { type: Number, default: 5 },
  disabled: { type: Boolean, default: false },
  showScore: { type: Boolean, default: false },
  showText: { type: Boolean, default: false },
  texts: { type: Array, default: () => [] },
  scoreTemplate: { type: String, default: '{value}' },
})

const emit = defineEmits(['update:modelValue'])

const currentValue = computed(() => Number(props.modelValue || 0))
const textValue = computed(() => props.texts[currentValue.value - 1] || '')
const scoreText = computed(() => props.scoreTemplate.replace('{value}', currentValue.value))

function updateValue(value) {
  if (!props.disabled) {
    emit('update:modelValue', value)
  }
}
</script>

<style scoped>
.el-rate {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.el-rate__star {
  border: 0;
  background: transparent;
  color: #d2c3af;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
}

.el-rate__star.is-active {
  color: #ff9900;
}

.el-rate__star.is-disabled {
  cursor: default;
}

.el-rate__text {
  margin-left: 8px;
  color: var(--mcm-text-soft);
  font-size: 13px;
}
</style>
