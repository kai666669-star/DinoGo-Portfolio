<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getSellerOrders } from '@/api/sellerOrderApi'

const orders = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeStatus = ref('ALL')
const keyword = ref('')
const fetchingOrders = ref(false)
const AUTO_REFRESH_INTERVAL_MS = 10_000
let autoRefreshTimer = null
let latestLoadRequestId = 0

const statusTabs = [
  { label: '全部', value: 'ALL', statuses: [] },
  { label: '待付款', value: 'PENDING_PAYMENT', statuses: ['PENDING_PAYMENT'] },
  { label: '待出貨', value: 'PENDING_SHIPMENT', statuses: ['PAID', 'PROCESSING'] },
  { label: '待收貨', value: 'PENDING_RECEIPT', statuses: ['SHIPPED'] },
  { label: '已完成', value: 'COMPLETED', statuses: ['COMPLETED'] },
  { label: '不成立', value: 'CANCELLED', statuses: ['CANCELLED'] },
]

const filteredOrders = computed(() => {
  const selectedTab = statusTabs.find((tab) => tab.value === activeStatus.value)
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return orders.value
    .filter((order) => !selectedTab || selectedTab.value === 'ALL' || selectedTab.statuses.includes(order.status))
    .filter((order) => !normalizedKeyword || [order.orderNo, order.buyerId, ...(order.items ?? []).map((item) => item.productName)]
      .filter(Boolean).join(' ').toLowerCase().includes(normalizedKeyword))
})

const statusLabels = {
  PENDING_PAYMENT: '待付款', PAID: '待出貨', PROCESSING: '待出貨',
  SHIPPED: '待收貨', COMPLETED: '已完成', CANCELLED: '不成立',
}

async function loadOrders({ silent = false } = {}) {
  if (fetchingOrders.value) return

  const requestId = ++latestLoadRequestId
  fetchingOrders.value = true
  if (!silent) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const response = await getSellerOrders()
    if (requestId === latestLoadRequestId) {
      orders.value = Array.isArray(response.data) ? response.data : []
    }
  } catch (error) {
    if (!silent && requestId === latestLoadRequestId) {
      errorMessage.value = error.response?.data?.message ?? '無法載入賣家訂單。'
    }
  } finally {
    if (requestId === latestLoadRequestId) {
      if (!silent) loading.value = false
      fetchingOrders.value = false
    }
  }
}

function refreshOrdersSilently() {
  if (document.hidden) return
  void loadOrders({ silent: true })
}

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
}).format(Number(amount ?? 0))
}

const statusClass = (status) => {
  if (status === 'COMPLETED') {
    return 'is-done'
  }
  if (status === 'PENDING_PAYMENT' || status === 'PAID' || status === 'PROCESSING') {
    return 'is-warning'
  }
  if (status === 'CANCELLED') return 'is-cancelled'
  return 'is-processing'
}

const formatDate = (value) => value ? new Intl.DateTimeFormat('zh-TW', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value)) : '—'

onMounted(() => {
  void loadOrders()
  window.addEventListener('focus', refreshOrdersSilently)
  document.addEventListener('visibilitychange', refreshOrdersSilently)
  autoRefreshTimer = window.setInterval(refreshOrdersSilently, AUTO_REFRESH_INTERVAL_MS)
})

onUnmounted(() => {
  window.removeEventListener('focus', refreshOrdersSilently)
  document.removeEventListener('visibilitychange', refreshOrdersSilently)
  if (autoRefreshTimer !== null) window.clearInterval(autoRefreshTimer)
})
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>賣家訂單列表</h1>
        <p class="page-description">集中檢視每筆訂單狀態，點選查看進入出貨與明細處理。</p>
      </div>
    </header>

    <section class="list-panel">
      <div class="list-toolbar">
        <input v-model="keyword" class="search-input" type="search" placeholder="搜尋訂單編號、買家編號或商品" />

        <div class="status-tabs" aria-label="訂單狀態篩選">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            :class="{ active: activeStatus === tab.value }"
            @click="activeStatus = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="state-card" aria-live="polite">正在載入訂單…</div>
      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadOrders">重新載入</button>
      </div>
      <div v-else-if="filteredOrders.length === 0" class="state-card">找不到符合條件的訂單。</div>
      <div v-else class="order-table">
        <div class="table-header">
          <span>訂單編號</span>
          <span>買家</span>
          <span>訂單金額</span>
          <span>狀態</span>
          <span>建立時間</span>
        </div>

        <article v-for="order in filteredOrders" :key="order.orderId" class="order-row">
          <RouterLink class="order-no" :to="{ name: 'SellerOrderDetail', params: { id: order.orderId } }">
            {{ order.orderNo }}
          </RouterLink>
          <span>會員 #{{ order.buyerId }}</span>
          <strong>{{ formatCurrency(order.totalAmount) }}</strong>
          <span class="status-badge" :class="statusClass(order.status)">
            {{ statusLabels[order.status] ?? order.status }}
          </span>
          <span>{{ formatDate(order.createdAt) }}</span>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow,
.page-description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.list-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.status-tabs button {
  min-height: 36px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.status-tabs button.active {
  border-color: var(--color-primary-800);
  background: var(--color-primary-800);
  color: var(--color-surface);
}

.search-input {
  flex: 1;
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

.order-table {
  display: grid;
}

.table-header,
.order-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.3fr) 0.9fr 0.9fr 0.8fr 1fr;
  align-items: center;
  gap: var(--space-4);
}

.table-header {
  min-height: 42px;
  padding: 0 var(--space-5);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.order-row {
  min-height: 64px;
  padding: 0 var(--space-5);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.order-no {
  color: var(--color-text);
  font-weight: 800;
  text-decoration: none;
}

.order-no:hover {
  color: var(--color-primary);
}

.status-badge {
  width: fit-content;
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.status-badge.is-paid,
.status-badge.is-done {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.status-badge.is-processing {
  background: var(--color-primary-soft);
  color: var(--color-primary-700);
}

.status-badge.is-cancelled,
.state-error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.state-card {
  display: flex;
  min-height: 160px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-5);
  color: var(--color-text-muted);
}


@media (max-width: 1100px) {
  .table-header {
    display: none;
  }

  .order-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-3);
    padding: var(--space-4) var(--space-5);
  }

  .order-no {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
