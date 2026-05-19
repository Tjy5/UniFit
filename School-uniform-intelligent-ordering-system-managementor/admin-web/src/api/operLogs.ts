import request, { type ApiResponse } from './request'
import type { OperLogListPayload, OperLogRecord } from '@/types/basic-data'

export function listOperLogs(params: Record<string, unknown>) {
  return request.get<ApiResponse<OperLogListPayload>>('/oper-logs/list', { params })
}

export function getOperLog(logId: number) {
  return request.get<ApiResponse<OperLogRecord>>(`/oper-logs/${logId}`)
}
