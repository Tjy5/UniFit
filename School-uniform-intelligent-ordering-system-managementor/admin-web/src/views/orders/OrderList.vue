<template>
  <section class="basic-page">
    <div class="page-title">
      <div>
        <h1>订单管理</h1>
        <p>查看订单履约状态、收货信息、状态历史和异常订单，并支持导出当前筛选结果。</p>
      </div>
    </div>

    <section class="section-card basic-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="用户ID">
          <el-input-number v-model="queryForm.userId" :min="1" controls-position="right" placeholder="用户ID" style="width: 180px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="queryForm.paymentStatus" clearable placeholder="全部支付状态" style="width: 180px">
            <el-option v-for="item in paymentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流状态">
          <el-select v-model="queryForm.shippingStatus" clearable placeholder="全部物流状态" style="width: 180px">
            <el-option v-for="item in shippingStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList()">查询</el-button>
          <el-button :type="queryForm.abnormalOnly ? 'warning' : 'default'" @click="toggleAnomalyFilter">异常订单</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="basic-page__actions">
        <ExportButton
          :payload="{ userId: queryForm.userId, status: queryForm.status, paymentStatus: queryForm.paymentStatus, shippingStatus: queryForm.shippingStatus }"
          filename="orders.xlsx"
          url="/orders/export"
        />
      </div>
    </section>

    <section class="section-card basic-page__table">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="id" label="订单ID" min-width="110" />
        <el-table-column prop="userAccount" label="用户账号" min-width="140" />
        <el-table-column prop="recipientName" label="收货人" min-width="120" />
        <el-table-column label="订单金额" min-width="140">
          <template #default="{ row }">
            {{ formatCurrency(row.totalPrice) }}
          </template>
        </el-table-column>
        <el-table-column label="订单状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTag(row.status)">{{ getOrderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" min-width="130">
          <template #default="{ row }">
            <el-tag :type="getPaymentStatusTag(row.paymentStatus)">{{ getPaymentStatusLabel(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物流状态" min-width="130">
          <template #default="{ row }">
            <el-tag :type="getShippingStatusTag(row.shippingStatus)">{{ getShippingStatusLabel(row.shippingStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常" min-width="190">
          <template #default="{ row }">
            <el-tag v-if="row.anomalySummary?.abnormal" type="danger">
              {{ row.anomalySummary.reasons.slice(0, 2).join('、') }}
            </el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.orderDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-dropdown v-if="row.allowedFulfillmentActions?.length" trigger="click" @command="runCommandPayload">
              <el-button link type="warning">履约动作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="command in row.allowedFulfillmentActions" :key="command" :command="{ row, command }">
                    {{ getCommandLabel(command) }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="basic-page__pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :background="true"
          layout="total, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="订单详情" width="940px">
      <template v-if="orderDetail">
        <el-descriptions :column="2" border class="order-detail__desc">
          <el-descriptions-item label="订单ID">{{ orderDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="用户账号">{{ orderDetail.userAccount || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ orderDetail.recipientName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ orderDetail.phoneNumber || '--' }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ orderDetail.fullAddress || '--' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ getOrderStatusLabel(orderDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ getPaymentStatusLabel(orderDetail.paymentStatus) }}</el-descriptions-item>
          <el-descriptions-item label="物流状态">{{ getShippingStatusLabel(orderDetail.shippingStatus) }}</el-descriptions-item>
          <el-descriptions-item label="异常标记" :span="2">
            <el-tag v-if="orderDetail.anomalySummary?.abnormal" type="danger">
              {{ orderDetail.anomalySummary.reasons.join('、') }}
            </el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDateTime(orderDetail.orderDate) }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">{{ formatCurrency(orderDetail.totalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ orderDetail.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="order-detail__items">
          <div class="order-detail__head">
            <h3>订单项</h3>
            <span>{{ orderDetail.orderItems?.length || 0 }} 件</span>
          </div>
          <el-table :data="orderDetail.orderItems || []">
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <div class="order-item__product">
                  <el-image v-if="row.imageSnapshot" :src="row.imageSnapshot" class="order-item__image" fit="cover" />
                  <div>
                    <strong>{{ row.uniformNameSnapshot || '未知商品' }}</strong>
                    <span>尺码：{{ row.sizeNameSnapshot || '--' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" min-width="80" />
            <el-table-column label="单价" min-width="120">
              <template #default="{ row }">
                {{ formatCurrency(row.unitPriceSnapshot) }}
              </template>
            </el-table-column>
            <el-table-column label="小计" min-width="120">
              <template #default="{ row }">
                {{ formatCurrency(row.itemTotalPrice) }}
              </template>
            </el-table-column>
            <el-table-column prop="reviewId" label="关联评论" min-width="120" />
          </el-table>
        </div>

        <div class="order-detail__timeline">
          <div class="order-detail__head">
            <h3>状态历史</h3>
            <span>{{ statusLogs.length }} 条</span>
          </div>
          <el-timeline v-if="statusLogs.length">
            <el-timeline-item v-for="log in statusLogs" :key="log.id" :timestamp="formatDateTime(log.createTime)">
              <strong>{{ getCommandLabel(log.eventType || '') }}</strong>
              <p>
                {{ getOrderStatusLabel(log.fromOrderStatus) }} / {{ getPaymentStatusLabel(log.fromPaymentStatus) }} /
                {{ getShippingStatusLabel(log.fromShippingStatus) }}
                ->
                {{ getOrderStatusLabel(log.toOrderStatus) }} / {{ getPaymentStatusLabel(log.toPaymentStatus) }} /
                {{ getShippingStatusLabel(log.toShippingStatus) }}
              </p>
              <span>{{ log.actorType || '--' }} {{ log.actorId || '' }} {{ log.reason || '' }}</span>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态历史" />
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { executeOrderFulfillmentCommand, getOrder, getOrderStatusLogs, listOrderAnomalies, listOrders } from '@/api/orders'
import ExportButton from '@/components/ExportButton.vue'
import type { OrderRecord, OrderStatusLogRecord } from '@/types/basic-data'
import { formatCurrency, formatDateTime } from '@/utils/format'

const orderStatusOptions = [
  { label: '待支付', value: 0 },
  { label: '待发货', value: 1 },
  { label: '已发货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '退款中', value: 5 },
  { label: '已退款', value: 6 },
]

const paymentStatusOptions = [
  { label: '待支付', value: 'PENDING' },
  { label: '已支付', value: 'PAID' },
  { label: '支付失败', value: 'FAILED' },
  { label: '退款处理中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '已关闭', value: 'CLOSED' },
]

const shippingStatusOptions = [
  { label: '未发货', value: 'NOT_SHIPPED' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已签收', value: 'DELIVERED' },
  { label: '退货申请中', value: 'RETURN_REQUESTED' },
  { label: '已退货', value: 'RETURNED' },
]

const loading = ref(false)
const detailVisible = ref(false)
const rows = ref<OrderRecord[]>([])
const orderDetail = ref<OrderRecord>()
const statusLogs = ref<OrderStatusLogRecord[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const queryForm = reactive({
  userId: undefined as number | undefined,
  status: undefined as number | undefined,
  paymentStatus: undefined as string | undefined,
  shippingStatus: undefined as string | undefined,
  abnormalOnly: false,
})

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const api = queryForm.abnormalOnly ? listOrderAnomalies : listOrders
    const { data } = await api({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      userId: queryForm.userId,
      status: queryForm.status,
      paymentStatus: queryForm.paymentStatus,
      shippingStatus: queryForm.shippingStatus,
    })
    rows.value = data.data.records
    pagination.total = data.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.userId = undefined
  queryForm.status = undefined
  queryForm.paymentStatus = undefined
  queryForm.shippingStatus = undefined
  queryForm.abnormalOnly = false
  pagination.pageNum = 1
  fetchList()
}

function toggleAnomalyFilter() {
  queryForm.abnormalOnly = !queryForm.abnormalOnly
  pagination.pageNum = 1
  fetchList()
}

async function openDetail(id: number) {
  const [detailResult, logsResult] = await Promise.all([getOrder(id), getOrderStatusLogs(id)])
  orderDetail.value = detailResult.data.data
  statusLogs.value = logsResult.data.data
  detailVisible.value = true
}

async function runCommandPayload(payload: unknown) {
  const { row, command } = payload as { row: OrderRecord; command: string }
  await executeOrderFulfillmentCommand(row.id, {
    command,
    reason: getCommandLabel(command),
  })
  ElMessage.success('订单履约状态已更新')
  await fetchList()
  if (orderDetail.value?.id === row.id) {
    await openDetail(row.id)
  }
}

function getOrderStatusLabel(value?: number) {
  return orderStatusOptions.find((item) => item.value === value)?.label || '--'
}

function getPaymentStatusLabel(value?: string) {
  return paymentStatusOptions.find((item) => item.value === value)?.label || '--'
}

function getShippingStatusLabel(value?: string) {
  return shippingStatusOptions.find((item) => item.value === value)?.label || '--'
}

function getOrderStatusTag(value?: number) {
  if (value === 3) {
    return 'success'
  }
  if (value === 4 || value === 6) {
    return 'info'
  }
  if (value === 0 || value === 5) {
    return 'warning'
  }
  return 'primary'
}

function getPaymentStatusTag(value?: string) {
  if (value === 'PAID') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (value === 'REFUNDING') {
    return 'warning'
  }
  return 'info'
}

function getShippingStatusTag(value?: string) {
  if (value === 'DELIVERED') {
    return 'success'
  }
  if (value === 'SHIPPED') {
    return 'primary'
  }
  if (value === 'RETURN_REQUESTED') {
    return 'warning'
  }
  return 'info'
}

function getCommandLabel(command: string) {
  const map: Record<string, string> = {
    SHIP_ORDER: '发货',
    CONFIRM_RECEIPT: '确认签收',
    APPROVE_REFUND: '同意退款',
    REJECT_REFUND: '拒绝退款',
    REQUEST_REFUND: '申请退款',
    SIMULATE_PAYMENT_SUCCESS: '模拟支付成功',
    SIMULATE_PAYMENT_FAILURE: '模拟支付失败',
    CANCEL_UNPAID: '取消未支付订单',
    LEGACY_STATUS_IMPORTED: '历史状态导入',
    ORDER_CREATED: '订单创建',
  }
  return map[command] || command || '--'
}
</script>

<style scoped lang="scss">
.basic-page {
  display: grid;
  gap: 18px;
}

.basic-page__toolbar,
.basic-page__table {
  padding: 20px;
}

.basic-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
}

.basic-page__actions {
  display: flex;
  gap: 12px;
}

.basic-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.order-detail__desc {
  margin-bottom: 20px;
}

.order-detail__items,
.order-detail__timeline {
  margin-top: 22px;
}

.order-detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-detail__head h3 {
  margin: 0;
}

.order-detail__head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.order-detail__timeline p {
  margin: 6px 0;
  color: var(--text-primary);
}

.order-detail__timeline span {
  color: var(--text-secondary);
  font-size: 13px;
}

.order-item__product {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-item__product strong,
.order-item__product span {
  display: block;
}

.order-item__product span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.order-item__image {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  border: 1px solid var(--line);
}
</style>
