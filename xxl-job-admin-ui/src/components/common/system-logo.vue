<script setup lang="ts">
const logoUrl = `${import.meta.env.BASE_URL}xxl-job-boost-logo.png`;
</script>

<template>
  <div class="app-logo">
    <img class="app-logo__image" :src="logoUrl" alt="XXL-JOB Boost" />
  </div>
</template>

<style scoped>
.app-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.app-logo__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
