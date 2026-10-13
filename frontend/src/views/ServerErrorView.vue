<script setup>
import ErrorStatePanel from '@/components/error/ErrorStatePanel.vue'

const reloadPage = () => {
  window.location.reload()
}
</script>

<template>
  <ErrorStatePanel code="500" label="系統暫時異常" heading="系統目前無法回應" description="請稍後再試，或重新整理頁面後繼續使用。">
    <template #actions>
      <button class="error-page__button dg-btn-primary dg-focus-ring" type="button" @click="reloadPage">重新整理</button>
    </template>
  </ErrorStatePanel>
</template>
