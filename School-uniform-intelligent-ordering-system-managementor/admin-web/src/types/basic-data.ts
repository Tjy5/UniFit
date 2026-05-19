export interface SchoolForm {
  schoolId?: number
  schoolName: string
  schoolAddress?: string
  contactPerson?: string
  contactPhone?: string
  remark?: string
}

export interface GradeForm {
  gradeId?: number
  gradeName: string
  schoolId?: number
  schoolName?: string
  remark?: string
}

export interface SizeForm {
  id?: number
  sizeName: string
  minHeight?: number
  maxHeight?: number
  minWeight?: number
  maxWeight?: number
  minChest?: number
  maxChest?: number
  minWaist?: number
  maxWaist?: number
  minHip?: number
  maxHip?: number
  minShoulder?: number
  maxShoulder?: number
  remark?: string
}

export interface SchoolListPayload {
  records: SchoolForm[]
  total: number
  pageNum: number
  pageSize: number
}

export interface GradeListPayload {
  records: GradeForm[]
  total: number
  pageNum: number
  pageSize: number
}

export interface SizeListPayload {
  records: SizeForm[]
  total: number
  pageNum: number
  pageSize: number
}

export interface UniformForm {
  id?: number
  name: string
  intro?: string
  image?: string
  imageList: string[]
  price?: number
  status?: number
  averageRating?: number
  reviewCount?: number
  schoolId?: number
  schoolName?: string
  gradeId?: number
  gradeName?: string
  remark?: string
}

export interface UniformListPayload {
  records: UniformForm[]
  total: number
  pageNum: number
  pageSize: number
}

export interface OrderItem {
  orderItemId: number
  uniformId?: number
  sizeId?: number
  uniformNameSnapshot?: string
  sizeNameSnapshot?: string
  quantity?: number
  unitPriceSnapshot?: number
  itemTotalPrice?: number
  imageSnapshot?: string
  reviewId?: number | null
}

export interface OrderRecord {
  id: number
  userId?: number
  userAccount?: string
  recipientName?: string
  phoneNumber?: string
  fullAddress?: string
  orderDate?: string
  totalPrice?: number
  addressId?: number
  paymentStatus?: string
  shippingStatus?: string
  status?: number
  remark?: string
  orderItems?: OrderItem[]
  allowedFulfillmentActions?: string[]
  anomalySummary?: OrderAnomalySummary
}

export interface OrderListPayload {
  records: OrderRecord[]
  total: number
  pageNum: number
  pageSize: number
}

export interface OrderStatusPayload {
  status?: number
  paymentStatus?: string
  shippingStatus?: string
}

export interface OrderFulfillmentCommandPayload {
  command: string
  reason?: string
}

export interface OrderStatusLogRecord {
  id: number
  orderId: number
  eventType?: string
  actorType?: string
  actorId?: number
  fromOrderStatus?: number
  toOrderStatus?: number
  fromPaymentStatus?: string
  toPaymentStatus?: string
  fromShippingStatus?: string
  toShippingStatus?: string
  reason?: string
  contextJson?: string
  createTime?: string
}

export interface OrderAnomalySummary {
  abnormal: boolean
  reasons: string[]
}

export interface ReviewRecord {
  reviewId: number
  userId?: number
  userAccount?: string
  uniformId?: number
  uniformName?: string
  orderItemId?: number
  rating?: number
  content?: string
  images?: string
  status?: number
  isAnonymous?: boolean
  createTime?: string
}

export interface ReviewListPayload {
  records: ReviewRecord[]
  total: number
  pageNum: number
  pageSize: number
}

export interface ReviewAuditPayload {
  status: number
}

export interface GuideForm {
  id?: number
  uniformId?: number
  uniformName?: string
  title: string
  content: string
  image?: string
  imageList: string[]
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface GuideListPayload {
  records: GuideForm[]
  total: number
  pageNum: number
  pageSize: number
}

export interface ActivityLogRecord {
  logId: number
  userId?: number
  userAccount?: string
  sessionId?: string
  actionType?: string
  targetType?: string
  targetId?: string
  logDetail?: string
  ipAddress?: string
  userAgent?: string
  requestUrl?: string
  requestMethod?: string
  logTime?: string
}

export interface ActivityLogListPayload {
  records: ActivityLogRecord[]
  total: number
  pageNum: number
  pageSize: number
}

export interface DashboardTrendPoint {
  day: string
  orderCount: number
}

export interface DashboardRecentOrder {
  id: number
  userAccount?: string
  totalPrice?: number
  status?: number
  orderDate?: string
}

export interface DashboardStats {
  uniformTotal: number
  todayOrders: number
  pendingReviews: number
  todayVisits: number
  orderTrend: DashboardTrendPoint[]
  recentOrders: DashboardRecentOrder[]
}

export interface OperLogRecord {
  logId: number
  userId?: number
  username?: string
  module?: string
  operation?: string
  method?: string
  requestUrl?: string
  requestBody?: string
  responseCode?: number
  ipAddress?: string
  operTime?: string
}

export interface OperLogListPayload {
  records: OperLogRecord[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PagePayload<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface InventorySkuRecord {
  skuId: number
  uniformId?: number
  sizeId?: number
  stockQuantity?: number
  reservedQuantity?: number
  availableQuantity?: number
  safetyStock?: number
  reorderPoint?: number
  leadTimeDays?: number
  status?: string
  uniformName?: string
  schoolName?: string
  gradeName?: string
  categoryKey?: string
  sizeName?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface InventorySkuPayload {
  skuId?: number
  uniformId?: number
  sizeId?: number
  stockQuantity?: number
  reservedQuantity?: number
  safetyStock?: number
  reorderPoint?: number
  leadTimeDays?: number
  status?: string
  remark?: string
}

export interface InventoryMovementRecord {
  movementId: number
  skuId?: number
  movementType?: string
  quantity?: number
  beforeStockQuantity?: number
  afterStockQuantity?: number
  beforeReservedQuantity?: number
  afterReservedQuantity?: number
  relatedOrderId?: number
  relatedOrderItemId?: number
  actorType?: string
  actorId?: number
  reason?: string
  createTime?: string
}

export interface InventoryMovementPayload {
  skuId: number
  movementType: string
  quantity: number
  relatedOrderId?: number
  relatedOrderItemId?: number
  reason?: string
}

export interface LowStockAlertRecord {
  skuId: number
  uniformId?: number
  sizeId?: number
  uniformName?: string
  schoolName?: string
  gradeName?: string
  categoryKey?: string
  sizeName?: string
  stockQuantity?: number
  reservedQuantity?: number
  availableQuantity?: number
  safetyStock?: number
  reorderPoint?: number
  shortageQuantity?: number
  severity?: string
}

export interface SupplySalesAnalyticsRecord {
  groupKey?: string
  groupName?: string
  totalQuantity?: number
  totalAmount?: number
  orderCount?: number
  uniformId?: number
  sizeId?: number
  uniformName?: string
  sizeName?: string
  schoolName?: string
  gradeName?: string
  categoryKey?: string
}

export interface SupplyTrendPointRecord {
  bucket: string
  totalQuantity?: number
  totalAmount?: number
  orderCount?: number
}

export interface SupplyForecastRecord {
  skuId: number
  uniformId?: number
  sizeId?: number
  uniformName?: string
  schoolName?: string
  gradeName?: string
  categoryKey?: string
  sizeName?: string
  availableQuantity?: number
  safetyStock?: number
  reorderPoint?: number
  leadTimeDays?: number
  historicalQuantity?: number
  salesDayCount?: number
  lookbackDays?: number
  horizonDays?: number
  dailyAverage?: number
  forecastDemand?: number
  dataCompleteness?: number
  reasonCodes?: string[]
}

export interface ReplenishmentSuggestionRecord {
  skuId: number
  uniformId?: number
  sizeId?: number
  uniformName?: string
  schoolName?: string
  gradeName?: string
  categoryKey?: string
  sizeName?: string
  availableQuantity?: number
  safetyStock?: number
  reorderPoint?: number
  leadTimeDays?: number
  forecastDemand?: number
  suggestedQuantity?: number
  reasonText?: string
}
