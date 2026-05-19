<template>
  <section class="analytics-page">
    <div class="page-title">
      <div>
        <h1>推荐效果分析</h1>
        <p>按时间、学校和商品跟踪推荐量、关联订单覆盖、反馈覆盖与匹配把握表现。</p>
      </div>
      <div class="analytics-page__head-actions">
        <el-button :loading="loading" plain type="primary" @click="fetchStats">刷新数据</el-button>
        <el-button type="primary" @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <section class="section-card analytics-page__toolbar">
      <el-form :inline="true">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
          />
        </el-form-item>
        <el-form-item label="学校">
          <el-select v-model="filters.schoolId" clearable filterable placeholder="全部学校" style="width: 180px">
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="filters.uniformId" clearable filterable placeholder="全部商品" style="width: 220px">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchStats">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="analytics-page__stats">
      <article v-for="card in statCards" :key="card.label" class="section-card analytics-page__stat" :class="`analytics-page__stat--${card.tone}`">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.note }}</small>
      </article>
    </div>

    <div class="analytics-page__grid">
      <section class="section-card analytics-page__panel">
        <div class="analytics-page__panel-head">
          <div>
            <h2>反馈结果分布</h2>
            <span>合身、偏大、偏小的当前筛选分布</span>
          </div>
        </div>
        <div ref="satisfactionChartRef" class="analytics-page__chart" />
      </section>

      <section class="section-card analytics-page__panel">
        <div class="analytics-page__panel-head">
          <div>
            <h2>覆盖率与风险</h2>
            <span>对比订单关联覆盖、反馈覆盖、合身率和低匹配把握率</span>
          </div>
        </div>
        <div ref="confidenceChartRef" class="analytics-page__chart" />
      </section>
    </div>

    <section class="section-card analytics-page__panel analytics-page__panel--full">
      <div class="analytics-page__panel-head">
        <div>
          <h2>趋势图</h2>
          <span>推荐量、关联订单数、反馈数和合身率的日趋势</span>
        </div>
      </div>
      <div ref="trendChartRef" class="analytics-page__chart analytics-page__chart--large" />
    </section>

    <section class="section-card analytics-page__panel analytics-page__panel--full">
      <div class="analytics-page__panel-head">
        <div>
          <h2>A/B 实验对比</h2>
          <span>按当前筛选条件比较基础规则与校准策略</span>
        </div>
      </div>
      <el-table :data="experimentRows" border stripe>
        <el-table-column prop="label" label="实验组" min-width="110" />
        <el-table-column prop="totalRecommendations" label="推荐数" min-width="100" />
        <el-table-column prop="linkedOrderCount" label="关联订单" min-width="100" />
        <el-table-column prop="totalFeedbacks" label="反馈数" min-width="90" />
        <el-table-column label="合身率" min-width="95">
          <template #default="{ row }">{{ formatPercent(row.fitRate) }}</template>
        </el-table-column>
        <el-table-column label="低匹配把握率" min-width="105">
          <template #default="{ row }">{{ formatPercent(row.lowConfidenceRate) }}</template>
        </el-table-column>
        <el-table-column label="反馈提交率" min-width="110">
          <template #default="{ row }">{{ formatPercent(row.feedbackSubmissionRate) }}</template>
        </el-table-column>
        <el-table-column label="推荐下单率" min-width="110">
          <template #default="{ row }">{{ formatPercent(row.recommendationToOrderConversionRate) }}</template>
        </el-table-column>
        <el-table-column label="偏大偏小率" min-width="110">
          <template #default="{ row }">{{ formatPercent(row.sizeIssueRate) }}</template>
        </el-table-column>
        <el-table-column prop="avgConfidenceScore" label="平均匹配把握" min-width="110" />
      </el-table>
      <div class="analytics-page__diagnostics">
        <article v-for="item in experimentDiagnostics" :key="item.label" class="analytics-page__diagnostic">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getRecommendationExperimentMetrics, getRecommendationStats } from '@/api/analytics'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import type { RecommendationExperimentVariantMetrics, RecommendationStats } from '@/types/analytics'
import type { SchoolForm, UniformForm } from '@/types/basic-data'
import { downloadCsv } from '@/utils/csv'

use([PieChart, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const loading = ref(false)
const schoolOptions = ref<SchoolForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const satisfactionChartRef = ref<HTMLDivElement>()
const confidenceChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

const dateRange = ref<[string, string]>(buildDefaultRange())
const filters = reactive({
  schoolId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
})

const stats = ref<RecommendationStats>(createEmptyStats())
const experimentVariants = ref<RecommendationExperimentVariantMetrics[]>(createEmptyExperimentVariants())

let satisfactionChart: EChartsType | null = null
let confidenceChart: EChartsType | null = null
let trendChart: EChartsType | null = null

const statCards = computed(() => [
  {
    label: '总推荐量',
    value: stats.value.totalRecommendations.toLocaleString(),
    note: '当前筛选范围内记录到的推荐请求数',
    tone: 'orange',
  },
  {
    label: '关联订单覆盖率',
    value: formatPercent(stats.value.linkedOrderCoverage),
    note: `${stats.value.linkedOrderCount} 笔已形成订单关联`,
    tone: 'olive',
  },
  {
    label: '反馈覆盖率',
    value: formatPercent(stats.value.feedbackCoverage),
    note: `${stats.value.totalFeedbacks} 条反馈已回流`,
    tone: 'blue',
  },
  {
    label: '平均匹配把握',
    value: stats.value.avgConfidenceScore.toFixed(1),
    note: `${stats.value.lowConfidenceCount} 条低于 60 分`,
    tone: 'walnut',
  },
])

const experimentRows = computed(() =>
  experimentVariants.value.map((item) => ({
    ...item,
    label: item.experimentVariant === 'A' ? 'A 组 基础规则' : 'B 组 校准策略',
  })),
)

const bVariant = computed(() => experimentVariants.value.find((item) => item.experimentVariant === 'B') || createEmptyExperimentVariant('B'))

const experimentDiagnostics = computed(() => [
  { label: 'B 组校准命中率', value: formatPercent(bVariant.value.calibrationHitRate) },
  { label: 'B 组校准应用率', value: formatPercent(bVariant.value.calibrationApplicationRate) },
  { label: 'B 组改荐率', value: formatPercent(bVariant.value.recommendationChangeRate) },
  { label: '安全闸门跳过', value: bVariant.value.safetyGateSkipCount.toLocaleString() },
])

onMounted(async () => {
  await Promise.all([loadOptions(), fetchStats()])
  await nextTick()
  initCharts()
  renderCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  satisfactionChart?.dispose()
  confidenceChart?.dispose()
  trendChart?.dispose()
})

async function loadOptions() {
  const [schoolResult, uniformResult] = await Promise.all([
    listSchoolOptions(),
    listUniforms({ pageNum: 1, pageSize: 500 }),
  ])
  schoolOptions.value = schoolResult.data.data || []
  uniformOptions.value = uniformResult.data.data.records || []
}

async function fetchStats() {
  loading.value = true
  try {
    const query = {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      schoolId: filters.schoolId,
      uniformId: filters.uniformId,
    }
    const [statsResult, experimentResult] = await Promise.all([
      getRecommendationStats(query),
      getRecommendationExperimentMetrics(query),
    ])
    stats.value = normalizeStats(statsResult.data.data)
    experimentVariants.value = normalizeExperimentVariants(experimentResult.data.data?.variants)
    renderCharts()
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  dateRange.value = buildDefaultRange()
  filters.schoolId = undefined
  filters.uniformId = undefined
  fetchStats()
}

function exportCsv() {
  downloadCsv(
    `recommendation_analytics_${dateRange.value[0]}_${dateRange.value[1]}.csv`,
    ['类型', '日期', '学校', '商品', '实验组', '推荐次数', '关联订单数', '反馈数', '合身率', '低匹配把握率', '反馈提交率', '推荐下单率', '偏大偏小率', '校准命中率', '校准应用率', '改荐率', '安全闸门跳过', '平均匹配把握'],
    [
      ...stats.value.trend.map((item) => [
        '日趋势',
        item.day,
        resolveSchoolName(filters.schoolId),
        resolveUniformName(filters.uniformId),
        '',
        item.recommendationCount,
        item.linkedOrderCount,
        item.feedbackCount,
        formatPercent(item.fitRate),
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        stats.value.avgConfidenceScore.toFixed(1),
      ]),
      ...experimentRows.value.map((item) => [
        '实验对比',
        `${dateRange.value[0]} 至 ${dateRange.value[1]}`,
        resolveSchoolName(filters.schoolId),
        resolveUniformName(filters.uniformId),
        item.label,
        item.totalRecommendations,
        item.linkedOrderCount,
        item.totalFeedbacks,
        formatPercent(item.fitRate),
        formatPercent(item.lowConfidenceRate),
        formatPercent(item.feedbackSubmissionRate),
        formatPercent(item.recommendationToOrderConversionRate),
        formatPercent(item.sizeIssueRate),
        formatPercent(item.calibrationHitRate),
        formatPercent(item.calibrationApplicationRate),
        formatPercent(item.recommendationChangeRate),
        item.safetyGateSkipCount,
        item.avgConfidenceScore,
      ]),
    ],
  )
}

function normalizeExperimentVariants(value?: RecommendationExperimentVariantMetrics[]) {
  const byVariant = new Map((value || []).map((item) => [item.experimentVariant, item]))
  return [
    normalizeExperimentVariant(byVariant.get('A'), 'A'),
    normalizeExperimentVariant(byVariant.get('B'), 'B'),
  ]
}

function normalizeExperimentVariant(value: Partial<RecommendationExperimentVariantMetrics> | undefined, variant: 'A' | 'B'): RecommendationExperimentVariantMetrics {
  return {
    ...createEmptyExperimentVariant(variant),
    ...value,
    experimentVariant: variant,
  }
}

function createEmptyExperimentVariants(): RecommendationExperimentVariantMetrics[] {
  return [createEmptyExperimentVariant('A'), createEmptyExperimentVariant('B')]
}

function createEmptyExperimentVariant(variant: 'A' | 'B'): RecommendationExperimentVariantMetrics {
  return {
    experimentVariant: variant,
    totalRecommendations: 0,
    linkedOrderCount: 0,
    totalFeedbacks: 0,
    fitCount: 0,
    tooLargeCount: 0,
    tooSmallCount: 0,
    lowConfidenceCount: 0,
    avgConfidenceScore: 0,
    calibrationHitCount: 0,
    calibrationAppliedCount: 0,
    recommendationChangedCount: 0,
    safetyGateSkipCount: 0,
    fitRate: 0,
    lowConfidenceRate: 0,
    feedbackSubmissionRate: 0,
    recommendationToOrderConversionRate: 0,
    sizeIssueRate: 0,
    calibrationHitRate: 0,
    calibrationApplicationRate: 0,
    recommendationChangeRate: 0,
  }
}

function initCharts() {
  if (satisfactionChartRef.value && !satisfactionChart) {
    satisfactionChart = init(satisfactionChartRef.value)
  }
  if (confidenceChartRef.value && !confidenceChart) {
    confidenceChart = init(confidenceChartRef.value)
  }
  if (trendChartRef.value && !trendChart) {
    trendChart = init(trendChartRef.value)
  }
}

function renderCharts() {
  renderSatisfactionChart()
  renderConfidenceChart()
  renderTrendChart()
}

function renderSatisfactionChart() {
  if (!satisfactionChart) {
    return
  }
  satisfactionChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['46%', '72%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        data: [
          { value: stats.value.satisfactionDistribution.FIT ?? 0, name: '合身', itemStyle: { color: '#15803d' } },
          { value: stats.value.satisfactionDistribution.TOO_LARGE ?? 0, name: '偏大', itemStyle: { color: '#b45309' } },
          { value: stats.value.satisfactionDistribution.TOO_SMALL ?? 0, name: '偏小', itemStyle: { color: '#dc2626' } },
        ],
      },
    ],
  })
}

function renderConfidenceChart() {
  if (!confidenceChart) {
    return
  }
  confidenceChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 24, top: 20, bottom: 28, containLabel: true },
    xAxis: {
      type: 'category',
      data: ['订单关联覆盖率', '反馈覆盖率', '合身率', '低匹配把握率'],
      axisLabel: { interval: 0 },
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: (value: number) => `${value}%` },
    },
    series: [
      {
        type: 'bar',
        barWidth: 38,
        data: [
          roundPercent(stats.value.linkedOrderCoverage),
          roundPercent(stats.value.feedbackCoverage),
          roundPercent(stats.value.fitRate),
          roundPercent(stats.value.lowConfidenceRate),
        ],
        itemStyle: {
          color: '#0f4c81',
          borderRadius: [8, 8, 0, 0],
        },
      },
    ],
  })
}

function renderTrendChart() {
  if (!trendChart) {
    return
  }
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 32, right: 32, top: 42, bottom: 28, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: stats.value.trend.map((item) => item.day.slice(5)),
    },
    yAxis: [
      { type: 'value', minInterval: 1, name: '数量' },
      { type: 'value', min: 0, max: 1, axisLabel: { formatter: (value: number) => `${Math.round(value * 100)}%` }, name: '合身率' },
    ],
    series: [
      {
        name: '推荐量',
        type: 'line',
        smooth: true,
        data: stats.value.trend.map((item) => item.recommendationCount),
        lineStyle: { width: 3, color: '#0f4c81' },
        itemStyle: { color: '#0f4c81' },
      },
      {
        name: '关联订单',
        type: 'line',
        smooth: true,
        data: stats.value.trend.map((item) => item.linkedOrderCount),
        lineStyle: { width: 3, color: '#15803d' },
        itemStyle: { color: '#15803d' },
      },
      {
        name: '反馈量',
        type: 'line',
        smooth: true,
        data: stats.value.trend.map((item) => item.feedbackCount),
        lineStyle: { width: 3, color: '#64748b' },
        itemStyle: { color: '#64748b' },
      },
      {
        name: '合身率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: stats.value.trend.map((item) => item.fitRate),
        lineStyle: { width: 2, color: '#0f766e', type: 'dashed' },
        itemStyle: { color: '#0f766e' },
      },
    ],
  })
}

function handleResize() {
  satisfactionChart?.resize()
  confidenceChart?.resize()
  trendChart?.resize()
}

function normalizeStats(value?: Partial<RecommendationStats>): RecommendationStats {
  return {
    ...createEmptyStats(),
    ...value,
    satisfactionDistribution: value?.satisfactionDistribution || { FIT: 0, TOO_LARGE: 0, TOO_SMALL: 0 },
    trend: Array.isArray(value?.trend) ? value.trend : [],
  }
}

function createEmptyStats(): RecommendationStats {
  return {
    totalRecommendations: 0,
    linkedOrderCount: 0,
    linkedOrderCoverage: 0,
    totalFeedbacks: 0,
    feedbackCoverage: 0,
    satisfactionDistribution: { FIT: 0, TOO_LARGE: 0, TOO_SMALL: 0 },
    fitRate: 0,
    lowConfidenceCount: 0,
    lowConfidenceRate: 0,
    avgConfidenceScore: 0,
    confidenceBands: {},
    trend: [],
  }
}

function buildDefaultRange(): [string, string] {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return [formatDate(start), formatDate(end)]
}

function formatDate(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatPercent(value: number) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function roundPercent(value?: number) {
  return Number((Number(value || 0) * 100).toFixed(1))
}

function resolveSchoolName(schoolId?: number) {
  return schoolOptions.value.find((item) => item.schoolId === schoolId)?.schoolName || '全部学校'
}

function resolveUniformName(uniformId?: number) {
  return uniformOptions.value.find((item) => item.id === uniformId)?.name || '全部商品'
}
</script>

<style scoped lang="scss">
.analytics-page {
  display: grid;
  gap: 18px;
}

.analytics-page__head-actions {
  display: flex;
  gap: 12px;
}

.analytics-page__toolbar,
.analytics-page__panel {
  padding: 20px;
}

.analytics-page__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.analytics-page__stat {
  display: grid;
  gap: 10px;
  position: relative;
  overflow: hidden;
}

.analytics-page__stat span,
.analytics-page__stat small {
  color: var(--text-secondary);
}

.analytics-page__stat strong {
  font-size: 32px;
  color: var(--text-primary);
}

.analytics-page__stat::after {
  content: '';
  position: absolute;
  inset: auto -20px -20px auto;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-lg);
  opacity: 0.12;
}

.analytics-page__stat--orange::after {
  background: var(--brand);
}

.analytics-page__stat--olive::after {
  background: var(--success);
}

.analytics-page__stat--blue::after {
  background: var(--info);
}

.analytics-page__stat--walnut::after {
  background: var(--text-secondary);
}

.analytics-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.analytics-page__panel {
  background: var(--surface);
}

.analytics-page__panel--full {
  grid-column: 1 / -1;
}

.analytics-page__panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.analytics-page__panel-head h2 {
  margin: 0;
}

.analytics-page__panel-head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.analytics-page__chart {
  width: 100%;
  height: 320px;
}

.analytics-page__chart--large {
  height: 360px;
}

.analytics-page__diagnostics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.analytics-page__diagnostic {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.analytics-page__diagnostic span {
  color: var(--text-secondary);
  font-size: 13px;
}

.analytics-page__diagnostic strong {
  color: var(--text-primary);
  font-size: 22px;
}

@media (max-width: 1200px) {
  .analytics-page__stats,
  .analytics-page__grid,
  .analytics-page__diagnostics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .analytics-page__head-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
