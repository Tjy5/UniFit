import request, { type ApiResponse } from './request'
import type { ActivityLogListPayload, ActivityLogRecord } from '@/types/basic-data'

export function listActivityLogs(params: Record<string, unknown>) {
  return request.get<ApiResponse<ActivityLogListPayload>>('/activity-logs/list', { params })
}

export function getActivityLog(logId: number) {
  return request.get<ApiResponse<ActivityLogRecord>>(`/activity-logs/${logId}`)
}
