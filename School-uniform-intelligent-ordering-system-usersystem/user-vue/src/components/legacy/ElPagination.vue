<template>
  <div class="el-pagination">
    <span class="el-pagination__total">共 {{ total }} 条</span>
    <select class="el-pagination__sizes" :value="pageSize" @change="handleSizeChange">
      <option v-for="size in sizeOptions" :key="size" :value="size">{{ size }}/页</option>
    </select>
    <button type="button" class="el-pagination__btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
    <button
      v-for="page in pages"
      :key="page"
      type="button"
      class="el-pagination__page"
      :class="{ 'is-active': page === currentPage }"
      @click="changePage(page)"
    >
      {{ page }}
    </button>
    <button type="button" class="el-pagination__btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
  currentPage: { type: Number, default: 1 },
})

const emit = defineEmits(['size-change', 'current-change'])

const sizeOptions = [10, 20, 30, 50]
const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const pages = computed(() => {
  const total = totalPages.value
  const current = props.currentPage
  const start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

function handleSizeChange(event) {
  emit('size-change', Number(event.target.value))
}

function changePage(page) {
  emit('current-change', Math.max(1, Math.min(totalPages.value, page)))
}
</script>

<style scoped>
.el-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.el-pagination__sizes,
.el-pagination__btn,
.el-pagination__page {
  border-radius: 10px;
  border: 1px solid rgba(107, 93, 79, 0.16);
  background: rgba(255, 250, 245, 0.92);
  padding: 8px 12px;
  color: var(--mcm-text);
}

.el-pagination__page.is-active {
  background: var(--mcm-walnut);
  color: var(--mcm-beige);
}
</style>
