import request, { type ApiResponse } from './request'
import type { DashboardStats } from '@/types/basic-data'

export function getDashboardStats() {
  return request.get<ApiResponse<DashboardStats>>('/dashboard/stats')
}
