export const RECOMMENDATION_SOURCES = [
  { label: '商城弹窗', value: 'mall-home-dialog' },
  { label: '收藏夹', value: 'wishlist' },
  { label: '订单反馈', value: 'order-feedback' },
  { label: '商品详情页', value: 'product-detail' },
  { label: '未知入口', value: 'unknown' },
] as const

export function recommendationSourceLabel(value?: string) {
  return RECOMMENDATION_SOURCES.find((item) => item.value === value)?.label || value || '未知入口'
}
