<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>用户行为漏斗</h1>
        <p>按会话统计浏览、详情、推荐、加购与下单阶段，观察关键路径的阶段转化。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
            value-format="YYYY-MM-DD"
            @change="fetchData"
          />
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

    <section class="funnel-kpis">
      <div v-for="stage in funnel.stages" :key="stage.stage" class="kpi-card">
        <span>{{ stage.label }}</span>
        <strong>{{ stage.sessionsReached }}</strong>
        <small>到下一步 {{ formatPercent(stage.conversionRate) }}</small>
      </div>
      <div class="kpi-card">
        <span>推荐采纳率</span>
        <strong>{{ formatPercent(adoption.adoptionRate) }}</strong>
        <small>修改率 {{ formatPercent(adoption.sizeChangeRate) }}</small>
      </div>
    </section>

    <section class="section-card chart-card" v-loading="loading">
      <div v-if="isEmpty" class="empty-state">
        <el-empty description="当前筛选条件下暂无行为漏斗数据" />
      </div>
      <div v-else ref="chartRef" class="chart"></div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="funnel.stages">
        <el-table-column prop="label" label="阶段" min-width="120" />
        <el-table-column prop="sessionsReached" label="达到会话数" min-width="140" />
        <el-table-column label="到下一阶段转化率" min-width="160">
          <template #default="{ row }">
            {{ formatPercent(row.conversionRate) }}
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { FunnelChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBehaviorFunnel, getRecommendationAdoption } from '@/api/analytics'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import { RECOMMENDATION_SOURCES, recommendationSourceLabel } from '@/constants/recommendationSources'
import type { BehaviorAnalyticsQuery, BehaviorFunnelResponse, RecommendationAdoptionStats } from '@/types/analytics'
import type { GradeForm, SchoolForm, UniformForm } from '@/types/basic-data'
import { downloadCsv, formatCsvPercent } from '@/utils/csv'

use([FunnelChart, TooltipComponent, LegendComponent, CanvasRenderer])

const loading = ref(false)
const exporting = ref(false)
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

const funnel = reactive<BehaviorFunnelResponse>({
  stages: [],
  totalSessions: 0,
  sessionInactivityGapMinutes: 30,
})

const adoption = reactive<RecommendationAdoptionStats>({
  linkedOrderCount: 0,
  adoptedCount: 0,
  sizeChangeCount: 0,
  withFeedbackCount: 0,
  withoutFeedbackCount: 0,
  adoptionRate: 0,
  sizeChangeRate: 0,
})

const isEmpty = computed(() => !loading.value && funnel.stages.every((stage) => !stage.sessionsReached))

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
  const { data } = await listUniforms({
    pageNum: 1,
    pageSize: 200,
    status: 0,
    schoolId: filters.schoolId,
    gradeId: filters.gradeId,
  })
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
    const [funnelResult, adoptionResult] = await Promise.all([
      getBehaviorFunnel(params),
      getRecommendationAdoption(params),
    ])
    Object.assign(funnel, funnelResult.data.data)
    Object.assign(adoption, adoptionResult.data.data)
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
  if (!chartRef.value || isEmpty.value) return
  chart.value ??= init(chartRef.value)
  chart.value.setOption({
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value }: { name: string; value: number }) => `${name}: ${value} 会话`,
    },
    legend: { bottom: 0 },
    series: [
      {
        name: '行为漏斗',
        type: 'funnel',
        top: 20,
        bottom: 40,
        left: '8%',
        right: '8%',
        minSize: '20%',
        maxSize: '92%',
        sort: 'none',
        label: { formatter: '{b}: {c}' },
        data: funnel.stages.map((stage) => ({
          name: stage.label,
          value: stage.sessionsReached,
        })),
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
    downloadCsv(`behavior_funnel_${startDate}_${endDate}.csv`, [
      '阶段',
      '达到会话数',
      '到下一阶段转化率',
      '推荐入口',
      '会话间隔分钟',
      '推荐采纳率',
      '尺码修改率',
    ], funnel.stages.map((stage) => [
      stage.label,
      stage.sessionsReached,
      formatCsvPercent(stage.conversionRate),
      filters.source ? recommendationSourceLabel(filters.source) : '全部入口',
      funnel.sessionInactivityGapMinutes,
      formatCsvPercent(adoption.adoptionRate),
      formatCsvPercent(adoption.sizeChangeRate),
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
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

function formatPercent(value?: number, digits = 1) {
  return formatCsvPercent(value, digits)
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

.funnel-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.kpi-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.kpi-card span,
.kpi-card small {
  color: var(--text-secondary);
}

.kpi-card strong {
  color: var(--text-primary);
  font-size: 26px;
}

.chart {
  height: 360px;
}

.empty-state {
  display: grid;
  min-height: 320px;
  place-items: center;
}
</style>
