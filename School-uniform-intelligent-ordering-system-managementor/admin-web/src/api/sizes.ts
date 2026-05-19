import request, { type ApiResponse } from './request'
import type { SizeForm, SizeListPayload } from '@/types/basic-data'

export function listSizes(params: Record<string, unknown>) {
  return request.get<ApiResponse<SizeListPayload>>('/sizes/list', { params })
}

export function getSize(id: number) {
  return request.get<ApiResponse<SizeForm>>(`/sizes/${id}`)
}

export function createSize(data: SizeForm) {
  return request.post<ApiResponse<null>>('/sizes', data)
}

export function updateSize(data: SizeForm) {
  return request.put<ApiResponse<null>>('/sizes', data)
}

export function deleteSize(ids: string) {
  return request.delete<ApiResponse<null>>(`/sizes/${ids}`)
}
