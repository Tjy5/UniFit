import request, { type ApiResponse } from './request'
import type { GradeForm, GradeListPayload } from '@/types/basic-data'

export function listGrades(params: Record<string, unknown>) {
  return request.get<ApiResponse<GradeListPayload>>('/grades/list', { params })
}

export function listGradeOptions(params?: { schoolId?: number }) {
  return request.get<ApiResponse<GradeForm[]>>('/grades/selectList', { params })
}

export function getGrade(gradeId: number) {
  return request.get<ApiResponse<GradeForm>>(`/grades/${gradeId}`)
}

export function createGrade(data: GradeForm) {
  return request.post<ApiResponse<null>>('/grades', data)
}

export function updateGrade(data: GradeForm) {
  return request.put<ApiResponse<null>>('/grades', data)
}

export function deleteGrade(ids: string) {
  return request.delete<ApiResponse<null>>(`/grades/${ids}`)
}
