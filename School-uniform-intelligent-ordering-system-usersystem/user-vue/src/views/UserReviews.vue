<template>
  <div class="reviews-page">
    <section class="reviews-shell" data-testid="user-reviews-panel">
      <div class="reviews-shell__header">
        <div class="reviews-shell__copy">
          <span class="reviews-shell__eyebrow">评价记录</span>
          <h1 class="page-title">我的评价</h1>
          <p>集中查看已提交评价，并快速修改评分与评价内容。</p>
        </div>
        <el-tag class="reviews-shell__count" type="info">
          {{ total || reviewList.length }} 条记录
        </el-tag>
      </div>

      <div v-if="loading" class="reviews-shell__loading">
        <el-skeleton :rows="6" />
      </div>

      <div v-else-if="!reviewList.length" class="reviews-shell__empty">
        暂无评价
      </div>

      <div v-else class="reviews-list">
        <article v-for="review in reviewList" :key="review.reviewId" class="review-card">
          <div class="review-card__top">
            <div>
              <p class="review-card__id">评价ID：{{ review.reviewId }}</p>
              <h2>{{ review.uniformName }}</h2>
              <p class="review-card__user">用户：{{ review.displayName }}</p>
            </div>
            <div class="review-card__meta">
              <el-rate :model-value="review.rating" disabled show-score score-template="{value} 星" />
              <el-tag :type="review.isAnonymous ? 'info' : 'success'">{{ review.isAnonymous ? '匿名' : '实名' }}</el-tag>
            </div>
          </div>

          <p class="review-card__content">{{ review.content || '-' }}</p>
          <p class="review-card__time">评论时间：{{ parseTime(review.createTime) }}</p>

          <div class="review-card__actions">
            <el-button size="small" type="text" @click="handleEdit(review)">修改</el-button>
            <el-button size="small" type="text" @click="handleDelete(review.reviewId)">删除</el-button>
          </div>
        </article>
      </div>

      <div v-if="total > 0" class="reviews-shell__pagination">
        <el-pagination
          :total="total"
          :page-size="queryParams.pageSize"
          :current-page="queryParams.pageNum"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <el-dialog title="编辑评论" v-model:visible="openEdit" width="700px" append-to-body @close="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评论ID：">{{ editForm.reviewId }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名：">{{ editForm.displayName }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="校服名称：">{{ editForm.uniformName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分：" prop="rating">
              <el-rate v-model="editForm.rating" :max="5" show-score score-template="{value} 星" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否匿名：" prop="isAnonymous">
              <el-switch v-model="editForm.isAnonymous"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评论内容：" prop="content">
              <el-input
                v-model="editForm.content"
                type="textarea"
                :rows="5"
                placeholder="请输入评论内容 (可选)"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="openEdit = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitEditForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from "@/api/api";

export default {
  name: "UserReviews",
  data() {
    return {
      loading: true,
      submitLoading: false,
      total: 0,
      reviewList: [],
      openEdit: false,
      editForm: {
        reviewId: null,
        displayName: "",
        uniformName: "",
        rating: 0,
        isAnonymous: false,
        content: "",
        uniformId: null,
        orderItemId: null,
      },
      editFormRules: {
        rating: [
          { required: true, message: "评分不能为空", trigger: "change" },
          { type: "number", min: 1, message: "评分至少为1星", trigger: "change" },
        ],
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      api.getMySubmittedReviews(this.queryParams.pageNum, this.queryParams.pageSize)
        .then((response) => {
          const pageResult = response.data;
          if (pageResult && Array.isArray(pageResult.list)) {
            this.reviewList = pageResult.list;
            this.total = pageResult.total || 0;
          } else if (pageResult && Array.isArray(pageResult.rows)) {
            this.reviewList = pageResult.rows;
            this.total = pageResult.total || 0;
          } else {
            console.warn("getMySubmittedReviews: Received unexpected data structure:", pageResult);
            this.reviewList = [];
            this.total = 0;
          }
          this.loading = false;
        })
        .catch((error) => {
          console.error("Failed to load reviews:", error);
          this.reviewList = [];
          this.total = 0;
          this.loading = false;
          this.$message.error("加载评论列表失败，请稍后重试");
        });
    },
    handleSizeChange(newSize) {
      this.queryParams.pageSize = newSize;
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleCurrentChange(newPage) {
      this.queryParams.pageNum = newPage;
      this.getList();
    },
    handleEdit(row) {
      this.editForm = {
        reviewId: row.reviewId,
        displayName: row.displayName,
        uniformName: row.uniformName,
        rating: row.rating,
        isAnonymous: row.isAnonymous,
        content: row.content,
        uniformId: row.uniformId,
        orderItemId: row.orderItemId,
      };
      this.openEdit = true;
      this.$nextTick(() => {
        this.$refs.editFormRef?.clearValidate();
      });
    },
    resetEditForm() {
      this.editForm = {
        reviewId: null,
        displayName: "",
        uniformName: "",
        rating: 0,
        isAnonymous: false,
        content: "",
        uniformId: null,
        orderItemId: null,
      };
      this.$refs.editFormRef?.clearValidate();
    },
    submitEditForm() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (!valid) {
          this.$message.warning("请检查表单输入是否正确");
          return;
        }
        this.submitLoading = true;
        try {
          const updatePayload = {
            uniformId: this.editForm.uniformId,
            orderItemId: this.editForm.orderItemId,
            rating: this.editForm.rating,
            content: this.editForm.content,
            isAnonymous: this.editForm.isAnonymous,
          };
          await api.updateReview(this.editForm.reviewId, updatePayload);
          this.$message.success("评论更新成功！");
          this.openEdit = false;
          this.getList();
        } catch (error) {
          console.error("评论更新失败:", error);
          this.$message.error("评论更新失败：" + (error.response?.data?.message || error.message || "请稍后再试"));
        } finally {
          this.submitLoading = false;
        }
      });
    },
    handleDelete(reviewId) {
      this.$confirm("确认要删除该评论吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => api.deleteReview(reviewId))
        .then(() => {
          this.getList();
          this.$message.success("评论删除成功");
        })
        .catch((error) => {
          if (error !== "cancel") {
            console.info("删除操作已取消或失败:", error);
          }
        });
    },
    parseTime(time, pattern = "YYYY-MM-DD HH:mm:ss") {
      if (!time) return "";
      const date = new Date(time);
      if (isNaN(date.getTime())) {
        console.warn("Invalid time value for parseTime:", time);
        return time;
      }

      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, "0");
      const day = date.getDate().toString().padStart(2, "0");
      const hours = date.getHours().toString().padStart(2, "0");
      const minutes = date.getMinutes().toString().padStart(2, "0");
      const seconds = date.getSeconds().toString().padStart(2, "0");

      return pattern
        .replace("YYYY", year)
        .replace("MM", month)
        .replace("DD", day)
        .replace("HH", hours)
        .replace("mm", minutes)
        .replace("ss", seconds);
    },
  },
};
</script>

<style scoped>
.reviews-page {
  width: min(1200px, calc(100% - 40px));
  margin: 0 auto;
  min-height: calc(100vh - var(--navbar-height));
  padding: 24px 0 48px;
}

.reviews-shell {
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.reviews-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.reviews-shell__copy {
  max-width: 680px;
}

.reviews-shell__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 0;
}

.page-title {
  margin: 16px 0 10px;
  color: var(--text-primary);
  font-size: 26px;
  line-height: 1.2;
  letter-spacing: 0;
}

.reviews-shell__copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.reviews-list {
  display: grid;
  gap: 18px;
}

.review-card {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.review-card__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.review-card__id,
.review-card__user,
.review-card__time {
  margin: 4px 0;
  color: var(--text-secondary);
}

.review-card h2 {
  margin: 8px 0;
  color: var(--text-primary);
}

.review-card__meta {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.review-card__content {
  margin: 18px 0 12px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  color: var(--text-primary);
  white-space: pre-wrap;
}

.review-card__actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.reviews-shell__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.reviews-shell__loading,
.reviews-shell__empty {
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  padding: 24px;
}

.reviews-shell__empty {
  text-align: center;
  color: var(--text-secondary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .reviews-page {
    padding-top: 16px;
  }

  .reviews-shell {
    padding: 20px;
  }

  .reviews-shell__header,
  .review-card__top {
    flex-direction: column;
  }

  .review-card__meta,
  .reviews-shell__pagination {
    justify-items: start;
    justify-content: flex-start;
  }
}
</style>
