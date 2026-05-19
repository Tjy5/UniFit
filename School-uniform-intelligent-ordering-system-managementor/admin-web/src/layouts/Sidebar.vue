<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': appStore.sidebarCollapsed }">
    <div class="sidebar__brand">
      <div class="sidebar__crest">SU</div>
      <div v-if="!appStore.sidebarCollapsed" class="sidebar__copy">
        <small class="sidebar__eyebrow">运营后台</small>
        <strong>校服智能订购</strong>
        <span>订单与商品管理</span>
      </div>
    </div>

    <el-scrollbar class="sidebar__scroll">
      <el-menu
        :collapse="appStore.sidebarCollapsed"
        :default-active="route.path"
        class="sidebar__menu"
        router
      >
        <template v-for="group in menuGroups" :key="group.title">
          <el-sub-menu :index="group.title">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import {
  DataAnalysis,
  DataBoard,
  Goods,
  Histogram,
  List,
  Notebook,
  OfficeBuilding,
  Operation,
  Promotion,
  TrendCharts,
  WarningFilled,
} from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()

const menuGroups = [
  {
    title: '运营概览',
    icon: DataBoard,
    items: [{ path: '/', label: '工作台', icon: Histogram }],
  },
  {
    title: '商品管理',
    icon: Goods,
    items: [
      { path: '/uniforms', label: '校服管理', icon: Goods },
      { path: '/sizes', label: '尺码管理', icon: Operation },
      { path: '/guides', label: '穿搭指南', icon: Promotion },
    ],
  },
  {
    title: '基础数据',
    icon: OfficeBuilding,
    items: [
      { path: '/schools', label: '学校管理', icon: OfficeBuilding },
      { path: '/grades', label: '年级管理', icon: List },
    ],
  },
  {
    title: '业务管理',
    icon: Notebook,
    items: [
      { path: '/orders', label: '订单管理', icon: Notebook },
      { path: '/reviews', label: '评论审核', icon: DataAnalysis },
    ],
  },
  {
    title: '数据分析',
    icon: DataAnalysis,
    items: [
      { path: '/analytics/recommendations', label: '推荐效果分析', icon: TrendCharts },
      { path: '/analytics/feedback-distribution', label: '反馈分布分析', icon: DataBoard },
      { path: '/analytics/low-confidence-hotspots', label: '低匹配把握热点', icon: WarningFilled },
      { path: '/analytics/behavior-funnel', label: '用户行为漏斗', icon: TrendCharts },
      { path: '/analytics/entry-comparison', label: '推荐入口对比', icon: DataAnalysis },
      { path: '/analytics/drop-off', label: '流失分布', icon: DataBoard },
      { path: '/analytics/stalled-carts', label: '滞留购物车', icon: Goods },
      { path: '/analytics/calibration-params', label: '校准参数管理', icon: Operation },
      { path: '/supply/inventory-skus', label: '库存 SKU 管理', icon: Goods },
      { path: '/supply/sales', label: '供需销量分析', icon: TrendCharts },
      { path: '/supply/low-stock', label: '低库存告警', icon: WarningFilled },
      { path: '/supply/forecast', label: '预测与补货', icon: DataBoard },
      { path: '/logs', label: '用户行为日志', icon: DataAnalysis },
      { path: '/oper-logs', label: '操作日志', icon: DataAnalysis },
    ],
  },
]
</script>

<style scoped lang="scss">
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--layout-sidebar);
  min-width: var(--layout-sidebar);
  padding: 16px 0;
  transition: width 0.24s ease, min-width 0.24s ease;
  background: #172033;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  color: #e5eef8;
  overflow: hidden;
}

.sidebar--collapsed {
  width: var(--layout-sidebar-collapsed);
  min-width: var(--layout-sidebar-collapsed);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar__crest {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  color: #fff;
  font-weight: 700;
  background: var(--brand);
}

.sidebar__copy {
  display: grid;
  gap: 2px;
}

.sidebar__eyebrow {
  color: rgba(229, 238, 248, 0.62);
  font-size: 12px;
  letter-spacing: 0;
}

.sidebar__copy strong {
  font-size: 14px;
}

.sidebar__copy span {
  color: rgba(229, 238, 248, 0.7);
  font-size: 12px;
}

.sidebar__scroll {
  flex: 1;
}

:deep(.sidebar__menu) {
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-text-color: rgba(229, 238, 248, 0.74);
  --el-menu-active-color: #fff;

  border-right: 0;
  background: transparent;
  padding-top: 10px;
}

:deep(.sidebar__menu .el-menu) {
  background: transparent;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  margin: 3px 10px;
  border-radius: 8px;
  height: 42px;
  color: rgba(229, 238, 248, 0.74);
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title),
:deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(15, 76, 129, 0.92);
}

:deep(.el-sub-menu .el-menu-item) {
  margin-left: 18px;
  margin-right: 18px;
  background: transparent;
}

:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}
</style>
