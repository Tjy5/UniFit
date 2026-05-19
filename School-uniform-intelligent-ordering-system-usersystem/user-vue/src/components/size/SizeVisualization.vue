<template>
  <div v-if="visibleMatches.length" class="size-visualization" :class="{ 'size-visualization--compact': compact }">
    <div v-for="match in visibleMatches" :key="match.dimensionKey" class="size-visualization__row">
      <div class="size-visualization__meta">
        <div class="size-visualization__label">{{ match.label }}</div>
        <div class="size-visualization__range">
          {{ formatValue(match.userValue) }} / {{ formatRange(match.minValue, match.maxValue) }}
        </div>
      </div>
      <div class="size-visualization__bar">
        <div
          class="size-visualization__fill"
          :class="{ 'is-warning': match.withinRange === false }"
          :style="{ width: `${match.matchPercent || 0}%` }"
        ></div>
      </div>
      <div class="size-visualization__message" :class="{ 'is-warning': match.withinRange === false }">
        {{ match.message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  matches: {
    type: Array,
    default: () => [],
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const visibleMatches = computed(() => (props.matches || []).filter(Boolean))

function formatValue(value) {
  return value == null ? '-' : `${Number(value).toFixed(1)}`
}

function formatRange(min, max) {
  if (min != null && max != null) {
    return `${Number(min).toFixed(1)}-${Number(max).toFixed(1)}`
  }
  if (min != null) {
    return `>= ${Number(min).toFixed(1)}`
  }
  if (max != null) {
    return `<= ${Number(max).toFixed(1)}`
  }
  return '未配置'
}
</script>

<style scoped>
.size-visualization {
  display: grid;
  gap: 10px;
}

.size-visualization__row {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 250, 245, 0.82);
  border: 1px solid rgba(107, 93, 79, 0.12);
}

.size-visualization__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.size-visualization__label {
  color: var(--mcm-brown);
  font-weight: 600;
}

.size-visualization__range {
  color: var(--mcm-text-soft);
}

.size-visualization__bar {
  height: 8px;
  margin: 8px 0 6px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(222, 208, 189, 0.56);
}

.size-visualization__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--mcm-olive), #91b37d);
}

.size-visualization__fill.is-warning {
  background: linear-gradient(90deg, var(--mcm-coral), #ffb188);
}

.size-visualization__message {
  font-size: 12px;
  color: var(--mcm-text-soft);
}

.size-visualization__message.is-warning {
  color: var(--mcm-coral);
}

.size-visualization--compact .size-visualization__row {
  padding: 10px;
}
</style>
