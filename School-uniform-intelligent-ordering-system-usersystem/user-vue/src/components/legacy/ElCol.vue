<template>
  <div class="el-col" :style="colStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'

const props = defineProps({
  span: { type: Number, default: 24 },
  xs: { type: Number, default: undefined },
  sm: { type: Number, default: undefined },
  md: { type: Number, default: undefined },
  lg: { type: Number, default: undefined },
})

const gutter = inject('legacy-row-gutter', computed(() => 0))

const colStyle = computed(() => ({
  width: `${(((props.md ?? props.sm ?? props.xs ?? props.lg ?? props.span) || 24) / 24) * 100}%`,
  paddingLeft: gutter.value ? `${gutter.value / 2}px` : undefined,
  paddingRight: gutter.value ? `${gutter.value / 2}px` : undefined,
  boxSizing: 'border-box',
}))
</script>
