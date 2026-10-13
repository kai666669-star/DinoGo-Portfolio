<script setup>
import { RouterView } from 'vue-router'
import AdminNav from '@/components/admin/AdminNav.vue'
</script>

<template>
  <div class="admin-layout">
    <AdminNav />
    <main class="admin-layout__main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 264px minmax(0, 1fr);
  overflow-x: clip;
  background: var(--color-bg);
}

.admin-layout__main {
  min-width: 0;
  width: 100%;
  padding: var(--space-6) 36px;
}

@media (max-width: 760px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-layout__main {
    padding: var(--space-4);
  }
}
</style>
