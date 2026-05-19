import request, { type ApiResponse } from './request'
import type {
  InventoryMovementPayload,
  InventoryMovementRecord,
  InventorySkuPayload,
  InventorySkuRecord,
  LowStockAlertRecord,
  PagePayload,
  ReplenishmentSuggestionRecord,
  SupplyForecastRecord,
  SupplySalesAnalyticsRecord,
  SupplyTrendPointRecord,
} from '@/types/basic-data'

export function listInventorySkus(params: Record<string, unknown>) {
  return request.get<ApiResponse<PagePayload<InventorySkuRecord>>>('/inventory/sku/list', { params })
}

export function exportInventorySkus(params: Record<string, unknown>) {
  return request.post<Blob>('/inventory/sku/export', undefined, { params, responseType: 'blob' })
}

export function createInventorySku(data: InventorySkuPayload) {
  return request.post<ApiResponse<InventorySkuRecord>>('/inventory/sku', data)
}

export function updateInventorySku(data: InventorySkuPayload) {
  return request.put<ApiResponse<InventorySkuRecord>>('/inventory/sku', data)
}

export function deleteInventorySkus(ids: string) {
  return request.delete<ApiResponse<null>>(`/inventory/sku/${ids}`)
}

export function listInventoryMovements(params: Record<string, unknown>) {
  return request.get<ApiResponse<InventoryMovementRecord[]>>('/inventory/movement/list', { params })
}

export function applyInventoryMovement(data: InventoryMovementPayload) {
  return request.post<ApiResponse<InventoryMovementRecord>>('/inventory/movement', data)
}

export function listInventoryLowStockAlerts(params: Record<string, unknown>) {
  return request.get<ApiResponse<LowStockAlertRecord[]>>('/inventory/alerts/low-stock', { params })
}

export function exportInventoryLowStockAlerts(params: Record<string, unknown>) {
  return request.post<Blob>('/inventory/alerts/low-stock/export', undefined, { params, responseType: 'blob' })
}

export function getSupplySales(params: Record<string, unknown>) {
  return request.get<ApiResponse<SupplySalesAnalyticsRecord[]>>('/analytics/supply/sales', { params })
}

export function exportSupplySales(params: Record<string, unknown>) {
  return request.post<Blob>('/analytics/supply/sales/export', undefined, { params, responseType: 'blob' })
}

export function getSupplyTrend(params: Record<string, unknown>) {
  return request.get<ApiResponse<SupplyTrendPointRecord[]>>('/analytics/supply/trend', { params })
}

export function getSupplyLowStock(params: Record<string, unknown>) {
  return request.get<ApiResponse<LowStockAlertRecord[]>>('/analytics/supply/low-stock', { params })
}

export function exportSupplyLowStock(params: Record<string, unknown>) {
  return request.post<Blob>('/analytics/supply/low-stock/export', undefined, { params, responseType: 'blob' })
}

export function getSupplyForecast(params: Record<string, unknown>) {
  return request.get<ApiResponse<SupplyForecastRecord[]>>('/analytics/supply/forecast', { params })
}

export function exportSupplyForecast(params: Record<string, unknown>) {
  return request.post<Blob>('/analytics/supply/forecast/export', undefined, { params, responseType: 'blob' })
}

export function getReplenishmentSuggestions(params: Record<string, unknown>) {
  return request.get<ApiResponse<ReplenishmentSuggestionRecord[]>>('/analytics/supply/replenishment', { params })
}

export function exportReplenishmentSuggestions(params: Record<string, unknown>) {
  return request.post<Blob>('/analytics/supply/replenishment/export', undefined, { params, responseType: 'blob' })
}
