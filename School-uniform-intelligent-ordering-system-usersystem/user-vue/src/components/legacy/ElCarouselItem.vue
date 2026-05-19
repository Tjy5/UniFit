<template>
  <div class="el-carousel-item">
    <slot />
  </div>
</template>

<style scoped>
.el-carousel-item {
  min-width: 100%;
  height: 100%;
  scroll-snap-align: start;
}
</style>
