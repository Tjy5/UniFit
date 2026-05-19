<template>
  <div class="style-guide-home mcm-page-shell">
    <section class="header">
      <div>
        <h1>穿搭指南</h1>
        <p>按学校和年级浏览校园着装搭配，查看清晰图片和说明。</p>
      </div>
      <div v-if="!loading.guides && !loading.uniforms" class="filter-container">
        <!-- 学校筛选 -->
        <el-select v-model="selectedSchoolId" placeholder="请选择学校" @change="handleSchoolFilterChangeForGuide" clearable style="margin-right: 10px;">
          <el-option label="全部学校" :value="null"></el-option>
          <el-option
            v-for="school in schoolOptions"
            :key="school.schoolId"
            :label="school.schoolName"
            :value="school.schoolId">
          </el-option>
        </el-select>
        <!-- 年级筛选 -->
        <el-select v-model="selectedGradeId" placeholder="请选择年级" @change="filterGuides" clearable :disabled="!selectedSchoolId && gradeOptions.length === 0">
          <el-option label="全部年级" :value="null"></el-option>
          <el-option
            v-for="grade in gradeOptions"
            :key="grade.gradeId"
            :label="grade.gradeName"
            :value="grade.gradeId">
          </el-option>
        </el-select>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-if="loading.guides || loading.uniforms" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error.guides || error.uniforms" class="error-container">
      <el-empty description="获取数据失败" :image-size="200">
        <el-button type="primary" @click="retryLoading">重新加载</el-button>
      </el-empty>
    </div>

    <!-- 空数据状态 -->
    <div v-else-if="filteredGuides.length === 0" class="empty-container">
      <el-empty description="没有找到符合条件的穿搭指南" :image-size="200"></el-empty>
    </div>

    <!-- 内容状态 -->
    <div v-else class="guide-list">
      <article v-for="guide in filteredGuides" :key="guide.id" class="guide-card">
        <div class="card-container">
          <div class="image-container" @click="showImage(getFirstImage(guide.image))">
            <img :src="getFirstImage(guide.image)" alt="穿搭图片" class="guide-image" @error="onImageError" />
          </div>
          <div class="info-container">
            <h2 @click="goToDetail(guide.id)" class="guide-title">{{ guide.title }}</h2>
            <p class="uniform-info">
              校服: <span @click="goToDetail(guide.id)" class="uniform-name">{{ getUniformName(guide.uniformId) }}</span>
              <!-- 使用 getUniformSchool 和 getUniformGrade 方法 -->
              ({{ getUniformSchool(guide.uniformId) }}, {{ getUniformGrade(guide.uniformId) }})
            </p>
            <p class="intro">{{ stripHtml(guide.content) }}</p>
          </div>
        </div>
      </article>
    </div>

    <!-- 图片放大弹窗 -->
    <div v-if="showImageModal" class="image-modal" @click="closeImage">
      <img :src="enlargedImage" alt="放大图片" class="enlarged-image" />
    </div>
  </div>
</template>

<script>
import api from "../api/api"; // 确保 api.js 中有 getSchoolOptions 和 getGradeOptionsBySchool
import { getImageFallback, resolveImageList } from "@/utils/image";

export default {
  name: "StyleGuideList",
  data() {
    return {
      guides: [],
      uniforms: [], // 这个数组中的校服对象应包含 schoolId, gradeId, schoolName, gradeName
      filteredGuides: [],
      schoolOptions: [], // 新增：存储从API获取的学校选项
      gradeOptions: [],  // 新增：存储从API获取的年级选项
      selectedSchoolId: null, // 新增：存储选择的学校ID
      selectedGradeId: null,  // 新增：存储选择的年级ID
      showImageModal: false,
      enlargedImage: "",
      defaultImage: getImageFallback(),
      loading: {
        guides: false,
        uniforms: false
      },
      error: {
        guides: false,
        uniforms: false
      }
    };
  },
  async mounted() {
    this.loading.uniforms = true;
    this.loading.guides = true;

    await this.loadSchoolFilterOptions(); // 首先加载学校筛选选项

    try {
      await this.fetchUniforms(); // 获取校服数据
    } catch (error) {
      console.error("Failed to load uniforms on mount:", error);
    }

    try {
      await this.fetchGuides(); // 获取穿搭指南数据
    } catch (error) {
      console.error("Failed to load guides on mount:", error);
    }
  },
  methods: {
    async loadSchoolFilterOptions() {
      try {
        const response = await api.getSchoolOptions();
        if (response && response.data) {
          this.schoolOptions = response.data;
        }
      } catch (error) {
        console.error("Error loading school filter options for guides:", error);
        this.$message.error("加载学校筛选数据失败");
      }
    },

    async handleSchoolFilterChangeForGuide(schoolId) {
      this.selectedGradeId = null;
      this.gradeOptions = [];
      if (schoolId) {
        try {
          const response = await api.getGradeOptionsBySchool(schoolId);
          if (response && response.data) {
            this.gradeOptions = response.data;
          }
        } catch (error) {
          console.error("Error loading grade filter options for guides:", error);
          this.$message.error("加载年级筛选数据失败");
        }
      }
      this.filterGuides(); // 学校选择改变后，重新筛选指南
    },

    async fetchGuides() {
      this.loading.guides = true;
      this.error.guides = false;
      try {
        const response = await api.get("/api/s-style-guides/active");
        this.guides = response.data || [];
        this.filterGuides(); // 获取指南后应用当前筛选
      } catch (error) {
        console.error("Error fetching style guides:", error);
        this.$message.error("获取穿搭指南失败");
        this.guides = [];
        this.error.guides = true;
      } finally {
        this.loading.guides = false;
        // 确保在两个都加载完毕后再执行一次筛选，以防时序问题
        if (!this.loading.uniforms) {
            this.filterGuides();
        }
      }
    },

    async fetchUniforms() {
      this.loading.uniforms = true;
      this.error.uniforms = false;
      try {
        // 假设这个API返回的校服对象已包含 schoolId, gradeId, schoolName, gradeName
        const response = await api.get("/api/s-uniform/active");
        this.uniforms = response.data || [];
      } catch (error) {
        console.error("Error fetching uniforms:", error);
        this.$message.error("获取校服数据失败");
        this.uniforms = [];
        this.error.uniforms = true;
      } finally {
        this.loading.uniforms = false;
        // 确保在两个都加载完毕后再执行一次筛选
        if (!this.loading.guides) {
            this.filterGuides();
        }
      }
    },

    // extractSchoolsAndGrades() { // 这个方法不再需要，因为筛选选项从API获取
    // },

    filterGuides() {
      if (!this.guides || !this.uniforms) {
        this.filteredGuides = [];
        return;
      }

      this.filteredGuides = this.guides.filter((guide) => {
        if (!guide || !guide.uniformId) return false;

        const uniform = this.uniforms.find(
          (u) => u && u.id && u.id.toString() === guide.uniformId.toString()
        );

        if (!uniform) return false;

        // 使用校服对象中的 schoolId 和 gradeId 进行比较
        const matchSchool = !this.selectedSchoolId || (uniform.schoolId && uniform.schoolId.toString() === this.selectedSchoolId.toString());
        const matchGrade = !this.selectedGradeId || (uniform.gradeId && uniform.gradeId.toString() === this.selectedGradeId.toString());

        return matchSchool && matchGrade;
      });
    },

    getUniformName(uniformId) {
      if (!uniformId || !this.uniforms || !this.uniforms.length) return "未知校服";
      const uniform = this.uniforms.find(
        (u) => u && u.id && u.id.toString() === uniformId.toString()
      );
      return uniform && uniform.name ? uniform.name : "未知校服";
    },

    getUniformSchool(uniformId) {
      if (!uniformId || !this.uniforms || !this.uniforms.length) return "未知学校";
      const uniform = this.uniforms.find(
        (u) => u && u.id && u.id.toString() === uniformId.toString()
      );
      return uniform && uniform.schoolName ? uniform.schoolName : "未知学校"; // 使用 schoolName
    },

    getUniformGrade(uniformId) {
      if (!uniformId || !this.uniforms || !this.uniforms.length) return "未知年级";
      const uniform = this.uniforms.find(
        (u) => u && u.id && u.id.toString() === uniformId.toString()
      );
      return uniform && uniform.gradeName ? uniform.gradeName : "未知年级"; // 使用 gradeName
    },

    getFirstImage(image) {
      const images = resolveImageList(image);
      return images[0] || this.defaultImage;
    },

    stripHtml(content) {
      if (!content) return "";
      try {
        const stripped = content.replace(/<[^>]+>/g, "");
        return stripped.length > 100 ? stripped.substring(0, 100) + "..." : stripped;
      } catch (e) {
        console.error("Error stripping HTML:", e);
        return "";
      }
    },

    showImage(image) {
      if (!image) return;
      this.enlargedImage = image;
      this.showImageModal = true;
    },

    closeImage() {
      this.showImageModal = false;
    },
    onImageError(event) {
      event.target.src = this.defaultImage;
    },

    goToDetail(id) {
      if (!id) {
        this.$message.error("无效的穿搭指南ID");
        return;
      }
      this.$router.push({ path: `/style-guide/detail/${id}` });
    },

    retryLoading() {
      // 重置加载和错误状态
      this.loading.guides = false;
      this.loading.uniforms = false;
      this.error.guides = false;
      this.error.uniforms = false;

      this.loadSchoolFilterOptions(); // 重新加载学校选项
      this.fetchUniforms();
      this.fetchGuides();
    }
  }
};
</script>

<style scoped>
.style-guide-home {
  min-height: calc(100vh - var(--navbar-height));
  padding: 24px 0 56px;
  box-sizing: border-box;
}

.header {
  width: 100%;
  display: grid;
  gap: 18px;
  margin: 0 0 28px;
  padding: 28px 28px 24px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  position: relative;
  overflow: hidden;
}

.header::before {
  display: none;
}

.header h1 {
  font-size: 28px;
  color: var(--text-primary);
  letter-spacing: 0;
  font-weight: bold;
  margin: 0 0 8px;
  position: relative;
  z-index: 1;
}

.header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.filter-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  background: var(--surface-muted);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  position: relative;
  z-index: 1;
}

.el-select {
  min-width: 160px;
}

.guide-list {
  display: grid;
  gap: 26px;
  margin-top: 20px;
}

.loading-container, .error-container, .empty-container {
  width: 100%;
  margin: 40px 0;
  padding: 30px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  text-align: center;
}

.guide-card {
  display: block;
}

.card-container {
  width: 100%;
  display: flex;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  height: 250px;
  overflow: hidden;
  transition: box-shadow 0.16s ease, border-color 0.16s ease;
  border: 1px solid var(--line);
}

.card-container:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--brand);
}

.image-container {
  flex: 0 0 300px;
  height: 250px;
  cursor: pointer;
  border-right: 1px solid var(--line);
  background: var(--surface-muted);
  position: relative;
  overflow: hidden;
}

.guide-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-container {
  flex: 1;
  padding: 22px 25px 15px 25px;
  text-align: left;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.guide-title {
  font-size: 1.32rem;
  margin-bottom: 13px;
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 0;
  transition: color 0.18s;
}

.guide-title:hover {
  color: var(--brand);
  text-decoration: underline;
}

.uniform-info {
  font-size: 1.01rem;
  color: var(--text-secondary);
  margin-bottom: 10px;
  font-weight: 400;
}

.uniform-name {
  color: var(--brand);
  cursor: pointer;
  font-weight: 500;
  transition: color 0.18s;
}

.uniform-name:hover {
  color: var(--brand-hover);
  text-decoration: underline;
}

.intro {
  font-size: 0.98rem;
  color: var(--text-secondary);
  margin-bottom: 10px;
  line-height: 1.7;
  letter-spacing: 0;
  background: var(--surface-muted);
  border-radius: var(--radius-md);
  padding: 9px 14px 7px 14px;
  text-align: justify;
}

.image-modal {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background: rgba(15, 23, 42, 0.82);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  cursor: zoom-out;
  animation: fadeInModal 0.2s;
}

.enlarged-image {
  max-width: 85vw;
  max-height: 85vh;
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  animation: popInImg 0.13s;
}

@media (max-width: 768px) {
  .card-container {
    flex-direction: column;
    height: auto;
    max-width: 500px;
  }

  .image-container {
    width: 100%;
    height: 200px;
    flex: none;
  }

  .header {
    padding: 24px 20px;
    text-align: center;
  }

  .guide-list {
    margin-top: 16px;
  }

  .filter-container {
    justify-content: center;
  }

  .el-select {
    margin: 5px;
  }
}

@keyframes fadeInModal {
  from { opacity: 0;}
  to { opacity: 1;}
}

@keyframes popInImg {
  from { transform: scale(0.97);}
  to { transform: scale(1);}
}
</style>
