<template>
  <div class="home-page mcm-page-shell">
    <section class="home-hero">
      <div class="home-hero__copy">
        <span class="home-hero__eyebrow">校服订购</span>
        <h1>快速完成本学期校服预订</h1>
        <p>按学校和年级查找校服，加入购物车后完成结算，也可以随时查看订单与尺码推荐。</p>
        <div class="home-hero__actions">
          <router-link to="/mall" class="home-button home-button--primary">预订校服</router-link>
          <router-link to="/my-orders" class="home-button">查看订单</router-link>
        </div>
      </div>
      <div class="home-hero__status">
        <article v-for="metric in heroMetrics" :key="metric.label" class="home-metric">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </div>
    </section>

    <section class="home-section">
      <div class="section-heading">
        <div>
          <h2>常用功能</h2>
          <p>把订购、查询、尺码参考和穿搭内容放在同一入口，减少来回查找。</p>
        </div>
      </div>
      <div class="entry-grid">
        <router-link
          v-for="entry in quickEntries"
          :key="entry.to"
          :to="entry.to"
          class="entry-card"
          :class="`entry-card--${entry.tone}`"
        >
          <span class="entry-card__badge">{{ entry.badge }}</span>
          <p class="entry-card__eyebrow">{{ entry.eyebrow }}</p>
          <h3>{{ entry.title }}</h3>
          <p class="entry-card__description">{{ entry.description }}</p>
          <div class="entry-card__footer">
            <span class="entry-card__link">{{ entry.actionLabel }}</span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
const heroMetrics = [
  { label: '商品查找', value: '学校 / 年级筛选' },
  { label: '尺码参考', value: '下单前查看推荐' },
  { label: '订单进度', value: '支付与物流可追踪' },
]

const quickEntries = [
  { to: '/mall', badge: '01', eyebrow: '在线选购', title: '预订校服', description: '查看各学校、各年级的校服款式与价格，按需要选择商品。', actionLabel: '进入商城', tone: 'primary' },
  { to: '/my-orders', badge: '02', eyebrow: '订单查询', title: '我的订单', description: '查看付款状态、发货进度和历史订单，售后回访更清晰。', actionLabel: '查看订单', tone: 'info' },
  { to: '/profile', badge: '03', eyebrow: '尺码资料', title: '个人资料', description: '维护身高、体重等信息，为尺码推荐提供参考。', actionLabel: '更新资料', tone: 'success' },
  { to: '/style-guide', badge: '04', eyebrow: '穿搭参考', title: '穿搭指南', description: '浏览不同场景下的校服搭配图片和说明。', actionLabel: '查看指南', tone: 'warning' },
]
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - var(--navbar-height));
  padding: 24px 0 56px;
}

.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  gap: 20px;
  align-items: stretch;
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.home-hero__copy {
  display: grid;
  align-content: center;
  gap: 14px;
}

.home-hero__eyebrow,
.entry-card__eyebrow {
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.home-hero h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 0;
}

.home-hero p,
.section-heading p,
.entry-card__description {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}

.home-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  color: var(--text-primary);
  background: var(--surface);
  font-weight: 700;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.home-button:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-soft);
}

.home-button--primary {
  color: #fff;
  border-color: var(--brand);
  background: var(--brand);
}

.home-button--primary:hover {
  color: #fff;
  background: var(--brand-hover);
}

.home-hero__status {
  display: grid;
  gap: 12px;
}

.home-metric {
  padding: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface-muted);
}

.home-metric span {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.home-metric strong {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 17px;
}

.home-section {
  margin-top: 24px;
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.25;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.entry-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 220px;
  padding: 20px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.entry-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-md);
}

.entry-card__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  color: #fff;
  font-weight: 700;
  background: var(--brand);
}

.entry-card--info .entry-card__badge {
  background: var(--info);
}

.entry-card--success .entry-card__badge {
  background: var(--success);
}

.entry-card--warning .entry-card__badge {
  background: var(--warning);
}

.entry-card__eyebrow {
  margin: 2px 0 0;
}

.entry-card h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 20px;
  line-height: 1.25;
}

.entry-card__footer {
  margin-top: auto;
  display: flex;
  align-items: center;
}

.entry-card__link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--brand);
  font-weight: 700;
}

.entry-card__link::after {
  content: '>';
  font-size: 1rem;
  line-height: 1;
}

@media (max-width: 1120px) {
  .home-hero,
  .entry-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .home-page {
    padding-bottom: 40px;
  }

  .home-hero,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .home-hero {
    padding: 22px;
  }

  .home-hero h1 {
    font-size: 28px;
  }
}
</style>
