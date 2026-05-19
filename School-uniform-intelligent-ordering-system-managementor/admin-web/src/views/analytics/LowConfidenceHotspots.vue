<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>低匹配把握热点</h1>
        <p>筛出低匹配把握推荐占比最高的商品与尺码组合，优先定位最需要校准的热点。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="学校">
          <el-select
            v-model="queryForm.schoolId"
            clearable
            filterable
            placeholder="全部学校"
            style="width: 220px"
            @change="handleSchoolChange"
          >
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="queryForm.uniformId" clearable filterable placeholder="全部商品" style="width: 240px">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="低匹配把握阈值">
          <div class="hotspots__slider">
            <el-slider v-model="queryForm.threshold" :min="0" :max="100" :step="1" show-input />
            <el-tooltip content="低于该分数的推荐会被计入低匹配把握热点" placement="top">
              <el-icon class="hotspots__info"><WarningFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="uniformName" label="商品" min-width="220" />
        <el-table-column prop="sizeName" label="尺码" min-width="120" />
        <el-table-column prop="lowConfidenceCount" label="低匹配把握次数" min-width="120" />
        <el-table-column prop="totalRecommendations" label="总推荐数" min-width="120" />
        <el-table-column label="低匹配把握率" min-width="120">
          <template #default="{ row }">
            <el-tag :type="hotspotTag(row.lowConfidenceRate)" effect="plain">{{ formatPercent(row.lowConfidenceRate) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avgConfidence" label="平均匹配把握" min-width="120" />
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { getLowConfidenceHotspots } from '@/api/analytics'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import type { SchoolForm, UniformForm } from '@/types/basic-data'
import type { LowConfidenceHotspot } from '@/types/analytics'
import { downloadCsv } from '@/utils/csv'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<LowConfidenceHotspot[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const uniformOptions = ref<UniformForm[]>([])

const queryForm = reactive({
  dateRange: defaultDateRange() as [string, string],
  schoolId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
  threshold: 60,
  limit: 20,
})

let thresholdTimer: number | undefined

onMounted(async () => {
  await Promise.all([loadSchools(), loadUniforms()])
  await fetchList()
})

onBeforeUnmount(() => {
  if (thresholdTimer) {
    window.clearTimeout(thresholdTimer)
  }
})

watch(
  () => queryForm.threshold,
  () => {
    if (thresholdTimer) {
      window.clearTimeout(thresholdTimer)
    }
    thresholdTimer = window.setTimeout(() => {
      fetchList()
    }, 250)
  },
)

async function loadSchools() {
  const { data } = await listSchoolOptions()
  schoolOptions.value = data.data
}

async function loadUniforms(schoolId?: number) {
  const { data } = await listUniforms({
    pageNum: 1,
    pageSize: 200,
    status: 0,
    schoolId,
  })
  uniformOptions.value = data.data.records
}

async function handleSchoolChange(value?: number) {
  queryForm.uniformId = undefined
  await loadUniforms(value)
}

async function fetchList() {
  const [startDate, endDate] = queryForm.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }

  loading.value = true
  try {
    const { data } = await getLowConfidenceHotspots({
      schoolId: queryForm.schoolId,
      uniformId: queryForm.uniformId,
      threshold: queryForm.threshold,
      limit: queryForm.limit,
      startDate,
      endDate,
    })
    rows.value = data.data
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.dateRange = defaultDateRange() as [string, string]
  queryForm.schoolId = undefined
  queryForm.uniformId = undefined
  queryForm.threshold = 60
  queryForm.limit = 20
  loadUniforms()
  fetchList()
}

function exportCsv() {
  const [startDate, endDate] = queryForm.dateRange || []
  exporting.value = true
  try {
    downloadCsv(`low_confidence_hotspots_${startDate}_${endDate}.csv`, [
      '学校',
      '商品',
      '尺码',
      '低匹配把握次数',
      '总推荐数',
      '低匹配把握率',
      '平均匹配把握',
      '阈值',
    ], rows.value.map((row) => [
      schoolOptions.value.find((item) => item.schoolId === queryForm.schoolId)?.schoolName || '全部学校',
      row.uniformName || '--',
      row.sizeName || '--',
      row.lowConfidenceCount,
      row.totalRecommendations,
      formatPercent(row.lowConfidenceRate),
      row.avgConfidence,
      queryForm.threshold,
    ]))
  } finally {
    exporting.value = false
  }
}

function hotspotTag(value: number) {
  if (value >= 0.45) {
    return 'danger'
  }
  if (value >= 0.25) {
    return 'warning'
  }
  return 'success'
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
  return `${((value ?? 0) * 100).toFixed(digits)}%`
}
</script>

<style scoped lang="scss">
.basic-page {
  display: grid;
  gap: 18px;
}

.basic-page__toolbar,
.basic-page__table {
  padding: 20px;
}

.basic-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.hotspots__slider {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 320px;
}

.hotspots__info {
  color: var(--text-secondary);
}
</style>
