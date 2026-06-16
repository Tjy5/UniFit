<template>
  <FeaturePlaceholder
    title="用户行为日志"
    description="集中承接浏览、详情、推荐、加购和下单等用户行为记录，支撑行为漏斗与入口转化分析。"
    :todo-items="todoItems"
  />
</template>

<script setup lang="ts">
import FeaturePlaceholder from '@/components/FeaturePlaceholder.vue'

const todoItems = [
  '对接用户行为日志分页接口',
  '补全用户、会话、行为类型和时间范围筛选',
  '支持查看行为上下文与推荐入口信息',
]
</script>
