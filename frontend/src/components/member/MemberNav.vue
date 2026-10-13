<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const memberName = computed(() => {
  const { lastName = '', firstName = '' } = authStore.member ?? {}
  return `${lastName}${firstName}`.trim() || '會員'
})

// 會員可能同時擁有多個角色，顯示時以管理員、商家、一般會員的順序決定標籤。
const memberLevel = computed(() => {
  if (authStore.hasRole('admin')) return '管理員'
  if (authStore.hasRole('seller')) return '商家會員'
  return '一般會員'
})

// 商家申請屬於一般會員的服務入口；商家與管理員分別從其專屬後台操作。
const canAccessMerchantService = computed(
  () => authStore.hasRole('buyer') && !authStore.hasRole('seller') && !authStore.hasRole('admin'),
)

const memberItems = [
  { label: '總覽', to: '/member/overview', icon: 'bi-grid' },
  { label: '訂單', to: '/member/orders', icon: 'bi-box-seam' },
  { label: '收藏', to: '/member/favorites', icon: 'bi-heart' },
  { label: '優惠券', to: '/member/coupons', icon: 'bi-ticket-perforated' },
  { label: '訊息', to: '/member/messages', icon: 'bi-bell' },
]

// 帳戶設定包含會員資料、地址與密碼，任一子頁啟用時父選單保持 active。
const accountItems = [
  { label: '個人資料', routeName: 'MemberProfile', icon: 'bi-person-vcard' },
  { label: '地址管理', routeName: 'MemberAddresses', icon: 'bi-geo-alt' },
  { label: '修改密碼', routeName: 'MemberPassword', icon: 'bi-key' },
  { label: '註銷帳號', routeName: 'MemberDeactivateAccount', icon: 'bi-person-x' },
]
const isAccountActive = computed(() => accountItems.some((item) => item.routeName === route.name))
const accountMenu = ref(null)
const isAccountMenuOpen = ref(false)

function toggleAccountMenu() {
  isAccountMenuOpen.value = !isAccountMenuOpen.value
}

function closeAccountMenu() {
  isAccountMenuOpen.value = false
}

function closeAccountMenuOnOutsidePointer(event) {
  if (accountMenu.value && !accountMenu.value.contains(event.target)) {
    closeAccountMenu()
  }
}

onMounted(() => document.addEventListener('pointerdown', closeAccountMenuOnOutsidePointer))
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeAccountMenuOnOutsidePointer))
</script>

<template>
  <nav class="dg-member-nav" aria-label="會員中心導覽">
    <div class="container dg-member-nav-inner">
      <div class="dg-member-profile">
        <span class="dg-member-avatar" aria-hidden="true">D</span>
        <span class="dg-member-profile-copy">
          <strong class="dg-member-name">{{ memberName }}</strong>
          <span class="dg-member-level">{{ memberLevel }}</span>
        </span>
      </div>

      <div class="dg-member-menu" role="list">
        <RouterLink
          v-for="item in memberItems"
          :key="item.label"
          :to="item.to"
          class="dg-member-nav-link"
          role="listitem"
        >
          <i class="bi" :class="item.icon" aria-hidden="true"></i>
          <span>{{ item.label }}</span>
        </RouterLink>

        <RouterLink
          v-if="canAccessMerchantService"
          to="/member/seller-application"
          class="dg-member-nav-link"
        >
          <i class="bi bi-shop" aria-hidden="true"></i>
          <span>商家服務</span>
        </RouterLink>

        <!-- 帳戶設定使用下拉選單切換會員資料、地址與密碼。 -->
        <div
          ref="accountMenu"
          class="dropdown dg-member-account-menu"
          role="listitem"
          @keydown.esc="closeAccountMenu"
        >
          <button
            class="dg-member-nav-link dg-member-account-button"
            :class="{ 'is-active': isAccountActive }"
            type="button"
            :aria-expanded="isAccountMenuOpen"
            @click="toggleAccountMenu"
          >
            <i class="bi bi-person" aria-hidden="true"></i>
            <span>帳戶設定</span>
            <i class="bi bi-chevron-down dg-member-account-arrow" aria-hidden="true"></i>
          </button>

          <ul
            class="dropdown-menu dropdown-menu-end dg-member-account-dropdown"
            :class="{ show: isAccountMenuOpen }"
          >
            <li v-for="item in accountItems" :key="item.routeName">
              <RouterLink
                :to="{ name: item.routeName }"
                class="dropdown-item dg-member-account-item"
                active-class="is-active"
                @click="closeAccountMenu"
              >
                <i class="bi" :class="item.icon" aria-hidden="true"></i>
                <span>{{ item.label }}</span>
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.dg-member-nav {
  --member-nav-height: 84px;
  min-height: var(--member-nav-height);
  color: var(--color-text);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.dg-member-nav-inner {
  display: flex;
  height: calc(var(--member-nav-height) - 1px);
  max-width: 1440px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.dg-member-profile {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-3);
}

.dg-member-avatar {
  display: inline-grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  color: var(--color-primary-700);
  font-size: var(--font-size-base);
  font-weight: 700;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--color-primary-100);
}

.dg-member-profile-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dg-member-name {
  color: var(--color-text);
  font-size: 15px;
  line-height: var(--line-height-heading);
}

.dg-member-level {
  color: var(--color-text-muted);
  font-size: 11px;
}

.dg-member-menu {
  display: flex;
  min-width: 0;
  align-self: stretch;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-1);
}

.dg-member-nav-link {
  position: relative;
  display: inline-flex;
  min-width: 0;
  min-height: 0;
  align-self: stretch;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 14px;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 400;
  text-decoration: none;
  white-space: nowrap;
  border-radius: 0;
}

.dg-member-nav-link > .bi:not(.dg-member-account-arrow) {
  color: var(--color-text-muted);
  font-size: 17px;
}

.dg-member-nav-link::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 0;
  content: '';
  background: var(--color-primary);
}

.dg-member-nav-link:hover,
.dg-member-nav-link:focus-visible {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.dg-member-nav-link:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.dg-member-nav-link.router-link-active {
  color: var(--color-primary);
  font-weight: 600;
}

.dg-member-nav-link.router-link-active > .bi {
  color: var(--color-primary-active);
}

.dg-member-nav-link.router-link-active::after {
  height: 3px;
}

.dg-member-account-button {
  min-height: 40px;
  align-self: center;
  justify-content: center;
  padding-inline: var(--space-3);
  background: var(--color-bg);
  border: 0;
  border-radius: var(--radius-sm);
}

/* 帳戶設定以按鈕底色標示目前會員頁面，不重複顯示導覽底線。 */
.dg-member-account-button.is-active {
  color: var(--color-primary);
}

.dg-member-account-button::after {
  content: none;
}

.dg-member-account-arrow {
  margin-left: var(--space-2);
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
  transition: transform 160ms ease;
}

.dg-member-account-button[aria-expanded='true'] .dg-member-account-arrow {
  transform: rotate(180deg);
}

.dg-member-account-button[aria-expanded='true'] {
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
}

/* 帳戶設定下拉選單。 */
.dg-member-account-menu {
  flex: 0 0 auto;
}

/* 面板與帳戶設定按鈕共用寬度，延續同一塊背景。 */
.dg-member-account-dropdown {
  width: 100%;
  min-width: 100%;
  margin-top: 0 !important;
  padding: 6px;
  background: var(--color-secondary-100);
  border: 1px solid var(--color-border);
  border-top: 0;
  border-radius: var(--radius-lg);
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  box-shadow: none;
}

.dg-member-account-item {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  width: calc(100% - var(--space-2));
  gap: var(--space-1);
  margin-right: auto;
  margin-left: auto;
  padding: var(--space-3) var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  line-height: var(--line-height-base);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
}

/* 下拉選項以深色框線呈現目前頁面，避免只有底色時辨識度不足。 */
.dg-member-account-item:hover,
.dg-member-account-item:focus {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}

.dg-member-account-item.is-active {
  color: var(--color-primary-active);
  font-weight: 700;
  background: transparent;
  border-top-color: var(--color-primary-active);
  border-right-color: transparent;
  border-bottom-color: var(--color-primary-active);
  border-left-color: transparent;
  border-radius: 0;
  box-shadow: none;
}

.dg-member-account-item.is-active:hover,
.dg-member-account-item.is-active:focus {
  background: transparent;
}

/* Tablet 以水平捲動保留單列導覽，避免項目換行而拉高 MemberNav。 */
@media (max-width: 1199.98px) {
  .dg-member-nav {
    overflow-x: auto;
  }

  .dg-member-nav-inner {
    width: max-content;
    min-width: 100%;
    gap: var(--space-5);
  }

  .dg-member-menu {
    flex-wrap: nowrap;
  }
}

@media (max-width: 767.98px) {
  .dg-member-nav-inner {
    padding-inline: var(--space-4);
  }
}
</style>
