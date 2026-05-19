<template>
  <section class="mcm-hero" :class="$attrs.class">
    <div class="mcm-hero__copy">
      <span v-if="eyebrow" class="mcm-hero__eyebrow">{{ eyebrow }}</span>
      <h1 class="mcm-hero__title">{{ title }}</h1>
      <p v-if="description" class="mcm-hero__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="mcm-hero__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup>
defineOptions({ inheritAttrs: false })

defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<style scoped>
.mcm-hero {
  display: grid;
  gap: 18px;
  margin-bottom: 28px;
  padding: 32px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
  position: relative;
  overflow: hidden;
}

.mcm-hero::before {
  display: none;
}

.mcm-hero__copy,
.mcm-hero__actions {
  position: relative;
  z-index: 1;
}

.mcm-hero__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 0;
}

.mcm-hero__title {
  margin: 14px 0 10px;
  font-size: 28px;
  color: var(--text-primary);
  font-weight: 700;
  letter-spacing: 0;
}

.mcm-hero__description {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
