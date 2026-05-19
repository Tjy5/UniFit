<template>
  <section class="calibration-page">
    <div class="page-title">
      <div>
        <h1>校准参数管理</h1>
        <p>查看自动校准参数、版本记录、审计轨迹和推荐分影响。</p>
      </div>
      <div class="calibration-page__head-actions">
        <el-button :icon="Refresh" :loading="loading" plain type="primary" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <section class="section-card calibration-page__toolbar">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="作用域">
          <el-select v-model="queryForm.scopeType" clearable placeholder="全部作用域" style="width: 150px">
            <el-option label="全局" value="GLOBAL" />
            <el-option label="学校" value="SCHOOL" />
            <el-option label="商品" value="PRODUCT" />
          </el-select>
        </el-form-item>
        <el-form-item label="作用域 ID">
          <el-input v-model="queryForm.scopeId" clearable placeholder="学校或商品 ID" style="width: 160px" />
        </el-form-item>
        <el-form-item label="目标尺码">
          <el-select v-model="queryForm.targetSizeId" clearable filterable placeholder="全部尺码" style="width: 160px">
            <el-option v-for="size in sizeOptions" :key="size.id" :label="size.sizeName" :value="size.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="queryForm.enabled" clearable placeholder="全部" style="width: 130px">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="参数状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="生效" value="ACTIVE" />
            <el-option label="观察" value="OBSERVING" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配把握等级">
          <el-select v-model="queryForm.confidenceLevel" clearable placeholder="全部等级" style="width: 150px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input-number v-model="queryForm.version" :min="1" :precision="0" clearable controls-position="right" style="width: 130px" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="section-card calibration-page__table">
      <el-table v-loading="loading" :data="rows" row-key="paramId">
        <el-table-column prop="paramId" label="参数 ID" width="100" />
        <el-table-column label="作用域" min-width="180">
          <template #default="{ row }">
            <div class="calibration-page__cell-main">{{ scopeLabel(row.scopeType) }}</div>
            <small>{{ row.scopeName || row.scopeId || '全局' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="目标尺码" min-width="130">
          <template #default="{ row }">
            {{ row.targetSizeName || '全部尺码' }}
          </template>
        </el-table-column>
        <el-table-column prop="adjustmentValue" label="偏移" width="110">
          <template #default="{ row }">
            <span :class="deltaClass(row.adjustmentValue)">{{ formatSigned(row.adjustmentValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="150">
          <template #default="{ row }">
            <div class="calibration-page__tag-row">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              <el-tag :type="statusTag(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="样本" min-width="130">
          <template #default="{ row }">
            <div class="calibration-page__cell-main">{{ row.sampleSize ?? 0 }}</div>
            <small>{{ row.uniqueUserCount ?? 0 }} 用户</small>
          </template>
        </el-table-column>
        <el-table-column label="匹配把握" width="100">
          <template #default="{ row }">
            <el-tag :type="confidenceTag(row.confidenceLevel)" effect="plain">{{ confidenceLabel(row.confidenceLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" min-width="150">
          <template #default="{ row }">
            <div class="calibration-page__tag-row">
              <el-tag effect="plain">v{{ row.version ?? '-' }}</el-tag>
              <el-tag v-if="row.latestVersion" type="success" effect="plain">最新</el-tag>
              <el-tag v-if="row.effectiveNow" type="success" effect="plain">当前生效</el-tag>
              <el-tag v-if="row.shadowedByHigherVersion" type="warning" effect="plain">被遮蔽</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="生效窗口" min-width="210">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.effectiveFrom) }}</div>
            <small>至 {{ formatDateTime(row.effectiveUntil) || '长期' }}</small>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="{ row }">
            <div class="calibration-page__row-actions">
              <el-tooltip content="查看详情" placement="top">
                <el-button :icon="View" circle plain type="primary" @click="openDetail(row.paramId)" />
              </el-tooltip>
              <el-tooltip content="启用参数" placement="top">
                <el-button :disabled="row.enabled || row.status === 'OBSERVING'" :icon="SwitchButton" circle plain type="success" @click="openOperation('enable', row)" />
              </el-tooltip>
              <el-tooltip content="停用参数" placement="top">
                <el-button :disabled="!row.enabled" :icon="CircleClose" circle plain type="danger" @click="openOperation('disable', row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无校准参数" />
        </template>
      </el-table>

      <div class="basic-page__pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :background="true"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="fetchList"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="detailDrawerVisible" :size="drawerSize" destroy-on-close>
      <template #header>
        <div class="calibration-page__drawer-title">
          <span>参数详情</span>
          <small v-if="detail">#{{ detail.paramId }} · v{{ detail.version ?? '-' }}</small>
        </div>
      </template>

      <div v-loading="detailLoading" class="calibration-detail">
        <template v-if="detail">
          <section class="calibration-detail__header">
            <div>
              <h2>{{ detail.scopeName || scopeLabel(detail.scopeType) }}</h2>
              <p>{{ detail.targetSizeName || '全部尺码' }} · {{ detail.calibrationType }}</p>
            </div>
            <div class="calibration-detail__actions">
              <el-button :disabled="detail.enabled || detail.status === 'OBSERVING'" :icon="SwitchButton" type="success" @click="openOperation('enable', detail)">启用</el-button>
              <el-button :disabled="!detail.enabled" :icon="CircleClose" type="danger" plain @click="openOperation('disable', detail)">停用</el-button>
              <el-button :disabled="versions.length === 0" :icon="RefreshLeft" plain type="primary" @click="openOperation('rollback', detail, detail.paramId)">回滚</el-button>
            </div>
          </section>

          <el-descriptions :column="3" border>
            <el-descriptions-item label="作用域">{{ scopeLabel(detail.scopeType) }}</el-descriptions-item>
            <el-descriptions-item label="作用域 ID">{{ detail.scopeId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="目标尺码">{{ detail.targetSizeName || '全部尺码' }}</el-descriptions-item>
            <el-descriptions-item label="偏移值">
              <span :class="deltaClass(detail.adjustmentValue)">{{ formatSigned(detail.adjustmentValue) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <div class="calibration-page__tag-row">
                <el-tag :type="detail.enabled ? 'success' : 'info'" effect="plain">{{ detail.enabled ? '启用' : '停用' }}</el-tag>
                <el-tag :type="statusTag(detail.status)" effect="plain">{{ statusLabel(detail.status) }}</el-tag>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="匹配把握等级">
              <el-tag :type="confidenceTag(detail.confidenceLevel)" effect="plain">{{ confidenceLabel(detail.confidenceLevel) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="样本量">{{ detail.sampleSize ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="唯一用户">{{ detail.uniqueUserCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="人工参数">{{ detail.isManual ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="生效开始">{{ formatDateTime(detail.effectiveFrom) }}</el-descriptions-item>
            <el-descriptions-item label="生效结束">{{ formatDateTime(detail.effectiveUntil) || '长期' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
          </el-descriptions>

          <div class="calibration-detail__grid">
            <section class="calibration-detail__panel">
              <div class="calibration-detail__panel-head">
                <h3>反馈分布</h3>
              </div>
              <div v-if="feedbackEntries(detail.feedbackDistribution).length" class="calibration-detail__feedback">
                <div v-for="[key, value] in feedbackEntries(detail.feedbackDistribution)" :key="key">
                  <span>{{ key }}</span>
                  <strong>{{ formatUnknown(value) }}</strong>
                </div>
              </div>
              <el-empty v-else :image-size="72" description="暂无反馈分布" />
            </section>

            <section class="calibration-detail__panel">
              <div class="calibration-detail__panel-head">
                <h3>版本时间线</h3>
              </div>
              <el-timeline v-if="versions.length">
                <el-timeline-item v-for="item in versions" :key="item.paramId" :timestamp="formatDateTime(item.updateTime || item.createTime)" placement="top">
                  <div class="calibration-detail__timeline-item">
                    <div>
                      <strong>v{{ item.version ?? '-' }}</strong>
                      <span :class="deltaClass(item.adjustmentValue)">{{ formatSigned(item.adjustmentValue) }}</span>
                    </div>
                    <div class="calibration-page__tag-row">
                      <el-tag v-if="item.latestVersion" size="small" type="success" effect="plain">最新</el-tag>
                      <el-tag v-if="item.effectiveNow" size="small" type="success" effect="plain">当前生效</el-tag>
                      <el-tag v-if="item.shadowedByHigherVersion" size="small" type="warning" effect="plain">被遮蔽</el-tag>
                      <el-button size="small" text type="primary" @click="openOperation('rollback', detail, item.paramId)">回滚到此版本</el-button>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else :image-size="72" description="暂无版本记录" />
            </section>
          </div>

          <section class="calibration-detail__panel">
            <div class="calibration-detail__panel-head">
              <h3>审计历史</h3>
            </div>
            <el-timeline v-if="audits.length">
              <el-timeline-item v-for="audit in audits" :key="audit.auditId" :timestamp="formatDateTime(audit.createTime)" placement="top">
                <div class="calibration-detail__audit">
                  <div class="calibration-detail__audit-head">
                    <el-tag effect="plain">{{ audit.action }}</el-tag>
                    <span>{{ audit.operator || 'system' }}</span>
                    <small>{{ audit.reason || '无原因记录' }}</small>
                  </div>
                  <el-collapse>
                    <el-collapse-item title="快照" :name="String(audit.auditId)">
                      <div class="calibration-detail__snapshot">
                        <pre>{{ formatJson(audit.oldValue) }}</pre>
                        <pre>{{ formatJson(audit.newValue) }}</pre>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :image-size="72" description="暂无审计记录" />
          </section>

          <section class="calibration-detail__panel">
            <div class="calibration-detail__panel-head calibration-detail__panel-head--wrap">
              <h3>推荐分影响</h3>
              <el-form :inline="true" class="calibration-detail__impact-form">
                <el-form-item label="日期">
                  <el-date-picker
                    v-model="impactDateRange"
                    type="daterange"
                    value-format="YYYY-MM-DD"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    range-separator="至"
                    style="width: 260px"
                  />
                </el-form-item>
                <el-form-item label="学校">
                  <el-select v-model="impactFilters.schoolId" clearable filterable placeholder="全部学校" style="width: 180px" @change="handleImpactSchoolChange">
                    <el-option v-for="school in schoolOptions" :key="school.schoolId" :label="school.schoolName" :value="school.schoolId" />
                  </el-select>
                </el-form-item>
                <el-form-item label="商品">
                  <el-select v-model="impactFilters.uniformId" clearable filterable placeholder="全部商品" style="width: 200px">
                    <el-option v-for="uniform in uniformOptions" :key="uniform.id" :label="uniform.name" :value="uniform.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="尺码">
                  <el-select v-model="impactFilters.sizeId" clearable filterable placeholder="全部尺码" style="width: 150px">
                    <el-option v-for="size in sizeOptions" :key="size.id" :label="size.sizeName" :value="size.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="版本">
                  <el-select v-model="impactFilters.version" clearable placeholder="全部版本" style="width: 130px">
                    <el-option v-for="item in versions" :key="item.paramId" :label="`v${item.version ?? '-'}`" :value="item.version" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button :icon="Search" :loading="impactLoading" type="primary" @click="fetchImpact">查询</el-button>
                </el-form-item>
              </el-form>
            </div>

            <div class="calibration-detail__impact-stats">
              <article>
                <span>应用次数</span>
                <strong>{{ impact.summary.appliedCount }}</strong>
              </article>
              <article>
                <span>主推变化</span>
                <strong>{{ impact.summary.bestSizeChangedCount }}</strong>
              </article>
              <article>
                <span>平均基础分</span>
                <strong>{{ formatNumber(impact.summary.avgBaseBestScore) }}</strong>
              </article>
              <article>
                <span>平均校准分</span>
                <strong>{{ formatNumber(impact.summary.avgCalibratedBestScore) }}</strong>
              </article>
              <article>
                <span>平均变化</span>
                <strong :class="deltaClass(impact.summary.avgScoreDelta)">{{ formatSigned(impact.summary.avgScoreDelta) }}</strong>
              </article>
            </div>

            <el-table v-loading="impactLoading" :data="impact.records" row-key="logId">
              <el-table-column prop="logId" label="日志 ID" width="100" />
              <el-table-column label="上下文" min-width="220">
                <template #default="{ row }">
                  <div class="calibration-page__cell-main">{{ row.uniformName || '未关联商品' }}</div>
                  <small>{{ row.schoolName || '未关联学校' }}</small>
                </template>
              </el-table-column>
              <el-table-column label="基础推荐" min-width="160">
                <template #default="{ row }">
                  <div>{{ row.baseBestSizeName || '-' }}</div>
                  <small>{{ formatNumber(row.baseBestScore) }}</small>
                </template>
              </el-table-column>
              <el-table-column label="校准推荐" min-width="160">
                <template #default="{ row }">
                  <div>{{ row.calibratedBestSizeName || '-' }}</div>
                  <small>{{ formatNumber(row.calibratedBestScore) }}</small>
                </template>
              </el-table-column>
              <el-table-column label="最终推荐" min-width="160">
                <template #default="{ row }">
                  <div>{{ row.finalBestSizeName || row.recommendedSizeName || '-' }}</div>
                  <small>{{ formatNumber(row.finalBestScore) }}</small>
                </template>
              </el-table-column>
              <el-table-column label="变化" width="110">
                <template #default="{ row }">
                  <span :class="deltaClass(row.scoreDelta)">{{ formatSigned(row.scoreDelta) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="归因" min-width="180">
                <template #default="{ row }">
                  <div class="calibration-page__tag-row">
                    <el-tag :type="row.attributionStatus === 'TRACEABLE' ? 'success' : 'info'" effect="plain">
                      {{ row.attributionStatus === 'TRACEABLE' ? '可追溯' : 'unknown' }}
                    </el-tag>
                    <el-tag v-for="hit in row.matchedParams || []" :key="`${row.logId}-${hit.sizeId}-${hit.paramId}-${hit.version}`" effect="plain">
                      {{ hit.source || 'NONE' }} · v{{ hit.version ?? '-' }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="时间" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无影响记录" />
              </template>
            </el-table>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="operationDialog.visible" :title="operationTitle" width="520px">
      <el-form label-position="top">
        <el-form-item v-if="operationDialog.action === 'rollback'" label="来源版本">
          <el-select v-model="operationDialog.sourceParamId" filterable placeholder="选择回滚来源版本" style="width: 100%">
            <el-option
              v-for="item in versions"
              :key="item.paramId"
              :label="`#${item.paramId} · v${item.version ?? '-'} · ${formatSigned(item.adjustmentValue)}`"
              :value="item.paramId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作原因">
          <el-input v-model="operationDialog.reason" :rows="4" maxlength="500" show-word-limit type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="operationDialog.visible = false">取消</el-button>
        <el-button :loading="operationDialog.submitting" type="primary" @click="submitOperation">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  CircleClose,
  Refresh,
  RefreshLeft,
  Search,
  SwitchButton,
  View,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  disableCalibrationParam,
  enableCalibrationParam,
  getCalibrationImpact,
  getCalibrationParam,
  listCalibrationParamAudits,
  listCalibrationParamVersions,
  listCalibrationParams,
  rollbackCalibrationParam,
} from '@/api/calibration'
import { listSchoolOptions } from '@/api/schools'
import { listSizes } from '@/api/sizes'
import { listUniforms } from '@/api/uniform'
import type { SchoolForm, SizeForm, UniformForm } from '@/types/basic-data'
import type {
  CalibrationAuditRecord,
  CalibrationConfidenceLevel,
  CalibrationImpactResponse,
  CalibrationParamDetail,
  CalibrationParamListItem,
  CalibrationParamQuery,
  CalibrationParamVersion,
  CalibrationScopeType,
  CalibrationStatus,
} from '@/types/calibration'

type LifecycleAction = 'enable' | 'disable' | 'rollback'

const loading = ref(false)
const detailLoading = ref(false)
const impactLoading = ref(false)
const detailDrawerVisible = ref(false)
const rows = ref<CalibrationParamListItem[]>([])
const detail = ref<CalibrationParamDetail>()
const versions = ref<CalibrationParamVersion[]>([])
const audits = ref<CalibrationAuditRecord[]>([])
const schoolOptions = ref<SchoolForm[]>([])
const uniformOptions = ref<UniformForm[]>([])
const sizeOptions = ref<SizeForm[]>([])

const drawerSize = computed(() => (window.innerWidth <= 900 ? '96%' : '78%'))

const queryForm = reactive<CalibrationParamQuery>({
  pageNum: 1,
  pageSize: 10,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const impactDateRange = ref<[string, string]>(defaultDateRange())
const impactFilters = reactive({
  schoolId: undefined as number | undefined,
  uniformId: undefined as number | undefined,
  sizeId: undefined as number | undefined,
  version: undefined as number | undefined,
})

const impact = ref<CalibrationImpactResponse>({
  summary: {
    appliedCount: 0,
    bestSizeChangedCount: 0,
    avgBaseBestScore: 0,
    avgCalibratedBestScore: 0,
    avgScoreDelta: 0,
  },
  records: [],
  total: 0,
  limit: 20,
  offset: 0,
})

const operationDialog = reactive({
  visible: false,
  action: 'enable' as LifecycleAction,
  target: undefined as CalibrationParamListItem | undefined,
  sourceParamId: undefined as number | undefined,
  reason: '',
  submitting: false,
})

const operationTitle = computed(() => {
  const actionMap: Record<LifecycleAction, string> = {
    enable: '启用校准参数',
    disable: '停用校准参数',
    rollback: '回滚校准参数',
  }
  return actionMap[operationDialog.action]
})

onMounted(async () => {
  await Promise.all([loadOptions(), fetchList()])
})

async function loadOptions() {
  const [schoolResult, uniformResult, sizeResult] = await Promise.all([
    listSchoolOptions(),
    listUniforms({ pageNum: 1, pageSize: 500 }),
    listSizes({ pageNum: 1, pageSize: 500 }),
  ])
  schoolOptions.value = schoolResult.data.data || []
  uniformOptions.value = uniformResult.data.data.records || []
  sizeOptions.value = sizeResult.data.data.records || []
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listCalibrationParams({
      ...queryForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      scopeId: queryForm.scopeId?.trim() || undefined,
    })
    rows.value = data.data.records || []
    pagination.total = data.data.total
    pagination.pageNum = data.data.pageNum
    pagination.pageSize = data.data.pageSize
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function resetQuery() {
  queryForm.scopeType = undefined
  queryForm.scopeId = undefined
  queryForm.targetSizeId = undefined
  queryForm.enabled = undefined
  queryForm.status = undefined
  queryForm.confidenceLevel = undefined
  queryForm.version = undefined
  pagination.pageNum = 1
  fetchList()
}

function handlePageSizeChange() {
  pagination.pageNum = 1
  fetchList()
}

async function openDetail(paramId: number) {
  detailDrawerVisible.value = true
  await loadDetailBundle(paramId)
}

async function loadDetailBundle(paramId: number) {
  detailLoading.value = true
  try {
    const [detailResult, versionsResult, auditsResult] = await Promise.all([
      getCalibrationParam(paramId),
      listCalibrationParamVersions(paramId),
      listCalibrationParamAudits(paramId),
    ])
    detail.value = detailResult.data.data
    versions.value = versionsResult.data.data || []
    audits.value = auditsResult.data.data || []
    impactFilters.version = detail.value.version
    await fetchImpact()
  } finally {
    detailLoading.value = false
  }
}

async function fetchImpact() {
  if (!detail.value?.paramId) {
    return
  }
  const [startDate, endDate] = impactDateRange.value || []
  impactLoading.value = true
  try {
    const { data } = await getCalibrationImpact(detail.value.paramId, {
      version: impactFilters.version,
      schoolId: impactFilters.schoolId,
      uniformId: impactFilters.uniformId,
      sizeId: impactFilters.sizeId,
      startDate,
      endDate,
      limit: 20,
      offset: 0,
    })
    impact.value = normalizeImpact(data.data)
  } finally {
    impactLoading.value = false
  }
}

async function handleImpactSchoolChange(value?: number) {
  impactFilters.uniformId = undefined
  const { data } = await listUniforms({
    pageNum: 1,
    pageSize: 500,
    schoolId: value,
  })
  uniformOptions.value = data.data.records || []
}

function openOperation(action: LifecycleAction, target: CalibrationParamListItem, sourceParamId?: number) {
  operationDialog.action = action
  operationDialog.target = target
  operationDialog.sourceParamId = sourceParamId ?? (action === 'rollback' ? versions.value[0]?.paramId : undefined)
  operationDialog.reason = ''
  operationDialog.visible = true
}

async function submitOperation() {
  const target = operationDialog.target
  if (!target?.paramId) {
    return
  }
  if (!operationDialog.reason.trim()) {
    ElMessage.warning('请输入操作原因')
    return
  }
  if (operationDialog.action === 'rollback' && !operationDialog.sourceParamId) {
    ElMessage.warning('请选择回滚来源版本')
    return
  }

  operationDialog.submitting = true
  try {
    const payload = { reason: operationDialog.reason.trim() }
    const result = operationDialog.action === 'enable'
      ? await enableCalibrationParam(target.paramId, payload)
      : operationDialog.action === 'disable'
        ? await disableCalibrationParam(target.paramId, payload)
        : await rollbackCalibrationParam(target.paramId, {
            ...payload,
            sourceParamId: operationDialog.sourceParamId,
          })

    operationDialog.visible = false
    ElMessage.success(result.data.msg || '操作成功')
    await fetchList()
    if (detailDrawerVisible.value) {
      await loadDetailBundle(result.data.data.paramId || target.paramId)
    }
  } finally {
    operationDialog.submitting = false
  }
}

function normalizeImpact(value?: Partial<CalibrationImpactResponse>): CalibrationImpactResponse {
  return {
    summary: {
      appliedCount: value?.summary?.appliedCount ?? 0,
      bestSizeChangedCount: value?.summary?.bestSizeChangedCount ?? 0,
      avgBaseBestScore: value?.summary?.avgBaseBestScore ?? 0,
      avgCalibratedBestScore: value?.summary?.avgCalibratedBestScore ?? 0,
      avgScoreDelta: value?.summary?.avgScoreDelta ?? 0,
    },
    records: value?.records || [],
    total: value?.total ?? 0,
    limit: value?.limit ?? 20,
    offset: value?.offset ?? 0,
  }
}

function scopeLabel(value?: CalibrationScopeType) {
  const map: Record<CalibrationScopeType, string> = {
    GLOBAL: '全局',
    SCHOOL: '学校',
    PRODUCT: '商品',
  }
  return value ? map[value] || value : '-'
}

function statusLabel(value?: CalibrationStatus) {
  const map: Record<CalibrationStatus, string> = {
    ACTIVE: '生效',
    OBSERVING: '观察',
    DISABLED: '停用',
  }
  return value ? map[value] || value : '-'
}

function statusTag(value?: CalibrationStatus) {
  if (value === 'ACTIVE') {
    return 'success'
  }
  if (value === 'OBSERVING') {
    return 'warning'
  }
  return 'info'
}

function confidenceLabel(value?: CalibrationConfidenceLevel) {
  const map: Record<CalibrationConfidenceLevel, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
  }
  return value ? map[value] || value : '-'
}

function confidenceTag(value?: CalibrationConfidenceLevel) {
  if (value === 'HIGH') {
    return 'success'
  }
  if (value === 'MEDIUM') {
    return 'warning'
  }
  return 'info'
}

function deltaClass(value?: number) {
  const numeric = Number(value || 0)
  return {
    'calibration-page__delta--positive': numeric > 0,
    'calibration-page__delta--negative': numeric < 0,
  }
}

function formatSigned(value?: number) {
  const numeric = Number(value || 0)
  const prefix = numeric > 0 ? '+' : ''
  return `${prefix}${numeric.toFixed(4)}`
}

function formatNumber(value?: number, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toFixed(digits)
}

function formatDateTime(value?: string | number) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const parts = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ]
  const time = [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
  ].join(':')
  return `${parts.join('-')} ${time}`
}

function feedbackEntries(value: unknown) {
  return Object.entries(asRecord(value)).filter(([, item]) => item !== null && item !== undefined && item !== '')
}

function asRecord(value: unknown): Record<string, unknown> {
  if (!value) {
    return {}
  }
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value) as unknown
      return asRecord(parsed)
    } catch {
      return {}
    }
  }
  if (typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }
  return {}
}

function formatUnknown(value: unknown) {
  if (typeof value === 'number') {
    return String(value)
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value)
}

function formatJson(value: unknown) {
  if (!value) {
    return '-'
  }
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  return JSON.stringify(value, null, 2)
}

function defaultDateRange(): [string, string] {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return [formatDate(start), formatDate(end)]
}

function formatDate(date: Date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}
</script>

<style scoped lang="scss">
.calibration-page {
  display: grid;
  gap: 18px;
}

.calibration-page__head-actions,
.calibration-detail__actions,
.calibration-page__row-actions,
.calibration-page__tag-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.calibration-page__toolbar,
.calibration-page__table {
  padding: 20px;
}

.calibration-page__cell-main {
  font-weight: 600;
  color: var(--text-primary);
}

.calibration-page small,
.calibration-detail p,
.calibration-detail small {
  color: var(--text-secondary);
}

.calibration-page__delta--positive {
  color: var(--success);
  font-weight: 700;
}

.calibration-page__delta--negative {
  color: var(--danger);
  font-weight: 700;
}

.calibration-page__drawer-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-weight: 700;
}

.calibration-detail {
  display: grid;
  gap: 18px;
}

.calibration-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.calibration-detail__header h2 {
  margin: 0;
  font-size: 22px;
  letter-spacing: 0;
}

.calibration-detail__header p {
  margin: 6px 0 0;
}

.calibration-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 0.86fr) minmax(0, 1.14fr);
  gap: 18px;
}

.calibration-detail__panel {
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 18px;
  background: var(--surface);
}

.calibration-detail__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.calibration-detail__panel-head--wrap {
  align-items: flex-start;
  flex-wrap: wrap;
}

.calibration-detail__panel h3 {
  margin: 0;
  font-size: 16px;
  letter-spacing: 0;
}

.calibration-detail__feedback {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.calibration-detail__feedback div,
.calibration-detail__impact-stats article {
  display: grid;
  gap: 6px;
  min-height: 70px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}

.calibration-detail__feedback span,
.calibration-detail__impact-stats span {
  color: var(--text-secondary);
  font-size: 12px;
}

.calibration-detail__feedback strong,
.calibration-detail__impact-stats strong {
  color: var(--text-primary);
  font-size: 20px;
}

.calibration-detail__timeline-item {
  display: grid;
  gap: 8px;
}

.calibration-detail__timeline-item > div:first-child {
  display: flex;
  align-items: center;
  gap: 10px;
}

.calibration-detail__audit {
  display: grid;
  gap: 10px;
}

.calibration-detail__audit-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.calibration-detail__snapshot {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.calibration-detail__snapshot pre {
  max-height: 260px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: var(--radius-md);
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.5;
}

.calibration-detail__impact-form {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}

.calibration-detail__impact-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 1200px) {
  .calibration-detail__grid,
  .calibration-detail__snapshot,
  .calibration-detail__impact-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .calibration-detail__header {
    display: grid;
  }

  .calibration-detail__feedback {
    grid-template-columns: 1fr;
  }
}
</style>
