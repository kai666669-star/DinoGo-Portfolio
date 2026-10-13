<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'
import { getMemberProfile } from '@/api/member'
import { getMemberOrders } from '@/api/order'
import MemberOrderProgress from '@/components/member/MemberOrderProgress.vue'
import MemberQuickActions from '@/components/member/MemberQuickActions.vue'
import MemberSummaryCards from '@/components/member/MemberSummaryCards.vue'

const overviewDate = ref('—')
const updatedTime = ref('—')
const latestOrder = ref(null)
const orderState = ref('loading')
const summaries = ref([
  { value: '—', label: '配送中的訂單', hint: '訂單資料載入中', icon: 'bi-box-seam' },
  { value: '—', label: '可使用優惠券', hint: '優惠券資料載入中', icon: 'bi-ticket-perforated' },
  { value: '-', label: '未讀平台訊息', icon: 'bi-envelope' },
])

function toDate(value) {
  const date = value ? new Date(value) : null
  return date && !Number.isNaN(date.getTime()) ? date : null
}

function formatDate(value) {
  const date = toDate(value)
  if (!date) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
    .format(date)
    .replaceAll('/', ' / ')
}

function formatTime(value) {
  const date = toDate(value)
  if (!date) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function formatJourneyTime(value, fallback) {
  const date = toDate(value)
  if (!date) return fallback
  return new Intl.DateTimeFormat('zh-TW', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(date)
    .replace(',', '')
}

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
}

function isShipped(order) {
  return order.status === 'SHIPPED'
}

function buildProgress(order) {
  const isDelivered = order.status === 'COMPLETED'
  const hasShipment = isShipped(order)
  const shipment = order.shipment ?? {}

  return [
    {
      label: '訂單成立',
      time: formatJourneyTime(order.createdAt, '訂單已建立'),
      icon: 'bi-check-lg',
      complete: true,
    },
    {
      label: '商家出貨',
      time: formatJourneyTime(shipment.shippedAt, hasShipment ? '已出貨' : '等待出貨'),
      icon: 'bi-box-seam',
      complete: hasShipment,
    },
    {
      label: '配送中',
      time: formatJourneyTime(
        shipment.availablePickupAt,
        hasShipment ? '配送資訊待更新' : '等待配送',
      ),
      icon: 'bi-truck',
      complete: hasShipment,
    },
    {
      label: '完成取貨',
      time: formatJourneyTime(shipment.deliveredAt, '等待完成'),
      icon: 'bi-house',
      complete: isDelivered,
    },
  ]
}

function buildOrderOverview(order) {
  const item = order.items?.[0] ?? {}
  const shipment = order.shipment ?? {}
  const status = order.status === 'COMPLETED' ? '已完成' : isShipped(order) ? '配送中' : '處理中'

  return {
    id: order.orderId,
    number: order.orderNo,
    date: formatDate(order.createdAt),
    status,
    productName: [item.productName, item.skuSpec].filter(Boolean).join(' · ') || '商品資訊待提供',
    sellerName: `商家 #${order.sellerId ?? '—'}`,
    quantity: item.quantity ?? 0,
    amount: formatCurrency(order.totalAmount),
    progress: buildProgress(order),
    deliveryHint: shipment.availablePickupAt
      ? `預計 ${formatDate(shipment.availablePickupAt)} 送達`
      : '配送資訊待更新',
  }
}

function buildCouponHint(coupons) {
  const sevenDaysLater = Date.now() + 7 * 24 * 60 * 60 * 1000
  const expiringCount = coupons.filter((coupon) => {
    const endAt = toDate(coupon.endAt)
    return endAt && endAt.getTime() <= sevenDaysLater
  }).length
  return expiringCount ? `${expiringCount} 張將於 7 天內到期` : '暫無即將到期優惠券'
}

async function loadOverview() {
  const [ordersResult, profileResult, couponsResult] = await Promise.allSettled([
    // D：會員訂單、物流狀態、商品、數量與金額。
    getMemberOrders(),
    getMemberProfile(),
    // E：優惠券模組 API，MemberCouponsView 亦使用同一個 endpoint。
    api.get('/member/coupons'),
  ])

  const orders =
    ordersResult.status === 'fulfilled' && Array.isArray(ordersResult.value.data)
      ? ordersResult.value.data
      : []
  const profile = profileResult.status === 'fulfilled' ? profileResult.value.data : null
  const coupons =
    couponsResult.status === 'fulfilled' && Array.isArray(couponsResult.value.data)
      ? couponsResult.value.data
      : []
  const activeOrder = orders.find(isShipped) ?? orders[0] ?? null

  latestOrder.value = activeOrder ? buildOrderOverview(activeOrder) : null
  orderState.value =
    ordersResult.status !== 'fulfilled' ? 'error' : latestOrder.value ? 'ready' : 'empty'

  const overviewTimestamp = activeOrder?.createdAt ?? profile?.updatedAt
  overviewDate.value = formatDate(overviewTimestamp)
  updatedTime.value = formatTime(profile?.updatedAt)

  const deliveryOrders = orders.filter(isShipped)
  const availableCoupons = coupons.filter((coupon) => coupon.status === 'AVAILABLE')
  summaries.value = [
    ordersResult.status === 'fulfilled'
      ? {
          value: String(deliveryOrders.length),
          label: '配送中的訂單',
          hint: latestOrder.value?.deliveryHint ?? '目前沒有配送中的訂單',
          icon: 'bi-box-seam',
        }
      : {
          value: '—',
          label: '配送中的訂單',
          hint: '訂單資料暫時無法載入',
          icon: 'bi-box-seam',
        },
    couponsResult.status === 'fulfilled'
      ? {
          value: String(availableCoupons.length),
          label: '可使用優惠券',
          hint: buildCouponHint(availableCoupons),
          icon: 'bi-ticket-perforated',
        }
      : {
          value: '—',
          label: '可使用優惠券',
          hint: '優惠券資料暫時無法載入',
          icon: 'bi-ticket-perforated',
        },
    { value: '-', label: '未讀平台訊息', icon: 'bi-envelope' },
  ]
}

onMounted(loadOverview)
</script>

<template>
  <section class="member-overview" aria-labelledby="member-overview-title">
    <div class="container member-overview__container">
      <header class="member-overview__header">
        <div>
          <p>MEMBER OVERVIEW · {{ overviewDate }}</p>
          <h1 id="member-overview-title">今天的會員摘要</h1>
        </div>
        <div class="member-overview__profile-action">
          <span>資料更新於 {{ updatedTime }}</span>
          <RouterLink :to="{ name: 'MemberProfile' }">編輯個人資料</RouterLink>
        </div>
      </header>

      <MemberSummaryCards :summaries="summaries" />

      <div class="member-overview__content">
        <MemberOrderProgress :order="latestOrder" :state="orderState" />
        <MemberQuickActions />
      </div>
    </div>
  </section>
</template>

<style scoped>
.member-overview {
  min-height: 720px;
  background: var(--color-bg);
}
.member-overview__container {
  max-width: 1232px;
  padding-top: 28px;
  padding-bottom: var(--space-8);
}
.member-overview__header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}
.member-overview__header p,
.member-overview__header h1 {
  margin: 0;
}
.member-overview__header p {
  color: var(--color-primary-active);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.member-overview__header h1 {
  margin-top: 5px;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.member-overview__profile-action {
  display: flex;
  align-items: center;
  gap: 10px;
}
.member-overview__profile-action span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.member-overview__profile-action a {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  padding: 0 var(--space-3);
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.member-overview__profile-action a:hover {
  background: var(--color-primary-soft);
}
.member-overview__profile-action a:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
.member-overview__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: var(--space-5);
  margin-top: var(--space-5);
}
@media (max-width: 991.98px) {
  .member-overview__content {
    grid-template-columns: 1fr;
  }
  .member-overview__container {
    padding-inline: var(--space-5);
  }
}
@media (max-width: 767.98px) {
  .member-overview__container {
    padding: var(--space-5) var(--space-4) var(--space-7);
  }
  .member-overview__header {
    align-items: flex-start;
    flex-direction: column;
  }
  .member-overview__profile-action {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
