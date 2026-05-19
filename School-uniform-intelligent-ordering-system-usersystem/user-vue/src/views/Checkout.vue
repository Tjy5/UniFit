<template>
  <div class="checkout-container">
    <h1 class="page-title">确认订单信息</h1>

    <el-row :gutter="20">
      <!-- 左侧：商品清单 -->
      <el-col :xs="24" :sm="24" :md="14">
        <el-card class="box-card order-summary-card">
          <template #header>
          <div class="clearfix">
            <span>商品清单</span>
          </div>
          </template>
          <div v-if="loadingCart" class="loading-text">加载中...</div>
          <div v-else-if="!cartItems || cartItems.length === 0" class="empty-cart-text">
            购物车是空的，请先去<router-link to="/mall">选购商品</router-link>。
          </div>
          <div v-else>
            <div v-for="item in cartItems" :key="item.cartItemId" class="cart-item-checkout">
              <img :src="getImageUrl(item.uniformImage)" :alt="item.uniformName" class="item-image-checkout">
              <div class="item-details-checkout">
                <p class="item-name-checkout">{{ item.uniformName || '未知商品' }}</p>
                <p class="item-spec-checkout">尺码: {{ item.sizeNameDisplay || item.sizeId }} | 数量: {{ item.quantity }}</p>
              </div>
              <div class="item-price-checkout">¥{{ formatPrice(item.unitPrice * item.quantity) }}</div>
            </div>
            <el-divider></el-divider>
            <div class="total-summary-checkout">
              <span>商品总计: </span>
              <span class="total-price-checkout">¥{{ formatPrice(totalOrderPrice) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：收货地址选择和订单操作 -->
      <el-col :xs="24" :sm="24" :md="10">
        <el-card class="box-card shipping-info-card">
          <template #header>
          <div class="clearfix address-header">
            <span>选择收货地址</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              @click="goToAddressManagement"
            >
              管理地址
            </el-button>
          </div>
          </template>

          <div v-if="loadingAddresses" class="loading-text">地址加载中...</div>
          <div v-else-if="addresses.length === 0" class="no-address-text">
            您还没有添加收货地址，请先
            <el-button type="text" @click="goToAddressManagement">添加地址</el-button>
          </div>
          <!-- 地址选择列表 -->
          <el-radio-group v-else v-model="selectedAddressId" class="address-list">
            <div
              v-for="addr in addresses"
              :key="addr.id"
              class="address-item"
              :class="{ 'selected': selectedAddressId === addr.id }"
            >
              <el-radio :label="addr.id" border size="medium">
                <div class="address-content">
                  <div class="address-line name-phone">
                    <strong class="recipient-name">{{ addr.recipientName }}</strong>
                    <span class="phone-number">{{ addr.phoneNumber }}</span>
                    <el-tag v-if="addr.isDefault" type="success" size="mini" class="default-tag">默认</el-tag>
                  </div>
                  <div class="address-line full-address">
                    {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.streetAddress }}
                  </div>
                </div>
              </el-radio>
            </div>
          </el-radio-group>

          <!-- 订单备注 -->
          <el-form :model="orderForm" ref="orderFormRef" label-width="0px" style="margin-top: 20px;">
             <el-form-item label="" prop="remark">
                <el-input type="textarea" v-model="orderForm.remark" placeholder="订单备注 (选填，给商家留言)"></el-input>
             </el-form-item>
          </el-form>

        </el-card>

        <div class="checkout-actions">
          <el-button
            type="primary"
            @click="submitOrder"
            :loading="submittingOrder"
            :disabled="!cartItems || cartItems.length === 0 || loadingCart || loadingAddresses || !selectedAddressId"
            class="submit-order-btn"
          >
            提交订单
          </el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import api from '@/api/api';
import { getImageFallback, resolveImageUrl } from '@/utils/image';

export default {
  name: 'CheckoutPage',
  data() {
    return {
      cartItems: [], // 购物车商品列表
      loadingCart: true, // 购物车加载状态
      addresses: [], // 用户地址列表
      loadingAddresses: true, // 地址加载状态
      selectedAddressId: null, // 选中的地址ID
      submittingOrder: false, // 订单提交状态
      orderForm: {
        remark: '', // 订单备注
      },
      defaultImage: getImageFallback(),
    };
  },
  computed: {
    // 计算订单总价
    totalOrderPrice() {
      return this.cartItems.reduce((sum, item) => {
        const price = item.unitPrice || 0;
        const quantity = item.quantity || 0;
        return sum + price * quantity;
      }, 0);
    },
  },
  async created() {
    // 并发加载购物车和地址数据
    this.loadingCart = true;
    this.loadingAddresses = true;
    try {
        await Promise.all([
            this.fetchCartItems(),
            this.fetchAddresses()
        ]);
    } catch (error) {
         console.error("加载结算页数据失败:", error);
    } finally {
        this.loadingCart = false;
        this.loadingAddresses = false;
    }
  },
  methods: {
    // 获取购物车商品列表
    async fetchCartItems() {
      try {
        const response = await api.getCartItems();
        this.cartItems = response.data || [];
        if (this.cartItems.length === 0 && !this.loadingCart) {
            this.$message.warning("购物车是空的，正在返回商城首页...");
            setTimeout(() => {
                this.$router.push('/mall');
            }, 1500);
        }
      } catch (error) {
        console.error('获取购物车数据失败:', error);
        this.$message.error('加载购物车信息失败，请刷新页面重试');
        this.cartItems = [];
      }
    },

    // 获取用户地址列表
    async fetchAddresses() {
       try {
         const response = await api.listAddresses();
         this.addresses = response.data || [];
         // 自动选择默认地址
         const defaultAddress = this.addresses.find(addr => addr.isDefault);
         if (defaultAddress) {
           this.selectedAddressId = defaultAddress.id;
         } else {
           // 强制用户选择地址
           this.selectedAddressId = null;
         }
       } catch (error) {
         console.error('获取地址列表失败:', error);
         this.$message.error('加载地址列表失败，请稍后重试。');
         this.addresses = [];
       }
    },

    // 跳转到地址管理页面
    goToAddressManagement() {
      this.$router.push('/profile');
    },

    // 提交订单
    submitOrder() {
      // 验证地址选择
      if (!this.selectedAddressId) {
          this.$message.error('请选择一个收货地址。');
          return;
      }
      if (!this.cartItems || this.cartItems.length === 0) {
        this.$message.error('购物车中没有商品，无法提交订单。');
        return;
      }

      this.submittingOrder = true;
      const orderPayload = {
        addressId: this.selectedAddressId,
        remark: this.orderForm.remark,
      };

      api.createOrder(orderPayload)
        .then(response => {
          const createdOrder = response.data;
          this.$message.success('订单提交成功！');
          // 跳转到订单列表页面
          this.$router.push({ name: 'MyOrders', params: { newOrderId: createdOrder.id } });
        })
        .catch(error => {
          console.error('提交订单失败:', error.response?.data || error.message);
          this.$message.error(error.response?.data?.message || '订单提交失败，请检查网络或稍后重试。');
        })
        .finally(() => {
          this.submittingOrder = false;
        });
    },

    // 格式化价格显示
    formatPrice(price) {
      return Number(price || 0).toFixed(2);
    },

    // 获取商品图片URL
    getImageUrl(imagePath) {
      if (!imagePath) return this.defaultImage;
      if (imagePath.includes(',')) {
        imagePath = imagePath.split(',')[0].trim();
      }
      return resolveImageUrl(imagePath);
    },
  },
};
</script>

<style scoped>
.checkout-container {
  width: min(1200px, calc(100% - 40px));
  min-height: calc(100vh - var(--navbar-height));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.page-title {
  font-size: 26px;
  color: var(--text-primary);
  margin: 0 0 18px;
  letter-spacing: 0;
}

.box-card {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
}

.order-summary-card .clearfix span,
.shipping-info-card .clearfix span {
  font-weight: bold;
  font-size: 16px;
  color: var(--text-primary);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Styles for address list */
.address-list {
  width: 100%;
}
.address-item {
  margin-bottom: 15px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 5px;
  transition: border-color 0.2s;
  background: var(--surface);
}
.address-item:hover {
  border-color: var(--brand);
}
.address-item.selected {
   border-color: var(--brand);
   background-color: var(--brand-soft);
}
.address-item .el-radio {
    width: 100%;
    height: auto;
    padding: 10px 15px;
    white-space: normal;
    display: flex;
    align-items: flex-start;
}
.address-item :deep(.el-radio__label) {
    padding-left: 10px;
    line-height: 1.4;
    flex-grow: 1;
}
.address-content {
  display: flex;
  flex-direction: column;
}
.address-line {
  margin-bottom: 4px;
}
.address-line:last-child {
  margin-bottom: 0;
}
.name-phone {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 6px;
}
.recipient-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 15px;
}
.phone-number {
  color: var(--text-secondary);
  font-size: 14px;
}
.default-tag {
  margin-left: auto;
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
}
.full-address {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}
.no-address-text {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
}


.cart-item-checkout {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(107, 93, 79, 0.12);
}
.cart-item-checkout:last-child {
  border-bottom: none;
}
.item-image-checkout {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-md);
  margin-right: 15px;
  border: 1px solid var(--line);
  background: var(--surface-muted);
}
.item-details-checkout { flex-grow: 1; }
.item-name-checkout { font-size: 14px; color: var(--text-primary); margin-bottom: 5px; }
.item-spec-checkout { font-size: 12px; color: var(--text-muted); }
.item-price-checkout { font-size: 14px; color: var(--text-secondary); min-width: 80px; text-align: right; }
.total-summary-checkout { text-align: right; font-size: 16px; margin-top: 15px; }
.total-price-checkout { font-weight: bold; color: var(--brand); font-size: 22px; }
.checkout-actions { margin-top: 20px; text-align: right; }
.submit-order-btn { width: 150px; font-size: 16px; }
.loading-text, .empty-cart-text { text-align: center; color: var(--text-secondary); padding: 20px; }

@media (max-width: 900px) {
  .checkout-actions {
    text-align: stretch;
  }

  .submit-order-btn {
    width: 100%;
  }
}
</style>
