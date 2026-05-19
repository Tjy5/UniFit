<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>评论审核</h1>
        <p>集中处理待审评论，查看正文与图片后快速做出通过或拒绝操作。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="校服ID">
          <el-input-number v-model="queryForm.uniformId" :min="1" controls-position="right" placeholder="校服ID" style="width: 180px" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option v-for="item in reviewStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <ExportButton :payload="{ uniformId: queryForm.uniformId, status: queryForm.status }" filename="reviews.xlsx" url="/reviews/export" />
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="reviewId" label="评论ID" min-width="100" />
        <el-table-column prop="userAccount" label="用户账号" min-width="140" />
        <el-table-column prop="uniformName" label="校服名称" min-width="180" />
        <el-table-column label="评分" min-width="100">
          <template #default="{ row }">
            <el-rate :model-value="row.rating || 0" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="内容预览" min-width="240">
          <template #default="{ row }">
            <div class="review-snippet">{{ row.content || '无评论内容' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="匿名" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isAnonymous" type="info">匿名</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusTag(row.status)">{{ getReviewStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.reviewId)">预览</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handleAudit(row.reviewId, 1)">通过</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleAudit(row.reviewId, 2)">拒绝</el-button>
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

    <el-dialog v-model="detailVisible" title="评论预览" width="760px">
      <template v-if="currentReview">
        <el-descriptions :column="2" border class="review-detail__desc">
          <el-descriptions-item label="评论ID">{{ currentReview.reviewId }}</el-descriptions-item>
          <el-descriptions-item label="用户账号">{{ currentReview.userAccount || '--' }}</el-descriptions-item>
          <el-descriptions-item label="校服名称">{{ currentReview.uniformName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="订单项ID">{{ currentReview.orderItemId || '--' }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ currentReview.rating || 0 }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ getReviewStatusLabel(currentReview.status) }}</el-descriptions-item>
          <el-descriptions-item label="评论时间" :span="2">{{ formatDateTime(currentReview.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="评论内容" :span="2">
            <div class="review-detail__content">{{ currentReview.content || '无评论内容' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="review-detail__images">
          <div class="review-detail__head">
            <h3>评论图片</h3>
            <span>{{ currentReviewImages.length }} 张</span>
          </div>
          <div v-if="currentReviewImages.length" class="review-detail__grid">
            <el-image
              v-for="image in currentReviewImages"
              :key="image"
              :preview-src-list="currentReviewImages"
              :src="image"
              class="review-detail__image"
              fit="cover"
            />
          </div>
          <el-empty v-else description="该评论未上传图片" />
        </div>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentReview?.status === 0"
          type="success"
          @click="handleAudit(currentReview.reviewId, 1)"
        >
          审核通过
        </el-button>
        <el-button
          v-if="currentReview?.status === 0"
          type="danger"
          @click="handleAudit(currentReview.reviewId, 2)"
        >
          审核拒绝
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { auditReview, getReview, listReviews } from '@/api/reviews'
import ExportButton from '@/components/ExportButton.vue'
import type { ReviewRecord } from '@/types/basic-data'
import { formatDateTime, splitImageUrls } from '@/utils/format'

const reviewStatusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
]

const loading = ref(false)
const detailVisible = ref(false)
const rows = ref<ReviewRecord[]>([])
const currentReview = ref<ReviewRecord>()

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  uniformId: undefined as number | undefined,
  status: undefined as number | undefined,
})

const currentReviewImages = computed(() => splitImageUrls(currentReview.value?.images))

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listReviews({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      uniformId: queryForm.uniformId,
      status: queryForm.status,
    })
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.uniformId = undefined
  queryForm.status = undefined
  pagination.pageNum = 1
  fetchList()
}

async function openDetail(reviewId: number) {
  const { data } = await getReview(reviewId)
  currentReview.value = data.data
  detailVisible.value = true
}

async function handleAudit(reviewId: number, status: number) {
  const actionLabel = status === 1 ? '通过' : '拒绝'
  await ElMessageBox.confirm(`确认${actionLabel}这条评论吗？`, '提示', { type: 'warning' })
  await auditReview(reviewId, { status })
  ElMessage.success(`评论已${actionLabel}`)
  detailVisible.value = false
  fetchList()
}

function getReviewStatusLabel(value?: number) {
  return reviewStatusOptions.find((item) => item.value === value)?.label || '未知'
}

function getReviewStatusTag(value?: number) {
  if (value === 1) {
    return 'success'
  }
  if (value === 2) {
    return 'danger'
  }
  return 'warning'
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

.review-snippet {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.review-detail__desc {
  margin-bottom: 20px;
}

.review-detail__content {
  white-space: pre-wrap;
  line-height: 1.7;
}

.review-detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.review-detail__head h3 {
  margin: 0;
}

.review-detail__head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.review-detail__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.review-detail__image {
  width: 100%;
  height: 120px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
}
</style>
