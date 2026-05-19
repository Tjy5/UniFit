<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>穿搭指南</h1>
        <p>发布图文搭配建议内容，关联具体校服并管理展示状态。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="标题">
          <el-input v-model="queryForm.title" clearable placeholder="按标题搜索" @keyup.enter="fetchList()" />
        </el-form-item>
        <el-form-item label="关联校服">
          <el-select v-model="queryForm.uniformId" clearable filterable placeholder="筛选校服" style="width: 240px">
            <el-option v-for="item in uniformOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option v-for="item in guideStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <el-button type="primary" @click="openCreate">新增指南</el-button>
        <el-button :disabled="!selectedIds.length" type="danger" plain @click="handleDelete()">批量删除</el-button>
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" />
        <el-table-column label="封面" min-width="100">
          <template #default="{ row }">
            <el-image v-if="row.imageList?.length" :preview-src-list="row.imageList" :src="row.imageList[0]" class="guide-cover" fit="cover" />
            <span v-else class="muted">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="uniformName" label="关联校服" min-width="180" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ getGuideStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" destroy-on-close fullscreen>
      <div class="guide-form">
        <section class="guide-form__sidebar section-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入指南标题" />
            </el-form-item>
            <el-form-item label="关联校服" prop="uniformId">
              <el-select v-model="form.uniformId" filterable placeholder="请选择校服" style="width: 100%">
                <el-option v-for="item in uniformOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio-button v-for="item in guideStatusOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="封面/配图">
              <ImageUpload v-model="form.imageList" :limit="6" />
            </el-form-item>
          </el-form>
        </section>

        <section class="guide-form__editor">
          <RichTextEditor v-model="form.content" placeholder="请输入穿搭指南正文内容..." />
        </section>
      </div>
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
import { defineAsyncComponent, onMounted, reactive, ref } from 'vue'
import { createGuide, deleteGuide, getGuide, listGuides, updateGuide } from '@/api/guides'
import { listUniforms } from '@/api/uniform'
import type { GuideForm, UniformForm } from '@/types/basic-data'
import { formatDateTime, joinImageUrls, splitImageUrls } from '@/utils/format'

const ImageUpload = defineAsyncComponent(() => import('@/components/ImageUpload.vue'))
const RichTextEditor = defineAsyncComponent(() => import('@/components/RichTextEditor.vue'))

const guideStatusOptions = [
  { label: '已发布', value: 0 },
  { label: '已停用', value: 1 },
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增指南')
const rows = ref<GuideForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const selectedIds = ref<number[]>([])
const formRef = ref<FormInstance>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  title: '',
  uniformId: undefined as number | undefined,
  status: undefined as number | undefined,
})

const form = reactive<GuideForm>({
  title: '',
  uniformId: undefined,
  content: '',
  imageList: [],
  status: 0,
})

const rules: FormRules<GuideForm> = {
  title: [{ required: true, message: '请输入指南标题', trigger: 'blur' }],
  uniformId: [{ required: true, message: '请选择关联校服', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
}

onMounted(async () => {
  await loadUniformOptions()
  fetchList()
})

async function loadUniformOptions() {
  const { data } = await listUniforms({
    pageNum: 1,
    pageSize: 200,
  })
  uniformOptions.value = data.data.records
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listGuides({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      title: queryForm.title || undefined,
      uniformId: queryForm.uniformId,
      status: queryForm.status,
    })
    rows.value = data.data.records.map((item) => ({
      ...item,
      imageList: splitImageUrls(item.image),
    }))
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.title = ''
  queryForm.uniformId = undefined
  queryForm.status = undefined
  pagination.pageNum = 1
  fetchList()
}

function resetForm() {
  form.id = undefined
  form.title = ''
  form.uniformId = undefined
  form.content = ''
  form.imageList = []
  form.status = 0
}

function handleSelectionChange(selection: GuideForm[]) {
  selectedIds.value = selection.map((item) => item.id!).filter(Boolean)
}

function openCreate() {
  dialogTitle.value = '新增指南'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(id?: number) {
  if (!id) {
    return
  }
  dialogTitle.value = '编辑指南'
  const { data } = await getGuide(id)
  Object.assign(form, {
    id: data.data.id,
    title: data.data.title,
    uniformId: data.data.uniformId,
    content: data.data.content || '',
    imageList: splitImageUrls(data.data.image),
    status: data.data.status ?? 0,
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
      title: form.title,
      uniformId: form.uniformId,
      content: form.content,
      image: joinImageUrls(form.imageList || []),
      status: form.status,
    }
    if (form.id) {
      await updateGuide(payload)
      ElMessage.success('穿搭指南更新成功')
    } else {
      await createGuide(payload)
      ElMessage.success('穿搭指南创建成功')
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
  await ElMessageBox.confirm('确认删除所选穿搭指南吗？', '提示', { type: 'warning' })
  await deleteGuide(ids.join(','))
  ElMessage.success('删除成功')
  fetchList()
}

function getGuideStatusLabel(value?: number) {
  return guideStatusOptions.find((item) => item.value === value)?.label || '未知'
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
  gap: 12px;
}

.basic-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.guide-cover {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}

.guide-form {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 220px);
}

.guide-form__sidebar {
  padding: 20px;
  background: var(--surface);
}

.guide-form__editor {
  min-width: 0;
}

.muted {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1200px) {
  .guide-form {
    grid-template-columns: 1fr;
  }
}
</style>
