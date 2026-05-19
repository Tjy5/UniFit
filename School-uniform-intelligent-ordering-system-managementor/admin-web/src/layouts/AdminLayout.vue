<template>
  <div class="page-shell admin-layout">
    <Sidebar />
    <div class="admin-layout__main">
      <Navbar />
      <main class="admin-layout__content">
        <div class="admin-layout__content-inner">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Navbar from './Navbar.vue'
import Sidebar from './Sidebar.vue'
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  gap: 0;
  min-height: 100vh;
  padding: 0;
  min-width: var(--layout-min-width);
  background: var(--app-bg);
}

.admin-layout__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
}

.admin-layout__content {
  flex: 1;
  min-width: 0;
  padding: 22px;
  background: var(--app-bg);
  overflow: auto;
}

.admin-layout__content-inner {
  position: relative;
  z-index: 1;
}
</style>
