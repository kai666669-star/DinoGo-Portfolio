<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const adminName = computed(() => authStore.memberName || '平台管理員')

const navItems = [
  { label: '總覽', to: '/admin/dashboard', icon: 'bi-speedometer2' },
  { label: '商家申請', to: '/admin/seller-applications', icon: 'bi-clipboard-check' },
  { label: '會員管理', to: '/admin/members', icon: 'bi-people' },
  { label: '訊息中心', to: '/admin/messages', icon: 'bi-chat-left-text' },
  { label: '系統設定', to: '/admin/settings', icon: 'bi-gear' },
]

function isItemActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}
</script>

<template>
  <aside class="admin-nav-shell">
    <RouterLink class="admin-nav-brand" to="/admin/dashboard">
      <span class="admin-nav-brand__mark" aria-hidden="true">D</span>
      <strong>DINO-GO 管理後台</strong>
    </RouterLink>

    <p class="admin-nav-heading">平台管理</p>

    <nav class="admin-nav" aria-label="管理後台導覽">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="admin-nav-link"
        :class="{ 'is-active': isItemActive(item) }"
      >
        <i class="bi" :class="item.icon" aria-hidden="true"></i>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="admin-nav-profile">
      <span class="admin-nav-profile__avatar" aria-hidden="true">管</span>
      <span>{{ adminName }}</span>
    </div>
  </aside>
</template>

<style scoped>
.admin-nav-shell {
  position: sticky;
  top: 0;
  display: grid;
  width: 264px;
  height: 100vh;
  min-height: 100vh;
  grid-template-rows: auto auto auto 1fr;
  gap: var(--space-5);
  padding: 28px var(--space-5);
  color: var(--color-surface);
  background: var(--color-primary-active);
  font-family: var(--font-body);
}

.admin-nav-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  color: var(--color-surface);
  text-decoration: none;
}

.admin-nav-brand__mark {
  font-size: var(--font-size-lg);
  font-weight: 700;
  line-height: 1;
}

.admin-nav-brand strong {
  font-size: var(--font-size-base);
  font-weight: 700;
}

.admin-nav-heading {
  margin: 0;
  color: var(--color-text-200);
  font-size: var(--font-size-xs);
}

.admin-nav {
  display: grid;
  align-content: start;
  gap: 6px;
}

.admin-nav-link {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  color: var(--color-text-100);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.admin-nav-link i {
  width: 20px;
  color: inherit;
  font-size: 18px;
  text-align: center;
}

.admin-nav-link:hover,
.admin-nav-link:focus-visible {
  color: var(--color-surface);
  background: rgba(255, 255, 255, 0.1);
}

.admin-nav-link.is-active {
  color: var(--color-surface);
  background: var(--color-primary);
  font-weight: 600;
}

.admin-nav-brand:focus-visible,
.admin-nav-link:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.admin-nav-profile {
  display: flex;
  align-self: end;
  align-items: center;
  gap: 10px;
  color: var(--color-surface);
  font-size: var(--font-size-sm);
}

.admin-nav-profile__avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

@media (max-width: 760px) {
  .admin-nav-shell {
    position: static;
    width: 100%;
    height: auto;
    min-height: 0;
    grid-template-columns: auto 1fr auto;
    grid-template-rows: auto auto;
    gap: var(--space-3);
    padding: var(--space-4);
  }

  .admin-nav-heading {
    display: none;
  }

  .admin-nav {
    grid-column: 1 / -1;
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .admin-nav-link {
    justify-content: center;
    padding-inline: var(--space-2);
  }

  .admin-nav-profile {
    align-self: center;
  }
}

@media (max-width: 560px) {
  .admin-nav {
    display: flex;
    overflow-x: auto;
  }

  .admin-nav-link {
    min-width: max-content;
  }
}
</style>
