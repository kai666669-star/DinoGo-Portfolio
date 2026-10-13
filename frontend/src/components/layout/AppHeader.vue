<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import SearchBar from './SearchBar.vue'
import headerLogoUrl from '@/assets/images/dinogo-logo-s.png'
import {
  getMemberInbox,
  getMemberUnreadCount,
  MEMBER_UNREAD_CHANGED_EVENT,
} from '@/api/memberMessageApi.js'
import { useCartStore } from '@/stores/cart'
import { getPersistedToken } from '@/utils/auth-session.js'

const cartStore = useCartStore()
const notificationItems = ref([])
const unreadCount = ref(0)
const notificationPopoverOpen = ref(false)
const latestNotifications = computed(() => notificationItems.value.slice(0, 3))
const hasMoreNotifications = computed(() => notificationItems.value.length > 3)

const memberInboxCategories = ['SYSTEM_INBOX', 'ORDER_INBOX', 'SELLER_INBOX']
const notificationSyncIntervalMs = 500
let notificationRefreshPending = false
let notificationSyncTimer = null

async function fetchNotificationItems() {
  const inboxResults = await Promise.allSettled(
    memberInboxCategories.map((category) => getMemberInbox(category)),
  )

  notificationItems.value = inboxResults
    .filter((result) => result.status === 'fulfilled')
    .flatMap((result) => result.value.data.items ?? [])
    .sort((left, right) => {
      const timeDifference = Date.parse(right.recordCreatedAt) - Date.parse(left.recordCreatedAt)
      return timeDifference || right.recordId - left.recordId
    })
    .slice(0, 4)
}

async function fetchNotificationPreview() {
  const [unreadResult] = await Promise.allSettled([getMemberUnreadCount(), fetchNotificationItems()])

  if (unreadResult.status === 'fulfilled') {
    unreadCount.value = unreadResult.value.data.unreadCount ?? 0
  }
}

async function syncNotificationCount() {
  if (notificationRefreshPending || !getPersistedToken() || document.hidden) return
  notificationRefreshPending = true
  try {
    const response = await getMemberUnreadCount()
    const nextUnreadCount = response.data.unreadCount ?? 0
    if (nextUnreadCount !== unreadCount.value) {
      unreadCount.value = nextUnreadCount
      await fetchNotificationItems()
    }
  } catch {
    // 背景同步失敗時保留目前畫面，下一個 500ms 週期會自動重試。
  } finally {
    notificationRefreshPending = false
  }
}

async function refreshNotifications() {
  if (notificationRefreshPending || !getPersistedToken()) return
  notificationRefreshPending = true
  try {
    await fetchNotificationPreview()
  } finally {
    notificationRefreshPending = false
  }
}

function closeNotificationPopover() {
  notificationPopoverOpen.value = false
}

function openNotificationPopover() {
  notificationPopoverOpen.value = true
  refreshNotifications()
}

function handlePageVisible() {
  if (!document.hidden) refreshNotifications()
}

onMounted(async () => {
  const token = getPersistedToken()

  // 沒登入，不取得購物車
  if (!token) {
    console.log('🛒 未登入，不取得購物車')
    return
  }

  try {
    await Promise.all([cartStore.fetchCart(), fetchNotificationPreview()])
  } catch (error) {
    console.error('Header 資料載入失敗:', error)
  }
  notificationSyncTimer = window.setInterval(syncNotificationCount, notificationSyncIntervalMs)
  window.addEventListener('focus', refreshNotifications)
  document.addEventListener('visibilitychange', handlePageVisible)
})

window.addEventListener(MEMBER_UNREAD_CHANGED_EVENT, refreshNotifications)
onBeforeUnmount(() => {
  if (notificationSyncTimer != null) window.clearInterval(notificationSyncTimer)
  window.removeEventListener('focus', refreshNotifications)
  document.removeEventListener('visibilitychange', handlePageVisible)
  window.removeEventListener(MEMBER_UNREAD_CHANGED_EVENT, refreshNotifications)
})
</script>
<template>
  <header class="app-header">
    <div class="container app-header__main d-flex align-items-center">
      <RouterLink class="brand-mark flex-shrink-0" to="/" aria-label="DinoGo 首頁">
        <span class="brand-mark__logo" aria-hidden="true">
          <img :src="headerLogoUrl" alt="" class="brand-mark__logo-image" />
        </span>
        <span class="brand-mark__copy">
          <span class="brand-mark__name">DINO-GO</span>
          <span class="brand-mark__tagline">好物，慢慢挑</span>
        </span>
      </RouterLink>
      <div class="header-search flex-grow-1 d-none d-md-block"><SearchBar /></div>
      <nav class="header-actions d-flex align-items-center gap-2" aria-label="Header actions">
        <RouterLink class="header-action" to="/member/favorites" aria-label="收藏"
          ><i class="bi bi-heart" aria-hidden="true"></i
          ><span class="header-action__label">收藏</span></RouterLink
        >
        <div
          class="notification-region"
          @mouseenter="openNotificationPopover"
          @mouseleave="closeNotificationPopover"
        >
          <RouterLink
            class="header-action header-action--badge"
            :to="{ name: 'MemberMessages' }"
            aria-label="通知"
            @click="closeNotificationPopover"
          >
            <i class="bi bi-bell" aria-hidden="true"></i>
            <span v-if="unreadCount > 0" class="notification-badge">
              <span
                class="notification-badge__value"
                :class="{
                  'notification-badge__value--compact': unreadCount > 9,
                  'notification-badge__value--dense': unreadCount > 99,
                }"
                >{{ unreadCount > 99 ? '99+' : unreadCount }}</span
              >
            </span>
            <span class="header-action__label">通知</span>
          </RouterLink>

          <div v-show="notificationPopoverOpen" class="notification-popover" aria-label="最新通知">
            <RouterLink
              v-for="item in latestNotifications"
              :key="item.recordId"
              class="notification-preview"
              :to="{
                name: 'MemberMessages',
                query: { recordId: item.recordId, category: item.memberInbox },
              }"
              @click="closeNotificationPopover"
            >
              <span
                class="notification-read-dot"
                :class="{ 'notification-read-dot--read': item.recordStatus === 'READ' }"
                :aria-label="item.recordStatus === 'READ' ? '已讀' : '未讀'"
              ></span>
              <span class="notification-preview__copy">
                <!-- //msg-title// 後端 sendTitle -->
                <strong>{{ item.sendTitle }}</strong>
                <!-- //msg-content// 後端 sendContent -->
                <small>{{ item.sendContent }}</small>
              </span>
            </RouterLink>
            <RouterLink
              v-if="hasMoreNotifications"
              class="notification-more"
              :to="{ name: 'MemberMessages' }"
              aria-label="查看更多通知"
              @click="closeNotificationPopover"
              >…</RouterLink
            >
          </div>
        </div>
        <RouterLink class="header-action header-action--badge" to="/cart" aria-label="購物車"
          ><i class="bi bi-cart" aria-hidden="true"></i
          ><span class="notification-badge">{{ cartStore.totalQuantity }}</span
          ><span class="header-action__label">購物車</span></RouterLink
        >
        <RouterLink
          class="header-action member-action d-none d-lg-inline-flex"
          to="/member/overview"
        >
          <i class="bi bi-person-circle" aria-hidden="true"></i
          ><span class="header-action__label">會員中心</span>
        </RouterLink>
      </nav>
    </div>
    <div class="container app-header__search-mobile d-md-none"><SearchBar compact /></div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: var(--space-6);
  z-index: 1030;
  margin: 0;
  color: var(--color-text);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-soft);
}
.app-header__main,
.app-header__search-mobile {
  max-width: 1440px;
}
.app-header__main {
  min-height: 92px;
  gap: clamp(var(--space-3), 2vw, var(--space-6));
}
.app-header__search-mobile {
  padding-bottom: var(--space-4);
}
.brand-mark {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-3);
  font-family: var(--font-heading);
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}
.header-search {
  min-width: 180px;
}
.brand-mark__logo {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  overflow: hidden;
  border-radius: var(--radius-pill);
}
.brand-mark__logo-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: inherit;
}
.brand-mark__copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.brand-mark__name {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-lg);
  line-height: 1.2;
}
.brand-mark__tagline {
  color: var(--color-text-subtle);
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: 400;
}
.header-action,
.member-action {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
}
.header-action {
  display: inline-flex;
  width: 64px;
  min-height: 64px;
  flex: 0 0 64px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  font-size: 21px;
  border-radius: var(--radius-md);
}
.header-action__label {
  font-size: 11px;
  line-height: 1.2;
}
.header-action:hover,
.header-action:focus-visible,
.member-action:hover,
.member-action:focus-visible {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}
.header-action--badge .notification-badge {
  position: absolute;
  top: 10px;
  right: 16px;
  display: inline-flex;
  width: 16px;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: var(--color-surface);
  font-size: 9px;
  line-height: 1;
  text-align: center;
  border-radius: var(--radius-pill);
  background: var(--color-danger);
}
.notification-region {
  position: relative;
  display: inline-flex;
}
.notification-popover {
  position: absolute;
  z-index: 1040;
  top: 100%;
  right: 0;
  display: block;
  width: calc(var(--space-8) * 6);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
.notification-preview {
  display: grid;
  min-height: calc((var(--space-8) + var(--space-2)) * 0.75);
  grid-template-columns: var(--space-3) minmax(0, 1fr);
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border);
}
.notification-preview:hover {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}
.notification-preview:focus-visible,
.notification-more:focus-visible {
  outline: none;
  box-shadow: inset var(--shadow-focus);
}
.notification-read-dot {
  width: var(--space-2);
  height: var(--space-2);
  background: var(--color-primary);
  border-radius: var(--radius-pill);
}
.notification-read-dot--read {
  background: var(--color-disabled);
}
.notification-preview__copy {
  display: grid;
  min-width: 0;
  gap: var(--space-1);
}
.notification-preview__copy strong,
.notification-preview__copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notification-preview__copy strong {
  font-size: var(--font-size-sm);
}
.notification-preview__copy small {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.notification-more {
  display: grid;
  min-height: var(--space-5);
  padding: var(--space-1);
  place-items: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-lg);
  line-height: 1;
  text-decoration: none;
  background: var(--color-surface-soft);
}
.notification-more:hover {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}
.notification-badge__value {
  display: inline-block;
  line-height: 1;
  transform-origin: center;
}
.notification-badge__value--compact {
  font-size: 8px;
}
.notification-badge__value--dense {
  font-size: 7px;
  letter-spacing: -0.5px;
}
.member-action {
  padding: 0;
  border-radius: var(--radius-md);
}

@media (max-width: 575.98px) {
  .brand-mark__logo {
    width: 40px;
    height: 40px;
  }
  .brand-mark__copy {
    display: none;
  }
  .header-action {
    min-width: 40px;
    min-height: 48px;
  }
  .header-action__label {
    display: none;
  }
}

@media (max-width: 767.98px) {
  .app-header__main {
    min-height: 72px;
  }
}

@media (min-width: 768px) and (max-width: 991.98px) {
  .brand-mark__logo {
    width: 44px;
    height: 44px;
  }
  .brand-mark__name {
    font-size: var(--font-size-md);
  }
  .brand-mark__tagline {
    font-size: 11px;
  }
  .header-action {
    width: 52px;
    flex-basis: 52px;
  }
}
</style>
