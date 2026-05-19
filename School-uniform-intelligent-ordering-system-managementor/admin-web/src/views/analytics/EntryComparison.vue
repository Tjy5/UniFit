<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>推荐入口对比</h1>
        <p>比较不同推荐入口的曝光、加购、下单与采纳表现，未知入口单独展示。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="日期范围">
          <el-date-picker v-model="filters.dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" value-format="YYYY-MM-DD" @change="fetchData" />
        </el-form-item>
        <el-form-item label="学校">
          <el-select v-model="filters.schoolId" clearable filterable placeholder="全部学校" style="width: 200px" @change="handleSchoolChange">
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="filters.gradeId" clearable filterable placeholder="全部年级" style="width: 180px" @change="handleGradeChange">
            <el-option v-for="grade in gradeOptions" :key="grade.gradeId" :label="grade.gradeName" :value="grade.gradeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="filters.uniformId" clearable filterable placeholder="全部商品" style="width: 220px" @change="fetchData">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐入口">
          <el-select v-model="filters.source" clearable placeholder="全部入口" style="width: 160px" @change="fetchData">
            <el-option v-for="source in RECOMMENDATION_SOURCES" :key="source.value" :label="source.label" :value="source.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card chart-card" v-loading="loading">
      <div v-if="!rows.length" class="empty-state">
        <el-empty description="当前筛选条件下暂无入口对比数据" />
      </div>
      <div v-else ref="chartRef" class="chart"></div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" :default-sort="{ prop: 'orderConversionRate', order: 'descending' }">
        <el-table-column label="入口" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.requestSource === 'unknown'" type="warning" effect="plain">{{ sourceLabel(row.requestSource) }}</el-tag>
            <span v-else>{{ sourceLabel(row.requestSource) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="exposureCount" label="曝光" min-width="100" />
        <el-table-column prop="linkedCartCount" label="加购" min-width="100" />
        <el-table-column prop="linkedOrderCount" label="下单" min-width="100" />
        <el-table-column prop="adoptedCount" label="采纳" min-width="100" />
        <el-table-column prop="cartConversionRate" label="加购转化" min-width="120" sortable>
          <template #default="{ row }">{{ formatPercent(row.cartConversionRate) }}</template>
        </el-table-column>
        <el-table-column prop="orderConversionRate" label="下单转化" min-width="120" sortable>
          <template #default="{ row }">{{ formatPercent(row.orderConversionRate) }}</template>
        </el-table-column>
        <el-table-column prop="adoptionRate" label="采纳率" min-width="120" sortable>
          <template #default="{ row }">{{ formatPercent(row.adoptionRate) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecommendationEntryComparison } from '@/api/analytics'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import { RECOMMENDATION_SOURCES, recommendationSourceLabel } from '@/constants/recommendationSources'
import type { BehaviorAnalyticsQuery, RecommendationEntryComparisonRow } from '@/types/analytics'
import type { GradeForm, SchoolForm, UniformForm } from '@/types/basic-data'
import { downloadCsv, formatCsvPercent } from '@/utils/csv'

use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const loading = ref(false)
const exporting = ref(false)
const rows = ref<RecommendationEntryComparisonRow[]>([])
const chartRef = ref<HTMLDivElement>()
const chart = ref<EChartsType>()
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])

const filters = reactive({
  dateRange: defaultDateRange() as [string, string],
  schoolId: undefined as number | undefined,
  gradeId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
  source: undefined as string | undefined,
})

onMounted(async () => {
  await Promise.all([loadSchools(), loadGrades(), loadUniforms()])
  await fetchData()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart.value?.dispose()
})

async function loadSchools() {
  const { data } = await listSchoolOptions()
  schoolOptions.value = data.data
}

async function loadGrades(schoolId?: number) {
  const { data } = await listGradeOptions({ schoolId })
  gradeOptions.value = data.data
}

async function loadUniforms() {
  const { data } = await listUniforms({ pageNum: 1, pageSize: 200, status: 0, schoolId: filters.schoolId, gradeId: filters.gradeId })
  uniformOptions.value = data.data.records
}

async function handleSchoolChange(value?: number) {
  filters.gradeId = undefined
  filters.uniformId = undefined
  await Promise.all([loadGrades(value), loadUniforms()])
  await fetchData()
}

async function handleGradeChange() {
  filters.uniformId = undefined
  await loadUniforms()
  await fetchData()
}

async function fetchData() {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const { data } = await getRecommendationEntryComparison(params)
    rows.value = [...data.data.rows].sort((a, b) => b.orderConversionRate - a.orderConversionRate)
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.dateRange = defaultDateRange() as [string, string]
  filters.schoolId = undefined
  filters.gradeId = undefined
  filters.uniformId = undefined
  filters.source = undefined
  Promise.all([loadGrades(), loadUniforms()]).then(fetchData)
}

function buildParams(): BehaviorAnalyticsQuery | null {
  const [startDate, endDate] = filters.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return null
  }
  return {
    schoolId: filters.schoolId,
    gradeId: filters.gradeId,
    uniformId: filters.uniformId,
    source: filters.source,
    startDate,
    endDate,
  }
}

function renderChart() {
  if (!chartRef.value || !rows.value.length) return
  chart.value ??= init(chartRef.value)
  const labels = rows.value.map((row) => sourceLabel(row.requestSource))
  chart.value.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 44, right: 24, top: 30, bottom: 72 },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [
      { name: '曝光', type: 'bar', data: rows.value.map((row) => row.exposureCount) },
      { name: '加购', type: 'bar', data: rows.value.map((row) => row.linkedCartCount) },
      { name: '下单', type: 'bar', data: rows.value.map((row) => row.linkedOrderCount) },
      { name: '采纳', type: 'bar', data: rows.value.map((row) => row.adoptedCount) },
    ],
  })
}

function resizeChart() {
  chart.value?.resize()
}

function exportCsv() {
  const [startDate, endDate] = filters.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }
  exporting.value = true
  try {
    downloadCsv(`entry_comparison_${startDate}_${endDate}.csv`, [
      '推荐入口',
      '曝光',
      '加购',
      '下单',
      '采纳',
      '加购转化率',
      '下单转化率',
      '采纳率',
    ], rows.value.map((row) => [
      sourceLabel(row.requestSource),
      row.exposureCount,
      row.linkedCartCount,
      row.linkedOrderCount,
      row.adoptedCount,
      formatCsvPercent(row.cartConversionRate),
      formatCsvPercent(row.orderConversionRate),
      formatCsvPercent(row.adoptionRate),
    ]))
  } finally {
    exporting.value = false
  }
}

function sourceLabel(value?: string) {
  return recommendationSourceLabel(value)
}

function defaultDateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return [formatDate(start), formatDate(end)]
}

function formatDate(date: Date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}

function formatPercent(value?: number) {
  return formatCsvPercent(value)
}
</script>

<style scoped lang="scss">
.basic-page {
  display: grid;
  gap: 18px;
}

.basic-page__toolbar,
.basic-page__table,
.chart-card {
  padding: 20px;
}

.chart {
  height: 340px;
}

.empty-state {
  display: grid;
  min-height: 280px;
  place-items: center;
}
</style>
