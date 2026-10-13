<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getMySellerApplication } from '@/api/sellerApplicationApi'

const application = reactive({
  storeName: '',
  submittedAt: '',
  status: 'PENDING',
  rejectReason: '',
})
const apiError = ref('')
const isLoading = ref(true)

const statusLabel = computed(() => ({ PENDING: '審核中', APPROVED: '已核准', REJECTED: '已駁回' })[application.status] || application.status)
const statusTitle = computed(() => ({ PENDING: '你的申請正在審核中', APPROVED: '你的商家申請已核准', REJECTED: '你的商家申請未通過' })[application.status] || '商家申請狀態')
const progress = computed(() => ({ PENDING: '等待管理員審核', APPROVED: '商家資格已啟用，請重新登入', REJECTED: '請依駁回原因修正後重新送件' })[application.status] || '-')
const nextStep = computed(() => ({ PENDING: '核准後請重新登入，系統才會將商家角色寫入新的登入憑證。', APPROVED: '申請已核准；請重新登入，系統才會將商家角色寫入新的登入憑證。', REJECTED: '請依駁回原因修正公開資訊後，再次提出申請。' })[application.status] || '')
const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-TW') : '-')

async function loadApplication() {
  try {
    const { data } = await getMySellerApplication()
    application.storeName = data.storeName
    application.submittedAt = formatDate(data.createdAt)
    application.status = data.status
    application.rejectReason = data.rejectReason || ''
  } catch (error) {
    apiError.value = error.response?.data?.message || '申請狀態載入失敗，請稍後再試。'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadApplication)
</script>

<template>
  <section class="seller-application-status" aria-labelledby="seller-application-status-title">
    <div class="container seller-application-status__container">
      <p class="seller-application-status__eyebrow">商家會員申請</p>
      <h1 id="seller-application-status-title">{{ statusTitle }}</h1>
      <p class="seller-application-status__description">
        {{ application.status === 'PENDING' ? '平台通常會在 3–5 個工作天內完成審核；結果將透過站內通知告知。' : '請確認本次申請結果與後續操作。' }}
      </p>

      <p v-if="isLoading" class="seller-application-status__description" role="status">載入申請狀態中…</p>
      <p v-else-if="apiError" class="seller-application-status__error" role="alert">{{ apiError }}</p>
      <article
        v-else
        class="seller-application-status__card dg-card"
        aria-labelledby="application-pending-title"
      >
        <header class="seller-application-status__card-header">
          <span class="seller-application-status__icon" aria-hidden="true">
            <i class="bi bi-clock-history"></i>
          </span>
          <span id="application-pending-title" class="seller-application-status__badge"
            >{{ statusLabel }}</span
          >
        </header>

        <dl class="seller-application-status__details">
          <div>
            <dt>申請店鋪</dt>
            <dd>{{ application.storeName }}</dd>
          </div>
          <div>
            <dt>送出時間</dt>
            <dd>{{ application.submittedAt }}</dd>
          </div>
          <div>
            <dt>目前進度</dt>
            <dd>{{ progress }}</dd>
          </div>
          <div v-if="application.status === 'REJECTED'">
            <dt>駁回原因</dt>
            <dd>{{ application.rejectReason }}</dd>
          </div>
        </dl>

        <div class="status-divider" aria-hidden="true"></div>

        <p class="seller-application-status__restriction">
          資料送出後不可直接修改；若審核未通過，你可依駁回原因修正並重新提出申請。
        </p>

        <footer class="seller-application-status__actions">
          <RouterLink
            class="seller-application-status__secondary dg-focus-ring"
            to="/member/seller-application"
          >
            {{ application.status === 'REJECTED' ? '重新提出申請' : '查看申請資料' }}
          </RouterLink>
          <RouterLink
            class="seller-application-status__primary dg-btn-primary dg-focus-ring"
            to="/member/messages"
          >
            前往通知中心
          </RouterLink>
        </footer>
      </article>

      <aside v-if="!isLoading && !apiError" class="seller-application-status__next-step" role="note">
        <i class="bi bi-info-circle" aria-hidden="true"></i>
        <p>{{ nextStep }}</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.seller-application-status {
  padding: var(--space-7) 0;
  color: var(--color-text);
  font-family: var(--font-body);
}

.seller-application-status__container {
  --bs-gutter-x: var(--space-6);
  display: grid;
  justify-items: center;
  gap: var(--space-5);
  max-width: 1112px;
}

.seller-application-status__eyebrow,
h1,
p {
  margin: 0;
}

.seller-application-status__eyebrow {
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

.seller-application-status__description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.seller-application-status__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}

.seller-application-status__card,
.seller-application-status__next-step {
  width: min(100%, 760px);
}

.seller-application-status__card {
  display: grid;
  gap: 20px;
  padding: 28px;
}

.seller-application-status__card-header,
.seller-application-status__details > div,
.seller-application-status__actions,
.seller-application-status__next-step {
  display: flex;
  align-items: center;
}

.seller-application-status__card-header,
.seller-application-status__details > div {
  justify-content: space-between;
}

.seller-application-status__icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: var(--radius-pill);
  color: var(--color-primary-active);
  background: var(--color-primary-notice);
}

.seller-application-status__icon i {
  font-size: 26px;
}

.seller-application-status__badge {
  border-radius: 16px;
  padding: 6px 12px;
  color: var(--color-primary-active);
  background: var(--color-bg);
  font-size: 13px;
  font-weight: 700;
}

.seller-application-status__details {
  display: grid;
  gap: var(--space-3);
  margin: 0;
}

.seller-application-status__details > div {
  min-height: 42px;
  padding: var(--space-3) 0;
}

dt,
dd {
  margin: 0;
  font-size: var(--font-size-sm);
}

dt {
  color: var(--color-text-muted);
}

dd {
  color: var(--color-text);
  font-weight: 700;
}

.status-divider {
  height: 1px;
  background: var(--color-border);
}

.seller-application-status__restriction {
  color: var(--color-text-muted);
  font-size: 13px;
}

.seller-application-status__actions {
  justify-content: end;
  gap: var(--space-3);
}

.seller-application-status__secondary,
.seller-application-status__primary {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: 0 18px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-decoration: none;
}

.seller-application-status__secondary {
  border: 1px solid var(--color-border);
  color: var(--color-text);
  background: var(--color-surface);
}

.seller-application-status__secondary:hover {
  color: var(--color-primary-active);
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.seller-application-status__primary {
  border: 1px solid var(--color-primary);
}

.seller-application-status__next-step {
  gap: var(--space-3);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  background: var(--color-primary-notice);
}

.seller-application-status__next-step i {
  color: var(--color-primary-active);
  font-size: 20px;
}

.seller-application-status__next-step p {
  color: var(--color-text);
  font-size: 13px;
}

@media (max-width: 575.98px) {
  .seller-application-status {
    padding: var(--space-6) 0;
  }

  .seller-application-status__card {
    padding: var(--space-5);
  }

  .seller-application-status__details > div {
    align-items: start;
    flex-direction: column;
    gap: var(--space-1);
  }

  .seller-application-status__actions {
    flex-direction: column;
  }

  .seller-application-status__secondary,
  .seller-application-status__primary {
    width: 100%;
  }

  .seller-application-status__next-step {
    align-items: start;
  }
}
</style>
