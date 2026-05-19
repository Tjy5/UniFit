import request, { type ApiResponse } from './request'
import type { ReviewAuditPayload, ReviewListPayload, ReviewRecord } from '@/types/basic-data'

export function listReviews(params: Record<string, unknown>) {
  return request.get<ApiResponse<ReviewListPayload>>('/reviews/list', { params })
}

export function getReview(reviewId: number) {
  return request.get<ApiResponse<ReviewRecord>>(`/reviews/${reviewId}`)
}

export function auditReview(reviewId: number, data: ReviewAuditPayload) {
  return request.put<ApiResponse<null>>(`/reviews/${reviewId}/audit`, data)
}
