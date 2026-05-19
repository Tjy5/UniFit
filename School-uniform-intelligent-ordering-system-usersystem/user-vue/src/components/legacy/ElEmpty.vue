<template>
  <div class="el-empty">
    <p class="el-empty__description">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
})
</script>

<style scoped>
.el-empty {
  display: grid;
  gap: 14px;
  justify-items: center;
  color: var(--mcm-text-soft);
  padding: 24px;
}
</style>
