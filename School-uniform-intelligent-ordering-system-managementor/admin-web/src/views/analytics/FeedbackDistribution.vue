<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>反馈分布分析</h1>
        <p>按学校、商品或尺码观察合身 / 偏大 / 偏小的分布，并锁定问题最集中的分组。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="分组方式">
          <el-select v-model="queryForm.groupBy" style="width: 160px" @change="handleGroupByChange">
            <el-option label="学校" value="school" />
            <el-option label="商品" value="product" />
            <el-option label="尺码" value="size" />
          </el-select>
        </el-form-item>
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
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @sort-change="handleSortChange">
        <el-table-column prop="groupName" label="分组" min-width="220" />
        <el-table-column prop="totalFeedback" label="总反馈数" min-width="120" sortable="custom" />
        <el-table-column prop="fit" label="合身" min-width="100" />
        <el-table-column label="合身率" min-width="120" sortable="custom" prop="fitRate">
          <template #default="{ row }">
            <el-tag :type="fitRateTag(row.fitRate)" effect="plain">{{ formatPercent(row.fitRate) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="偏大%" min-width="120" sortable="custom" prop="tooLargeRate">
          <template #default="{ row }">
            {{ formatPercent(tooLargeRate(row)) }}
          </template>
        </el-table-column>
        <el-table-column label="偏小%" min-width="120" sortable="custom" prop="tooSmallRate">
          <template #default="{ row }">
            {{ formatPercent(tooSmallRate(row)) }}
          </template>
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
import type { TableColumnCtx } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { getFeedbackDistribution } from '@/api/analytics'
import { listSchoolOptions } from '@/api/schools'
import { listUniforms } from '@/api/uniform'
import type { SchoolForm, UniformForm } from '@/types/basic-data'
import type { FeedbackDistributionGroup, FeedbackDistributionQuery } from '@/types/analytics'
import { downloadCsv } from '@/utils/csv'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<FeedbackDistributionGroup[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const uniformOptions = ref<UniformForm[]>([])

const queryForm = reactive({
  groupBy: 'school' as FeedbackDistributionQuery['groupBy'],
  dateRange: defaultDateRange() as [string, string],
  schoolId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
  sortBy: 'fitRate' as NonNullable<FeedbackDistributionQuery['sortBy']>,
  sortOrder: 'asc' as NonNullable<FeedbackDistributionQuery['sortOrder']>,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

onMounted(async () => {
  await Promise.all([loadSchools(), loadUniforms()])
  await fetchList()
})

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

function handleGroupByChange() {
  pagination.pageNum = 1
  fetchList()
}

async function fetchList() {
  const [startDate, endDate] = queryForm.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }

  loading.value = true
  try {
    const { data } = await getFeedbackDistribution({
      groupBy: queryForm.groupBy,
      schoolId: queryForm.schoolId,
      uniformId: queryForm.uniformId,
      startDate,
      endDate,
      limit: pagination.pageSize,
      offset: (pagination.pageNum - 1) * pagination.pageSize,
      sortBy: queryForm.sortBy,
      sortOrder: queryForm.sortOrder,
    })
    rows.value = data.data.groups
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.groupBy = 'school'
  queryForm.dateRange = defaultDateRange() as [string, string]
  queryForm.schoolId = undefined
  queryForm.uniformId = undefined
  queryForm.sortBy = 'fitRate'
  queryForm.sortOrder = 'asc'
  pagination.pageNum = 1
  pagination.pageSize = 10
  loadUniforms()
  fetchList()
}

function handlePageSizeChange() {
  pagination.pageNum = 1
  fetchList()
}

function handleSortChange({ prop, order }: { column: TableColumnCtx<FeedbackDistributionGroup>; prop: string; order: 'ascending' | 'descending' | null }) {
  if (!prop || !order) {
    queryForm.sortBy = 'fitRate'
    queryForm.sortOrder = 'asc'
  } else {
    queryForm.sortBy = mapSortProp(prop)
    queryForm.sortOrder = order === 'descending' ? 'desc' : 'asc'
  }
  fetchList()
}

async function exportCsv() {
  const [startDate, endDate] = queryForm.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }

  exporting.value = true
  try {
    const { data } = await getFeedbackDistribution({
      groupBy: queryForm.groupBy,
      schoolId: queryForm.schoolId,
      uniformId: queryForm.uniformId,
      startDate,
      endDate,
      limit: Math.min(Math.max(pagination.total, pagination.pageSize), 200),
      offset: 0,
      sortBy: queryForm.sortBy,
      sortOrder: queryForm.sortOrder,
    })

    if (data.data.total > 200) {
      ElMessage.warning('当前仅导出前 200 条筛选结果')
    }

    downloadCsv(`feedback_distribution_${startDate}_${endDate}.csv`, [
      '分组类型',
      '分组名称',
      '总反馈数',
      '合身',
      '偏大',
      '偏小',
      '合身率',
      '偏大率',
      '偏小率',
    ], data.data.groups.map((row) => [
      groupByLabel(queryForm.groupBy),
      row.groupName,
      row.totalFeedback,
      row.fit,
      row.tooLarge,
      row.tooSmall,
      formatPercent(row.fitRate),
      formatPercent(tooLargeRate(row)),
      formatPercent(tooSmallRate(row)),
    ]))
  } finally {
    exporting.value = false
  }
}

function tooLargeRate(row: FeedbackDistributionGroup) {
  return row.totalFeedback ? row.tooLarge / row.totalFeedback : 0
}

function tooSmallRate(row: FeedbackDistributionGroup) {
  return row.totalFeedback ? row.tooSmall / row.totalFeedback : 0
}

function fitRateTag(value: number) {
  if (value >= 0.8) {
    return 'success'
  }
  if (value >= 0.65) {
    return 'warning'
  }
  return 'danger'
}

function mapSortProp(prop: string): NonNullable<FeedbackDistributionQuery['sortBy']> {
  switch (prop) {
    case 'totalFeedback':
      return 'totalFeedback'
    case 'fitRate':
    default:
      return 'fitRate'
  }
}

function groupByLabel(value: FeedbackDistributionQuery['groupBy']) {
  if (value === 'product') {
    return '商品'
  }
  if (value === 'size') {
    return '尺码'
  }
  return '学校'
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

.basic-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
