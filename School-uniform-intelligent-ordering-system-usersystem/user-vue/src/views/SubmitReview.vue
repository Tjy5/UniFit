<template>
  <div class="submit-review-container">
    <h1 class="page-title">评价商品</h1>

    <el-card v-if="uniformInfo.name" class="uniform-info-card">
      <template #header>
      <div class="clearfix">
        <span>您正在评价以下商品</span>
      </div>
      </template>
      <div class="uniform-details">
        <img :src="uniformInfo.image" :alt="uniformInfo.name" class="uniform-image-preview" @error="onImageError" />
        <div class="uniform-text-details">
          <h3 class="uniform-name">{{ uniformInfo.name }}</h3>
          <p class="order-info">订单ID: {{ orderInfo.id }}</p>
        </div>
      </div>
    </el-card>

    <el-card v-else-if="loadingProduct" class="loading-card">
      <p v-loading="true" element-loading-text="正在加载商品信息...">商品信息加载中...</p>
    </el-card>

    <el-card v-else class="error-card">
      <p>无法加载商品信息，请返回重试。</p>
    </el-card>

    <el-form
      v-if="uniformInfo.name && !reviewSubmitted"
      ref="reviewForm"
      :model="reviewForm"
      :rules="reviewRules"
      label-width="100px"
      class="review-form"
      v-loading="submitting"
    >
      <el-form-item label="商品评分" prop="rating">
        <el-rate
          v-model="reviewForm.rating"
          :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
          show-text
          :texts="['极差', '失望', '一般', '满意', '惊喜']"
          text-color="#ff9900"
        ></el-rate>
      </el-form-item>

      <el-form-item label="评价内容" prop="content">
        <el-input
          type="textarea"
          v-model="reviewForm.content"
          :rows="5"
          placeholder="分享您的使用心得吧 (最多1000字)"
          maxlength="1000"
          show-word-limit
        ></el-input>
      </el-form-item>

      <el-form-item label="匿名评价">
        <el-switch v-model="reviewForm.isAnonymous"></el-switch>
        <span class="anonymous-tip">
          {{ reviewForm.isAnonymous ? '您的评价将匿名显示' : '您的用户名和头像将会显示' }}
        </span>
      </el-form-item>

      <el-form-item>
        <el-button 
          type="primary" 
          @click="submitReviewForm" 
          :disabled="submitting || uploading"
          :loading="submitting"
        >
          {{ submitting ? '提交中...' : '提交评价' }}
        </el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>

    <div v-if="reviewSubmitted" class="submission-success">
      <i class="el-icon-success success-icon"></i>
      <h2>评价提交成功！</h2>
      <p>感谢您的评价，我们会尽快审核。</p>
      <el-button type="primary" @click="goToMyOrders">返回我的订单</el-button>
      <el-button @click="continueShopping">继续购物</el-button>
    </div>
  </div>
</template>

<script>
import api from "@/api/api";
import { getImageFallback, resolveImageUrl } from "@/utils/image";

export default {
  name: "SubmitReview",
  data() {
    return {
      loadingProduct: true,
      submitting: false,
      uploading: false,
      reviewSubmitted: false,
      uniformInfo: {
        id: null,
        name: '',
        image: '',
      },
      orderInfo: {
        id: null,
        itemId: null,
      },
      reviewForm: {
        rating: 0,
        content: '',
        isAnonymous: false,
      },
      reviewRules: {
        rating: [
          { required: true, message: '请选择商品评分', trigger: 'change' },
          { type: 'number', min: 1, message: '评分至少为1星', trigger: 'change' }
        ],
        content: [
          { max: 1000, message: '评价内容不能超过1000字符', trigger: 'blur' }
        ],
      },
      // 图片上传相关
      fileList: [],
      dialogImageUrl: '',
      dialogVisible: false,
      defaultImage: getImageFallback(),
    };
  },
  created() {
    this.loadReviewData();
  },
  methods: {
    onImageError(event) {
      event.target.src = this.defaultImage;
    },
    
    async loadReviewData() {
      const query = this.$route.query;
      if (query.orderItemId && query.uniformId && query.orderId) {
        this.orderInfo.itemId = query.orderItemId;
        this.orderInfo.id = query.orderId;
        this.uniformInfo.id = query.uniformId;
        this.uniformInfo.name = query.uniformName || '加载中...';
        this.uniformInfo.image = this.formatImageUrl(query.uniformImage);
        this.loadingProduct = false;
      } else {
        this.$message.error('评价参数不完整，无法加载评价页面。');
        this.loadingProduct = false;
        this.$router.go(-1);
      }
    },

    formatImageUrl(imagePath) {
      return resolveImageUrl(imagePath);
    },

    // 图片上传相关方法
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isImage) {
        this.$message.error('只能上传图片格式!');
        return false;
      }
      if (!isLt2M) {
        this.$message.error('上传图片大小不能超过 2MB!');
        return false;
      }
      return true;
    },

    handleRemove(file, fileList) {
      this.fileList = fileList;
    },

    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url;
      this.dialogVisible = true;
    },

    handleExceed(files) {
  this.$message.warning(`当前限制选择 5 个文件，本次选择了 ${files.length} 个文件`);
},

    async handleFileChange(file, fileList) {
      if (!this.beforeUpload(file.raw)) {
        this.fileList = fileList.filter(f => f.uid !== file.uid);
        return false;
      }
      
      this.fileList = fileList;
    },

    async uploadImages() {
      this.uploading = true;
      try {
        return '';
      } finally {
        this.uploading = false;
      }
    },

    // 提交评价
    async submitReviewForm() {
      this.$refs.reviewForm.validate(async (valid) => {
        if (!valid) {
          this.$message.warning('请检查表单输入是否正确');
          return;
        }

        this.submitting = true;
        
        try {
          // 先上传图片
          let imageUrls = '';
          if (this.fileList.length > 0) {
            imageUrls = await this.uploadImages();
          }

          // 提交评价
          const reviewData = {
            uniformId: this.uniformInfo.id,
            orderItemId: this.orderInfo.itemId,
            rating: this.reviewForm.rating,
            content: this.reviewForm.content,
            imageUrls: imageUrls,
            isAnonymous: this.reviewForm.isAnonymous,
          };

          await api.submitReview(reviewData);
          this.$message.success('评价提交成功！');
          this.reviewSubmitted = true;
        } catch (error) {
          console.error("评价提交失败:", error);
          this.$message.error('评价提交失败：' + (error.response?.data?.message || error.message || '请稍后再试'));
        } finally {
          this.submitting = false;
        }
      });
    },

    goBack() {
      this.$router.go(-1);
    },

    goToMyOrders() {
      this.$router.push({ name: 'MyOrders' });
    },

    continueShopping() {
      this.$router.push({ name: 'MallHome' });
    }
  }
};
</script>

<style scoped>
.submit-review-container {
  width: min(800px, calc(100% - 40px));
  padding: 24px 0 48px;
  margin: 0 auto;
  min-height: calc(100vh - var(--navbar-height));
}

.page-title {
  color: var(--text-primary);
  font-size: 26px;
  margin: 0 0 18px;
  letter-spacing: 0;
}

.uniform-info-card {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
}

.uniform-details {
  display: flex;
  align-items: center;
}

.uniform-image-preview {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-md);
  margin-right: 15px;
  border: 1px solid var(--line);
  background: var(--surface-muted);
}

.uniform-text-details .uniform-name {
  font-size: 16px;
  color: var(--text-primary);
  margin-top: 0;
  margin-bottom: 8px;
}

.uniform-text-details .order-info {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 0;
}

.review-form {
  margin-top: 20px;
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.anonymous-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.loading-card, .error-card {
  text-align: center;
  padding: 20px;
  color: var(--text-secondary);
}

.el-upload__tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 5px;
}

.submission-success {
  text-align: center;
  padding: 40px 20px;
}

.success-icon {
  font-size: 60px;
  color: var(--success);
  margin-bottom: 20px;
}

.submission-success h2 {
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.submission-success p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 30px;
}
</style>
