<script setup>
import { computed, onMounted, ref } from 'vue'
import { listSellerApplications } from '@/api/sellerApplicationApi'

const pendingApplications = ref([])
const recentFeedback = ref('')
const isRecentLoading = ref(false)
const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-TW') : '-')

const overview = computed(() => [
  {
    label: '待審核商家申請',
    value: String(pendingApplications.value.length),
    detail: '目前待處理件數',
    icon: 'bi-clipboard-check',
  },
  { label: '本月新增商家', value: '2', icon: 'bi-shop' },
  { label: '平台會員數', value: '15', icon: 'bi-people' },
])

const recentApplications = computed(() => pendingApplications.value.slice(0, 3))

async function loadPendingApplications() {
  isRecentLoading.value = true
  recentFeedback.value = ''
  try {
    const response = await listSellerApplications('PENDING')
    pendingApplications.value = response.data
      .slice()
      .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
      .map((application) => ({
        id: application.applicationId,
        store: application.storeName,
        member: `會員 #${application.memberId}`,
        submittedAt: formatDate(application.createdAt),
      }))
  } catch (error) {
    recentFeedback.value = error.response?.data?.message || '待審核申請載入失敗。'
  } finally {
    isRecentLoading.value = false
  }
}

onMounted(loadPendingApplications)
</script>

<template>
  <section class="admin-dashboard" aria-labelledby="admin-dashboard-title">
    <header class="admin-dashboard__header">
      <div>
        <p>平台總覽</p>
        <h1 id="admin-dashboard-title">管理者總覽</h1>
        <span>以下數據為介面示範資料，尚未串接統計 API。</span>
      </div>
      <RouterLink
        class="admin-dashboard__action dg-btn-primary dg-focus-ring"
        to="/admin/seller-applications"
      >
        查看商家申請
      </RouterLink>
    </header>

    <div class="admin-dashboard__metrics">
      <article v-for="item in overview" :key="item.label" class="admin-dashboard__metric dg-card">
        <i class="bi" :class="item.icon" aria-hidden="true"></i>
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.detail }}</span>
      </article>
    </div>

    <article class="admin-dashboard__recent dg-card" aria-labelledby="admin-dashboard-recent-title">
      <header>
        <div>
          <h2 id="admin-dashboard-recent-title">最新待審核申請</h2>
          <p>依送出時間由新到舊顯示</p>
        </div>
        <RouterLink to="/admin/seller-applications">查看全部</RouterLink>
      </header>
      <div class="admin-dashboard__table" role="table" aria-label="最新待審核商家申請">
        <div class="admin-dashboard__row admin-dashboard__row--header" role="row">
          <span role="columnheader">店鋪名稱</span><span role="columnheader">申請會員</span
          ><span role="columnheader">送出時間</span>
        </div>
        <div
          v-for="application in recentApplications"
          :key="application.store"
          class="admin-dashboard__row"
          role="row"
        >
          <strong role="cell">{{ application.store }}</strong
          ><span role="cell">{{ application.member }}</span
          ><span role="cell">{{ application.submittedAt }}</span>
        </div>
        <p v-if="recentFeedback" class="admin-dashboard__empty" role="status">
          {{ recentFeedback }}
        </p>
        <p v-else-if="isRecentLoading" class="admin-dashboard__empty" role="status">
          待審核申請載入中。
        </p>
        <p v-else-if="!recentApplications.length" class="admin-dashboard__empty" role="status">
          目前沒有待審核商家申請。
        </p>
      </div>
    </article>
  </section>
</template>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: var(--space-5);
  color: var(--color-text);
  font-family: var(--font-body);
}
.admin-dashboard__header,
.admin-dashboard__recent > header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-5);
}
.admin-dashboard__header > div {
  display: grid;
  gap: 6px;
}
.admin-dashboard p,
h1,
h2 {
  margin: 0;
}
.admin-dashboard__header p {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.admin-dashboard h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.admin-dashboard__header span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.admin-dashboard__action {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-decoration: none;
}
.admin-dashboard__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}
.admin-dashboard__metric {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  transition: background-color 0.15s ease;
}
.admin-dashboard__metric:hover {
  background: var(--color-surface-soft);
}
.admin-dashboard__metric i {
  color: var(--color-primary-active);
  font-size: 22px;
}
.admin-dashboard__metric p,
.admin-dashboard__metric span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.admin-dashboard__metric strong {
  color: var(--color-text);
  font-size: 30px;
  line-height: 1;
}
.admin-dashboard__recent {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}
.admin-dashboard__recent header > div {
  display: grid;
  gap: var(--space-1);
}
.admin-dashboard__recent h2 {
  font-size: var(--font-size-md);
}
.admin-dashboard__recent header p {
  color: var(--color-text-muted);
  font-size: 13px;
}
.admin-dashboard__recent a {
  border-radius: var(--radius-md);
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-decoration: none;
}
.admin-dashboard__recent a:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}
.admin-dashboard__recent a:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
.admin-dashboard__table {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.admin-dashboard__row {
  display: grid;
  min-height: 58px;
  grid-template-columns: 1.2fr 1fr 1fr;
  align-items: center;
  gap: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  padding: 0 var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
.admin-dashboard__row:last-child {
  border-bottom: 0;
}
.admin-dashboard__row--header {
  min-height: 42px;
  color: var(--color-text);
  background: var(--color-bg);
  font-size: 13px;
  font-weight: 700;
}
.admin-dashboard__row strong {
  color: var(--color-text);
}
@media (max-width: 760px) {
  .admin-dashboard__header {
    align-items: start;
    flex-direction: column;
  }
  .admin-dashboard__metrics {
    grid-template-columns: 1fr;
  }
  .admin-dashboard__row {
    min-width: 580px;
  }
  .admin-dashboard__recent {
    overflow: auto;
  }
}
</style>
