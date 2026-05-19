<template>
  <div class="el-row" :style="rowStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed, provide } from 'vue'

const props = defineProps({
  gutter: { type: Number, default: 0 },
})

provide('legacy-row-gutter', computed(() => props.gutter))

const rowStyle = computed(() => ({
  display: 'flex',
  flexWrap: 'wrap',
  marginLeft: props.gutter ? `-${props.gutter / 2}px` : undefined,
  marginRight: props.gutter ? `-${props.gutter / 2}px` : undefined,
}))
</script>
