<template>
  <div class="style-guide-detail">
    <div class="guide-card">
      <h1>
        <i class="el-icon-s-cooperation" style="color: #409eff; margin-right: 8px;"></i>
        {{ guide.title }}
      </h1>
      <div class="uniform-info">
        <i class="el-icon-school" style="color: #67C23A; margin-right: 5px;"></i>
        校服：{{ uniformDetails.name || '未知校服' }}
        <span class="school-grade">
          （{{ uniformDetails.schoolName || '未知学校' }}，{{ uniformDetails.gradeName || '未知年级' }}）
        </span>
      </div>
      <div class="image-gallery">
        <div
          v-for="(img, index) in splitImages(guide.image)"
          :key="index"
          class="img-wrap"
          @click="showImage(getImageUrl(img))"
        >
          <img
            :src="img"
            alt="穿搭图片"
            class="detail-image"
            @error="onImageError"
          />
          <div class="img-mask">
            <i class="el-icon-zoom-in"></i>
          </div>
        </div>
      </div>
      <el-card class="content-card" shadow="never">
        <div class="content" v-html="guide.content"></div>
      </el-card>
    </div>
    <!-- 图片放大弹窗 -->
    <div v-if="showImageModal" class="image-modal" @click="closeImage">
      <img :src="enlargedImage" alt="放大图片" class="enlarged-image" />
    </div>
  </div>
</template>

<script>
import api from "../api/api";
import { getImageFallback, resolveImageList, resolveImageUrl } from "@/utils/image";

export default {
  name: "StyleGuideDetail",
  data() {
    return {
      guide: {},
      uniforms: [], // 这里的校服对象应该包含 schoolName 和 gradeName
      uniformDetails: {
        name: '',
        schoolName: '', // 初始化期望的字段
        gradeName: ''   // 初始化期望的字段
      },
      showImageModal: false,
      enlargedImage: "",
      defaultImage: getImageFallback(),
    };
  },
  async mounted() { // 改为 async 以便使用 await
    // 先获取校服数据，因为 matchUniformDetails 依赖它
    await this.fetchUniforms();
    // 再获取指南详情
    await this.fetchGuideDetail();
  },
  methods: {
    async fetchGuideDetail() {
      const id = this.$route.params.id;
      if (!id) {
        this.$message.error("无效的穿搭指南ID");
        return;
      }
      try {
        const response = await api.get(`/api/s-style-guides/${id}`);
        this.guide = response.data || {};
        this.matchUniformDetails(); // 在指南详情和校服都加载后进行匹配
      } catch (error) {
        console.error("Error fetching style guide detail:", error);
        this.$message.error("获取穿搭指南详情失败");
      }
    },
    async fetchUniforms() {
      try {
        // 这个接口现在应该返回包含 schoolName 和 gradeName 的校服列表
        const response = await api.get("/api/s-uniform/active");
        this.uniforms = response.data || []; // 确保是数组
        this.matchUniformDetails(); // 在指南详情和校服都加载后进行匹配
      } catch (error) {
        console.error("Error fetching uniforms:", error);
        this.$message.error("获取校服数据失败");
      }
    },
    matchUniformDetails() {
      // 确保 guide 和 uniforms 都有数据再进行匹配
      if (this.guide && this.guide.uniformId && this.uniforms && this.uniforms.length) {
        const foundUniform = this.uniforms.find(
          (u) => u && u.id != null && u.id.toString() === this.guide.uniformId.toString()
        );
        this.uniformDetails = foundUniform || { name: '关联校服未找到', schoolName: '', gradeName: '' };
      }
    },
    splitImages(image) {
      const images = resolveImageList(image);
      return images.length ? images : [this.defaultImage];
    },
    getImageUrl(imagePath) {
      return resolveImageUrl(imagePath);
    },
    showImage(imageUrl) {
      if (!imageUrl) return;
      this.enlargedImage = imageUrl;
      this.showImageModal = true;
    },
    closeImage() {
      this.showImageModal = false;
    },
    onImageError(event) {
      event.target.src = this.defaultImage;
    },
  },
};
</script>

<style scoped>
.style-guide-detail {
  width: min(900px, calc(100% - 40px));
  margin: 0 auto;
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 34px 0 80px;
}
.guide-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  max-width: 900px;
  width: 100%;
  padding: 38px 38px 28px 38px;
  border: 1px solid var(--line);
  position: relative;
  overflow: hidden;
}

.guide-card::before {
  display: none;
}

h1 {
  font-size: 2rem;
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 20px;
  letter-spacing: 0;
  text-align: left;
  position: relative;
  z-index: 1;
}
.uniform-info {
  font-size: 1.08rem;
  color: var(--text-secondary);
  margin-bottom: 18px;
  font-weight: 500;
  letter-spacing: 0;
  position: relative;
  z-index: 1;
}
.school-grade {
  color: var(--text-muted);
  font-weight: normal;
  font-size: 0.98em;
  margin-left: 6px;
}
.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}
.img-wrap {
  position: relative;
  width: 180px;
  height: 180px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  background: var(--surface-muted);
  transition: box-shadow 0.2s;
}
.img-wrap:hover {
  box-shadow: var(--shadow-md);
  z-index: 2;
}
.detail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.22s cubic-bezier(.33,2,.6,1);
}
.img-wrap:hover .detail-image {
  transform: scale(1.09);
}
.img-mask {
  position: absolute;
  left: 0; top: 0; width: 100%; height: 100%;
  background: rgba(15, 23, 42, 0.26);
  opacity: 0;
  display: flex; align-items: center; justify-content: center;
  transition: opacity 0.2s;
  font-size: 36px;
  color: #fff;
  pointer-events: none;
}
.img-wrap:hover .img-mask {
  opacity: 1;
}
.content-card {
  background: var(--surface-muted);
  border-radius: var(--radius-lg);
  margin-top: 10px;
  border: 1px solid var(--line);
  box-shadow: none;
  position: relative;
  z-index: 1;
}
.content {
  font-size: 1.04rem;
  color: var(--text-primary);
  line-height: 1.85;
  padding: 6px 2px 3px 2px;
}
.image-modal {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(15, 23, 42, 0.82);
  display: flex; justify-content: center; align-items: center;
  z-index: 9999;
  cursor: zoom-out;
  animation: fadeInModal 0.2s;
}
@keyframes fadeInModal {
  from { opacity: 0;}
  to { opacity: 1;}
}
.enlarged-image {
  max-width: 85vw;
  max-height: 85vh;
  border-radius: 12px;
  box-shadow: 0 8px 48px #2228;
  animation: popInImg 0.15s;
}
@keyframes popInImg {
  from { transform: scale(0.97);}
  to { transform: scale(1);}
}
</style>
