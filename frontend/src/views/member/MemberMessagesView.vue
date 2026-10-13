<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  announceMemberUnreadChanged,
  deleteMemberMessage,
  getMemberInbox,
  getMemberMessage,
  markMemberMessageRead,
} from '@/api/memberMessageApi.js'
import { getOrder } from '@/api/order.js'
import { getImageUrl } from '@/utils/imageUrl.js'

const tabs = [
  { key: 'SYSTEM_INBOX', label: '系統通知' }, //msg-系統通知//
  { key: 'ORDER_INBOX', label: '訂單通知' }, //msg-訂單通知//
  { key: 'SELLER_INBOX', label: '賣家通知' }, //msg-賣家通知//
]
const route = useRoute()
const inboxes = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, []])))
const loadedTabs = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, false])))
const loadingTabs = reactive(Object.fromEntries(tabs.map((tab) => [tab.key, false])))
const activeTab = ref('SYSTEM_INBOX')
const statusFilter = ref('ALL')
const pageSize = 12
const currentPage = ref(1)
const selectedIds = ref(new Set())
const selectedMessage = ref(null)
const selectedOrder = ref(null)
const selectedOrderItems = ref([])
const selectedOrderItemsLoading = ref(false)
const selectedOrderItemsError = ref('')
const actionPending = ref(false)
const errorMessage = ref('')
const loading = computed(() => loadingTabs[activeTab.value])
const unreadCounts = computed(() => Object.fromEntries(tabs.map((tab) => [
  tab.key,
  inboxes[tab.key].filter((message) => message.recordStatus === 'UNREAD').length,
])))
const filteredItems = computed(() => inboxes[activeTab.value].filter((message) => statusFilter.value === 'ALL' || message.recordStatus === statusFilter.value))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / pageSize)))
const visibleMessages = computed(() => filteredItems.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))
const pageButtons = computed(() => [1, 2].filter((page) => page <= totalPages.value))
const allVisibleSelected = computed(() => visibleMessages.value.length > 0 && visibleMessages.value.every((message) => selectedIds.value.has(message.recordId)))
const isCancelledMemberMessage = computed(() => selectedMessage.value?.orderStatus === 'CANCELLED'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isCompletedMemberMessage = computed(() => selectedMessage.value?.orderStatus === 'COMPLETED'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isDeliveredMemberMessage = computed(() => selectedMessage.value?.orderStatus === 'DELIVERED'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isShippedMemberMessage = computed(() => selectedMessage.value?.orderStatus === 'SHIPPED'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isPaidMemberMessage = computed(() => selectedMessage.value?.orderStatus === 'PAID'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isCashOnDeliveryCreatedMessage = computed(() => selectedMessage.value?.orderStatus === 'PROCESSING'
  && selectedMessage.value?.msgtoMemberId != null
  && selectedMessage.value?.orderId != null)
const isDetailedOrderMessage = computed(() => isCancelledMemberMessage.value
  || isCompletedMemberMessage.value
  || isDeliveredMemberMessage.value
  || isShippedMemberMessage.value
  || isPaidMemberMessage.value
  || isCashOnDeliveryCreatedMessage.value)
const detailedOrderTitle = computed(() => {
  if (isCompletedMemberMessage.value) return '訂單已完成'
  if (isDeliveredMemberMessage.value) return '訂單已到貨'
  if (isShippedMemberMessage.value) return '訂單已出貨'
  if (isPaidMemberMessage.value) return '訂單付款成功'
  if (isCashOnDeliveryCreatedMessage.value) return '訂單下單成功'
  return '訂單已取消'
})
const cancelledOrderNo = computed(() => {
  if (!isDetailedOrderMessage.value) return ''
  const storedOrderNo = selectedMessage.value.sendTitle?.match(/^訂單(?:已取消|已完成|已到貨|已出貨|付款成功|下單成功)-(.+)$/)?.[1]
  return selectedMessage.value?.orderNo
    || selectedOrder.value?.orderNo
    || storedOrderNo
    || ''
})
const messageContentParts = computed(() => {
  let content = selectedMessage.value?.sendContent ?? ''
  if (isCancelledMemberMessage.value && selectedOrder.value) {
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您今日光臨！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已取消，\n`
      + `   您的訂單編號為 /member/orders/${selectedOrder.value.orderId}，\n`
      + '   取消原因：\n'
      + `       ${selectedOrder.value.cancelReason || '未提供原因'}\n`
      + '   歡迎您來信說明，並再次訂購，您的意見是我們最重要的支持！'
  } else if (isCompletedMemberMessage.value && selectedOrder.value) {
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您的訂購！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已完成，\n`
      + `   您的訂單編號為 /member/orders/${selectedOrder.value.orderId}，\n`
      + '   歡迎您留下評價，感謝您的惠顧！'
  } else if (isDeliveredMemberMessage.value && selectedOrder.value) {
    const orderPath = `/member/orders/${selectedOrder.value.orderId}`
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您的訂購！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已到貨，\n`
      + `   您的訂單編號為 ${orderPath}，\n`
      + `   請於7日內取貨，並於${orderPath}按下\"完成訂單\"，感謝您的惠顧！`
  } else if (isShippedMemberMessage.value && selectedOrder.value) {
    const orderPath = `/member/orders/${selectedOrder.value.orderId}`
    const shipment = selectedOrder.value.shipment
    const trackingNo = shipment?.trackingNo || '尚未提供'
    const estimatedDays = shipment?.shippedAt && shipment?.availablePickupAt
      ? `${Math.max(1, Math.ceil((Date.parse(shipment.availablePickupAt) - Date.parse(shipment.shippedAt)) / 86400000))}日`
      : '物流商通知期限'
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您的訂購！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已出貨，\n`
      + `   您的訂單編號為 ${orderPath}，\n`
      + `   隨時點此查詢進度：${orderPath}，\n`
      + `   物流單號為 ${trackingNo}，預計於 ${estimatedDays} 內送達，\n`
      + '   物流追蹤：尚未提供\n'
      + '   請於包裹送達後，7日內取貨，感謝您的惠顧！'
  } else if (isPaidMemberMessage.value && selectedOrder.value) {
    const orderPath = `/member/orders/${selectedOrder.value.orderId}`
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您的訂購！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已完成下單，\n`
      + '   我們已收到您的信用卡款項，請核對為本人付款，\n'
      + `   您的訂單編號為 ${orderPath}，\n`
      + `   隨時點此查詢進度：${orderPath}，\n`
      + '   請於貨物送達後，7日內取貨，感謝您的惠顧！'
  } else if (isCashOnDeliveryCreatedMessage.value && selectedOrder.value) {
    const orderPath = `/member/orders/${selectedOrder.value.orderId}`
    content = `親愛的 會員-${selectedMessage.value.msgtoMemberId} 您好:\n`
      + `   感謝您的訂購！您於 ${formatTemplateDate(selectedOrder.value.createdAt)} 下單之商品已完成下單，\n`
      + `   我們已收到您的訂單，請於到貨後現金付款新台幣共${formatAmount(selectedOrder.value.totalAmount)}元，\n`
      + `   您的訂單編號為 ${orderPath}，\n`
      + `   隨時點此查詢進度：${orderPath}，\n`
      + '   請於貨物送達後，7日內取貨，感謝您的惠顧！'
  }
  const orderId = selectedMessage.value?.orderId
  const orderPath = orderId == null ? '' : `/member/orders/${orderId}`
  if (!orderPath || !content.includes(orderPath)) return [{ type: 'text', value: content }]
  const textParts = content.split(orderPath)
  const linkLabels = isDeliveredMemberMessage.value
    ? [cancelledOrderNo.value || '查看訂單詳情', '我的訂單-查看訂單-訂單詳情']
    : isShippedMemberMessage.value || isPaidMemberMessage.value || isCashOnDeliveryCreatedMessage.value
      ? [cancelledOrderNo.value || '查看訂單詳情', cancelledOrderNo.value || '查看訂單詳情']
    : [isDetailedOrderMessage.value ? cancelledOrderNo.value || '查看訂單詳情' : '查看訂單詳情']
  return textParts.flatMap((text, index) => {
    const result = [{ type: 'text', value: text }]
    if (index < textParts.length - 1) {
      result.push({
        type: 'link',
        value: linkLabels[index] || '查看訂單詳情',
        to: { name: 'MemberOrderDetail', params: { id: orderId } },
      })
    }
    return result
  })
})

function sortNewestFirst(items) {
  return items.sort((left, right) => {
    const timeDifference = Date.parse(right.recordCreatedAt) - Date.parse(left.recordCreatedAt)
    return timeDifference || right.recordId - left.recordId
  })
}

async function loadInbox(category, force = false) {
  if ((loadedTabs[category] || loadingTabs[category]) && !force) return
  loadingTabs[category] = true
  errorMessage.value = ''
  try {
    const firstResponse = await getMemberInbox(category, 0)
    const firstPage = firstResponse.data
    const remainingResponses = await Promise.all(
      Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
        getMemberInbox(category, index + 1),
      ),
    )
    inboxes[category] = sortNewestFirst([
      ...(firstPage.items ?? []),
      ...remainingResponses.flatMap((response) => response.data.items ?? []),
    ])
    loadedTabs[category] = true
  } catch (error) {
    if (category === activeTab.value) {
      errorMessage.value = error.response?.data?.message || '會員收件匣載入失敗，請稍後再試。'
    }
  } finally {
    loadingTabs[category] = false
  }
}

function loadRemainingTabs(category) {
  void Promise.allSettled(
    tabs.filter((tab) => tab.key !== category).map((tab) => loadInbox(tab.key)),
  )
}

async function selectTab(key) {
  activeTab.value = key
  currentPage.value = 1
  selectedIds.value = new Set()
  await loadInbox(key)
}
function toggleMessage(id) { const next = new Set(selectedIds.value); next.has(id) ? next.delete(id) : next.add(id); selectedIds.value = next }
function toggleAllVisible() { const next = new Set(selectedIds.value); visibleMessages.value.forEach((message) => allVisibleSelected.value ? next.delete(message.recordId) : next.add(message.recordId)); selectedIds.value = next }
async function deleteSelected() {
  const ids = [...selectedIds.value]
  if (!ids.length || actionPending.value) return
  actionPending.value = true
  errorMessage.value = ''
  const results = await Promise.allSettled(ids.map((id) => deleteMemberMessage(id)))
  const deletedIds = new Set(ids.filter((id, index) => results[index].status === 'fulfilled'))
  inboxes[activeTab.value] = inboxes[activeTab.value].filter((message) => !deletedIds.has(message.recordId))
  selectedIds.value = new Set(ids.filter((id) => !deletedIds.has(id)))
  currentPage.value = Math.min(currentPage.value, totalPages.value)
  if (deletedIds.size !== ids.length) errorMessage.value = '部分訊息刪除失敗，請稍後再試。'
  if (deletedIds.size > 0) announceMemberUnreadChanged()
  actionPending.value = false
}
async function markAllFilteredRead() {
  const unreadMessages = filteredItems.value.filter((message) => message.recordStatus === 'UNREAD')
  if (!unreadMessages.length || actionPending.value) return
  actionPending.value = true
  errorMessage.value = ''
  const results = await Promise.allSettled(unreadMessages.map((message) => markMemberMessageRead(message.recordId)))
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') unreadMessages[index].recordStatus = 'READ'
  })
  if (results.some((result) => result.status === 'rejected')) errorMessage.value = '部分訊息設為已讀失敗，請稍後再試。'
  if (results.some((result) => result.status === 'fulfilled')) announceMemberUnreadChanged()
  currentPage.value = 1
  actionPending.value = false
}
async function openMessage(message) {
  selectedMessage.value = message
  selectedOrder.value = null
  selectedOrderItems.value = []
  selectedOrderItemsError.value = ''
  document.body.style.overflow = 'hidden'
  try {
    const wasUnread = message.recordStatus === 'UNREAD'
    const response = wasUnread
      ? await markMemberMessageRead(message.recordId)
      : await getMemberMessage(message.recordId)
    message.recordStatus = 'READ'
    selectedMessage.value = { ...message, ...response.data }
    if (wasUnread) announceMemberUnreadChanged()
    if (isDetailedOrderMessage.value) {
      selectedOrderItemsLoading.value = true
      try {
        const orderResponse = await getOrder(selectedMessage.value.orderId)
        selectedOrder.value = orderResponse.data
        selectedOrderItems.value = orderResponse.data.items ?? []
      } catch {
        selectedOrderItemsError.value = '商品明細暫時無法載入。'
      } finally {
        selectedOrderItemsLoading.value = false
      }
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '訊息內容載入失敗，請稍後再試。'
  }
}
function closeMessage() { selectedMessage.value = null; document.body.style.overflow = '' }
function senderLabel(message) {
  return message.msgFunction?.startsWith('SC')
    ? message.storeName || '商家訊息'
    : '系統自動訊息'
}
function goToPage(page) { currentPage.value = Math.min(Math.max(1, page), totalPages.value); selectedIds.value = new Set(); requestAnimationFrame(() => document.querySelector('.inbox-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })) }
function formatDate(value) { return new Intl.DateTimeFormat('zh-TW', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value)) }
function formatTemplateDate(value) {
  const date = new Date(value)
  const parts = new Intl.DateTimeFormat('zh-TW', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).formatToParts(date)
  const part = (type) => parts.find((item) => item.type === type)?.value ?? ''
  return `${part('year')}/${part('month')}/${part('day')} ${part('hour')}:${part('minute')}`
}
function formatCurrency(value) { return new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD', maximumFractionDigits: 0 }).format(value ?? 0) }
function formatAmount(value) { return new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(value ?? 0) }
function formatItemTotal(item) { return formatCurrency(Number(item?.unitPrice ?? 0) * Number(item?.quantity ?? 0)) }
function messagePreview(message) {
  const orderPathPattern = /\/member\/orders?\/\d+/g
  let preview = message.sendContent ?? ''
  if (message.orderNo) {
    preview = preview.replace(orderPathPattern, message.orderNo)
  } else {
    preview = preview
      .replace(orderPathPattern, '')
      .replace(/您的訂單編號為\s*[「\"]?\s*[」\"]?，?/g, '')
      .replace(/隨時點此查詢進度：\s*，?/g, '')
      .replace(/追蹤進度連結：\s*，?/g, '')
  }
  return preview
    .replace(/\s+/g, ' ')
    .trim()
}
watch(statusFilter, () => { currentPage.value = 1; selectedIds.value = new Set() })
watch(
  () => [route.query.recordId, route.query.category],
  async ([recordIdQuery, categoryQuery]) => {
    const category = tabs.some((tab) => tab.key === categoryQuery)
      ? categoryQuery
      : activeTab.value
    activeTab.value = category
    currentPage.value = 1
    selectedIds.value = new Set()
    await loadInbox(category)
    loadRemainingTabs(category)

    const recordId = Number(recordIdQuery)
    if (!Number.isInteger(recordId) || recordId <= 0) return
    const message = inboxes[category].find((item) => item.recordId === recordId)
    if (message) await openMessage(message)
  },
  { immediate: true },
)
onBeforeUnmount(() => { document.body.style.overflow = '' })
</script>

<template>
  <main class="member-inbox-page container">
    <header class="inbox-heading"><p>會員中心 · 訊息</p><h1>會員收件匣</h1></header>
    <nav class="inbox-tabs" role="tablist" aria-label="收件匣分類">
      <button v-for="tab in tabs" :key="tab.key" type="button" role="tab" :aria-selected="activeTab === tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)"><span>{{ tab.label }}</span><span class="inbox-tab-count" :aria-label="`${unreadCounts[tab.key]} 則未讀`">({{ unreadCounts[tab.key] }})</span></button>
    </nav>
    <section class="inbox-card">
      <div class="inbox-toolbar">
        <label><input type="checkbox" :checked="allVisibleSelected" :disabled="loading || !visibleMessages.length" @change="toggleAllVisible" />全選目前結果</label>
        <label class="status-filter"><span class="visually-hidden">訊息讀取狀態</span><select v-model="statusFilter"><option value="ALL">全部訊息</option><option value="UNREAD">未讀取</option><option value="READ">已讀取</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
        <button type="button" class="read-all-button" :disabled="actionPending || !filteredItems.some((message) => message.recordStatus === 'UNREAD')" @click="markAllFilteredRead">設為已讀</button>
        <button type="button" class="delete-button" :disabled="actionPending || selectedIds.size === 0" @click="deleteSelected">刪除已選({{ selectedIds.size }})</button>
      </div>
      <p v-if="errorMessage" class="inbox-error" role="alert">{{ errorMessage }}</p>
      <div class="message-list">
        <div v-if="loading" class="inbox-state">訊息載入中…</div>
        <div v-else-if="visibleMessages.length === 0" class="inbox-state">目前沒有符合篩選條件的訊息。</div>
        <article v-for="message in visibleMessages" :key="message.recordId" class="message-row" :class="{ 'message-row--read': message.recordStatus === 'READ' }">
          <label class="message-check"><input type="checkbox" :checked="selectedIds.has(message.recordId)" :aria-label="`選取 ${message.sendTitle}`" @change="toggleMessage(message.recordId)" /></label>
          <button type="button" class="message-open" @click="openMessage(message)"><span class="message-dot" :class="{ read: message.recordStatus === 'READ' }"></span><span class="message-copy"><strong>{{ message.sendTitle }}</strong><small>{{ messagePreview(message) }}</small></span><time :datetime="message.recordCreatedAt">{{ formatDate(message.recordCreatedAt) }}</time></button>
        </article>
      </div>
      <nav v-if="!loading" class="inbox-pagination" aria-label="會員收件匣頁籤">
        <button type="button" :disabled="currentPage === 1" @click="goToPage(1)">&lt;&lt;</button><button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button>
        <button v-for="page in pageButtons" :key="page" type="button" :class="{ active: currentPage === page }" @click="goToPage(page)">{{ page }}</button><span v-if="totalPages > 2" class="pagination-ellipsis">…</span><button v-if="totalPages > 2" type="button" :class="{ active: currentPage === totalPages }" @click="goToPage(totalPages)">{{ totalPages }}</button>
        <button type="button" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">&gt;</button><button type="button" :disabled="currentPage === totalPages" @click="goToPage(totalPages)">&gt;&gt;</button>
      </nav>
    </section>
    <div v-if="selectedMessage" class="message-overlay" @click.self="closeMessage">
      <article class="message-dialog" role="dialog" aria-modal="true" aria-labelledby="preview-message-detail-title">
        <button type="button" class="message-dialog__close" aria-label="關閉詳細訊息" @click="closeMessage">×</button>
        <header><div><p>from：{{ senderLabel(selectedMessage) }}</p><h2 id="preview-message-detail-title"><template v-if="cancelledOrderNo"><span>{{ detailedOrderTitle }}</span><RouterLink :to="{ name: 'MemberOrderDetail', params: { id: selectedMessage.orderId } }" class="message-dialog__title-link" @click="closeMessage">{{ cancelledOrderNo }}</RouterLink></template><template v-else>{{ selectedMessage.sendTitle }}</template></h2><time :datetime="selectedMessage.recordCreatedAt">{{ formatDate(selectedMessage.recordCreatedAt) }}</time></div></header>
        <div class="message-dialog__content"><template v-for="(part, index) in messageContentParts" :key="index"><RouterLink v-if="part.type === 'link'" :to="part.to" class="message-dialog__order-link" @click="closeMessage">{{ part.value }}</RouterLink><span v-else>{{ part.value }}</span></template></div>
        <div v-if="selectedOrderItemsLoading" class="message-dialog__items-state">商品明細載入中…</div>
        <p v-else-if="selectedOrderItemsError" class="message-dialog__items-state">{{ selectedOrderItemsError }}</p>
        <div v-else-if="selectedOrderItems.length" class="message-dialog__items" :aria-label="`${detailedOrderTitle}商品明細`">
          <article v-for="item in selectedOrderItems" :key="item.orderItemId" class="message-dialog__item">
            <div class="message-dialog__item-image"><img v-if="item.productImageUrl" :src="getImageUrl(item.productImageUrl)" :alt="item.productName" /><i v-else class="bi bi-image" aria-hidden="true"></i></div>
            <div class="message-dialog__item-copy"><strong>{{ item.productName }}</strong><span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span></div>
            <strong class="message-dialog__item-price">{{ formatItemTotal(item) }}</strong>
          </article>
        </div>
      </article>
    </div>
  </main>
</template>

<style scoped>
.member-inbox-page { --bs-gutter-x: var(--space-6); max-width: 1232px; padding-block: 40px; }.inbox-heading p, .inbox-heading h1 { margin: 0; }.inbox-heading p { color: var(--color-primary-active); font-size: var(--font-size-sm); font-weight: 700; }.inbox-heading h1 { margin-top: var(--space-1); color: var(--color-text); font-family: var(--font-body); font-size: var(--font-size-xl); font-weight: 700; line-height: var(--line-height-heading); }
.inbox-tabs { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: var(--space-5); }.inbox-tabs button { position: relative; min-height: var(--space-7); padding-inline: var(--space-5); color: var(--color-text-muted); font: inherit; font-weight: 600; background: transparent; border: 0; border-bottom: var(--space-1) solid transparent; border-radius: var(--radius-md) var(--radius-md) 0 0; }.inbox-tabs button + button::before { position: absolute; bottom: 0; left: 0; width: 1px; height: 66.6667%; content: ''; background: var(--color-border-strong); }.inbox-tabs button:hover { color: var(--color-primary-active); background: var(--color-primary-soft); }.inbox-tabs button.active { color: var(--color-primary-active); background: var(--color-primary-soft); border-bottom-color: var(--color-primary); }.inbox-tab-count { color: var(--color-primary-active); font-variant-numeric: tabular-nums; }
.inbox-card { overflow: hidden; margin-top: var(--space-4); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }.inbox-toolbar { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3) var(--space-4); background: var(--color-surface-soft); border-bottom: 1px solid var(--color-border); }.inbox-toolbar label { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--font-size-sm); }.page-size { margin-left: auto; }.inbox-toolbar select, .delete-button { width: 112px; height: 32px; min-height: 32px; font: inherit; border-radius: var(--radius-md); }.inbox-toolbar select { padding-inline: var(--space-3); background: var(--color-surface); border: 1px solid var(--color-border-strong); }.delete-button { padding-inline: var(--space-1); color: var(--color-danger); white-space: nowrap; background: var(--color-surface); border: 1px solid var(--color-danger); }.delete-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); }
.status-filter { position: relative; width: 112px; height: 32px; }.status-filter select { appearance: none; padding-right: 38px; color: var(--color-text); background: var(--color-surface); }.status-filter > i { position: absolute; top: 1px; right: 1px; display: grid; width: 30px; height: 30px; place-items: center; color: var(--color-primary-700); font-size: 11px; background: var(--color-primary-soft); border-left: 1px solid var(--color-border); border-radius: 0 var(--radius-md) var(--radius-md) 0; pointer-events: none; }.status-filter:hover > i, .status-filter:focus-within > i { color: var(--color-primary-active); background: var(--color-primary-200); }
.message-list { min-height: 420px; }.message-row { display: grid; height: var(--inbox-message-row-height); overflow: hidden; grid-template-columns: var(--space-7) minmax(0, 1fr); border-bottom: 1px solid var(--color-border); }.message-check { display: grid; place-items: center; }.message-open { display: grid; width: 100%; min-width: 0; grid-template-columns: var(--space-3) minmax(0, 1fr); align-items: center; gap: var(--space-2); padding: var(--space-1) var(--space-4); color: var(--color-text); text-align: left; background: transparent; border: 0; }.message-row:hover { background: var(--color-primary-soft); }.message-dot { width: var(--space-2); height: var(--space-2); background: var(--color-primary); border-radius: var(--radius-pill); }.message-dot.read { background: var(--color-disabled); }.message-copy { display: grid; min-width: 0; gap: 0; }.message-copy strong, .message-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.message-copy small, .message-dialog time { color: var(--color-text-muted); font-size: var(--font-size-xs); }.message-row--read .message-open, .message-row--read .message-copy strong, .message-row--read .message-copy small, .message-row--read .message-open time { color: var(--color-text-subtle); }.inbox-state { display: grid; min-height: var(--inbox-message-row-height); place-items: center; color: var(--color-text-muted); }
.message-overlay { position: fixed; z-index: 1050; inset: 0; display: grid; overflow-y: auto; place-items: center; padding: var(--space-5); background: color-mix(in srgb, var(--color-text) 65%, transparent); }.message-dialog { position: relative; width: min(100%, 720px); max-height: calc(100vh - (2 * var(--space-5))); overflow-y: auto; overscroll-behavior: contain; padding: var(--space-6); background: var(--color-surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }.message-dialog__close { position: absolute; top: var(--space-3); right: var(--space-3); width: var(--space-7); height: var(--space-7); color: var(--color-text-muted); font-size: var(--font-size-xl); background: transparent; border: 0; }.message-dialog header { padding-right: var(--space-7); }.message-dialog header p, .message-dialog h2 { margin: 0; }.message-dialog h2 { margin-block: var(--space-1); }.message-dialog__content { overflow-wrap: anywhere; margin-top: var(--space-5); padding-top: var(--space-5); white-space: pre-wrap; border-top: 1px solid var(--color-border); }
.message-dialog__order-link { color: var(--color-primary-active); text-decoration: none; }.message-dialog__order-link:hover { font-weight: 700; }
.message-dialog h2:has(.message-dialog__title-link) { display: grid; }.message-dialog__title-link { width: fit-content; color: var(--color-primary-active); font-size: 15px; font-weight: 400; text-decoration: none; }.message-dialog__title-link:hover { font-weight: 700; }
.message-dialog__items { display: grid; gap: var(--space-3); margin-top: var(--space-5); }.message-dialog__item { display: grid; grid-template-columns: var(--space-8) minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); padding: var(--space-3); background: var(--color-surface-soft); border: 1px solid var(--color-border); border-radius: var(--radius-md); }.message-dialog__item-image { display: grid; width: var(--space-8); height: var(--space-8); overflow: hidden; place-items: center; color: var(--color-text-muted); background: var(--color-surface); border-radius: var(--radius-md); }.message-dialog__item-image img { width: 100%; height: 100%; object-fit: cover; }.message-dialog__item-copy { display: grid; min-width: 0; gap: var(--space-1); }.message-dialog__item-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.message-dialog__item-copy span, .message-dialog__items-state { color: var(--color-text-muted); font-size: var(--font-size-sm); }.message-dialog__item-price { color: var(--color-text); white-space: nowrap; }.message-dialog__items-state { margin-top: var(--space-4); }
.inbox-pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-2); padding: var(--space-3); border-top: 1px solid var(--color-border); }.inbox-pagination button { min-width: calc(var(--space-6) + var(--space-1)); min-height: calc(var(--space-6) + var(--space-1)); color: var(--color-text-muted); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); }.inbox-pagination button.active { color: var(--color-surface); background: var(--color-primary); border-color: var(--color-primary); }.pagination-ellipsis { color: var(--color-text-muted); }
.inbox-card { --inbox-message-row-height: 65px; min-height: 0; margin-top: 0; padding-bottom: calc(3 * var(--inbox-message-row-height)); border-top: 0; border-radius: 0 0 var(--radius-lg) var(--radius-lg); scroll-margin-top: var(--space-5); }
.message-list { min-height: 0; }
.inbox-error { margin: 0; padding: var(--space-2) var(--space-4); color: var(--color-danger); font-size: var(--font-size-sm); background: var(--color-danger-soft); border-bottom: 1px solid var(--color-danger); }
button:focus-visible, select:focus-visible, input:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 767.98px) { .inbox-toolbar { align-items: stretch; flex-wrap: wrap; }.delete-button { margin-left: auto; }.inbox-pagination { justify-content: flex-start; overflow-x: auto; } }
@media (max-width: 575.98px) { .member-inbox-page { padding-block: var(--space-6); } }
.message-open { grid-template-columns: var(--space-3) minmax(0, 1fr) auto; }.message-open time { overflow: hidden; color: var(--color-text-muted); font-size: var(--font-size-xs); text-overflow: ellipsis; white-space: nowrap; }.status-filter { margin-left: auto; }.read-all-button { width: 112px; height: 32px; min-height: 32px; padding-inline: var(--space-1); color: var(--color-primary-active); white-space: nowrap; background: var(--color-surface); border: 1px solid var(--color-primary); border-radius:var(--radius-md); }.read-all-button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); }
</style>
