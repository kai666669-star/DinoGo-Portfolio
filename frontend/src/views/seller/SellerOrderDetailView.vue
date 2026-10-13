<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  createSellerShipment,
  getSellerOrder,
  getShipmentEvents,
  simulateTcatEvent,
  updateSellerShipmentTrackingInfo,
  updateSellerShipmentStatus,
} from '@/api/sellerOrderApi'
import { useSellerShipmentStatus } from './useSellerShipmentStatus'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const shipmentForm = ref({ carrierName: '', trackingNo: '' })
const carrierOptions = [
  { name: '黑貓宅急便', trackingNoTemplate: '範例：1234-5678-9012（12 碼）' },
  { name: '新竹物流', trackingNoTemplate: '範例：1234567890（10 碼）' },
  { name: '嘉里大榮物流', trackingNoTemplate: '範例：1234567890（10 碼）' },
]
const creatingShipment = ref(false)
const shipmentFormError = ref('')
const showShipmentConfirmModal = ref(false)
const editingShipmentInfo = ref(false)
const shipmentEvents = ref([])
const simulatingTcatEvent = ref(false)
const tcatSimulationError = ref('')
const fetchingOrder = ref(false)
const AUTO_REFRESH_INTERVAL_MS = 10_000
let autoRefreshTimer = null
let latestLoadRequestId = 0
const orderId = computed(() => Number(route.params.id))
const selectedCarrier = computed(() =>
  carrierOptions.find((carrier) => carrier.name === shipmentForm.value.carrierName),
)
const trackingNoPlaceholder = computed(
  () => selectedCarrier.value?.trackingNoTemplate ?? '請先選擇物流商',
)

const orderStatusLabels = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  PROCESSING: '備貨中',
  SHIPPED: '已出貨',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const paymentStatusLabels = {
  PENDING: '待付款',
  SUCCESS: '付款成功',
  FAILED: '付款失敗',
  CANCELLED: '付款已取消',
}
const shipmentStatusLabels = {
  PREPARING: '已建立寄件資料',
  SHIPPED: '已出貨',
  AVAILABLE_FOR_PICKUP: '可取貨',
  DELIVERED: '已送達',
}
const onlinePaymentProgressSteps = [
  {
    label: '訂單成立',
    statuses: ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'],
  },
  {
    label: '付款完成',
    statuses: ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'],
    paymentStatus: 'SUCCESS',
  },
  { label: '備貨中', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '已出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  { label: '已完成', statuses: ['COMPLETED'] },
]
const cashOnDeliveryProgressSteps = [
  { label: '訂單成立', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '備貨中', statuses: ['PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '已出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  {
    label: '已送達',
    statuses: ['SHIPPED', 'COMPLETED'],
    shipmentStatuses: ['AVAILABLE_FOR_PICKUP', 'DELIVERED'],
  },
  { label: '已完成', statuses: ['COMPLETED'] },
]
const isCashOnDelivery = computed(
  () => order.value?.payment?.paymentMethodCode === 'CASH_ON_DELIVERY',
)
const progressSteps = computed(() =>
  isCashOnDelivery.value ? cashOnDeliveryProgressSteps : onlinePaymentProgressSteps,
)
const canCreateShipment = computed(
  () => !order.value?.shipment && ['PAID', 'PROCESSING'].includes(order.value?.status),
)
const nextTcatEvent = computed(() => {
  if (order.value?.shipment?.status !== 'SHIPPED') return null
  const previous = shipmentEvents.value.at(-1)?.eventType
  if (previous === 'HANDED_OVER') return { type: 'IN_TRANSIT', label: '運送中' }
  if (previous === 'IN_TRANSIT') return { type: 'OUT_FOR_DELIVERY', label: '配送中' }
  if (previous === 'OUT_FOR_DELIVERY') return { type: 'DELIVERED', label: '已送達' }
  return null
})
const { shipmentAction, shipmentActionError, updatingShipment, updateShipmentStatus } =
  useSellerShipmentStatus({
    order,
    updateStatus: async (status) => {
      const shipment = (await updateSellerShipmentStatus(orderId.value, status)).data
      invalidatePendingOrderLoad()
      return shipment
    },
  })

const fullAddress = computed(() => {
  if (!order.value) return '-'
  return [
    order.value.shippingPostalCode,
    order.value.shippingCity,
    order.value.shippingDistrict,
    order.value.shippingDetailAddress,
  ]
    .filter(Boolean)
    .join(' ')
})

async function loadOrder({ silent = false, force = false } = {}) {
  if (fetchingOrder.value && !force) return

  const requestId = ++latestLoadRequestId
  fetchingOrder.value = true
  if (!silent) {
    loading.value = true
    errorMessage.value = ''
  }
  if (!Number.isInteger(orderId.value) || orderId.value <= 0) {
    if (!silent) {
      errorMessage.value = '訂單編號格式不正確。'
      if (requestId === latestLoadRequestId) loading.value = false
    }
    if (requestId === latestLoadRequestId) fetchingOrder.value = false
    return
  }
  try {
    const response = await getSellerOrder(orderId.value)
    let refreshedShipmentEvents = null
    if (response.data?.shipment) {
      try {
        refreshedShipmentEvents = (await getShipmentEvents(orderId.value)).data ?? []
      } catch {
        refreshedShipmentEvents = null
      }
    } else {
      refreshedShipmentEvents = []
    }
    if (requestId === latestLoadRequestId) {
      order.value = response.data
      if (refreshedShipmentEvents !== null) shipmentEvents.value = refreshedShipmentEvents
    }
  } catch (error) {
    if (!silent && requestId === latestLoadRequestId) {
      errorMessage.value = error.response?.data?.message ?? '無法載入訂單詳情。'
    }
  } finally {
    if (requestId === latestLoadRequestId) {
      if (!silent) loading.value = false
      fetchingOrder.value = false
    }
  }
}

function invalidatePendingOrderLoad() {
  latestLoadRequestId += 1
  fetchingOrder.value = false
}

function shouldAutoRefresh() {
  return order.value && !['COMPLETED', 'CANCELLED'].includes(order.value.status)
}

function refreshOrderSilently() {
  if (document.hidden || !shouldAutoRefresh()) return
  void loadOrder({ silent: true })
}

async function submitShipment() {
  if ((!canCreateShipment.value && !editingShipmentInfo.value) || creatingShipment.value) return

  shipmentFormError.value = ''
  const normalize = (value) => value.trim() || null
  const carrierName = normalize(shipmentForm.value.carrierName)
  const trackingNo = normalize(shipmentForm.value.trackingNo)
  if (!carrierName || !trackingNo) {
    shipmentFormError.value = '物流商與物流單號皆為必填。'
    return
  }

  creatingShipment.value = true
  try {
    const saveShipment = editingShipmentInfo.value
      ? updateSellerShipmentTrackingInfo
      : createSellerShipment
    const response = await saveShipment(orderId.value, {
      carrierName,
      trackingNo,
    })
    invalidatePendingOrderLoad()
    order.value.shipment = response.data
    editingShipmentInfo.value = false
  } catch (error) {
    shipmentFormError.value = error.response?.data?.message ?? '建立出貨資訊失敗，請稍後再試。'
  } finally {
    creatingShipment.value = false
  }
}

function openShipmentConfirmModal() {
  if (!shipmentAction.value) return
  showShipmentConfirmModal.value = true
}

function closeShipmentConfirmModal() {
  if (!updatingShipment.value) {
    showShipmentConfirmModal.value = false
  }
}

function editShipmentInfo() {
  if (updatingShipment.value || shipmentAction.value?.status !== 'SHIPPED') return
  shipmentForm.value = {
    carrierName: order.value?.shipment?.carrierName ?? '',
    trackingNo: order.value?.shipment?.trackingNo ?? '',
  }
  shipmentFormError.value = ''
  editingShipmentInfo.value = true
  showShipmentConfirmModal.value = false
}

async function confirmShipmentStatus() {
  await updateShipmentStatus()
  if (!shipmentActionError.value) {
    showShipmentConfirmModal.value = false
    await loadOrder({ force: true })
  }
}

function handleCarrierChange() {
  shipmentFormError.value = ''
}

async function simulateNextTcatEvent() {
  if (!nextTcatEvent.value || simulatingTcatEvent.value) return
  simulatingTcatEvent.value = true
  tcatSimulationError.value = ''
  try {
    const event = nextTcatEvent.value
    const response = await simulateTcatEvent(orderId.value, event.type)
    order.value.shipment = response.data
    shipmentEvents.value = [...shipmentEvents.value, { eventType: event.type }]
    void loadOrder({ silent: true, force: true })
  } catch (error) {
    tcatSimulationError.value = error.response?.data?.message ?? '物流模擬回報失敗。'
  } finally {
    simulatingTcatEvent.value = false
  }
}

const isStepComplete = (step) =>
  order.value &&
  step.statuses.includes(order.value.status) &&
  (!step.paymentStatus || order.value.payment?.status === step.paymentStatus) &&
  (!step.shipmentStatuses || step.shipmentStatuses.includes(order.value.shipment?.status))
const formatCurrency = (value) =>
  new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
const formatDate = (value) =>
  value
    ? new Intl.DateTimeFormat('zh-TW', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value))
    : '-'

onMounted(() => {
  void loadOrder()
  window.addEventListener('focus', refreshOrderSilently)
  document.addEventListener('visibilitychange', refreshOrderSilently)
  autoRefreshTimer = window.setInterval(refreshOrderSilently, AUTO_REFRESH_INTERVAL_MS)
})

onUnmounted(() => {
  window.removeEventListener('focus', refreshOrderSilently)
  document.removeEventListener('visibilitychange', refreshOrderSilently)
  if (autoRefreshTimer !== null) window.clearInterval(autoRefreshTimer)
})
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>商家訂單詳情</h1>
        <p class="page-description">核對買家、商品、付款與配送資訊。</p>
      </div>
      <RouterLink class="back-button" :to="{ name: 'SellerOrders' }">返回訂單列表</RouterLink>
    </header>

    <div v-if="loading" class="state-card" aria-live="polite">載入訂單中…</div>
    <div v-else-if="errorMessage" class="state-card state-error" role="alert">
      <strong>無法載入訂單</strong><span>{{ errorMessage }}</span>
      <button class="secondary-button" type="button" @click="loadOrder()">重新載入</button>
    </div>

    <template v-else-if="order">
      <section class="status-card">
        <div class="status-copy">
          <p class="section-label">訂單 {{ order.orderNo }}</p>
          <strong>{{ orderStatusLabels[order.status] ?? order.status }}</strong>
          <small>{{ formatDate(order.createdAt) }}</small>
        </div>
        <div class="order-progress" aria-label="訂單進度">
          <div
            v-for="step in progressSteps"
            :key="step.label"
            class="progress-item"
            :class="{ completed: isStepComplete(step), cancelled: order.status === 'CANCELLED' }"
          >
            <span class="progress-dot"></span><small>{{ step.label }}</small>
          </div>
        </div>
      </section>

      <div v-if="order.status === 'CANCELLED'" class="cancelled-notice" role="status">
        <strong>此訂單已取消</strong><span>{{ order.cancelReason || '未提供取消原因' }}</span>
      </div>

      <div class="detail-layout">
        <div class="detail-main">
          <section class="detail-card">
            <h2>商品明細</h2>
            <div class="item-list">
              <div v-for="item in order.items" :key="item.orderItemId" class="item-row">
                <div>
                  <strong>{{ item.productName }}</strong>
                  <p>{{ item.skuSpec || '單一規格' }} × {{ item.quantity }}</p>
                </div>
                <span>{{ formatCurrency(item.subtotal) }}</span>
              </div>
            </div>
            <div class="total-row">
              <span>商品小計</span><strong>{{ formatCurrency(order.subtotalAmount) }}</strong>
            </div>
            <div class="total-row compact-row">
              <span>運費</span><strong>{{ formatCurrency(order.shippingFee) }}</strong>
            </div>
            <div v-if="Number(order.discountAmount) > 0" class="total-row discount-row">
              <span>優惠折抵</span><strong>-{{ formatCurrency(order.discountAmount) }}</strong>
            </div>
            <div class="total-row payable-row">
              <span>訂單總額</span><strong>{{ formatCurrency(order.totalAmount) }}</strong>
            </div>
          </section>

          <section class="detail-card">
            <h2>買家與收件資料</h2>
            <div class="info-grid">
              <div>
                <p class="section-label">收件人</p>
                <strong>{{ order.receiverName }}</strong>
              </div>
              <div>
                <p class="section-label">電話</p>
                <strong>{{ order.receiverPhone }}</strong>
              </div>
              <div class="full-width">
                <p class="section-label">地址</p>
                <strong>{{ fullAddress }}</strong>
              </div>
              <div class="full-width">
                <p class="section-label">買家備註</p>
                <strong>{{ order.buyerRemark || '無' }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-card">
            <h2>付款資訊</h2>
            <div v-if="order.payment" class="info-grid">
              <div>
                <p class="section-label">付款方式</p>
                <strong>{{
                  order.payment.paymentMethodName ?? order.payment.paymentMethodCode
                }}</strong>
              </div>
              <div>
                <p class="section-label">付款狀態</p>
                <strong>{{
                  paymentStatusLabels[order.payment.status] ?? order.payment.status
                }}</strong>
              </div>
              <div v-if="order.payment.paidAt" class="full-width">
                <p class="section-label">付款時間</p>
                <strong>{{ formatDate(order.payment.paidAt) }}</strong>
              </div>
              <div v-if="order.payment.failureReason" class="full-width failure-message">
                <p class="section-label">失敗原因</p>
                <strong>{{ order.payment.failureReason }}</strong>
              </div>
            </div>
            <p v-else class="empty-message">尚未建立付款紀錄。</p>
          </section>
        </div>

        <aside class="shipping-card">
          <h2>物流資訊</h2>
          <template v-if="order.shipment && !editingShipmentInfo">
            <div>
              <p class="section-label">物流狀態</p>
              <strong>{{
                shipmentStatusLabels[order.shipment.status] ?? order.shipment.status
              }}</strong>
            </div>
            <div>
              <p class="section-label">物流商</p>
              <strong>{{ order.shipment.carrierName || '尚未填寫' }}</strong>
            </div>
            <div>
              <p class="section-label">物流單號</p>
              <strong>{{ order.shipment.trackingNo || '尚未填寫' }}</strong>
            </div>
            <div v-if="order.shipment.shippedAt">
              <p class="section-label">出貨時間</p>
              <strong>{{ formatDate(order.shipment.shippedAt) }}</strong>
            </div>
            <div v-if="order.shipment.availablePickupAt">
              <p class="section-label">可取貨時間</p>
              <strong>{{ formatDate(order.shipment.availablePickupAt) }}</strong>
            </div>
            <div v-if="order.shipment.deliveredAt">
              <p class="section-label">送達時間</p>
              <strong>{{ formatDate(order.shipment.deliveredAt) }}</strong>
            </div>
            <p v-if="shipmentActionError" class="form-error" role="alert">
              {{ shipmentActionError }}
            </p>
            <button
              v-if="shipmentAction"
              class="shipment-submit"
              type="button"
              :disabled="updatingShipment"
              @click="shipmentAction.status === 'AVAILABLE_FOR_PICKUP' ? updateShipmentStatus() : openShipmentConfirmModal()"
            >
              {{ shipmentAction.label }}
            </button>
            <button
              v-if="nextTcatEvent"
              class="secondary-button"
              type="button"
              :disabled="simulatingTcatEvent"
              @click="simulateNextTcatEvent"
            >
              {{ nextTcatEvent.label }}
            </button>
            <p v-if="tcatSimulationError" class="form-error" role="alert">{{ tcatSimulationError }}</p>
            <div
              v-if="showShipmentConfirmModal"
              class="modal-backdrop"
              @click.self="closeShipmentConfirmModal"
            >
              <section
                class="shipment-confirm-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="shipment-confirm-title"
              >
                <h2 id="shipment-confirm-title">
                  {{ shipmentAction?.status === 'SHIPPED' ? '確認此筆訂單已出貨？' : '確認商品已送達取貨地點？' }}
                </h2>

                <p>
                  {{ shipmentAction?.status === 'SHIPPED'
                    ? '確認後，訂單狀態將更新為「已出貨」，買家訂單詳情也會同步顯示「商品出貨」。'
                    : '確認後，物流狀態將更新為「可取貨」。' }}
                </p>

                <dl class="confirm-order-info">
                  <div>
                    <dt>訂單編號</dt>
                    <dd>{{ order.orderNo }}</dd>
                  </div>
                  <div>
                    <dt>物流商</dt>
                    <dd>{{ order.shipment?.carrierName }}</dd>
                  </div>
                  <div>
                    <dt>物流單號</dt>
                    <dd>{{ order.shipment?.trackingNo }}</dd>
                  </div>
                </dl>

                <p v-if="shipmentActionError" class="form-error" role="alert">
                  {{ shipmentActionError }}
                </p>

                <div class="modal-actions">
                  <button
                    type="button"
                    class="secondary-button"
                    :disabled="updatingShipment"
                    @click="shipmentAction?.status === 'SHIPPED' ? editShipmentInfo() : closeShipmentConfirmModal()"
                  >
                    {{ shipmentAction?.status === 'SHIPPED' ? '重新填寫物流資訊' : '取消' }}
                  </button>
                  <button
                    type="button"
                    class="shipment-submit"
                    :disabled="updatingShipment"
                    @click="confirmShipmentStatus"
                  >
                    {{ updatingShipment ? '出貨確認中…' : '確認出貨' }}
                  </button>
                </div>
              </section>
            </div>
          </template>
          <form
            v-else-if="canCreateShipment || editingShipmentInfo"
            class="shipment-form"
            @submit.prevent="submitShipment"
          >
            <p class="form-description">
              {{ editingShipmentInfo ? '修改物流商或單號後，再確認商品已交寄。' : '填寫物流商與單號後，系統只會建立寄件資料；商品尚未視為已交寄。' }}
            </p>
            <div class="form-field">
              <label for="carrier-name">物流商</label>
              <select
                id="carrier-name"
                v-model="shipmentForm.carrierName"
                name="carrierName"
                required
                :aria-invalid="Boolean(shipmentFormError)"
                :disabled="creatingShipment"
                @change="handleCarrierChange"
              >
                <option value="" disabled>請選擇物流商</option>
                <option v-for="carrier in carrierOptions" :key="carrier.name" :value="carrier.name">
                  {{ carrier.name }}
                </option>
              </select>
            </div>
            <div class="form-field">
              <label for="tracking-no">物流單號</label>
              <input
                id="tracking-no"
                v-model="shipmentForm.trackingNo"
                name="trackingNo"
                type="text"
                maxlength="100"
                required
                autocomplete="off"
                :placeholder="trackingNoPlaceholder"
                :aria-invalid="Boolean(shipmentFormError)"
                :disabled="creatingShipment"
              />
            </div>
            <p v-if="shipmentFormError" class="form-error" role="alert">{{ shipmentFormError }}</p>
            <button class="shipment-submit" type="submit" :disabled="creatingShipment">
              {{ creatingShipment ? '儲存中…' : editingShipmentInfo ? '儲存寄件資料' : '建立寄件資料' }}
            </button>
          </form>
          <p v-else class="empty-message">此訂單目前無法建立物流資料。</p>
        </aside>
      </div>
    </template>
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
.page-description,
.section-label,
.detail-card p,
.shipping-card p,
.status-card small {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.eyebrow,
.page-description,
.section-label {
  margin: 0 0 var(--space-1);
}
h1,
h2,
p {
  margin-top: 0;
}
h1 {
  margin-bottom: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}
h2 {
  margin-bottom: var(--space-4);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}
.back-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-weight: 700;
  text-decoration: none;
}
.state-card,
.status-card,
.detail-card,
.shipping-card,
.cancelled-notice {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  padding: var(--space-5);
}
.state-card,
.cancelled-notice {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.state-error,
.failure-message,
.cancelled-notice {
  color: var(--color-danger);
}
.status-card {
  display: grid;
  gap: var(--space-5);
}
.status-copy {
  display: grid;
  gap: var(--space-1);
}
.status-copy > strong {
  color: var(--color-warning);
  font-size: var(--font-size-xl);
}
.order-progress {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
}
.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
  text-align: center;
}
.progress-item::before,
.progress-item::after {
  position: absolute;
  top: 5px;
  width: 50%;
  height: 2px;
  content: '';
  background: var(--color-border);
}
.progress-item::before {
  left: 0;
}
.progress-item::after {
  right: 0;
}
.progress-item:first-child::before,
.progress-item:last-child::after {
  display: none;
}
.progress-item.completed::before,
.progress-item.completed::after,
.progress-item.completed .progress-dot {
  background: var(--color-primary);
}
.progress-item.cancelled::before,
.progress-item.cancelled::after,
.progress-item.cancelled .progress-dot {
  background: var(--color-border);
}
.progress-dot {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  box-shadow: 0 0 0 6px var(--color-surface);
  border-radius: 50%;
  background: var(--color-border-strong);
}
.progress-item.completed {
  color: var(--color-text-700);
  font-weight: 700;
}
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  align-items: start;
}
.detail-main,
.item-list,
.shipping-card {
  display: grid;
  gap: var(--space-4);
}
.item-row,
.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}
.item-row {
  min-height: 52px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-3);
}
.item-row p {
  margin: var(--space-1) 0 0;
}
.total-row {
  padding-top: var(--space-4);
}
.compact-row {
  padding-top: var(--space-2);
}
.total-row strong {
  font-size: var(--font-size-lg);
}
.discount-row {
  color: var(--color-success);
}
.discount-row strong {
  font-size: var(--font-size-base);
}
.payable-row {
  border-top: 1px solid var(--color-border);
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}
.full-width {
  grid-column: 1 / -1;
}
.shipping-card {
  position: sticky;
  top: var(--space-5);
}
.shipping-card h2,
.shipping-card p,
.empty-message {
  margin-bottom: 0;
}
.shipment-form,
.form-field {
  display: grid;
  gap: var(--space-2);
}
.shipment-form {
  gap: var(--space-4);
}
.form-description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.form-field label {
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.form-field input,
.form-field select {
  min-height: 42px;
  width: 100%;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}
.form-field input:focus-visible,
.form-field select:focus-visible {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}
.form-field input:disabled,
.form-field select:disabled {
  border-color: var(--color-disabled);
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
  cursor: not-allowed;
}
.form-error {
  color: var(--color-danger) !important;
  font-size: var(--font-size-sm);
}
.shipment-submit {
  border: 1px solid var(--color-primary-700);
  background: var(--color-primary-700);
  color: var(--color-surface);
}
.shipment-submit:hover:not(:disabled) {
  background: var(--color-primary-800);
}
.shipment-submit:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
.shipment-submit:disabled {
  border-color: var(--color-disabled);
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
  cursor: not-allowed;
}
button {
  min-height: 42px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font-weight: 700;
}
.secondary-button {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-700);
}
.secondary-button:focus-visible,
.back-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .shipping-card {
    position: static;
  }
}
@media (max-width: 720px) {
  .page-header,
  .state-card,
  .cancelled-notice {
    align-items: flex-start;
    flex-direction: column;
  }
  .order-progress,
  .info-grid {
    grid-template-columns: 1fr;
  }
  .progress-item {
    justify-items: start;
    text-align: left;
  }
  .progress-item::before,
  .progress-item::after {
    display: none;
  }
  .full-width {
    grid-column: auto;
  }
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: var(--space-4);
  background: rgb(0 0 0 / 45%);
}

.shipment-confirm-modal {
  width: min(100%, 480px);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--color-surface);
  box-shadow: var(--shadow-lg);
}

.confirm-order-info {
  display: grid;
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.confirm-order-info div,
.modal-actions {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
}

.confirm-order-info dt {
  color: var(--color-text-muted);
}

.confirm-order-info dd {
  margin: 0;
  font-weight: 700;
}

.modal-actions {
  justify-content: flex-end;
}

</style>
