<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  approveSellerApplication,
  getSellerApplication,
  rejectSellerApplication,
} from '@/api/sellerApplicationApi'

const route = useRoute()
const router = useRouter()
const decision = ref('approve')
const rejectionReason = ref('')
const isConfirmed = ref(false)
const feedback = ref('')
const isSubmitting = ref(false)

const application = reactive({
  id: route.params.id,
  memberId: '',
  storeName: '',
  description: '',
  logoUrl: '',
  submittedAt: '',
  status: 'PENDING',
  rejectReason: '',
  reviewedBy: null,
  reviewedAt: null,
})

const isPending = computed(() => application.status === 'PENDING')
const canConfirm = computed(
  () => isPending.value && isConfirmed.value && (decision.value === 'approve' || rejectionReason.value.trim()),
)
const confirmationText = computed(() => decision.value === 'approve'
  ? '我已確認申請資料，並知悉核准會授予商家角色。'
  : '我已確認申請資料，並確認駁回原因將通知申請會員。')
const reviewResult = computed(() => ({ APPROVED: '已核准', REJECTED: '已駁回' })[application.status] || '')

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-TW') : '-')

const loadApplication = async () => {
  try {
    const response = await getSellerApplication(application.id)
    const data = response.data
    application.memberId = data.memberId
    application.storeName = data.storeName
    application.description = data.storeDescription || '-'
    application.logoUrl = data.storeLogoUrl || '-'
    application.submittedAt = formatDate(data.createdAt)
    application.status = data.status
    application.rejectReason = data.rejectReason || ''
    application.reviewedBy = data.reviewedBy
    application.reviewedAt = data.reviewedAt
  } catch (error) {
    feedback.value = error.response?.data?.message || '申請資料載入失敗。'
  }
}

async function confirmDecision() {
  if (!canConfirm.value || isSubmitting.value) return

  isSubmitting.value = true
  feedback.value = ''

  try {
    if (decision.value === 'approve') {
      await approveSellerApplication(application.id)
      await router.push({ name: 'AdminSellerApplications', query: { status: 'APPROVED' } })
    } else {
      await rejectSellerApplication(application.id, rejectionReason.value.trim())
      await router.push({ name: 'AdminSellerApplications', query: { status: 'REJECTED' } })
    }
  } catch (error) {
    feedback.value = error.response?.data?.message || '審核操作失敗，請稍後再試。'
  } finally {
    isSubmitting.value = false
  }
}

function selectDecision(nextDecision) {
  if (!isPending.value) return
  decision.value = nextDecision
  isConfirmed.value = false
}

onMounted(loadApplication)
</script>

<template>
  <section class="admin-application-detail" aria-labelledby="admin-application-detail-title">
    <header class="admin-application-detail__header">
      <div>
        <p class="admin-application-detail__breadcrumb">
          商家申請審核&nbsp;&nbsp;/&nbsp;&nbsp;{{ application.id }}
        </p>
        <h1 id="admin-application-detail-title">審核：{{ application.storeName }}</h1>
      </div>
      <span class="admin-application-detail__status">{{ application.status }}</span>
    </header>

    <aside class="admin-application-detail__notice" aria-label="核准影響說明">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <p>核准後將建立商家資料並授予 seller 角色；操作完成後不可直接復原。</p>
    </aside>

    <div class="admin-application-detail__columns">
      <div class="admin-application-detail__information">
        <article class="admin-application-detail__card">
          <h2>申請會員</h2>
          <dl>
            <div><dt>會員編號</dt><dd>{{ application.memberId }}</dd></div>
          </dl>
        </article>

        <article class="admin-application-detail__card">
          <h2>申請店鋪公開資訊</h2>
          <dl>
            <div><dt>店鋪名稱</dt><dd>{{ application.storeName }}</dd></div>
            <div><dt>店鋪介紹</dt><dd>{{ application.description }}</dd></div>
            <div><dt>Logo 網址</dt><dd class="admin-application-detail__url">{{ application.logoUrl }}</dd></div>
            <div><dt>送出時間</dt><dd>{{ application.submittedAt }}</dd></div>
          </dl>
        </article>
      </div>

      <aside class="admin-application-detail__decision-panel">
        <form class="admin-application-detail__card" @submit.prevent="confirmDecision">
          <h2>審核決策</h2>
          <div class="admin-application-detail__choices" aria-label="審核結果">
            <button
              type="button"
              :class="{ 'is-active': decision === 'approve' }"
              :disabled="!isPending"
              @click="selectDecision('approve')"
            >核准</button>
            <button
              type="button"
              :class="{ 'is-active': decision === 'reject' }"
              :disabled="!isPending"
              @click="selectDecision('reject')"
            >駁回</button>
          </div>

          <label v-if="decision === 'reject' && isPending" class="admin-application-detail__reason">
            <span>駁回原因（選擇駁回時必填）</span>
            <textarea
              v-model="rejectionReason"
              :required="decision === 'reject'"
              placeholder="請清楚說明需補正或不符合資格的原因，內容會顯示給申請會員。"
            ></textarea>
          </label>

          <label v-if="isPending" class="admin-application-detail__confirmation">
            <input v-model="isConfirmed" type="checkbox" />
            <span>{{ confirmationText }}</span>
          </label>

          <p v-if="feedback" class="admin-application-detail__feedback" role="status">{{ feedback }}</p>

          <div v-if="isPending" class="admin-application-detail__actions">
            <RouterLink class="admin-application-detail__back dg-focus-ring" to="/admin/seller-applications">
              返回清單
            </RouterLink>
            <button class="admin-application-detail__confirm dg-btn-primary dg-focus-ring" :disabled="!canConfirm || isSubmitting" type="submit">
              確認{{ decision === 'approve' ? '核准' : '駁回' }}
            </button>
          </div>
        </form>

        <article class="admin-application-detail__audit">
          <h2>系統紀錄</h2>
          <p v-if="isPending">尚無審核紀錄。</p>
          <dl v-else class="admin-application-detail__audit-list">
            <div><dt>審核結果</dt><dd>{{ reviewResult }}</dd></div>
            <div><dt>審核者</dt><dd>會員 #{{ application.reviewedBy ?? '-' }}</dd></div>
            <div><dt>審核時間</dt><dd>{{ formatDate(application.reviewedAt) }}</dd></div>
            <div v-if="application.status === 'REJECTED'"><dt>駁回原因</dt><dd>{{ application.rejectReason || '-' }}</dd></div>
          </dl>
        </article>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.admin-application-detail { display: grid; gap: 22px; color: var(--color-text); font-family: var(--font-body); }
.admin-application-detail__header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-5); }
.admin-application-detail__header p, .admin-application-detail h1, .admin-application-detail h2, .admin-application-detail__notice p, .admin-application-detail__audit p { margin: 0; }
.admin-application-detail__header > div { display: grid; gap: 6px; }
.admin-application-detail__breadcrumb { color: var(--color-text-muted); font-size: 13px; }
h1 { font-size: var(--font-size-xl); font-weight: 700; line-height: var(--line-height-heading); }
.admin-application-detail__status { border-radius: var(--radius-pill); padding: 6px 12px; color: var(--color-primary-active); background: var(--color-bg); font-size: 13px; font-weight: 700; }
.admin-application-detail__notice { display: flex; align-items: center; gap: 10px; border-radius: var(--radius-lg); padding: 14px 18px; color: var(--color-text); background: var(--color-primary-soft); font-size: 13px; }
.admin-application-detail__notice i { color: var(--color-primary-active); font-size: 20px; }
.admin-application-detail__columns { display: grid; grid-template-columns: minmax(0, 1fr) 440px; gap: var(--space-5); align-items: start; }
.admin-application-detail__information, .admin-application-detail__decision-panel { display: grid; gap: 18px; }
.admin-application-detail__decision-panel { gap: var(--space-4); }
.admin-application-detail__card { display: grid; gap: var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-5); background: var(--color-surface); }
h2 { font-size: var(--font-size-md); font-weight: 700; line-height: var(--line-height-heading); }
dl { margin: 0; }
dl > div { display: grid; min-height: 46px; grid-template-columns: 150px minmax(0, 1fr); align-items: center; gap: var(--space-4); border-bottom: 1px solid var(--color-border); padding: 11px 0; font-size: var(--font-size-sm); }
dl > div:last-child { border-bottom: 0; }
dt { color: var(--color-text-muted); } dd { margin: 0; color: var(--color-text); font-weight: 700; } .admin-application-detail__url { overflow-wrap: anywhere; }
.admin-application-detail__choices { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.admin-application-detail__choices button { min-height: 42px; border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text-muted); background: var(--color-surface); font: inherit; font-size: var(--font-size-sm); font-weight: 700; }
.admin-application-detail__choices button.is-active { border-color: var(--color-primary-active); color: var(--color-surface); background: var(--color-primary-active); }
.admin-application-detail__choices button:disabled { cursor: not-allowed; opacity: .55; }
.admin-application-detail__reason { display: grid; gap: var(--space-2); color: var(--color-text); font-size: var(--font-size-sm); font-weight: 700; }
textarea { width: 100%; min-height: 112px; resize: vertical; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 14px; color: var(--color-text); background: var(--color-surface); font: 400 13px/var(--line-height-base) var(--font-body); }
textarea::placeholder { color: var(--color-text-muted); opacity: 1; }
.admin-application-detail__confirmation { display: flex; align-items: flex-start; gap: 10px; color: var(--color-text-muted); font-size: 13px; }
.admin-application-detail__confirmation input { width: 18px; height: 18px; flex: 0 0 18px; accent-color: var(--color-primary); }
.admin-application-detail__feedback { margin: 0; color: var(--color-primary-active); font-size: 13px; }
.admin-application-detail__actions { display: flex; justify-content: end; gap: var(--space-3); }
.admin-application-detail__actions > * { display: inline-flex; min-height: 44px; align-items: center; justify-content: center; border-radius: var(--radius-md); padding: 0 18px; font: 600 var(--font-size-sm) var(--font-body); text-decoration: none; }
.admin-application-detail__back { border: 1px solid var(--color-border); color: var(--color-text); background: var(--color-surface); }
.admin-application-detail__confirm { border-radius: var(--radius-md); padding: 0 18px; font: 600 var(--font-size-sm) var(--font-body); }
.admin-application-detail__confirm:disabled { cursor: not-allowed; border-color: var(--color-disabled-bg); color: var(--color-disabled); background: var(--color-disabled-bg); }
.admin-application-detail button:focus-visible, textarea:focus-visible, .admin-application-detail__back:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
.admin-application-detail__audit { display: grid; gap: var(--space-2); border-radius: var(--radius-lg); padding: 20px; background: var(--color-bg); }
.admin-application-detail__audit h2 { font-size: 15px; }.admin-application-detail__audit p { color: var(--color-text-muted); font-size: 13px; }
.admin-application-detail__audit-list > div { min-height: 0; grid-template-columns: 86px minmax(0, 1fr); padding: 9px 0; font-size: 13px; }
@media (max-width: 1160px) { .admin-application-detail__columns { grid-template-columns: 1fr; } .admin-application-detail__decision-panel { grid-template-columns: minmax(0, 1fr) 300px; } }
@media (max-width: 760px) { .admin-application-detail__header { align-items: start; flex-direction: column; } .admin-application-detail__decision-panel { grid-template-columns: 1fr; } }
@media (max-width: 560px) { dl > div { grid-template-columns: 1fr; align-items: start; gap: var(--space-1); } .admin-application-detail__actions { flex-direction: column-reverse; }.admin-application-detail__actions > * { width: 100%; } }
</style>
