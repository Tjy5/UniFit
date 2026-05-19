<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item
      v-for="item in crumbs"
      :key="item.path"
      :to="item.path === route.path ? undefined : item.path"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const crumbs = computed(() =>
  route.matched
    .filter((record) => typeof record.meta.title === 'string')
    .map((record) => ({
      path: record.path === '' ? '/' : record.path,
      title: String(record.meta.title),
    })),
)
</script>

<style scoped lang="scss">
.breadcrumb {
  display: flex;
  align-items: center;
  min-height: 18px;
}

:deep(.el-breadcrumb__inner) {
  font-weight: 500;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  font-weight: 600;
}
</style>
