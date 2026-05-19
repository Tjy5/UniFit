<template>
  <section class="section-card feature-placeholder">
    <div class="page-title">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <el-tag type="info" effect="plain">基础骨架已就位</el-tag>
    </div>
    <div class="feature-placeholder__content">
      <div class="feature-placeholder__panel">
        <h3>下一步实现</h3>
        <ul>
          <li v-for="item in todoItems" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div class="feature-placeholder__panel">
        <h3>当前收益</h3>
        <ul>
          <li>路由、权限守卫、布局容器已经统一接好</li>
          <li>后续业务开发只需要专注接口和表单逻辑</li>
          <li>页面级风格和交互骨架可直接复用</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description: string
  accent?: string
  todoItems?: string[]
}>(), {
  accent: 'var(--brand)',
  todoItems: () => ['对接真实接口', '完成筛选与表格', '补全新增/编辑弹窗'],
})
</script>

<style scoped lang="scss">
.feature-placeholder {
  position: relative;
  padding: 28px;
  overflow: hidden;
  background: var(--surface);
  border-radius: var(--radius-lg);
}

.feature-placeholder__content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.feature-placeholder__panel {
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}

.feature-placeholder__panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.feature-placeholder__panel ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .feature-placeholder__content {
    grid-template-columns: 1fr;
  }
}
</style>
