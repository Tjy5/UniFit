<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>校服管理</h1>
        <p>维护校服资料、学校年级关联、上下架状态和多图资源。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="校服名称">
          <el-input v-model="queryForm.name" clearable placeholder="按校服名称搜索" @keyup.enter="fetchList()" />
        </el-form-item>
        <el-form-item label="学校">
          <el-select
            v-model="queryForm.schoolId"
            clearable
            placeholder="筛选学校"
            style="width: 220px"
            @change="handleQuerySchoolChange"
          >
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="queryForm.gradeId" clearable placeholder="筛选年级" style="width: 220px">
            <el-option v-for="grade in queryGradeOptions" :key="grade.gradeId" :label="grade.gradeName" :value="grade.gradeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 160px">
            <el-option v-for="item in uniformStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <el-button type="primary" @click="openCreate">新增校服</el-button>
        <ExportButton
          :payload="{ name: queryForm.name || undefined, schoolId: queryForm.schoolId, gradeId: queryForm.gradeId, status: queryForm.status }"
          filename="uniforms.xlsx"
          url="/uniform/export"
        />
        <el-button :disabled="!selectedIds.length" type="danger" plain @click="handleDelete()">批量删除</el-button>
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" />
        <el-table-column label="封面" min-width="100">
          <template #default="{ row }">
            <el-image v-if="row.imageList?.length" :preview-src-list="row.imageList" :src="row.imageList[0]" class="uniform-cover" fit="cover" />
            <span v-else class="muted">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="校服名称" min-width="180" />
        <el-table-column prop="schoolName" label="学校" min-width="160" />
        <el-table-column prop="gradeName" label="年级" min-width="120" />
        <el-table-column label="价格" min-width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.price) }}
          </template>
        </el-table-column>
        <el-table-column label="评分" min-width="120">
          <template #default="{ row }">
            <div class="rating-cell">
              <strong>{{ row.averageRating ?? 0 }}</strong>
              <span>{{ row.reviewCount ?? 0 }} 条评价</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 0"
              active-text="上架"
              inactive-text="下架"
              @change="handleStatusChange(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
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
        <div class="uniform-form__grid">
          <el-form-item label="校服名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入校服名称" />
          </el-form-item>
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="所属学校" prop="schoolId">
            <el-select v-model="form.schoolId" placeholder="请选择学校" style="width: 100%" @change="handleFormSchoolChange">
              <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属年级" prop="gradeId">
            <el-select v-model="form.gradeId" placeholder="请选择年级" style="width: 100%">
              <el-option v-for="grade in formGradeOptions" :key="grade.gradeId" :label="grade.gradeName" :value="grade.gradeId" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="商品介绍" prop="intro">
          <el-input v-model="form.intro" :rows="3" type="textarea" placeholder="请输入校服介绍" />
        </el-form-item>
        <el-form-item label="商品图片">
          <ImageUpload v-model="form.imageList" :limit="6" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button v-for="item in uniformStatusOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" :rows="2" type="textarea" placeholder="选填" />
        </el-form-item>
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
import { listGradeOptions } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import { createUniform, deleteUniform, getUniform, listUniforms, updateUniform } from '@/api/uniform'
import ExportButton from '@/components/ExportButton.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import type { GradeForm, SchoolForm, UniformForm } from '@/types/basic-data'
import { formatCurrency, joinImageUrls, splitImageUrls } from '@/utils/format'

const uniformStatusOptions = [
  { label: '上架', value: 0 },
  { label: '下架', value: 1 },
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增校服')
const rows = ref<UniformForm[]>([])
const selectedIds = ref<number[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const queryGradeOptions = ref<GradeForm[]>([])
const formGradeOptions = ref<GradeForm[]>([])
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  name: '',
  schoolId: undefined as number | undefined,
  gradeId: undefined as number | undefined,
  status: undefined as number | undefined,
})

const form = reactive<UniformForm>({
  name: '',
  intro: '',
  imageList: [],
  price: undefined,
  status: 0,
  schoolId: undefined,
  gradeId: undefined,
  remark: '',
})

const rules: FormRules<UniformForm> = {
  name: [{ required: true, message: '请输入校服名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入校服价格', trigger: 'blur' }],
  schoolId: [{ required: true, message: '请选择学校', trigger: 'change' }],
  gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
}

onMounted(async () => {
  await loadSchools()
  fetchList()
})

async function loadSchools() {
  const { data } = await listSchoolOptions()
  schoolOptions.value = data.data
}

async function loadQueryGrades(schoolId?: number) {
  if (!schoolId) {
    queryGradeOptions.value = []
    return
  }
  const { data } = await listGradeOptions({ schoolId })
  queryGradeOptions.value = data.data
}

async function loadFormGrades(schoolId?: number) {
  if (!schoolId) {
    formGradeOptions.value = []
    return
  }
  const { data } = await listGradeOptions({ schoolId })
  formGradeOptions.value = data.data
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listUniforms({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      name: queryForm.name || undefined,
      schoolId: queryForm.schoolId,
      gradeId: queryForm.gradeId,
      status: queryForm.status,
    })
    rows.value = data.data.records.map(normalizeRow)
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function normalizeRow(item: UniformForm): UniformForm {
  return {
    ...item,
    imageList: splitImageUrls(item.image),
  }
}

function resetQuery() {
  queryForm.name = ''
  queryForm.schoolId = undefined
  queryForm.gradeId = undefined
  queryForm.status = undefined
  queryGradeOptions.value = []
  pagination.pageNum = 1
  fetchList()
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.intro = ''
  form.imageList = []
  form.price = undefined
  form.status = 0
  form.schoolId = undefined
  form.gradeId = undefined
  form.remark = ''
  formGradeOptions.value = []
}

function handleSelectionChange(selection: UniformForm[]) {
  selectedIds.value = selection.map((item) => item.id!).filter(Boolean)
}

async function handleQuerySchoolChange(value?: number) {
  queryForm.gradeId = undefined
  await loadQueryGrades(value)
}

async function handleFormSchoolChange(value?: number) {
  form.gradeId = undefined
  await loadFormGrades(value)
}

function openCreate() {
  dialogTitle.value = '新增校服'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(id?: number) {
  if (!id) {
    return
  }
  dialogTitle.value = '编辑校服'
  const { data } = await getUniform(id)
  const record = data.data
  await loadFormGrades(record.schoolId)
  Object.assign(form, {
    id: record.id,
    name: record.name,
    intro: record.intro || '',
    imageList: splitImageUrls(record.image),
    price: record.price,
    status: record.status ?? 0,
    schoolId: record.schoolId,
    gradeId: record.gradeId,
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      intro: form.intro,
      image: joinImageUrls(form.imageList || []),
      price: form.price,
      status: form.status,
      schoolId: form.schoolId,
      gradeId: form.gradeId,
      remark: form.remark,
    }
    if (form.id) {
      await updateUniform(payload)
      ElMessage.success('校服更新成功')
    } else {
      await createUniform(payload)
      ElMessage.success('校服创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id?: number) {
  const ids = id ? [id] : selectedIds.value
  if (!ids.length) {
    return
  }
  await ElMessageBox.confirm('确认删除所选校服吗？', '提示', { type: 'warning' })
  await deleteUniform(ids.join(','))
  ElMessage.success('删除成功')
  fetchList()
}

async function handleStatusChange(row: UniformForm, enabled: string | number | boolean) {
  const nextStatus = enabled ? 0 : 1
  await updateUniform({
    id: row.id,
    name: row.name,
    intro: row.intro,
    image: row.image,
    price: row.price,
    status: nextStatus,
    schoolId: row.schoolId,
    gradeId: row.gradeId,
    remark: row.remark,
  })
  row.status = nextStatus
  ElMessage.success(`校服已${nextStatus === 0 ? '上架' : '下架'}`)
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

.uniform-cover {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}

.rating-cell {
  display: grid;
  gap: 4px;
}

.rating-cell strong {
  font-size: 16px;
  color: var(--brand);
}

.rating-cell span,
.muted {
  color: var(--text-secondary);
  font-size: 12px;
}

.uniform-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
</style>
