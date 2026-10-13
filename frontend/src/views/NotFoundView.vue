<script setup>
import { RouterLink } from 'vue-router'

import ErrorStatePanel from '@/components/error/ErrorStatePanel.vue'
</script>

<template>
  <ErrorStatePanel code="404" label="找不到頁面" heading="你要前往的頁面不存在" description="網址可能輸入錯誤，或此頁面已經移動。">
    <template #actions>
      <RouterLink class="error-page__button dg-btn-primary dg-focus-ring" :to="{ name: 'Home' }">回到首頁</RouterLink>
    </template>
  </ErrorStatePanel>
</template>
