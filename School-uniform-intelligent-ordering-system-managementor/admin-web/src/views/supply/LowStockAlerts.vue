<template>
  <section class="supply-page">
    <div class="page-title">
      <div>
        <h1>低库存告警</h1>
        <p>基于可用库存、补货点和安全库存识别缺口，并按严重程度优先排序。</p>
      </div>
      <div class="supply-page__actions">
        <el-button :loading="loading" plain type="primary" @click="fetchData">刷新</el-button>
        <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <section class="section-card supply-page__toolbar">
      <el-form :inline="true" :model="filters">
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
        <el-form-item label="严重程度">
          <el-select v-model="filters.severity" clearable placeholder="全部程度" style="width: 140px">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="预警" value="WARNING" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="supply-page__stats">
      <article class="section-card supply-page__stat">
        <span>告警总数</span>
        <strong>{{ rows.length }}</strong>
        <small>当前筛选范围内低库存 SKU</small>
      </article>
      <article class="section-card supply-page__stat">
        <span>严重告警</span>
        <strong>{{ criticalCount }}</strong>
        <small>缺口达到严重阈值的 SKU</small>
      </article>
      <article class="section-card supply-page__stat">
        <span>总缺口</span>
        <strong>{{ totalShortage }}</strong>
        <small>补货点与可用库存差额合计</small>
      </article>
    </div>

    <section class="section-card supply-page__table">
      <el-table v-loading="loading" :data="sortedRows" border stripe>
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="uniformName" label="校服" min-width="180" show-overflow-tooltip />
        <el-table-column prop="schoolName" label="学校" width="150" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="120" />
        <el-table-column prop="categoryKey" label="品类" width="120" />
        <el-table-column prop="sizeName" label="尺码" width="100" />
        <el-table-column prop="stockQuantity" label="库存" width="100" />
        <el-table-column prop="reservedQuantity" label="预留" width="100" />
        <el-table-column prop="availableQuantity" label="可用库存" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column prop="reorderPoint" label="补货点" width="100" />
        <el-table-column prop="shortageQuantity" label="缺口" width="100">
          <template #default="{ row }">
            <strong class="supply-page__danger">{{ row.shortageQuantity ?? shortage(row) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="程度" width="110">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : 'warning'">{{ severityText(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选条件下暂无低库存告警" />
        </template>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getSupplyLowStock } from '@/api/supply'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listSizes } from '@/api/sizes'
import { listUniforms } from '@/api/uniform'
import type { GradeForm, LowStockAlertRecord, SchoolForm, SizeForm, UniformForm } from '@/types/basic-data'
import { downloadCsv } from '@/utils/csv'

type AlertFilters = {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  sizeId?: number
  categoryKey?: string
  severity?: string
}

const loading = ref(false)
const exporting = ref(false)
const rows = ref<LowStockAlertRecord[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const sizeOptions = ref<SizeForm[]>([])
const filters = reactive<AlertFilters>({})

const sortedRows = computed(() =>
  [...rows.value].sort((left, right) => {
    const leftSeverity = left.severity === 'CRITICAL' ? 0 : 1
    const rightSeverity = right.severity === 'CRITICAL' ? 0 : 1
    if (leftSeverity !== rightSeverity) {
      return leftSeverity - rightSeverity
    }
    return shortage(right) - shortage(left)
  }),
)
const criticalCount = computed(() => rows.value.filter((item) => item.severity === 'CRITICAL').length)
const totalShortage = computed(() => rows.value.reduce((sum, item) => sum + shortage(item), 0))

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
    const { data } = await getSupplyLowStock(cleanParams(filters))
    rows.value = data.data || []
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
    severity: undefined,
  })
  await loadOptions()
  fetchData()
}

function exportCsv() {
  exporting.value = true
  try {
    downloadCsv(
      `low_stock_alerts_${new Date().toISOString().slice(0, 10)}.csv`,
      ['SKU ID', '校服', '学校', '年级', '品类', '尺码', '库存', '预留', '可用库存', '安全库存', '补货点', '缺口', '严重程度'],
      sortedRows.value.map((item) => [
        item.skuId,
        item.uniformName,
        item.schoolName,
        item.gradeName,
        item.categoryKey,
        item.sizeName,
        item.stockQuantity,
        item.reservedQuantity,
        item.availableQuantity,
        item.safetyStock,
        item.reorderPoint,
        shortage(item),
        severityText(item.severity),
      ]),
    )
  } finally {
    exporting.value = false
  }
}

function shortage(row: LowStockAlertRecord) {
  return Number(row.shortageQuantity ?? Math.max(0, Number(row.reorderPoint ?? 0) - Number(row.availableQuantity ?? 0)))
}

function severityText(value?: string) {
  if (value === 'CRITICAL') {
    return '严重'
  }
  if (value === 'WARNING') {
    return '预警'
  }
  return value || '--'
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
.supply-page__table {
  padding: 20px;
}

.supply-page__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.supply-page__danger {
  color: var(--danger);
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
