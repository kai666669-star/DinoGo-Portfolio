<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  createSystemMessage,
  createSystemTemplate,
  deleteSystemTemplate,
  getSystemRecords,
  getSystemTemplates,
  updateSystemTemplate,
} from '../../api/adminMessageApi.js'

const PAGE_SIZE = 20
const tabs = [
  { key: 'CREATE', label: '新增訊息' },
  { key: 'TEMPLATES', label: '範本管理' },
  { key: 'OVERVIEW', label: '訊息總覽' },
  { key: 'SEND_HISTORY', label: '發送紀錄' },
]
const templateFilters = ['ALL', 'OA', 'OS', 'OC']
const messageFilters = ['ALL', 'OA', 'OS', 'OC', 'AS', 'AC', 'SC']
const activeTab = ref('CREATE')
const templateFilter = ref('ALL')
const templateSort = reactive({ key: 'sendUpdAt', direction: 'desc' })
const messageFilter = ref('ALL')
const overviewFilters = reactive({ msgPrefix: '', msgNumber: '', sendStatus: '', senderType: 'ALL', senderId: '', sentDate: '' })
const templatePage = ref(1)
const messagePage = ref(1)
const recordPage = ref(1)
const templatePanel = ref(null)
const selectedTemplate = ref(null)
const messagePanel = ref(null)
const notice = ref('')
const templateLoading = ref(false)
const templateError = ref('')
const templatesLoaded = ref(false)
const recordsLoaded = ref(false)
const recordLoading = ref(false)
const recordError = ref('')
let recordRefreshTimer = null
const recordFilters = reactive({ msgPrefix: '', msgNumber: '', status: '', senderName: '', senderId: '', recipientId: '', createdDate: '' })
const form = reactive({ saveAsTemplate: false, msgType: '', recipientId: '', templateId: '', msgLabel: '', sendTitle: '', sendContent: '', submitting: false, error: '' })
const templateCreate = reactive({ open: false, sendId: null, msgType: 'OA', msgLabel: '', sendTitle: '', sendContent: '', submitting: false, error: '' })

const templateItems = ref([])
const recordItems = ref([])
const messageItems = Array.from({ length: 73 }, (_, index) => ({
  sendId: 3000 + index,
  msgfromSellerId: index % 7 === 6 ? 200 + index : 1,
  systemAdminMemberId: 9001,
  msgFunction: `${['OA', 'OS', 'OC', 'AS', 'AC', 'SC'][index % 6]}-${String(index + 1).padStart(3, '0')}`,
  msgLabel: `訊息標籤 ${index + 1}`,
  sendTitle: `平台訊息 ${index + 1}`,
  sendContent: '訊息總覽內容會固定壓縮成單行，不影響每列 65px 的高度。',
  sendStatus: index % 11 === 0 ? 'DELETE' : (index % 6 === 5 && index % 5 === 0 ? 'SAVE' : 'SEND'),
  sendUpdAt: new Date(Date.now() - index * 2700000).toISOString(),
}))

const prefixOf = (item) => item.msgFunction?.slice(0, 2)
const filteredTemplates = computed(() => templateItems.value
  .filter((item) => item.sendStatus === 'SAVE' && (templateFilter.value === 'ALL' || prefixOf(item) === templateFilter.value))
  .sort((left, right) => {
    const comparison = templateSort.key === 'msgFunction'
      ? String(left.msgFunction || '').localeCompare(String(right.msgFunction || ''), 'zh-TW', { numeric: true })
      : new Date(left.sendUpdAt).getTime() - new Date(right.sendUpdAt).getTime()
    return templateSort.direction === 'asc' ? comparison : -comparison
  }))
const isSendHistory = computed(() => activeTab.value === 'SEND_HISTORY')
const messageSenderType = (item) => prefixOf(item) === 'SC' ? 'SELLER' : 'ADMIN'
const messageSenderId = (item) => messageSenderType(item) === 'SELLER'
  ? item.msgfromSellerId
  : (item.systemAdminMemberId ?? item.memberId ?? item.msgfromSellerId)
const messageDate = (item) => item.sendUpdAt?.slice(0, 10) || ''
const suggestionValues = (selector) => [...new Set(messageItems.map(selector).filter((value) => value !== null && value !== undefined && value !== ''))]
  .map(String)
  .sort((left, right) => left.localeCompare(right, 'zh-TW', { numeric: true }))
const msgFunctionSuggestions = computed(() => suggestionValues((item) =>
  !overviewFilters.msgPrefix || prefixOf(item) === overviewFilters.msgPrefix
    ? item.msgFunction?.split('-')[1]
    : null))
const senderIdSuggestions = computed(() => suggestionValues((item) =>
  overviewFilters.senderType === 'ALL' || messageSenderType(item) === overviewFilters.senderType ? messageSenderId(item) : null))
const sentDateSuggestions = computed(() => suggestionValues(messageDate).reverse())
const filteredMessages = computed(() => messageItems.filter((item) => (isSendHistory.value
  ? item.sendStatus === 'SEND' && ['OA', 'OS', 'OC'].includes(prefixOf(item))
  : item.sendStatus === 'SEND' || item.sendStatus === 'DELETE' || (item.sendStatus === 'SAVE' && prefixOf(item) === 'SC'))
  && (messageFilter.value === 'ALL' || prefixOf(item) === messageFilter.value)
  && (isSendHistory.value || !overviewFilters.msgPrefix || prefixOf(item) === overviewFilters.msgPrefix)
  && (isSendHistory.value || !overviewFilters.msgNumber.trim() || item.msgFunction?.split('-')[1]?.includes(overviewFilters.msgNumber.trim()))
  && (isSendHistory.value || !overviewFilters.sendStatus || item.sendStatus === overviewFilters.sendStatus)
  && (isSendHistory.value || overviewFilters.senderType === 'ALL' || messageSenderType(item) === overviewFilters.senderType)
  && (isSendHistory.value || !overviewFilters.senderId.trim() || String(messageSenderId(item) ?? '').includes(overviewFilters.senderId.trim()))
  && (isSendHistory.value || !overviewFilters.sentDate || messageDate(item) === overviewFilters.sentDate)))
const pageCount = (items) => Math.max(1, Math.ceil(items.length / PAGE_SIZE))
const templatePageCount = computed(() => pageCount(filteredTemplates.value))
const messagePageCount = computed(() => pageCount(filteredMessages.value))
const visibleTemplates = computed(() => filteredTemplates.value.slice((templatePage.value - 1) * PAGE_SIZE, templatePage.value * PAGE_SIZE))
const visibleMessages = computed(() => filteredMessages.value.slice((messagePage.value - 1) * PAGE_SIZE, messagePage.value * PAGE_SIZE))
const filteredRecords = computed(() => recordItems.value.filter((item) =>
  (!recordFilters.msgPrefix || prefixOf(item) === recordFilters.msgPrefix)
  && (!recordFilters.msgNumber || item.msgFunction?.split('-')[1] === recordFilters.msgNumber)
  && (!recordFilters.status || item.recordStatus === recordFilters.status)
  && (!recordFilters.senderName.trim() || recordSenderName(item).includes(recordFilters.senderName.trim()))
  && (!recordFilters.senderId.trim() || String(item.msgfromSellerId ?? '').includes(recordFilters.senderId.trim()))
  && (!recordFilters.recipientId.trim() || String(recordRecipientId(item) ?? '').includes(recordFilters.recipientId.trim()))
  && (!recordFilters.createdDate || item.recordCreatedAt?.slice(0, 10) === recordFilters.createdDate)))
const recordPageCount = computed(() => pageCount(filteredRecords.value))
const visibleRecords = computed(() => filteredRecords.value.slice((recordPage.value - 1) * PAGE_SIZE, recordPage.value * PAGE_SIZE))
const recordNumberSuggestions = computed(() => [...new Set(recordItems.value.filter((item) => !recordFilters.msgPrefix || prefixOf(item) === recordFilters.msgPrefix).map((item) => item.msgFunction?.split('-')[1]).filter(Boolean))].sort())
const recordSenderNameSuggestions = computed(() => [...new Set(recordItems.value.map(recordSenderName).filter(Boolean))].sort((left, right) => left.localeCompare(right, 'zh-TW')))
const recordSenderIdSuggestions = computed(() => [...new Set(recordItems.value.filter((item) => !recordFilters.senderName || recordSenderName(item) === recordFilters.senderName).map((item) => item.msgfromSellerId).filter((value) => value != null))].map(String).sort((left, right) => left.localeCompare(right, 'zh-TW', { numeric: true })))
const recordRecipientIdSuggestions = computed(() => [...new Set(recordItems.value.map(recordRecipientId).filter((value) => value != null))].map(String).sort((left, right) => left.localeCompare(right, 'zh-TW', { numeric: true })))
const recordDateSuggestions = computed(() => [...new Set(recordItems.value.map((item) => item.recordCreatedAt?.slice(0, 10)).filter(Boolean))].sort().reverse())
const firstPages = (total) => Array.from({ length: Math.min(2, total) }, (_, index) => index + 1)
const formatTime = (value) => new Intl.DateTimeFormat('zh-TW', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
const senderLabel = (item) => prefixOf(item) === 'SC'
  ? `商家 ${item.msgfromSellerId ?? '—'}`
  : `系統管理員 ${item.systemAdminMemberId ?? item.memberId ?? item.msgfromSellerId ?? '—'}`
const templateEditorLabel = (item) => `系統管理員 ${item.lastModifiedMemberId ?? item.memberId ?? item.msgfromSellerId ?? '—'}`
function recordSenderName(item) { return prefixOf(item) === 'SC' ? (item.storeName || '商家') : '系統自動訊息' }
function recordRecipientId(item) { return item.msgtoMemberId ?? item.msgtoSellerId }
const selectedTemplateIds = ref(new Set())
const allVisibleTemplatesSelected = computed(() => visibleTemplates.value.length > 0 && visibleTemplates.value.every((item) => selectedTemplateIds.value.has(item.sendId)))

function toggleTemplate(sendId) { const next = new Set(selectedTemplateIds.value); next.has(sendId) ? next.delete(sendId) : next.add(sendId); selectedTemplateIds.value = next }
function toggleAllVisibleTemplates() { const next = new Set(selectedTemplateIds.value); visibleTemplates.value.forEach((item) => allVisibleTemplatesSelected.value ? next.delete(item.sendId) : next.add(item.sendId)); selectedTemplateIds.value = next }
function toggleTemplateSort(key) {
  if (templateSort.key === key) templateSort.direction = templateSort.direction === 'asc' ? 'desc' : 'asc'
  else {
    templateSort.key = key
    templateSort.direction = 'desc'
  }
  templatePage.value = 1
}
const templateSortLabel = (key) => templateSort.key === key
  ? (templateSort.direction === 'asc' ? '正序' : '倒序')
  : '倒序'
const templateSortIcon = (key) => templateSort.key === key
  ? (templateSort.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down')
  : 'bi-arrow-down'
const apiErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback

async function loadTemplates() {
  templateLoading.value = true
  templateError.value = ''
  try {
    const first = (await getSystemTemplates(0)).data
    const remaining = first.totalPages > 1
      ? await Promise.all(Array.from({ length: first.totalPages - 1 }, (_, index) => getSystemTemplates(index + 1)))
      : []
    templateItems.value = [
      ...(first.items || []),
      ...remaining.flatMap((response) => response.data?.items || []),
    ].filter((item) => item.sendStatus === 'SAVE')
      .sort((left, right) => new Date(right.sendUpdAt) - new Date(left.sendUpdAt) || right.sendId - left.sendId)
    selectedTemplateIds.value = new Set()
    templatesLoaded.value = true
    templatePage.value = Math.min(templatePage.value, pageCount(filteredTemplates.value))
  } catch (error) {
    templateError.value = apiErrorMessage(error, '系統範本載入失敗')
  } finally {
    templateLoading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  recordError.value = ''
  try {
    const first = (await getSystemRecords(0)).data
    const remaining = first.totalPages > 1 ? await Promise.all(Array.from({ length: first.totalPages - 1 }, (_, index) => getSystemRecords(index + 1))) : []
    recordItems.value = [...(first.items || []), ...remaining.flatMap((response) => response.data?.items || [])]
    recordsLoaded.value = true
    recordPage.value = 1
  } catch (error) {
    recordError.value = apiErrorMessage(error, '發送紀錄載入失敗')
  } finally {
    recordLoading.value = false
  }
}

function openTemplateCreate() {
  Object.assign(templateCreate, {
    open: true,
    sendId: null,
    msgType: templateFilter.value === 'ALL' ? 'OA' : templateFilter.value,
    msgLabel: '',
    sendTitle: '',
    sendContent: '',
    submitting: false,
    error: '',
  })
  document.body.style.overflow = 'hidden'
}
function closeTemplateCreate() {
  templateCreate.open = false
  document.body.style.overflow = ''
}
async function addTemplate() {
  if (templateCreate.submitting) return
  const payload = {
    msgType: templateCreate.msgType,
    msgLabel: templateCreate.msgLabel.trim() || templateCreate.sendTitle.trim(),
    sendTitle: templateCreate.sendTitle.trim(),
    sendContent: templateCreate.sendContent.trim(),
  }
  if (!payload.sendTitle || !payload.sendContent) return
  templateCreate.submitting = true
  templateCreate.error = ''
  try {
    if (templateCreate.sendId == null) await createSystemTemplate(payload)
    else await updateSystemTemplate(templateCreate.sendId, payload)
    await loadTemplates()
    closeTemplateCreate()
  } catch (error) {
    templateCreate.error = apiErrorMessage(error, templateCreate.sendId == null ? '新增範本失敗' : '修改範本失敗')
  } finally {
    templateCreate.submitting = false
  }
}

function editTemplate(item) {
  Object.assign(templateCreate, { open: true, sendId: item.sendId, msgType: prefixOf(item), msgLabel: item.msgLabel || '', sendTitle: item.sendTitle || '', sendContent: item.sendContent || '', submitting: false, error: '' })
  document.body.style.overflow = 'hidden'
}

async function deleteTemplate(item) {
  if (!window.confirm(`是否確認刪除${item.msgFunction}範本`)) return false
  templateError.value = ''
  try {
    await deleteSystemTemplate(item.sendId)
    await loadTemplates()
    return true
  } catch (error) {
    templateError.value = apiErrorMessage(error, '刪除範本失敗')
    return false
  }
}

function openTemplateDetail(item) {
  selectedTemplate.value = item
  document.body.style.overflow = 'hidden'
}
function closeTemplateDetail() {
  selectedTemplate.value = null
  document.body.style.overflow = ''
}
function editSelectedTemplate() {
  const item = selectedTemplate.value
  selectedTemplate.value = null
  editTemplate(item)
}
async function deleteSelectedTemplate() {
  const deleted = await deleteTemplate(selectedTemplate.value)
  if (deleted) closeTemplateDetail()
}

async function deleteSelectedTemplates() {
  const targets = templateItems.value.filter((item) => selectedTemplateIds.value.has(item.sendId))
  if (targets.length === 0 || !window.confirm(`是否確認刪除已選取的 ${targets.length} 筆範本`)) return
  templateError.value = ''
  try {
    await Promise.all(targets.map((item) => deleteSystemTemplate(item.sendId)))
    await loadTemplates()
  } catch (error) {
    const message = apiErrorMessage(error, '批次刪除範本失敗，已重新載入目前資料')
    await loadTemplates()
    templateError.value = message
  }
}

async function changePage(target, page, total) {
  const next = Math.min(Math.max(1, page), total)
  if (target === 'template') templatePage.value = next
  else messagePage.value = next
  await nextTick()
  ;(target === 'template' ? templatePanel.value : messagePanel.value)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function changeFilter(target) {
  if (target === 'template') templatePage.value = 1
  else messagePage.value = 1
}
function activateOverviewFilter(group) {
  if (group === 'status') {
    changeFilter('message')
    return
  }
  if (group !== 'function') {
    overviewFilters.msgPrefix = ''
    overviewFilters.msgNumber = ''
  }
  if (group !== 'sender') {
    overviewFilters.senderType = 'ALL'
    overviewFilters.senderId = ''
  }
  if (group !== 'date') overviewFilters.sentDate = ''
  changeFilter('message')
}
function changeOverviewPrefix() {
  const normalized = overviewFilters.msgPrefix.trim().toUpperCase()
  overviewFilters.msgPrefix = ['', 'OA', 'OS', 'OC', 'AS', 'AC', 'SC'].includes(normalized)
    ? normalized
    : overviewFilters.msgPrefix
  overviewFilters.msgNumber = ''
  activateOverviewFilter('function')
}
function changeOverviewSenderType() {
  overviewFilters.senderId = ''
  activateOverviewFilter('sender')
}
function selectOverviewSuggestion(field, event) {
  const value = event.target.value
  if (!value) return
  overviewFilters[field] = value
  activateOverviewFilter(field === 'senderId' ? 'sender' : 'date')
  event.target.selectedIndex = 0
}
function selectMessageTemplate() {
  const item = templateItems.value.find((entry) => entry.sendId === Number(form.templateId))
  if (!item) return
  form.msgType = prefixOf(item)
  form.msgLabel = item.msgLabel || ''
  form.sendTitle = item.sendTitle || ''
  form.sendContent = item.sendContent || ''
}
function openOverviewOptions(event) {
  const select = event.currentTarget.previousElementSibling
  if (typeof select?.showPicker === 'function') select.showPicker()
  else select?.click()
}
async function selectTab(key) {
  activeTab.value = key
  if (key === 'TEMPLATES' && !templatesLoaded.value) await loadTemplates()
  if (key === 'OVERVIEW' || key === 'SEND_HISTORY') {
    messageFilter.value = 'ALL'
    messagePage.value = 1
  }
  if (key === 'SEND_HISTORY') await loadRecords()
}
function activateRecordFilter(group) {
  if (group !== 'function' && group !== 'status' && group !== 'date') {
    recordFilters.msgPrefix = ''
    recordFilters.msgNumber = ''
  }
  if (group !== 'sender' && group !== 'status' && group !== 'date') {
    recordFilters.senderName = ''
    recordFilters.senderId = ''
  }
  if (group !== 'recipient' && group !== 'status' && group !== 'date') recordFilters.recipientId = ''
  recordPage.value = 1
}
function changeRecordPrefix() {
  recordFilters.msgNumber = ''
  activateRecordFilter('function')
}
function changeRecordSenderName() {
  recordFilters.senderId = ''
  activateRecordFilter('sender')
}
function selectRecordSuggestion(field, event, group) {
  const value = event.target.value
  if (!value) return
  recordFilters[field] = value
  activateRecordFilter(group)
  event.target.selectedIndex = 0
}
async function changeRecordPage(page) {
  recordPage.value = Math.min(Math.max(1, page), recordPageCount.value)
  await nextTick()
  messagePanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
async function submitMessage() {
  if (form.submitting) return
  form.submitting = true
  form.error = ''
  notice.value = ''
  const recipientId = form.recipientId === '' ? null : Number(form.recipientId)
  const sendPayload = {
    msgType: form.msgType,
    sendTitle: form.sendTitle.trim(),
    sendContent: form.sendContent.trim(),
    msgtoMemberId: form.msgType !== 'OS' ? recipientId : null,
    msgtoSellerId: form.msgType === 'OS' ? recipientId : null,
  }
  try {
    await createSystemMessage(sendPayload)
    if (form.saveAsTemplate) {
      await createSystemTemplate({ msgType: form.msgType, msgLabel: form.msgLabel.trim() || form.sendTitle.trim(), sendTitle: form.sendTitle.trim(), sendContent: form.sendContent.trim() })
      await loadTemplates()
    }
    await loadRecords()
    clearTimeout(recordRefreshTimer)
    recordRefreshTimer = setTimeout(loadRecords, 1000)
    notice.value = form.saveAsTemplate ? '訊息已送出並儲存為範本。' : '訊息已送出。'
    Object.assign(form, { saveAsTemplate: false, recipientId: '', templateId: '', msgLabel: '', sendTitle: '', sendContent: '' })
  } catch (error) {
    form.error = apiErrorMessage(error, '訊息送出失敗')
  } finally {
    form.submitting = false
  }
}
onMounted(loadTemplates)
onBeforeUnmount(() => {
  clearTimeout(recordRefreshTimer)
  document.body.style.overflow = ''
})
</script>

<template>
  <section class="admin-message-page">
    <header class="page-heading"><p>管理後台</p><h1>訊息中心</h1></header>
    <nav class="section-tabs" aria-label="訊息中心功能">
      <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">{{ tab.label }}</button>
    </nav>

    <form v-if="activeTab === 'CREATE'" class="content-panel create-form" @submit.prevent="submitMessage">
      <header><h2>新增訊息</h2></header>
      <label class="create-save-template"><input v-model="form.saveAsTemplate" type="checkbox" />寄出訊息並同時儲存為範本</label>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <label>*寄送對象 (必填)<select v-model="form.msgType" required><option value="" disabled>請選擇寄送對象</option><option value="OA">全部</option><option value="OS">商家會員</option><option value="OC">一般會員</option></select></label>
      <label>輸入個別會員ID<input v-model="form.recipientId" type="number" min="1" inputmode="numeric" placeholder="留空則依寄送對象處理" /></label>
      <label>可選擇現有範本<select v-model="form.templateId" :disabled="templateLoading" @change="selectMessageTemplate"><option value="">不使用範本</option><option v-for="item in templateItems" :key="item.sendId" :value="item.sendId">{{ item.msgFunction }}｜{{ item.msgLabel }}</option></select></label>
      <label>範本名稱<input v-model="form.msgLabel" maxlength="50" :disabled="!form.saveAsTemplate" /></label>
      <label>*訊息標題 (必填)<input v-model="form.sendTitle" maxlength="100" required /></label>
      <label class="create-message-textarea">*訊息內容 (必填)<textarea v-model="form.sendContent" maxlength="1000" rows="8" required></textarea><small>{{ form.sendContent.length }}/1000</small></label>
      <p v-if="form.error" class="template-create-error" role="alert">{{ form.error }}</p>
      <button class="primary-action create-message-submit" type="submit" :disabled="form.submitting">{{ form.submitting ? '送出中…' : '送出' }}</button>
    </form>

    <section v-else-if="activeTab === 'TEMPLATES'" ref="templatePanel" class="content-panel paged-panel">
      <header class="panel-toolbar"><div><h2>範本管理</h2>
      </div><div class="admin-template-toolbar"><label class="unified-select"><span class="visually-hidden">篩選範本</span><select v-model="templateFilter" aria-label="篩選範本" :disabled="templateLoading" @change="changeFilter('template')"><option v-for="filter in templateFilters" :key="filter" :value="filter">{{ filter === 'ALL' ? '全部範本' : filter }}</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label><button class="template-add-action" :disabled="templateLoading" @click="openTemplateCreate"><i class="bi bi-plus-lg" aria-hidden="true"></i>新增範本</button><button class="template-batch-delete" :disabled="templateLoading || selectedTemplateIds.size === 0" @click="deleteSelectedTemplates">批次刪除（{{ selectedTemplateIds.size }}）</button></div></header>
      <p v-if="templateError" class="template-feedback error" role="alert">{{ templateError }}</p><p v-else-if="templateLoading" class="template-feedback" role="status">範本載入中…</p><div v-else class="template-list"><div class="template-columns"><label><input type="checkbox" :checked="allVisibleTemplatesSelected" @change="toggleAllVisibleTemplates" />全選</label><span>send_id</span><button class="column-sort" :class="{ active: templateSort.key === 'msgFunction' }" :aria-label="`msg_function ${templateSortLabel('msgFunction')}`" @click="toggleTemplateSort('msgFunction')"><span>msg_function</span><i class="bi" :class="templateSortIcon('msgFunction')" aria-hidden="true"></i><span class="visually-hidden">{{ templateSortLabel('msgFunction') }}</span></button><span>自訂範本名稱</span><span>訊息標題 / 訊息內容</span><button class="column-sort" :class="{ active: templateSort.key === 'sendUpdAt' }" :aria-label="`最新修改 ${templateSortLabel('sendUpdAt')}`" @click="toggleTemplateSort('sendUpdAt')"><span>最新修改</span><i class="bi" :class="templateSortIcon('sendUpdAt')" aria-hidden="true"></i><span class="visually-hidden">{{ templateSortLabel('sendUpdAt') }}</span></button><span aria-hidden="true"></span></div><article v-for="item in visibleTemplates" :key="item.sendId" class="template-row" role="button" tabindex="0" :aria-label="`檢視範本 ${item.msgLabel}`" @click="openTemplateDetail(item)" @keydown.enter="openTemplateDetail(item)" @keydown.space.prevent="openTemplateDetail(item)"><label class="template-check" @click.stop><input type="checkbox" :checked="selectedTemplateIds.has(item.sendId)" @change="toggleTemplate(item.sendId)" /></label><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span class="template-label"><strong>{{ item.msgLabel }}</strong></span><span class="template-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><span class="template-modified"><time>{{ formatTime(item.sendUpdAt) }}</time><small>{{ templateEditorLabel(item) }}</small></span><span class="template-row-actions" @click.stop><button class="template-edit-action" @click="editTemplate(item)"><i class="bi bi-pencil" aria-hidden="true"></i>修改</button><button class="template-delete-action" @click="deleteTemplate(item)"><span aria-hidden="true">×</span>刪除</button></span></article><p v-if="visibleTemplates.length === 0" class="template-feedback">目前沒有符合條件的範本。</p></div>
      <nav class="pagination" aria-label="範本頁籤"><button :disabled="templatePage === 1" @click="changePage('template', 1, templatePageCount)">&lt;&lt;</button><button :disabled="templatePage === 1" @click="changePage('template', templatePage - 1, templatePageCount)">&lt;</button><button v-for="page in firstPages(templatePageCount)" :key="page" :class="{ active: templatePage === page }" @click="changePage('template', page, templatePageCount)">{{ page }}</button><span v-if="templatePageCount > 2">…</span><button v-if="templatePageCount > 2" :class="{ active: templatePage === templatePageCount }" @click="changePage('template', templatePageCount, templatePageCount)">{{ templatePageCount }}</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePage + 1, templatePageCount)">&gt;</button><button :disabled="templatePage === templatePageCount" @click="changePage('template', templatePageCount, templatePageCount)">&gt;&gt;</button></nav>
    </section>

    <section v-else ref="messagePanel" class="content-panel paged-panel">
      <header class="panel-toolbar"><div><h2>{{ isSendHistory ? '發送紀錄' : '訊息總覽' }}</h2></div></header>
      <div v-if="isSendHistory" class="record-history">
        <div class="record-filters">
          <span></span>
          <div class="function-filter-pair"><label class="unified-select"><select v-model="recordFilters.msgPrefix" aria-label="篩選 msg_function 前綴" @change="changeRecordPrefix"><option value="">全部</option><option v-for="prefix in messageFilters.slice(1)" :key="prefix" :value="prefix">{{ prefix }}</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label><b>-</b><label class="unified-select"><select v-model="recordFilters.msgNumber" aria-label="篩選 msg_function 流水號" @change="activateRecordFilter('function')"><option value="">全部</option><option v-for="number in recordNumberSuggestions" :key="number" :value="number">{{ number }}</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label></div>
          <label class="unified-select"><select v-model="recordFilters.status" aria-label="篩選紀錄狀態" @change="activateRecordFilter('status')"><option value="">全部</option><option value="READ">READ</option><option value="UNREAD">UNREAD</option><option value="DELETE">DELETE</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          <label class="filter-combobox"><input v-model="recordFilters.senderName" aria-label="篩選 from" placeholder="手動輸入" @input="activateRecordFilter('sender')" /><select class="combobox-menu" aria-label="選擇 from" tabindex="-1" @change="selectRecordSuggestion('senderName', $event, 'sender')"><option value="" hidden></option><option v-for="value in recordSenderNameSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟 from 選單" @click="openOverviewOptions"><i class="bi bi-chevron-down"></i></button></label>
          <label class="filter-combobox"><input v-model="recordFilters.senderId" aria-label="篩選寄件人 ID" placeholder="手動輸入" @input="activateRecordFilter('sender')" /><select class="combobox-menu" aria-label="選擇寄件人 ID" tabindex="-1" @change="selectRecordSuggestion('senderId', $event, 'sender')"><option value="" hidden></option><option v-for="value in recordSenderIdSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件人 ID 選單" @click="openOverviewOptions"><i class="bi bi-chevron-down"></i></button></label>
          <label class="filter-combobox"><input v-model="recordFilters.recipientId" aria-label="篩選收件人 ID" placeholder="手動輸入" @input="activateRecordFilter('recipient')" /><select class="combobox-menu" aria-label="選擇收件人 ID" tabindex="-1" @change="selectRecordSuggestion('recipientId', $event, 'recipient')"><option value="" hidden></option><option v-for="value in recordRecipientIdSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟收件人 ID 選單" @click="openOverviewOptions"><i class="bi bi-chevron-down"></i></button></label>
          <span></span>
          <label class="filter-combobox"><input v-model="recordFilters.createdDate" aria-label="篩選寄件時間" placeholder="輸入寄件日期" @input="activateRecordFilter('date')" /><select class="combobox-menu" aria-label="選擇寄件時間" tabindex="-1" @change="selectRecordSuggestion('createdDate', $event, 'date')"><option value="" hidden></option><option v-for="date in recordDateSuggestions" :key="date" :value="date">{{ date }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件時間選單" @click="openOverviewOptions"><i class="bi bi-chevron-down"></i></button></label>
        </div>
        <div class="record-columns"><span>send_id</span><span>msg_function</span><span>狀態</span><span>from</span><span>寄件人ID</span><span>收件人ID</span><span>訊息標題 / 訊息內容</span><span>寄件時間</span></div>
        <p v-if="recordError" class="template-feedback error" role="alert">{{ recordError }}</p><p v-else-if="recordLoading" class="template-feedback">發送紀錄載入中…</p>
        <article v-for="item in visibleRecords" v-else :key="item.recordId" class="record-row"><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span>{{ item.recordStatus }}</span><span>{{ recordSenderName(item) }}</span><span>{{ item.msgfromSellerId ?? '—' }}</span><span>{{ recordRecipientId(item) ?? '—' }}</span><span class="message-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.recordCreatedAt) }}</time></article>
        <p v-if="!recordLoading && !recordError && visibleRecords.length === 0" class="template-feedback">目前沒有符合條件的發送紀錄。</p>
      </div>
      <div v-else class="overview-list">
        <div class="overview-filters">
          <span aria-hidden="true"></span>
          <div class="function-filter-pair">
            <label class="unified-select"><span class="visually-hidden">篩選訊息類型</span><select v-model="overviewFilters.msgPrefix" @change="changeOverviewPrefix"><option value="">全部</option><option value="OA">OA</option><option value="OS">OS</option><option value="OC">OC</option><option value="AS">AS</option><option value="AC">AC</option><option value="SC">SC</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
            <b aria-hidden="true">-</b>
            <label class="unified-select"><span class="visually-hidden">篩選三位流水號</span><select v-model="overviewFilters.msgNumber" @change="activateOverviewFilter('function')"><option value="">全部</option><option v-for="value in msgFunctionSuggestions" :key="value" :value="value">{{ value }}</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          </div>
          <label class="unified-select"><span class="visually-hidden">篩選訊息狀態</span><select v-model="overviewFilters.sendStatus" @change="activateOverviewFilter('status')"><option value="">全部</option><option value="SEND">SEND</option><option value="SAVE">SAVE</option><option value="DELETE">DELETE</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          <span aria-hidden="true"></span>
          <label class="unified-select"><span class="visually-hidden">篩選寄件來源</span><select v-model="overviewFilters.senderType" @change="changeOverviewSenderType"><option value="ALL">全部</option><option value="SELLER">商家</option><option value="ADMIN">系統訊息</option></select><i class="bi bi-chevron-down" aria-hidden="true"></i></label>
          <label class="filter-combobox"><span class="visually-hidden">篩選寄件人 ID</span><input v-model="overviewFilters.senderId" placeholder="手動輸入" @input="activateOverviewFilter('sender')" /><select class="combobox-menu" aria-label="選擇寄件人 ID" tabindex="-1" @change="selectOverviewSuggestion('senderId', $event)"><option value="" hidden></option><option v-for="value in senderIdSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件人 ID 選單" @click="openOverviewOptions"><i class="bi bi-chevron-down" aria-hidden="true"></i></button></label>
          <span aria-hidden="true"></span>
          <label class="filter-combobox"><span class="visually-hidden">篩選寄件時間</span><input v-model="overviewFilters.sentDate" placeholder="手動輸入" @input="activateOverviewFilter('date')" /><select class="combobox-menu" aria-label="選擇寄件時間" tabindex="-1" @change="selectOverviewSuggestion('sentDate', $event)"><option value="" hidden></option><option v-for="value in sentDateSuggestions" :key="value" :value="value">{{ value }}</option></select><button type="button" class="combobox-trigger" aria-label="開啟寄件時間選單" @click="openOverviewOptions"><i class="bi bi-chevron-down" aria-hidden="true"></i></button></label>
        </div>
        <div class="overview-columns"><span>send_id</span><span>msg_function</span><span>狀態</span><span>自訂範本名稱</span><span>from</span><span>寄件人ID</span><span>訊息標題 / 訊息內容</span><span>寄件時間</span></div>
        <article v-for="item in visibleMessages" :key="item.sendId" class="overview-row"><span>#{{ item.sendId }}</span><span>{{ item.msgFunction }}</span><span>{{ item.sendStatus }}</span><span>{{ item.msgLabel }}</span><span>{{ messageSenderType(item) === 'SELLER' ? '商家' : '系統管理員' }}</span><span>{{ messageSenderId(item) ?? '—' }}</span><span class="message-copy"><strong>{{ item.sendTitle }}</strong><small>{{ item.sendContent }}</small></span><time>{{ formatTime(item.sendUpdAt) }}</time></article>
        <p v-if="visibleMessages.length === 0" class="template-feedback">目前沒有符合篩選條件的訊息。</p>
      </div>
      <nav v-if="isSendHistory" class="pagination" aria-label="發送紀錄頁籤"><button :disabled="recordPage === 1" @click="changeRecordPage(1)">&lt;&lt;</button><button :disabled="recordPage === 1" @click="changeRecordPage(recordPage-1)">&lt;</button><button v-for="page in firstPages(recordPageCount)" :key="page" :class="{ active: recordPage === page }" @click="changeRecordPage(page)">{{ page }}</button><span v-if="recordPageCount > 2">…</span><button v-if="recordPageCount > 2" :class="{ active: recordPage === recordPageCount }" @click="changeRecordPage(recordPageCount)">{{ recordPageCount }}</button><button :disabled="recordPage === recordPageCount" @click="changeRecordPage(recordPage+1)">&gt;</button><button :disabled="recordPage === recordPageCount" @click="changeRecordPage(recordPageCount)">&gt;&gt;</button></nav>
      <nav v-else class="pagination" aria-label="訊息總覽頁籤"><button :disabled="messagePage === 1" @click="changePage('message', 1, messagePageCount)">&lt;&lt;</button><button :disabled="messagePage === 1" @click="changePage('message', messagePage - 1, messagePageCount)">&lt;</button><button v-for="page in firstPages(messagePageCount)" :key="page" :class="{ active: messagePage === page }" @click="changePage('message', page, messagePageCount)">{{ page }}</button><span v-if="messagePageCount > 2">…</span><button v-if="messagePageCount > 2" :class="{ active: messagePage === messagePageCount }" @click="changePage('message', messagePageCount, messagePageCount)">{{ messagePageCount }}</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePage + 1, messagePageCount)">&gt;</button><button :disabled="messagePage === messagePageCount" @click="changePage('message', messagePageCount, messagePageCount)">&gt;&gt;</button></nav>
    </section>

    <div v-if="selectedTemplate" class="template-detail-overlay" @click.self="closeTemplateDetail">
      <article class="admin-template-dialog" role="dialog" aria-modal="true" aria-labelledby="admin-template-detail-title">
        <button type="button" class="admin-template-dialog__close" aria-label="關閉範本詳情" @click="closeTemplateDetail">×</button>
        <header>
          <small>自訂範本名稱</small>
          <p>{{ selectedTemplate.msgLabel || '—' }}</p>
          <small>訊息標題</small>
          <h2 id="admin-template-detail-title">{{ selectedTemplate.sendTitle }}</h2>
        </header>
        <div class="admin-template-dialog__actions"><button class="template-edit-action" @click="editSelectedTemplate"><i class="bi bi-pencil" aria-hidden="true"></i>修改</button><button class="template-delete-action" @click="deleteSelectedTemplate"><span aria-hidden="true">×</span>刪除</button></div>
        <hr />
        <section class="template-detail-field"><label>訊息內容</label><div>{{ selectedTemplate.sendContent }}</div><small>{{ (selectedTemplate.sendContent || '').length }}/1000</small></section>
        <section class="template-detail-field"><label>備註</label><div>{{ selectedTemplate.sendRemark || '—' }}</div><small>{{ (selectedTemplate.sendRemark || '').length }}/1000</small></section>
      </article>
    </div>
    <div v-if="templateCreate.open" class="template-detail-overlay" @click.self="closeTemplateCreate">
      <form class="admin-template-dialog admin-template-create-dialog" @submit.prevent="addTemplate">
        <button type="button" class="admin-template-dialog__close" :aria-label="templateCreate.sendId == null ? '關閉新增範本' : '關閉修改範本'" @click="closeTemplateCreate">×</button>
        <div class="template-create-heading"><h2>{{ templateCreate.sendId == null ? '新增範本' : '修改範本' }}</h2></div>
        <label>預設對象<select v-model="templateCreate.msgType"><option value="OA">全部</option><option value="OS">商家會員</option><option value="OC">一般會員</option></select></label>
        <label>自訂範本名稱<input v-model="templateCreate.msgLabel" maxlength="50" /></label>
        <label>*訊息標題 (必填)<input v-model="templateCreate.sendTitle" maxlength="100" required /></label>
        <label class="template-create-textarea">*訊息內容 (必填)<textarea v-model="templateCreate.sendContent" maxlength="1000" rows="8" required></textarea><small>{{ templateCreate.sendContent.length }}/1000</small></label>
        <p v-if="templateCreate.error" class="template-create-error" role="alert">{{ templateCreate.error }}</p>
        <button type="submit" class="primary-action template-create-save" :disabled="templateCreate.submitting">{{ templateCreate.submitting ? '儲存中…' : '儲存' }}</button>
      </form>
    </div>
  </section>
</template>

<style scoped>
.admin-message-page{display:grid;gap:var(--space-4);color:var(--color-text)}.page-heading p,.page-heading h1,.content-panel h2,.content-panel header p{margin:0}.page-heading p,.content-panel header p{color:var(--color-text-muted);font-size:var(--font-size-sm)}.page-heading h1{font-family:var(--font-heading);font-size:var(--font-size-xl)}.section-tabs{display:flex;border-bottom:1px solid var(--color-border)}.section-tabs button{min-height:48px;padding-inline:var(--space-5);font:inherit;font-weight:600;color:var(--color-text-muted);background:transparent;border:0;border-bottom:4px solid transparent}.section-tabs button.active{color:var(--color-primary-active);border-bottom-color:var(--color-primary)}.content-panel{overflow:hidden;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);scroll-margin-top:var(--space-5)}.create-form{display:grid;gap:var(--space-4);padding:var(--space-5)}.create-form label{display:grid;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.create-form input,.create-form textarea,.create-form select,.panel-toolbar select{padding:var(--space-3);font:inherit;background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.primary-action{justify-self:end;min-height:40px;padding-inline:var(--space-4);color:var(--color-surface);font-weight:600;background:var(--color-primary);border:1px solid var(--color-primary);border-radius:var(--radius-md)}.notice{padding:var(--space-3);color:var(--color-primary-active);background:var(--color-primary-soft);border-radius:var(--radius-md)}.paged-panel{padding-bottom:195px}.panel-toolbar{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);padding:var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.panel-toolbar label{display:flex;align-items:center;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.template-row,.message-row{display:grid;height:65px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);border-bottom:1px solid var(--color-border)}.template-row{grid-template-columns:1fr 1.5fr 90px 150px}.message-row{grid-template-columns:80px 120px minmax(0,1fr) 100px 150px}.template-row span,.template-row strong,.template-row small,.template-row time,.message-row>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-copy{display:grid;min-width:0}.message-copy strong,.message-copy small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-row small,.template-row time,.message-row,.message-copy small{color:var(--color-text-muted);font-size:var(--font-size-sm)}.pagination{display:flex;align-items:center;justify-content:center;gap:var(--space-2);padding:var(--space-3);border-top:1px solid var(--color-border)}.pagination button{min-width:36px;min-height:36px;color:var(--color-text-muted);background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-md)}.pagination button.active{color:var(--color-surface);background:var(--color-primary);border-color:var(--color-primary)}button:focus-visible,select:focus-visible,input:focus-visible,textarea:focus-visible{outline:none;box-shadow:var(--shadow-focus)}@media(max-width:900px){.message-row{grid-template-columns:70px 90px minmax(180px,1fr) 80px 130px}.content-panel{overflow-x:auto}.template-row,.message-row,.panel-toolbar,.pagination{min-width:760px}}
.message-row,.message-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.message-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.message-label{display:grid;min-width:0}.message-label>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.message-label small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.message-columns{min-width:760px}}
.template-row,.template-columns{grid-template-columns:80px 110px minmax(150px,.9fr) minmax(240px,1.8fr) 150px}.template-columns{display:grid;min-height:44px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4);color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.template-label,.template-copy{display:grid;min-width:0}.template-label>*,.template-copy>*{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-label small,.template-copy small{color:var(--color-text-muted);font-size:var(--font-size-xs)}@media(max-width:900px){.template-columns{min-width:760px}}
.template-actions-bar{display:flex;min-height:60px;align-items:center;justify-content:space-between;gap:var(--space-3);padding:var(--space-3) var(--space-4);background:var(--color-surface-soft);border-bottom:1px solid var(--color-border)}.template-actions-bar>label,.template-actions-bar>div,.template-row-actions{display:flex;align-items:center;gap:var(--space-2)}.template-actions-bar button,.template-row-actions button{display:inline-flex;min-height:36px;align-items:center;justify-content:center;gap:var(--space-1);padding-inline:var(--space-3);font-weight:600;border-radius:var(--radius-md)}.template-actions-bar button{min-width:132px}.template-add-action,.template-edit-action{color:var(--color-surface);background:var(--color-primary);border:1px solid var(--color-primary)}.template-batch-delete,.template-delete-action{color:var(--color-danger);background:var(--color-surface);border:1px solid var(--color-danger)}.template-batch-delete:disabled{color:var(--color-text-subtle);background:var(--color-disabled-bg);border-color:var(--color-disabled)}.template-row,.template-columns{grid-template-columns:42px 74px 105px minmax(145px,.8fr) minmax(210px,1.4fr) 145px 176px}.template-check{display:grid;height:65px;place-items:center}.template-row-actions{overflow:visible}.template-row-actions button{padding-inline:var(--space-2)}@media(max-width:1100px){.template-row,.template-columns,.template-actions-bar{min-width:1000px}}
.admin-template-toolbar{display:flex;align-items:center;justify-content:flex-end;gap:var(--space-2)}.admin-template-toolbar label{display:block}.admin-template-toolbar select,.admin-template-toolbar button{width:112px;height:32px;min-height:32px;padding:0 var(--space-2);font-size:var(--font-size-sm);border-radius:var(--radius-md)}.admin-template-toolbar select{background:var(--color-surface);border:1px solid var(--color-border-strong)}.admin-template-toolbar button{display:inline-flex;align-items:center;justify-content:center;gap:var(--space-1);font-weight:600}.template-row,.template-columns{grid-template-columns:80px 80px 110px 160px minmax(240px,1fr) 170px 176px}.template-columns>label{display:inline-flex;align-items:center;gap:var(--space-2);white-space:nowrap}.template-modified{display:grid;min-width:0;gap:0}.template-modified time,.template-modified small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.template-modified small{color:var(--color-text-muted);font-size:var(--font-size-xs)}.template-row-actions button{width:80px;height:28px;min-height:28px;padding:0 var(--space-2)}@media(max-width:1100px){.template-row,.template-columns,.panel-toolbar{min-width:1050px}}
.template-feedback{margin:0;padding:var(--space-5);color:var(--color-text-muted);text-align:center}.template-feedback.error{color:var(--color-danger)}
.column-sort{display:flex;align-items:center;justify-content:flex-start;gap:var(--space-1);padding:0;color:inherit;font:inherit;text-align:left;background:transparent;border:0}.column-sort i{color:var(--color-text-subtle);font-size:14px;line-height:1}.column-sort.active{color:var(--color-primary-active)}.column-sort.active i{color:var(--color-primary)}
.overview-filters,.overview-columns,.overview-row{display:grid;grid-template-columns:70px 145px 85px 145px 120px 100px minmax(260px,1fr) 160px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4)}.overview-filters{min-height:82px;background:var(--color-bg-muted)}.overview-columns{min-height:36px;color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.overview-columns>span:not(:nth-child(7)),.overview-row>span:not(:nth-child(7)),.overview-row>time{text-align:center}.overview-filters label{display:block;position:relative;min-width:0}.function-filter-stack{display:grid;gap:5px}.overview-filters input,.overview-filters select{width:100%;height:30px;min-width:0;padding:2px 24px 2px 7px;color:var(--color-text);font-size:var(--font-size-xs);background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-sm)}.overview-filters select{appearance:none}.overview-filters input:hover,.overview-filters select:hover{border-color:var(--color-primary)}.overview-filters input:focus-visible,.overview-filters select:focus-visible{outline:0;border-color:var(--color-primary);box-shadow:var(--shadow-focus)}.overview-filters label>i{position:absolute;top:50%;right:7px;color:var(--color-text-muted);font-size:11px;pointer-events:none;transform:translateY(-50%)}.overview-row{height:65px;border-bottom:1px solid var(--color-border)}.overview-row>span,.overview-row time,.overview-row strong,.overview-row small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.overview-row time,.overview-row small{color:var(--color-text-muted);font-size:var(--font-size-sm)}@media(max-width:1200px){.overview-filters,.overview-columns,.overview-row{min-width:1220px}}
.overview-filters{min-height:50px}.function-filter-pair{display:grid;grid-template-columns:minmax(0,1fr) auto minmax(0,1fr);align-items:center;gap:4px}.function-filter-pair>b{color:var(--color-text-muted);font-size:var(--font-size-sm);text-align:center}
.filter-combobox>.combobox-menu{position:absolute;z-index:-1;inset:0;width:100%;height:30px;padding:0;pointer-events:none;opacity:0}.filter-combobox>.combobox-trigger{position:absolute;z-index:2;top:0;right:0;width:30px;height:30px;padding:0;color:var(--color-primary-700);background:var(--color-primary-soft);border:0;border-left:1px solid var(--color-border);border-radius:0 var(--radius-sm) var(--radius-sm) 0}.filter-combobox>.combobox-trigger i{font-size:11px}.filter-combobox>.combobox-trigger:hover{color:var(--color-primary-active);background:var(--color-primary-200)}.filter-combobox>.combobox-trigger:focus-visible{outline:0;box-shadow:var(--shadow-focus)}.filter-combobox:focus-within>input{border-color:var(--color-primary);box-shadow:var(--shadow-focus)}
.unified-select{position:relative}.unified-select>select{appearance:none;padding-right:34px!important}.unified-select>i{position:absolute!important;z-index:1;top:1px!important;right:1px!important;display:grid;width:29px;height:calc(100% - 2px);place-items:center;color:var(--color-primary-700)!important;font-size:11px!important;background:var(--color-primary-soft);border-left:1px solid var(--color-border);border-radius:0 var(--radius-sm) var(--radius-sm) 0;pointer-events:none;transform:none!important}.unified-select:hover>i{background:var(--color-primary-200)}.unified-select:focus-within>i{color:var(--color-primary-active)!important;background:var(--color-primary-200)}.admin-template-toolbar .unified-select{width:112px;height:32px}.admin-template-toolbar .unified-select>select{width:100%;height:100%}.admin-template-toolbar .unified-select>i{border-radius:0 var(--radius-md) var(--radius-md) 0}
.template-row[role="button"]{cursor:pointer}.template-row[role="button"]:hover,.template-row[role="button"]:focus-visible{background:var(--color-primary-soft)}.template-detail-overlay{position:fixed;z-index:1050;inset:0;display:grid;overflow-y:auto;place-items:center;padding:var(--space-5);background:color-mix(in srgb,var(--color-text) 65%,transparent)}.admin-template-dialog{position:relative;display:grid;width:min(100%,720px);max-height:calc(100vh - (2 * var(--space-5)));overflow-y:auto;overscroll-behavior:contain;gap:var(--space-4);padding:var(--space-6);background:var(--color-surface);border-radius:var(--radius-lg);box-shadow:var(--shadow-card)}.admin-template-dialog__close{position:absolute;top:var(--space-3);right:var(--space-3);width:var(--space-7);height:var(--space-7);padding:0;color:var(--color-text-muted);font-size:var(--font-size-xl);background:transparent;border:0}.admin-template-dialog>header{display:grid;gap:var(--space-1);padding-right:var(--space-7)}.admin-template-dialog>header small,.template-detail-field>label{color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:600}.admin-template-dialog>header p,.admin-template-dialog>header h2{margin:0}.admin-template-dialog>header p{font-size:var(--font-size-base);font-weight:600}.admin-template-dialog>header h2{font-family:var(--font-body);font-size:var(--font-size-xl);line-height:var(--line-height-heading)}.admin-template-dialog__actions{display:flex;justify-content:flex-end;gap:var(--space-2)}.admin-template-dialog__actions button{display:inline-flex;width:80px;height:28px;align-items:center;justify-content:center;gap:var(--space-1);font-weight:600;border-radius:var(--radius-md)}.admin-template-dialog>hr{width:100%;margin:0;color:var(--color-border);opacity:1}.template-detail-field{position:relative;display:grid;gap:var(--space-2)}.template-detail-field>div{min-height:112px;overflow-wrap:anywhere;padding:var(--space-3);white-space:pre-wrap;background:var(--color-surface-soft);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.template-detail-field>small{position:absolute;right:var(--space-2);bottom:var(--space-1);color:var(--color-text-subtle);font-size:var(--font-size-xs)}
.admin-template-create-dialog>h2{margin:0;padding-right:var(--space-7);font-family:var(--font-heading);font-size:var(--font-size-xl)}.admin-template-create-dialog>label{display:grid;gap:var(--space-2);color:var(--color-text-muted);font-size:var(--font-size-sm)}.admin-template-create-dialog input,.admin-template-create-dialog select,.admin-template-create-dialog textarea{width:100%;padding:var(--space-3);font:inherit;color:var(--color-text);background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-md)}.template-create-textarea{position:relative}.template-create-textarea textarea{padding-bottom:var(--space-5)}.template-create-textarea>small{position:absolute;right:var(--space-2);bottom:var(--space-1);color:var(--color-text-subtle);font-size:var(--font-size-xs)}.template-create-error{margin:0;padding:var(--space-2) var(--space-3);color:var(--color-danger);font-size:var(--font-size-sm);background:var(--color-danger-soft);border-radius:var(--radius-md)}.template-create-save{justify-self:center;min-width:112px}
.template-create-heading{display:flex;align-items:center;justify-content:space-between;gap:var(--space-4);padding-right:var(--space-7)}.template-create-heading h2{margin:0;font-family:var(--font-heading);font-size:var(--font-size-xl)}
.create-save-template{display:flex!important;align-items:center;gap:var(--space-2);color:var(--color-text)!important;font-size:var(--font-size-base)!important}.create-save-template input{width:18px;height:18px;padding:0}.create-message-textarea{position:relative}.create-message-textarea textarea{padding-bottom:var(--space-5)}.create-message-textarea small{position:absolute;right:var(--space-2);bottom:var(--space-1);color:var(--color-text-subtle);font-size:var(--font-size-xs)}.create-message-submit{justify-self:center;min-width:112px}
.record-filters,.record-columns,.record-row{display:grid;grid-template-columns:80px 150px 100px 140px 110px 110px minmax(280px,1fr) 170px;align-items:center;gap:var(--space-3);padding-inline:var(--space-4)}.record-filters{min-height:50px;background:var(--color-bg-muted)}.record-filters label{position:relative;display:block;min-width:0}.record-filters input,.record-filters select{width:100%;height:30px;min-width:0;padding:2px 24px 2px 7px;font:inherit;font-size:var(--font-size-xs);background:var(--color-surface);border:1px solid var(--color-border-strong);border-radius:var(--radius-sm)}.record-columns{min-height:36px;color:var(--color-text-muted);font-size:var(--font-size-xs);font-weight:700;background:var(--color-bg-muted);border-bottom:1px solid var(--color-border)}.record-row{height:65px;border-bottom:1px solid var(--color-border)}.record-columns>span:not(:nth-child(7)),.record-row>span:not(:nth-child(7)),.record-row>time{text-align:center}.record-row>span,.record-row time,.record-row strong,.record-row small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.record-row time,.record-row small{color:var(--color-text-muted);font-size:var(--font-size-sm)}@media(max-width:1200px){.record-filters,.record-columns,.record-row{min-width:1250px}}
</style>
