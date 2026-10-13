<script setup>
import { useRouter } from 'vue-router'

import ErrorStatePanel from '@/components/error/ErrorStatePanel.vue'

const router = useRouter()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push({ name: 'Home' })
}
</script>

<template>
  <ErrorStatePanel code="403" label="權限不足" heading="你沒有權限存取此頁面" description="請返回上一頁，或切換具備權限的帳號後再試。">
    <template #actions>
      <button class="error-page__button dg-btn-primary dg-focus-ring" type="button" @click="goBack">返回上一頁</button>
    </template>
  </ErrorStatePanel>
</template>
