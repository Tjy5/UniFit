<template>
  <header class="navbar">
    <div class="navbar__shell">
      <router-link to="/home" class="navbar__brand">
        <span class="navbar__mark">SU</span>
        <div class="navbar__brand-copy">
          <strong>校服智订</strong>
          <small>校服预订服务</small>
        </div>
      </router-link>

      <nav class="navbar__nav">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="navbar__nav-link"
          :class="{ 'is-active': isActiveNav(item.to) }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="navbar__actions">
        <router-link to="/cart" class="navbar__pill">
          <span class="navbar__icon" aria-hidden="true">购物车</span>
          <span>购物车</span>
        </router-link>

        <div ref="menuRef" class="navbar__menu">
          <button type="button" class="navbar__account" @click="menuOpen = !menuOpen">
            <span class="navbar__avatar">{{ displayInitial }}</span>
            <span class="navbar__account-copy">
              <strong>{{ displayName }}</strong>
              <small>个人中心</small>
            </span>
          </button>

          <div v-if="menuOpen" class="navbar__dropdown">
            <button type="button" class="navbar__dropdown-item" @click="navigateTo('/profile')">个人资料</button>
            <button type="button" class="navbar__dropdown-item" @click="navigateTo('/my-wishlist')">我的收藏</button>
            <button type="button" class="navbar__dropdown-item" @click="navigateTo('/user-reviews')">我的评价</button>
            <button type="button" class="navbar__dropdown-item" @click="navigateTo('/change-password')">修改密码</button>
            <button type="button" class="navbar__dropdown-item is-danger" @click="handleLogout">退出登录</button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const menuRef = ref(null)

const navItems = [
  { to: '/home', label: '首页' },
  { to: '/style-guide', label: '穿搭指南' },
  { to: '/mall', label: '预定校服' },
  { to: '/my-orders', label: '我的订单' },
]

const displayName = computed(() => localStorage.getItem('userAccount') || '我的账户')
const displayInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())

function navigateTo(path) {
  menuOpen.value = false
  router.push(path)
}

function isActiveNav(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  menuOpen.value = false
  router.push('/login')
}

function handleOutsideClick(event) {
  if (!menuRef.value?.contains(event.target)) {
    menuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  padding: 0;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.navbar__shell {
  width: min(1280px, calc(100% - 32px));
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 10px 0;
  background: transparent;
  border: 0;
  box-shadow: none;
  position: relative;
  overflow: visible;
}

.navbar__shell::before {
  display: none;
}

.navbar__brand,
.navbar__nav,
.navbar__actions {
  position: relative;
  z-index: 1;
}

.navbar__brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.navbar__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--brand);
  color: #fff;
  font-weight: 700;
}

.navbar__brand-copy {
  display: grid;
  gap: 2px;
  color: var(--text-primary);
}

.navbar__brand-copy strong {
  font-size: 18px;
  letter-spacing: 0;
}

.navbar__brand-copy small {
  color: var(--text-secondary);
  font-size: 12px;
  letter-spacing: 0;
}

.navbar__nav {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.navbar__nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 15px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.navbar__nav-link:hover,
.navbar__nav-link.is-active {
  background: var(--brand-soft);
  color: var(--brand);
}

.navbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.navbar__pill,
.navbar__account {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.navbar__pill:hover,
.navbar__account:hover {
  background: var(--brand-soft);
  border-color: var(--brand);
}

.navbar__icon {
  font-size: 0;
}

.navbar__icon::before {
  content: '';
  display: block;
  width: 16px;
  height: 16px;
  border: 2px solid var(--brand);
  border-top: 0;
  border-radius: 0 0 4px 4px;
}

.navbar__menu {
  position: relative;
}

.navbar__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--brand);
  color: #fff;
  font-weight: 700;
}

.navbar__account-copy {
  display: grid;
  gap: 1px;
}

.navbar__account-copy strong {
  font-size: 14px;
}

.navbar__account-copy small {
  color: var(--text-secondary);
  font-size: 11px;
}

.navbar__dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  min-width: 180px;
  padding: 8px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.navbar__dropdown-item {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  background: transparent;
  border-radius: var(--radius-md);
  text-align: left;
  color: var(--text-primary);
  cursor: pointer;
}

.navbar__dropdown-item:hover {
  background: var(--brand-soft);
  color: var(--brand);
}

.navbar__dropdown-item.is-danger {
  color: var(--danger);
}

@media (max-width: 960px) {
  .navbar__shell {
    flex-wrap: wrap;
    justify-content: space-between;
    padding: 10px 0;
  }

  .navbar__nav {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
  }

  .navbar__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
