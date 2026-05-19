export type CalibrationScopeType = 'GLOBAL' | 'SCHOOL' | 'PRODUCT'
export type CalibrationStatus = 'ACTIVE' | 'OBSERVING' | 'DISABLED'
export type CalibrationConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW'
export type CalibrationAttributionStatus = 'TRACEABLE' | 'UNKNOWN'

export interface CalibrationPagePayload<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface CalibrationParamQuery {
  pageNum?: number
  pageSize?: number
  scopeType?: CalibrationScopeType
  scopeId?: string
  targetSizeId?: number
  enabled?: boolean
  status?: CalibrationStatus
  confidenceLevel?: CalibrationConfidenceLevel
  version?: number
}

export interface CalibrationParamListItem {
  paramId: number
  scopeType: CalibrationScopeType
  scopeId?: string
  scopeName?: string
  targetSizeId?: number
  targetSizeName?: string
  calibrationType: string
  adjustmentValue?: number
  isManual?: boolean
  enabled?: boolean
  status?: CalibrationStatus
  sampleSize?: number
  uniqueUserCount?: number
  feedbackDistribution?: unknown
  confidenceLevel?: CalibrationConfidenceLevel
  effectiveFrom?: string
  effectiveUntil?: string
  version?: number
  latestVersion?: boolean
  effectiveNow?: boolean
  shadowedByHigherVersion?: boolean
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export type CalibrationParamDetail = CalibrationParamListItem
export type CalibrationParamVersion = CalibrationParamListItem

export interface CalibrationAuditRecord {
  auditId: number
  paramId: number
  action: string
  oldValue?: unknown
  newValue?: unknown
  reason?: string
  operator?: string
  createTime?: string
}

export interface CalibrationLifecyclePayload {
  reason: string
}

export interface CalibrationRollbackPayload extends CalibrationLifecyclePayload {
  sourceParamId?: number
}

export interface CalibrationImpactQuery {
  version?: number
  schoolId?: number
  uniformId?: number
  sizeId?: number
  startDate?: string
  endDate?: string
  limit?: number
  offset?: number
}

export interface CalibrationImpactSummary {
  appliedCount: number
  bestSizeChangedCount: number
  avgBaseBestScore?: number
  avgCalibratedBestScore?: number
  avgScoreDelta?: number
}

export interface CalibrationImpactParamHit {
  sizeId?: number
  paramId?: number
  version?: number
  calibrationType?: string
  source?: string
  offset?: number
  effectiveOffset?: number
  sampleSize?: number
  uniqueUserCount?: number
  confidenceLevel?: CalibrationConfidenceLevel
  status?: CalibrationStatus
}

export interface CalibrationImpactRecord {
  logId: number
  createTime?: string
  schoolId?: number
  schoolName?: string
  uniformId?: number
  uniformName?: string
  recommendedSizeId?: number
  recommendedSizeName?: string
  baseBestSizeId?: number
  baseBestSizeName?: string
  baseBestScore?: number
  calibratedBestSizeId?: number
  calibratedBestSizeName?: string
  calibratedBestScore?: number
  finalBestSizeId?: number
  finalBestSizeName?: string
  finalBestScore?: number
  scoreDelta?: number
  attributionStatus?: CalibrationAttributionStatus
  matchedParams?: CalibrationImpactParamHit[]
}

export interface CalibrationImpactResponse {
  summary: CalibrationImpactSummary
  records: CalibrationImpactRecord[]
  total: number
  limit: number
  offset: number
}
