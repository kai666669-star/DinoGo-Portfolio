<script setup>
import { RouterView } from 'vue-router'
import SellerNav from '../components/seller/SellerNav.vue'
</script>

<template>
  <div class="seller-layout">
    <SellerNav />

    <main class="seller-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.seller-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  background: var(--color-bg-muted);
}

.seller-main {
  min-width: 0;
  padding: var(--space-5) var(--space-6);
}

@media (max-width: 760px) {
  .seller-layout {
    grid-template-columns: 1fr;
  }

  .seller-main {
    padding: var(--space-4);
  }
}
</style>
