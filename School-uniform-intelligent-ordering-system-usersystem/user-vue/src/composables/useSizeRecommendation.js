import { RECOMMENDATION_SOURCES, normalizeRecommendationSource } from '@/constants/recommendationSources'

export function createEmptyRecommendation(message = '') {
  return {
    available: false,
    message,
    confidence: null,
    confidenceLevel: '',
    confidenceMessage: '',
    lowConfidence: false,
    recommendationLogId: null,
    calibrationApplied: false,
    personalizationApplied: false,
    preferenceSummary: '',
    calibrationDetails: {},
    personalizationDetails: {},
    usedDimensions: [],
    reasons: [],
    recommended: null,
    alternatives: [],
  }
}

function normalizeCandidate(candidate) {
  if (!candidate) {
    return null
  }

  return {
    ...candidate,
    reasons: Array.isArray(candidate.reasons) ? candidate.reasons : [],
    dimensionMatches: Array.isArray(candidate.dimensionMatches) ? candidate.dimensionMatches.filter(Boolean) : [],
  }
}

export function normalizeRecommendationResult(result = {}) {
  return {
    ...createEmptyRecommendation(),
    ...result,
    recommendationLogId: result.recommendationLogId ?? null,
    calibrationApplied: Boolean(result.calibrationApplied),
    personalizationApplied: Boolean(result.personalizationApplied),
    preferenceSummary: result.preferenceSummary || '',
    calibrationDetails: result.calibrationDetails && typeof result.calibrationDetails === 'object'
      ? result.calibrationDetails
      : {},
    personalizationDetails: result.personalizationDetails && typeof result.personalizationDetails === 'object'
      ? result.personalizationDetails
      : {},
    usedDimensions: Array.isArray(result.usedDimensions) ? result.usedDimensions : [],
    reasons: Array.isArray(result.reasons) ? result.reasons : [],
    alternatives: Array.isArray(result.alternatives) ? result.alternatives.map(normalizeCandidate).filter(Boolean) : [],
    recommended: normalizeCandidate(result.recommended),
  }
}

export function getRecommendedSizeName(result) {
  return result?.recommended?.sizeName || ''
}

export function buildRecommendationPayload(profile = {}, source = RECOMMENDATION_SOURCES.UNKNOWN, uniformId = undefined) {
  const normalizedSource = normalizeRecommendationSource(source)
  const payload = {
    messageUserAge: profile.messageUserAge ?? undefined,
    messageUserSex: profile.messageUserSex ?? undefined,
    uniformId,
    height: profile.height ?? undefined,
    weight: profile.weight ?? undefined,
    chest: profile.chest ?? undefined,
    waist: profile.waist ?? undefined,
    hip: profile.hip ?? undefined,
    shoulder: profile.shoulder ?? undefined,
    source: normalizedSource,
  }
  return Object.fromEntries(Object.entries(payload).filter(([, value]) => value !== undefined && value !== ''))
}
