export interface RecommendationAnalyticsQuery {
  schoolId?: number
  uniformId?: number
  startDate: string
  endDate: string
}

export interface BehaviorAnalyticsQuery {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  source?: string
  startDate: string
  endDate: string
}

export interface RecommendationTrendPoint {
  day: string
  recommendationCount: number
  linkedOrderCount: number
  feedbackCount: number
  fitRate: number
}

export interface RecommendationStats {
  totalRecommendations: number
  linkedOrderCount: number
  linkedOrderCoverage: number
  totalFeedbacks: number
  feedbackCoverage: number
  satisfactionDistribution: Record<string, number>
  fitRate: number
  lowConfidenceCount: number
  lowConfidenceRate: number
  avgConfidenceScore: number
  confidenceBands: Record<string, number>
  trend: RecommendationTrendPoint[]
}

export interface RecommendationExperimentVariantMetrics {
  experimentKey?: string
  experimentVariant: 'A' | 'B'
  totalRecommendations: number
  linkedOrderCount: number
  totalFeedbacks: number
  fitCount: number
  tooLargeCount: number
  tooSmallCount: number
  lowConfidenceCount: number
  avgConfidenceScore: number
  calibrationHitCount: number
  calibrationAppliedCount: number
  recommendationChangedCount: number
  safetyGateSkipCount: number
  fitRate: number
  lowConfidenceRate: number
  feedbackSubmissionRate: number
  recommendationToOrderConversionRate: number
  sizeIssueRate: number
  calibrationHitRate: number
  calibrationApplicationRate: number
  recommendationChangeRate: number
}

export interface RecommendationExperimentMetricsResponse {
  variants: RecommendationExperimentVariantMetrics[]
}

export interface FeedbackDistributionQuery {
  groupBy: 'school' | 'product' | 'size'
  schoolId?: number
  uniformId?: number
  startDate: string
  endDate: string
  limit?: number
  offset?: number
  sortBy?: 'fitRate' | 'totalFeedback'
  sortOrder?: 'asc' | 'desc'
}

export interface FeedbackDistributionGroup {
  groupKey: string
  groupName: string
  totalFeedback: number
  fit: number
  tooLarge: number
  tooSmall: number
  fitRate: number
}

export interface FeedbackDistributionResponse {
  groups: FeedbackDistributionGroup[]
  total: number
  limit: number
  offset: number
}

export interface LowConfidenceHotspotQuery {
  schoolId?: number
  uniformId?: number
  threshold?: number
  limit?: number
  startDate?: string
  endDate?: string
}

export interface LowConfidenceHotspot {
  uniformId?: number
  uniformName?: string
  sizeId?: number
  sizeName?: string
  lowConfidenceCount: number
  totalRecommendations: number
  lowConfidenceRate: number
  avgConfidence: number
}

export interface BehaviorFunnelStage {
  stage: 'browse' | 'detail' | 'recommend' | 'cart' | 'order'
  label: string
  sessionsReached: number
  conversionRate: number
}

export interface BehaviorFunnelResponse {
  stages: BehaviorFunnelStage[]
  totalSessions: number
  sessionInactivityGapMinutes: number
}

export interface RecommendationEntryComparisonRow {
  requestSource: string
  exposureCount: number
  linkedCartCount: number
  linkedOrderCount: number
  adoptedCount: number
  cartConversionRate: number
  orderConversionRate: number
  adoptionRate: number
}

export interface RecommendationEntryComparisonResponse {
  rows: RecommendationEntryComparisonRow[]
}

export interface RecommendationAdoptionStats {
  linkedOrderCount: number
  adoptedCount: number
  sizeChangeCount: number
  withFeedbackCount: number
  withoutFeedbackCount: number
  adoptionRate: number
  sizeChangeRate: number
}

export interface DropOffDistributionQuery extends BehaviorAnalyticsQuery {
  limit?: number
}

export interface DropOffDistributionRow {
  lastActionType: string
  sessionCount: number
  share: number
}

export interface DropOffDistributionResponse {
  rows: DropOffDistributionRow[]
  totalSessions: number
  limit: number
}

export interface StalledCartsQuery {
  schoolId?: number
  gradeId?: number
  uniformId?: number
  startDate: string
  endDate: string
  limit?: number
  offset?: number
}

export interface StalledCartRow {
  userId: number
  userAccount?: string
  uniformId: number
  uniformName?: string
  sizeId: number
  sizeName?: string
  quantity: number
  addedAt: string
  hoursSinceAdd: number
  recommendationLogId?: number
  lastActivityAt?: string
}

export interface StalledCartsResponse {
  records: StalledCartRow[]
  total: number
  limit: number
  offset: number
}
