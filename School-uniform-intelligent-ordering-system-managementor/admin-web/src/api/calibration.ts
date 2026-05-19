import request, { type ApiResponse } from './request'
import type {
  CalibrationAuditRecord,
  CalibrationImpactQuery,
  CalibrationImpactResponse,
  CalibrationLifecyclePayload,
  CalibrationPagePayload,
  CalibrationParamDetail,
  CalibrationParamListItem,
  CalibrationParamQuery,
  CalibrationParamVersion,
  CalibrationRollbackPayload,
} from '@/types/calibration'

const baseUrl = '/calibration/params'

export function listCalibrationParams(params: CalibrationParamQuery) {
  return request.get<ApiResponse<CalibrationPagePayload<CalibrationParamListItem>>>(`${baseUrl}/list`, { params })
}

export function getCalibrationParam(paramId: number) {
  return request.get<ApiResponse<CalibrationParamDetail>>(`${baseUrl}/${paramId}`)
}

export function listCalibrationParamVersions(paramId: number) {
  return request.get<ApiResponse<CalibrationParamVersion[]>>(`${baseUrl}/${paramId}/versions`)
}

export function listCalibrationParamAudits(paramId: number) {
  return request.get<ApiResponse<CalibrationAuditRecord[]>>(`${baseUrl}/${paramId}/audits`)
}

export function getCalibrationImpact(paramId: number, params: CalibrationImpactQuery) {
  return request.get<ApiResponse<CalibrationImpactResponse>>(`${baseUrl}/${paramId}/impact`, { params })
}

export function enableCalibrationParam(paramId: number, data: CalibrationLifecyclePayload) {
  return request.post<ApiResponse<CalibrationParamDetail>>(`${baseUrl}/${paramId}/enable`, data)
}

export function disableCalibrationParam(paramId: number, data: CalibrationLifecyclePayload) {
  return request.post<ApiResponse<CalibrationParamDetail>>(`${baseUrl}/${paramId}/disable`, data)
}

export function rollbackCalibrationParam(paramId: number, data: CalibrationRollbackPayload) {
  return request.post<ApiResponse<CalibrationParamDetail>>(`${baseUrl}/${paramId}/rollback`, data)
}
