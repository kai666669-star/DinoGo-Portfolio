<script setup>
import { RouterView } from 'vue-router'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import UtilityBar from '@/components/layout/UtilityBar.vue'
</script>

<template>
  <div class="layout-shell d-flex min-vh-100 flex-column">
    <UtilityBar />
    <AppHeader />
    <main class="flex-grow-1"><RouterView /></main>
    <AppFooter />
  </div>
</template>
