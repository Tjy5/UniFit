<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>尺码管理</h1>
        <p>维护尺码区间与推荐维度，并支持 Excel 导出。</p>
      </div>
      <el-tag type="info" effect="plain">空字段不参与推荐评分</el-tag>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="尺码名称">
          <el-input v-model="queryForm.sizeName" clearable placeholder="按尺码名称搜索" @keyup.enter="fetchList()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <el-button type="primary" @click="openCreate">新增尺码</el-button>
        <ExportButton :payload="{ sizeName: queryForm.sizeName || undefined }" filename="sizes.xlsx" url="/sizes/export" />
        <el-button :disabled="!selectedIds.length" type="danger" plain @click="handleDelete()">批量删除</el-button>
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" />
        <el-table-column prop="sizeName" label="尺码名称" min-width="120" fixed="left" />
        <el-table-column label="基础区间">
          <el-table-column prop="minHeight" label="最低身高(cm)" min-width="120" />
          <el-table-column prop="maxHeight" label="最高身高(cm)" min-width="120" />
          <el-table-column prop="minWeight" label="最小体重(kg)" min-width="120" />
          <el-table-column prop="maxWeight" label="最大体重(kg)" min-width="120" />
        </el-table-column>
        <el-table-column label="增强维度">
          <el-table-column v-for="field in measurementFields" :key="field.key" :label="field.label" min-width="150">
            <template #default="{ row }">
              {{ formatRange(row[field.minProp], row[field.maxProp], field.unit) }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="basic-page__pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :background="true"
          layout="total, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="尺码名称" prop="sizeName">
          <el-input v-model="form.sizeName" placeholder="请输入尺码名称" />
        </el-form-item>

        <section class="size-form__section">
          <div class="size-form__section-title">基础区间</div>
          <div class="size-form__grid">
            <el-form-item label="最低身高(cm)" prop="minHeight">
              <el-input-number v-model="form.minHeight" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最高身高(cm)" prop="maxHeight">
              <el-input-number v-model="form.maxHeight" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最小体重(kg)" prop="minWeight">
              <el-input-number v-model="form.minWeight" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最大体重(kg)" prop="maxWeight">
              <el-input-number v-model="form.maxWeight" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </div>
        </section>

        <section class="size-form__section">
          <div class="size-form__section-title">增强维度（可选）</div>
          <p class="size-form__hint">未填写的范围不会参与推荐评分，适合分阶段补齐多维度尺码数据。</p>
          <div class="size-form__grid">
            <template v-for="field in measurementFields" :key="field.key">
              <el-form-item :label="`${field.label}最小值(${field.unit})`" :prop="field.minProp">
                <el-input-number v-model="form[field.minProp]" :min="0" :precision="1" style="width: 100%" />
              </el-form-item>
              <el-form-item :label="`${field.label}最大值(${field.unit})`" :prop="field.maxProp">
                <el-input-number v-model="form[field.maxProp]" :min="0" :precision="1" style="width: 100%" />
              </el-form-item>
            </template>
          </div>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { createSize, deleteSize, getSize, listSizes, updateSize } from '@/api/sizes'
import ExportButton from '@/components/ExportButton.vue'
import type { SizeForm } from '@/types/basic-data'

const measurementFields = [
  { key: 'chest', label: '胸围', minProp: 'minChest', maxProp: 'maxChest', unit: 'cm' },
  { key: 'waist', label: '腰围', minProp: 'minWaist', maxProp: 'maxWaist', unit: 'cm' },
  { key: 'hip', label: '臀围', minProp: 'minHip', maxProp: 'maxHip', unit: 'cm' },
  { key: 'shoulder', label: '肩宽', minProp: 'minShoulder', maxProp: 'maxShoulder', unit: 'cm' },
] as const

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增尺码')
const rows = ref<SizeForm[]>([])
const selectedIds = ref<number[]>([])
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  sizeName: '',
})

const form = reactive<SizeForm>({
  sizeName: '',
  minHeight: undefined,
  maxHeight: undefined,
  minWeight: undefined,
  maxWeight: undefined,
  minChest: undefined,
  maxChest: undefined,
  minWaist: undefined,
  maxWaist: undefined,
  minHip: undefined,
  maxHip: undefined,
  minShoulder: undefined,
  maxShoulder: undefined,
})

const rules: FormRules<SizeForm> = {
  sizeName: [{ required: true, message: '请输入尺码名称', trigger: 'blur' }],
}

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listSizes({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      sizeName: queryForm.sizeName || undefined,
    })
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.sizeName = ''
  pagination.pageNum = 1
  fetchList()
}

function resetForm() {
  form.id = undefined
  form.sizeName = ''
  form.minHeight = undefined
  form.maxHeight = undefined
  form.minWeight = undefined
  form.maxWeight = undefined
  form.minChest = undefined
  form.maxChest = undefined
  form.minWaist = undefined
  form.maxWaist = undefined
  form.minHip = undefined
  form.maxHip = undefined
  form.minShoulder = undefined
  form.maxShoulder = undefined
}

function openCreate() {
  dialogTitle.value = '新增尺码'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(id?: number) {
  if (!id) {
    return
  }
  dialogTitle.value = '编辑尺码'
  const { data } = await getSize(id)
  Object.assign(form, data.data)
  dialogVisible.value = true
}

async function submitForm() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateSize(form)
      ElMessage.success('尺码更新成功')
    } else {
      await createSize(form)
      ElMessage.success('尺码创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

function handleSelectionChange(selection: SizeForm[]) {
  selectedIds.value = selection.map((item) => item.id!).filter(Boolean)
}

async function handleDelete(id?: number) {
  const ids = id ? [id] : selectedIds.value
  if (!ids.length) {
    return
  }
  await ElMessageBox.confirm('确认删除所选尺码吗？', '提示', { type: 'warning' })
  await deleteSize(ids.join(','))
  ElMessage.success('删除成功')
  fetchList()
}

function formatRange(min?: number, max?: number, unit?: string) {
  if (min == null && max == null) {
    return '未配置'
  }
  if (min != null && max != null) {
    return `${min}-${max}${unit ?? ''}`
  }
  if (min != null) {
    return `>= ${min}${unit ?? ''}`
  }
  return `<= ${max}${unit ?? ''}`
}
</script>

<style scoped lang="scss">
.basic-page {
  display: grid;
  gap: 18px;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.basic-page__toolbar,
.basic-page__table {
  padding: 20px;
}

.basic-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
}

.basic-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.basic-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.size-form__section {
  margin-top: 12px;
  padding: 18px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}

.size-form__section-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.size-form__hint {
  margin: 0 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
}

.size-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

:deep(.el-table .cell) {
  white-space: nowrap;
}

@media (max-width: 900px) {
  .page-title,
  .basic-page__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .size-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
