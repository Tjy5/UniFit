<template>
  <div class="size-panel">
    <div v-if="loading" class="size-panel__loading">
      <el-skeleton :rows="compact ? 3 : 5" animated />
    </div>
    <div v-else-if="result.available && result.recommended" class="size-panel__content">
      <div class="size-panel__header">
        <div>
          <div class="size-panel__eyebrow">智能尺码推荐</div>
          <div class="size-panel__title">{{ result.recommended.sizeName }} 码</div>
        </div>
        <div class="size-panel__confidence">
          <el-tag :type="confidenceTagType">{{ confidenceLabel }}</el-tag>
          <span v-if="result.confidence != null">{{ result.confidence }}%</span>
        </div>
      </div>

      <p class="size-panel__message">{{ result.message }}</p>
      <p v-if="result.calibrationApplied" class="size-panel__calibration-hint">
        {{ calibrationHint }}
      </p>
      <p v-if="result.personalizationApplied" class="size-panel__personalization-hint">
        {{ personalizationHint }}
      </p>
      <p v-if="result.confidenceMessage" class="size-panel__confidence-message">{{ result.confidenceMessage }}</p>
      <el-alert
        v-if="result.lowConfidence"
        class="size-panel__alert"
        type="warning"
        :closable="false"
        show-icon
        :title="result.confidenceMessage || '匹配把握较低，建议手动确认尺码表'"
      />

      <div v-if="result.reasons.length" class="size-panel__reasons">
        <div v-for="reason in result.reasons" :key="reason" class="size-panel__reason">{{ reason }}</div>
      </div>

      <div v-if="result.alternatives.length" class="size-panel__alternatives">
        <div class="size-panel__alt-header">
          <span class="size-panel__alt-label">备选尺码</span>
          <button type="button" class="size-panel__compare-trigger" @click="toggleAlternativeComparison">
            {{ showAlternativeComparison ? '收起其他尺码' : '查看其他尺码' }}
          </button>
        </div>
        <div class="size-panel__alt-tags">
          <el-tag v-for="item in result.alternatives" :key="item.sizeName" effect="plain">
            {{ item.sizeName }} 码
          </el-tag>
        </div>
      </div>

      <size-visualization
        v-if="showVisualization"
        :matches="result.recommended.dimensionMatches"
        :compact="compact"
      />

      <div v-if="showAlternativeComparison && comparisonCandidates.length > 1" class="size-panel__comparison">
        <div class="size-panel__comparison-header">
          <div class="size-panel__comparison-title">尺码匹配度对比</div>
          <p class="size-panel__comparison-hint">对比主推荐与备选尺码在各维度上的匹配情况</p>
        </div>

        <div class="size-panel__comparison-grid" :class="{ 'size-panel__comparison-grid--compact': compact }">
          <article
            v-for="candidate in comparisonCandidates"
            :key="candidate.sizeName"
            class="size-panel__candidate"
            :class="{ 'is-recommended': candidate.isRecommended }"
          >
            <div class="size-panel__candidate-header">
              <div>
                <div class="size-panel__candidate-size">{{ candidate.sizeName }} 码</div>
                <div v-if="candidate.scoreLabel" class="size-panel__candidate-score">{{ candidate.scoreLabel }}</div>
              </div>
              <el-tag :type="candidate.isRecommended ? 'success' : 'info'" effect="plain">
                {{ candidate.badge }}
              </el-tag>
            </div>

            <div v-if="candidate.summaryReasons.length" class="size-panel__candidate-reasons">
              <span
                v-for="reason in candidate.summaryReasons"
                :key="`${candidate.sizeName}-${reason}`"
                class="size-panel__candidate-reason"
              >
                {{ reason }}
              </span>
            </div>

            <size-visualization :matches="candidate.dimensionMatches" :compact="compact" />
          </article>
        </div>
      </div>
    </div>
    <div v-else-if="result.message" class="size-panel__empty">
      {{ result.message }}
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import SizeVisualization from './SizeVisualization.vue'

const props = defineProps({
  result: {
    type: Object,
    default: () => ({}),
  },
  loading: {
    type: Boolean,
    default: false,
  },
  compact: {
    type: Boolean,
    default: false,
  },
  showVisualization: {
    type: Boolean,
    default: true,
  },
})

const showAlternativeComparison = ref(false)

const confidenceLabel = computed(() => {
  if (props.result?.confidenceLevel === 'HIGH') return '匹配把握较高'
  if (props.result?.confidenceLevel === 'MEDIUM') return '匹配把握中等'
  if (props.result?.confidenceLevel === 'LOW') return '建议确认'
  return '待评估'
})

const confidenceTagType = computed(() => {
  if (props.result?.confidenceLevel === 'HIGH') return 'success'
  if (props.result?.confidenceLevel === 'MEDIUM') return 'warning'
  if (props.result?.confidenceLevel === 'LOW') return 'danger'
  return 'info'
})

const calibrationHint = computed(() => {
  const details = props.result?.calibrationDetails || {}
  const baseName = details.baseBestSizeName
  const calibratedName = details.calibratedBestSizeName
  if (baseName && calibratedName && baseName !== calibratedName) {
    return `群体反馈将推荐从 ${baseName} 码微调为 ${calibratedName} 码`
  }
  return '已参考群体尺码反馈微调推荐顺序'
})

const personalizationHint = computed(() => {
  if (props.result?.preferenceSummary) {
    return props.result.preferenceSummary
  }
  const details = props.result?.personalizationDetails || {}
  if (details.preferenceSource === 'EXPLICIT') {
    return '已参考您主动设置的穿着偏好'
  }
  return '已参考历史穿着反馈微调推荐顺序'
})

const comparisonCandidates = computed(() => {
  const candidates = [props.result?.recommended, ...(props.result?.alternatives || [])].filter((item) => item?.sizeName)
  return candidates.map((candidate, index) => ({
    ...candidate,
    badge: index === 0 ? '主推荐' : `备选 ${index}`,
    isRecommended: index === 0,
    summaryReasons: Array.isArray(candidate.reasons) ? candidate.reasons.slice(0, 2) : [],
    scoreLabel: candidate.score == null ? '' : `综合匹配度 ${Math.round(Number(candidate.score))}%`,
  }))
})

watch(
  () => [props.result?.recommended?.sizeName, (props.result?.alternatives || []).map((item) => item?.sizeName).join(',')],
  () => {
    showAlternativeComparison.value = false
  }
)

function toggleAlternativeComparison() {
  showAlternativeComparison.value = !showAlternativeComparison.value
}
</script>

<style scoped>
.size-panel {
  display: grid;
  gap: 12px;
}

.size-panel__content {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 250, 245, 0.88);
  border: 1px solid rgba(107, 93, 79, 0.12);
}

.size-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.size-panel__eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--mcm-text-soft);
  text-transform: uppercase;
}

.size-panel__title {
  font-size: 24px;
  font-weight: 700;
  color: var(--mcm-brown);
}

.size-panel__confidence {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--mcm-text-soft);
}

.size-panel__message,
.size-panel__calibration-hint,
.size-panel__personalization-hint,
.size-panel__confidence-message {
  margin: 0;
  color: var(--mcm-text-soft);
  line-height: 1.5;
}

.size-panel__calibration-hint {
  color: var(--mcm-olive);
  font-size: 13px;
  font-weight: 600;
}

.size-panel__personalization-hint {
  color: var(--mcm-brown);
  font-size: 13px;
  font-weight: 600;
}

.size-panel__reasons {
  display: grid;
  gap: 8px;
}

.size-panel__reason {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(245, 230, 211, 0.32);
  color: var(--mcm-text-soft);
  font-size: 13px;
}

.size-panel__alternatives {
  display: grid;
  gap: 10px;
}

.size-panel__alt-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.size-panel__alt-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-panel__alt-label {
  color: var(--mcm-text-faint);
  font-size: 13px;
}

.size-panel__compare-trigger {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--mcm-brown);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.size-panel__compare-trigger:hover {
  color: var(--mcm-olive);
}

.size-panel__comparison {
  display: grid;
  gap: 12px;
  margin-top: 6px;
}

.size-panel__comparison-header {
  display: grid;
  gap: 4px;
}

.size-panel__comparison-title {
  color: var(--mcm-brown);
  font-size: 15px;
  font-weight: 700;
}

.size-panel__comparison-hint {
  margin: 0;
  color: var(--mcm-text-soft);
  font-size: 13px;
  line-height: 1.5;
}

.size-panel__comparison-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.size-panel__comparison-grid--compact {
  grid-template-columns: 1fr;
}

.size-panel__candidate {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(107, 93, 79, 0.12);
}

.size-panel__candidate.is-recommended {
  border-color: rgba(126, 147, 110, 0.42);
  box-shadow: inset 0 0 0 1px rgba(126, 147, 110, 0.16);
}

.size-panel__candidate-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.size-panel__candidate-size {
  color: var(--mcm-brown);
  font-size: 18px;
  font-weight: 700;
}

.size-panel__candidate-score {
  color: var(--mcm-text-soft);
  font-size: 12px;
  margin-top: 4px;
}

.size-panel__candidate-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-panel__candidate-reason {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 230, 211, 0.36);
  color: var(--mcm-text-soft);
  font-size: 12px;
}

.size-panel__alert {
  margin-top: 4px;
}

.size-panel__empty {
  font-size: 13px;
  color: var(--mcm-text-soft);
}
</style>
