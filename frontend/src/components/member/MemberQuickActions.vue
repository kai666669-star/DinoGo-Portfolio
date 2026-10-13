<script setup>
import { RouterLink } from 'vue-router'

const actions = [
  {
    title: '查看全部訂單',
    description: '追蹤訂單與配送進度',
    icon: 'bi-list-check',
    to: { name: 'MemberOrders' },
  },
  {
    title: '管理收件地址',
    description: '新增或調整預設地址',
    icon: 'bi-geo-alt',
    to: { name: 'MemberAddresses' },
  },
  {
    title: '編輯個人資料',
    description: '更新聯絡與會員資訊',
    icon: 'bi-person',
    to: { name: 'MemberProfile' },
  },
  {
    title: '修改密碼',
    description: '管理帳戶登入安全',
    icon: 'bi-key',
    to: { name: 'MemberPassword' },
  },
  {
    title: '聯絡客服',
    description: '取得訂單與平台協助',
    icon: 'bi-headset',
    to: { name: 'MemberMessages' },
  },
]
</script>

<template>
  <aside class="quick-actions" aria-labelledby="quick-actions-title">
    <header class="quick-actions__header">
      <h2 id="quick-actions-title">快捷操作</h2>
      <p>快速前往常用會員功能</p>
    </header>
    <nav aria-label="會員快捷操作">
      <RouterLink
        v-for="action in actions"
        :key="action.title"
        class="quick-actions__link"
        :to="action.to"
      >
        <span class="quick-actions__icon" aria-hidden="true"
          ><i class="bi" :class="action.icon"></i
        ></span>
        <span class="quick-actions__copy"
          ><strong>{{ action.title }}</strong
          ><small>{{ action.description }}</small></span
        >
        <i class="bi bi-chevron-right" aria-hidden="true"></i>
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.quick-actions {
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.quick-actions__header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}
.quick-actions__header h2,
.quick-actions__header p {
  margin: 0;
}
.quick-actions__header h2 {
  color: var(--color-text);
  font-size: var(--font-size-md);
  font-weight: 700;
}
.quick-actions__header p {
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 10px;
}
.quick-actions__link {
  display: flex;
  min-height: 80px;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4);
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border);
}
.quick-actions__link:last-child {
  border-bottom: 0;
}
.quick-actions__link:hover {
  color: var(--color-primary-active);
  background: var(--color-primary-50);
}
.quick-actions__link:focus-visible {
  outline: none;
  box-shadow: inset var(--shadow-focus);
}
.quick-actions__icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  color: var(--color-primary-active);
  font-size: 18px;
  place-items: center;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
}
.quick-actions__copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}
.quick-actions__copy strong {
  font-size: var(--font-size-sm);
}
.quick-actions__copy small {
  color: var(--color-text-muted);
  font-size: 10px;
}
.quick-actions__link > .bi-chevron-right {
  margin-left: auto;
  color: var(--color-text-subtle);
  font-size: 16px;
}
</style>
