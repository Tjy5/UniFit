<template>
  <div class="my-wishlist-page">
    <div class="page-header">
      <h1 class="page-title">我的收藏</h1>
    </div>

    <div v-if="loading && !wishlistItems.length" class="loading-indicator">
      <i class="el-icon-loading"></i> 加载中...
    </div>
    <div v-else-if="!loading && wishlistItems.length === 0" class="empty-wishlist">
      <el-empty description="您的收藏夹还是空的，快去逛逛吧！">
        <el-button type="primary" @click="$router.push('/mall')">去预定校服</el-button>
      </el-empty>
    </div>
    <div v-else class="wishlist-grid">
      <el-card v-for="item in wishlistItems" :key="item.wishlistId" class="wishlist-card" shadow="hover">
        <div class="card-content">
          <div class="item-image-wrapper">
            <img
              :src="getMainImageUrl(item.uniformImage)"
              :alt="item.uniformName"
              class="item-image"
              @error="onImageError"
              @click="goToProductDetail(item.uniformId)"
            />
          </div>
          <div class="item-info">
            <h3 class="item-name" @click="goToProductDetail(item.uniformId)">
              {{ item.uniformName }}
            </h3>
            <p class="item-price">¥{{ item.uniformPrice }}</p>
            <p class="item-school">学校: {{ item.schoolName }}</p>
            <p class="item-grade">年级: {{ item.gradeName }}</p>
            <p class="added-time">收藏于: {{ formatDate(item.addedTime) }}</p>
          </div>
        </div>
        <div class="card-actions">
          <el-button
            size="small"
            plain
            @click="goToProductDetail(item.uniformId)"
          >
            查看详情
          </el-button>
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="small"
            plain
            @click="removeFromWishlist(item.uniformId, item.wishlistId)"
            :loading="item.loadingRemove"
          >
            取消收藏
          </el-button>
          <el-button
            type="primary"
            icon="el-icon-shopping-cart-2"
            size="small"
            @click="openSizeSelectionDialog(item)"
          >
            加入购物车
          </el-button>
        </div>
      </el-card>
    </div>

    <el-dialog title="选择尺码和数量" v-model:visible="sizeSelectionDialogVisible" width="460px" append-to-body @close="resetSizeSelectionForm">
      <div v-if="currentUniformForCart" style="margin-bottom: 10px;">
        正在为 <strong>{{ currentUniformForCart.uniformName }}</strong> 选择尺码
      </div>

      <size-recommendation-panel
        :result="recommendationResult"
        :loading="recommendationLoading"
        :compact="true"
      />

      <div v-if="!recommendationResult.available && !loadingInitialInfo" class="wishlist-tip">
        {{ recommendationResult.message || '请先在个人资料中完善身高和体重信息。' }}
      </div>

      <el-form :model="sizeSelectionForm" :rules="sizeSelectionRules" ref="sizeSelectionFormRef" label-width="80px">
        <el-form-item label="尺码" prop="sizeId">
          <el-select v-model="sizeSelectionForm.sizeId" placeholder="请选择尺码" style="width:100%;">
            <el-option
              v-for="size in availableSizesForOrder"
              :key="size.id"
              :label="size.sizeName + (size.sizeName === finalRecommendedSize ? ' (推荐)' : '')"
              :value="size.id"
              :class="{ 'recommended-size-option': size.sizeName === finalRecommendedSize }"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="sizeSelectionForm.quantity" :min="1" :max="10" controls-position="right" style="width:100%;"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="sizeSelectionDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleAddToCart" :loading="addingToCart">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api/api'
import { getImageFallback, resolveImageList } from '@/utils/image'
import SizeRecommendationPanel from '@/components/size/SizeRecommendationPanel.vue'
import {
  buildRecommendationPayload,
  createEmptyRecommendation,
  getRecommendedSizeName,
  normalizeRecommendationResult,
} from '@/composables/useSizeRecommendation'
import { RECOMMENDATION_SOURCES } from '@/constants/recommendationSources'

export default {
  name: 'MyWishlist',
  components: {
    SizeRecommendationPanel,
  },
  data() {
    return {
      wishlistItems: [],
      loading: true,
      loadingInitialInfo: true,
      defaultImage: getImageFallback(),
      sizeSelectionDialogVisible: false,
      currentUniformForCart: null,
      sizeSelectionForm: {
        sizeId: null,
        quantity: 1,
      },
      sizeSelectionRules: {
        sizeId: [{ required: true, message: '请选择尺码', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
      },
      allSizesFromDB: [],
      availableSizesForOrder: [],
      addingToCart: false,
      userInfoForm: {
        messageUserAge: null,
        messageUserSex: '',
        height: null,
        weight: null,
        chest: null,
        waist: null,
        hip: null,
        shoulder: null,
      },
      recommendationLoading: false,
      recommendationResult: createEmptyRecommendation(),
      finalRecommendedSize: '',
    }
  },
  async created() {
    this.loading = true
    this.loadingInitialInfo = true
    await this.fetchSizes()
    await this.checkUserInfoAndRecommend()
    this.loadingInitialInfo = false
    await this.fetchWishlistItems()
  },
  methods: {
    async fetchWishlistItems() {
      this.loading = true
      try {
        const response = await api.getUserWishlist()
        this.wishlistItems = Array.isArray(response.data)
          ? response.data.map((dto) => ({ ...dto, loadingRemove: false }))
          : []
      } catch (error) {
        console.error('Error fetching wishlist:', error)
        this.$message.error('获取收藏列表失败: ' + (error.response?.data?.message || error.message))
        this.wishlistItems = []
      } finally {
        this.loading = false
      }
    },
    async removeFromWishlist(uniformId, wishlistIdForFrontendLookup) {
      const itemIndex = this.wishlistItems.findIndex((i) => i.wishlistId === wishlistIdForFrontendLookup)
      if (itemIndex === -1) return

      const item = this.wishlistItems[itemIndex]
      item.loadingRemove = true

      try {
        const response = await api.removeFromWishlist(uniformId)
        if (response.status === 204) {
          this.wishlistItems.splice(itemIndex, 1)
          this.$message.success('已从收藏夹移除')
        } else {
          this.$message.error('移除失败 (意外状态)，请重试')
        }
      } catch (error) {
        console.error('Error removing from wishlist:', error)
        if (error.response?.status === 404) {
          this.$message.warn(error.response.data || '该商品已不在您的收藏夹中')
          this.wishlistItems.splice(itemIndex, 1)
        } else {
          this.$message.error('移除失败: ' + (error.response?.data || error.message))
        }
      } finally {
        const existingItem = this.wishlistItems.find((i) => i.wishlistId === wishlistIdForFrontendLookup)
        if (existingItem) {
          existingItem.loadingRemove = false
        }
      }
    },
    getMainImageUrl(imagePaths) {
      const images = resolveImageList(imagePaths)
      return images[0] || this.defaultImage
    },
    onImageError(event) {
      event.target.src = this.defaultImage
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    },
    async fetchSizes() {
      try {
        const response = await api.getAllSizes()
        this.allSizesFromDB = response.data || []
        this.availableSizesForOrder = this.allSizesFromDB.map((size) => ({
          id: size.id,
          sizeName: String(size.sizeName).trim(),
        }))
      } catch (error) {
        console.error("Error fetching sizes for wishlist:", error)
        this.$message.error("获取尺码数据失败")
      }
    },
    applyUserMessage(userData = {}) {
      this.userInfoForm.messageUserAge = userData.messageUserAge ?? null
      this.userInfoForm.messageUserSex = userData.messageUserSex || ''
      this.userInfoForm.height = userData.height ?? null
      this.userInfoForm.weight = userData.weight ?? null
      this.userInfoForm.chest = userData.chest ?? null
      this.userInfoForm.waist = userData.waist ?? null
      this.userInfoForm.hip = userData.hip ?? null
      this.userInfoForm.shoulder = userData.shoulder ?? null
    },
    async checkUserInfoAndRecommend() {
      const userId = localStorage.getItem("userId")
      if (!userId) {
        this.clearAllRecommendations()
        return
      }
      try {
        const response = await api.getUserMessage(userId)
        this.applyUserMessage(response.data || {})
        await this.refreshRecommendation(RECOMMENDATION_SOURCES.WISHLIST)
      } catch (error) {
        if (error.response?.status !== 404) {
          console.error("Error fetching user info for wishlist:", error)
        }
        this.clearAllRecommendations()
      }
    },
    async refreshRecommendation(source, uniformId = undefined) {
      if (!this.userInfoForm.height || !this.userInfoForm.weight) {
        this.clearAllRecommendations('请先在个人资料中完善身高和体重信息。')
        return
      }
      this.recommendationLoading = true
      try {
        const response = await api.getSizeRecommendation(buildRecommendationPayload(this.userInfoForm, source, uniformId))
        this.recommendationResult = normalizeRecommendationResult(response.data)
        this.finalRecommendedSize = getRecommendedSizeName(this.recommendationResult)
        if (this.sizeSelectionDialogVisible && this.finalRecommendedSize) {
          const recommended = this.availableSizesForOrder.find((item) => item.sizeName === this.finalRecommendedSize)
          if (recommended) {
            this.sizeSelectionForm.sizeId = recommended.id
          }
        }
      } catch (error) {
        console.error('Error fetching recommendation for wishlist:', error)
        this.clearAllRecommendations('暂时无法获取尺码推荐，请稍后重试。')
      } finally {
        this.recommendationLoading = false
      }
    },
    clearAllRecommendations(message = '') {
      this.recommendationResult = createEmptyRecommendation(message)
      this.finalRecommendedSize = ''
    },
    async openSizeSelectionDialog(item) {
      this.currentUniformForCart = item
      this.clearAllRecommendations('正在获取当前商品推荐...')
      this.sizeSelectionDialogVisible = true
      const recommendedSizeObject = this.availableSizesForOrder.find((size) => size.sizeName === this.finalRecommendedSize)
      if (recommendedSizeObject) {
        this.sizeSelectionForm.sizeId = recommendedSizeObject.id
      } else if (this.availableSizesForOrder.length > 0) {
        this.sizeSelectionForm.sizeId = this.availableSizesForOrder[0].id
      }
      if (item?.uniformId && this.userInfoForm.height && this.userInfoForm.weight) {
        await this.refreshRecommendation(RECOMMENDATION_SOURCES.WISHLIST, item.uniformId)
      }
    },
    resetSizeSelectionForm() {
      if (this.$refs.sizeSelectionFormRef) {
        this.$refs.sizeSelectionFormRef.resetFields()
      }
      this.sizeSelectionForm.quantity = 1
      this.sizeSelectionForm.sizeId = null
    },
    async handleAddToCart() {
      if (!this.$refs.sizeSelectionFormRef) return
      this.$refs.sizeSelectionFormRef.validate(async (valid) => {
        if (!valid) {
          return
        }
        if (!this.currentUniformForCart) {
          this.$message.error('操作异常，请刷新页面重试')
          return
        }
        this.addingToCart = true
        try {
          const uniformId = this.currentUniformForCart.uniformId
          const sizeIdToSend = this.sizeSelectionForm.sizeId
          const quantity = this.sizeSelectionForm.quantity
          await api.addCartItem({
            uniformId,
            sizeId: sizeIdToSend,
            quantity,
            recommendationLogId: this.recommendationResult?.recommendationLogId || undefined,
          })
          this.sizeSelectionDialogVisible = false
          this.$message.success(`"${this.currentUniformForCart.uniformName}" 已成功添加到购物车`)
        } catch (error) {
          console.error('Error adding item to cart from wishlist:', error)
          this.$message.error(error.response?.data?.message || error.message || '添加到购物车失败，请重试')
        } finally {
          this.addingToCart = false
        }
      })
    },
    goToProductDetail(uniformId) {
      if (!uniformId) {
        return
      }
      this.$router.push({ name: 'ProductDetail', params: { uniformId } })
    },
  },
}
</script>

<style scoped>
.my-wishlist-page {
  width: min(1200px, calc(100% - 40px));
  min-height: calc(100vh - var(--navbar-height));
  margin: 0 auto;
  padding: 24px 0 48px;
  position: relative;
}

.page-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 18px;
}

.page-title {
  font-size: 26px;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0;
}

.loading-indicator,
.empty-wishlist {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  font-size: 1.2rem;
  color: var(--text-secondary);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
}

.empty-wishlist .el-button {
  margin-top: 20px;
}

.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.wishlist-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
  transition: box-shadow 0.16s ease, border-color 0.16s ease;
}

.wishlist-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-md);
}

.card-content {
  display: flex;
  padding: 15px;
}

.item-image-wrapper {
  width: 100px;
  height: 100px;
  margin-right: 15px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
  background-color: var(--surface-muted);
  border: 1px solid var(--line);
}

.item-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.item-image:focus-visible,
.item-name:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.item-info {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.item-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 5px;
  cursor: pointer;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-name:hover {
  color: var(--brand);
}

.item-price {
  font-size: 1rem;
  color: var(--brand);
  font-weight: bold;
  margin: 0 0 5px;
}

.item-school,
.item-grade,
.added-time {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  padding: 20px 15px 15px;
  border-top: 1px solid var(--line);
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.recommended-size-option {
  color: var(--success);
  font-weight: bold;
}

.el-select-dropdown__item.recommended-size-option.selected {
  color: var(--success) !important;
  background-color: var(--success-soft) !important;
}

.wishlist-tip {
  margin: 10px 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
