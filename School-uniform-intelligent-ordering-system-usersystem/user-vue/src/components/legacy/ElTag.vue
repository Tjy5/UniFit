<template>
  <span class="el-tag" :class="`el-tag--${type || 'default'}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  type: { type: String, default: '' },
})
</script>

<style scoped>
.el-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid rgba(107, 93, 79, 0.18);
  padding: 4px 10px;
  font-size: 12px;
  color: var(--mcm-brown);
  background: rgba(217, 118, 66, 0.12);
}

.el-tag--success {
  background: rgba(74, 124, 89, 0.12);
  color: var(--mcm-olive);
}

.el-tag--danger {
  background: rgba(229, 122, 119, 0.12);
  color: #b44a49;
}

.el-tag--warning {
  background: rgba(212, 165, 116, 0.16);
  color: #8b5f26;
}

.el-tag--info,
.el-tag--primary {
  background: rgba(125, 155, 168, 0.14);
  color: var(--mcm-blue-gray);
}
</style>
