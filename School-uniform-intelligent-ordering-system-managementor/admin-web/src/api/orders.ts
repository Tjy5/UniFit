import request, { type ApiResponse } from './request'
import type {
  OrderFulfillmentCommandPayload,
  OrderListPayload,
  OrderRecord,
  OrderStatusLogRecord,
  OrderStatusPayload,
} from '@/types/basic-data'

export function listOrders(params: Record<string, unknown>) {
  return request.get<ApiResponse<OrderListPayload>>('/orders/list', { params })
}

export function getOrder(id: number) {
  return request.get<ApiResponse<OrderRecord>>(`/orders/${id}`)
}

export function updateOrderStatus(id: number, data: OrderStatusPayload) {
  return request.put<ApiResponse<null>>(`/orders/${id}/status`, data)
}

export function executeOrderFulfillmentCommand(id: number, data: OrderFulfillmentCommandPayload) {
  return request.post<ApiResponse<OrderRecord>>(`/orders/${id}/fulfillment-commands`, data)
}

export function getOrderStatusLogs(id: number) {
  return request.get<ApiResponse<OrderStatusLogRecord[]>>(`/orders/${id}/status-logs`)
}

export function listOrderAnomalies(params: Record<string, unknown>) {
  return request.get<ApiResponse<OrderListPayload>>('/orders/anomalies', { params })
}
