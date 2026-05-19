<template>
  <div id="app" :class="{ 'app-shell--with-navbar': showNavBar }">
    <NavBar v-if="showNavBar" ref="navBarRef" />
    <main class="app-shell__view" :class="{ 'app-shell__view--offset': showNavBar }">
      <router-view />
    </main>
    <Toaster rich-colors position="top-right" :expand="true" />
    <ConfirmDialogHost />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Toaster } from 'vue-sonner'
import NavBar from './components/NavBar.vue'
import ConfirmDialogHost from './components/legacy/ConfirmDialogHost.vue'

const route = useRoute()
const navBarRef = ref(null)
const noNavBarRoutes = ['/login', '/register']

const showNavBar = computed(() => !noNavBarRoutes.includes(route.path))

function syncNavHeight() {
  nextTick(() => {
    const height = navBarRef.value?.$el?.offsetHeight || 76
    document.documentElement.style.setProperty('--navbar-height', `${height}px`)
  })
}

watch(
  () => route.fullPath,
  () => {
    syncNavHeight()
  },
)

watch(showNavBar, () => {
  syncNavHeight()
})

onMounted(() => {
  syncNavHeight()
})
</script>

<style>
#app {
  min-height: 100vh;
  color: var(--mcm-text);
}

.app-shell__view {
  min-height: 100vh;
}

.app-shell__view--offset {
  padding-top: calc(var(--navbar-height) + 18px);
}
</style>
