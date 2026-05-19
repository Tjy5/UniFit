<template>
  <section class="el-card">
    <header v-if="$slots.header" class="el-card__header">
      <slot name="header" />
    </header>
    <div class="el-card__body">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="el-card__footer">
      <slot name="footer" />
    </footer>
  </section>
</template>

<style scoped>
.el-card {
  border-radius: 16px;
  border: 1px solid rgba(107, 93, 79, 0.16);
  background: rgba(255, 250, 245, 0.92);
  box-shadow: var(--mcm-shadow-md);
  overflow: hidden;
}

.el-card__header,
.el-card__footer {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(107, 93, 79, 0.12);
}

.el-card__footer {
  border-top: 1px solid rgba(107, 93, 79, 0.12);
  border-bottom: 0;
}

.el-card__body {
  padding: 20px;
}
</style>
