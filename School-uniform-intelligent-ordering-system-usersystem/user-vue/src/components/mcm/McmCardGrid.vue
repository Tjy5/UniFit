<template>
  <div class="mcm-card-grid" :class="$attrs.class">
    <slot />
  </div>
</template>

<script setup>
defineOptions({ inheritAttrs: false })
</script>

<style scoped>
.mcm-card-grid {
  display: grid;
  gap: 20px;
}
</style>
