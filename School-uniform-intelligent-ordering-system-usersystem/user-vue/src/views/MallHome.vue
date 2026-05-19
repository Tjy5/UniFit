<template>
  <div class="mall-home mcm-page-shell">
    <el-dialog
      title="请完善个人信息以获取尺码推荐"
      v-model:visible="infoDialogVisible"
      :close-on-click-modal="false"
      :show-close="true"
      @close="handleInfoDialogClose"
      width="480px"
    >
      <el-form :model="userInfoForm" :rules="userInfoRules" ref="userInfoFormRefDialog" label-width="100px">
        <el-form-item label="身高 (cm)" prop="height">
          <el-input-number v-model="userInfoForm.height" controls-position="right" :min="50" :max="250" :precision="1" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="体重 (kg)" prop="weight">
          <el-input-number v-model="userInfoForm.weight" controls-position="right" :min="10" :max="200" :precision="1" style="width: 100%;"></el-input-number>
        </el-form-item>
      </el-form>

      <size-recommendation-panel
        :result="recommendationResult"
        :loading="recommendationLoading"
        :compact="true"
      />

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelSubmitUserInfo">取 消</el-button>
          <el-button type="primary" :loading="savingUserInfo" @click="submitUserInfoAndRecommend">保存并获取推荐</el-button>
        </span>
      </template>
    </el-dialog>

    <section v-if="!infoDialogVisible" class="mall-toolbar">
      <div class="mall-toolbar__head">
        <div>
          <h1>查找并预订校服</h1>
          <p>按学校、年级和关键词筛选商品，下单前可查看当前账号的尺码推荐。</p>
        </div>
        <div class="mall-toolbar__count">
          <span>当前结果</span>
          <strong>{{ filteredUniforms.length }}</strong>
        </div>
      </div>

      <div class="mall-filters">
        <el-input
          v-model="searchQuery"
          class="mall-filters__search"
          placeholder="搜索校服名称"
          clearable
          @clear="filterUniforms"
          @keyup.enter="filterUniforms"
        />
        <el-select v-model="selectedSchoolId" placeholder="选择学校" clearable @change="handleSchoolFilterChange">
          <el-option label="全部学校" :value="null" />
          <el-option
            v-for="school in schoolOptions"
            :key="school.schoolId"
            :label="school.schoolName"
            :value="school.schoolId"
          />
        </el-select>
        <el-select
          v-model="selectedGradeId"
          placeholder="选择年级"
          clearable
          :disabled="gradeOptions.length === 0 && !selectedSchoolId"
          @change="filterBySchoolAndGrade"
        >
          <el-option label="全部年级" :value="null" />
          <el-option
            v-for="grade in gradeOptions"
            :key="grade.gradeId"
            :label="grade.gradeName"
            :value="grade.gradeId"
          />
        </el-select>
        <el-select v-model="sortMode" placeholder="排序" @change="filterBySchoolAndGrade">
          <el-option label="默认排序" value="default" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="评分优先" value="ratingDesc" />
        </el-select>
        <el-button type="primary" @click="filterUniforms">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <div v-if="activeFilters.length" class="applied-filters">
        <span>已选条件</span>
        <button v-for="filter in activeFilters" :key="filter.key" type="button" @click="clearFilter(filter.key)">
          {{ filter.label }} ×
        </button>
      </div>
    </section>

    <section v-if="!infoDialogVisible" class="product-section">
      <div class="product-section__head">
        <h2>校服商品</h2>
        <p>{{ loading ? '正在加载商品数据' : `共 ${filteredUniforms.length} 件商品` }}</p>
      </div>

      <div v-if="loading" class="loading-indicator">加载中...</div>
      <div v-else-if="!filteredUniforms.length" class="no-results">暂无符合条件的校服，请调整筛选条件。</div>
      <div v-else class="product-grid">
        <article v-for="uniform in filteredUniforms" :key="uniform.id" class="product-card">
          <div class="product-image-frame">
            <button type="button" class="product-image-wrapper" @click="goToProductDetail(uniform.id)">
              <img
                :src="getUniformImages(uniform.image)[0]"
                :alt="uniform.name || '校服图片'"
                class="product-image"
                @error="onImageError"
              />
              <span v-if="getUniformImages(uniform.image).length > 1" class="product-image-count">
                {{ getUniformImages(uniform.image).length }} 张
              </span>
            </button>
            <button
              type="button"
              class="product-image-preview"
              @click.stop="openImage(getUniformImages(uniform.image)[0])"
            >
              查看大图
            </button>
          </div>
          <div class="product-card__body">
            <div class="product-card__title-row">
              <button type="button" class="product-name-button" @click="goToProductDetail(uniform.id)">
                {{ uniform.name }}
              </button>
              <button
                type="button"
                class="wishlist-button"
                :class="{ 'is-active': isWishlisted(uniform.id) }"
                :disabled="uniform.wishlistLoading"
                @click.stop="toggleWishlist(uniform)"
              >
                {{ isWishlisted(uniform.id) ? '已收藏' : '收藏' }}
              </button>
            </div>
            <p class="product-intro">{{ getShortIntro(uniform.intro) }}</p>
            <div class="product-meta">
              <span>{{ uniform.schoolName || '未配置学校' }}</span>
              <span>{{ uniform.gradeName || '未配置年级' }}</span>
              <span v-if="finalRecommendedSize">推荐尺码：{{ finalRecommendedSize }}</span>
            </div>
            <div class="product-card__footer">
              <strong class="product-price">¥{{ formatPrice(uniform.price) }}</strong>
              <div class="product-card__actions">
                <el-button plain @click="goToProductDetail(uniform.id)">查看详情</el-button>
                <el-button type="primary" @click="showSizeSelectionDialog(uniform)">快速选购</el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <el-dialog title="选择尺码" v-model:visible="sizeSelectionDialogVisible" width="460px">
      <size-recommendation-panel
        :result="recommendationResult"
        :loading="recommendationLoading"
        :compact="true"
      />

      <el-form :model="sizeSelectionForm" :rules="sizeSelectionRules" ref="sizeSelectionFormRef" label-width="100px">
        <el-form-item label="尺码" prop="sizeId">
          <el-select v-model="sizeSelectionForm.sizeId" placeholder="请选择尺码" style="width:100%;">
            <el-option
              v-for="size in availableSizesForOrder"
              :key="size.id || size.sizeName"
              :label="size.sizeName + (size.sizeName === finalRecommendedSize ? ' (推荐)' : '')"
              :value="size.id || size.sizeName"
              :class="{ 'recommended-size-option': size.sizeName === finalRecommendedSize }"
            ></el-option>
          </el-select>
          <div v-if="finalRecommendedSize" class="size-tip">
            当前推荐尺码：{{ finalRecommendedSize }}
          </div>
          <div v-else-if="recommendationResult.message" class="size-tip">
            {{ recommendationResult.message }}
          </div>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="sizeSelectionForm.quantity" :min="1" :max="10" controls-position="right"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="sizeSelectionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addToCart">确认添加</el-button>
        </span>
      </template>
    </el-dialog>

    <div v-if="isImageOpen" class="image-modal" @click.self="closeImage">
      <img :src="largeImageUrl" alt="校服大图" class="large-image" />
      <button class="close-btn" @click.stop="closeImage">×</button>
    </div>
  </div>
</template>

<script>
import api from "../api/api";
import SizeRecommendationPanel from "@/components/size/SizeRecommendationPanel.vue";
import { getImageFallback, resolveImageList } from "@/utils/image";
import {
  buildRecommendationPayload,
  createEmptyRecommendation,
  getRecommendedSizeName,
  normalizeRecommendationResult,
} from "@/composables/useSizeRecommendation";
import { RECOMMENDATION_SOURCES } from "@/constants/recommendationSources";

export default {
  name: "MallHome",
  components: {
    SizeRecommendationPanel,
  },
  data() {
    return {
      uniforms: [],
      filteredUniforms: [],
      schoolOptions: [],
      gradeOptions: [],
      allSizesFromDB: [],
      availableSizesForOrder: [],
      userWishlist: [],
      selectedSchoolId: null,
      selectedGradeId: null,
      sortMode: "default",
      searchQuery: "",
      currentUniform: null,
      loading: false,
      isImageOpen: false,
      largeImageUrl: "",
      defaultImage: getImageFallback(),
      infoDialogVisible: false,
      savingUserInfo: false,
      userInfoForm: {
        messageUserAge: null,
        messageUserSex: "",
        height: null,
        weight: null,
        chest: null,
        waist: null,
        hip: null,
        shoulder: null,
      },
      savedUserMessage: {},
      userInfoRules: {
        height: [
          { required: true, message: '请输入身高', trigger: 'blur' },
          { type: 'number', message: '身高必须为数字值' },
          { validator: (rule, value, callback) => {
            if (value && (value < 50 || value > 250)) {
              callback(new Error('身高范围应在50-250cm之间'));
            } else {
              callback();
            }
          }, trigger: 'blur' },
        ],
        weight: [
          { required: true, message: '请输入体重', trigger: 'blur' },
          { type: 'number', message: '体重必须为数字值' },
          { validator: (rule, value, callback) => {
            if (value && (value < 10 || value > 200)) {
              callback(new Error('体重范围应在10-200kg之间'));
            } else {
              callback();
            }
          }, trigger: 'blur' },
        ],
      },
      sizeSelectionDialogVisible: false,
      sizeSelectionForm: {
        sizeId: "",
        quantity: 1,
      },
      sizeSelectionRules: {
        sizeId: [{ required: true, message: '请选择尺码', trigger: 'change' }],
        quantity: [{ required: true, message: '请选择数量', trigger: 'change' }],
      },
      recommendationLoading: false,
      recommendationResult: createEmptyRecommendation(),
      finalRecommendedSize: "",
      recommendationTimer: null,
    };
  },
  async created() {
    await Promise.all([
      this.loadFilterOptions(),
      this.fetchSizes(),
      this.fetchUserWishlist(),
    ]);
    await this.checkUserInfo();
  },
  beforeUnmount() {
    if (this.recommendationTimer) {
      clearTimeout(this.recommendationTimer);
    }
  },
  computed: {
    selectedSchoolName() {
      return this.schoolOptions.find((school) => String(school.schoolId) === String(this.selectedSchoolId))?.schoolName || "";
    },
    selectedGradeName() {
      return this.gradeOptions.find((grade) => String(grade.gradeId) === String(this.selectedGradeId))?.gradeName || "";
    },
    activeFilters() {
      const filters = [];
      if (this.searchQuery.trim()) {
        filters.push({ key: "search", label: `关键词：${this.searchQuery.trim()}` });
      }
      if (this.selectedSchoolId) {
        filters.push({ key: "school", label: `学校：${this.selectedSchoolName}` });
      }
      if (this.selectedGradeId) {
        filters.push({ key: "grade", label: `年级：${this.selectedGradeName}` });
      }
      if (this.sortMode !== "default") {
        const sortLabels = {
          priceAsc: "价格从低到高",
          priceDesc: "价格从高到低",
          ratingDesc: "评分优先",
        };
        filters.push({ key: "sort", label: `排序：${sortLabels[this.sortMode]}` });
      }
      return filters;
    },
  },
  methods: {
    async loadFilterOptions() {
      try {
        const schoolResponse = await api.getSchoolOptions();
        this.schoolOptions = Array.isArray(schoolResponse.data) ? schoolResponse.data : [];
      } catch (error) {
        console.error("Error loading school options:", error);
        this.schoolOptions = [];
      }
      this.gradeOptions = [];
    },
    async handleSchoolFilterChange(schoolId) {
      this.selectedGradeId = null;
      this.gradeOptions = [];
      if (schoolId) {
        try {
          const gradeResponse = await api.getGradeOptionsBySchool(schoolId);
          this.gradeOptions = Array.isArray(gradeResponse.data) ? gradeResponse.data : [];
        } catch (error) {
          console.error("Error loading grade options:", error);
          this.gradeOptions = [];
        }
      }
      this.filterBySchoolAndGrade();
    },
    async fetchUniforms() {
      this.loading = true;
      try {
        const response = await api.get("/api/s-uniform/active");
        this.uniforms = (response.data || []).map((item) => ({ ...item, wishlistLoading: false }));
        this.filterBySchoolAndGrade();
      } catch (error) {
        console.error("Error fetching uniforms:", error);
        this.$message.error("获取校服数据失败，请重试");
        this.uniforms = [];
        this.filteredUniforms = [];
      } finally {
        this.loading = false;
      }
    },
    async fetchSizes() {
      try {
        const response = await api.getAllSizes();
        this.allSizesFromDB = response.data || [];
        this.availableSizesForOrder = this.allSizesFromDB.map((size) => ({
          id: size.id,
          sizeName: String(size.sizeName).trim(),
        }));
      } catch (error) {
        console.error("Error fetching sizes:", error);
        this.$message.error("获取尺码数据失败，请重试");
        this.allSizesFromDB = [];
        this.availableSizesForOrder = [];
      }
    },
    applyUserMessage(userData = {}) {
      this.savedUserMessage = { ...userData };
      this.userInfoForm.messageUserAge = userData.messageUserAge ?? null;
      this.userInfoForm.messageUserSex = userData.messageUserSex || "";
      this.userInfoForm.height = userData.height ?? null;
      this.userInfoForm.weight = userData.weight ?? null;
      this.userInfoForm.chest = userData.chest ?? null;
      this.userInfoForm.waist = userData.waist ?? null;
      this.userInfoForm.hip = userData.hip ?? null;
      this.userInfoForm.shoulder = userData.shoulder ?? null;
    },
    async checkUserInfo() {
      const userId = localStorage.getItem("userId");
      if (!userId) {
        this.$message.error("请先登录");
        this.$router.push("/login");
        return;
      }
      try {
        const response = await api.getUserMessage(userId);
        this.applyUserMessage(response.data || {});
        if (!this.userInfoForm.height || !this.userInfoForm.weight) {
          this.infoDialogVisible = true;
          this.clearAllRecommendations("请先完善身高和体重信息");
        } else {
          this.infoDialogVisible = false;
          await this.refreshRecommendation(RECOMMENDATION_SOURCES.MALL_HOME_DIALOG);
          await this.fetchUniforms();
        }
      } catch (error) {
        if (error.response?.status !== 404) {
          console.error("Error fetching user info:", error);
        }
        this.infoDialogVisible = true;
        this.clearAllRecommendations("请先完善身高和体重信息");
      }
    },
    handleInfoDialogClose() {
      if (!this.userInfoForm.height || !this.userInfoForm.weight) {
        this.$message.warn("请完善身高体重信息以获取尺码推荐和浏览商品。");
      }
      this.fetchInitialDataIfNeeded();
    },
    cancelSubmitUserInfo() {
      this.infoDialogVisible = false;
      if (!this.userInfoForm.height || !this.userInfoForm.weight) {
        this.$message.info("您未保存身高体重信息，将无法获得尺码推荐。");
      }
      this.fetchInitialDataIfNeeded();
    },
    async submitUserInfoAndRecommend() {
      this.$refs.userInfoFormRefDialog.validate(async (valid) => {
        if (!valid) {
          this.clearAllRecommendations();
          return false;
        }
        this.savingUserInfo = true;
        const userId = localStorage.getItem("userId");
        try {
          const payload = {
            ...this.savedUserMessage,
            messageUserId: Number(userId),
            height: this.userInfoForm.height,
            weight: this.userInfoForm.weight,
            chest: this.userInfoForm.chest,
            waist: this.userInfoForm.waist,
            hip: this.userInfoForm.hip,
            shoulder: this.userInfoForm.shoulder,
            updateBy: userId,
          };
          await api.updateUserMessage(payload);
          await this.refreshRecommendation(RECOMMENDATION_SOURCES.MALL_HOME_DIALOG);
          this.$message.success("个人信息已更新");
          this.infoDialogVisible = false;
          await this.fetchInitialDataIfNeeded();
        } catch (error) {
          console.error("Error updating user info:", error);
          this.$message.error("更新个人信息失败: " + (error.response?.data?.message || error.response?.data || error.message));
        } finally {
          this.savingUserInfo = false;
        }
      });
    },
    async refreshRecommendation(source, uniformId = undefined) {
      if (!this.userInfoForm.height || !this.userInfoForm.weight) {
        this.clearAllRecommendations("请先完善身高和体重信息");
        return;
      }
      this.recommendationLoading = true;
      try {
        const response = await api.getSizeRecommendation(buildRecommendationPayload(this.userInfoForm, source, uniformId));
        this.recommendationResult = normalizeRecommendationResult(response.data);
        this.finalRecommendedSize = getRecommendedSizeName(this.recommendationResult);
        if (this.sizeSelectionDialogVisible && this.finalRecommendedSize) {
          const recommendedSizeObject = this.availableSizesForOrder.find(
            (size) => size.sizeName === this.finalRecommendedSize
          );
          if (recommendedSizeObject) {
            this.sizeSelectionForm.sizeId = recommendedSizeObject.id || recommendedSizeObject.sizeName;
          }
        }
      } catch (error) {
        console.error("Error fetching recommendation:", error);
        this.clearAllRecommendations("暂时无法获取尺码推荐，请稍后重试。");
      } finally {
        this.recommendationLoading = false;
      }
    },
    scheduleRecommendationRefresh() {
      if (this.recommendationTimer) {
        clearTimeout(this.recommendationTimer);
      }
      if (!this.userInfoForm.height || !this.userInfoForm.weight) {
        this.clearAllRecommendations();
        return;
      }
      this.recommendationTimer = setTimeout(() => {
        this.refreshRecommendation(RECOMMENDATION_SOURCES.MALL_HOME_DIALOG);
      }, 250);
    },
    async fetchInitialDataIfNeeded() {
      if (!this.infoDialogVisible && this.uniforms.length === 0) {
        await this.fetchUniforms();
      }
    },
    clearAllRecommendations(message = '') {
      this.recommendationResult = createEmptyRecommendation(message);
      this.finalRecommendedSize = "";
    },
    async showSizeSelectionDialog(uniform) {
      this.currentUniform = uniform;
      this.clearAllRecommendations("正在获取当前商品推荐...");
      if (this.$refs.sizeSelectionFormRef) {
        this.$refs.sizeSelectionFormRef.resetFields();
      }
      this.sizeSelectionForm.quantity = 1;
      const recommendedSizeObject = this.availableSizesForOrder.find(
        (size) => size.sizeName === this.finalRecommendedSize
      );
      if (recommendedSizeObject) {
        this.sizeSelectionForm.sizeId = recommendedSizeObject.id || recommendedSizeObject.sizeName;
      } else if (this.availableSizesForOrder.length > 0) {
        this.sizeSelectionForm.sizeId = this.availableSizesForOrder[0].id || this.availableSizesForOrder[0].sizeName;
      } else {
        this.sizeSelectionForm.sizeId = "";
      }
      this.sizeSelectionDialogVisible = true;
      if (uniform?.id && this.userInfoForm.height && this.userInfoForm.weight) {
        await this.refreshRecommendation(RECOMMENDATION_SOURCES.MALL_HOME_DIALOG, uniform.id);
      }
    },
    goToProductDetail(uniformId) {
      if (!uniformId) {
        return;
      }
      this.$router.push({ name: 'ProductDetail', params: { uniformId } });
    },
    async addToCart() {
      this.$refs.sizeSelectionFormRef.validate(async (valid) => {
        if (!valid) {
          return false;
        }
        try {
          const uniformId = this.currentUniform.id;
          const selectedSize = this.availableSizesForOrder.find((size) => (size.id || size.sizeName) === this.sizeSelectionForm.sizeId);
          const sizeIdToSend = selectedSize ? selectedSize.id : this.sizeSelectionForm.sizeId;
          const quantity = this.sizeSelectionForm.quantity;
          await api.addCartItem({
            uniformId,
            sizeId: sizeIdToSend,
            quantity,
            recommendationLogId: this.recommendationResult?.recommendationLogId || undefined,
          });
          this.sizeSelectionDialogVisible = false;
          this.$message.success('商品已成功添加到购物车');
        } catch (error) {
          console.error('Error adding item to cart:', error);
          this.$message.error(error.response?.data?.message || error.message || '添加到购物车失败，请重试');
        }
      });
    },
    isWishlisted(uniformId) {
      return this.userWishlist.includes(uniformId);
    },
    async fetchUserWishlist() {
      const userId = localStorage.getItem("userId");
      if (!userId) return;
      try {
        const response = await api.getUserWishlist();
        this.userWishlist = Array.isArray(response.data) ? response.data.map((item) => item.uniformId) : [];
      } catch (error) {
        console.error("Error fetching user wishlist:", error);
        this.userWishlist = [];
      }
    },
    async toggleWishlist(uniform) {
      const userId = localStorage.getItem("userId");
      if (!userId) {
        this.$message.error("请先登录后再操作");
        this.$router.push("/login");
        return;
      }
      uniform.wishlistLoading = true;
      try {
        if (this.isWishlisted(uniform.id)) {
          const response = await api.removeFromWishlist(uniform.id);
          if (response.status === 204) {
            this.userWishlist = this.userWishlist.filter((id) => id !== uniform.id);
            this.$message.success("已取消收藏");
          }
        } else {
          const response = await api.addToWishlist(uniform.id);
          if (response.status === 201) {
            this.userWishlist.push(uniform.id);
            this.$message.success(response.data || "已加入收藏");
          }
        }
      } catch (error) {
        console.error("Error toggling wishlist:", error);
        this.$message.error(error.response?.data || error.message || "操作失败，请检查网络连接");
      } finally {
        uniform.wishlistLoading = false;
      }
    },
    filterBySchoolAndGrade() {
      let tempFiltered = this.uniforms;
      if (this.selectedSchoolId) {
        tempFiltered = tempFiltered.filter((uniform) => String(uniform.schoolId) === String(this.selectedSchoolId));
      }
      if (this.selectedGradeId) {
        tempFiltered = tempFiltered.filter((uniform) => String(uniform.gradeId) === String(this.selectedGradeId));
      }
      const query = this.searchQuery.trim().toLowerCase();
      if (query) {
        tempFiltered = tempFiltered.filter((uniform) =>
          uniform.name && uniform.name.toLowerCase().includes(query)
        );
      }
      const sorted = [...tempFiltered];
      if (this.sortMode === "priceAsc") {
        sorted.sort((a, b) => Number(a.price || 0) - Number(b.price || 0));
      } else if (this.sortMode === "priceDesc") {
        sorted.sort((a, b) => Number(b.price || 0) - Number(a.price || 0));
      } else if (this.sortMode === "ratingDesc") {
        sorted.sort((a, b) => Number(b.averageRating || 0) - Number(a.averageRating || 0));
      }
      this.filteredUniforms = sorted;
    },
    filterUniforms() {
      this.filterBySchoolAndGrade();
    },
    resetFilters() {
      this.searchQuery = "";
      this.selectedSchoolId = null;
      this.selectedGradeId = null;
      this.gradeOptions = [];
      this.sortMode = "default";
      this.filterBySchoolAndGrade();
    },
    clearFilter(key) {
      if (key === "search") {
        this.searchQuery = "";
      } else if (key === "school") {
        this.selectedSchoolId = null;
        this.selectedGradeId = null;
        this.gradeOptions = [];
      } else if (key === "grade") {
        this.selectedGradeId = null;
      } else if (key === "sort") {
        this.sortMode = "default";
      }
      this.filterBySchoolAndGrade();
    },
    getUniformImages(image) {
      const images = resolveImageList(image);
      return images.length ? images : [this.defaultImage];
    },
    getShortIntro(intro) {
      if (!intro) return "暂无介绍";
      const maxLength = 72;
      return intro.length > maxLength ? intro.slice(0, maxLength) + "..." : intro;
    },
    formatPrice(price) {
      const value = Number(price || 0);
      return Number.isFinite(value) ? value.toFixed(2) : "0.00";
    },
    openImage(imageUrl) {
      this.largeImageUrl = imageUrl;
      this.isImageOpen = true;
    },
    closeImage() {
      this.isImageOpen = false;
      this.largeImageUrl = "";
    },
    onImageError(event) {
      event.target.src = this.defaultImage;
    },
  },
  watch: {
    'userInfoForm.height'() {
      if (this.infoDialogVisible) {
        this.scheduleRecommendationRefresh();
      }
    },
    'userInfoForm.weight'() {
      if (this.infoDialogVisible) {
        this.scheduleRecommendationRefresh();
      }
    },
    uniforms: {
      handler() {
        this.filterBySchoolAndGrade();
      },
      deep: true,
    },
  },
};
</script>

<style scoped>
.mall-home {
  min-height: calc(100vh - var(--navbar-height));
  padding: 24px 0 56px;
  box-sizing: border-box;
}

.mall-toolbar,
.product-section {
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.mall-toolbar {
  display: grid;
  gap: 18px;
  margin-bottom: 18px;
  padding: 22px;
}

.mall-toolbar__head,
.product-section__head,
.product-card__title-row,
.product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.mall-toolbar__head h1 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1.25;
  letter-spacing: 0;
}

.mall-toolbar__head p,
.product-section__head p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.mall-toolbar__count {
  display: grid;
  min-width: 92px;
  padding: 12px 14px;
  border-radius: var(--radius-lg);
  background: var(--brand-soft);
  text-align: center;
}

.mall-toolbar__count span {
  color: var(--text-secondary);
  font-size: 12px;
}

.mall-toolbar__count strong {
  color: var(--brand);
  font-size: 24px;
}

.mall-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(3, minmax(150px, 0.9fr)) auto auto;
  gap: 12px;
  align-items: center;
}

.applied-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.applied-filters > span {
  color: var(--text-secondary);
  font-size: 13px;
}

.applied-filters button {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--brand);
  background: var(--brand-soft);
  cursor: pointer;
}

.product-section {
  padding: 22px;
}

.product-section__head {
  margin-bottom: 16px;
}

.product-section__head h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 22px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  width: 100%;
  align-items: stretch;
}

.product-card {
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.product-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-md);
}

.product-image-frame {
  position: relative;
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: var(--surface-muted);
  cursor: pointer;
}

.product-image-wrapper:focus-visible,
.product-image-preview:focus-visible,
.product-name-button:focus-visible,
.wishlist-button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.product-image-preview {
  position: absolute;
  top: 10px;
  left: 10px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  color: #fff;
  background: rgba(15, 23, 42, 0.68);
  cursor: pointer;
  backdrop-filter: blur(6px);
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-image-count {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 4px 8px;
  border-radius: 999px;
  color: #fff;
  background: rgba(15, 23, 42, 0.72);
  font-size: 12px;
}

.product-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

.product-name-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name-button:hover {
  color: var(--brand);
}

.product-card__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.product-name {
  margin: 0;
}

.wishlist-button {
  flex-shrink: 0;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: var(--surface);
  cursor: pointer;
}

.wishlist-button:hover,
.wishlist-button.is-active {
  color: var(--danger);
  border-color: var(--danger);
  background: var(--danger-soft);
}

.product-intro {
  min-height: 44px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.55;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.product-meta span {
  padding: 4px 8px;
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--surface-muted);
  font-size: 12px;
}

.product-card__footer {
  margin-top: auto;
}

.product-price {
  color: var(--brand);
  font-size: 22px;
  line-height: 1;
}

.no-results,
.loading-indicator {
  text-align: center;
  color: var(--text-secondary);
  padding: 30px;
  font-size: 1rem;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
}

.el-form-item {
  margin-bottom: 15px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.size-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 6px;
}

.el-select-dropdown__item.recommended-size-option {
  color: var(--success);
  font-weight: bold;
}

.el-select-dropdown__item.recommended-size-option.selected {
  color: var(--success) !important;
  background-color: var(--success-soft) !important;
}

.image-modal {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.82);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.large-image {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: var(--radius-md);
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background-color: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  width: 36px;
  height: 36px;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--text-primary);
  transition: background-color 0.16s ease;
}

.close-btn:hover {
  background-color: var(--surface-muted);
}

@media (max-width: 1080px) {
  .mall-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .mall-toolbar__head,
  .product-section__head,
  .product-card__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .mall-filters {
    grid-template-columns: 1fr;
  }

  .mall-filters > * {
    width: 100% !important;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
