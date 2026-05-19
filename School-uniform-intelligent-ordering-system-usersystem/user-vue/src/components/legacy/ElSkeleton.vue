<template>
  <div class="el-skeleton">
    <span v-for="row in rows" :key="row" class="el-skeleton__row"></span>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
})
</script>

<style scoped>
.el-skeleton {
  display: grid;
  gap: 10px;
}

.el-skeleton__row {
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(245, 230, 211, 0.92), rgba(255, 250, 245, 0.92), rgba(245, 230, 211, 0.92));
}
</style>
