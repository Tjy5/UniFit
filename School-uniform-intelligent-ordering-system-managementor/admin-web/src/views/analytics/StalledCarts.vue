<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>滞留购物车</h1>
        <p>查看超过阈值且未转化为同商品订单的购物车候选项，辅助运营跟进。</p>
      </div>
    </div>

    <el-alert type="warning" :closable="false" show-icon title="本列表为候选名单，不代表已确认放弃" />

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="日期范围">
          <el-date-picker v-model="filters.dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" value-format="YYYY-MM-DD" @change="fetchList" />
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
          <el-select v-model="filters.uniformId" clearable filterable placeholder="全部商品" style="width: 220px" @change="fetchList">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" empty-text="当前筛选条件下暂无滞留购物车候选">
        <el-table-column prop="userAccount" label="用户" min-width="130">
          <template #default="{ row }">{{ row.userAccount || row.userId }}</template>
        </el-table-column>
        <el-table-column prop="uniformName" label="商品" min-width="220" />
        <el-table-column prop="sizeName" label="尺码" min-width="100" />
        <el-table-column prop="quantity" label="数量" min-width="90" />
        <el-table-column prop="hoursSinceAdd" label="滞留小时" min-width="110" />
        <el-table-column label="添加时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.addedAt) }}</template>
        </el-table-column>
        <el-table-column label="最近行为" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastActivityAt) || '--' }}</template>
        </el-table-column>
        <el-table-column prop="recommendationLogId" label="推荐日志" min-width="120">
          <template #default="{ row }">{{ row.recommendationLogId || '--' }}</template>
        </el-table-column>
      </el-table>

      <div class="basic-page__pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :background="true"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="fetchList"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStalledCarts } from '@/api/analytics'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import type { StalledCartRow, StalledCartsQuery } from '@/types/analytics'
import type { GradeForm, SchoolForm, UniformForm } from '@/types/basic-data'
import { downloadCsv, formatCsvDateTime } from '@/utils/csv'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<StalledCartRow[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])

const filters = reactive({
  dateRange: defaultDateRange() as [string, string],
  schoolId: undefined as number | undefined,
  gradeId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

onMounted(async () => {
  await Promise.all([loadSchools(), loadGrades(), loadUniforms()])
  await fetchList()
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
  pagination.pageNum = 1
  await Promise.all([loadGrades(value), loadUniforms()])
  await fetchList()
}

async function handleGradeChange() {
  filters.uniformId = undefined
  pagination.pageNum = 1
  await loadUniforms()
  await fetchList()
}

async function fetchList() {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const { data } = await getStalledCarts(params)
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function handlePageSizeChange() {
  pagination.pageNum = 1
  fetchList()
}

function resetFilters() {
  filters.dateRange = defaultDateRange() as [string, string]
  filters.schoolId = undefined
  filters.gradeId = undefined
  filters.uniformId = undefined
  pagination.pageNum = 1
  pagination.pageSize = 10
  Promise.all([loadGrades(), loadUniforms()]).then(fetchList)
}

function buildParams(): StalledCartsQuery | null {
  const [startDate, endDate] = filters.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return null
  }
  return {
    schoolId: filters.schoolId,
    gradeId: filters.gradeId,
    uniformId: filters.uniformId,
    startDate,
    endDate,
    limit: pagination.pageSize,
    offset: (pagination.pageNum - 1) * pagination.pageSize,
  }
}

function exportCsv() {
  exporting.value = true
  try {
    downloadCsv(`stalled_carts_${formatDate(new Date())}.csv`, [
      '用户',
      '商品',
      '尺码',
      '数量',
      '添加时间',
      '滞留小时',
      '推荐日志ID',
      '最近行为时间',
      '分页页码',
      '分页大小',
    ], rows.value.map((row) => [
      row.userAccount || row.userId,
      row.uniformName,
      row.sizeName,
      row.quantity,
      formatDateTime(row.addedAt),
      row.hoursSinceAdd,
      row.recommendationLogId,
      formatDateTime(row.lastActivityAt),
      pagination.pageNum,
      pagination.pageSize,
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

function formatDateTime(value?: string) {
  return formatCsvDateTime(value)
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

.basic-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
