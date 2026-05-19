<template>
  <header class="navbar">
    <div class="navbar__left">
      <button class="navbar__toggle" type="button" @click="appStore.toggleSidebar()">
        <el-icon size="20">
          <component :is="appStore.sidebarCollapsed ? Expand : Fold" />
        </el-icon>
      </button>
      <div class="navbar__headline">
        <span class="navbar__label">校服订购运营台</span>
        <Breadcrumb />
      </div>
    </div>

    <div class="navbar__right">
      <div class="navbar__summary">
        <span class="navbar__summary-label">当前账号</span>
        <strong>{{ authStore.displayName }}</strong>
      </div>
      <el-dropdown trigger="click">
        <button class="navbar__profile" type="button">
          <span>{{ initials }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Expand, Fold, SwitchButton } from '@element-plus/icons-vue'
import Breadcrumb from './Breadcrumb.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const initials = computed(() => authStore.displayName.slice(0, 1).toUpperCase())

async function handleLogout() {
  await authStore.logoutAction()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<style scoped lang="scss">
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: var(--layout-navbar);
  padding: 8px 20px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
}

.navbar__left,
.navbar__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.navbar__toggle,
.navbar__profile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  cursor: pointer;
}

.navbar__toggle {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  color: var(--brand);
  background: var(--brand-soft);
}

.navbar__headline {
  display: grid;
  gap: 2px;
}

.navbar__label {
  font-size: 11px;
  letter-spacing: 0;
  color: var(--text-muted);
}

.navbar__summary {
  display: grid;
  gap: 1px;
  text-align: right;
  padding: 0 10px 0 0;
}

.navbar__summary-label {
  font-size: 11px;
  color: var(--text-muted);
}

.navbar__profile {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  color: #fff;
  background: var(--brand);
}

@media (max-width: 768px) {
  .navbar {
    flex-wrap: wrap;
    padding: 12px 16px;
  }

  .navbar__summary {
    display: none;
  }
}
</style>
