<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { createSellerShipment, getSellerOrder, getSellerOrders } from '@/api/sellerOrderApi'

const orders = ref([])
const selectedOrderIds = ref([])
const loading = ref(true)
const errorMessage = ref('')
const activeDeadlineFilter = ref('ALL')
const sortBy = ref('DEADLINE')
const showShipmentModal = ref(false)
const submitting = ref(false)
const batchError = ref('')
const batchResult = ref([])
const shipmentForms = ref({})

const deadlineFilters = [
  { value: 'ALL', label: '全部' },
  { value: 'OVERDUE', label: '已逾建議期限' },
  { value: 'TODAY', label: '建議今天出貨' },
  { value: 'THREE_DAYS', label: '建議三天內出貨' },
]

const pendingOrders = computed(() => orders.value
  .filter((order) => ['PAID', 'PROCESSING'].includes(order.status) && !order.shipment)
  .map((order) => ({ ...order, suggestedShipBy: addDays(order.createdAt, 3) })))

const visibleOrders = computed(() => {
  const now = new Date()
  const endOfToday = new Date(now)
  endOfToday.setHours(23, 59, 59, 999)
  const endOfThreeDays = new Date(now)
  endOfThreeDays.setDate(endOfThreeDays.getDate() + 3)
  endOfThreeDays.setHours(23, 59, 59, 999)

  return pendingOrders.value
    .filter((order) => {
      const deadline = new Date(order.suggestedShipBy)
      if (activeDeadlineFilter.value === 'OVERDUE') return deadline < now
      if (activeDeadlineFilter.value === 'TODAY') return deadline >= now && deadline <= endOfToday
      if (activeDeadlineFilter.value === 'THREE_DAYS') return deadline >= now && deadline <= endOfThreeDays
      return true
    })
    .sort((a, b) => sortBy.value === 'DEADLINE'
      ? new Date(a.suggestedShipBy) - new Date(b.suggestedShipBy)
      : new Date(b.createdAt) - new Date(a.createdAt))
})

const selectedOrders = computed(() => pendingOrders.value.filter((order) => selectedOrderIds.value.includes(order.orderId)))
const allVisibleSelected = computed(() => visibleOrders.value.length > 0 && visibleOrders.value.every((order) => selectedOrderIds.value.includes(order.orderId)))

function addDays(value, days) {
  const date = new Date(value)
  date.setDate(date.getDate() + days)
  return date.toISOString()
}

function formatDate(value) {
  return value ? new Intl.DateTimeFormat('zh-TW', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
}

function formatDeadline(order) {
  const diff = new Date(order.suggestedShipBy).getTime() - Date.now()
  const days = Math.ceil(diff / 86_400_000)
  if (days < 0) return `已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '建議今天出貨'
  return `${days} 天內`
}

function deadlineClass(order) {
  const diff = new Date(order.suggestedShipBy).getTime() - Date.now()
  if (diff < 0) return 'is-overdue'
  if (diff <= 86_400_000) return 'is-today'
  return 'is-normal'
}

function productSummary(order) {
  const first = order.items?.[0]?.productName ?? '商品資料'
  const rest = Math.max((order.items?.length ?? 1) - 1, 0)
  return rest ? `${first} 等 ${rest + 1} 項商品` : first
}

function toggleSelectAll() {
  const visibleIds = visibleOrders.value.map((order) => order.orderId)
  if (allVisibleSelected.value) {
    selectedOrderIds.value = selectedOrderIds.value.filter((id) => !visibleIds.includes(id))
    return
  }
  selectedOrderIds.value = [...new Set([...selectedOrderIds.value, ...visibleIds])]
}

function openShipmentModal() {
  batchError.value = ''
  batchResult.value = []
  shipmentForms.value = Object.fromEntries(selectedOrders.value.map((order) => [
    order.orderId,
    { carrierName: '', trackingNo: '' },
  ]))
  showShipmentModal.value = true
}

function closeShipmentModal() {
  if (!submitting.value) showShipmentModal.value = false
}

async function submitBatchShipments() {
  const invalidOrder = selectedOrders.value.find((order) => {
    const form = shipmentForms.value[order.orderId]
    return !form?.carrierName?.trim() || !form?.trackingNo?.trim()
  })
  if (invalidOrder) {
    batchError.value = `請完整填寫訂單 ${invalidOrder.orderNo} 的物流商與物流單號。`
    return
  }

  submitting.value = true
  batchError.value = ''
  const results = []
  for (const order of selectedOrders.value) {
    const form = shipmentForms.value[order.orderId]
    try {
      await createSellerShipment(order.orderId, {
        carrierName: form.carrierName.trim(),
        trackingNo: form.trackingNo.trim(),
      })
      results.push({ orderId: order.orderId, orderNo: order.orderNo, success: true })
    } catch (error) {
      results.push({
        orderId: order.orderId,
        orderNo: order.orderNo,
        success: false,
        message: error.response?.data?.message ?? '建立失敗，請稍後再試。',
      })
    }
  }
  batchResult.value = results
  selectedOrderIds.value = selectedOrderIds.value.filter((id) => !results.some((result) => result.orderId === id && result.success))
  submitting.value = false
}

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getSellerOrders()
    const sellerOrders = Array.isArray(response.data) ? response.data : []
    const shipmentEligibleOrders = sellerOrders.filter((order) => ['PAID', 'PROCESSING'].includes(order.status))
    const detailedOrders = await Promise.all(shipmentEligibleOrders.map(async (order) => {
      try {
        const detailResponse = await getSellerOrder(order.orderId)
        return detailResponse.data
      } catch {
        return order
      }
    }))
    const detailById = new Map(detailedOrders.map((order) => [order.orderId, order]))
    orders.value = sellerOrders.map((order) => detailById.get(order.orderId) ?? order)
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '無法載入待出貨訂單。'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void loadOrders() })
</script>

<template>
  <section class="batch-shipment-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理／宅配</p>
        <h1>批次出貨</h1>
        <p>集中處理待出貨訂單，建立寄件資料後仍需由賣家確認交寄。</p>
      </div>
      <RouterLink class="secondary-link" :to="{ name: 'SellerOrders' }">返回訂單管理</RouterLink>
    </header>

    <div class="batch-layout">
      <section class="order-panel" aria-labelledby="pending-shipment-title">
        <div class="panel-heading">
          <div>
            <h2 id="pending-shipment-title">待出貨</h2>
            <p>建議期限以訂單成立後 3 天計算，實際出貨規則以平台政策為準。</p>
          </div>
          <select v-model="sortBy" aria-label="待出貨訂單排序">
            <option value="DEADLINE">建議期限優先</option>
            <option value="CREATED_AT">訂單成立時間優先</option>
          </select>
        </div>

        <div class="filter-row" aria-label="建議出貨期限篩選">
          <button v-for="filter in deadlineFilters" :key="filter.value" type="button" :class="{ active: activeDeadlineFilter === filter.value }" @click="activeDeadlineFilter = filter.value">
            {{ filter.label }} ({{ filter.value === 'ALL' ? pendingOrders.length : visibleOrders.length }})
          </button>
        </div>

        <div v-if="loading" class="state-card" aria-live="polite">正在載入待出貨訂單…</div>
        <div v-else-if="errorMessage" class="state-card state-error" role="alert"><span>{{ errorMessage }}</span><button type="button" @click="loadOrders">重新載入</button></div>
        <div v-else-if="visibleOrders.length === 0" class="state-card"><i class="bi bi-inbox" aria-hidden="true"></i><strong>目前沒有待出貨訂單</strong><span>新的待出貨宅配訂單會顯示在這裡。</span></div>
        <div v-else class="shipment-table">
          <div class="table-header">
            <label><input type="checkbox" :checked="allVisibleSelected" @change="toggleSelectAll"><span class="visually-hidden">全選目前清單</span></label>
            <span>商品</span><span>訂單編號</span><span>買家</span><span>建議期限</span><span>訂單狀態</span>
          </div>
          <article v-for="order in visibleOrders" :key="order.orderId" class="shipment-row">
            <label><input v-model="selectedOrderIds" type="checkbox" :value="order.orderId" :aria-label="`選取訂單 ${order.orderNo}`"></label>
            <span class="product-name">{{ productSummary(order) }}</span>
            <RouterLink :to="{ name: 'SellerOrderDetail', params: { id: order.orderId } }">{{ order.orderNo }}</RouterLink>
            <span>會員 #{{ order.buyerId }}</span>
            <span class="deadline" :class="deadlineClass(order)"><strong>{{ formatDeadline(order) }}</strong><small>{{ formatDate(order.suggestedShipBy) }}</small></span>
            <span class="status-badge">待出貨</span>
          </article>
        </div>
      </section>

      <aside class="batch-summary" aria-live="polite">
        <h2>批次建立寄件資料</h2>
        <p>已選取 <strong>{{ selectedOrders.length }}</strong> 筆訂單</p>
        <div class="notice"><i class="bi bi-info-circle" aria-hidden="true"></i><span>建立寄件資料只會儲存物流商與單號，並不代表商品已交寄。</span></div>
        <button type="button" class="primary-button" :disabled="selectedOrders.length === 0" @click="openShipmentModal">批次建立寄件資料</button>
      </aside>
    </div>

    <div v-if="showShipmentModal" class="modal-backdrop" @click.self="closeShipmentModal">
      <section class="shipment-modal" role="dialog" aria-modal="true" aria-labelledby="batch-modal-title">
        <header><div><p class="eyebrow">宅配寄件資料</p><h2 id="batch-modal-title">批次建立寄件資料</h2></div><button type="button" class="close-button" aria-label="關閉" :disabled="submitting" @click="closeShipmentModal"><i class="bi bi-x-lg" aria-hidden="true"></i></button></header>
        <p class="modal-description">請逐筆填入物流商與物流單號；送出後狀態為「已建立寄件資料」，尚未代表已交寄。</p>
        <div class="shipment-form-list">
          <article v-for="order in selectedOrders" :key="order.orderId" class="shipment-form-row">
            <strong>{{ order.orderNo }}</strong><span>{{ productSummary(order) }}</span>
            <label>物流商<input v-model="shipmentForms[order.orderId].carrierName" type="text" placeholder="例如：黑貓宅急便" :disabled="submitting"></label>
            <label>物流單號<input v-model="shipmentForms[order.orderId].trackingNo" type="text" placeholder="請輸入單號" :disabled="submitting"></label>
          </article>
        </div>
        <p v-if="batchError" class="form-error" role="alert">{{ batchError }}</p>
        <ul v-if="batchResult.length" class="batch-results"><li v-for="result in batchResult" :key="result.orderId" :class="{ failed: !result.success }">{{ result.orderNo }}：{{ result.success ? '已建立寄件資料' : result.message }}</li></ul>
        <footer><button type="button" class="secondary-button" :disabled="submitting" @click="closeShipmentModal">{{ batchResult.length ? '完成' : '取消' }}</button><button v-if="!batchResult.length" type="button" class="primary-button" :disabled="submitting" @click="submitBatchShipments">{{ submitting ? '建立中…' : '建立寄件資料' }}</button></footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
.batch-shipment-page { display: grid; gap: var(--space-5); }
.page-header, .panel-heading, .batch-summary header, .shipment-modal header, .shipment-modal footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.eyebrow, .page-header p, .panel-heading p, .modal-description, .batch-summary > p { margin: 0; color: var(--color-text-muted); font-size: var(--font-size-sm); }
.eyebrow { margin-bottom: var(--space-1) !important; }
h1, h2 { margin: 0; color: var(--color-text); }
h1 { font-family: var(--font-heading); font-size: var(--font-size-xl); }
h2 { font-size: var(--font-size-lg); }
.secondary-link, .secondary-button { min-height: 40px; padding: 0 var(--space-3); border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); background: var(--color-surface); color: var(--color-text-700); font: inherit; font-size: var(--font-size-sm); font-weight: 600; text-decoration: none; cursor: pointer; }
.secondary-link, .secondary-button { display: inline-flex; align-items: center; justify-content: center; }
.batch-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; align-items: start; gap: var(--space-5); }
.order-panel, .batch-summary { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--shadow-soft); }
.panel-heading { padding: var(--space-5); border-bottom: 1px solid var(--color-border); }
select, input[type='text'] { min-height: 40px; border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); padding: 0 var(--space-3); background: var(--color-surface); color: var(--color-text); font: inherit; }
.filter-row { display: flex; flex-wrap: wrap; gap: var(--space-2); padding: var(--space-4) var(--space-5); }
.filter-row button { min-height: 36px; padding: 0 var(--space-3); border: 1px solid var(--color-border-strong); border-radius: var(--radius-pill); background: var(--color-surface); color: var(--color-text-700); font: inherit; font-size: var(--font-size-sm); cursor: pointer; }
.filter-row button.active { border-color: var(--color-primary-700); background: var(--color-primary-soft); color: var(--color-primary-800); font-weight: 700; }
.shipment-table { overflow-x: auto; border-top: 1px solid var(--color-border); }
.table-header, .shipment-row { display: grid; grid-template-columns: 28px minmax(150px, 1.2fr) minmax(145px, 1fr) 100px minmax(145px, 1fr) 80px; align-items: center; gap: var(--space-3); min-width: 840px; padding: 0 var(--space-5); }
.table-header { min-height: 44px; background: var(--color-bg-muted); color: var(--color-text-muted); font-size: var(--font-size-xs); font-weight: 700; }
.shipment-row { min-height: 76px; border-top: 1px solid var(--color-border); color: var(--color-text-700); font-size: var(--font-size-sm); }
.shipment-row a { color: var(--color-primary-800); font-weight: 700; text-decoration: none; }.shipment-row a:hover { text-decoration: underline; }
.product-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.deadline { display: grid; gap: 2px; }.deadline small { color: var(--color-text-muted); font-size: var(--font-size-xs); }.deadline.is-overdue strong { color: var(--color-danger); }.deadline.is-today strong { color: var(--color-warning); }
.status-badge { width: fit-content; padding: var(--space-1) var(--space-2); border-radius: var(--radius-sm); background: var(--color-warning-soft); color: var(--color-warning); font-size: var(--font-size-xs); font-weight: 700; }
.batch-summary { display: grid; gap: var(--space-4); padding: var(--space-5); position: sticky; top: var(--space-5); }.batch-summary strong { color: var(--color-primary-800); font-size: var(--font-size-lg); }
.notice { display: flex; align-items: flex-start; gap: var(--space-2); padding: var(--space-3); border-radius: var(--radius-md); background: var(--color-info-soft); color: var(--color-text-700); font-size: var(--font-size-sm); line-height: 1.5; }.notice i { color: var(--color-info); }
.primary-button { min-height: 44px; border: 0; border-radius: var(--radius-md); padding: 0 var(--space-4); background: var(--color-primary-800); color: var(--color-surface); font: inherit; font-size: var(--font-size-sm); font-weight: 700; cursor: pointer; }.primary-button:hover:not(:disabled) { background: var(--color-primary-900); }.primary-button:disabled, .secondary-button:disabled { border-color: var(--color-disabled); background: var(--color-disabled-bg); color: var(--color-text-subtle); cursor: not-allowed; }
.state-card { min-height: 220px; display: grid; place-items: center; align-content: center; gap: var(--space-2); padding: var(--space-5); color: var(--color-text-muted); text-align: center; }.state-card i { font-size: 42px; }.state-error { color: var(--color-danger); }.state-error button { color: inherit; }
.modal-backdrop { position: fixed; z-index: 20; inset: 0; display: grid; place-items: center; padding: var(--space-4); background: rgba(15, 20, 29, .45); }.shipment-modal { width: min(800px, 100%); max-height: calc(100vh - 48px); overflow: auto; padding: var(--space-5); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--shadow-card); }.close-button { width: 40px; height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); color: var(--color-text-700); cursor: pointer; }.shipment-form-list { display: grid; gap: var(--space-3); margin: var(--space-4) 0; }.shipment-form-row { display: grid; grid-template-columns: 1.1fr 1fr 1fr; gap: var(--space-3); align-items: end; padding: var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); }.shipment-form-row > span { color: var(--color-text-muted); font-size: var(--font-size-xs); }.shipment-form-row label { display: grid; gap: var(--space-1); color: var(--color-text-700); font-size: var(--font-size-xs); font-weight: 700; }.form-error { color: var(--color-danger); font-size: var(--font-size-sm); }.batch-results { display: grid; gap: var(--space-1); margin: 0 0 var(--space-4); padding-left: var(--space-5); color: var(--color-success); font-size: var(--font-size-sm); }.batch-results .failed { color: var(--color-danger); }
button:focus-visible, a:focus-visible, select:focus-visible, input:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 1050px) { .batch-layout { grid-template-columns: 1fr; }.batch-summary { position: static; }.shipment-form-row { grid-template-columns: 1fr 1fr; }.shipment-form-row > strong, .shipment-form-row > span { grid-column: 1 / -1; } }
@media (max-width: 640px) { .page-header, .panel-heading, .shipment-modal header, .shipment-modal footer { align-items: flex-start; flex-direction: column; }.secondary-link { width: 100%; }.panel-heading select { width: 100%; }.shipment-form-row { grid-template-columns: 1fr; }.shipment-form-row > strong, .shipment-form-row > span { grid-column: auto; } }
</style>
