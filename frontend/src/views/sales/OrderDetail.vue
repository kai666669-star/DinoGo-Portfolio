<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  cancelOrder,
  confirmDelivery,
  createPayment,
  getOrder,
  getShipmentEvents,
  submitEcpayCheckout,
} from '@/api/order'
import { getOrderDisplayStatus } from '@/utils/orderDisplayStatus'
import { getImageUrl } from '@/utils/imageUrl'
import { getOrderStars } from '@/api/review'
import OrderItemReviewView from '@/views/review/OrderItemReviewView.vue'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const confirmingDelivery = ref(false)
const deliveryErrorMessage = ref('')
const shipmentEvents = ref([])
const cancellingOrder = ref(false)
const cancellationErrorMessage = ref('')
const cancellationReason = ref('')
const showCancellationModal = ref(false)
const retryingPayment = ref(false)
const paymentRetryErrorMessage = ref('')
const reviewItemId = ref(null)
const reviewStarsByOrderItemId = ref(new Map())
const fetchingOrder = ref(false)
const AUTO_REFRESH_INTERVAL_MS = 10_000
let autoRefreshTimer = null
let latestLoadRequestId = 0

const orderId = computed(() => Number(route.params.id ?? route.params.orderId))
const canCancelOrder = computed(() => {
  if (order.value?.status === 'PENDING_PAYMENT') return true

  return order.value?.status === 'PROCESSING'
    && order.value.payment?.paymentMethodCode === 'CASH_ON_DELIVERY'
    && order.value.payment?.status === 'PENDING'
    && (!order.value.shipment || order.value.shipment.status === 'PREPARING')
})
const displayStatus = computed(() => getOrderDisplayStatus(order.value))
const canRetryCreditCardPayment = computed(() => (
  order.value?.status === 'PENDING_PAYMENT'
  && order.value.payment?.paymentMethodCode === 'CREDIT_CARD'
  && ['PENDING', 'FAILED'].includes(order.value.payment?.status)
))

const paymentStatusLabels = {
  PENDING: '待付款',
  SUCCESS: '已付款',
  FAILED: '付款失敗',
  CANCELLED: '付款已取消',
}

const shipmentStatusLabels = {
  PREPARING: '備貨中',
  SHIPPED: '已出貨',
  AVAILABLE_FOR_PICKUP: '可取貨',
  DELIVERED: '已送達',
}
const shipmentEventLabels = {
  LABEL_CREATED: '賣家已建立寄件資料', HANDED_OVER: '賣家已交寄',
  IN_TRANSIT: '包裹抵達理貨中心', OUT_FOR_DELIVERY: '配送中',
  AVAILABLE_FOR_PICKUP: '包裹已可取貨', DELIVERED: '已送達',
}

const progressSteps = [
  {
    label: '訂單成立',
    statuses: ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'],
  },
  { label: '賣家處理', statuses: ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'] },
  { label: '商品出貨', statuses: ['SHIPPED', 'COMPLETED'] },
  {
    label: '待收貨',
    statuses: ['COMPLETED'],
    shipmentStatuses: ['DELIVERED'],
  },
  { label: '完成訂單', statuses: ['COMPLETED'] },
]

const fullAddress = computed(() => {
  if (!order.value) return '—'
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
    const response = await getOrder(orderId.value)
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
      errorMessage.value = error.response?.data?.message ?? '訂單詳情載入失敗，請稍後再試。'
    }
  } finally {
    if (requestId === latestLoadRequestId) {
      if (!silent) loading.value = false
      fetchingOrder.value = false
    }
  }
}

function applyOrderResponse(nextOrder) {
  latestLoadRequestId += 1
  fetchingOrder.value = false
  order.value = nextOrder
}

function shouldAutoRefresh() {
  return order.value && !['COMPLETED', 'CANCELLED'].includes(order.value.status)
}

function refreshOrderSilently() {
  if (document.hidden || !shouldAutoRefresh()) return
  void loadOrder({ silent: true })
}

async function handleConfirmDelivery() {
  if (!order.value?.shipment || confirmingDelivery.value) return
  confirmingDelivery.value = true
  deliveryErrorMessage.value = ''
  try {
    await confirmDelivery(orderId.value)
    await loadOrder({ silent: true, force: true })
  } catch (error) {
    deliveryErrorMessage.value = error.response?.data?.message ?? '確認收貨失敗，請稍後再試'
  } finally {
    confirmingDelivery.value = false
  }
}

function openCancellationModal() {
  if (!canCancelOrder.value) return
  cancellationReason.value = ''
  cancellationErrorMessage.value = ''
  showCancellationModal.value = true
}

function closeCancellationModal() {
  if (!cancellingOrder.value) showCancellationModal.value = false
}

async function handleCancelOrder() {
  if (!canCancelOrder.value || cancellingOrder.value) return

  const reason = cancellationReason.value.trim()
  if (!reason) {
    cancellationErrorMessage.value = '請輸入取消訂單原因'
    return
  }
  if (reason.length > 500) {
    cancellationErrorMessage.value = '取消原因不可超過 500 字'
    return
  }
  cancellingOrder.value = true
  cancellationErrorMessage.value = ''
  try {
    applyOrderResponse((await cancelOrder(orderId.value, { reason })).data)
    showCancellationModal.value = false
  } catch (error) {
    cancellationErrorMessage.value = error.response?.data?.message ?? '取消訂單失敗，請稍後再試'
  } finally {
    cancellingOrder.value = false
  }
}

async function handleRetryCreditCardPayment() {
  if (!canRetryCreditCardPayment.value || retryingPayment.value) return

  retryingPayment.value = true
  paymentRetryErrorMessage.value = ''
  try {
    const response = await createPayment(orderId.value, 'CREDIT_CARD')
    const checkout = response.data?.ecpayCheckout
    if (!checkout) {
      paymentRetryErrorMessage.value = '目前無法建立信用卡付款，請稍後再試。'
      return
    }
    submitEcpayCheckout(checkout)
  } catch (error) {
    paymentRetryErrorMessage.value = error.response?.data?.message ?? '重新付款失敗，請稍後再試。'
  } finally {
    retryingPayment.value = false
  }
}

function isStepComplete(step) {
  if (!order.value) return false
  if (step.shipmentStatuses?.includes(order.value.shipment?.status)) return true
  return step.statuses.includes(order.value.status)
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
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function isItemReviewed(item) {
  return Number(reviewStarsByOrderItemId.value.get(item.orderItemId)?.fiveStar ?? 0) > 0
}

async function loadOrderReviewStars() {
  if (order.value?.status !== 'COMPLETED') {
    reviewStarsByOrderItemId.value = new Map()
    return
  }
  try {
    const response = await getOrderStars(orderId.value)
    reviewStarsByOrderItemId.value = new Map(
      (response.data ?? []).map((star) => [star.orderItemId, star]),
    )
  } catch {
    reviewStarsByOrderItemId.value = new Map()
  }
}

function openReviewModal(item) {
  reviewItemId.value = item.orderItemId
}

function closeReviewModal() {
  reviewItemId.value = null
}

function updateReviewStar(nextStar) {
  reviewStarsByOrderItemId.value = new Map(reviewStarsByOrderItemId.value).set(
    nextStar.orderItemId,
    nextStar,
  )
}

onMounted(() => {
  void loadOrder().then(loadOrderReviewStars)
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
  <section class="order-detail-page">
    <div class="container detail-container">
      <header class="page-header">
        <div>
          <h1>訂單詳情</h1>
          <p v-if="order">訂單 #{{ order.orderNo }}・建立於 {{ formatDate(order.createdAt) }}</p>
          <p v-else>查看訂單商品、配送與付款資訊</p>
        </div>

        <div class="header-actions">
          <button
            v-if="canCancelOrder"
            class="cancel-order-button"
            type="button"
            :disabled="cancellingOrder"
            @click="openCancellationModal"
          >
            {{ cancellingOrder ? '取消中...' : '取消訂單' }}
          </button>
          <RouterLink class="back-button" :to="{ name: 'MemberOrders' }"> 返回訂單列表 </RouterLink>
        </div>
      </header>

      <p v-if="cancellationErrorMessage && !showCancellationModal" class="cancellation-error" role="alert">
        {{ cancellationErrorMessage }}
      </p>

      <div v-if="loading" class="state-card" aria-live="polite">
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span>正在載入訂單詳情...</span>
      </div>

      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>無法載入訂單</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadOrder()">重新載入</button>
      </div>

      <template v-else-if="order">
        <section class="progress-card" aria-label="訂單進度">
          <div
            v-for="(step, index) in progressSteps"
            :key="step.label"
            class="progress-step"
            :class="{ complete: isStepComplete(step), cancelled: order.status === 'CANCELLED' }"
          >
            <span class="step-marker">
              <i
                class="bi"
                :class="isStepComplete(step) ? 'bi-check-lg' : 'bi-circle'"
                aria-hidden="true"
              ></i>
            </span>
            <strong>{{ step.label }}</strong>
            <span
              v-if="index < progressSteps.length - 1"
              class="step-line"
              aria-hidden="true"
            ></span>
          </div>
        </section>

        <div v-if="order.status === 'CANCELLED'" class="cancelled-notice" role="status">
          <i class="bi bi-x-circle" aria-hidden="true"></i>
          <div>
            <strong>此訂單已取消</strong>
            <span>{{ order.cancelReason || '未提供取消原因' }}</span>
          </div>
        </div>

        <div class="detail-columns">
          <section class="detail-card product-card">
            <div class="card-heading">
              <h2>商品明細</h2>
              <span class="status-badge" :class="`status-${displayStatus.key.toLowerCase()}`">
                {{ displayStatus.label }}
              </span>
            </div>

            <article v-for="item in order.items" :key="item.orderItemId" class="product-row">
              <div class="product-image">
                <img
                  v-if="item.productImageUrl"
                  :src="getImageUrl(item.productImageUrl)"
                  :alt="item.productName"
                />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>

              <div class="product-copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ item.skuSpec || '單一規格' }}・數量 {{ item.quantity }}</span>
                <small>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</small>
              </div>

              <strong class="product-subtotal">{{ formatCurrency(item.subtotal) }}</strong>

              <button
                v-if="order.status === 'COMPLETED'"
                type="button"
                class="review-endcap"
                :class="isItemReviewed(item) ? 'review-endcap--reviewed' : 'review-endcap--pending'"
                :aria-label="isItemReviewed(item) ? `修改 ${item.productName} 的評價` : `評價 ${item.productName}`"
                :title="isItemReviewed(item) ? '已評價' : '未評價'"
                @click="openReviewModal(item)"
              >
                <!-- //review-未評價// -->
                <i v-if="!isItemReviewed(item)" class="bi bi-star-fill" aria-hidden="true"></i>
                <!-- //review-已評價// -->
                <i v-else class="bi bi-star-fill" aria-hidden="true"></i>
                <span>{{ isItemReviewed(item) ? '修改' : '評價' }}</span>
              </button>
            </article>

            <div class="remark-row">
              <span>訂單備註</span>
              <strong>{{ order.buyerRemark || '無' }}</strong>
            </div>
          </section>

          <aside class="detail-card delivery-card">
            <h2>配送與付款</h2>

            <section class="info-section">
              <h3>收件資訊</h3>
              <p>{{ order.receiverName }}・{{ order.receiverPhone }}</p>
              <p>{{ fullAddress }}</p>
            </section>

            <section class="info-section">
              <h3>付款資訊</h3>
              <template v-if="order.payment">
                <p>{{ paymentStatusLabels[order.payment.status] ?? order.payment.status }}</p>
                <p class="muted">
                  {{ order.payment.paymentMethodName ?? order.payment.paymentMethodCode }}
                </p>
                <p v-if="order.payment.failureReason" class="muted payment-failure">
                  {{ order.payment.failureReason }}
                </p>
                <div v-if="canRetryCreditCardPayment" class="payment-actions">
                  <button
                    class="retry-payment-button"
                    type="button"
                    :disabled="retryingPayment"
                    @click="handleRetryCreditCardPayment"
                  >
                    <span
                      v-if="retryingPayment"
                      class="spinner-border spinner-border-sm"
                      aria-hidden="true"
                    ></span>
                    {{ retryingPayment ? '正在建立付款…' : '重新信用卡付款' }}
                  </button>
                  <RouterLink class="payment-back-button" :to="{ name: 'MemberOrders' }">
                    返回訂單列表
                  </RouterLink>
                </div>
                <p v-if="paymentRetryErrorMessage" class="payment-retry-error" role="alert">
                  {{ paymentRetryErrorMessage }}
                </p>
              </template>
              <p v-else>尚未建立付款資料</p>
            </section>

            <section class="info-section">
              <h3>物流資訊</h3>
              <template v-if="order.shipment">
                <p>{{ shipmentStatusLabels[order.shipment.status] ?? order.shipment.status }}</p>
                <p class="muted">
                  {{ order.shipment.carrierName || '物流商待確認' }}
                  <template v-if="order.shipment.trackingNo">
                    ・{{ order.shipment.trackingNo }}
                  </template>
                </p>
                <ol v-if="shipmentEvents.length" class="shipment-timeline" aria-label="物流軌跡">
                  <li v-for="event in shipmentEvents" :key="event.shipmentEventId">
                    <strong>{{ shipmentEventLabels[event.eventType] ?? event.eventType }}</strong>
                    <small>{{ formatDate(event.occurredAt) }}</small>
                  </li>
                </ol>
                <button
                  v-if="order.status === 'SHIPPED' && ['AVAILABLE_FOR_PICKUP', 'DELIVERED'].includes(order.shipment.status)"
                  class="btn btn-primary mt-2"
                  type="button"
                  :disabled="confirmingDelivery"
                  @click="handleConfirmDelivery"
                >
                  {{ confirmingDelivery ? '確認中...' : '完成訂單' }}
                </button>
                <p v-if="deliveryErrorMessage" class="text-danger mt-2" role="alert">
                  {{ deliveryErrorMessage }}
                </p>
              </template>
              <p v-else>尚未建立物流資料</p>
            </section>

            <dl class="amount-summary">
              <div>
                <dt>商品小計</dt>
                <dd>{{ formatCurrency(order.subtotalAmount) }}</dd>
              </div>
              <div>
                <dt>運費</dt>
                <dd>{{ formatCurrency(order.shippingFee) }}</dd>
              </div>
              <div>
                <dt>折扣</dt>
                <dd>− {{ formatCurrency(order.discountAmount) }}</dd>
              </div>
              <div class="total-row">
                <dt>訂單總額</dt>
                <dd>{{ formatCurrency(order.totalAmount) }}</dd>
              </div>
            </dl>
          </aside>
        </div>
      </template>

      <div
        v-if="showCancellationModal"
        class="modal-backdrop"
        @click.self="closeCancellationModal"
      >
        <form class="cancellation-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-order-title" @submit.prevent="handleCancelOrder">
          <h2 id="cancel-order-title">取消此筆訂單？</h2>
          <p>取消後無法復原；符合資格的訂單將停止處理並回補庫存。</p>
          <label for="cancellation-reason">取消原因</label>
          <textarea
            id="cancellation-reason"
            v-model="cancellationReason"
            maxlength="500"
            required
            autofocus
            placeholder="請說明取消原因"
            :disabled="cancellingOrder"
            :aria-invalid="Boolean(cancellationErrorMessage)"
          ></textarea>
          <div class="reason-count">{{ cancellationReason.length }} / 500</div>
          <p v-if="cancellationErrorMessage" class="cancellation-error" role="alert">
            {{ cancellationErrorMessage }}
          </p>
          <div class="modal-actions">
            <button type="button" class="modal-secondary-button" :disabled="cancellingOrder" @click="closeCancellationModal">返回訂單</button>
            <button type="submit" class="modal-danger-button" :disabled="cancellingOrder">
              {{ cancellingOrder ? '取消中...' : '確認取消訂單' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <OrderItemReviewView
      v-if="reviewItemId !== null"
      :order-data="order"
      :initial-order-item-id="reviewItemId"
      modal
      @close="closeReviewModal"
      @updated="updateReviewStar"
    />
  </section>
</template>

<style scoped>
.shipment-timeline { display: grid; gap: var(--space-3); margin: var(--space-4) 0 0; padding-left: var(--space-4); border-left: 2px solid var(--color-primary-300); }
.shipment-timeline li { display: grid; gap: var(--space-1); color: var(--color-text-700); font-size: var(--font-size-base); line-height: var(--line-height-base); }
.shipment-timeline small, .shipment-timeline span { color: var(--color-text-muted); font-size: var(--font-size-sm); line-height: var(--line-height-base); }
.order-detail-page {
  min-height: 620px;
  padding: 40px 0;
  background: var(--color-bg);
}

.detail-container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
}

.page-header {
  display: flex;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
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
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.cancel-order-button {
  min-height: 42px;
  padding: 0 var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-surface);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
}

.cancel-order-button:hover:not(:disabled) {
  color: var(--color-surface);
  background: var(--color-danger);
}

.cancel-order-button:disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}

.cancellation-error {
  margin: var(--space-2) 0 0;
  padding: var(--space-3) var(--space-4);
  color: var(--color-danger);
  background: var(--color-danger-soft);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
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

.cancellation-modal {
  display: grid;
  width: min(100%, 480px);
  gap: var(--space-3);
  padding: var(--space-5);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.cancellation-modal h2,
.cancellation-modal p {
  margin: 0;
}

.cancellation-modal > p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.cancellation-modal label {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.cancellation-modal textarea {
  min-height: 112px;
  padding: var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  font: inherit;
  resize: vertical;
}

.cancellation-modal textarea:focus-visible {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.reason-count {
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
  text-align: right;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.modal-secondary-button,
.modal-danger-button {
  min-height: 42px;
  padding: 0 var(--space-4);
  border-radius: var(--radius-md);
  font: inherit;
  font-weight: 700;
}

.modal-secondary-button {
  color: var(--color-text-700);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
}

.modal-danger-button {
  color: var(--color-surface);
  background: var(--color-danger);
  border: 1px solid var(--color-danger);
}

.modal-secondary-button:focus-visible,
.modal-danger-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.back-button {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  color: var(--color-surface);
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-decoration: none;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.back-button:hover {
  color: var(--color-surface);
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.back-button:focus-visible,
.cancel-order-button:focus-visible,
.state-card button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.progress-card {
  display: grid;
  min-height: 130px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  gap: 18px;
  margin-top: var(--space-5);
  padding: 22px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.progress-step {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: var(--space-2);
  color: var(--color-text-muted);
}

.progress-step strong {
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.step-marker {
  z-index: 1;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--color-text-muted);
  background: var(--color-bg-muted);
  border-radius: var(--radius-pill);
}

.progress-step.complete {
  color: var(--color-text);
}

.progress-step.complete .step-marker {
  color: var(--color-surface);
  background: var(--color-primary);
}

.step-line {
  position: absolute;
  top: 16px;
  left: calc(50% + 26px);
  width: calc(100% - 34px);
  height: 2px;
  background: var(--color-border);
}

.progress-step.complete .step-line {
  background: var(--color-primary-300);
}

.cancelled-notice {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-5);
  padding: var(--space-4);
  color: var(--color-danger);
  background: var(--color-danger-soft);
  border-radius: var(--radius-lg);
}

.cancelled-notice > i {
  font-size: var(--font-size-lg);
}

.cancelled-notice div {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cancelled-notice span {
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.detail-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
  margin-top: var(--space-4);
}

.detail-card {
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.detail-card h2,
.detail-card h3 {
  margin: 0;
  color: var(--color-text);
}

.detail-card h2 {
  font-size: 19px;
  font-weight: 700;
}

.detail-card h3 {
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.product-row {
  display: grid;
  min-height: 96px;
  grid-template-columns: 82px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: var(--space-3) 0;
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

.product-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.product-copy strong {
  overflow: hidden;
  font-size: var(--font-size-base);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-copy span,
.product-copy small {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.product-subtotal {
  font-size: var(--font-size-md);
  white-space: nowrap;
}

.review-endcap {
  display: grid;
  min-width: calc(var(--space-7) + var(--space-2));
  align-self: stretch;
  align-content: center;
  place-items: center;
  gap: var(--space-1);
  margin-block: calc(var(--space-3) * -1);
  font-size: var(--font-size-lg);
  text-decoration: none;
  appearance: none;
  border: 0;
  border-left: 1px solid var(--color-border);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  box-shadow: none;
}

.review-endcap span {
  font-size: var(--font-size-xs);
  font-weight: 700;
  line-height: 1;
}

/* 未評價：強烈主視覺綠色 */
.review-endcap--pending {
  color: var(--color-surface);
  background: var(--color-primary-active);
}
/* 已評價：灰暗主視覺綠色 */
.review-endcap--reviewed {
  color: var(--color-primary-600);
  background: var(--color-primary-100);
}

.review-endcap:hover {
  color: var(--color-surface);
  background: var(--color-primary-hover);
}

.review-endcap:active {
  background: var(--color-primary-active);
}

.review-endcap:focus-visible {
  
  box-shadow: none;
}

.remark-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
  border-top: 1px solid var(--color-border);
}

.remark-row strong {
  color: var(--color-text);
  text-align: right;
}

.status-badge {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  justify-content: center;
  padding: var(--space-1) var(--space-3);
  color: var(--color-info);
  font-size: var(--font-size-sm);
  font-weight: 600;
  line-height: 1.3;
  background: var(--color-info-soft);
  border-radius: var(--radius-sm);
}

.status-pending_payment,
.status-pending_shipment,
.status-pending_pickup {
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.status-completed {
  color: var(--color-success);
  background: var(--color-success-soft);
}

.status-cancelled {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.delivery-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.info-section p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.7;
}

.info-section .muted {
  color: var(--color-text-subtle);
}

.retry-payment-button {
  display: inline-flex;
  width: fit-content;
  min-height: 40px;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding: 0 var(--space-3);
  color: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 700;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.payment-actions .retry-payment-button {
  margin-top: 0;
}

.payment-back-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  padding: 0 var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.payment-back-button:hover {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
}

.payment-back-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.retry-payment-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.retry-payment-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.retry-payment-button:disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}

.payment-retry-error {
  margin: var(--space-2) 0 0;
  color: var(--color-danger) !important;
}

.amount-summary {
  display: grid;
  gap: var(--space-2);
  margin: auto 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.amount-summary div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
}

.amount-summary dt,
.amount-summary dd {
  margin: 0;
  font-size: var(--font-size-sm);
}

.amount-summary dt {
  color: var(--color-text-muted);
  font-weight: 400;
}

.amount-summary dd {
  color: var(--color-text);
  font-weight: 600;
}

.amount-summary .total-row {
  margin-top: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.amount-summary .total-row dt,
.amount-summary .total-row dd {
  color: var(--color-primary-active);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.state-card {
  display: flex;
  min-height: 220px;
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

.state-card strong {
  color: var(--color-text);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.state-card > i {
  color: var(--color-danger);
  font-size: var(--font-size-xl);
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

@media (max-width: 991.98px) {
  .detail-columns {
    grid-template-columns: 1fr;
  }

  .delivery-card {
    min-height: auto;
  }
}

@media (max-width: 767.98px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    padding-bottom: var(--space-3);
  }

  .header-actions {
    width: 100%;
  }

  .progress-card {
    grid-template-columns: repeat(2, 1fr);
    row-gap: var(--space-5);
  }

  .step-line {
    display: none;
  }
}

@media (max-width: 575.98px) {
  .order-detail-page {
    padding: var(--space-6) 0;
  }
}

@media (max-width: 479.98px) {
  .header-actions {
    flex-direction: column;
  }

  .cancel-order-button,
  .back-button {
    width: 100%;
  }

  .product-row {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .product-image {
    width: 64px;
    height: 64px;
  }

  .product-subtotal {
    grid-column: 2;
  }
}
</style>
