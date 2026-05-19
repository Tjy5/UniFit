import axios from 'axios'
import { createRouter, createWebHistory } from 'vue-router'

const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const Home = () => import('../views/Home.vue')
const MallHome = () => import('../views/MallHome.vue')
const UserProfile = () => import('../views/UserProfile.vue')
const ChangePassword = () => import('../views/ChangePassword.vue')
const PaymentSuccess = () => import('../views/PaymentSuccess.vue')
const StyleGuideList = () => import('../views/StyleGuideList.vue')
const StyleGuideDetail = () => import('../views/StyleGuideDetail.vue')
const MyOrders = () => import('../views/MyOrders.vue')
const ShoppingCart = () => import('../views/ShoppingCart.vue')
const CheckoutPage = () => import('../views/Checkout.vue')
const MyWishlist = () => import('../views/MyWishlist.vue')
const ProductDetail = () => import('../views/ProductDetail.vue')
const SubmitReview = () => import('../views/SubmitReview.vue')
const UserReviews = () => import('../views/UserReviews.vue')

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'UserLogin', component: Login },
  { path: '/register', name: 'UserRegister', component: Register },
  { path: '/home', name: 'UserHome', component: Home, meta: { requiresAuth: true } },
  { path: '/mall', name: 'MallHome', component: MallHome, meta: { requiresAuth: true } },
  { path: '/uniforms/:uniformId', name: 'ProductDetail', component: ProductDetail, meta: { requiresAuth: true } },
  { path: '/profile', name: 'UserProfile', component: UserProfile, meta: { requiresAuth: true } },
  { path: '/change-password', name: 'ChangePassword', component: ChangePassword, meta: { requiresAuth: true } },
  { path: '/checkout', name: 'CheckoutPage', component: CheckoutPage, meta: { requiresAuth: true } },
  { path: '/my-wishlist', name: 'MyWishlist', component: MyWishlist, meta: { requiresAuth: true } },
  { path: '/payment-success', name: 'PaymentSuccess', component: PaymentSuccess, meta: { requiresAuth: true } },
  { path: '/style-guide', name: 'StyleGuideList', component: StyleGuideList, meta: { requiresAuth: true } },
  { path: '/style-guide/detail/:id', name: 'StyleGuideDetail', component: StyleGuideDetail, meta: { requiresAuth: true } },
  { path: '/my-orders', name: 'MyOrders', component: MyOrders, meta: { requiresAuth: true } },
  { path: '/cart', name: 'ShoppingCart', component: ShoppingCart, meta: { requiresAuth: true } },
  { path: '/submit-review', name: 'SubmitReview', component: SubmitReview, meta: { requiresAuth: true } },
  { path: '/user-reviews', name: 'UserReviews', component: UserReviews, meta: { requiresAuth: true } },
]

async function validateToken() {
  const token = localStorage.getItem('token')
  if (!token) return false

  try {
    const response = await axios.get('/api/auth/validate', {
      headers: { Authorization: `Bearer ${token}` },
    })
    return response.status === 200
  } catch {
    return false
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    const isValid = await validateToken()
    if (!isValid) {
      localStorage.removeItem('token')
      window.alert('登录已过期，请重新登录')
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

export default router
