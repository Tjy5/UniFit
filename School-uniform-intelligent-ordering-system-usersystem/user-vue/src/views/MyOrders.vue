<template>
  <div class="my-orders-page">
    <section class="my-orders-shell">
      <h1 class="page-title">我的订单</h1>

      <div v-if="loading" class="orders-loading">
        <el-skeleton :rows="6" />
      </div>

      <div v-else-if="!visibleOrders.length" class="no-orders-placeholder">
        <p>您还没有相关的订单记录哦~</p>
      </div>

      <div v-else class="orders-list">
        <article v-for="order in visibleOrders" :key="order.id" class="order-card">
          <div class="order-card__summary">
            <div class="order-card__meta">
              <h2>订单 #{{ order.id }}</h2>
              <p>下单时间：{{ formatDate(order.orderDate) }}</p>
              <p>订单总价：{{ formatCurrency(order.totalPrice) }}</p>
            </div>

            <div class="order-card__status">
              <el-tag :type="getStatusTagType(order.status)">{{ getStatusText(order.status) }}</el-tag>
              <el-tag :type="getPaymentStatusTagType(order.paymentStatusCode)">{{ getPaymentStatusText(order.paymentStatusCode) }}</el-tag>
              <el-tag :type="getShippingStatusTagType(order.shippingStatusCode)">{{ getShippingStatusText(order.shippingStatusCode) }}</el-tag>
            </div>
          </div>

          <div class="order-card__address">
            <template v-if="order.shippingAddress">
              <strong>{{ order.shippingAddress.recipientName }}</strong>
              <span>{{ order.shippingAddress.phoneNumber }}</span>
              <p>{{ formatFullAddress(order.shippingAddress) }}</p>
            </template>
            <template v-else-if="order.addressId">
              <p>地址信息加载中或失败 (ID: {{ order.addressId }})</p>
            </template>
            <template v-else>
              <p>地址信息缺失</p>
            </template>
          </div>

          <div class="order-card__actions">
            <el-button
              v-if="hasAction(order, 'SIMULATE_PAYMENT_SUCCESS')"
              type="success"
              size="small"
              @click="handlePayOrder(order)"
            >
              支付
            </el-button>
            <el-button
              v-if="hasAction(order, 'CANCEL_UNPAID')"
              type="danger"
              size="small"
              @click="handleCancelOrder(order)"
            >
              取消订单
            </el-button>
            <el-button
              v-if="hasAction(order, 'CONFIRM_RECEIPT')"
              type="primary"
              size="small"
              @click="handleConfirmReceipt(order)"
            >
              确认签收
            </el-button>
            <el-button
              v-if="hasAction(order, 'REQUEST_REFUND')"
              type="warning"
              size="small"
              @click="handleRefundOrder(order)"
            >
              申请退货
            </el-button>
            <el-button
              v-if="order.status === 3 || order.status === 4 || order.status === 6"
              type="info"
              size="small"
              @click="handleDeleteOrderLocally(order.id)"
            >
              删除
            </el-button>
          </div>

          <div class="order-card__items">
            <div v-for="item in order.orderItems" :key="item.orderItemId" class="order-item">
              <img
                :src="formatImageUrl(item.imageSnapshot)"
                alt="商品图片"
                class="order-item__image"
                @error="onImageError"
              />
              <div class="order-item__info">
                <h3>{{ item.uniformNameSnapshot }}</h3>
                <p>尺码：{{ item.sizeNameSnapshot }}</p>
                <p>单价：{{ formatCurrency(item.unitPriceSnapshot) }}</p>
                <p>数量：{{ item.quantity }}</p>
                <p>小计：{{ formatCurrency(item.itemTotalPrice) }}</p>
              </div>
              <div class="order-item__review">
                <el-button
                  v-if="isReviewable(order, item)"
                  type="primary"
                  size="small"
                  plain
                  @click="handleReviewItem(item, order.id)"
                >
                  去评价
                </el-button>
                <span v-else-if="item.reviewId && item.reviewId > 0">已评价</span>
                <span v-else>-</span>

                <el-button
                  v-if="isFeedbackEligible(order)"
                  type="success"
                  size="small"
                  plain
                  @click="openFeedbackDialog(order, item)"
                >
                  {{ getFeedbackButtonLabel(item) }}
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <size-feedback-dialog
      v-model="feedbackDialogVisible"
      :loading="feedbackDialogLoading"
      :submitting="feedbackSubmitting"
      :order-item="feedbackTargetItem"
      :existing-feedback="currentFeedbackRecord"
      :preference="currentSizePreference"
      :recommendation="feedbackRecommendation"
      :recommendation-loading="feedbackRecommendationLoading"
      @submit="handleFeedbackSubmit"
    />
  </div>
</template>

<script>
import api from "@/api/api";
import SizeFeedbackDialog from "@/components/size/SizeFeedbackDialog.vue";
import { getImageFallback, resolveImageUrl } from "@/utils/image";
import {
  createEmptyRecommendation,
  normalizeRecommendationResult,
} from "@/composables/useSizeRecommendation";
import { RECOMMENDATION_SOURCES } from "@/constants/recommendationSources";

export default {
  name: "MyOrders",
  components: {
    SizeFeedbackDialog,
  },
  data() {
    return {
      orders: [],
      loading: false,
      defaultImage: getImageFallback(),
      REVIEWABLE_ORDER_STATUSES: [3],
      feedbackDialogVisible: false,
      feedbackDialogLoading: false,
      feedbackSubmitting: false,
      feedbackRecommendationLoading: false,
      feedbackRecommendation: createEmptyRecommendation(),
      feedbackTargetItem: null,
      feedbackOrderId: null,
      feedbackRecords: {},
      currentFeedbackRecord: null,
      currentSizePreference: null,
    };
  },
  computed: {
    visibleOrders() {
      const hidden = localStorage.getItem("hiddenOrderIds");
      const hiddenIds = hidden ? JSON.parse(hidden) : [];
      const filteredOrders = this.orders.filter((order) => !hiddenIds.includes(order.id));
      return filteredOrders.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));
    },
  },
  mounted() {
    this.fetchOrders();
  },
  methods: {
    formatCurrency(value) {
      const numeric = typeof value === "number" ? value : Number(value) || 0;
      return `¥${numeric.toFixed(2)}`;
    },
    async fetchOrders() {
      this.loading = true;
      try {
        const response = await api.getMyOrders();
        this.orders = (response.data || []).map((order) => ({
          ...order,
          orderItems: (order.orderItems || []).map((item) => ({
            ...item,
            reviewId: item.reviewId !== undefined ? item.reviewId : null,
          })),
          shippingAddress: order.shippingAddress || null,
        }));
      } catch (error) {
        console.error("Error fetching orders:", error);
        this.$message.error("获取订单列表失败，请稍后重试。");
        this.orders = [];
      } finally {
        this.loading = false;
      }
    },
    formatImageUrl(imagePath) {
      return resolveImageUrl(imagePath);
    },
    onImageError(event) {
      event.target.src = this.defaultImage;
    },
    formatDate(dateString) {
      if (!dateString) return "N/A";
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return "无效日期";
      return date
        .toLocaleString("zh-CN", {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
        })
        .replace(/\//g, "-");
    },
    formatFullAddress(address) {
      if (!address) return "地址信息不完整";
      const parts = [address.province, address.city, address.district, address.streetAddress];
      return parts.filter(Boolean).join(" ");
    },
    getStatusText(status) {
      const statusMap = {
        0: "待支付",
        1: "待发货",
        2: "已发货",
        3: "已完成",
        4: "已取消",
        5: "退款中",
        6: "已退款",
      };
      return statusMap[status] ?? `未知状态 (${status})`;
    },
    getStatusTagType(status) {
      const typeMap = {
        0: "warning",
        1: "info",
        2: "primary",
        3: "success",
        4: "",
        5: "warning",
        6: "danger",
      };
      return typeMap[status] || "info";
    },
    getPaymentStatusText(code) {
      const map = {
        PENDING: "待支付",
        PAID: "已支付",
        FAILED: "支付失败",
        REFUNDING: "退款处理中",
        REFUNDED: "已退款",
        CLOSED: "已关闭",
      };
      return map[code] || code || "未知";
    },
    getPaymentStatusTagType(code) {
      const map = {
        PENDING: "warning",
        PAID: "success",
        FAILED: "danger",
        REFUNDING: "warning",
        REFUNDED: "info",
        CLOSED: "info",
      };
      return map[code] || "";
    },
    getShippingStatusText(code) {
      const map = {
        NOT_SHIPPED: "未发货",
        SHIPPED: "已发货",
        DELIVERED: "已签收",
        RETURN_REQUESTED: "退货申请中",
        RETURNED: "已退货",
      };
      return map[code] || code || "未知";
    },
    getShippingStatusTagType(code) {
      const map = {
        NOT_SHIPPED: "info",
        SHIPPED: "primary",
        DELIVERED: "success",
        RETURN_REQUESTED: "warning",
        RETURNED: "info",
      };
      return map[code] || "";
    },
    hasAction(order, action) {
      return Array.isArray(order?.allowedFulfillmentActions) && order.allowedFulfillmentActions.includes(action);
    },
    async runFulfillmentCommand(order, command, successMessage, reason) {
      this.loading = true;
      try {
        await api.executeOrderFulfillmentCommand(order.id, command, reason);
        this.$message.success(successMessage);
        await this.fetchOrders();
      } catch (error) {
        console.error("Error executing order fulfillment command:", error);
        this.$message.error(error.response?.data?.message || error.message || "订单操作失败");
      } finally {
        this.loading = false;
      }
    },
    async handlePayOrder(order) {
      const formattedPrice = this.formatCurrency(order.totalPrice);
      this.$confirm(`模拟支付成功。您将为订单 ${order.id} (总价: ${formattedPrice}) 更新状态。`, "模拟支付", {
        confirmButtonText: "确认更新",
        cancelButtonText: "取消",
        type: "info",
      })
        .then(async () => {
          await this.runFulfillmentCommand(order, "SIMULATE_PAYMENT_SUCCESS", "模拟支付成功！订单状态已更新。");
        })
        .catch(() => this.$message.info("操作已取消"));
    },
    async handleCancelOrder(order) {
      if (!this.hasAction(order, "CANCEL_UNPAID")) {
        this.$message.warning("当前订单状态无法取消。");
        return;
      }
      this.$confirm(`确定要取消订单 ${order.id} 吗？此操作无法撤销。`, "确认取消订单", {
        confirmButtonText: "确定取消",
        cancelButtonText: "再想想",
        type: "warning",
      })
        .then(async () => {
          await this.runFulfillmentCommand(order, "CANCEL_UNPAID", "订单已取消");
        })
        .catch(() => this.$message.info("取消操作已撤销"));
    },
    async handleConfirmReceipt(order) {
      this.$confirm(`确认订单 ${order.id} 已签收吗？`, "确认签收", {
        confirmButtonText: "确认签收",
        cancelButtonText: "取消",
        type: "info",
      })
        .then(async () => {
          await this.runFulfillmentCommand(order, "CONFIRM_RECEIPT", "订单已确认签收");
        })
        .catch(() => this.$message.info("操作已取消"));
    },
    async handleRefundOrder(order) {
      if (!this.hasAction(order, "REQUEST_REFUND")) {
        this.$message.warning("当前订单状态无法申请退货退款。");
        return;
      }
      this.$confirm(`确定要为订单 ${order.id} 申请退货退款吗？`, "申请退货退款", {
        confirmButtonText: "确定申请",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(async () => {
          await this.runFulfillmentCommand(order, "REQUEST_REFUND", "已提交退货退款申请，请等待处理。");
        })
        .catch(() => this.$message.info("申请退货操作已取消"));
    },
    handleDeleteOrderLocally(orderId) {
      this.$confirm("确定要删除此订单记录吗？", "确认删除订单", {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          const hidden = localStorage.getItem("hiddenOrderIds");
          const hiddenIds = hidden ? JSON.parse(hidden) : [];
          if (!hiddenIds.includes(orderId)) {
            hiddenIds.push(orderId);
            localStorage.setItem("hiddenOrderIds", JSON.stringify(hiddenIds));
            this.$message.success("订单记录已删除");
            this.orders = [...this.orders];
          } else {
            this.$message.info("此订单记录已被删除");
          }
        })
        .catch(() => this.$message.info("删除操作已取消"));
    },
    isReviewable(mainOrder, orderItem) {
      if (!mainOrder || !orderItem) return false;
      const orderStatusIsReviewable = this.REVIEWABLE_ORDER_STATUSES.includes(mainOrder.status);
      const itemNotReviewed = !orderItem.reviewId || orderItem.reviewId <= 0;
      return orderStatusIsReviewable && itemNotReviewed;
    },
    isFeedbackEligible(order) {
      return order?.status === 3;
    },
    getFeedbackButtonLabel(item) {
      return this.feedbackRecords[item.orderItemId]?.submitted ? "修改尺码反馈" : "尺码反馈";
    },
    handleReviewItem(orderItem, mainOrderId) {
      this.$router.push({
        name: "SubmitReview",
        query: {
          orderItemId: orderItem.orderItemId,
          uniformId: orderItem.uniformId,
          orderId: mainOrderId,
          uniformName: orderItem.uniformNameSnapshot,
          uniformImage: this.formatImageUrl(orderItem.imageSnapshot),
        },
      });
    },
    async openFeedbackDialog(order, item) {
      this.feedbackDialogVisible = true;
      this.feedbackDialogLoading = true;
      this.feedbackRecommendationLoading = true;
      this.feedbackTargetItem = item;
      this.feedbackOrderId = order.id;
      this.currentFeedbackRecord = null;
      this.currentSizePreference = null;
      this.feedbackRecommendation = createEmptyRecommendation();

      try {
        const [feedbackResult, recommendationSnapshotResult, preferenceResult] = await Promise.allSettled([
          api.getOrderItemSizeFeedback(item.orderItemId),
          api.getOrderItemRecommendationSnapshot(item.orderItemId),
          api.getSizePreference({ uniformId: item.uniformId }),
        ]);

        if (feedbackResult.status === 'fulfilled') {
          this.currentFeedbackRecord = feedbackResult.value.data;
          if (feedbackResult.value.data?.submitted) {
            this.feedbackRecords[item.orderItemId] = feedbackResult.value.data;
          }
        } else if (feedbackResult.reason?.response?.status !== 404) {
          throw feedbackResult.reason;
        }

        if (recommendationSnapshotResult.status === 'fulfilled') {
          this.feedbackRecommendation = normalizeRecommendationResult(recommendationSnapshotResult.value.data);
        } else if (recommendationSnapshotResult.reason?.response?.status === 404) {
          const fallbackRecommendation = await api.getSizeRecommendation({
            source: RECOMMENDATION_SOURCES.ORDER_FEEDBACK,
            uniformId: item.uniformId,
          });
          this.feedbackRecommendation = normalizeRecommendationResult(fallbackRecommendation.data);
        } else {
          throw recommendationSnapshotResult.reason;
        }

        if (preferenceResult.status === 'fulfilled') {
          this.currentSizePreference = preferenceResult.value.data;
        } else if (preferenceResult.reason?.response?.status !== 404) {
          console.warn('Unable to load size preference:', preferenceResult.reason);
        }
      } catch (error) {
        console.error('Error preparing size feedback dialog:', error);
        this.$message.error('加载尺码反馈信息失败，请稍后重试。');
        this.feedbackDialogVisible = false;
      } finally {
        this.feedbackDialogLoading = false;
        this.feedbackRecommendationLoading = false;
      }
    },
    async handleFeedbackSubmit(payload) {
      if (!this.feedbackTargetItem) {
        return;
      }
      this.feedbackSubmitting = true;
      try {
        const response = await api.saveOrderItemSizeFeedback(this.feedbackTargetItem.orderItemId, payload);
        this.feedbackRecords[this.feedbackTargetItem.orderItemId] = response.data;
        this.currentFeedbackRecord = response.data;
        this.feedbackDialogVisible = false;
        if (payload.fitPreference) {
          api.saveSizePreference({
            uniformId: this.feedbackTargetItem.uniformId,
            fitPreference: payload.fitPreference,
          }).catch((error) => {
            console.warn('Unable to save size preference:', error);
          });
        }
        this.$message.success('尺码反馈已保存');
      } catch (error) {
        console.error('Error saving size feedback:', error);
        this.$message.error(error.response?.data?.message || '保存尺码反馈失败');
      } finally {
        this.feedbackSubmitting = false;
      }
    },
  },
};
</script>

<style scoped>
.my-orders-page {
  width: min(1200px, calc(100% - 40px));
  margin: 0 auto;
  min-height: calc(100vh - var(--navbar-height));
  padding: 24px 0 48px;
}

.my-orders-shell {
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.page-title {
  color: var(--text-primary);
  font-size: 26px;
  margin: 0 0 20px;
  font-weight: 700;
  letter-spacing: 0;
}

.orders-list {
  display: grid;
  gap: 20px;
}

.order-card {
  border-radius: var(--radius-lg);
  padding: 24px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  border: 1px solid var(--line);
}

.order-card__summary {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.order-card__meta h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
}

.order-card__meta p,
.order-card__address p {
  margin: 4px 0;
  color: var(--text-secondary);
}

.order-card__status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.order-card__address {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
}

.order-card__address strong {
  margin-right: 12px;
  color: var(--text-primary);
}

.order-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.order-card__items {
  display: grid;
  gap: 14px;
  margin-top: 22px;
}

.order-item {
  display: grid;
  grid-template-columns: 88px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
}

.order-item__image {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: var(--radius-md);
  background: var(--surface);
  border: 1px solid var(--line);
}

.order-item__info h3 {
  margin: 0 0 8px;
  color: var(--text-primary);
}

.order-item__info p {
  margin: 4px 0;
  color: var(--text-secondary);
}

.order-item__review {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.orders-loading,
.no-orders-placeholder {
  border-radius: var(--radius-lg);
  background: var(--surface-muted);
  padding: 24px;
}

.no-orders-placeholder {
  text-align: center;
  color: var(--text-secondary);
  font-size: 16px;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .my-orders-shell {
    padding: 20px;
  }

  .order-card__summary {
    flex-direction: column;
  }

  .order-item {
    grid-template-columns: 1fr;
  }

  .order-item__review {
    align-items: flex-start;
  }
}
</style>
