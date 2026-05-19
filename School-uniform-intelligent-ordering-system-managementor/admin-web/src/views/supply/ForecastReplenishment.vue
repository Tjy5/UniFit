<template>
  <section class="supply-page">
    <div class="page-title">
      <div>
        <h1>需求预测与补货建议</h1>
        <p>使用历史销量日均值生成确定性预测，并结合安全库存、当前可用量和提前期给出补货建议。</p>
      </div>
      <div class="supply-page__actions">
        <el-button :loading="loading" type="primary" @click="fetchData">计算</el-button>
        <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <section class="section-card supply-page__toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="回看天数"><el-input-number v-model="filters.lookbackDays" :min="7" :max="365" /></el-form-item>
        <el-form-item label="预测天数"><el-input-number v-model="filters.horizonDays" :min="1" :max="180" /></el-form-item>
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
        <el-form-item label="显示全部">
          <el-switch v-model="includeAll" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="supply-page__stats">
      <article class="section-card supply-page__stat">
        <span>预测 SKU</span>
        <strong>{{ forecastRows.length }}</strong>
        <small>当前筛选范围内参与预测的 SKU</small>
      </article>
      <article class="section-card supply-page__stat">
        <span>建议补货 SKU</span>
        <strong>{{ positiveSuggestionCount }}</strong>
        <small>建议数量大于 0 的 SKU</small>
      </article>
      <article class="section-card supply-page__stat">
        <span>建议补货总量</span>
        <strong>{{ totalSuggestedQuantity }}</strong>
        <small>所有补货建议数量合计</small>
      </article>
      <article class="section-card supply-page__stat">
        <span>低置信预测</span>
        <strong>{{ lowConfidenceCount }}</strong>
        <small>数据完整性低于 50%</small>
      </article>
    </div>

    <section class="section-card supply-page__panel">
      <div class="supply-page__panel-head">
        <div>
          <h2>预测结果</h2>
          <span>包含历史销量、日均销量、预测需求、数据完整性和原因码</span>
        </div>
      </div>
      <el-table v-loading="loading" :data="forecastRows" border stripe>
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="uniformName" label="校服" min-width="180" show-overflow-tooltip />
        <el-table-column prop="schoolName" label="学校" width="150" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="120" />
        <el-table-column prop="categoryKey" label="品类" width="120" />
        <el-table-column prop="sizeName" label="尺码" width="100" />
        <el-table-column prop="historicalQuantity" label="历史销量" width="110" />
        <el-table-column label="日均" width="100">
          <template #default="{ row }">{{ formatNumber(row.dailyAverage, 2) }}</template>
        </el-table-column>
        <el-table-column prop="forecastDemand" label="预测需求" width="110" />
        <el-table-column prop="availableQuantity" label="可用库存" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column label="完整性" width="110">
          <template #default="{ row }">{{ formatPercent(row.dataCompleteness) }}</template>
        </el-table-column>
        <el-table-column label="原因码" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="code in row.reasonCodes || []" :key="code" class="supply-page__reason" type="info">{{ code }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选条件下暂无预测数据" />
        </template>
      </el-table>
    </section>

    <section class="section-card supply-page__panel">
      <div class="supply-page__panel-head">
        <div>
          <h2>补货建议</h2>
          <span>默认仅显示需要补货的 SKU；开启显示全部可查看所有预测行</span>
        </div>
      </div>
      <el-table :data="suggestionRows" border stripe>
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="uniformName" label="校服" min-width="180" show-overflow-tooltip />
        <el-table-column prop="schoolName" label="学校" width="150" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="120" />
        <el-table-column prop="categoryKey" label="品类" width="120" />
        <el-table-column prop="sizeName" label="尺码" width="100" />
        <el-table-column prop="availableQuantity" label="可用库存" width="110" />
        <el-table-column prop="forecastDemand" label="预测需求" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column prop="leadTimeDays" label="提前期" width="100" />
        <el-table-column prop="suggestedQuantity" label="建议补货" width="120">
          <template #default="{ row }">
            <strong :class="{ 'supply-page__danger': Number(row.suggestedQuantity ?? 0) > 0 }">{{ row.suggestedQuantity ?? 0 }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="reasonText" label="原因" min-width="260" show-overflow-tooltip />
        <template #empty>
          <el-empty description="当前筛选条件下暂无补货建议" />
        </template>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getReplenishmentSuggestions, getSupplyForecast } from '@/api/supply'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listSizes } from '@/api/sizes'
import { listUniforms } from '@/api/uniform'
import type { GradeForm, ReplenishmentSuggestionRecord, SchoolForm, SizeForm, SupplyForecastRecord, UniformForm } from '@/types/basic-data'
import { downloadCsv } from '@/utils/csv'

type ForecastFilters = {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  sizeId?: number
  categoryKey?: string
  lookbackDays: number
  horizonDays: number
}

const loading = ref(false)
const exporting = ref(false)
const includeAll = ref(false)
const forecastRows = ref<SupplyForecastRecord[]>([])
const suggestionRows = ref<ReplenishmentSuggestionRecord[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const sizeOptions = ref<SizeForm[]>([])
const filters = reactive<ForecastFilters>({ lookbackDays: 90, horizonDays: 30 })

const positiveSuggestionCount = computed(() => suggestionRows.value.filter((item) => Number(item.suggestedQuantity ?? 0) > 0).length)
const totalSuggestedQuantity = computed(() => suggestionRows.value.reduce((sum, item) => sum + Number(item.suggestedQuantity ?? 0), 0))
const lowConfidenceCount = computed(() => forecastRows.value.filter((item) => Number(item.dataCompleteness ?? 0) < 0.5).length)

onMounted(async () => {
  await Promise.all([loadOptions(), fetchData()])
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
  loading.value = true
  try {
    const [forecast, suggestions] = await Promise.all([
      getSupplyForecast(buildQueryParams()),
      getReplenishmentSuggestions({ ...buildQueryParams(), includeAll: includeAll.value }),
    ])
    forecastRows.value = forecast.data.data || []
    suggestionRows.value = suggestions.data.data || []
  } finally {
    loading.value = false
  }
}

async function resetFilters() {
  Object.assign(filters, {
    schoolId: undefined,
    gradeId: undefined,
    uniformId: undefined,
    sizeId: undefined,
    categoryKey: undefined,
    lookbackDays: 90,
    horizonDays: 30,
  })
  includeAll.value = false
  await loadOptions()
  fetchData()
}

function exportCsv() {
  exporting.value = true
  try {
    downloadCsv(
      `forecast_replenishment_${new Date().toISOString().slice(0, 10)}.csv`,
      ['数据类型', 'SKU ID', '校服', '学校', '年级', '品类', '尺码', '历史销量', '日均', '预测需求', '可用库存', '安全库存', '补货点', '提前期', '建议补货', '数据完整性', '原因'],
      [
        ...forecastRows.value.map((item) => [
          '预测结果',
          item.skuId,
          item.uniformName,
          item.schoolName,
          item.gradeName,
          item.categoryKey,
          item.sizeName,
          item.historicalQuantity,
          formatNumber(item.dailyAverage, 2),
          item.forecastDemand,
          item.availableQuantity,
          item.safetyStock,
          item.reorderPoint,
          item.leadTimeDays,
          '',
          formatPercent(item.dataCompleteness),
          (item.reasonCodes || []).join('|'),
        ]),
        ...suggestionRows.value.map((item) => [
          '补货建议',
          item.skuId,
          item.uniformName,
          item.schoolName,
          item.gradeName,
          item.categoryKey,
          item.sizeName,
          '',
          '',
          item.forecastDemand,
          item.availableQuantity,
          item.safetyStock,
          item.reorderPoint,
          item.leadTimeDays,
          item.suggestedQuantity,
          '',
          item.reasonText,
        ]),
      ],
    )
  } finally {
    exporting.value = false
  }
}

function buildQueryParams() {
  return cleanParams(filters)
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

function formatNumber(value?: number, digits = 0) {
  return Number(value ?? 0).toFixed(digits)
}

function formatPercent(value?: number) {
  return `${(Number(value ?? 0) * 100).toFixed(1)}%`
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

.supply-page__reason {
  margin: 2px 4px 2px 0;
}

.supply-page__danger {
  color: var(--danger);
}

@media (max-width: 1200px) {
  .supply-page__stats {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 960px) {
  .page-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .supply-page__stats {
    grid-template-columns: 1fr;
  }
}
</style>
