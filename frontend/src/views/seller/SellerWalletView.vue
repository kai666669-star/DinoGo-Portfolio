<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createSellerWalletWithdrawal,
  getSellerWallet,
  getSellerWalletTransactions,
} from '@/api/sellerWalletApi'

const wallet = ref({
  availableBalance: 0,
  pendingBalance: 0,
  withdrawnBalance: 0,
  bankName: null,
  bankAccountLast4: null,
  bankVerified: false,
})

const transactions = ref([])
const isLoading = ref(false)
const isWithdrawing = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showBankModal = ref(false)
const currentDate = new Date()

const filters = reactive({
  year: currentDate.getFullYear(),
  month: currentDate.getMonth() + 1,
  status: 'all',
  keyword: '',
})

const transactionStatuses = [
  { label: '全部', value: 'all' },
  { label: '待入帳', value: 'PENDING' },
  { label: '已入帳', value: 'AVAILABLE' },
]

const monthOptions = Array.from({ length: 12 }, (_, index) => ({
  label: `${index + 1} 月`,
  value: index + 1,
}))

const yearOptions = computed(() => {
  const years = new Set([currentDate.getFullYear()])

  transactions.value.forEach((item) => {
    if (item.year) {
      years.add(item.year)
    }
  })

  return Array.from(years).sort((a, b) => b - a)
})

const monthlyTransactions = computed(() => {
  return transactions.value.filter((item) => {
    return item.year === Number(filters.year) && item.month === Number(filters.month)
  })
})

const filteredTransactions = computed(() => {
  return monthlyTransactions.value.filter((item) => {
    const matchesStatus = filters.status === 'all' || item.statusValue === filters.status
    const keyword = filters.keyword.trim().toLowerCase()
    const matchesKeyword =
      !keyword ||
      item.orderNo.toLowerCase().includes(keyword) ||
      item.id.toLowerCase().includes(keyword)

    return matchesStatus && matchesKeyword
  })
})

const hasTransactions = computed(() => filteredTransactions.value.length > 0)

const transactionSummary = computed(() => {
  const availableCount = monthlyTransactions.value.filter((item) => item.statusValue === 'AVAILABLE').length
  const pendingCount = monthlyTransactions.value.filter((item) => item.statusValue === 'PENDING').length

  return {
    totalCount: monthlyTransactions.value.length,
    availableCount,
    pendingCount,
  }
})

const bankInitial = computed(() => {
  return wallet.value.bankName?.slice(0, 1) || '銀'
})

const hasWithdrawableBalance = computed(() => Number(wallet.value.availableBalance || 0) > 0)

function formatCurrency(value) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : amount < 0 ? '-' : ''
  return `${prefix}NT$${Math.abs(amount).toLocaleString('zh-TW')}`
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-TW')
}

function formatDate(value) {
  if (!value) {
    return '-'
  }

  return value.slice(0, 10).replaceAll('-', '/')
}

function mapTransactionStatus(status) {
  const statusMap = {
    AVAILABLE: '已入帳',
    PENDING: '待入帳',
  }

  return statusMap[status] || status
}

function mapTransactionType(type) {
  const typeMap = {
    ORDER_INCOME: '訂單收入',
  }

  return typeMap[type] || type
}

function getApiErrorMessage(error, fallbackMessage) {
  const data = error.response?.data

  if (typeof data === 'string') {
    return data
  }

  return data?.message || data?.error || fallbackMessage
}

async function handleWithdrawClick() {
  if (!hasWithdrawableBalance.value || isWithdrawing.value) {
    return
  }

  isWithdrawing.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await createSellerWalletWithdrawal()
    wallet.value = response.data.wallet
    successMessage.value = `已送出提款申請 NT$${formatAmount(response.data.amount)}，平台通知已建立。`
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '目前無法送出提款申請，請稍後再試。')
  } finally {
    isWithdrawing.value = false
  }
}

function getStatusClass(status) {
  return {
    'status-pill': true,
    available: status === 'AVAILABLE',
    pending: status === 'PENDING',
  }
}

async function loadWallet() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    successMessage.value = ''
    const walletResponse = await getSellerWallet()
    wallet.value = walletResponse.data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(
      error,
      '目前無法載入錢包總覽，請確認後端已重新啟動並使用賣家帳號登入。',
    )
    isLoading.value = false
    return
  }

  try {
    const transactionResponse = await getSellerWalletTransactions()
    transactions.value = transactionResponse.data.map((item) => ({
      id: `ORDER-${item.orderId}`,
      orderNo: item.orderNo,
      occurredAt: item.occurredAt,
      year: item.occurredAt ? Number(item.occurredAt.slice(0, 4)) : null,
      month: item.occurredAt ? Number(item.occurredAt.slice(5, 7)) : null,
      date: formatDate(item.occurredAt),
      type: mapTransactionType(item.transactionType),
      typeValue: item.transactionType,
      direction: item.direction,
      amount: item.amount,
      status: mapTransactionStatus(item.status),
      statusValue: item.status,
    }))
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '目前無法載入交易紀錄，請稍後再試。')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadWallet)
</script>

<template>
  <section class="seller-page wallet-page">
    <header class="page-header">
      <div>
        <p class="section-kicker">Seller Wallet</p>
        <h1>賣家錢包</h1>
        <p class="page-description">管理提領金額、銀行帳號與近期交易紀錄。</p>
      </div>
    </header>

    <section class="wallet-block">
      <h2>金額總覽</h2>

      <div class="balance-card">
        <div class="balance-main">
          <span>可提領金額</span>
          <div class="balance-value">
            <strong>NT${{ formatAmount(wallet.availableBalance) }}</strong>
            <button
              class="withdraw-button"
              type="button"
              :class="{ inactive: !hasWithdrawableBalance }"
              :disabled="!hasWithdrawableBalance || isWithdrawing"
              @click="handleWithdrawClick"
            >
              {{ isWithdrawing ? '處理中' : '提款' }}
            </button>
          </div>
          <p>待入帳金額 NT${{ formatAmount(wallet.pendingBalance) }}</p>
          <div class="balance-breakdown">
            <span>
              <b>已提領</b>
              NT${{ formatAmount(wallet.withdrawnBalance) }}
            </span>
            <span>
              <b>待入帳</b>
              NT${{ formatAmount(wallet.pendingBalance) }}
            </span>
          </div>
        </div>

        <div class="bank-panel">
          <div class="bank-heading">
            <h3>我的銀行帳號</h3>
            <button class="link-button" type="button" @click="showBankModal = true">
              更多 <i class="bi bi-chevron-right"></i>
            </button>
          </div>
          <div v-if="wallet.bankName" class="bank-account">
            <div class="bank-logo">{{ bankInitial }}</div>
            <div>
              <strong>{{ wallet.bankName }}</strong>
              <p>
                <span v-if="wallet.bankVerified">已驗證</span>
                <b>預設</b>
                **** {{ wallet.bankAccountLast4 }}
              </p>
            </div>
          </div>
          <div v-else class="bank-empty">
            <i class="bi bi-bank" aria-hidden="true"></i>
            <span>尚未設定提領銀行帳號</span>
          </div>
        </div>
      </div>
    </section>

    <section class="wallet-block transaction-block">
      <div class="section-title-row">
        <div>
          <h2>最近的交易</h2>
          <p>由訂單狀態同步產生，完成訂單會列入可提領金額。</p>
        </div>
        <button class="secondary-button" type="button" @click="loadWallet">
          重新整理
        </button>
      </div>

      <div class="transaction-summary">
        <article>
          <span>全部交易</span>
          <strong>{{ transactionSummary.totalCount }}</strong>
        </article>
        <article>
          <span>待入帳</span>
          <strong>{{ transactionSummary.pendingCount }}</strong>
        </article>
        <article>
          <span>已入帳</span>
          <strong>{{ transactionSummary.availableCount }}</strong>
        </article>
      </div>

      <div class="filter-panel">
        <div class="filter-field date-filter">
          <span>交易日期</span>
          <div class="date-select-row">
            <select v-model.number="filters.year">
              <option v-for="year in yearOptions" :key="year" :value="year">
                {{ year }} 年
              </option>
            </select>
            <select v-model.number="filters.month">
              <option v-for="month in monthOptions" :key="month.value" :value="month.value">
                {{ month.label }}
              </option>
            </select>
          </div>
        </div>

        <div class="filter-field segmented-row" aria-label="入帳狀態">
          <span>入帳狀態</span>
          <div class="segmented-control">
            <button
              v-for="item in transactionStatuses"
              :key="item.value"
              type="button"
              :class="{ active: filters.status === item.value }"
              @click="filters.status = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </div>

      <div class="transaction-tools">
        <label class="search-box">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="filters.keyword" type="search" placeholder="搜尋訂單編號" />
        </label>
      </div>

      <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <div v-if="isLoading" class="empty-state">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
        <p>錢包資料載入中</p>
      </div>

      <div v-else-if="hasTransactions" class="transaction-table">
        <div class="table-head">
          <span>日期</span>
          <span>訂單/交易編號</span>
          <span>交易類型</span>
          <span>狀態</span>
          <span>金額</span>
        </div>

        <article v-for="item in filteredTransactions" :key="item.id" class="table-row">
          <span>{{ item.date }}</span>
          <strong>{{ item.orderNo }}</strong>
          <span>{{ item.type }}</span>
          <span :class="getStatusClass(item.statusValue)">{{ item.status }}</span>
          <strong :class="item.direction === 'income' ? 'income' : 'expense'">
            {{ formatCurrency(item.amount) }}
          </strong>
        </article>
      </div>

      <div v-else class="empty-state">
        <i class="bi bi-file-earmark-bar-graph" aria-hidden="true"></i>
        <p>無交易紀錄</p>
      </div>
    </section>

    <div v-if="showBankModal" class="modal-backdrop" @click.self="showBankModal = false">
      <section class="wallet-modal bank-modal" role="dialog" aria-modal="true" aria-labelledby="bankTitle">
        <button class="modal-close" type="button" aria-label="關閉" @click="showBankModal = false">
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>

        <p class="section-kicker">Bank Account</p>
        <h2 id="bankTitle">銀行帳號</h2>

        <div v-if="wallet.bankName" class="bank-detail-card">
          <div class="bank-logo">{{ bankInitial }}</div>
          <div>
            <strong>{{ wallet.bankName }}</strong>
            <p>
              <span v-if="wallet.bankVerified">已驗證</span>
              預設提款帳號 · **** {{ wallet.bankAccountLast4 }}
            </p>
          </div>
        </div>

        <div v-else class="bank-empty large">
          <i class="bi bi-bank" aria-hidden="true"></i>
          <span>尚未設定提領銀行帳號</span>
        </div>

        <button class="secondary-button full-button" type="button" disabled>
          新增或變更銀行帳號
        </button>
      </section>
    </div>
  </section>
</template>

<style scoped>
.wallet-page {
  display: grid;
  gap: var(--space-5);
  width: 100%;
  max-width: none;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-kicker,
.page-description,
.balance-main span,
.balance-main p,
.section-title-row p,
.transaction-summary span,
.filter-panel span,
.bank-account p,
.table-head,
.table-row span,
.empty-state p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.section-kicker,
h1,
h2,
h3,
p {
  margin-top: 0;
}

.section-kicker {
  margin-bottom: var(--space-1);
  font-weight: 800;
}

h1,
h2,
h3 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
}

h1 {
  margin-bottom: var(--space-1);
  font-size: var(--font-size-xl);
}

h2 {
  margin-bottom: var(--space-4);
  font-size: var(--font-size-lg);
}

h3 {
  margin-bottom: 0;
  font-size: var(--font-size-md);
}

.page-description,
.balance-main p,
.section-title-row p,
.bank-account p,
.empty-state p {
  margin-bottom: 0;
}

.wallet-block {
  width: 100%;
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.balance-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  gap: var(--space-5);
  min-height: 128px;
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.balance-main {
  display: grid;
  align-content: center;
  gap: var(--space-2);
}

.balance-value {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.balance-value strong {
  color: var(--color-text-900);
  font-size: 32px;
  line-height: 1;
}

.balance-breakdown {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.balance-breakdown span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: 32px;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  background: var(--color-bg-muted);
  color: var(--color-text-700);
}

.balance-breakdown b {
  color: var(--color-text-muted);
}

.withdraw-button {
  min-height: 40px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0 var(--space-5);
  background: #ef604f;
  color: var(--color-surface);
  font-weight: 800;
}

.withdraw-button.inactive {
  background: #ef604f;
  opacity: 0.82;
}

.withdraw-button:disabled {
  cursor: not-allowed;
}

.bank-panel {
  display: grid;
  align-content: center;
  gap: var(--space-4);
  min-width: 0;
  padding-left: var(--space-5);
  border-left: 1px solid var(--color-border);
}

.bank-heading,
.bank-account,
.bank-empty,
.transaction-tools,
.search-box {
  display: flex;
  align-items: center;
}

.bank-heading {
  justify-content: space-between;
}

.bank-account {
  gap: var(--space-3);
  min-width: 0;
}

.bank-empty {
  min-height: 56px;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.bank-empty i {
  color: var(--color-primary-active);
  font-size: 22px;
}

.bank-logo {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #e84c3d;
  color: var(--color-surface);
  font-weight: 900;
}

.bank-account strong {
  display: block;
  margin-bottom: 4px;
  color: var(--color-primary-active);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-account b {
  margin: 0 var(--space-2);
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
}

.link-button,
.secondary-button,
.segmented-control button {
  min-height: 36px;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-weight: 800;
}

.link-button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border: 0;
  color: var(--color-primary-active);
}

.transaction-block {
  display: grid;
  gap: var(--space-4);
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-title-row h2 {
  margin-bottom: var(--space-1);
}

.transaction-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.transaction-summary article {
  display: grid;
  gap: var(--space-1);
  min-height: 86px;
  align-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-bg-muted);
}

.transaction-summary strong {
  color: var(--color-text-900);
  font-size: 28px;
  line-height: 1;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(240px, 1fr);
  gap: var(--space-4) var(--space-5);
  align-items: start;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.filter-field {
  display: grid;
  gap: var(--space-2);
}

.date-select-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(100px, 0.8fr);
  gap: var(--space-3);
}

.date-filter select {
  min-height: 38px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

.segmented-control {
  display: flex;
  flex-wrap: wrap;
}

.segmented-control button {
  border: 1px solid #ef604f;
  border-right: 0;
  color: #ef604f;
  white-space: nowrap;
}

.segmented-control button:first-child {
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
}

.segmented-control button:last-child {
  border-right: 1px solid #ef604f;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.segmented-control button.active {
  background: #ef604f;
  color: var(--color-surface);
}

.secondary-button {
  border: 1px solid var(--color-border-strong);
}

.transaction-tools {
  justify-content: flex-end;
  gap: var(--space-3);
}

.search-box {
  min-width: 260px;
  min-height: 38px;
  gap: var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  background: var(--color-surface);
}

.search-box input {
  width: 100%;
  border: 0;
  outline: 0;
  color: var(--color-text);
  font: inherit;
}

.transaction-table {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 140px minmax(180px, 1fr) 160px 120px 140px;
  align-items: center;
  gap: var(--space-3);
  min-width: 760px;
  padding: var(--space-3) var(--space-4);
}

.table-head {
  background: var(--color-bg-muted);
  font-weight: 800;
}

.table-row {
  min-height: 64px;
  border-top: 1px solid var(--color-border);
}

.table-row strong {
  color: var(--color-text-900);
}

.table-row .income {
  color: var(--color-primary-active);
}

.table-row .expense {
  color: #b6473d;
}

.status-pill {
  display: inline-flex;
  width: fit-content;
  min-height: 28px;
  align-items: center;
  border-radius: 999px;
  padding: 0 var(--space-3);
  font-weight: 800;
}

.status-pill.available {
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
}

.status-pill.pending {
  background: #fbf3df;
  color: #9a6a1d;
}

.empty-state {
  display: grid;
  min-height: 240px;
  place-items: center;
  align-content: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
}

.success-message,
.error-message {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-weight: 800;
}

.success-message {
  background: #edf8f2;
  color: #227447;
}

.error-message {
  background: #fff4f1;
  color: #b6473d;
}

.empty-state i {
  font-size: 56px;
  opacity: 0.4;
}

.modal-backdrop {
  position: fixed;
  z-index: 40;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: rgba(15, 23, 42, 0.42);
}

.wallet-modal {
  position: relative;
  display: grid;
  gap: var(--space-4);
  width: min(460px, 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-5);
  background: var(--color-surface);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
}

.wallet-modal h2 {
  margin-bottom: 0;
}

.modal-close {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-700);
}

.bank-detail-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-bg-muted);
}

.bank-detail-card p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.bank-detail-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.bank-detail-card strong {
  color: var(--color-primary-active);
}

.bank-empty.large {
  min-height: 120px;
  justify-content: center;
}

.full-button {
  width: 100%;
}

@media (max-width: 980px) {
  .balance-card,
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .bank-panel {
    padding-top: var(--space-4);
    padding-left: 0;
    border-top: 1px solid var(--color-border);
    border-left: 0;
  }

  .transaction-tools {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .wallet-block,
  .balance-card,
  .filter-panel {
    padding: var(--space-4);
  }

  .balance-value,
  .bank-heading,
  .section-title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .transaction-summary {
    grid-template-columns: 1fr;
  }

  .withdraw-button,
  .secondary-button {
    width: 100%;
  }

  .segmented-control button {
    flex: 1 1 33.333%;
  }
}
</style>
