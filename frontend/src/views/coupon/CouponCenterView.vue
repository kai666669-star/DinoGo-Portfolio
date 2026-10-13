<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const coupons = ref([])
const memberCoupons = ref([])
const loading = ref(false)
const claimingCouponId = ref(null)
const message = ref('')
const errorMessage = ref('')

const claimedCouponIds = computed(
  () => new Set(memberCoupons.value.map((coupon) => Number(coupon.couponId))),
)

const storeSellerId = computed(() => route.params.sellerId || route.query.sellerId)
const isStoreCouponCenter = computed(() => Boolean(storeSellerId.value))

function discountText(coupon) {
  if (coupon.discountType === 'PERCENT') {
    return `${Number(coupon.discountValue)}% 折扣`
  }
  return `折 NT$ ${Number(coupon.discountValue || 0).toLocaleString('zh-TW')}`
}

function requirementText(coupon) {
  const minimum = Number(coupon.minPurchaseAmount || 0)
  return minimum > 0 ? `滿 NT$ ${minimum.toLocaleString('zh-TW')} 可使用` : '不限最低消費'
}

function expiryText(endAt) {
  return new Date(endAt).toLocaleDateString('zh-TW')
}

async function loadCoupons() {
  loading.value = true
  errorMessage.value = ''
  try {
    const publicRequest = api.get('/coupons/available', {
      params: storeSellerId.value ? { sellerId: storeSellerId.value } : {},
    })
    const memberRequest = authStore.isAuthenticated
      ? api.get('/member/coupons')
      : Promise.resolve({ data: [] })
    const [publicResponse, memberResponse] = await Promise.all([publicRequest, memberRequest])
    coupons.value = publicResponse.data || []
    memberCoupons.value = memberResponse.data || []
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '無法取得優惠券，請稍後再試。'
  } finally {
    loading.value = false
  }
}

async function claimCoupon(couponId) {
  if (!authStore.isAuthenticated) {
    await router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  claimingCouponId.value = couponId
  message.value = ''
  errorMessage.value = ''
  try {
    await api.post(`/member/coupons/${couponId}/claim`)
    message.value = '優惠券領取成功，已放入我的優惠券。'
    await loadCoupons()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '優惠券領取失敗。'
  } finally {
    claimingCouponId.value = null
  }
}

onMounted(loadCoupons)

watch(
  storeSellerId,
  () => {
    loadCoupons()
  },
)
</script>

<template>
  <main class="coupon-center container">
    <header class="coupon-center__header">
      <div>
        <p>Coupon Center</p>
        <h1>{{ isStoreCouponCenter ? '店鋪優惠券' : '優惠券中心' }}</h1>
        <span>先領取，結帳時才能使用</span>
      </div>
      <RouterLink v-if="authStore.isAuthenticated" to="/member/coupons" class="my-coupons-link">
        查看我的優惠券
      </RouterLink>
    </header>

    <p v-if="message" class="notice notice--success">{{ message }}</p>
    <p v-if="errorMessage" class="notice notice--error">{{ errorMessage }}</p>
    <p v-if="loading" class="state">優惠券載入中...</p>

    <section v-else-if="coupons.length" class="coupon-grid">
      <article v-for="coupon in coupons" :key="coupon.couponId" class="coupon-ticket">
        <div class="coupon-ticket__value">{{ discountText(coupon) }}</div>
        <div class="coupon-ticket__body">
          <span class="coupon-code">{{ coupon.sellerName || 'DINO-GO 合作商家' }}</span>
          <h2>{{ coupon.couponName }}</h2>
          <p>{{ requirementText(coupon) }}</p>
          <small>有效期限至 {{ expiryText(coupon.endAt) }}</small>
        </div>
        <button
          type="button"
          :disabled="claimedCouponIds.has(Number(coupon.couponId)) || claimingCouponId === coupon.couponId"
          @click="claimCoupon(coupon.couponId)"
        >
          {{
            claimedCouponIds.has(Number(coupon.couponId))
              ? '已領取'
              : claimingCouponId === coupon.couponId
                ? '領取中...'
                : '領取'
          }}
        </button>
      </article>
    </section>

    <p v-else class="state">目前沒有可領取的優惠券。</p>
  </main>
</template>

<style scoped>
.coupon-center { padding-block: 40px 64px; }
.coupon-center__header { display: flex; justify-content: space-between; align-items: end; gap: 24px; margin-bottom: 28px; }
.coupon-center__header p, .coupon-center__header h1, .coupon-center__header span { margin: 0; }
.coupon-center__header p { color: var(--color-primary); font-weight: 800; }
.coupon-center__header h1 { margin-block: 4px; font-size: 32px; }
.coupon-center__header span { color: var(--color-text-muted); }
.my-coupons-link { border: 1px solid var(--color-primary); border-radius: var(--radius-md); padding: 10px 16px; color: var(--color-primary); font-weight: 700; text-decoration: none; }
.notice, .state { border-radius: var(--radius-md); padding: 14px 16px; }
.notice--success { background: #ecfdf3; color: #166534; }
.notice--error { background: #fff1f2; color: #b42318; }
.state { background: var(--color-bg-muted); color: var(--color-text-muted); }
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; }
.coupon-ticket { display: grid; grid-template-columns: 120px 1fr auto; align-items: center; gap: 18px; min-height: 160px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; background: var(--color-surface); }
.coupon-ticket__value { color: var(--color-primary-active); font-size: 24px; font-weight: 900; text-align: center; }
.coupon-ticket__body h2, .coupon-ticket__body p { margin: 4px 0; }
.coupon-ticket__body h2 { font-size: 18px; }
.coupon-ticket__body p, .coupon-ticket__body small { color: var(--color-text-muted); }
.coupon-code { color: var(--color-primary); font-size: 12px; font-weight: 800; }
.coupon-seller { display: block; color: var(--color-text-muted); font-size: 12px; font-weight: 700; }
.coupon-ticket button { border: 0; border-radius: var(--radius-md); padding: 10px 16px; background: var(--color-primary); color: white; font-weight: 800; cursor: pointer; }
.coupon-ticket button:disabled { background: var(--color-bg-muted); color: var(--color-text-muted); cursor: default; }
@media (max-width: 720px) {
  .coupon-center__header { align-items: start; flex-direction: column; }
  .coupon-ticket { grid-template-columns: 1fr; }
  .coupon-ticket__value { text-align: left; }
}
</style>
