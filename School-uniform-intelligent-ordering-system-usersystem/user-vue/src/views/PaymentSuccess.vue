<template>
  <div class="payment-success">
    <div class="success-card">
      <div class="icon-wrapper">
        <i class="el-icon-circle-check success-icon"></i>
      </div>
      <h1>支付成功</h1>
      <p v-if="orderData" class="subtitle">感谢您的购买！订单信息如下：</p>
      <el-card v-if="orderData && uniformDetails" class="order-info-card" shadow="always">
        <ul>
          <li><span>校服名称：</span>{{ uniformDetails.name }}</li>
          <li><span>学校：</span>{{ uniformDetails.school }}</li>
          <li><span>年级：</span>{{ uniformDetails.grade }}</li>
          <li><span>尺码：</span>{{ orderData.size }}</li>
          <li><span>数量：</span>{{ orderData.quantity }}</li>
          <li><span>总价：</span><span class="price">¥{{ orderData.totalPrice }}</span></li>
          <li><span>收货地址：</span>{{ orderData.address }}</li>
          <li><span>创建时间：</span>{{ formatDate(orderData.createTime) }}</li>
        </ul>
      </el-card>
      <el-card v-else-if="orderData && !uniformDetails" class="order-info-card" shadow="always">
        正在加载校服信息...
      </el-card>
      <p v-else class="fail-tip">订单信息不可用，请返回重试</p>
      <el-button class="back-mall-btn" type="primary" @click="$router.push('/mall')">返回商城</el-button>
    </div>
  </div>
</template>

<script>
import api from "../api/api";
export default {
  name: "PaymentSuccess",
  data() {
    return {
      orderData: null,
      uniforms: [],
      uniformDetails: null,
    };
  },
  async created() {
    const orderDataStr = this.$route.query.orderData;
    if (orderDataStr) {
      this.orderData = JSON.parse(orderDataStr);
      await this.fetchUniforms();
      this.matchUniformDetails();
    }
  },
  methods: {
    async fetchUniforms() {
      try {
        const response = await api.get("/api/s-uniform/active");
        this.uniforms = response.data;
      } catch (error) {
        console.error("Error fetching uniforms:", error);
        this.$message.error("获取校服数据失败，请重试");
      }
    },
    matchUniformDetails() {
      if (this.orderData && this.uniforms.length) {
        this.uniformDetails = this.uniforms.find(
          (uniform) => uniform.id.toString() === this.orderData.uniform
        );
        if (!this.uniformDetails) {
          console.error("No matching uniform found for ID:", this.orderData.uniform);
          this.$message.warning("未找到对应的校服信息");
        }
      }
    },
    formatDate(date) {
      if (!date) return "暂无时间";
      const d = new Date(date);
      return d.toLocaleString("zh-CN");
    },
  },
};
</script>

<style scoped>
.payment-success {
  min-height: calc(100vh - var(--navbar-height));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.success-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 40px 36px 32px 36px;
  max-width: 430px;
  width: 100%;
  text-align: center;
  position: relative;
  border: 1px solid var(--line);
}
.icon-wrapper {
  margin-bottom: 14px;
}
.success-icon {
  font-size: 64px;
  color: var(--success);
}
h1 {
  color: var(--text-primary);
  font-size: 2rem;
  margin-bottom: 8px;
  font-weight: bold;
  letter-spacing: 0;
}
.subtitle {
  color: var(--text-secondary);
  font-size: 1.09rem;
  margin-bottom: 15px;
}
.order-info-card {
  text-align: left;
  margin: 0 auto 18px auto;
  background: var(--surface-muted);
  border-radius: var(--radius-lg);
  padding: 20px 22px 12px 22px;
  font-size: 15px;
  box-shadow: none;
  border: 1px solid var(--line);
}
.order-info-card ul {
  list-style: none;
  padding: 0 0 0 3px;
  margin: 0;
}
.order-info-card li {
  margin: 8px 0;
  border-left: 4px solid var(--brand);
  padding-left: 9px;
  color: var(--text-primary);
  line-height: 1.7;
}
.order-info-card span {
  font-weight: 500;
  color: var(--text-secondary);
}
.price {
  color: var(--brand);
  font-weight: bold;
  font-size: 1.14em;
}
.fail-tip {
  color: var(--danger);
  margin-bottom: 20px;
}
.back-mall-btn {
  margin-top: 15px;
  width: 160px;
  height: 40px;
  font-size: 16px;
  border-radius: var(--radius-md);
  letter-spacing: 0;
}
</style>
