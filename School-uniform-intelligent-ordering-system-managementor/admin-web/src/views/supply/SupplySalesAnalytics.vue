<template>
  <section class="supply-page">
    <div class="page-title">
      <div>
        <h1>供需销量分析</h1>
        <p>按学校、年级、校服、品类、尺码和时间窗口聚合有效商业需求，并展示趋势。</p>
      </div>
      <div class="supply-page__actions">
        <el-button :loading="loading" plain type="primary" @click="fetchData">刷新</el-button>
        <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <section class="section-card supply-page__toolbar">
      <el-form :inline="true" :model="filters">
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
          <el-select v-model="filters.schoolId" clearable filterable placeholder="全部学校" style="width: 180px" @change="handleSchoolChange">
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="filters.gradeId" clearable filterable placeholder="全部年级" style="width: 160px" @change="handleGradeChange">
            <el-option v-for="grade in gradeOptions" :key="grade.gradeId" :label="grade.gradeName" :value="grade.gradeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="校服">
          <el-select v-model="filters.uniformId" clearable filterable placeholder="全部校服" style="width: 220px">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="尺码">
          <el-select v-model="filters.sizeId" clearable filterable placeholder="全部尺码" style="width: 140px">
            <el-option v-for="size in sizeOptions" :key="size.id" :label="size.sizeName" :value="size.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品类">
          <el-input v-model="filters.categoryKey" clearable placeholder="categoryKey" style="width: 150px" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="filters.groupBy" style="width: 130px">
            <el-option label="学校" value="school" />
            <el-option label="年级" value="grade" />
            <el-option label="校服" value="uniform" />
            <el-option label="品类" value="category" />
            <el-option label="尺码" value="size" />
          </el-select>
        </el-form-item>
        <el-form-item label="趋势">
          <el-select v-model="filters.bucketType" style="width: 120px">
            <el-option label="日" value="day" />
            <el-option label="周" value="week" />
            <el-option label="月" value="month" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="supply-page__stats">
      <article v-for="card in statCards" :key="card.label" class="section-card supply-page__stat">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.note }}</small>
      </article>
    </div>

    <div class="supply-page__grid">
      <section class="section-card supply-page__panel">
        <div class="supply-page__panel-head">
          <div>
            <h2>{{ groupByText }}销量排行</h2>
            <span>当前筛选范围内各分组销量对比</span>
          </div>
        </div>
        <div ref="groupChartRef" class="supply-page__chart" />
      </section>

      <section class="section-card supply-page__panel">
        <div class="supply-page__panel-head">
          <div>
            <h2>季节趋势</h2>
            <span>按{{ bucketTypeText }}展示销量与销售额</span>
          </div>
        </div>
        <div ref="trendChartRef" class="supply-page__chart" />
      </section>
    </div>

    <section class="section-card supply-page__panel">
      <div class="supply-page__panel-head">
        <div>
          <h2>聚合明细</h2>
          <span>包含分组键、分组名称、销量、订单数和上下文信息</span>
        </div>
      </div>
      <el-table v-loading="loading" :data="salesRows" border stripe>
        <el-table-column prop="groupKey" label="分组键" width="130" />
        <el-table-column prop="groupName" label="分组名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="totalQuantity" label="销量" width="120" />
        <el-table-column label="销售额" width="140">
          <template #default="{ row }">{{ formatCurrency(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="120" />
        <el-table-column prop="uniformName" label="校服" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sizeName" label="尺码" width="100" />
        <el-table-column prop="schoolName" label="学校" width="140" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="120" />
        <el-table-column prop="categoryKey" label="品类" width="120" />
        <template #empty>
          <el-empty description="当前筛选条件下暂无销量数据" />
        </template>
      </el-table>
    </section>

    <section class="section-card supply-page__panel">
      <div class="supply-page__panel-head">
        <div>
          <h2>趋势明细</h2>
          <span>缺失时间桶由后端补零后返回</span>
        </div>
      </div>
      <el-table :data="trendRows" border stripe>
        <el-table-column prop="bucket" label="时间桶" width="140" />
        <el-table-column prop="totalQuantity" label="销量" width="120" />
        <el-table-column label="销售额" width="140">
          <template #default="{ row }">{{ formatCurrency(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="120" />
        <template #empty>
          <el-empty description="当前筛选条件下暂无趋势数据" />
        </template>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getSupplySales, getSupplyTrend } from '@/api/supply'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listSizes } from '@/api/sizes'
import { listUniforms } from '@/api/uniform'
import type { GradeForm, SchoolForm, SizeForm, SupplySalesAnalyticsRecord, SupplyTrendPointRecord, UniformForm } from '@/types/basic-data'
import { downloadCsv } from '@/utils/csv'
import { formatCurrency } from '@/utils/format'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

type SalesFilters = {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  sizeId?: number
  categoryKey?: string
  groupBy: string
  bucketType: string
}

const loading = ref(false)
const exporting = ref(false)
const salesRows = ref<SupplySalesAnalyticsRecord[]>([])
const trendRows = ref<SupplyTrendPointRecord[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const sizeOptions = ref<SizeForm[]>([])
const dateRange = ref<[string, string] | null>(buildDefaultRange())
const filters = reactive<SalesFilters>({ groupBy: 'uniform', bucketType: 'day' })
const groupChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

let groupChart: EChartsType | null = null
let trendChart: EChartsType | null = null

const totalQuantity = computed(() => salesRows.value.reduce((sum, row) => sum + Number(row.totalQuantity ?? 0), 0))
const totalAmount = computed(() => salesRows.value.reduce((sum, row) => sum + Number(row.totalAmount ?? 0), 0))
const totalOrders = computed(() => salesRows.value.reduce((sum, row) => sum + Number(row.orderCount ?? 0), 0))
const groupByText = computed(() => groupLabels[filters.groupBy] || filters.groupBy)
const bucketTypeText = computed(() => bucketLabels[filters.bucketType] || filters.bucketType)
const dateRangeText = computed(() => (dateRange.value ? `${dateRange.value[0]} 至 ${dateRange.value[1]}` : '未选择日期'))
const statCards = computed(() => [
  { label: '总销量', value: totalQuantity.value.toLocaleString(), note: `${groupByText.value}分组汇总数量` },
  { label: '销售额', value: formatCurrency(totalAmount.value), note: dateRangeText.value },
  { label: '订单数', value: totalOrders.value.toLocaleString(), note: '当前聚合结果订单数合计' },
  { label: '分组数', value: salesRows.value.length.toLocaleString(), note: `按${groupByText.value}聚合` },
])

const groupLabels: Record<string, string> = {
  school: '学校',
  grade: '年级',
  uniform: '校服',
  category: '品类',
  size: '尺码',
}

const bucketLabels: Record<string, string> = {
  day: '日',
  week: '周',
  month: '月',
}

onMounted(async () => {
  await Promise.all([loadOptions(), fetchData()])
  await nextTick()
  initCharts()
  renderCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  groupChart?.dispose()
  trendChart?.dispose()
})

async function loadOptions() {
  const [schoolResult, gradeResult, uniformResult, sizeResult] = await Promise.all([
    listSchoolOptions(),
    listGradeOptions(),
    listUniforms({ pageNum: 1, pageSize: 500 }),
    listSizes({ pageNum: 1, pageSize: 500 }),
  ])
  schoolOptions.value = schoolResult.data.data || []
  gradeOptions.value = gradeResult.data.data || []
  uniformOptions.value = uniformResult.data.data.records || []
  sizeOptions.value = sizeResult.data.data.records || []
}

async function handleSchoolChange() {
  filters.gradeId = undefined
  filters.uniformId = undefined
  const [gradeResult, uniformResult] = await Promise.all([
    listGradeOptions({ schoolId: filters.schoolId }),
    listUniforms({ pageNum: 1, pageSize: 500, schoolId: filters.schoolId }),
  ])
  gradeOptions.value = gradeResult.data.data || []
  uniformOptions.value = uniformResult.data.data.records || []
}

async function handleGradeChange() {
  filters.uniformId = undefined
  const { data } = await listUniforms({
    pageNum: 1,
    pageSize: 500,
    schoolId: filters.schoolId,
    gradeId: filters.gradeId,
  })
  uniformOptions.value = data.data.records || []
}

async function fetchData() {
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    const [sales, trend] = await Promise.all([
      getSupplySales(buildQueryParams()),
      getSupplyTrend(buildQueryParams()),
    ])
    salesRows.value = sales.data.data || []
    trendRows.value = trend.data.data || []
    renderCharts()
  } finally {
    loading.value = false
  }
}

async function resetFilters() {
  dateRange.value = buildDefaultRange()
  Object.assign(filters, {
    schoolId: undefined,
    gradeId: undefined,
    uniformId: undefined,
    sizeId: undefined,
    categoryKey: undefined,
    groupBy: 'uniform',
    bucketType: 'day',
  })
  await loadOptions()
  fetchData()
}

function exportCsv() {
  const range = dateRange.value
  if (!range?.[0] || !range?.[1]) {
    ElMessage.warning('请选择日期范围')
    return
  }
  exporting.value = true
  try {
    downloadCsv(
      `supply_sales_${range[0]}_${range[1]}.csv`,
      ['数据类型', '分组键/时间桶', '分组名称', '销量', '销售额', '订单数', '学校', '年级', '校服', '品类', '尺码', '日期范围'],
      [
        ...salesRows.value.map((item) => [
          '聚合明细',
          item.groupKey,
          item.groupName,
          item.totalQuantity,
          Number(item.totalAmount ?? 0).toFixed(2),
          item.orderCount,
          item.schoolName,
          item.gradeName,
          item.uniformName,
          item.categoryKey,
          item.sizeName,
          `${range[0]} 至 ${range[1]}`,
        ]),
        ...trendRows.value.map((item) => [
          '趋势明细',
          item.bucket,
          bucketTypeText.value,
          item.totalQuantity,
          Number(item.totalAmount ?? 0).toFixed(2),
          item.orderCount,
          resolveSchoolName(filters.schoolId),
          resolveGradeName(filters.gradeId),
          resolveUniformName(filters.uniformId),
          filters.categoryKey,
          resolveSizeName(filters.sizeId),
          `${range[0]} 至 ${range[1]}`,
        ]),
      ],
    )
  } finally {
    exporting.value = false
  }
}

function initCharts() {
  if (groupChartRef.value && !groupChart) {
    groupChart = init(groupChartRef.value)
  }
  if (trendChartRef.value && !trendChart) {
    trendChart = init(trendChartRef.value)
  }
}

function renderCharts() {
  renderGroupChart()
  renderTrendChart()
}

function renderGroupChart() {
  if (!groupChart) {
    return
  }
  groupChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 28, right: 24, top: 24, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: salesRows.value.map((item) => item.groupName || item.groupKey || '未知'),
      axisLabel: { interval: 0, rotate: salesRows.value.length > 6 ? 28 : 0 },
    },
    yAxis: { type: 'value', minInterval: 1, name: '销量' },
    series: [
      {
        name: '销量',
        type: 'bar',
        barMaxWidth: 42,
        data: salesRows.value.map((item) => Number(item.totalQuantity ?? 0)),
        itemStyle: { color: '#0f4c81', borderRadius: [8, 8, 0, 0] },
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
    grid: { left: 32, right: 44, top: 42, bottom: 28, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendRows.value.map((item) => item.bucket),
    },
    yAxis: [
      { type: 'value', minInterval: 1, name: '销量' },
      { type: 'value', name: '销售额' },
    ],
    series: [
      {
        name: '销量',
        type: 'line',
        smooth: true,
        data: trendRows.value.map((item) => Number(item.totalQuantity ?? 0)),
        lineStyle: { width: 3, color: '#0f4c81' },
        itemStyle: { color: '#0f4c81' },
      },
      {
        name: '销售额',
        type: 'bar',
        yAxisIndex: 1,
        data: trendRows.value.map((item) => Number(item.totalAmount ?? 0)),
        itemStyle: { color: '#d97706', borderRadius: [6, 6, 0, 0] },
      },
    ],
  })
}

function handleResize() {
  groupChart?.resize()
  trendChart?.resize()
}

function buildQueryParams() {
  const range = dateRange.value || buildDefaultRange()
  return cleanParams({
    ...filters,
    startDate: range[0],
    endDate: range[1],
  })
}

function cleanParams(source: object) {
  const params: Record<string, unknown> = {}
  Object.entries(source).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value
    }
  })
  return params
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

function resolveSchoolName(schoolId?: number) {
  return schoolOptions.value.find((item) => item.schoolId === schoolId)?.schoolName || '全部学校'
}

function resolveGradeName(gradeId?: number) {
  return gradeOptions.value.find((item) => item.gradeId === gradeId)?.gradeName || '全部年级'
}

function resolveUniformName(uniformId?: number) {
  return uniformOptions.value.find((item) => item.id === uniformId)?.name || '全部校服'
}

function resolveSizeName(sizeId?: number) {
  return sizeOptions.value.find((item) => item.id === sizeId)?.sizeName || '全部尺码'
}
</script>

<style scoped lang="scss">
.supply-page {
  display: grid;
  gap: 18px;
}

.supply-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.supply-page__toolbar,
.supply-page__panel {
  padding: 20px;
}

.supply-page__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.supply-page__stat {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.supply-page__stat span,
.supply-page__stat small {
  color: var(--text-secondary);
}

.supply-page__stat strong {
  color: var(--text-primary);
  font-size: 28px;
}

.supply-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.supply-page__panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.supply-page__panel-head h2 {
  margin: 0;
}

.supply-page__panel-head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.supply-page__chart {
  width: 100%;
  height: 320px;
}

@media (max-width: 1200px) {
  .supply-page__stats,
  .supply-page__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .page-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
