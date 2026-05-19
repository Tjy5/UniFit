<template>
  <section class="dashboard">
    <div class="page-title">
      <div>
        <h1>运营工作台</h1>
        <p>从校服、订单、评论和访问行为四个维度快速掌握当前运营状态。</p>
      </div>
      <el-button :loading="loading" type="primary" plain @click="fetchStats">刷新数据</el-button>
    </div>

    <div class="dashboard__stats">
      <article v-for="card in statCards" :key="card.label" class="section-card dashboard__stat" :class="`dashboard__stat--${card.tone}`">
        <span class="dashboard__stat-label">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.note }}</small>
      </article>
    </div>

    <div class="dashboard__grid">
      <section class="section-card dashboard__panel">
        <div class="dashboard__panel-head">
          <div>
            <h2>最近 7 天订单趋势</h2>
            <span>按下单日期统计订单数量</span>
          </div>
        </div>
        <div ref="chartRef" class="dashboard__chart" />
      </section>

      <section class="section-card dashboard__panel">
        <div class="dashboard__panel-head">
          <div>
            <h2>最近订单</h2>
            <span>最新 5 条订单速览</span>
          </div>
        </div>
        <el-table :data="stats.recentOrders" size="small">
          <el-table-column prop="id" label="订单ID" min-width="120" />
          <el-table-column prop="userAccount" label="用户" min-width="120" />
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusTag(row.status)">{{ getOrderStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="120">
            <template #default="{ row }">
              {{ formatCurrency(row.totalPrice) }}
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.orderDate) }}
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import type { DashboardStats } from '@/types/basic-data'
import { formatCurrency, formatDateTime } from '@/utils/format'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const orderStatusOptions = [
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 },
]

const loading = ref(false)
const chartRef = ref<HTMLDivElement>()

const stats = ref<DashboardStats>({
  uniformTotal: 0,
  todayOrders: 0,
  pendingReviews: 0,
  todayVisits: 0,
  orderTrend: [],
  recentOrders: [],
})

let chartInstance: EChartsType | null = null

const statCards = computed(() => [
  { label: '校服总数', value: stats.value.uniformTotal, note: '当前系统内可维护的校服条目数', tone: 'primary' },
  { label: '今日订单', value: stats.value.todayOrders, note: '今日新增订单笔数', tone: 'success' },
  { label: '待审评论', value: stats.value.pendingReviews, note: '仍需管理员处理的评论数', tone: 'warning' },
  { label: '今日访问', value: stats.value.todayVisits, note: '今日用户行为日志数量', tone: 'info' },
])

onMounted(async () => {
  await nextTick()
  initChart()
  await fetchStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

async function fetchStats() {
  loading.value = true
  try {
    const { data } = await getDashboardStats()
    stats.value = data.data
    renderChart()
  } finally {
    loading.value = false
  }
}

function initChart() {
  if (!chartRef.value) {
    return
  }
  chartInstance = init(chartRef.value)
  renderChart()
}

function renderChart() {
  if (!chartInstance) {
    return
  }
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: 24,
      right: 24,
      top: 24,
      bottom: 24,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: stats.value.orderTrend.map((item) => item.day.slice(5)),
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        data: stats.value.orderTrend.map((item) => item.orderCount),
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#0f4c81',
        },
        itemStyle: {
          color: '#0f4c81',
        },
        areaStyle: {
          color: 'rgba(15, 76, 129, 0.12)',
        },
      },
    ],
  })
}

function handleResize() {
  chartInstance?.resize()
}

function getOrderStatusLabel(value?: number) {
  return orderStatusOptions.find((item) => item.value === value)?.label || '未知'
}

function getOrderStatusTag(value?: number) {
  if (value === 2) {
    return 'success'
  }
  if (value === 3) {
    return 'info'
  }
  if (value === 1) {
    return 'warning'
  }
  return 'danger'
}
</script>

<style scoped lang="scss">
.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard__stat {
  display: grid;
  gap: 8px;
  padding: 20px;
  position: relative;
  border-radius: var(--radius-lg);
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
}

.dashboard__stat::before {
  content: '';
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  opacity: 0.16;
}

.dashboard__stat-label,
.dashboard__stat small {
  color: var(--text-secondary);
}

.dashboard__stat strong {
  font-size: 32px;
  color: var(--text-primary);
  line-height: 1.1;
}

.dashboard__stat--primary::before {
  background: var(--brand);
}

.dashboard__stat--success::before {
  background: var(--success);
}

.dashboard__stat--warning::before {
  background: var(--warning);
}

.dashboard__stat--info::before {
  background: var(--info);
}

.dashboard__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.dashboard__panel {
  padding: 20px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
}

.dashboard__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.dashboard__panel-head h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.dashboard__panel-head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.dashboard__chart {
  width: 100%;
  height: 320px;
}

@media (max-width: 1100px) {
  .dashboard__stats,
  .dashboard__grid {
    grid-template-columns: 1fr;
  }
}
</style>
