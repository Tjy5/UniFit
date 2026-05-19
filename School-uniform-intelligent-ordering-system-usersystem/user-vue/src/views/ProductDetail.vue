<template>
  <div class="product-detail-page">
    <section class="detail-shell">
      <div class="detail-nav">
        <el-button plain @click="goBack">返回</el-button>
        <el-button @click="goToMall">继续浏览</el-button>
      </div>

      <section v-if="pageLoading" class="detail-state-card">
        <el-skeleton :rows="8" animated />
      </section>

      <section v-else-if="pageError" class="detail-state-card">
        <el-empty :description="pageError">
          <el-button type="primary" @click="loadPage">重新加载</el-button>
          <el-button @click="goToMall">返回商城</el-button>
        </el-empty>
      </section>

      <section v-else-if="!uniform" class="detail-state-card">
        <el-empty description="未找到该校服商品">
          <el-button type="primary" @click="goToMall">返回商城</el-button>
        </el-empty>
      </section>

      <section v-else class="detail-layout">
        <article class="detail-gallery-card">
          <div class="detail-main-image">
            <img :src="currentImage" :alt="uniform.name || '校服图片'" @error="onImageError" />
          </div>
          <div v-if="galleryImages.length > 1" class="detail-thumbnails" aria-label="商品图片缩略图">
            <button
              v-for="(image, index) in galleryImages"
              :key="`${uniform.id}-${index}`"
              type="button"
              class="detail-thumbnail"
              :class="{ 'is-active': index === activeImageIndex }"
              :aria-label="`查看第 ${index + 1} 张图片`"
              @click="selectImage(index)"
            >
              <img :src="image" :alt="`${uniform.name || '校服'} 缩略图 ${index + 1}`" @error="onImageError" />
            </button>
          </div>
        </article>

        <article class="detail-summary-card">
          <div class="detail-summary-head">
            <div class="detail-summary-head__text">
              <p class="detail-summary-kicker">{{ uniform.schoolName || '未配置学校' }} / {{ uniform.gradeName || '未配置年级' }}</p>
              <h1>{{ uniform.name }}</h1>
            </div>
            <el-tag :type="uniform.status === 0 ? 'success' : 'warning'">
              {{ uniform.status === 0 ? '可预订' : '暂不可预订' }}
            </el-tag>
          </div>

          <div class="detail-price">¥{{ formatPrice(uniform.price) }}</div>

          <p v-if="uniform.intro" class="detail-intro">{{ uniform.intro }}</p>

          <div class="detail-meta-grid">
            <div class="detail-meta-item">
              <span>所属学校</span>
              <strong>{{ uniform.schoolName || '未配置学校' }}</strong>
            </div>
            <div class="detail-meta-item">
              <span>适用年级</span>
              <strong>{{ uniform.gradeName || '未配置年级' }}</strong>
            </div>
            <div class="detail-meta-item">
              <span>商品编号</span>
              <strong>#{{ uniform.id }}</strong>
            </div>
            <div class="detail-meta-item">
              <span>当前状态</span>
              <strong>{{ uniform.status === 0 ? '可预订' : '暂不可预订' }}</strong>
            </div>
          </div>

          <section class="detail-section">
            <div class="detail-section__head">
              <h2>尺码推荐</h2>
              <div class="detail-section__actions">
                <el-button plain :loading="recommendationLoading" @click="refreshRecommendationForDetail">重新获取推荐</el-button>
                <el-button v-if="!hasProfileMeasurements" type="primary" plain @click="$router.push('/profile')">完善资料</el-button>
              </div>
            </div>

            <size-recommendation-panel
              :result="recommendationResult"
              :loading="recommendationLoading"
            />
          </section>

          <section class="detail-section">
            <div class="detail-section__head">
              <h2>选择尺码</h2>
              <p v-if="finalRecommendedSize" class="detail-section__hint">当前推荐尺码：{{ finalRecommendedSize }}</p>
              <p v-else-if="recommendationResult.message" class="detail-section__hint">{{ recommendationResult.message }}</p>
            </div>

            <div v-if="!availableSizesForOrder.length" class="detail-size-empty">
              当前暂无可选尺码，请稍后重试。
            </div>
            <div
              v-else
              class="detail-size-options"
              role="radiogroup"
              aria-label="可选尺码"
            >
              <button
                v-for="(size, index) in availableSizesForOrder"
                :key="size.id"
                ref="sizeOptionButtons"
                type="button"
                class="detail-size-option"
                :class="{
                  'is-selected': selectedSizeId === size.id,
                  'is-recommended': size.sizeName === finalRecommendedSize,
                }"
                :aria-checked="selectedSizeId === size.id"
                role="radio"
                @click="selectSize(size.id)"
                @keydown="handleSizeKeydown($event, index)"
              >
                <span>{{ size.sizeName }}</span>
                <small v-if="size.sizeName === finalRecommendedSize">推荐</small>
              </button>
            </div>

            <div class="detail-quantity">
              <span>数量</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="10"
                controls-position="right"
                aria-label="购买数量"
              />
            </div>

            <div class="detail-primary-actions">
              <el-button plain @click="goToMall">继续浏览</el-button>
              <el-button
                type="primary"
                :loading="addingToCart"
                :disabled="!canAddToCart"
                @click="addToCart"
              >
                加入购物车
              </el-button>
            </div>
          </section>
        </article>
      </section>
    </section>
  </div>
</template>

<script>
import api from '@/api/api'
import SizeRecommendationPanel from '@/components/size/SizeRecommendationPanel.vue'
import {
  buildRecommendationPayload,
  createEmptyRecommendation,
  getRecommendedSizeName,
  normalizeRecommendationResult,
} from '@/composables/useSizeRecommendation'
import { RECOMMENDATION_SOURCES } from '@/constants/recommendationSources'
import { getImageFallback, resolveImageList } from '@/utils/image'

export default {
  name: 'ProductDetail',
  components: {
    SizeRecommendationPanel,
  },
  data() {
    return {
      uniform: null,
      pageLoading: false,
      pageError: '',
      sharedDataLoaded: false,
      allSizesFromDB: [],
      availableSizesForOrder: [],
      activeImageIndex: 0,
      defaultImage: getImageFallback(),
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
      recommendationResult: createEmptyRecommendation('请先在个人资料中完善身高和体重信息。'),
      finalRecommendedSize: '',
      selectedSizeId: null,
      quantity: 1,
      addingToCart: false,
    }
  },
  computed: {
    uniformId() {
      const value = Number(this.$route.params.uniformId)
      return Number.isFinite(value) && value > 0 ? value : null
    },
    galleryImages() {
      const images = resolveImageList(this.uniform?.image)
      return images.length ? images : [this.defaultImage]
    },
    currentImage() {
      return this.galleryImages[this.activeImageIndex] || this.galleryImages[0] || this.defaultImage
    },
    hasProfileMeasurements() {
      return Boolean(this.userInfoForm.height && this.userInfoForm.weight)
    },
    canAddToCart() {
      return Boolean(
        this.uniform
          && this.uniform.status === 0
          && this.availableSizesForOrder.length
          && this.selectedSizeId
          && !this.addingToCart
      )
    },
  },
  async created() {
    await this.loadSharedData()
    await this.loadPage()
  },
  watch: {
    '$route.params.uniformId': {
      async handler() {
        this.activeImageIndex = 0
        this.quantity = 1
        await this.loadPage()
      },
    },
  },
  methods: {
    async loadSharedData() {
      if (this.sharedDataLoaded) {
        return
      }

      await Promise.all([
        this.fetchSizes(),
        this.fetchUserInfo(),
      ])
      this.sharedDataLoaded = true
    },
    async loadPage() {
      if (!this.sharedDataLoaded) {
        await this.loadSharedData()
      }

      if (!this.uniformId) {
        this.uniform = null
        this.pageError = '商品编号无效，请返回列表重新选择。'
        return
      }

      this.pageLoading = true
      this.pageError = ''
      this.uniform = null

      try {
        const response = await api.getUniformDetail(this.uniformId)
        this.uniform = response.data || null
        this.activeImageIndex = 0
        this.applyDefaultSizeSelection()

        if (this.hasProfileMeasurements) {
          await this.refreshRecommendationForDetail()
        } else {
          this.clearRecommendation('请先在个人资料中完善身高和体重信息。')
        }
      } catch (error) {
        console.error('Error fetching uniform detail:', error)
        this.uniform = null
        if (error.response?.status === 404) {
          this.pageError = '该商品不存在或暂时无法查看。'
        } else {
          this.pageError = '商品详情加载失败，请稍后重试。'
        }
      } finally {
        this.pageLoading = false
      }
    },
    async fetchSizes() {
      try {
        const response = await api.getAllSizes()
        this.allSizesFromDB = response.data || []
        this.availableSizesForOrder = this.allSizesFromDB.map((size) => ({
          id: size.id,
          sizeName: String(size.sizeName).trim(),
        }))
        this.applyDefaultSizeSelection()
      } catch (error) {
        console.error('Error fetching sizes for detail page:', error)
        this.$message.error('获取尺码数据失败，请稍后重试。')
        this.allSizesFromDB = []
        this.availableSizesForOrder = []
      }
    },
    async fetchUserInfo() {
      const userId = localStorage.getItem('userId')
      if (!userId) {
        return
      }

      try {
        const response = await api.getUserMessage(userId)
        this.applyUserMessage(response.data || {})
      } catch (error) {
        if (error.response?.status !== 404) {
          console.error('Error fetching user info for detail page:', error)
        }
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
    async refreshRecommendationForDetail() {
      if (!this.uniformId || !this.hasProfileMeasurements) {
        this.clearRecommendation('请先在个人资料中完善身高和体重信息。')
        return
      }

      this.recommendationLoading = true
      try {
        const response = await api.getSizeRecommendation(
          buildRecommendationPayload(this.userInfoForm, RECOMMENDATION_SOURCES.PRODUCT_DETAIL, this.uniformId),
        )
        this.recommendationResult = normalizeRecommendationResult(response.data)
        this.finalRecommendedSize = getRecommendedSizeName(this.recommendationResult)
        this.applyDefaultSizeSelection()
      } catch (error) {
        console.error('Error fetching recommendation for detail page:', error)
        this.clearRecommendation('暂时无法获取尺码推荐，请稍后重试。')
      } finally {
        this.recommendationLoading = false
      }
    },
    clearRecommendation(message = '') {
      this.recommendationResult = createEmptyRecommendation(message)
      this.finalRecommendedSize = ''
      this.applyDefaultSizeSelection()
    },
    applyDefaultSizeSelection() {
      if (!this.availableSizesForOrder.length) {
        this.selectedSizeId = null
        return
      }

      if (this.finalRecommendedSize) {
        const recommended = this.availableSizesForOrder.find((size) => size.sizeName === this.finalRecommendedSize)
        if (recommended) {
          this.selectedSizeId = recommended.id
          return
        }
      }

      if (!this.availableSizesForOrder.some((size) => size.id === this.selectedSizeId)) {
        this.selectedSizeId = this.availableSizesForOrder[0].id
      }
    },
    selectImage(index) {
      this.activeImageIndex = index
    },
    selectSize(sizeId) {
      this.selectedSizeId = sizeId
    },
    handleSizeKeydown(event, index) {
      if (!this.availableSizesForOrder.length) {
        return
      }

      const forwardKeys = ['ArrowRight', 'ArrowDown']
      const backwardKeys = ['ArrowLeft', 'ArrowUp']
      let nextIndex = null

      if (forwardKeys.includes(event.key)) {
        nextIndex = (index + 1) % this.availableSizesForOrder.length
      } else if (backwardKeys.includes(event.key)) {
        nextIndex = (index - 1 + this.availableSizesForOrder.length) % this.availableSizesForOrder.length
      } else {
        return
      }

      event.preventDefault()
      const nextSize = this.availableSizesForOrder[nextIndex]
      this.selectedSizeId = nextSize.id
      this.$nextTick(() => {
        const buttons = this.$refs.sizeOptionButtons
        const target = Array.isArray(buttons) ? buttons[nextIndex] : buttons
        target?.focus?.()
      })
    },
    async addToCart() {
      if (!this.canAddToCart) {
        return
      }

      this.addingToCart = true
      try {
        await api.addCartItem({
          uniformId: this.uniformId,
          sizeId: this.selectedSizeId,
          quantity: this.quantity,
          recommendationLogId: this.recommendationResult?.recommendationLogId || undefined,
        })
        this.$message.success('商品已成功添加到购物车')
      } catch (error) {
        console.error('Error adding detail item to cart:', error)
        this.$message.error(error.response?.data?.message || error.message || '添加到购物车失败，请重试')
      } finally {
        this.addingToCart = false
      }
    },
    formatPrice(price) {
      const value = Number(price || 0)
      return Number.isFinite(value) ? value.toFixed(2) : '0.00'
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.back()
        return
      }
      this.goToMall()
    },
    goToMall() {
      this.$router.push({ name: 'MallHome' })
    },
    onImageError(event) {
      event.target.src = this.defaultImage
    },
  },
}
</script>

<style scoped>
.product-detail-page {
  width: min(1200px, calc(100% - 40px));
  min-height: calc(100vh - var(--navbar-height));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.detail-shell {
  display: grid;
  gap: 18px;
}

.detail-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-state-card,
.detail-gallery-card,
.detail-summary-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.detail-state-card {
  padding: 24px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 460px);
  gap: 20px;
  align-items: start;
}

.detail-gallery-card,
.detail-summary-card {
  padding: 18px;
}

.detail-gallery-card {
  display: grid;
  gap: 14px;
}

.detail-main-image {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface-muted);
}

.detail-main-image img,
.detail-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-thumbnails {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(72px, 1fr));
  gap: 10px;
}

.detail-thumbnail {
  aspect-ratio: 1;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  cursor: pointer;
}

.detail-thumbnail.is-active,
.detail-thumbnail:hover {
  border-color: var(--brand);
}

.detail-thumbnail:focus-visible,
.detail-size-option:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.detail-summary-card {
  display: grid;
  gap: 18px;
}

.detail-summary-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 14px;
}

.detail-summary-kicker {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-summary-head h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1.2;
}

.detail-price {
  color: var(--brand);
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.detail-intro {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-meta-item {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.detail-meta-item span {
  color: var(--text-secondary);
  font-size: 12px;
}

.detail-meta-item strong {
  color: var(--text-primary);
  font-size: 15px;
}

.detail-section {
  display: grid;
  gap: 14px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.detail-section__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-section__head h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
}

.detail-section__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-section__hint {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-size-empty {
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: var(--surface-muted);
}

.detail-size-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
  gap: 10px;
}

.detail-size-option {
  display: grid;
  gap: 4px;
  justify-items: center;
  min-height: 64px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--surface);
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background-color 0.16s ease,
    color 0.16s ease;
}

.detail-size-option small {
  color: var(--text-secondary);
  font-size: 11px;
}

.detail-size-option:hover,
.detail-size-option.is-selected {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
}

.detail-size-option.is-recommended small,
.detail-size-option.is-selected small {
  color: var(--brand);
}

.detail-quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.detail-quantity span {
  color: var(--text-primary);
  font-weight: 600;
}

.detail-primary-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 980px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .product-detail-page {
    width: min(100%, calc(100% - 24px));
    padding: 16px 0 36px;
  }

  .detail-gallery-card,
  .detail-summary-card,
  .detail-state-card {
    padding: 16px;
  }

  .detail-summary-head h1 {
    font-size: 24px;
  }

  .detail-price {
    font-size: 28px;
  }

  .detail-meta-grid {
    grid-template-columns: 1fr;
  }

  .detail-primary-actions,
  .detail-section__actions,
  .detail-nav {
    flex-direction: column;
  }

  .detail-primary-actions :deep(.el-button),
  .detail-section__actions :deep(.el-button),
  .detail-nav :deep(.el-button) {
    width: 100%;
  }
}
</style>
