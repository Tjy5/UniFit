<template>
  <section class="supply-page">
    <div class="page-title">
      <div>
        <h1>库存 SKU 管理</h1>
        <p>按校服与尺码维护库存、预留、安全库存、补货点和提前期，并记录入库与调整流水。</p>
      </div>
      <div class="supply-page__actions">
        <el-button :loading="loading" plain type="primary" @click="fetchData">刷新</el-button>
        <el-button :loading="exporting" type="primary" plain @click="exportCsv">导出 CSV</el-button>
        <el-button type="primary" @click="openCreateDialog">新增 SKU</el-button>
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
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card supply-page__table">
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="skuId" label="SKU ID" width="90" />
        <el-table-column prop="uniformName" label="校服" min-width="180" show-overflow-tooltip />
        <el-table-column prop="schoolName" label="学校" width="150" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="120" show-overflow-tooltip />
        <el-table-column prop="categoryKey" label="品类" width="120" />
        <el-table-column prop="sizeName" label="尺码" width="100" />
        <el-table-column prop="stockQuantity" label="库存" width="100" />
        <el-table-column prop="reservedQuantity" label="预留" width="100" />
        <el-table-column prop="availableQuantity" label="可用" width="100">
          <template #default="{ row }">
            <strong :class="{ 'supply-page__danger': Number(row.availableQuantity ?? 0) <= Number(row.reorderPoint ?? 0) }">
              {{ row.availableQuantity ?? 0 }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column prop="reorderPoint" label="补货点" width="100" />
        <el-table-column prop="leadTimeDays" label="提前期" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="success" @click="openMovementDialog(row, 'INBOUND')">入库</el-button>
            <el-button link type="warning" @click="openMovementDialog(row, 'ADJUST')">调整</el-button>
            <el-button link type="primary" @click="openMovementDrawer(row)">流水</el-button>
            <el-button link type="danger" @click="removeSku(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选条件下暂无库存 SKU" />
        </template>
      </el-table>

      <div class="supply-page__pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          @current-change="fetchData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="skuDialog.visible" :title="skuDialog.form.skuId ? '编辑 SKU' : '新增 SKU'" width="620px">
      <el-form label-width="110px" :model="skuDialog.form">
        <el-form-item label="校服">
          <el-select v-model="skuDialog.form.uniformId" filterable placeholder="请选择校服" style="width: 100%">
            <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="尺码">
          <el-select v-model="skuDialog.form.sizeId" filterable placeholder="请选择尺码" style="width: 100%">
            <el-option v-for="size in sizeOptions" :key="size.id" :label="size.sizeName" :value="size.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存"><el-input-number v-model="skuDialog.form.stockQuantity" :min="0" /></el-form-item>
        <el-form-item label="预留"><el-input-number v-model="skuDialog.form.reservedQuantity" :min="0" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="skuDialog.form.safetyStock" :min="0" /></el-form-item>
        <el-form-item label="补货点"><el-input-number v-model="skuDialog.form.reorderPoint" :min="0" /></el-form-item>
        <el-form-item label="提前期"><el-input-number v-model="skuDialog.form.leadTimeDays" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="skuDialog.form.status" style="width: 180px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="skuDialog.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skuDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitSku">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="movementDialog.visible" :title="movementDialog.type === 'INBOUND' ? '入库' : '库存调整'" width="460px">
      <el-descriptions :column="1" border class="supply-page__movement-context">
        <el-descriptions-item label="SKU ID">{{ movementDialog.sku?.skuId }}</el-descriptions-item>
        <el-descriptions-item label="校服">{{ movementDialog.sku?.uniformName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="尺码">{{ movementDialog.sku?.sizeName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ movementDialog.sku?.stockQuantity ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="当前预留">{{ movementDialog.sku?.reservedQuantity ?? 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="120px">
        <el-form-item :label="movementDialog.type === 'ADJUST' ? '调整后库存' : '入库数量'">
          <el-input-number
            v-model="movementDialog.quantity"
            :min="movementDialog.type === 'ADJUST' ? Number(movementDialog.sku?.reservedQuantity ?? 0) : 1"
          />
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="movementDialog.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="movementDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitMovement">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="movementDrawer.visible" size="720px" :title="`库存流水：SKU ${movementDrawer.sku?.skuId ?? ''}`">
      <el-form :inline="true" :model="movementFilters" class="supply-page__drawer-filter">
        <el-form-item label="类型">
          <el-select v-model="movementFilters.movementType" clearable placeholder="全部类型" style="width: 150px">
            <el-option v-for="type in movementTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID">
          <el-input-number v-model="movementFilters.relatedOrderId" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchMovements">查询</el-button>
          <el-button @click="resetMovementFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="movementDrawer.loading" :data="movementDrawer.rows" border stripe>
        <el-table-column prop="movementType" label="类型" width="110" />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="beforeStockQuantity" label="前库存" width="90" />
        <el-table-column prop="afterStockQuantity" label="后库存" width="90" />
        <el-table-column prop="beforeReservedQuantity" label="前预留" width="90" />
        <el-table-column prop="afterReservedQuantity" label="后预留" width="90" />
        <el-table-column prop="relatedOrderId" label="订单ID" width="100" />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无库存流水" />
        </template>
      </el-table>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { applyInventoryMovement, createInventorySku, deleteInventorySkus, listInventoryMovements, listInventorySkus, updateInventorySku } from '@/api/supply'
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { listSizes } from '@/api/sizes'
import { listUniforms } from '@/api/uniform'
import type { GradeForm, InventoryMovementRecord, InventorySkuPayload, InventorySkuRecord, SchoolForm, SizeForm, UniformForm } from '@/types/basic-data'
import { downloadCsv } from '@/utils/csv'
import { formatDateTime } from '@/utils/format'

type SkuFilters = {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  sizeId?: number
  categoryKey?: string
  status?: string
}

const movementTypes = ['INBOUND', 'ADJUST', 'RESERVE', 'RELEASE', 'OUTBOUND']
const loading = ref(false)
const exporting = ref(false)
const rows = ref<InventorySkuRecord[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const gradeOptions = ref<GradeForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const sizeOptions = ref<SizeForm[]>([])
const filters = reactive<SkuFilters>({})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const skuDialog = reactive({
  visible: false,
  form: {} as InventorySkuPayload,
})

const movementDialog = reactive({
  visible: false,
  sku: null as InventorySkuRecord | null,
  type: 'INBOUND',
  quantity: 1,
  reason: '',
})

const movementFilters = reactive({
  movementType: undefined as string | undefined,
  relatedOrderId: undefined as number | undefined,
})

const movementDrawer = reactive({
  visible: false,
  loading: false,
  sku: null as InventorySkuRecord | null,
  rows: [] as InventoryMovementRecord[],
})

onMounted(async () => {
  await loadOptions()
  await fetchData()
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
    const { data } = await listInventorySkus({ ...buildFilterParams(), pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    rows.value = data.data.records || []
    pagination.total = data.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchData()
}

function handleSizeChange() {
  pagination.pageNum = 1
  fetchData()
}

async function resetFilters() {
  Object.assign(filters, {
    schoolId: undefined,
    gradeId: undefined,
    uniformId: undefined,
    sizeId: undefined,
    categoryKey: undefined,
    status: undefined,
  })
  await loadOptions()
  handleSearch()
}

function openCreateDialog() {
  skuDialog.form = {
    stockQuantity: 0,
    reservedQuantity: 0,
    safetyStock: 0,
    reorderPoint: 0,
    leadTimeDays: 0,
    status: 'ACTIVE',
  }
  skuDialog.visible = true
}

function openEditDialog(row: InventorySkuRecord) {
  skuDialog.form = {
    skuId: row.skuId,
    uniformId: row.uniformId,
    sizeId: row.sizeId,
    stockQuantity: row.stockQuantity ?? 0,
    reservedQuantity: row.reservedQuantity ?? 0,
    safetyStock: row.safetyStock ?? 0,
    reorderPoint: row.reorderPoint ?? 0,
    leadTimeDays: row.leadTimeDays ?? 0,
    status: row.status || 'ACTIVE',
    remark: row.remark,
  }
  skuDialog.visible = true
}

async function submitSku() {
  const stock = Number(skuDialog.form.stockQuantity ?? 0)
  const reserved = Number(skuDialog.form.reservedQuantity ?? 0)
  if (!skuDialog.form.uniformId || !skuDialog.form.sizeId) {
    ElMessage.warning('请选择校服和尺码')
    return
  }
  if (reserved > stock) {
    ElMessage.warning('预留库存不能大于库存')
    return
  }
  if (skuDialog.form.skuId) {
    await updateInventorySku(skuDialog.form)
  } else {
    await createInventorySku(skuDialog.form)
  }
  ElMessage.success('保存成功')
  skuDialog.visible = false
  fetchData()
}

function openMovementDialog(row: InventorySkuRecord, type: string) {
  movementDialog.sku = row
  movementDialog.type = type
  movementDialog.quantity = type === 'ADJUST' ? Number(row.stockQuantity ?? 0) : 1
  movementDialog.reason = ''
  movementDialog.visible = true
}

async function submitMovement() {
  if (!movementDialog.sku?.skuId) {
    return
  }
  if (movementDialog.type === 'INBOUND' && movementDialog.quantity <= 0) {
    ElMessage.warning('入库数量必须大于 0')
    return
  }
  if (movementDialog.type === 'ADJUST' && !movementDialog.reason.trim()) {
    ElMessage.warning('库存调整必须填写原因')
    return
  }
  await applyInventoryMovement({
    skuId: movementDialog.sku.skuId,
    movementType: movementDialog.type,
    quantity: movementDialog.quantity,
    reason: movementDialog.reason,
  })
  ElMessage.success('库存流水处理成功')
  movementDialog.visible = false
  fetchData()
}

async function openMovementDrawer(row: InventorySkuRecord) {
  movementDrawer.sku = row
  movementDrawer.visible = true
  resetMovementFilters(false)
  await fetchMovements()
}

async function fetchMovements() {
  if (!movementDrawer.sku?.skuId) {
    return
  }
  movementDrawer.loading = true
  try {
    const { data } = await listInventoryMovements({
      skuId: movementDrawer.sku.skuId,
      ...cleanParams(movementFilters),
    })
    movementDrawer.rows = data.data || []
  } finally {
    movementDrawer.loading = false
  }
}

function resetMovementFilters(shouldFetch = true) {
  movementFilters.movementType = undefined
  movementFilters.relatedOrderId = undefined
  if (shouldFetch) {
    fetchMovements()
  }
}

async function removeSku(row: InventorySkuRecord) {
  await ElMessageBox.confirm(`确认删除 SKU ${row.skuId}？`, '提示', { type: 'warning' })
  await deleteInventorySkus(String(row.skuId))
  ElMessage.success('删除成功')
  fetchData()
}

async function exportCsv() {
  exporting.value = true
  try {
    const { data } = await listInventorySkus({ ...buildFilterParams(), pageNum: 1, pageSize: 10000 })
    const exportRows = data.data.records || []
    downloadCsv(
      `inventory_skus_${new Date().toISOString().slice(0, 10)}.csv`,
      ['SKU ID', '校服', '学校', '年级', '品类', '尺码', '库存', '预留', '可用', '安全库存', '补货点', '提前期', '状态', '备注'],
      exportRows.map((item) => [
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
        item.leadTimeDays,
        statusText(item.status),
        item.remark,
      ]),
    )
  } finally {
    exporting.value = false
  }
}

function buildFilterParams() {
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

function statusText(status?: string) {
  if (status === 'ACTIVE') {
    return '启用'
  }
  if (status === 'INACTIVE') {
    return '停用'
  }
  return status || '--'
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

.supply-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.supply-page__danger {
  color: var(--danger);
}

.supply-page__movement-context {
  margin-bottom: 18px;
}

.supply-page__drawer-filter {
  margin-bottom: 14px;
}

@media (max-width: 960px) {
  .page-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
