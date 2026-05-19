<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>流失分布</h1>
        <p>统计未完成下单会话的最后行为，定位最常见的停留位置。</p>
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
        <el-form-item label="显示条数">
          <el-input-number v-model="filters.limit" :min="5" :max="50" controls-position="right" @change="fetchData" />
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
        <el-empty description="当前筛选条件下暂无流失分布数据" />
      </div>
      <div v-else ref="chartRef" class="chart"></div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="lastActionType" label="末位行为" min-width="220" />
        <el-table-column prop="sessionCount" label="会话数" min-width="120" />
        <el-table-column label="占比" min-width="120">
          <template #default="{ row }">{{ formatPercent(row.share) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBehaviorDropOff } from '@/api/analytics'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import { RECOMMENDATION_SOURCES } from '@/constants/recommendationSources'
import type { DropOffDistributionQuery, DropOffDistributionRow } from '@/types/analytics'
import type { GradeForm, SchoolForm, UniformForm } from '@/types/basic-data'
import { downloadCsv, formatCsvPercent } from '@/utils/csv'

use([PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const loading = ref(false)
const exporting = ref(false)
const rows = ref<DropOffDistributionRow[]>([])
const totalSessions = ref(0)
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
  limit: 20,
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
    const { data } = await getBehaviorDropOff(params)
    rows.value = data.data.rows
    totalSessions.value = data.data.totalSessions
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
  filters.limit = 20
  Promise.all([loadGrades(), loadUniforms()]).then(fetchData)
}

function buildParams(): DropOffDistributionQuery | null {
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
    limit: filters.limit,
  }
}

function renderChart() {
  if (!chartRef.value || !rows.value.length) return
  chart.value ??= init(chartRef.value)
  chart.value.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        name: '流失分布',
        type: 'pie',
        radius: ['38%', '68%'],
        center: ['50%', '46%'],
        data: rows.value.map((row) => ({ name: row.lastActionType, value: row.sessionCount })),
      },
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
    downloadCsv(`drop_off_${startDate}_${endDate}.csv`, [
      '末位行为',
      '会话数',
      '占比',
      '总流失会话数',
    ], rows.value.map((row) => [
      row.lastActionType,
      row.sessionCount,
      formatCsvPercent(row.share),
      totalSessions.value,
    ]))
  } finally {
    exporting.value = false
  }
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
