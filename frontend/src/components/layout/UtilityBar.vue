<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.signOut()
  router.push('/login')
}
</script>

<template>
  <div class="utility-bar">
    <div
      class="container utility-bar__inner d-flex align-items-center justify-content-between gap-3"
    >
      <span class="utility-tagline">安心選物・透明交易・平台保障</span>
      <nav class="utility-nav d-flex align-items-center" aria-label="Utility navigation">
        <RouterLink to="/" class="utility-link">首頁</RouterLink>
        <a href="#latest" class="utility-link">最新消息</a>
        <a href="#support" class="utility-link">客服中心</a>
        <a href="#help" class="utility-link">幫助中心</a>
        <template v-if="authStore.isAuthenticated">
          <button type="button" class="language-button" @click="logout">登出</button>
        </template>
        <template v-else>
          <span class="utility-auth-links">
            <RouterLink to="/register" class="utility-link">註冊</RouterLink>
            <span class="utility-separator" aria-hidden="true">|</span>
            <RouterLink to="/login" class="utility-link">登入</RouterLink>
          </span>
        </template>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.utility-bar {
  position: sticky;
  top: 0;
  z-index: 1040;
  min-height: 32px;
  color: var(--color-surface);
  background: var(--color-primary-active);
}
.utility-bar .container {
  min-height: inherit;
  max-width: 1440px;
}
.utility-tagline {
  font-size: var(--font-size-xs);
  white-space: nowrap;
}
.utility-nav {
  gap: 20px;
}
.utility-link,
.language-button,
.utility-separator {
  color: inherit;
  font-size: var(--font-size-xs);
  text-decoration: none;
  white-space: nowrap;
}
.utility-link:hover,
.utility-link:focus-visible,
.language-button:hover,
.language-button:focus-visible {
  color: var(--color-surface);
  text-decoration: underline;
}
.language-button {
  padding: 0;
  border: 0;
  background: transparent;
}
.utility-separator {
  opacity: 0.65;
}
.utility-auth-links {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

@media (max-width: 575.98px) {
  .utility-tagline {
    display: none;
  }
  .utility-nav {
    width: 100%;
    justify-content: flex-end;
    gap: var(--space-3);
  }
}
</style>
