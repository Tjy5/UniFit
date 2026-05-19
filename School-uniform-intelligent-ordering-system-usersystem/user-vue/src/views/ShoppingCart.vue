<template>
  <div class="shopping-cart-container">
    <h1 class="page-title">我的购物车</h1>
    
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="cartItems.length === 0" class="empty-cart">
      <i class="el-icon-shopping-cart-2 empty-cart-icon"></i>
      <p>购物车空空如也</p>
    </div>
    
    <div v-else class="cart-content">
      <div class="cart-items">
        <div class="cart-header">
          <div class="cart-column product-info">商品信息</div>
          <div class="cart-column product-price">单价</div>
          <div class="cart-column product-quantity">数量</div>
          <div class="cart-column product-total">小计</div>
          <div class="cart-column product-action">操作</div>
        </div>
        
        <div v-for="item in cartItems" :key="item.cartItemId" class="cart-item">
          <div class="product-info">
            <img :src="getImageUrl(item.uniformImage)" alt="商品图片" class="product-image" @error="onImageError">
            <div class="product-details">
              <h3>{{ item.uniformName || '未知商品' }}</h3>
              <p class="size-label">尺码: {{ item.sizeNameDisplay || item.sizeId }}</p>
            </div>
          </div>
          
          <div class="product-price">¥{{ formatPrice(item.unitPrice) }}</div>
          
          <div class="product-quantity">
            <el-input-number 
              v-model="item.quantity" 
              :min="1" 
              :max="10" 
              size="small"
              @change="(val) => updateItemQuantity(item.cartItemId, val)"
            ></el-input-number>
          </div>
          
          <div class="product-total">¥{{ formatPrice(item.unitPrice * item.quantity) }}</div>
          
          <div class="product-action">
            <el-button type="danger" size="small" @click="removeItem(item.cartItemId)">删除</el-button>
          </div>
        </div>
      </div>
      
      <div class="cart-summary">
        <div class="summary-info">
          <span>商品总数: <strong>{{ totalQuantity }}</strong> 件</span>
          <span>商品总价: <strong class="total-price">¥{{ formatPrice(totalPrice) }}</strong></span>
        </div>
        <div class="cart-actions">
          <el-button @click="clearCart" type="warning" plain>清空购物车</el-button>
          <el-button @click="goToCheckout" type="primary">去结算</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/api/api';
import { getImageFallback, resolveImageUrl } from '@/utils/image';

export default {
  name: 'ShoppingCart',
  data() {
    return {
      cartItems: [],
      loading: true,
      defaultImage: getImageFallback(),
    };
  },
  computed: {
    totalQuantity() {
      return this.cartItems.reduce((sum, item) => sum + (item.quantity || 0), 0);
    },
    totalPrice() {
      return this.cartItems.reduce((sum, item) => {
        const price = item.unitPrice || 0;
        const quantity = item.quantity || 0;
        return sum + (price * quantity);
      }, 0);
    }
  },
  created() {
    this.fetchCartItems();
  },
  methods: {
    async fetchCartItems() {
      this.loading = true;
      try {
        const response = await api.get('/api/cart');
        this.cartItems = response.data || [];
      } catch (error) {
        console.error('Error fetching cart items:', error);
        this.$message.error('获取购物车数据失败，请刷新页面重试');
        this.cartItems = [];
      } finally {
        this.loading = false;
      }
    },
    
    async updateItemQuantity(cartItemId, quantity) {
      try {
        await api.put(`/api/cart/item/${cartItemId}?quantity=${quantity}`);
        // 成功后可以选择重新获取购物车或只更新本地状态
        // this.fetchCartItems(); // 如果希望重新获取全部购物车数据
        this.$message.success('数量已更新');
      } catch (error) {
        console.error('Error updating quantity:', error);
        this.$message.error('更新数量失败，请重试');
        // 恢复原来的数量
        this.fetchCartItems();
      }
    },
    
    async removeItem(cartItemId) {
      try {
        await api.delete(`/api/cart/item/${cartItemId}`);
        // 从本地数组移除项目
        this.cartItems = this.cartItems.filter(item => item.cartItemId !== cartItemId);
        this.$message.success('商品已从购物车移除');
      } catch (error) {
        console.error('Error removing item:', error);
        this.$message.error('移除商品失败，请重试');
      }
    },
    
    async clearCart() {
      this.$confirm('确定要清空购物车吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await api.delete('/api/cart/clear');
          this.cartItems = [];
          this.$message.success('购物车已清空');
        } catch (error) {
          console.error('Error clearing cart:', error);
          this.$message.error('清空购物车失败，请重试');
        }
      }).catch(() => {
        // 用户取消操作
      });
    },
    
    goToCheckout() {
    if (this.cartItems.length === 0) {
        this.$message.warning('购物车为空，请先添加商品');
        return;
    }
    this.$router.push('/checkout'); // 导航到结算页面
    },
    
    formatPrice(price) {
      return Number(price).toFixed(2);
    },
    
    getImageUrl(imagePath) {
      if (!imagePath) return this.defaultImage;

      const primaryImage = typeof imagePath === 'string' && imagePath.includes(',')
        ? imagePath.split(',')[0].trim()
        : imagePath;

      return resolveImageUrl(primaryImage);
    },
    
    onImageError(e) {
      e.target.src = this.defaultImage;
    }
  }
};
</script>

<style scoped>
.shopping-cart-container {
  width: min(1200px, calc(100% - 40px));
  min-height: calc(100vh - var(--navbar-height));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.page-title {
  color: var(--text-primary);
  margin: 0 0 18px;
  font-weight: 700;
  font-size: 26px;
  letter-spacing: 0;
}

.empty-cart {
  text-align: center;
  padding: 64px 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.empty-cart-icon {
  font-size: 80px;
  color: var(--warning);
  margin-bottom: 20px;
}

.empty-cart p {
  color: var(--text-secondary);
  font-size: 18px;
  margin-bottom: 20px;
}

.cart-content {
  border: 1px solid var(--line);
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.cart-header {
  display: flex;
  padding: 18px 20px;
  background: var(--surface-muted);
  font-weight: bold;
  color: var(--text-primary);
  border-bottom: 1px solid var(--line);
}

.cart-column {
  text-align: center;
}

.product-info {
  flex: 3;
  display: flex;
  align-items: center;
  text-align: left;
}

.product-price,
.product-quantity,
.product-total,
.product-action {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-item {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid var(--line);
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  margin-right: 15px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--surface-muted);
}

.product-details h3 {
  margin: 0 0 5px;
  font-size: 16px;
  color: var(--text-primary);
}

.size-label {
  color: var(--text-muted);
  font-size: 14px;
  margin: 0;
}

.cart-summary {
  padding: 24px 20px;
  text-align: right;
  border-top: 1px solid var(--line);
  background: var(--surface-muted);
}

.summary-info {
  margin-bottom: 15px;
  color: var(--text-secondary);
}

.summary-info span {
  margin-left: 30px;
}

.total-price {
  color: var(--brand);
  font-size: 22px;
}

.cart-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}

.loading-container {
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

@media (max-width: 760px) {
  .cart-header {
    display: none;
  }

  .cart-item,
  .cart-summary {
    display: grid;
    gap: 12px;
    text-align: left;
  }

  .product-info,
  .product-price,
  .product-quantity,
  .product-total,
  .product-action {
    justify-content: flex-start;
  }

  .summary-info span {
    display: block;
    margin: 0 0 8px;
  }

  .cart-actions {
    justify-content: stretch;
    flex-direction: column;
  }
}
</style>
