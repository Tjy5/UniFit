<template>
  <div class="el-carousel">
    <div class="el-carousel__track">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.el-carousel {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.el-carousel__track {
  display: flex;
  gap: 12px;
  height: 100%;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
}
</style>
