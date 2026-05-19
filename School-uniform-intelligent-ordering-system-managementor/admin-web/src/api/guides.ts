import request, { type ApiResponse } from './request'
import type { GuideForm, GuideListPayload } from '@/types/basic-data'

export function listGuides(params: Record<string, unknown>) {
  return request.get<ApiResponse<GuideListPayload>>('/guides/list', { params })
}

export function getGuide(id: number) {
  return request.get<ApiResponse<GuideForm>>(`/guides/${id}`)
}

export function createGuide(data: Record<string, unknown>) {
  return request.post<ApiResponse<null>>('/guides', data)
}

export function updateGuide(data: Record<string, unknown>) {
  return request.put<ApiResponse<null>>('/guides', data)
}

export function deleteGuide(ids: string) {
  return request.delete<ApiResponse<null>>(`/guides/${ids}`)
}
