<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>学校管理</h1>
        <p>维护学校基础信息，为年级与校服模块提供关联数据。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="学校名称">
          <el-input v-model="queryForm.schoolName" clearable placeholder="按学校名称搜索" @keyup.enter="fetchList()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <el-button type="primary" @click="openCreate">新增学校</el-button>
        <el-button :disabled="!selectedIds.length" type="danger" plain @click="handleDelete()">批量删除</el-button>
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" />
        <el-table-column prop="schoolName" label="学校名称" min-width="180" />
        <el-table-column prop="schoolAddress" label="学校地址" min-width="220" />
        <el-table-column prop="contactPerson" label="联系人" min-width="120" />
        <el-table-column prop="contactPhone" label="联系电话" min-width="140" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.schoolId)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.schoolId)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="学校名称" prop="schoolName">
          <el-input v-model="form.schoolName" placeholder="请输入学校名称" />
        </el-form-item>
        <el-form-item label="学校地址" prop="schoolAddress">
          <el-input v-model="form.schoolAddress" placeholder="请输入学校地址" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
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
import { createSchool, deleteSchool, getSchool, listSchools, updateSchool } from '@/api/schools'
import type { SchoolForm } from '@/types/basic-data'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增学校')
const rows = ref<SchoolForm[]>([])
const selectedIds = ref<number[]>([])
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  schoolName: '',
})

const form = reactive<SchoolForm>({
  schoolName: '',
  schoolAddress: '',
  contactPerson: '',
  contactPhone: '',
})

const rules: FormRules<SchoolForm> = {
  schoolName: [{ required: true, message: '请输入学校名称', trigger: 'blur' }],
}

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listSchools({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      schoolName: queryForm.schoolName || undefined,
    })
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.schoolName = ''
  pagination.pageNum = 1
  fetchList()
}

function resetForm() {
  form.schoolId = undefined
  form.schoolName = ''
  form.schoolAddress = ''
  form.contactPerson = ''
  form.contactPhone = ''
}

function openCreate() {
  dialogTitle.value = '新增学校'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(schoolId?: number) {
  if (!schoolId) {
    return
  }
  dialogTitle.value = '编辑学校'
  const { data } = await getSchool(schoolId)
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
    if (form.schoolId) {
      await updateSchool(form)
      ElMessage.success('学校更新成功')
    } else {
      await createSchool(form)
      ElMessage.success('学校创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

function handleSelectionChange(selection: SchoolForm[]) {
  selectedIds.value = selection.map((item) => item.schoolId!).filter(Boolean)
}

async function handleDelete(id?: number) {
  const ids = id ? [id] : selectedIds.value
  if (!ids.length) {
    return
  }
  await ElMessageBox.confirm('确认删除所选学校吗？', '提示', { type: 'warning' })
  await deleteSchool(ids.join(','))
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
