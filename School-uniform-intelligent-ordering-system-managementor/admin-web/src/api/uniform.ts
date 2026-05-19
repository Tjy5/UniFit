import request, { type ApiResponse } from './request'
import type { UniformForm, UniformListPayload } from '@/types/basic-data'

export function listUniforms(params: Record<string, unknown>) {
  return request.get<ApiResponse<UniformListPayload>>('/uniform/list', { params })
}

export function getUniform(id: number) {
  return request.get<ApiResponse<UniformForm>>(`/uniform/${id}`)
}

export function createUniform(data: Record<string, unknown>) {
  return request.post<ApiResponse<null>>('/uniform', data)
}

export function updateUniform(data: Record<string, unknown>) {
  return request.put<ApiResponse<null>>('/uniform', data)
}

export function deleteUniform(ids: string) {
  return request.delete<ApiResponse<null>>(`/uniform/${ids}`)
}
