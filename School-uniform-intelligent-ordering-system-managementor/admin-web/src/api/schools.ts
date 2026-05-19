import request, { type ApiResponse } from './request'
import type { SchoolForm, SchoolListPayload } from '@/types/basic-data'

export function listSchools(params: Record<string, unknown>) {
  return request.get<ApiResponse<SchoolListPayload>>('/schools/list', { params })
}

export function listSchoolOptions() {
  return request.get<ApiResponse<SchoolForm[]>>('/schools/selectList')
}

export function getSchool(schoolId: number) {
  return request.get<ApiResponse<SchoolForm>>(`/schools/${schoolId}`)
}

export function createSchool(data: SchoolForm) {
  return request.post<ApiResponse<null>>('/schools', data)
}

export function updateSchool(data: SchoolForm) {
  return request.put<ApiResponse<null>>('/schools', data)
}

export function deleteSchool(ids: string) {
  return request.delete<ApiResponse<null>>(`/schools/${ids}`)
}
