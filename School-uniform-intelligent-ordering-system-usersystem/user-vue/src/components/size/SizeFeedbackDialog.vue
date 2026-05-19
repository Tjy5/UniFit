<template>
  <el-dialog :visible="modelValue" title="尺码反馈" width="560px" @close="handleClose">
    <div v-if="loading" class="feedback-dialog__loading">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <div class="feedback-dialog__summary">
        <div>
          <div class="feedback-dialog__eyebrow">订单项</div>
          <div class="feedback-dialog__title">{{ orderItem?.uniformNameSnapshot || '当前商品' }}</div>
        </div>
        <el-tag effect="plain">已购尺码：{{ orderItem?.sizeNameSnapshot || '-' }}</el-tag>
      </div>

      <size-recommendation-panel
        :result="recommendation"
        :loading="recommendationLoading"
        :compact="true"
      />

      <el-form label-position="top">
        <el-form-item label="穿着反馈">
          <el-radio-group v-model="form.satisfaction">
            <el-radio-button label="FIT">合身</el-radio-button>
            <el-radio-button label="TOO_LARGE">偏大</el-radio-button>
            <el-radio-button label="TOO_SMALL">偏小</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.satisfaction !== 'FIT'" label="问题部位">
          <el-checkbox-group v-model="form.issueParts">
            <el-checkbox label="shoulder">肩宽</el-checkbox>
            <el-checkbox label="chest">胸围</el-checkbox>
            <el-checkbox label="waist">腰围</el-checkbox>
            <el-checkbox label="length">长度</el-checkbox>
            <el-checkbox label="sleeve">袖长</el-checkbox>
          </el-checkbox-group>
          <p class="feedback-dialog__field-hint">问题部位会用于历史反馈摘要，不会单独决定推荐尺码。</p>
        </el-form-item>

        <el-form-item label="个人尺码偏好">
          <el-radio-group v-model="form.fitPreference">
            <el-radio-button label="">不设置</el-radio-button>
            <el-radio-button label="LOOSE">宽松</el-radio-button>
            <el-radio-button label="STANDARD">标准</el-radio-button>
            <el-radio-button label="SLIM">修身</el-radio-button>
          </el-radio-group>
          <p class="feedback-dialog__field-hint">主动偏好会优先于系统从历史反馈学习出的松紧倾向。</p>
        </el-form-item>

        <el-form-item label="补充说明">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="可选，补充具体穿着感受" />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存反馈</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch } from 'vue'
import SizeRecommendationPanel from './SizeRecommendationPanel.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  submitting: {
    type: Boolean,
    default: false,
  },
  orderItem: {
    type: Object,
    default: () => null,
  },
  existingFeedback: {
    type: Object,
    default: () => null,
  },
  recommendation: {
    type: Object,
    default: () => ({}),
  },
  recommendationLoading: {
    type: Boolean,
    default: false,
  },
  preference: {
    type: Object,
    default: () => null,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const form = reactive({
  satisfaction: 'FIT',
  issueParts: [],
  fitPreference: '',
  note: '',
})

watch(
  () => [props.modelValue, props.existingFeedback, props.preference],
  () => {
    if (!props.modelValue) return
    form.satisfaction = props.existingFeedback?.satisfaction || 'FIT'
    form.issueParts = Array.isArray(props.existingFeedback?.issueParts) ? [...props.existingFeedback.issueParts] : []
    form.fitPreference = props.preference?.explicitPreference || ''
    form.note = props.existingFeedback?.note || ''
  },
  { immediate: true },
)

function handleClose() {
  emit('update:modelValue', false)
}

function submit() {
  emit('submit', {
    satisfaction: form.satisfaction,
    issueParts: form.satisfaction === 'FIT' ? [] : [...form.issueParts],
    note: form.note,
    fitPreference: form.fitPreference || '',
    purchasedSize: props.orderItem?.sizeNameSnapshot || '',
    recommendedSize: props.existingFeedback?.recommendedSize || props.recommendation?.recommended?.sizeName || '',
  })
}
</script>

<style scoped>
.feedback-dialog__loading {
  padding: 8px 0 16px;
}

.feedback-dialog__summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.feedback-dialog__eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mcm-text-soft);
}

.feedback-dialog__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--mcm-brown);
}

.feedback-dialog__field-hint {
  margin: 8px 0 0;
  color: var(--mcm-text-soft);
  font-size: 12px;
  line-height: 1.5;
}
</style>
