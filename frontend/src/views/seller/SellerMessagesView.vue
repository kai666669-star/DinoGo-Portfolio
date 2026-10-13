<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createSellerMessage,
  createSellerTemplate,
  deleteSellerOutboxMessage,
  deleteSellerTemplate,
  deleteSellerInboxMessage,
  getSellerInbox,
  getSellerInboxMessage,
  getSellerOutbox,
  getSellerTemplates,
  getSellerUnreadCounts,
  markSellerInboxMessageRead,
  uploadSellerMessageImages,
  updateSellerTemplate,
} from '@/api/sellerMessageApi.js'
import { getSellerOrder, getSellerOrders } from '@/api/sellerOrderApi'
import { getSellerProfile } from '@/api/sellerProfileApi.js'
import { getImageUrl } from '@/utils/imageUrl.js'
const inboxTabs = [
  { key: 'ALL', label: '全部訊息' },
  { key: 'SYSTEM_NOTICE', label: '平台公告' },
  { key: 'NEW_ORDER', label: '訂單進度' },
  { key: 'CANCELLED_ORDER', label: '取消訂單' },
]
const outboxTabs = [
  { key: 'TEMPLATES', label: '範本管理' },
  { key: 'CREATE', label: '新增訊息' },
  { key: 'OUTBOX', label: '寄件備份' },
]
const sentBackup = reactive([])
const categoryTabs = inboxTabs.slice(1)
const messages = reactive([])
const activeTab = ref('ALL'),
  statusFilter = ref('ALL'),
  pageSize = 20,
  currentPage = ref(1),
  selectedIds = ref(new Set())
const inboxLoading = ref(false)
const inboxActionPending = ref(false)
const inboxError = ref('')
const sellerUnreadCounts = reactive({ ALL: 0, SYSTEM_NOTICE: 0, NEW_ORDER: 0, CANCELLED_ORDER: 0 })
let unreadCountsTimer = null
let outboxCountTimer = null
const createForm = reactive({
  saveAsTemplate: false,
  orderId: '',
  templateId: '',
  msgLabel: '',
  sendTitle: '',
  sendContent: '',
  sendRemark: '',
  images: [],
})
const createOrders = reactive([])
const createTemplates = reactive([])
const createDataLoading = ref(false)
const createSubmitting = ref(false)
const templateActionPending = ref(false)
const templateEditorError = ref('')
const selectedTemplateIds = ref(new Set()),
  templateDetail = ref(null),
  templateEditor = reactive({
    open: false,
    sendId: null,
    msgLabel: '',
    sendTitle: '',
    sendContent: '',
    sendRemark: '',
    images: [],
  })
const selectedOutboxIds = ref(new Set()),
  outboxDetail = ref(null)
const inboxDetail = ref(null)
const inboxDetailOrder = ref(null)
const inboxDetailStoreName = ref('')
const inboxDetailOrderLoading = ref(false)
const outboxPageSize = 20
const outboxCurrentPage = ref(1)
const outboxLoading = ref(false)
const outboxActionPending = ref(false)
const outboxError = ref('')
const outboxTotalCount = ref(0)
let outboxRefreshPending = false
const createNotice = ref('')
const canFilter = computed(() => categoryTabs.some((tab) => tab.key === activeTab.value))
const sourceMessages = computed(() =>
  messages
    .filter((item) => activeTab.value === 'ALL' || item.category === activeTab.value)
    .sort((a, b) => new Date(b.recordCreatedAt) - new Date(a.recordCreatedAt)),
)
const filteredMessages = computed(() =>
  !canFilter.value || statusFilter.value === 'ALL'
    ? sourceMessages.value
    : sourceMessages.value.filter((item) => item.recordStatus === statusFilter.value),
)
const pageCount = computed(() => Math.max(1, Math.ceil(filteredMessages.value.length / pageSize)))
const visibleMessages = computed(() =>
  filteredMessages.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize),
)
const pageButtons = computed(() => [1, 2].filter((page) => page <= pageCount.value))
const allSelected = computed(
  () =>
    visibleMessages.value.length > 0 &&
    visibleMessages.value.every((item) => selectedIds.value.has(item.recordId)),
)
const allTemplatesSelected = computed(
  () =>
    createTemplates.length > 0 &&
    createTemplates.every((item) => selectedTemplateIds.value.has(item.sendId)),
)
const outboxPageCount = computed(() => Math.max(1, Math.ceil(sentBackup.length / outboxPageSize)))
const visibleSentBackup = computed(() =>
  sentBackup.slice(
    (outboxCurrentPage.value - 1) * outboxPageSize,
    outboxCurrentPage.value * outboxPageSize,
  ),
)
const outboxPageButtons = computed(() => [1, 2].filter((page) => page <= outboxPageCount.value))
const allOutboxSelected = computed(
  () =>
    visibleSentBackup.value.length > 0 &&
    visibleSentBackup.value.every((item) => selectedOutboxIds.value.has(item.sendId)),
)
const selectedUnreadMessages = computed(() =>
  messages.filter(
    (item) => selectedIds.value.has(item.recordId) && item.recordStatus === 'UNREAD',
  ),
)
function unreadCount(category) {
  return sellerUnreadCounts[category] ?? 0
}
async function refreshSellerUnreadCounts() {
  try {
    const { data } = await getSellerUnreadCounts()
    Object.assign(sellerUnreadCounts, {
      ALL: data.all ?? 0,
      SYSTEM_NOTICE: data.systemNotice ?? 0,
      NEW_ORDER: data.newOrder ?? 0,
      CANCELLED_ORDER: data.cancelledOrder ?? 0,
    })
  } catch {
    // 背景輪詢失敗時保留上一次成功數值，避免徽章閃爍。
  }
}
function outboxTabLabel(tab) {
  if (tab.key === 'TEMPLATES') return `${tab.label}(${createTemplates.length})`
  if (tab.key === 'OUTBOX') return `${tab.label}(${outboxTotalCount.value > 999 ? '999+' : outboxTotalCount.value})`
  return tab.label
}
async function loadSellerInbox() {
  inboxLoading.value = true
  inboxError.value = ''
  try {
    const categoryResults = await Promise.all(categoryTabs.map(async (category) => {
      const firstResponse = await getSellerInbox(category.key, 0)
      const firstPage = firstResponse.data
      const remaining = await Promise.all(
        Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
          getSellerInbox(category.key, index + 1),
        ),
      )
      return [
        ...(firstPage.items ?? []),
        ...remaining.flatMap((response) => response.data.items ?? []),
      ].map((message) => ({ ...message, category: category.key }))
    }))
    messages.splice(0, messages.length, ...categoryResults.flat())
  } catch (error) {
    inboxError.value = error.response?.data?.message || '商家收件匣載入失敗，請稍後再試。'
  } finally {
    inboxLoading.value = false
  }
}
function selectTab(key) {
  activeTab.value = key
  statusFilter.value = 'ALL'
  resetPage()
  if (key === 'OUTBOX') void loadSellerOutbox()
}
function resetPage() {
  currentPage.value = 1
  selectedIds.value = new Set()
}
function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), pageCount.value)
  selectedIds.value = new Set()
  requestAnimationFrame(() =>
    document
      .querySelector('.message-panel')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
  )
}
function toggleOne(id) {
  const next = new Set(selectedIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedIds.value = next
}
function toggleAll() {
  const next = new Set(selectedIds.value)
  visibleMessages.value.forEach((item) =>
    allSelected.value ? next.delete(item.recordId) : next.add(item.recordId),
  )
  selectedIds.value = next
}
async function deleteSelected() {
  const ids = messages
    .filter((item) => selectedIds.value.has(item.recordId))
    .map((item) => item.recordId)
  if (!ids.length || inboxActionPending.value || !window.confirm('是否確認刪除訊息')) return
  inboxActionPending.value = true
  const results = await Promise.allSettled(ids.map((id) => deleteSellerInboxMessage(id)))
  const deletedIds = new Set(ids.filter((id, index) => results[index].status === 'fulfilled'))
  for (let index = messages.length - 1; index >= 0; index -= 1)
    if (deletedIds.has(messages[index].recordId)) messages.splice(index, 1)
  selectedIds.value = new Set(ids.filter((id) => !deletedIds.has(id)))
  currentPage.value = Math.min(currentPage.value, pageCount.value)
  if (deletedIds.size !== ids.length) inboxError.value = '部分訊息刪除失敗，請稍後再試。'
  inboxActionPending.value = false
}
async function markSelectedRead() {
  const unread = [...selectedUnreadMessages.value]
  if (!unread.length || inboxActionPending.value) return
  inboxActionPending.value = true
  const results = await Promise.allSettled(
    unread.map((item) => markSellerInboxMessageRead(item.recordId)),
  )
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') unread[index].recordStatus = 'READ'
  })
  if (results.some((result) => result.status === 'rejected')) inboxError.value = '部分訊息設為已讀失敗，請稍後再試。'
  inboxActionPending.value = false
}
async function openInboxMessage(message) {
  inboxDetail.value = { ...message }
  inboxDetailOrder.value = null
  inboxDetailStoreName.value = ''
  const requests = [getSellerInboxMessage(message.recordId)]
  if (message.recordStatus === 'UNREAD') requests.push(markSellerInboxMessageRead(message.recordId))
  const [detailResult, readResult] = await Promise.allSettled(requests)
  if (detailResult.status === 'fulfilled') inboxDetail.value = detailResult.value.data
  else inboxError.value = detailResult.reason?.response?.data?.message || '訊息詳情載入失敗，請稍後再試。'
  if (message.recordStatus === 'UNREAD') {
    if (readResult?.status === 'fulfilled') {
      message.recordStatus = 'READ'
      if (inboxDetail.value) inboxDetail.value.recordStatus = 'READ'
    } else inboxError.value = readResult?.reason?.response?.data?.message || '訊息設為已讀失敗，請稍後再試。'
  }
  if (
    ['CANCELLED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(
      inboxDetail.value?.orderStatus,
    ) &&
    inboxDetail.value?.orderId
  ) {
    inboxDetailOrderLoading.value = true
    const [orderResult, profileResult] = await Promise.allSettled([
      getSellerOrder(inboxDetail.value.orderId),
      getSellerProfile(),
    ])
    if (orderResult.status === 'fulfilled') inboxDetailOrder.value = orderResult.value.data
    if (profileResult.status === 'fulfilled') inboxDetailStoreName.value = profileResult.value.data.storeName ?? ''
    inboxDetailOrderLoading.value = false
  }
}
function inboxMessageSource(message) {
  return ['OA', 'OS', 'AS'].some((prefix) => message?.msgFunction?.startsWith(prefix))
    ? '系統自動訊息'
    : message?.storeName || '系統自動訊息'
}
function isNewSellerOrderMessage(message) {
  return message?.msgFunction?.startsWith('AS') && ['PAID', 'PROCESSING'].includes(message?.orderStatus)
}
function isSellerProgressMessage(message) {
  return message?.msgFunction?.startsWith('AS') && ['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(message?.orderStatus)
}
function sellerProgressTitle(message, order = null) {
  return {
    SHIPPED: '訂單已出貨',
    DELIVERED: '訂單已送達',
    COMPLETED:
      order?.payment?.paymentMethodCode === 'CASH_ON_DELIVERY' ? '訂單完成與收款' : '訂單已完成',
  }[message?.orderStatus] ?? ''
}
function sellerPaymentLabel(order) {
  return order?.payment?.paymentMethodCode === 'CASH_ON_DELIVERY' ? '貨到付款' : '信用卡付款'
}
function sellerInboxPreview(message) {
  if (!message?.msgFunction?.startsWith('AS') || !message.orderNo) return message?.sendContent ?? ''
  if (message.orderStatus === 'SHIPPED') return `訂單 ${message.orderNo} 已出貨，物流將盡速處理。`
  if (message.orderStatus === 'DELIVERED') return `訂單 ${message.orderNo} 已送達，請於7日內提醒客戶取貨。`
  if (message.totalAmount == null) return message?.sendContent ?? ''
  const amount = formatAmount(message.totalAmount)
  const paymentMethod = message.paymentMethodName?.trim()
  if (['PAID', 'PROCESSING'].includes(message.orderStatus) && paymentMethod) {
    const paymentText = paymentMethod === '信用卡' ? '信用卡付款' : paymentMethod
    return `收到訂單 ${message.orderNo} ，${paymentText}訂單金額總計 $ ${amount}。`
  }
  if (message.orderStatus === 'COMPLETED')
    return `訂單 ${message.orderNo} 已完成，訂單金額總計 $ ${amount}`
  return message?.sendContent ?? ''
}
function formatCurrency(value) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(value ?? 0)
}
function formatAmount(value) {
  return new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(value ?? 0)
}
function formatItemTotal(item) {
  return formatCurrency(Number(item?.unitPrice ?? 0) * Number(item?.quantity ?? 0))
}
function applySelectedTemplate() {
  const template = createTemplates.find((item) => item.sendId === Number(createForm.templateId))
  if (template)
    Object.assign(createForm, {
      msgLabel: template.msgLabel,
      sendTitle: template.sendTitle,
      sendContent: template.sendContent,
      sendRemark: template.sendRemark ?? '',
    })
}
async function selectImages(event) {
  const files = [...(event.target.files ?? [])].slice(0, 3)
  createForm.images = files
  event.target.value = ''
}
function imageFields(assets = []) {
  return assets.slice(0, 3).reduce((fields, asset, index) => {
    const position = ['One', 'Two', 'Three'][index]
    fields[`img${position}`] = asset.secureUrl
    fields[`img${position}PublicId`] = asset.publicId
    return fields
  }, {})
}
async function loadCreateData() {
  createDataLoading.value = true
  try {
    const [ordersResponse, firstTemplateResponse] = await Promise.all([
      getSellerOrders(),
      getSellerTemplates(0),
    ])
    const orders = Array.isArray(ordersResponse.data) ? ordersResponse.data : []
    createOrders.splice(
      0,
      createOrders.length,
      ...orders.filter((order) => !['COMPLETED', 'CANCELLED'].includes(order.status)),
    )
    const firstPage = firstTemplateResponse.data
    const remaining = await Promise.all(
      Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
        getSellerTemplates(index + 1),
      ),
    )
    createTemplates.splice(
      0,
      createTemplates.length,
      ...(firstPage.items ?? []),
      ...remaining.flatMap((response) => response.data.items ?? []),
    )
  } catch (error) {
    createNotice.value = error.response?.data?.message || '訂單或範本載入失敗，請稍後再試。'
  } finally {
    createDataLoading.value = false
  }
}
async function submitNewMessage() {
  if (
    createSubmitting.value ||
    !createForm.sendTitle.trim() ||
    !createForm.sendContent.trim() ||
    !createForm.orderId
  ) return
  createSubmitting.value = true
  createNotice.value = ''
  try {
    const assets = createForm.images.length
      ? (await uploadSellerMessageImages(createForm.images)).data.assets ?? []
      : []
    const sharedPayload = {
      sendTitle: createForm.sendTitle.trim(),
      sendContent: createForm.sendContent.trim(),
      sendRemark: createForm.sendRemark.trim() || null,
      ...imageFields(assets),
    }
    await createSellerMessage({ orderId: Number(createForm.orderId), ...sharedPayload })
    if (createForm.saveAsTemplate) {
      try {
        const response = await createSellerTemplate({
          msgLabel: createForm.msgLabel.trim() || createForm.sendTitle.trim(),
          ...sharedPayload,
        })
        createTemplates.unshift(response.data)
      } catch (error) {
        createNotice.value = '訊息已寄出，但範本儲存失敗，請稍後至範本管理重試。'
        return
      }
    }
    createNotice.value = createForm.saveAsTemplate ? '訊息已寄出，並已儲存為範本。' : '訊息已寄出。'
    Object.assign(createForm, {
      orderId: '', templateId: '', msgLabel: '', sendTitle: '', sendContent: '',
      sendRemark: '', images: [], saveAsTemplate: false,
    })
  } catch (error) {
    createNotice.value = error.response?.data?.message || '訊息寄出失敗，請稍後再試。'
  } finally {
    createSubmitting.value = false
  }
}
function toggleTemplate(id) {
  const next = new Set(selectedTemplateIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedTemplateIds.value = next
}
function toggleAllTemplates() {
  selectedTemplateIds.value = allTemplatesSelected.value
    ? new Set()
    : new Set(createTemplates.map((item) => item.sendId))
}
function openTemplateEditor(item = null) {
  templateEditorError.value = ''
  const images = ['One', 'Two', 'Three']
    .map((position, index) => item?.[`img${position}`] ? {
      name: `已上傳圖片 ${index + 1}`,
      secureUrl: item[`img${position}`],
      publicId: item[`img${position}PublicId`],
    } : null)
    .filter(Boolean)
  Object.assign(templateEditor, {
    open: true,
    sendId: item?.sendId ?? null,
    msgLabel: item?.msgLabel ?? '',
    sendTitle: item?.sendTitle ?? '',
    sendContent: item?.sendContent ?? '',
    sendRemark: item?.sendRemark ?? '',
    images,
  })
}
function openTemplateDetail(item) {
  openTemplateEditor(item)
  templateEditor.open = false
  templateDetail.value = item
}
function selectTemplateImages(event) {
  templateEditor.images = [...(event.target.files ?? [])].slice(0, 3)
  event.target.value = ''
}
async function deleteSelectedTemplates() {
  if (templateActionPending.value) return
  const confirmed = createTemplates.filter(
    (item) => selectedTemplateIds.value.has(item.sendId)
      && window.confirm(`是否確認刪除${item.msgLabel}範本`),
  )
  if (!confirmed.length) return
  templateActionPending.value = true
  const results = await Promise.allSettled(
    confirmed.map((item) => deleteSellerTemplate(item.sendId)),
  )
  const deletedIds = new Set(
    confirmed.filter((item, index) => results[index].status === 'fulfilled').map((item) => item.sendId),
  )
  for (let index = createTemplates.length - 1; index >= 0; index -= 1)
    if (deletedIds.has(createTemplates[index].sendId)) createTemplates.splice(index, 1)
  selectedTemplateIds.value = new Set(
    [...selectedTemplateIds.value].filter((id) => !deletedIds.has(id)),
  )
  templateActionPending.value = false
}
function editTemplate(item) {
  openTemplateEditor(item)
}
async function deleteTemplate(item) {
  if (templateActionPending.value || !window.confirm(`是否確認刪除${item.msgLabel}範本`)) return
  templateActionPending.value = true
  try {
    await deleteSellerTemplate(item.sendId)
    const index = createTemplates.findIndex((value) => value.sendId === item.sendId)
    if (index >= 0) createTemplates.splice(index, 1)
    const next = new Set(selectedTemplateIds.value)
    next.delete(item.sendId)
    selectedTemplateIds.value = next
    if (templateDetail.value?.sendId === item.sendId) templateDetail.value = null
    if (templateEditor.sendId === item.sendId) templateEditor.open = false
  } finally {
    templateActionPending.value = false
  }
}
function editTemplateFromDetail() {
  const item = templateDetail.value
  templateDetail.value = null
  if (item) openTemplateEditor(item)
}
async function deleteTemplateFromDetail() {
  const item = templateDetail.value
  if (item) await deleteTemplate(item)
}
async function saveTemplateEditor() {
  if (templateActionPending.value) return
  const payload = {
    msgLabel: templateEditor.msgLabel.trim(),
    sendTitle: templateEditor.sendTitle.trim(),
    sendContent: templateEditor.sendContent.trim(),
    sendRemark: templateEditor.sendRemark.trim() || null,
  }
  if (!payload.sendTitle || !payload.sendContent) return
  templateActionPending.value = true
  templateEditorError.value = ''
  const item = createTemplates.find((value) => value.sendId === templateEditor.sendId)
  try {
    const retainedAssets = templateEditor.images.filter((image) => image.secureUrl)
    const newFiles = templateEditor.images.filter((image) => !image.secureUrl)
    const uploadedAssets = newFiles.length
      ? (await uploadSellerMessageImages(newFiles)).data.assets ?? []
      : []
    Object.assign(payload, imageFields([...retainedAssets, ...uploadedAssets]))
    const response = item
      ? await updateSellerTemplate(item.sendId, payload)
      : await createSellerTemplate(payload)
    if (item) Object.assign(item, response.data)
    else createTemplates.unshift(response.data)
    templateEditor.open = false
  } catch (error) {
    templateEditorError.value = error.response?.data?.message || '範本儲存失敗，請稍後再試。'
  } finally {
    templateActionPending.value = false
  }
}
function toggleOutbox(id) {
  const next = new Set(selectedOutboxIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedOutboxIds.value = next
}
function toggleAllOutbox() {
  selectedOutboxIds.value = allOutboxSelected.value
    ? new Set()
    : new Set(visibleSentBackup.value.map((item) => item.sendId))
}
async function loadSellerOutbox(firstResponse = null, silent = false) {
  if (!silent) outboxLoading.value = true
  outboxError.value = ''
  try {
    const response = firstResponse ?? await getSellerOutbox(0)
    const firstPage = response.data
    outboxTotalCount.value = firstPage.totalElements ?? 0
    const remaining = await Promise.all(
      Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
        getSellerOutbox(index + 1),
      ),
    )
    const items = [
      ...(firstPage.items ?? []),
      ...remaining.flatMap((response) => response.data.items ?? []),
    ].filter((item) => item.msgFunction?.startsWith('SC'))
    sentBackup.splice(0, sentBackup.length, ...items)
  } catch (error) {
    outboxError.value = error.response?.data?.message || '寄件備份載入失敗，請稍後再試。'
  } finally {
    if (!silent) outboxLoading.value = false
  }
}
async function refreshSellerOutboxCount() {
  if (outboxRefreshPending) return
  outboxRefreshPending = true
  try {
    const response = await getSellerOutbox(0)
    const firstPage = response.data
    const firstIds = (firstPage.items ?? []).map((item) => item.sendId).join(',')
    const currentFirstIds = sentBackup.slice(0, firstPage.size ?? 10).map((item) => item.sendId).join(',')
    const changed = (firstPage.totalElements ?? 0) !== outboxTotalCount.value || firstIds !== currentFirstIds
    outboxTotalCount.value = firstPage.totalElements ?? 0
    if (activeTab.value === 'OUTBOX' && changed) await loadSellerOutbox(response, true)
  } catch {
    // 背景同步失敗時保留上一次成功數值。
  } finally {
    outboxRefreshPending = false
  }
}
async function deleteSelectedOutbox() {
  const ids = [...selectedOutboxIds.value]
  if (!ids.length || outboxActionPending.value || !window.confirm('是否確認刪除寄件備份')) return
  outboxActionPending.value = true
  outboxError.value = ''
  const results = await Promise.allSettled(ids.map((id) => deleteSellerOutboxMessage(id)))
  const deletedIds = new Set(ids.filter((id, index) => results[index].status === 'fulfilled'))
  for (let index = sentBackup.length - 1; index >= 0; index -= 1)
    if (deletedIds.has(sentBackup[index].sendId)) sentBackup.splice(index, 1)
  selectedOutboxIds.value = new Set(ids.filter((id) => !deletedIds.has(id)))
  outboxTotalCount.value = Math.max(0, outboxTotalCount.value - deletedIds.size)
  outboxCurrentPage.value = Math.min(outboxCurrentPage.value, outboxPageCount.value)
  if (deletedIds.size !== ids.length) outboxError.value = '部分寄件備份刪除失敗，請稍後再試。'
  outboxActionPending.value = false
}
function goToOutboxPage(page) {
  outboxCurrentPage.value = Math.min(Math.max(1, page), outboxPageCount.value)
  selectedOutboxIds.value = new Set()
  requestAnimationFrame(() =>
    document.querySelector('.sent-backup')?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
  )
}
function formatTime(value) {
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
onMounted(() => {
  void loadSellerInbox()
  void loadCreateData()
  void loadSellerOutbox()
  void refreshSellerUnreadCounts()
  unreadCountsTimer = window.setInterval(refreshSellerUnreadCounts, 1000)
  outboxCountTimer = window.setInterval(refreshSellerOutboxCount, 1000)
})
onBeforeUnmount(() => {
  if (unreadCountsTimer != null) window.clearInterval(unreadCountsTimer)
  if (outboxCountTimer != null) window.clearInterval(outboxCountTimer)
})
</script>
<template>
  <section class="seller-page">
    <header>
      <p class="eyebrow">訊息管理</p>
      <h1>訊息中心</h1>
      <p>查看平台公告與訂單動態，管理寄件範本。</p>
    </header>
    <div class="message-layout">
      <nav class="category-panel">
        <p class="category-heading">收件匣</p>
        <button
          v-for="tab in inboxTabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="selectTab(tab.key)"
        >
          <span>{{ tab.label }}({{ unreadCount(tab.key) }})</span>
        </button>
        <p class="category-heading outbox-heading">寄件匣</p>
        <button
          v-for="tab in outboxTabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="selectTab(tab.key)"
        >
          {{ outboxTabLabel(tab) }}
        </button>
      </nav>
      <div
        class="message-panel"
        :class="{ 'message-panel--inbox': !['TEMPLATES', 'CREATE', 'OUTBOX'].includes(activeTab) }"
      >
        <section v-if="activeTab === 'TEMPLATES'" class="template-manager">
          <header>
            <h2>範本管理</h2>
            <div class="template-header-actions">
              <button type="button" class="template-create-action" @click="openTemplateEditor()">
                <i class="bi bi-plus-lg" aria-hidden="true"></i>新增範本</button
              ><button
                class="template-batch-delete"
                :disabled="templateActionPending || !selectedTemplateIds.size"
                @click="deleteSelectedTemplates"
              >
                <span aria-hidden="true">×</span>批次刪除
              </button>
            </div>
          </header>
          <div class="template-columns">
            <label
              ><input
                type="checkbox"
                :checked="allTemplatesSelected"
                @change="toggleAllTemplates"
              />全選</label
            ><span>自訂範本名稱</span
            ><span>訊息標題 / 訊息內容</span
            ><span>最後修改時間</span
            ><span aria-hidden="true"></span>
          </div>
          <article v-for="item in createTemplates" :key="item.sendId">
            <label class="template-check" @click.stop
              ><input
                type="checkbox"
                :checked="selectedTemplateIds.has(item.sendId)"
                @change="toggleTemplate(item.sendId)" /></label
            ><span class="template-label">{{ item.msgLabel }}</span
            ><button class="template-summary" @click="openTemplateDetail(item)">
              <strong>{{ item.sendTitle }}</strong
              ><small>{{ item.sendContent }}</small></button
            ><time>{{ item.sendUpdAt ? formatTime(item.sendUpdAt) : '—' }}</time
            ><div class="template-row-actions">
              <button
                class="template-row-action"
                type="button"
                aria-label="修改範本"
                @click="editTemplate(item)"
              >
                <i class="bi bi-pencil" aria-hidden="true"></i><span>修改</span></button
              ><button
                class="template-row-action template-row-delete"
                type="button"
                aria-label="刪除範本"
                @click="deleteTemplate(item)"
              >
                <span aria-hidden="true">×</span><span>刪除</span>
              </button>
            </div>
          </article>
        </section>
        <form v-else-if="activeTab === 'CREATE'" class="create-message-form" @submit.prevent="submitNewMessage">
          <header>
            <h2>新增訊息</h2>
          </header>
          <label class="save-template-check">
            <input v-model="createForm.saveAsTemplate" type="checkbox" />
            寄出訊息並同時儲存為範本
          </label>
          <p v-if="createNotice" class="create-notice">{{ createNotice }}</p>
          <label
            >*選擇訂單（必填）<select v-model="createForm.orderId" required :disabled="createDataLoading">
              <option value="">{{ createDataLoading ? '資料載入中…' : '請選擇未完成且未取消的訂單' }}</option>
              <option v-for="order in createOrders" :key="order.orderId" :value="order.orderId">
                {{ order.orderNo }} · {{ order.status }}
              </option>
            </select></label
          ><label
            >套用現有範本<select
              v-model="createForm.templateId"
              :disabled="createDataLoading || !createTemplates.length"
              @change="applySelectedTemplate"
            >
              <option value="">
                {{ createDataLoading ? '範本載入中…' : createTemplates.length ? '不套用範本' : '目前無可用範本' }}
              </option>
              <option
                v-for="template in createTemplates"
                :key="template.sendId"
                :value="template.sendId"
              >
                {{ template.msgLabel }}
              </option>
            </select></label
          ><label
            >自訂範本名稱<input
              v-model="createForm.msgLabel"
              maxlength="50"
              :disabled="!createForm.saveAsTemplate"
              placeholder="未填則同訊息標題" /></label
          ><label>*訊息標題（必填）<input v-model="createForm.sendTitle" maxlength="100" required /></label
          ><label class="textarea-field"
            >*訊息內容（必填）<textarea
              v-model="createForm.sendContent"
              maxlength="1000"
              rows="8"
              required
            ></textarea
            ><small class="field-counter">{{ createForm.sendContent.length }}/1000</small></label
          ><label class="textarea-field"
            >備註<textarea v-model="createForm.sendRemark" maxlength="1000" rows="3"></textarea
            ><small class="field-counter">{{ createForm.sendRemark.length }}/1000</small></label
          ><label class="image-upload"
            >上傳圖片（至多三張）<input
              type="file"
              accept="image/*"
              multiple
              @change="selectImages"
            /></label
          >
          <ul v-if="createForm.images.length" class="image-file-list">
            <li v-for="image in createForm.images" :key="image.name">{{ image.name }}</li>
          </ul>
          <div class="create-actions">
            <button
              type="submit"
              class="send-template-button"
              :disabled="createSubmitting || createDataLoading || !createOrders.length"
            >
              {{ createSubmitting ? '送出中…' : '送出' }}
            </button>
          </div>
        </form>
        <div v-else-if="activeTab === 'OUTBOX'" class="sent-backup">
          <header>
            <div>
              <h2>寄件備份</h2>
              <p>保留商家已實際寄出的訊息紀錄。</p>
            </div>
            <button class="sent-backup-delete" :disabled="outboxActionPending || !selectedOutboxIds.size" @click="deleteSelectedOutbox">
              刪除已選（{{ selectedOutboxIds.size }}）
            </button>
          </header>
          <p v-if="outboxError" class="inbox-error" role="alert">{{ outboxError }}</p>
          <div class="sent-backup-columns">
            <label
              ><input
                type="checkbox"
                :checked="allOutboxSelected"
                :disabled="!sentBackup.length"
                @change="toggleAllOutbox"
              />全選</label
            ><span>訊息標題 / 訊息內容</span
            ><span>自訂範本名稱</span
            ><span>訂單編號</span
            ><span>寄件時間</span>
          </div>
          <div v-if="outboxLoading" class="feature-state">寄件備份載入中…</div>
          <div v-else-if="!visibleSentBackup.length" class="feature-state">目前沒有寄件備份。</div>
          <template v-else>
            <article v-for="message in visibleSentBackup" :key="message.sendId" class="sent-backup-row">
              <input
                type="checkbox"
                :checked="selectedOutboxIds.has(message.sendId)"
                @change="toggleOutbox(message.sendId)"
              />
              <button @click="outboxDetail = message">
                <strong>{{ message.sendTitle }}</strong
                ><small>{{ message.sendContent }}</small>
              </button>
              <span class="outbox-field">{{ message.msgLabel }}</span>
              <span class="outbox-field">{{ message.orderNo }}</span
              ><time>{{ formatTime(message.sendUpdAt) }}</time>
            </article>
          </template>
          <nav class="pagination sent-backup-pagination" aria-label="寄件備份頁籤">
            <button :disabled="outboxCurrentPage === 1" @click="goToOutboxPage(1)">&lt;&lt;</button
            ><button :disabled="outboxCurrentPage === 1" @click="goToOutboxPage(outboxCurrentPage - 1)">&lt;</button
            ><button
              v-for="page in outboxPageButtons"
              :key="page"
              :class="{ active: outboxCurrentPage === page }"
              @click="goToOutboxPage(page)"
            >
              {{ page }}</button
            ><span v-if="outboxPageCount > 2">…</span
            ><button
              v-if="outboxPageCount > 2"
              :class="{ active: outboxCurrentPage === outboxPageCount }"
              @click="goToOutboxPage(outboxPageCount)"
            >
              {{ outboxPageCount }}</button
            ><button
              :disabled="outboxCurrentPage === outboxPageCount"
              @click="goToOutboxPage(outboxCurrentPage + 1)"
            >
              &gt;</button
            ><button
              :disabled="outboxCurrentPage === outboxPageCount"
              @click="goToOutboxPage(outboxPageCount)"
            >
              &gt;&gt;
            </button>
          </nav>
        </div>
        <template v-else>
          <p v-if="inboxError" class="inbox-error" role="alert">{{ inboxError }}</p>
          <div class="message-toolbar">
            <label><input type="checkbox" :checked="allSelected" :disabled="inboxLoading || inboxActionPending || !visibleMessages.length" @change="toggleAll" />全選</label
            ><label v-if="canFilter" class="status-filter"
              ><select v-model="statusFilter" @change="resetPage">
                <option value="ALL">全部訊息</option>
                <option value="UNREAD">未讀取</option>
                <option value="READ">已讀取</option>
              </select><i class="bi bi-chevron-down" aria-hidden="true"></i></label
            ><button
              class="read-all-button"
              :class="{ 'push-right': !canFilter }"
              :disabled="inboxActionPending || !selectedUnreadMessages.length"
              @click="markSelectedRead"
            >
              設為已讀</button
            ><button class="delete-button" :disabled="inboxActionPending || !selectedIds.size" @click="deleteSelected">
              刪除已選（{{ selectedIds.size }}）
            </button>
          </div>
          <div v-if="inboxLoading" class="feature-state">訊息載入中…</div>
          <div v-else-if="!visibleMessages.length" class="feature-state">目前沒有符合條件的訊息。</div>
          <div v-else>
            <article
              v-for="message in visibleMessages"
              :key="message.recordId"
              class="message-item"
              :class="{ read: message.recordStatus === 'READ' }"
            >
              <label class="message-check"
                ><input
                  type="checkbox"
                  :checked="selectedIds.has(message.recordId)"
                  @change="toggleOne(message.recordId)" /></label
              ><button class="message-row" @click="openInboxMessage(message)">
                <span class="message-copy"
                  ><span class="message-title-line"
                    ><strong>{{ message.sendTitle }}</strong
                    ><span v-if="message.recordStatus === 'UNREAD'" class="status-dot"></span></span
                  ><span>{{ sellerInboxPreview(message) }}</span></span>
              </button>
              <time>{{ formatTime(message.recordCreatedAt) }}</time>
            </article>
          </div>
          <nav v-if="!inboxLoading" class="pagination" aria-label="商家收件匣頁籤">
            <button :disabled="currentPage === 1" @click="goToPage(1)">&lt;&lt;</button
            ><button :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">&lt;</button
            ><button
              v-for="page in pageButtons"
              :key="page"
              :class="{ active: currentPage === page }"
              @click="goToPage(page)"
            >
              {{ page }}</button
            ><span v-if="pageCount > 2">…</span
            ><button
              v-if="pageCount > 2"
              :class="{ active: currentPage === pageCount }"
              @click="goToPage(pageCount)"
            >
              {{ pageCount }}</button
            ><button :disabled="currentPage === pageCount" @click="goToPage(currentPage + 1)">
              &gt;</button
            ><button :disabled="currentPage === pageCount" @click="goToPage(pageCount)">
              &gt;&gt;
            </button>
          </nav></template
        >
      </div>
    </div>
    <div v-if="inboxDetail" class="template-overlay" @click.self="inboxDetail = null">
      <article class="template-dialog inbox-detail-dialog" role="dialog" aria-modal="true">
        <button
          class="template-detail-dialog__close"
          type="button"
          aria-label="關閉訊息詳情"
          @click="inboxDetail = null"
        >
          ×
        </button>
        <header>
          <p>from:{{ inboxMessageSource(inboxDetail) }}</p>
          <h2 v-if="inboxDetail.orderStatus === 'CANCELLED' && inboxDetail.orderId" class="inbox-cancelled-title">
            <span>訂單已取消</span>
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link inbox-order-link--title"
              @click="inboxDetail = null"
            >
              {{ inboxDetail.orderNo || inboxDetailOrder?.orderNo || '查看訂單' }}
            </RouterLink>
          </h2>
          <h2 v-else-if="isNewSellerOrderMessage(inboxDetail)" class="inbox-cancelled-title">
            <span>收到新訂單</span>
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link inbox-order-link--title"
              @click="inboxDetail = null"
            >
              {{ inboxDetail.orderNo || inboxDetailOrder?.orderNo || '查看訂單' }}
            </RouterLink>
          </h2>
          <h2 v-else-if="isSellerProgressMessage(inboxDetail)" class="inbox-cancelled-title">
            <span>{{ sellerProgressTitle(inboxDetail, inboxDetailOrder) }}</span>
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link inbox-order-link--title"
              @click="inboxDetail = null"
            >
              {{ inboxDetail.orderNo || inboxDetailOrder?.orderNo || '查看訂單' }}
            </RouterLink>
          </h2>
          <h2 v-else>{{ inboxDetail.sendTitle }}</h2>
          <time>{{ formatTime(inboxDetail.recordCreatedAt) }}</time>
        </header>
        <div
          v-if="inboxDetail.orderStatus === 'CANCELLED' && inboxDetailOrder"
          class="inbox-detail-dialog__content inbox-cancelled-content"
        >
          <span>親愛的 {{ inboxDetailStoreName || '商家' }} 您好:</span>
          <span class="inbox-message-line--indented">感謝您支持本平台！</span>
          <span class="inbox-message-line--indented">您有一筆訂單已取消，</span>
          <span
            class="inbox-message-line--indented"
            >訂單編號為
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link"
              @click="inboxDetail = null"
              >{{ inboxDetail.orderNo || inboxDetailOrder.orderNo }}</RouterLink
            >，</span
          >
          <span class="inbox-message-line--indented">取消原因：</span>
          <span class="inbox-cancel-reason">{{ inboxDetailOrder.cancelReason || '未提供原因' }}</span>
          <div v-if="inboxDetailOrder.items?.length" class="inbox-order-items inbox-order-items--indented">
            <article v-for="item in inboxDetailOrder.items" :key="item.orderItemId" class="inbox-order-item">
              <div class="inbox-order-item__image">
                <img
                  v-if="item.productImageUrl"
                  :src="getImageUrl(item.productImageUrl)"
                  :alt="item.productName"
                />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>
              <div class="inbox-order-item__copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span>
              </div>
              <strong class="inbox-order-item__total">{{ formatCurrency(inboxDetailOrder.totalAmount) }}</strong>
            </article>
          </div>
        </div>
        <div
          v-else-if="isNewSellerOrderMessage(inboxDetail) && inboxDetailOrder"
          class="inbox-detail-dialog__content inbox-cancelled-content"
        >
          <span>親愛的 {{ inboxDetailStoreName || '商家' }} 您好:</span>
          <span class="inbox-message-line--indented">感謝您支持本平台！</span>
          <span class="inbox-message-line--indented">您有一筆新訂單，</span>
          <span
            class="inbox-message-line--indented"
            >訂單編號為
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link"
              @click="inboxDetail = null"
              >{{ inboxDetail.orderNo || inboxDetailOrder.orderNo }}</RouterLink
            >，</span
          >
          <span class="inbox-message-line--indented">{{ sellerPaymentLabel(inboxDetailOrder) }} 新台幣 $ {{ formatAmount(inboxDetailOrder.totalAmount) }} 元</span>
          <div v-if="inboxDetailOrder.items?.length" class="inbox-order-items inbox-order-items--indented">
            <article v-for="item in inboxDetailOrder.items" :key="item.orderItemId" class="inbox-order-item">
              <div class="inbox-order-item__image">
                <img
                  v-if="item.productImageUrl"
                  :src="getImageUrl(item.productImageUrl)"
                  :alt="item.productName"
                />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>
              <div class="inbox-order-item__copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span>
              </div>
              <strong class="inbox-order-item__total">{{ formatItemTotal(item) }}</strong>
            </article>
          </div>
        </div>
        <div
          v-else-if="isSellerProgressMessage(inboxDetail) && inboxDetailOrder"
          class="inbox-detail-dialog__content inbox-cancelled-content"
        >
          <span>親愛的 {{ inboxDetailStoreName || '商家' }} 您好:</span>
          <span class="inbox-message-line--indented">感謝您支持本平台！</span>
          <span v-if="inboxDetail.orderStatus === 'COMPLETED'"
            class="inbox-message-line--indented"
            >您有一筆訂單已完成，{{ sellerPaymentLabel(inboxDetailOrder) }} 新台幣 $ {{ formatAmount(inboxDetailOrder.totalAmount) }} 元</span
          >
          <span v-else class="inbox-message-line--indented">您有一筆{{ sellerProgressTitle(inboxDetail, inboxDetailOrder) }}，</span>
          <span
            class="inbox-message-line--indented"
            >訂單編號為
            <RouterLink
              :to="{ name: 'SellerOrderDetail', params: { id: inboxDetail.orderId } }"
              class="inbox-order-link"
              @click="inboxDetail = null"
              >{{ inboxDetail.orderNo || inboxDetailOrder.orderNo }}</RouterLink
            ><template v-if="inboxDetail.orderStatus === 'DELIVERED'">，</template></span
          >
          <span v-if="inboxDetail.orderStatus === 'DELIVERED'" class="inbox-message-line--indented">請於7日內提醒客戶取貨，並提醒客戶於「我的訂單-查看訂單-訂單詳情」按下「完成訂單」。</span>
          <div v-if="inboxDetailOrder.items?.length" class="inbox-order-items inbox-order-items--indented">
            <article v-for="item in inboxDetailOrder.items" :key="item.orderItemId" class="inbox-order-item">
              <div class="inbox-order-item__image">
                <img
                  v-if="item.productImageUrl"
                  :src="getImageUrl(item.productImageUrl)"
                  :alt="item.productName"
                />
                <i v-else class="bi bi-image" aria-hidden="true"></i>
              </div>
              <div class="inbox-order-item__copy">
                <strong>{{ item.productName }}</strong>
                <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}</span>
              </div>
              <strong class="inbox-order-item__total">{{ formatItemTotal(item) }}</strong>
            </article>
          </div>
        </div>
        <div v-else class="inbox-detail-dialog__content">
          {{ inboxDetailOrderLoading ? '訂單內容載入中…' : inboxDetail.sendContent }}
        </div>
      </article>
    </div>
    <div v-if="outboxDetail" class="template-overlay" @click.self="outboxDetail = null">
      <article class="template-dialog outbox-dialog" role="dialog" aria-modal="true">
        <button class="outbox-dialog__close" aria-label="關閉寄件詳情" @click="outboxDetail = null">
          ×
        </button>
        <header>
          <p>{{ outboxDetail.msgLabel }}</p>
          <h2>{{ outboxDetail.sendTitle }}</h2>
          <p>訂單 {{ outboxDetail.orderNo }}</p>
          <time>{{ formatTime(outboxDetail.sendUpdAt) }}</time>
        </header>
        <div class="outbox-dialog__content">{{ outboxDetail.sendContent }}</div>
      </article>
    </div>
    <div v-if="templateDetail" class="template-overlay" @click.self="templateDetail = null">
      <article class="template-dialog template-detail-dialog" role="dialog" aria-modal="true">
        <button
          class="template-detail-dialog__close"
          aria-label="關閉範本詳情"
          @click="templateDetail = null"
        >
          ×
        </button>
        <header class="template-detail-fields">
          <div>
            <span>自訂範本名稱</span>
            <p>{{ templateEditor.msgLabel }}</p>
          </div>
          <div>
            <span>訊息標題</span>
            <h2>{{ templateEditor.sendTitle }}</h2>
          </div>
        </header>
        <div class="template-detail-dialog__actions">
          <button type="button" class="template-row-action" @click="editTemplateFromDetail">
            <i class="bi bi-pencil" aria-hidden="true"></i>修改</button
          ><button type="button" class="template-row-action template-row-delete" @click="deleteTemplateFromDetail">
            <span aria-hidden="true">×</span>刪除
          </button>
        </div>
        <label class="textarea-field">訊息內容
          <textarea v-model="templateEditor.sendContent" maxlength="1000" rows="8" readonly></textarea>
          <small class="field-counter">{{ templateEditor.sendContent.length }}/1000</small>
        </label>
        <label class="textarea-field">備註
          <textarea v-model="templateEditor.sendRemark" maxlength="1000" rows="4" readonly></textarea>
          <small class="field-counter">{{ templateEditor.sendRemark.length }}/1000</small>
        </label>
        <label class="image-upload disabled">上傳圖片
          <input type="file" accept="image/*" multiple disabled />
          <small>至多三張</small>
        </label>
        <ul v-if="templateEditor.images.length" class="image-file-list">
          <li v-for="image in templateEditor.images" :key="image.name">{{ image.name }}</li>
        </ul>
      </article>
    </div>
    <div
      v-if="templateEditor.open"
      class="template-overlay"
      @click.self="templateEditor.open = false"
    >
      <form class="template-dialog template-editor-dialog" @submit.prevent="saveTemplateEditor">
        <button class="template-detail-dialog__close" type="button" aria-label="關閉範本編輯" @click="templateEditor.open = false">×</button>
        <h2 class="template-editor-title">{{ templateEditor.sendId ? '修改範本' : '新增範本' }}</h2>
        <label>自訂範本名稱
          <input v-model="templateEditor.msgLabel" maxlength="50" />
        </label>
        <label>{{ templateEditor.sendId ? '訊息標題' : '*訊息標題 (必填)' }}
          <input v-model="templateEditor.sendTitle" maxlength="100" required />
        </label>
        <div v-if="templateEditor.sendId" class="template-editor-actions">
        </div>
        <label class="textarea-field">{{ templateEditor.sendId ? '訊息內容' : '*訊息內容 (必填)' }}
          <textarea v-model="templateEditor.sendContent" maxlength="1000" rows="8" required></textarea>
          <small class="field-counter">{{ templateEditor.sendContent.length }}/1000</small>
        </label>
        <p v-if="templateEditorError" class="template-editor-error" role="alert">{{ templateEditorError }}</p>
        <label class="textarea-field">備註
          <textarea v-model="templateEditor.sendRemark" maxlength="1000" rows="4"></textarea>
          <small class="field-counter">{{ templateEditor.sendRemark.length }}/1000</small>
        </label>
        <label class="image-upload">上傳圖片(至多三張)
          <input type="file" accept="image/*" multiple @change="selectTemplateImages" />
        </label>
        <ul v-if="templateEditor.images.length" class="image-file-list">
          <li v-for="image in templateEditor.images" :key="image.name">{{ image.name }}</li>
        </ul>
        <div class="template-editor-save">
          <button type="submit" class="send-template-button" :disabled="templateActionPending">
            {{ templateActionPending ? '儲存中…' : '儲存' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
  max-width: 1160px;
}
header p,
h1 {
  margin: 0;
}
.eyebrow,
header p:last-child {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
h1 {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}
.message-layout {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: var(--space-5);
  min-height: 460px;
}
.category-panel,
.message-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.category-panel {
  display: grid;
  align-self: start;
  gap: var(--space-1);
  padding: var(--space-3);
}
.category-heading {
  margin: var(--space-1) var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 700;
}
.outbox-heading {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}
.category-panel button {
  position: relative;
  min-height: 44px;
  padding: 0 var(--space-3);
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
}
.category-panel button.active {
  color: var(--color-primary-active);
  font-weight: 700;
  background: var(--color-primary-soft);
}
.message-panel {
  min-width: 0;
  overflow: hidden;
}
.message-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-soft);
  border-bottom: 1px solid var(--color-border);
}
.message-toolbar label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}
.status-filter {
  position: relative;
  margin-left: auto;
}
.message-toolbar select,
.delete-button,
.read-all-button {
  width: 112px;
  min-height: 32px;
  padding-inline: var(--space-2);
  font-size: var(--font-size-sm);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.delete-button {
  color: var(--color-danger);
  background: var(--color-surface);
  border-color: var(--color-danger);
}
.message-item {
  display: grid;
  grid-template-columns: var(--space-7) minmax(0, 1fr);
  border-bottom: 1px solid var(--color-border);
}
.message-check {
  display: grid;
  place-items: center;
}
.message-row {
  display: grid;
  width: 100%;
  min-height: 68px;
  grid-template-columns: var(--space-3) minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  text-align: left;
  background: var(--color-surface);
  border: 0;
}
.status-dot {
  width: var(--space-2);
  height: var(--space-2);
  background: var(--color-primary);
  border-radius: 50%;
  place-self: center;
}
.message-copy {
  display: grid;
  min-width: 0;
}
.message-copy strong,
.message-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-copy span,
time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.message-item.read strong,
.message-item.read .message-copy span,
.message-item.read time {
  color: var(--color-text-subtle);
}
.pagination {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3);
}
.pagination button {
  min-width: 36px;
  min-height: 36px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.pagination button.active {
  color: white;
  background: var(--color-primary);
}
.feature-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  justify-items: center;
  color: var(--color-text-muted);
}
.inbox-error {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  background: var(--color-danger-soft);
  border-bottom: 1px solid var(--color-danger);
}
@media (max-width: 767px) {
  .message-layout {
    grid-template-columns: 1fr;
  }
  .category-panel {
    display: flex;
    overflow-x: auto;
  }
  .category-heading {
    display: none;
  }
  .message-toolbar {
    flex-wrap: wrap;
  }
  .status-filter {
    margin-left: 0;
  }
  .message-row {
    grid-template-columns: var(--space-3) minmax(0, 1fr);
  }
  .message-row time {
    grid-column: 2;
  }
  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
.message-toolbar {
  min-height: 48px;
  padding-block: var(--space-2);
}
.status-filter {
  margin-left: auto;
}
.message-item {
  height: var(--seller-message-row-height);
  min-height: 0;
  overflow: hidden;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  padding-left: var(--space-2);
  background: var(--color-surface);
}
.message-row {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-1) var(--space-3) var(--space-1) 0;
  background: transparent;
}
.message-copy {
  gap: var(--space-1);
  line-height: 1.2;
}
.message-title-line {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: var(--space-2);
}
.message-title-line strong {
  min-width: 0;
  flex: 0 1 auto;
  font-size: var(--font-size-base);
}
.message-title-line .status-dot {
  flex: 0 0 auto;
  margin-top: 2px;
}
.message-copy strong,
.message-copy span,
.message-item > time {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-copy > span:not(.message-title-line),
.message-item > time {
  font-size: var(--font-size-sm);
}
.message-item > time {
  padding-right: var(--space-5);
}
.message-panel--inbox {
  --seller-message-row-height: 70px;
  padding-bottom: var(--seller-message-row-height);
  scroll-margin-top: var(--space-5);
}
@media (max-width: 767px) {
  .message-row {
    grid-template-columns: minmax(0, 1fr);
  }
  .message-item {
    grid-template-columns: 36px minmax(0, 1fr);
  }
  .message-item > time {
    display: none;
  }
}
.read-all-button {
  min-height: 32px;
  padding-inline: var(--space-2);
  color: var(--color-primary-active);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.read-all-button.push-right {
  margin-left: auto;
}
.status-filter + .read-all-button {
  margin-left: 0;
}
.read-all-button:disabled {
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}
.create-message-form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}
.create-message-form header h2,
.create-message-form header p {
  margin: 0;
}
.create-message-form header p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.create-message-form label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.create-message-form input,
.create-message-form textarea {
  width: 100%;
  padding: var(--space-3);
  font: inherit;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.create-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
}
.create-actions button {
  min-height: 40px;
  padding-inline: var(--space-4);
  border-radius: var(--radius-md);
}
.save-template-button {
  color: var(--color-primary-active);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
}
.send-template-button {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.create-notice {
  margin: 0;
  padding: var(--space-3);
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}
.sent-backup {
  display: grid;
  padding-bottom: 65px;
  scroll-margin-top: var(--space-5);
}
.sent-backup > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}
.sent-backup > header h2,
.sent-backup > header p {
  margin: 0;
}
.sent-backup-delete {
  width: 112px;
  min-height: 32px;
  padding-inline: var(--space-2);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
}
.status-filter select {
  appearance: none;
  padding-right: 34px;
  color: var(--color-text);
  background: var(--color-surface);
}
.status-filter > i {
  position: absolute;
  top: 1px;
  right: 1px;
  display: grid;
  width: 29px;
  height: calc(100% - 2px);
  place-items: center;
  color: var(--color-primary-700);
  font-size: 11px;
  background: var(--color-primary-soft);
  border-left: 1px solid var(--color-border);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  pointer-events: none;
}
.status-filter:hover > i,
.status-filter:focus-within > i {
  color: var(--color-primary-active);
  background: var(--color-primary-200);
}
.sent-backup-delete:disabled {
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}
.sent-backup-columns,
.sent-backup-row {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 120px 150px 160px;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-2) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}
.sent-backup-columns {
  min-height: 44px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-surface-soft);
}
.sent-backup-columns label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  white-space: nowrap;
}
.sent-backup-columns label:first-child {
  grid-column: 1;
}
.sent-backup-columns label:first-child + span {
  grid-column: 2;
}
.sent-backup-columns > span:first-of-type {
  text-align: left;
}
.sent-backup-columns > span:not(:first-of-type),
.sent-backup-row .outbox-field,
.sent-backup-row > time {
  text-align: center;
}
.sent-backup-row {
  min-height: 65px;
}
.sent-backup-row > button {
  display: grid;
  min-width: 0;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
}
.sent-backup-row strong,
.sent-backup-row small,
.sent-backup-row .outbox-field {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sent-backup-row small,
.sent-backup-row .outbox-field,
.sent-backup-row span,
.sent-backup-row time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.outbox-dialog__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  width: var(--space-7);
  height: var(--space-7);
  color: var(--color-text-muted);
  font-size: var(--font-size-xl);
  background: transparent;
  border: 0;
}
.outbox-dialog header {
  padding-right: var(--space-7);
}
.outbox-dialog header p,
.outbox-dialog h2 {
  margin: 0;
}
.outbox-dialog h2 {
  margin-block: var(--space-1);
}
.outbox-dialog time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.outbox-dialog__content {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  white-space: pre-wrap;
  border-top: 1px solid var(--color-border);
}
.textarea-field {
  position: relative;
}
.textarea-field textarea {
  padding-bottom: var(--space-6);
}
.field-counter {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-2);
  color: var(--color-text-subtle);
  font-size: var(--font-size-xs);
  line-height: 1;
  pointer-events: none;
}
.create-message-form select {
  width: 100%;
  min-height: 42px;
  padding-inline: var(--space-3);
  font: inherit;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.send-only-button {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary);
}
.image-upload {
  position: relative;
}
.image-upload small {
  position: absolute;
  top: 0;
  right: 0;
}
.image-file-list {
  margin: calc(-1 * var(--space-2)) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.create-message-form .save-template-check {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.create-message-form .save-template-check input {
  width: auto;
  margin: 0;
}
.template-manager {
  background: transparent;
  border: 0;
  border-radius: 0;
}
.template-manager > header,
.template-manager > div {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: var(--space-3);
}
.template-manager > .template-columns,
.template-manager article {
  display: grid;
  grid-template-columns: 68px 120px minmax(0, 1fr) 150px 232px;
  align-items: center;
  justify-content: start;
  gap: var(--space-2);
}
.template-columns {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-surface-soft);
  border-bottom: 1px solid var(--color-border);
}
.template-columns > span,
.template-manager article > .template-label,
.template-manager article > time {
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.template-columns > span:nth-of-type(2) {
  text-align: left;
}
.template-row-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
.template-header-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
.template-header-actions button {
  display: inline-flex;
  width: 112px;
  min-height: 32px;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding-inline: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border-radius: var(--radius-md);
}
.template-create-action {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}
.template-batch-delete {
  color: var(--color-danger);
  background: var(--color-surface);
  border: 1px solid var(--color-danger);
}
.template-batch-delete:hover:not(:disabled) {
  background: var(--color-danger-soft);
}
.template-batch-delete:disabled {
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}
.template-columns label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  justify-content: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.template-manager > header {
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5);
}
.template-manager > header h2 {
  margin: 0;
}
.template-add {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--color-surface);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-pill);
}
.template-manager article {
  padding-inline: var(--space-3);
  border-top: 1px solid var(--color-border);
}
.template-check {
  display: grid;
  min-height: 65px;
  place-items: center;
}
.template-manager article > .template-summary {
  display: grid;
  min-width: 0;
  padding: var(--space-3);
  text-align: left;
  background: transparent;
  border: 0;
}
.template-summary strong,
.template-summary small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.template-manager article > time {
  font-size: var(--font-size-xs);
}
.template-row-action {
  display: inline-flex;
  align-self: center;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  width: 80px;
  min-height: 28px;
  margin-right: 0;
  padding: 0 var(--space-2);
  color: var(--color-surface);
  font-weight: 600;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.template-row-action:hover {
  background: var(--color-primary-hover);
}
.template-row-delete {
  color: var(--color-danger);
  background: var(--color-surface);
  border-color: var(--color-danger);
  font-size: inherit;
}
.template-row-delete:hover {
  background: var(--color-danger-soft);
}
.template-overlay {
  position: fixed;
  z-index: 1060;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: #0008;
}
.template-dialog {
  position: relative;
  display: grid;
  gap: var(--space-3);
  width: min(100%, 680px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  padding: var(--space-6);
  background: white;
  border-radius: var(--radius-lg);
}
.template-detail-dialog__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  width: var(--space-7);
  height: var(--space-7);
  color: var(--color-text-muted);
  font-size: var(--font-size-xl);
  background: transparent;
  border: 0;
}
.template-detail-dialog header {
  padding-right: var(--space-7);
}
.template-detail-dialog header p,
.template-detail-dialog h2 {
  margin: 0;
}
.template-detail-dialog h2 {
  margin-top: var(--space-1);
}
.template-detail-dialog__content {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  white-space: pre-wrap;
  border-top: 1px solid var(--color-border);
}
.inbox-detail-dialog header {
  padding-right: var(--space-7);
}
.inbox-detail-dialog {
  width: min(100%, 720px);
  overscroll-behavior: contain;
  box-shadow: var(--shadow-card);
}
.inbox-detail-dialog header p,
.inbox-detail-dialog h2 {
  margin: 0;
}
.inbox-detail-dialog header p,
.inbox-detail-dialog time {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.inbox-detail-dialog h2 {
  margin-block: var(--space-1);
}
.inbox-detail-dialog__content {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border-top: 1px solid var(--color-border);
}
.inbox-cancelled-title {
  display: grid;
}
.inbox-order-link {
  width: fit-content;
  color: var(--color-primary-active);
  text-decoration: none;
}
.inbox-order-link:hover {
  font-weight: 700;
}
.inbox-order-link--title {
  font-size: 15px;
  font-weight: 400;
}
.inbox-cancelled-content {
  display: grid;
}
.inbox-message-line--indented {
  padding-left: var(--space-3);
}
.inbox-cancel-reason {
  padding-left: var(--space-7);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.inbox-order-items {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-5);
}
.inbox-order-items--indented {
  margin-left: var(--space-3);
}
.inbox-order-item {
  display: grid;
  grid-template-columns: var(--space-8) minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.inbox-order-item__image {
  display: grid;
  width: var(--space-8);
  height: var(--space-8);
  overflow: hidden;
  place-items: center;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-radius: var(--radius-md);
}
.inbox-order-item__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.inbox-order-item__copy {
  display: grid;
  min-width: 0;
  gap: var(--space-1);
}
.inbox-order-item__copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.inbox-order-item__copy span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.inbox-order-item__total {
  white-space: nowrap;
}
.template-detail-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}
.template-detail-dialog__actions .template-row-action {
  margin-right: 0;
}
.template-editor-dialog {
  padding-top: var(--space-7);
}
.template-editor-title {
  margin: 0;
  padding-right: var(--space-7);
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  line-height: var(--line-height-heading);
}
.template-editor-dialog input,
.template-editor-dialog textarea,
.template-detail-dialog input,
.template-detail-dialog textarea {
  width: 100%;
  padding: var(--space-3);
  font: inherit;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}
.template-editor-dialog label,
.template-detail-dialog label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.template-detail-fields {
  display: grid;
  gap: var(--space-3);
  padding-right: var(--space-7);
}
.template-detail-fields div {
  display: grid;
  gap: var(--space-1);
}
.template-detail-fields span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.template-detail-fields p,
.template-detail-fields h2 {
  margin: 0;
  color: var(--color-text);
}
.template-detail-fields p {
  font-size: var(--font-size-md);
}
.template-detail-fields h2 {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}
.template-detail-dialog [readonly] {
  color: var(--color-text);
  background: var(--color-bg-muted);
}
.template-detail-dialog .image-upload.disabled {
  color: var(--color-text-subtle);
}
.template-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
.template-editor-actions .template-row-action {
  margin-right: 0;
}
.template-editor-save {
  display: flex;
  justify-content: center;
  padding-top: var(--space-2);
}
.template-editor-save button {
  min-height: 40px;
  padding-inline: var(--space-5);
  border-radius: var(--radius-md);
}
.template-editor-error {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  background: var(--color-danger-soft);
  border-radius: var(--radius-md);
}
</style>
