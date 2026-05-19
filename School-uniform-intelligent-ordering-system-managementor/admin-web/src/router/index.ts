import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
      guestOnly: true,
    },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: 'Dashboard' },
      },
      {
        path: 'uniforms',
        name: 'uniforms',
        component: () => import('@/views/uniform/UniformList.vue'),
        meta: { title: '校服管理' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/orders/OrderList.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: () => import('@/views/reviews/ReviewList.vue'),
        meta: { title: '评论审核' },
      },
      {
        path: 'schools',
        name: 'schools',
        component: () => import('@/views/schools/SchoolList.vue'),
        meta: { title: '学校管理' },
      },
      {
        path: 'grades',
        name: 'grades',
        component: () => import('@/views/grades/GradeList.vue'),
        meta: { title: '年级管理' },
      },
      {
        path: 'sizes',
        name: 'sizes',
        component: () => import('@/views/sizes/SizeList.vue'),
        meta: { title: '尺码管理' },
      },
      {
        path: 'guides',
        name: 'guides',
        component: () => import('@/views/guides/GuideList.vue'),
        meta: { title: '穿搭指南' },
      },
      {
        path: 'analytics/recommendations',
        name: 'analytics-recommendations',
        component: () => import('@/views/analytics/RecommendationAnalytics.vue'),
        meta: { title: '推荐效果分析' },
      },
      {
        path: 'analytics/feedback-distribution',
        name: 'analytics-feedback-distribution',
        component: () => import('@/views/analytics/FeedbackDistribution.vue'),
        meta: { title: '反馈分布分析' },
      },
      {
        path: 'analytics/low-confidence-hotspots',
        name: 'analytics-low-confidence-hotspots',
        component: () => import('@/views/analytics/LowConfidenceHotspots.vue'),
        meta: { title: '低匹配把握热点' },
      },
      {
        path: 'analytics/behavior-funnel',
        name: 'analytics-behavior-funnel',
        component: () => import('@/views/analytics/BehaviorFunnel.vue'),
        meta: { title: '用户行为漏斗' },
      },
      {
        path: 'analytics/entry-comparison',
        name: 'analytics-entry-comparison',
        component: () => import('@/views/analytics/EntryComparison.vue'),
        meta: { title: '推荐入口对比' },
      },
      {
        path: 'analytics/drop-off',
        name: 'analytics-drop-off',
        component: () => import('@/views/analytics/DropOffDistribution.vue'),
        meta: { title: '流失分布' },
      },
      {
        path: 'analytics/stalled-carts',
        name: 'analytics-stalled-carts',
        component: () => import('@/views/analytics/StalledCarts.vue'),
        meta: { title: '滞留购物车' },
      },
      {
        path: 'analytics/calibration-params',
        name: 'analytics-calibration-params',
        component: () => import('@/views/analytics/CalibrationParameterManagement.vue'),
        meta: { title: '校准参数管理' },
      },
      {
        path: 'supply/inventory-skus',
        name: 'supply-inventory-skus',
        component: () => import('@/views/supply/InventorySkuList.vue'),
        meta: { title: '库存 SKU 管理' },
      },
      {
        path: 'supply/sales',
        name: 'supply-sales',
        component: () => import('@/views/supply/SupplySalesAnalytics.vue'),
        meta: { title: '供需销量分析' },
      },
      {
        path: 'supply/low-stock',
        name: 'supply-low-stock',
        component: () => import('@/views/supply/LowStockAlerts.vue'),
        meta: { title: '低库存告警' },
      },
      {
        path: 'supply/forecast',
        name: 'supply-forecast',
        component: () => import('@/views/supply/ForecastReplenishment.vue'),
        meta: { title: '预测与补货' },
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('@/views/logs/ActivityLogList.vue'),
        meta: { title: '用户行为日志' },
      },
      {
        path: 'oper-logs',
        name: 'oper-logs',
        component: () => import('@/views/logs/OperLogList.vue'),
        meta: { title: '操作日志' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  const token = getToken()
  const requiresAuth = to.meta.requiresAuth !== false
  const guestOnly = Boolean(to.meta.guestOnly)

  if (guestOnly && token) {
    return '/'
  }

  if (!requiresAuth) {
    return true
  }

  if (!token) {
    return {
      path: '/login',
      query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
    }
  }

  if (!authStore.initialized) {
    try {
      await authStore.fetchProfile()
    } catch {
      authStore.resetAuth()
      return {
        path: '/login',
        query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
      }
    }
  }

  return true
})

export default router
