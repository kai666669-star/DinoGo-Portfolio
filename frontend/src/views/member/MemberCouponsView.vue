<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/api/axios'

const coupons = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const activeTab = ref('available')

const couponSource = computed(() => coupons.value)

const normalizedCoupons = computed(() =>
  couponSource.value.map((coupon) => ({
    ...coupon,
    normalizedStatus: normalizeStatus(coupon.status),
  })),
)

const tabs = computed(() => [
  {
    label: '可使用',
    value: 'available',
    count: normalizedCoupons.value.filter((coupon) => coupon.normalizedStatus === 'available')
      .length,
  },
  {
    label: '已使用',
    value: 'used',
    count: normalizedCoupons.value.filter((coupon) => coupon.normalizedStatus === 'used').length,
  },
  {
    label: '已過期',
    value: 'expired',
    count: normalizedCoupons.value.filter((coupon) => coupon.normalizedStatus === 'expired').length,
  },
  {
    label: '不可使用',
    value: 'unavailable',
    count: normalizedCoupons.value.filter((coupon) => coupon.normalizedStatus === 'unavailable')
      .length,
  },
])

const visibleCoupons = computed(() =>
  normalizedCoupons.value.filter((coupon) => coupon.normalizedStatus === activeTab.value),
)

function couponAmount(coupon) {
  if (coupon.discountType === 'PERCENT' || coupon.discountType === 'PERCENTAGE') {
    return `${formatPercentDiscount(coupon.discountValue)}折`
  }

  return `NT$${Number(coupon.discountAmount ?? coupon.discountValue ?? 0).toLocaleString()}`
}

function formatPercentDiscount(discountValue) {
  const value = Number(discountValue ?? 0)
  const displayValue = Number.isFinite(value) ? value / 10 : 0

  return new Intl.NumberFormat('zh-TW', {
    maximumFractionDigits: 2,
  }).format(displayValue)
}

function couponDescription(coupon) {
  const description =
    coupon.description ||
    (Number(coupon.minPurchaseAmount ?? 0) > 0
      ? `滿 NT$${Number(coupon.minPurchaseAmount).toLocaleString()} 可使用`
      : '指定商品適用')

  return `${description} · ${formatExpireDate(coupon)} 到期`
}

function normalizeStatus(status) {
  if (['USED', 'REDEEMED'].includes(status)) {
    return 'used'
  }

  if (status === 'AVAILABLE') {
    return 'available'
  }

  if (status === 'EXPIRED') {
    return 'expired'
  }

  return 'unavailable'
}

function statusText(status) {
  if (status === 'used') return '已使用'
  if (status === 'expired') return '已過期'
  if (status === 'unavailable') return '不可使用'
  return '可使用'
}

function formatExpireDate(coupon) {
  const endAt = coupon.endAt || coupon.expireDate || coupon.endTime
  if (!endAt) return '未設定'

  return new Date(endAt).toLocaleDateString('zh-TW', {
    month: '2-digit',
    day: '2-digit',
  })
}

async function loadCoupons() {
  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await api.get('/member/coupons')
    coupons.value = response.data || []
  } catch (error) {
    errorMessage.value = '無法取得我的優惠券，請稍後再試。'
    coupons.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(loadCoupons)
</script>

<template>
  <main class="member-coupons-shell">
    <div class="container member-coupons-shell__container">
      <section class="member-coupons-page">
        <header class="page-header">
          <div>
            <h1>我的優惠券</h1>
            <p>查看可用、已使用與已過期的優惠券</p>
          </div>
        </header>

        <nav class="coupon-tabs" aria-label="優惠券分類">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
            <span v-if="tab.count">{{ tab.count }}</span>
          </button>
        </nav>

        <p v-if="errorMessage" class="notice-message">{{ errorMessage }}</p>
        <p v-if="isLoading" class="state-message">優惠券資料載入中...</p>

        <section v-else-if="visibleCoupons.length" class="coupon-list" aria-label="優惠券列表">
          <article
            v-for="coupon in visibleCoupons"
            :key="coupon.couponId || coupon.couponCode"
            class="coupon-card"
          >
            <div class="coupon-value">
              <strong>{{ couponAmount(coupon) }}</strong>
              <span>{{ statusText(coupon.normalizedStatus) }}</span>
            </div>

            <div class="coupon-copy">
              <h2>{{ coupon.couponName || 'DINO-GO 平台優惠券' }}</h2>
              <span class="coupon-seller">{{
                coupon.sellerName || `賣家 #${coupon.sellerId}`
              }}</span>
              <p>{{ couponDescription(coupon) }}</p>
            </div>

            <span class="coupon-badge">{{ statusText(coupon.normalizedStatus) }}</span>
          </article>
        </section>

        <section v-else class="empty-state">
          <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
          <strong>此分類目前沒有優惠券</strong>
        </section>
      </section>
    </div>
  </main>
</template>

<style scoped>
.member-coupons-shell {
  background: var(--color-bg);
}

.member-coupons-shell__container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
  padding-block: 40px;
}

.member-coupons-page {
  display: grid;
  gap: var(--space-5);
  width: 100%;
  margin: 0 auto;
  padding: 0;
}

.coupon-seller {
  display: block;
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 700;
}

.page-header {
  display: flex;
  min-height: 68px;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-1);
}

.page-header h1,
.page-header p,
.coupon-copy h2,
.coupon-copy p,
.notice-message,
.state-message {
  margin: 0;
}

.page-header h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.page-header p {
  margin-top: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.coupon-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.coupon-tabs button {
  min-height: 38px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.coupon-tabs button.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.coupon-tabs span {
  margin-left: 4px;
}

.notice-message,
.state-message {
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

@media (max-width: 991.98px) {
  .coupon-list {
    grid-template-columns: 1fr;
  }
}

.coupon-card {
  min-height: 138px;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.coupon-value {
  position: relative;
  display: grid;
  place-content: center;
  gap: var(--space-2);
  border-right: 1px dashed var(--color-border-strong);
  background: var(--color-bg-muted);
  text-align: center;
}

.coupon-value::before,
.coupon-value::after {
  position: absolute;
  right: -9px;
  width: 18px;
  height: 18px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg);
  content: '';
}

.coupon-value::before {
  top: -9px;
}

.coupon-value::after {
  bottom: -9px;
}

.coupon-value strong {
  color: var(--color-primary-active);
  font-family: var(--font-heading);
  font-size: 28px;
  line-height: 1;
}

.coupon-value span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.coupon-card {
  position: relative;
}

.coupon-copy {
  display: grid;
  align-content: center;
  gap: var(--space-2);
  min-width: 0;
  padding: var(--space-5) 120px var(--space-5) var(--space-5);
}

.coupon-copy h2 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-md);
}

.coupon-copy p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.coupon-badge {
  position: absolute;
  right: var(--space-4);
  top: var(--space-4);
  border-radius: 999px;
  padding: 4px 10px;
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.empty-state {
  min-height: 260px;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: var(--space-3);
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-text-muted);
  text-align: center;
}

.empty-state i {
  color: var(--color-primary);
  font-size: 32px;
}

.empty-state strong {
  color: var(--color-text-900);
  font-size: var(--font-size-md);
}

button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

@media (max-width: 640px) {
  .coupon-tabs button {
    flex: 1 1 100%;
  }

  .coupon-card {
    grid-template-columns: 1fr;
  }

  .coupon-copy {
    padding: var(--space-5);
  }

  .coupon-badge {
    position: static;
    width: fit-content;
    margin: 0 0 var(--space-5) var(--space-5);
  }

  .coupon-value {
    min-height: 112px;
    border-right: 0;
    border-bottom: 1px dashed var(--color-border-strong);
  }

  .coupon-value::before,
  .coupon-value::after {
    display: none;
  }
}

@media (max-width: 575.98px) {
  .member-coupons-shell__container {
    padding-block: var(--space-6);
  }
}
</style>
