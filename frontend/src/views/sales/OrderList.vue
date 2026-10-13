<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberOrders } from '@/api/order'
import { getOrderDisplayStatus, isOrderInDisplayGroup } from '@/utils/orderDisplayStatus'
import { getImageUrl } from '@/utils/imageUrl'

const orders = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeStatus = ref('ALL')
const keyword = ref('')
const sortOrder = ref('NEWEST')

const filters = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PENDING_SHIPMENT', label: '待出貨' },
  { value: 'PENDING_RECEIPT', label: '待收貨' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '不成立' },
]

const visibleOrders = computed(() => {
  const selectedFilter = filters.find((filter) => filter.value === activeStatus.value)
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return orders.value
    .filter(
      (order) =>
        !selectedFilter ||
        selectedFilter.value === 'ALL' ||
        isOrderInDisplayGroup(order, selectedFilter.value),
    )
    .filter((order) => {
      if (!normalizedKeyword) return true
      return [
        order.orderNo,
        order.sellerName,
        ...(order.items ?? []).flatMap((item) => [item.productName, item.skuSpec]),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(normalizedKeyword)
    })
    .toSorted((left, right) => {
      const difference = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
      return sortOrder.value === 'NEWEST' ? difference : -difference
    })
})

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getMemberOrders()
    orders.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '讀取訂單時發生問題，請稍後再試。'
  } finally {
    loading.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function itemQuantity(item) {
  return Number(item?.quantity ?? 1)
}

onMounted(loadOrders)
</script>

<template>
  <section class="order-page">
    <div class="container order-container">
      <header class="page-header">
        <h1>我的訂單</h1>
        <p>查看每筆訂單的商品、配送與付款進度。</p>
      </header>

      <nav class="order-tabs" aria-label="訂單狀態篩選">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: activeStatus === filter.value }"
          :aria-pressed="activeStatus === filter.value"
          @click="activeStatus = filter.value"
        >
          {{ filter.label }}
        </button>
      </nav>

      <div class="filter-row">
        <label class="search-control">
          <i class="bi bi-search" aria-hidden="true"></i
          ><span class="visually-hidden">搜尋訂單</span>
          <input v-model="keyword" type="search" placeholder="搜尋訂單編號、商品或規格" />
        </label>
        <label class="sort-control">
          <span class="visually-hidden">訂單排序</span>
          <select v-model="sortOrder">
            <option value="NEWEST">最新訂單</option>
            <option value="OLDEST">最早訂單</option>
          </select>
          <i class="bi bi-chevron-down" aria-hidden="true"></i>
        </label>
      </div>

      <div v-if="loading" class="state-card" aria-live="polite">
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span
        ><span>正在讀取訂單…</span>
      </div>
      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i><strong>無法讀取訂單</strong
        ><span>{{ errorMessage }}</span
        ><button type="button" @click="loadOrders">重新讀取</button>
      </div>
      <div v-else-if="visibleOrders.length === 0" class="state-card">
        <i class="bi bi-receipt" aria-hidden="true"></i><strong>目前沒有符合條件的訂單</strong
        ><span>試著切換訂單狀態，或調整搜尋關鍵字。</span>
      </div>

      <div v-else class="order-list">
        <article
          v-for="order in visibleOrders"
          :key="order.orderId"
          class="order-card"
          :data-order-id="order.orderId"
        >
          <header class="order-card__header">
            <div class="order-card__meta">
              <span class="order-card__number">訂單編號：{{ order.orderNo }}</span
              ><span>{{ formatDate(order.createdAt) }}</span>
            </div>
            <span
              class="status-label status-badge"
              :class="`status-${getOrderDisplayStatus(order).key.toLowerCase()}`"
              >{{ getOrderDisplayStatus(order).label }}</span
            >
          </header>

          <div class="order-card__items">
            <div v-for="item in order.items ?? []" :key="item.orderItemId" class="order-item">
              <div class="product-image">
                <img
                  v-if="item.productImageUrl"
                  :src="getImageUrl(item.productImageUrl)"
                  :alt="item.productName"
                /><i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>
              <div class="order-item__copy">
                <strong>{{ item.productName }}</strong
                ><span v-if="item.skuSpec">規格：{{ item.skuSpec }}</span
                ><span>數量：{{ itemQuantity(item) }}</span>
              </div>
              <strong class="order-item__price">{{
                formatCurrency(item.subtotal ?? item.unitPrice)
              }}</strong>
            </div>
          </div>

          <footer class="order-card__footer">
            <span>共 {{ order.items?.length ?? 0 }} 件商品</span>
            <div class="order-card__actions">
              <strong
                >訂單金額 <b>{{ formatCurrency(order.totalAmount) }}</b></strong
              ><RouterLink
                class="btn btn-sm order-detail-button"
                :to="{ name: 'MemberOrderDetail', params: { id: order.orderId } }"
                :aria-label="`查看訂單 ${order.orderNo}`"
                >查看訂單</RouterLink
              >
            </div>
          </footer>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.order-page {
  min-height: 620px;
  padding: 40px 0;
  background: var(--color-bg);
}
.order-container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
}
.page-header {
  display: flex;
  min-height: 68px;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-1);
  margin-bottom: var(--space-5);
}
.page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}
.order-tabs {
  display: flex;
  min-height: 52px;
  align-items: stretch;
  justify-content: space-between;
  gap: var(--space-4);
  overflow-x: auto;
  overflow-y: hidden;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  scrollbar-width: thin;
}
.order-tabs button {
  position: relative;
  flex: 1 0 max-content;
  padding: 0 var(--space-3);
  color: var(--color-text-muted);
  font: inherit;
  font-size: var(--font-size-base);
  font-weight: 500;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.order-tabs button::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: transparent;
}
.order-tabs button:hover,
.order-tabs button:focus-visible,
.order-tabs button.active {
  color: var(--color-danger);
}
.order-tabs button.active {
  font-weight: 700;
}
.order-tabs button.active::after {
  background: var(--color-danger);
}
.order-tabs button:focus-visible,
.search-control:focus-within,
.sort-control:focus-within,
.order-detail-button:focus-visible,
.state-card button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
.filter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
.search-control,
.sort-control {
  display: flex;
  height: 42px;
  align-items: center;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.search-control {
  gap: var(--space-2);
  padding: 0 var(--space-3);
}
.search-control input,
.sort-control select {
  width: 100%;
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-base);
  background: transparent;
  border: 0;
  outline: 0;
}
.search-control input::placeholder {
  color: var(--color-text-muted);
}
.sort-control {
  position: relative;
  padding: 0 var(--space-3);
}
.sort-control select {
  height: 100%;
  padding-right: var(--space-5);
  appearance: none;
  cursor: pointer;
}
.sort-control i {
  position: absolute;
  right: var(--space-3);
  pointer-events: none;
}
.order-list {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-4);
}
.order-card {
  overflow: hidden;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
}
.order-card__header,
.order-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
}
.order-card__header {
  border-bottom: 1px solid var(--color-border);
}
.order-card__meta {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.order-card__number {
  color: var(--color-text);
  font-weight: 600;
}
.status-label {
  flex: 0 0 auto;
  color: var(--color-info);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.status-pending_payment,
.status-pending_shipment,
.status-pending_pickup,
.status-in_transit {
  color: var(--color-warning);
}
.status-completed {
  color: var(--color-success);
}
.status-cancelled {
  color: var(--color-danger);
}
.order-card__items {
  padding: 0 var(--space-4);
}
.order-item {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr) minmax(110px, auto);
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) 0;
}
.order-item + .order-item {
  border-top: 1px solid var(--color-border);
}
.product-image {
  display: grid;
  width: 82px;
  height: 82px;
  overflow: hidden;
  place-items: center;
  color: var(--color-text-subtle);
  background: var(--color-bg-muted);
  border-radius: var(--radius-sm);
}
.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.order-item__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: var(--space-1);
}
.order-item__copy strong {
  overflow: hidden;
  font-size: var(--font-size-base);
  font-weight: 600;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-item__copy span {
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-item__price {
  font-size: var(--font-size-base);
  text-align: right;
}
.order-card__footer {
  min-height: 64px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  background: var(--color-surface-soft);
  border-top: 1px solid var(--color-border);
}
.order-card__actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.order-card__actions strong {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 500;
  white-space: nowrap;
}
.order-card__actions b {
  margin-left: var(--space-1);
  color: var(--color-danger);
  font-size: var(--font-size-md);
}
.order-detail-button {
  padding: var(--space-2) var(--space-4);
  color: var(--color-surface);
  font-weight: 600;
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.order-detail-button:hover {
  color: var(--color-surface);
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
.state-card {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding: var(--space-6);
  color: var(--color-text-muted);
  text-align: center;
  font-size: var(--font-size-base);
  line-height: var(--line-height-base);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.state-card > i {
  color: var(--color-primary);
  font-size: var(--font-size-xl);
}
.state-card strong {
  color: var(--color-text);
  font-size: var(--font-size-md);
  font-weight: 700;
}
.state-card button {
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-4);
  color: var(--color-surface);
  font: inherit;
  font-weight: 600;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.state-error > i {
  color: var(--color-danger);
}
@media (max-width: 767.98px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
  .order-card__header,
  .order-card__footer {
    padding-right: var(--space-3);
    padding-left: var(--space-3);
  }
  .order-card__items {
    padding: 0 var(--space-3);
  }
  .order-item {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: var(--space-3);
  }
  .product-image {
    width: 64px;
    height: 64px;
  }
  .order-item__price {
    grid-column: 2;
    text-align: left;
  }
  .order-card__footer {
    align-items: flex-end;
    flex-direction: column;
    gap: var(--space-2);
  }
}
@media (max-width: 575.98px) {
  .order-page {
    padding: var(--space-6) 0;
  }
  .order-container {
    --bs-gutter-x: var(--space-4);
  }
  .order-tabs {
    margin-right: calc(var(--space-4) * -1);
    margin-left: calc(var(--space-4) * -1);
    padding: 0 var(--space-4);
  }
  .order-tabs button {
    flex-basis: auto;
    padding: 0 var(--space-3);
    font-size: var(--font-size-sm);
  }
  .order-card__header {
    align-items: flex-start;
  }
  .order-card__meta {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }
  .order-card__actions {
    width: 100%;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--space-2);
  }
}
</style>
