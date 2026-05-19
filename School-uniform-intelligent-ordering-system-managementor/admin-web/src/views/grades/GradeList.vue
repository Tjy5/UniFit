<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>年级管理</h1>
        <p>维护年级与学校的关联关系，供校服录入和筛选使用。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="年级名称">
          <el-input v-model="queryForm.gradeName" clearable placeholder="按年级名称搜索" @keyup.enter="fetchList()" />
        </el-form-item>
        <el-form-item label="学校">
          <el-select v-model="queryForm.schoolId" clearable placeholder="筛选学校" style="width: 220px">
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <el-button type="primary" @click="openCreate">新增年级</el-button>
        <el-button :disabled="!selectedIds.length" type="danger" plain @click="handleDelete()">批量删除</el-button>
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" />
        <el-table-column prop="gradeName" label="年级名称" min-width="160" />
        <el-table-column prop="schoolName" label="所属学校" min-width="180" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.gradeId)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.gradeId)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="年级名称" prop="gradeName">
          <el-input v-model="form.gradeName" placeholder="请输入年级名称" />
        </el-form-item>
        <el-form-item label="所属学校" prop="schoolId">
          <el-select v-model="form.schoolId" placeholder="请选择学校" style="width: 100%">
            <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
          </el-select>
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
import { createGrade, deleteGrade, getGrade, listGrades } from '@/api/grades'
import { updateGrade } from '@/api/grades'
import { listSchoolOptions } from '@/api/schools'
import type { GradeForm, SchoolForm } from '@/types/basic-data'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增年级')
const rows = ref<GradeForm[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const selectedIds = ref<number[]>([])
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  gradeName: '',
  schoolId: undefined as number | undefined,
})

const form = reactive<GradeForm>({
  gradeName: '',
  schoolId: undefined,
})

const rules: FormRules<GradeForm> = {
  gradeName: [{ required: true, message: '请输入年级名称', trigger: 'blur' }],
  schoolId: [{ required: true, message: '请选择所属学校', trigger: 'change' }],
}

onMounted(async () => {
  await loadSchools()
  fetchList()
})

async function loadSchools() {
  const { data } = await listSchoolOptions()
  schoolOptions.value = data.data
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listGrades({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      gradeName: queryForm.gradeName || undefined,
      schoolId: queryForm.schoolId,
    })
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.gradeName = ''
  queryForm.schoolId = undefined
  pagination.pageNum = 1
  fetchList()
}

function resetForm() {
  form.gradeId = undefined
  form.gradeName = ''
  form.schoolId = undefined
}

function openCreate() {
  dialogTitle.value = '新增年级'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(gradeId?: number) {
  if (!gradeId) {
    return
  }
  dialogTitle.value = '编辑年级'
  const { data } = await getGrade(gradeId)
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
    if (form.gradeId) {
      await updateGrade(form)
      ElMessage.success('年级更新成功')
    } else {
      await createGrade(form)
      ElMessage.success('年级创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

function handleSelectionChange(selection: GradeForm[]) {
  selectedIds.value = selection.map((item) => item.gradeId!).filter(Boolean)
}

async function handleDelete(id?: number) {
  const ids = id ? [id] : selectedIds.value
  if (!ids.length) {
    return
  }
  await ElMessageBox.confirm('确认删除所选年级吗？', '提示', { type: 'warning' })
  await deleteGrade(ids.join(','))
  ElMessage.success('删除成功')
  fetchList()
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
</style>
