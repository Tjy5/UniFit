import request, { type ApiResponse } from './request'
import type {
  BehaviorAnalyticsQuery,
  BehaviorFunnelResponse,
  DropOffDistributionQuery,
  DropOffDistributionResponse,
  FeedbackDistributionQuery,
  FeedbackDistributionResponse,
  LowConfidenceHotspotQuery,
  LowConfidenceHotspot,
  RecommendationAdoptionStats,
  RecommendationAnalyticsQuery,
  RecommendationEntryComparisonResponse,
  RecommendationExperimentMetricsResponse,
  RecommendationStats,
  StalledCartsQuery,
  StalledCartsResponse,
} from '@/types/analytics'

export function getRecommendationStats(params: RecommendationAnalyticsQuery) {
  return request.get<ApiResponse<RecommendationStats>>('/analytics/recommendation-stats', { params })
}

export function getRecommendationExperimentMetrics(params: RecommendationAnalyticsQuery) {
  return request.get<ApiResponse<RecommendationExperimentMetricsResponse>>('/analytics/recommendation-experiment', { params })
}

export function getFeedbackDistribution(params: FeedbackDistributionQuery) {
  return request.get<ApiResponse<FeedbackDistributionResponse>>('/analytics/feedback-distribution', { params })
}

export function getLowConfidenceHotspots(params: LowConfidenceHotspotQuery) {
  return request.get<ApiResponse<LowConfidenceHotspot[]>>('/analytics/low-confidence-hotspots', { params })
}

export function getBehaviorFunnel(params: BehaviorAnalyticsQuery) {
  return request.get<ApiResponse<BehaviorFunnelResponse>>('/analytics/behavior-funnel', { params })
}

export function getRecommendationEntryComparison(params: BehaviorAnalyticsQuery) {
  return request.get<ApiResponse<RecommendationEntryComparisonResponse>>('/analytics/recommendation-entry-comparison', { params })
}

export function getRecommendationAdoption(params: BehaviorAnalyticsQuery) {
  return request.get<ApiResponse<RecommendationAdoptionStats>>('/analytics/recommendation-adoption', { params })
}

export function getBehaviorDropOff(params: DropOffDistributionQuery) {
  return request.get<ApiResponse<DropOffDistributionResponse>>('/analytics/behavior-drop-off', { params })
}

export function getStalledCarts(params: StalledCartsQuery) {
  return request.get<ApiResponse<StalledCartsResponse>>('/analytics/stalled-carts', { params })
}
