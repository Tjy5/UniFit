export const RECOMMENDATION_SOURCES = Object.freeze({
  MALL_HOME_DIALOG: 'mall-home-dialog',
  WISHLIST: 'wishlist',
  ORDER_FEEDBACK: 'order-feedback',
  PRODUCT_DETAIL: 'product-detail',
  UNKNOWN: 'unknown',
})

export const RECOMMENDATION_SOURCE_OPTIONS = Object.freeze(Object.values(RECOMMENDATION_SOURCES))

export function normalizeRecommendationSource(source) {
  return RECOMMENDATION_SOURCE_OPTIONS.includes(source) ? source : RECOMMENDATION_SOURCES.UNKNOWN
}
