<template>
  <form class="el-form" @submit.prevent>
    <slot />
  </form>
</template>

<script setup>
import { provide, reactive, watch } from 'vue'
import { formContextKey } from './form-context'

const props = defineProps({
  model: { type: Object, default: () => ({}) },
  rules: { type: Object, default: () => ({}) },
  labelWidth: { type: [String, Number], default: '' },
  labelPosition: { type: String, default: 'left' },
})

const errors = reactive({})
const registeredFields = new Set()
let initialModel = cloneModel(props.model)

watch(
  () => props.model,
  (value) => {
    initialModel = cloneModel(value)
  },
  { deep: true, immediate: true },
)

function cloneModel(model) {
  try {
    return JSON.parse(JSON.stringify(model || {}))
  } catch {
    return { ...(model || {}) }
  }
}

function getValueByPath(source, path) {
  if (!path) return undefined
  return path.split('.').reduce((current, key) => current?.[key], source)
}

function setValueByPath(source, path, value) {
  const segments = path.split('.')
  const last = segments.pop()
  let current = source

  for (const segment of segments) {
    if (!current[segment] || typeof current[segment] !== 'object') {
      current[segment] = {}
    }
    current = current[segment]
  }

  current[last] = value
}

function isEmptyValue(value) {
  return value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)
}

async function runRule(rule, value) {
  if (rule.required && isEmptyValue(value)) {
    throw new Error(rule.message || '该字段不能为空')
  }

  if (rule.type === 'number' && !isEmptyValue(value) && Number.isNaN(Number(value))) {
    throw new Error(rule.message || '请输入数字')
  }

  if (typeof rule.min === 'number' && !isEmptyValue(value)) {
    const numeric = Number(value)
    if (!Number.isNaN(numeric) && String(value).trim() !== '') {
      if (numeric < rule.min) {
        throw new Error(rule.message || `最小值为 ${rule.min}`)
      }
    } else if (String(value).length < rule.min) {
      throw new Error(rule.message || `长度至少为 ${rule.min}`)
    }
  }

  if (typeof rule.max === 'number' && !isEmptyValue(value)) {
    const numeric = Number(value)
    if (!Number.isNaN(numeric) && String(value).trim() !== '') {
      if (numeric > rule.max) {
        throw new Error(rule.message || `最大值为 ${rule.max}`)
      }
    } else if (String(value).length > rule.max) {
      throw new Error(rule.message || `长度不能超过 ${rule.max}`)
    }
  }

  if (typeof rule.validator === 'function') {
    await new Promise((resolve, reject) => {
      let callbackCalled = false
      const callback = (error) => {
        callbackCalled = true
        if (error) {
          reject(error instanceof Error ? error : new Error(String(error)))
          return
        }
        resolve()
      }

      const result = rule.validator(rule, value, callback)
      if (result instanceof Promise) {
        result.then(resolve).catch(reject)
      } else if (result !== undefined && !callbackCalled) {
        resolve()
      }
    })
  }
}

async function validateField(prop) {
  const rules = props.rules?.[prop] || []
  const value = getValueByPath(props.model, prop)

  try {
    for (const rule of rules) {
      await runRule(rule, value)
    }
    delete errors[prop]
    return true
  } catch (error) {
    errors[prop] = error.message || '校验失败'
    return false
  }
}

async function validate(callback) {
  const fields = Array.from(new Set([...Object.keys(props.rules || {}), ...registeredFields]))
  const results = await Promise.all(fields.map((field) => validateField(field)))
  const valid = results.every(Boolean)
  callback?.(valid)
  return valid
}

function resetFields() {
  const fields = Array.from(new Set([...Object.keys(props.rules || {}), ...registeredFields]))
  for (const field of fields) {
    setValueByPath(props.model, field, getValueByPath(initialModel, field) ?? null)
    delete errors[field]
  }
}

function clearValidate(fields) {
  if (!fields) {
    Object.keys(errors).forEach((key) => delete errors[key])
    return
  }

  const list = Array.isArray(fields) ? fields : [fields]
  list.forEach((field) => delete errors[field])
}

provide(formContextKey, {
  props,
  errors,
  registerField(prop) {
    if (prop) registeredFields.add(prop)
  },
  unregisterField(prop) {
    if (prop) registeredFields.delete(prop)
  },
  validateField,
  clearValidate,
})

defineExpose({
  validate,
  validateField,
  resetFields,
  clearValidate,
})
</script>
