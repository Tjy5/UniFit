<template>
  <FeaturePlaceholder
    title="操作日志"
    description="记录管理员在订单、库存、校服资料和校准参数中的关键操作，便于审计与问题回溯。"
    :todo-items="todoItems"
  />
</template>

<script setup lang="ts">
import FeaturePlaceholder from '@/components/FeaturePlaceholder.vue'

const todoItems = [
  '对接管理员操作日志分页接口',
  '补全操作模块、操作人、结果状态和时间范围筛选',
  '支持查看请求摘要、结果信息和异常详情',
]
</script>
