<template>
  <div class="el-input" :class="{ 'is-textarea': type === 'textarea' }">
    <textarea
      v-if="type === 'textarea'"
      class="el-input__inner el-textarea__inner"
      :rows="rows"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :value="stringValue"
      @input="handleInput"
      @blur="$emit('blur', $event)"
    />
    <template v-else>
      <input
        class="el-input__inner"
        :type="resolvedType"
        :placeholder="placeholder"
        :maxlength="maxlength"
        :value="stringValue"
        :disabled="disabled"
        @input="handleInput"
        @blur="$emit('blur', $event)"
        @focus="$emit('focus', $event)"
      />
      <button v-if="showPassword && type === 'password'" type="button" class="el-input__action" @click="passwordVisible = !passwordVisible">
        {{ passwordVisible ? '隐藏' : '显示' }}
      </button>
      <button
        v-if="clearable && stringValue"
        type="button"
        class="el-input__action"
        @click="clearValue"
      >
        清除
      </button>
    </template>
    <div v-if="showWordLimit && maxlength" class="el-input__count">{{ stringValue.length }}/{{ maxlength }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  maxlength: { type: [String, Number], default: undefined },
  rows: { type: [String, Number], default: 4 },
  clearable: { type: Boolean, default: false },
  showPassword: { type: Boolean, default: false },
  showWordLimit: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'clear'])
const passwordVisible = ref(false)

const stringValue = computed(() => (props.modelValue ?? '').toString())
const resolvedType = computed(() => {
  if (props.type === 'password' && props.showPassword) {
    return passwordVisible.value ? 'text' : 'password'
  }
  return props.type
})

function handleInput(event) {
  emit('update:modelValue', event.target.value)
}

function clearValue() {
  emit('update:modelValue', '')
  emit('clear')
}
</script>

<style scoped>
.el-input {
  position: relative;
}

.el-input__inner,
.el-textarea__inner {
  width: 100%;
  box-sizing: border-box;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--text-primary);
  padding: 12px 14px;
  font: inherit;
}

.el-input__inner:focus,
.el-textarea__inner:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}

.el-input__action {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}

.el-input__count {
  margin-top: 6px;
  text-align: right;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
