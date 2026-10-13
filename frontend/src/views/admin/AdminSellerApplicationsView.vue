<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSellerApplications } from '@/api/sellerApplicationApi'

const route = useRoute()
const router = useRouter()
const statuses = [
  { value: 'PENDING', label: '待審核' },
  { value: 'APPROVED', label: '已核准' },
  { value: 'REJECTED', label: '已駁回' },
]
const allowedStatuses = new Set(statuses.map((status) => status.value))
const activeStatus = ref(allowedStatuses.has(route.query.status) ? route.query.status : 'PENDING')
const searchQuery = ref('')
const feedback = ref('')
const isLoading = ref(false)
const applicationsByStatus = reactive({ PENDING: [], APPROVED: [], REJECTED: [] })

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-TW') : '-')
const statusLabel = (status) => statuses.find((item) => item.value === status)?.label || status
const mapApplication = (application) => ({
  id: application.applicationId,
  applicant: `會員 #${application.memberId}`,
  email: '-',
  storeName: application.storeName,
  submittedAt: formatDate(application.createdAt),
  status: application.status,
})
const applications = computed(() => applicationsByStatus[activeStatus.value])
const statusCounts = computed(() => Object.fromEntries(
  statuses.map(({ value }) => [value, applicationsByStatus[value].length]),
))

async function loadApplications() {
  isLoading.value = true
  feedback.value = ''
  try {
    const responses = await Promise.all(statuses.map(({ value }) => listSellerApplications(value)))
    statuses.forEach(({ value }, index) => {
      applicationsByStatus[value] = responses[index].data.map(mapApplication)
    })
  } catch (error) {
    feedback.value = error.response?.data?.message || '申請列表載入失敗。'
  } finally {
    isLoading.value = false
  }
}

function selectStatus(status) {
  activeStatus.value = status
  searchQuery.value = ''
  router.replace({ query: status === 'PENDING' ? {} : { status } })
}

onMounted(loadApplications)

const filteredApplications = computed(() => {
  const keyword = searchQuery.value.trim().toLocaleLowerCase()
  if (!keyword) return applications.value

  return applications.value.filter((application) =>
    [application.applicant, application.email, application.storeName].some((value) =>
      value.toLocaleLowerCase().includes(keyword),
    ),
  )
})
</script>

<template>
  <section class="admin-seller-applications" aria-labelledby="admin-seller-applications-title">
    <header class="admin-seller-applications__header">
      <div>
        <p class="admin-seller-applications__eyebrow">商家會員</p>
        <h1 id="admin-seller-applications-title">商家申請審核</h1>
        <p>依申請資料與會員資訊完成核准或駁回。</p>
      </div>
      <strong>待處理 {{ statusCounts.PENDING }} 件</strong>
    </header>

    <div class="admin-seller-applications__tabs" role="tablist" aria-label="申請狀態">
      <button
        v-for="status in statuses"
        :key="status.value"
        :class="{ 'is-active': activeStatus === status.value }"
        type="button"
        role="tab"
        :aria-selected="activeStatus === status.value"
        @click="selectStatus(status.value)"
      >{{ status.label }}（{{ statusCounts[status.value] }}）</button>
    </div>

    <div class="admin-seller-applications__filters">
      <label class="admin-seller-applications__search" for="seller-application-search">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input
          id="seller-application-search"
          v-model="searchQuery"
          type="search"
          placeholder="搜尋店鋪名稱或會員編號"
        />
      </label>
      <div class="admin-seller-applications__filter-actions">
        <button type="button" disabled>依送出時間排序</button>
        <button type="button" disabled>匯出清單</button>
      </div>
    </div>

    <div class="admin-seller-applications__table-card">
      <div
        class="admin-seller-applications__table admin-seller-applications__table--header"
        role="row"
      >
        <span role="columnheader">申請人</span>
        <span role="columnheader">店鋪名稱</span>
        <span role="columnheader">送出時間</span>
        <span role="columnheader">狀態</span>
        <span role="columnheader">操作</span>
      </div>

      <div
        v-for="application in filteredApplications"
        :key="application.id"
        class="admin-seller-applications__table admin-seller-applications__table--row"
        role="row"
      >
        <div class="admin-seller-applications__applicant" role="cell">
          <strong>{{ application.applicant }}</strong>
          <span>{{ application.email }}</span>
        </div>
        <span role="cell">{{ application.storeName }}</span>
        <span role="cell">{{ application.submittedAt }}</span>
        <span role="cell"><b>{{ statusLabel(application.status) }}</b></span>
        <span class="admin-seller-applications__review" role="cell">
          <RouterLink
            class="admin-seller-applications__review-link dg-btn-primary dg-focus-ring"
            :to="{ name: 'AdminSellerApplicationDetail', params: { id: application.id } }"
          >
            {{ application.status === 'PENDING' ? '審核申請' : '查看詳情' }}
          </RouterLink>
        </span>
      </div>

      <p v-if="feedback" class="admin-seller-applications__empty" role="status">{{ feedback }}</p>
      <p v-else-if="isLoading" class="admin-seller-applications__empty" role="status">申請列表載入中。</p>
      <p v-else-if="!filteredApplications.length" class="admin-seller-applications__empty" role="status">
        {{ searchQuery ? '找不到符合條件的申請。' : `目前沒有${statusLabel(activeStatus)}申請。` }}
      </p>
    </div>

    <footer class="admin-seller-applications__footer">
      <p>顯示 {{ filteredApplications.length }} 筆，共 {{ applications.length }} 筆{{ statusLabel(activeStatus) }}申請</p>
    </footer>
  </section>
</template>

<style scoped>
.admin-seller-applications {
  display: grid;
  gap: var(--space-5);
  color: var(--color-text);
  font-family: var(--font-body);
}

.admin-seller-applications__header,
.admin-seller-applications__filters,
.admin-seller-applications__footer {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-5);
}

.admin-seller-applications__header > div {
  display: grid;
  gap: 6px;
}

.admin-seller-applications p,
h1 {
  margin: 0;
}

.admin-seller-applications__eyebrow,
.admin-seller-applications__header > strong {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.admin-seller-applications__header > div > p:last-child,
.admin-seller-applications__footer p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.admin-seller-applications__tabs,
.admin-seller-applications__filter-actions {
  display: flex;
  gap: var(--space-2);
}

.admin-seller-applications__tabs button,
.admin-seller-applications__filter-actions button {
  min-height: 38px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  color: var(--color-text-muted);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
}

.admin-seller-applications__tabs button.is-active {
  border-color: var(--color-primary-active);
  color: var(--color-surface);
  background: var(--color-primary-active);
  font-weight: 700;
}

.admin-seller-applications__filter-actions button {
  min-height: 44px;
  padding-inline: 18px;
  color: var(--color-text);
  font-weight: 600;
}

.admin-seller-applications button:not(:disabled):hover,
.admin-seller-applications button:not(:disabled):focus-visible {
  border-color: var(--color-primary);
}

.admin-seller-applications button:focus-visible,
.admin-seller-applications__search input:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.admin-seller-applications__search {
  display: flex;
  width: 360px;
  min-height: 42px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  color: var(--color-text-muted);
  background: var(--color-surface);
}

.admin-seller-applications__search i {
  font-size: 18px;
}

.admin-seller-applications__search input {
  width: 100%;
  border: 0;
  color: var(--color-text);
  background: transparent;
  font: inherit;
  font-size: 13px;
}

.admin-seller-applications__search input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}

.admin-seller-applications__table-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.admin-seller-applications__table {
  display: grid;
  grid-template-columns: 260px 220px 180px 140px minmax(136px, 1fr);
  align-items: center;
  padding: 0 var(--space-5);
}

.admin-seller-applications__table--header {
  min-height: 48px;
  color: var(--color-text);
  background: var(--color-bg);
  font-size: 13px;
  font-weight: 700;
}

.admin-seller-applications__table--row {
  min-height: 72px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 13px;
}

.admin-seller-applications__table--row:last-of-type {
  border-bottom: 0;
}

.admin-seller-applications__applicant {
  display: grid;
  gap: 2px;
}

.admin-seller-applications__applicant strong {
  color: var(--color-text);
  font-weight: 700;
}

.admin-seller-applications__applicant span {
  color: var(--color-text-muted);
}

.admin-seller-applications__table--row b {
  display: inline-flex;
  border-radius: 14px;
  padding: 5px 10px;
  color: var(--color-primary-active);
  background: var(--color-bg);
  font-weight: 700;
}

.admin-seller-applications__review {
  display: flex;
  justify-content: end;
}

.admin-seller-applications__review-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: 0 18px;
  color: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-decoration: none;
}

.admin-seller-applications__empty {
  padding: var(--space-6);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}

.admin-seller-applications__footer {
  align-items: center;
}

.admin-seller-applications__footer > span {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
}

@media (max-width: 1100px) {
  .admin-seller-applications__table-card {
    overflow-x: auto;
  }

  .admin-seller-applications__table {
    min-width: 976px;
  }
}

@media (max-width: 760px) {
  .admin-seller-applications__header,
  .admin-seller-applications__filters {
    align-items: start;
    flex-direction: column;
  }

  .admin-seller-applications__search {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .admin-seller-applications__tabs,
  .admin-seller-applications__filter-actions {
    width: 100%;
    overflow-x: auto;
  }

  .admin-seller-applications__tabs button,
.admin-seller-applications__filter-actions button {
    flex: 0 0 auto;
  }

  .admin-seller-applications__footer {
    align-items: start;
    flex-direction: column;
  }
}

.admin-seller-applications__filter-actions button:disabled {
  cursor: not-allowed;
  opacity: .55;
}
</style>
