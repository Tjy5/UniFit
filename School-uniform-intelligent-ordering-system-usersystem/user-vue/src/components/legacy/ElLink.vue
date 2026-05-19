<template>
  <button type="button" class="el-link" @click="$emit('click', $event)">
    <slot />
  </button>
</template>

<script setup>
defineEmits(['click'])
</script>

<style scoped>
.el-link {
  border: 0;
  background: transparent;
  color: var(--mcm-orange);
  cursor: pointer;
  padding: 0;
  font-weight: 600;
}
</style>
