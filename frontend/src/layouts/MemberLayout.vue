<script setup>
import { RouterView } from 'vue-router'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import PrimaryNav from '@/components/layout/PrimaryNav.vue'
import UtilityBar from '@/components/layout/UtilityBar.vue'
import MemberNav from '@/components/member/MemberNav.vue'
</script>

<template>
  <div class="layout-shell d-flex min-vh-100 flex-column">
    <UtilityBar />
    <AppHeader />
    <PrimaryNav />
    <MemberNav />
    <main class="dg-member-layout-main flex-grow-1">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>
