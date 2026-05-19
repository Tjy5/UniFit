import axios from 'axios'

const api = axios.create({
  timeout: 5000,
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number' && 'data' in body) {
      response.apiCode = body.code
      response.apiMessage = body.message
      response.data = body.data
    }
    return response
  },
  (error) => {
    if (error.response) {
      const message = error.response.data?.message || error.response.data
      switch (error.response.status) {
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          if (window.location.pathname !== '/login') {
            console.error('401 未授权或Token已过期，正在重定向到登录页面')
            setTimeout(() => {
              window.location.href = '/login'
            }, 1500)
          }
          break
        case 403:
          console.error('403 禁止访问')
          break
        case 404:
          console.error('404 资源未找到:', error.config?.url)
          break
        case 500:
          console.error('500 服务器内部错误')
          break
        default:
          console.error(`错误 ${error.response.status}:`, message)
      }
    } else if (error.request) {
      console.error('网络错误或无响应:', error.request)
    } else {
      console.error('请求配置错误:', error.message)
    }
    return Promise.reject(error)
  },
)

api.getSchoolOptions = () => api.get('/api/s-schools/selectList')

api.getGradeOptionsBySchool = (schoolId) => api.get('/api/s-grades/listBySchool', { params: { schoolId } })

api.getUserMessage = (userId) => api.get(`/api/user-message/${userId}`)

api.updateUserMessage = (payload) => api.put('/api/user-message/update', payload)

api.getAllSizes = () => api.get('/api/s-sizes/all')

api.getUniformDetail = (uniformId) => api.get(`/api/s-uniform/${uniformId}`)

api.getSizeRecommendation = (payload = {}) => api.post('/api/size-recommendations/recommend', payload)

api.getSizePreference = (params = {}) => api.get('/api/size-preferences', { params })

api.saveSizePreference = (payload = {}) => api.put('/api/size-preferences', payload)

api.getOrderItemSizeFeedback = (orderItemId) => api.get(`/api/size-feedback/order-items/${orderItemId}`)

api.getOrderItemRecommendationSnapshot = (orderItemId) =>
  api.get(`/api/size-feedback/order-items/${orderItemId}/recommendation`)

api.saveOrderItemSizeFeedback = (orderItemId, payload) => api.put(`/api/size-feedback/order-items/${orderItemId}`, payload)

api.changePassword = (payload) => api.put('/api/users/updatePassword', payload)

api.getCartItems = () => api.get('/api/cart')

api.addCartItem = (params) => api.post('/api/cart/add', null, { params })

api.createOrder = (payload) => api.post('/api/s-orders/create', payload)

api.getMyOrders = () => api.get('/api/s-orders/user')

api.updateOrderStatus = (orderId, payload) => api.put(`/api/s-orders/${orderId}/status`, payload)

api.executeOrderFulfillmentCommand = (orderId, command, reason) =>
  api.post(`/api/s-orders/${orderId}/fulfillment-commands`, { command, reason })

api.getOrderStatusLogs = (orderId) => api.get(`/api/s-orders/${orderId}/status-logs`)

api.listAddresses = () => api.get('/api/s-addresses')

api.getAddress = (addressId) => api.get(`/api/s-addresses/${addressId}`)

api.createAddress = (addressData) => api.post('/api/s-addresses', addressData)

api.updateAddress = (addressId, addressData) => api.put(`/api/s-addresses/${addressId}`, addressData)

api.deleteAddress = (addressId) => api.delete(`/api/s-addresses/${addressId}`)

api.setDefaultAddress = (addressId) => api.put(`/api/s-addresses/${addressId}/default`)

api.addToWishlist = (uniformId) => api.post('/api/s-wishlist/add', null, { params: { uniformId } })

api.removeFromWishlist = (uniformId) => api.delete(`/api/s-wishlist/remove/${uniformId}`)

api.getUserWishlist = () => api.get('/api/s-wishlist/list')

api.getMySubmittedReviews = (pageNum = 1, pageSize = 10) =>
  api.get('/api/s-reviews/user', {
    params: { pageNum, pageSize },
  })

api.submitReview = (reviewData) => {
  const payload = {
    uniformId: reviewData.uniformId,
    orderItemId: reviewData.orderItemId,
    rating: reviewData.rating,
    content: reviewData.content,
    isAnonymous: reviewData.isAnonymous,
  }

  return api.post('/api/s-reviews', payload)
}

api.updateReview = (reviewId, payload) => api.put(`/api/s-reviews/${reviewId}`, payload)

api.deleteReview = (reviewId) => api.delete(`/api/s-reviews/${reviewId}`)

export default api
