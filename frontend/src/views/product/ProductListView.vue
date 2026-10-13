<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import ProductCard from '@/views/product/ProductCard.vue'
import { getPublicStore, getPublicStoreSummary, resolveSellerLogoUrl } from '@/api/sellerProfileApi'
import { useAuthStore } from '@/stores/auth'
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sort = ref(route.query.sort || '')
const products = ref([])
const loading = ref(true)
const errorMessage = ref('')
const currentPage = ref(0)
const pageSize = ref(12)
const storeProfile = ref(null)
const storeSummary = ref(null)
const storeCoupons = ref([])
const memberCoupons = ref([])
const claimingCouponId = ref(null)
const couponMessage = ref('')
const couponErrorMessage = ref('')
const categories = ref([])
const brands = ref([])

const minPrice = ref('')
const maxPrice = ref('')
const minRating = ref('')

const subcategories = ref([])

const selectedCategoryId = ref(route.query.categoryId || '')
const selectedSubcategoryId = ref(route.query.subcategoryId || '')
const selectedBrandId = ref(route.query.brandId || '')
const claimedCouponIds = computed(
  () => new Set(memberCoupons.value.map((coupon) => Number(coupon.couponId))),
)
const featuredStoreCoupons = computed(() => storeCoupons.value.slice(0, 4))

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

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data
  } catch (error) {
    console.error('取得分類失敗：', error)
  }
}
const fetchSubcategories = async () => {
  try {
    // 沒選大分類時，不顯示任何子分類
    if (!selectedCategoryId.value) {
      subcategories.value = []
      return
    }

    const response = await api.get('/subcategories', {
      params: {
        categoryId: selectedCategoryId.value,
      },
    })

    subcategories.value = response.data
  } catch (error) {
    console.error('取得子分類失敗：', error)
    subcategories.value = []
  }
}
const fetchBrands = async () => {
  try {
    const response = await api.get('/brands')
    brands.value = response.data
  } catch (error) {
    console.error('取得品牌失敗：', error)
  }
}

const applyFilters = () => {
  currentPage.value = 0

  router.push({
    query: {
      ...route.query,

      categoryId: selectedCategoryId.value || undefined,
      subcategoryId: selectedSubcategoryId.value || undefined,
      brandId: selectedBrandId.value || undefined,

      minPrice: minPrice.value || undefined,
      maxPrice: maxPrice.value || undefined,
      minRating: minRating.value || undefined,
    },
  })
}

const resetFilters = () => {
  selectedCategoryId.value = ''
  selectedSubcategoryId.value = ''
  selectedBrandId.value = ''

  subcategories.value = []

  minPrice.value = ''
  maxPrice.value = ''
  minRating.value = ''

  router.push({
    query: {
      keyword: route.query.keyword || undefined,
      sellerId: route.query.sellerId || undefined,
      sort: route.query.sort || undefined,
    },
  })
}
const handleCategoryChange = async () => {
  // 換大分類後，原本子分類一定失效
  selectedSubcategoryId.value = ''
  subcategories.value = []

  if (selectedCategoryId.value) {
    await fetchSubcategories()
  }
}
const loadStoreProfile = async () => {
  if (!route.query.sellerId) {
    storeProfile.value = null
    storeSummary.value = null
    return
  }

  try {
    const [profileResponse, summaryResponse] = await Promise.all([
      getPublicStore(route.query.sellerId),
      getPublicStoreSummary(route.query.sellerId),
    ])
    storeProfile.value = profileResponse.data
    storeSummary.value = summaryResponse.data
  } catch (error) {
    console.error('Load public store failed:', error)
    storeProfile.value = null
    storeSummary.value = null
  }
}

const loadStoreCoupons = async () => {
  if (!route.query.sellerId) {
    storeCoupons.value = []
    memberCoupons.value = []
    return
  }

  couponErrorMessage.value = ''
  try {
    const publicRequest = api.get('/coupons/available', {
      params: { sellerId: route.query.sellerId },
    })
    const memberRequest = authStore.isAuthenticated
      ? api.get('/member/coupons')
      : Promise.resolve({ data: [] })
    const [publicResponse, memberResponse] = await Promise.all([publicRequest, memberRequest])
    storeCoupons.value = publicResponse.data || []
    memberCoupons.value = memberResponse.data || []
  } catch (error) {
    console.error('Load store coupons failed:', error)
    storeCoupons.value = []
    couponErrorMessage.value = error.response?.data?.message || '無法取得店鋪優惠券'
  }
}

const claimCoupon = async (couponId) => {
  if (!authStore.isAuthenticated) {
    await router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  claimingCouponId.value = couponId
  couponMessage.value = ''
  couponErrorMessage.value = ''
  try {
    await api.post(`/member/coupons/${couponId}/claim`)
    couponMessage.value = '優惠券已領取。'
    await loadStoreCoupons()
  } catch (error) {
    couponErrorMessage.value = error.response?.data?.message || '優惠券領取失敗'
  } finally {
    claimingCouponId.value = null
  }
}
const totalPages = ref(0)
const totalElements = ref(0)

// 取得商品
const fetchProducts = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }

    if (route.query.keyword) {
      params.keyword = route.query.keyword
    }

    if (route.query.categoryId) {
      params.categoryId = route.query.categoryId
    }

    if (route.query.subcategoryId) {
      params.subcategoryId = route.query.subcategoryId
    }

    if (route.query.brandId) {
      params.brandId = route.query.brandId
    }

    if (route.query.minPrice) {
      params.minPrice = route.query.minPrice
    }

    if (route.query.maxPrice) {
      params.maxPrice = route.query.maxPrice
    }
    if (route.query.minRating) {
      params.minRating = route.query.minRating
    }

    if (route.query.sort) {
      params.sort = route.query.sort
    }

    if (route.query.sellerId) {
      params.sellerId = route.query.sellerId
    }

    const response = await api.get('/products', { params })

    products.value = response.data.content

    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
    currentPage.value = response.data.number
  } catch (error) {
    logSafeError('取得商品失敗：', error)
    errorMessage.value = '商品資料載入失敗'
  } finally {
    loading.value = false
  }
}
//新增換頁
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) {
    return
  }

  currentPage.value = page
  fetchProducts()
}

//新增每頁 12 / 24 切換
const changePageSize = () => {
  currentPage.value = 0
  fetchProducts()
}
// 上方重新搜尋時，重置左側篩選條件
watch(
  () => route.query.keyword,
  (newKeyword, oldKeyword) => {
    // 第一次進入頁面不處理，只處理真正的搜尋關鍵字變更
    if (newKeyword === oldKeyword) {
      return
    }

    selectedCategoryId.value = ''
    selectedSubcategoryId.value = ''
    selectedBrandId.value = ''

    subcategories.value = []

    minPrice.value = ''
    maxPrice.value = ''
    minRating.value = ''
  },
)
//當搜尋或分類條件改變時，回第一頁
watch(
  () => [
    route.query.keyword,
    route.query.categoryId,
    route.query.subcategoryId,
    route.query.brandId,
    route.query.minPrice,
    route.query.maxPrice,
    route.query.minRating,
    route.query.sort,
    route.query.sellerId,
  ],
  async () => {
    currentPage.value = 0

    // =========================
    // URL → 左側篩選
    // =========================

    selectedCategoryId.value = route.query.categoryId || ''

    selectedBrandId.value = route.query.brandId || ''

    minPrice.value = route.query.minPrice || ''
    maxPrice.value = route.query.maxPrice || ''
    minRating.value = route.query.minRating || ''

    sort.value = route.query.sort || ''

    // =========================
    // 子分類
    // =========================
    if (selectedCategoryId.value) {
      await fetchSubcategories()

      selectedSubcategoryId.value = route.query.subcategoryId || ''
    } else {
      selectedSubcategoryId.value = ''
      subcategories.value = []
    }

    // =========================
    // 其他資料
    // =========================
    loadStoreProfile()
    loadStoreCoupons()
    fetchProducts()
  },
)

// 切換排序
const changeSort = () => {
  currentPage.value = 0

  router.push({
    query: {
      ...route.query,
      sort: sort.value || undefined,
    },
  })
}

// 第一次進入頁面時取得商品
onMounted(() => {
  loadStoreProfile()
  loadStoreCoupons()

  fetchCategories()
  fetchBrands()
  fetchProducts()

  if (selectedCategoryId.value) {
    fetchSubcategories()
  }
})
</script>

<template>
  <main class="product-list-page">
    <div class="container py-5">
      <!-- 賣家資訊 -->
      <section v-if="route.query.sellerId && storeProfile" class="store-banner">
        <div class="store-identity">
          <img
            v-if="storeProfile.storeLogoUrl"
            class="store-avatar-image"
            :src="resolveSellerLogoUrl(storeProfile.storeLogoUrl)"
            :alt="`${storeProfile.storeName} Logo`"
          />
          <div v-else class="store-avatar-placeholder" aria-hidden="true">
            <i class="bi bi-shop"></i>
          </div>

          <div class="store-copy">
            <h1>{{ storeProfile.storeName }}</h1>
            <p>{{ storeProfile.storeDescription || '店鋪尚未提供介紹。' }}</p>
          </div>
        </div>

        <div v-if="storeSummary" class="store-stats" aria-label="店鋪統計">
          <div class="store-stat">
            <i class="bi bi-star-fill"></i>
            <span>店鋪評價</span>
            <strong>
              {{ storeSummary.ratingCount ? `${storeSummary.averageRating} / 5` : '尚無評價' }}
            </strong>
          </div>
          <div class="store-stat">
            <i class="bi bi-box-seam"></i>
            <span>上架商品</span>
            <strong>{{ Number(storeSummary.activeProductCount).toLocaleString('zh-TW') }}</strong>
          </div>
          <div class="store-stat">
            <i class="bi bi-cart-check"></i>
            <span>累計已售</span>
            <strong>{{ Number(storeSummary.soldCount).toLocaleString('zh-TW') }}</strong>
          </div>
          <div class="store-stat">
            <i class="bi bi-ticket-perforated"></i>
            <span>可領優惠券</span>
            <strong>{{ Number(storeSummary.availableCouponCount).toLocaleString('zh-TW') }}</strong>
          </div>
        </div>
      </section>

      <section v-if="route.query.sellerId" class="store-coupon-section">
        <div class="store-coupon-heading">
          <div>
            <h2>店鋪優惠券</h2>
            <p>領取後可在結帳時選用</p>
          </div>
          <RouterLink
            :to="{ name: 'StoreCouponCenter', params: { sellerId: route.query.sellerId } }"
          >
            此店鋪所有優惠券 <i class="bi bi-chevron-right" aria-hidden="true"></i>
          </RouterLink>
        </div>

        <p v-if="couponMessage" class="coupon-notice coupon-notice--success">{{ couponMessage }}</p>
        <p v-if="couponErrorMessage" class="coupon-notice coupon-notice--error">
          {{ couponErrorMessage }}
        </p>

        <div v-if="featuredStoreCoupons.length" class="store-coupon-list">
          <article
            v-for="coupon in featuredStoreCoupons"
            :key="coupon.couponId"
            class="store-coupon-card"
          >
            <strong>{{ discountText(coupon) }}</strong>
            <div>
              <h3>{{ coupon.couponName }}</h3>
              <p>{{ requirementText(coupon) }}</p>
              <small>有效期限至 {{ expiryText(coupon.endAt) }}</small>
            </div>
            <button
              type="button"
              :disabled="
                claimedCouponIds.has(Number(coupon.couponId)) ||
                claimingCouponId === coupon.couponId
              "
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
        </div>

        <p v-else class="store-coupon-empty">目前沒有可領取的店鋪優惠券。</p>
      </section>

      <!-- =========================
           左側篩選 + 右側商品
           ========================= -->
      <div class="product-list-layout">
        <!-- 左側篩選 -->
        <aside class="filter-sidebar">
          <div class="filter-sidebar-header">
            <h2>篩選條件</h2>

            <button type="button" class="filter-reset-text" @click="resetFilters">清除</button>
          </div>

          <!-- 分類 -->
          <section class="filter-section">
            <h3>分類</h3>

            <select
              v-model="selectedCategoryId"
              class="filter-select"
              @change="handleCategoryChange"
            >
              <option value="">全部分類</option>

              <option
                v-for="category in categories"
                :key="category.categoryId"
                :value="category.categoryId"
              >
                {{ category.categoryName }}
              </option>
            </select>
          </section>

          <!-- 子分類 -->
          <section class="filter-section">
            <h3>子分類</h3>

            <select
              v-model="selectedSubcategoryId"
              class="filter-select"
              :disabled="!selectedCategoryId"
            >
              <option value="">
                {{ selectedCategoryId ? '全部子分類' : '請先選擇分類' }}
              </option>

              <option
                v-for="subcategory in subcategories"
                :key="subcategory.subcategoryId"
                :value="subcategory.subcategoryId"
              >
                {{ subcategory.subcategoryName }}
              </option>
            </select>
          </section>

          <!-- 品牌 -->
          <section class="filter-section">
            <h3>品牌</h3>

            <select v-model="selectedBrandId" class="filter-select">
              <option value="">全部品牌</option>

              <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
                {{ brand.brandName }}
              </option>
            </select>
          </section>

          <!-- 價格 -->
          <section class="filter-section">
            <h3>價格區間</h3>

            <div class="price-range">
              <input v-model="minPrice" type="number" class="price-input" placeholder="最低價" />

              <span class="price-separator">－</span>

              <input v-model="maxPrice" type="number" class="price-input" placeholder="最高價" />
            </div>
          </section>

          <!-- 評價 -->
          <section class="filter-section">
            <h3>商品評價</h3>

            <label v-for="rating in [5, 4, 3, 2, 1]" :key="rating" class="rating-option">
              <input v-model.number="minRating" type="radio" name="rating-filter" :value="rating" />

              <span class="filter-stars">
                <i
                  v-for="star in 5"
                  :key="star"
                  class="bi"
                  :class="star <= rating ? 'bi-star-fill' : 'bi-star'"
                ></i>
              </span>

              <span> {{ rating }} 星以上 </span>
            </label>
          </section>

          <button type="button" class="apply-filter-button" @click="applyFilters">套用篩選</button>
        </aside>

        <!-- 右側商品 -->
        <section class="product-results">
          <!-- 標題 + 排序 -->
          <div class="product-results-header">
            <div>
              <h1 class="page-title">商品列表</h1>

              <div class="product-count">共 {{ totalElements }} 件商品</div>
            </div>

            <div class="toolbar-actions">
              <div class="sort-selector">
                <label for="sort"> 排序： </label>

                <select id="sort" v-model="sort" @change="changeSort">
                  <option value="">預設排序</option>

                  <option value="newest">最新上架</option>

                  <option value="priceAsc">價格：低到高</option>

                  <option value="priceDesc">價格：高到低</option>

                  <option value="salesDesc">銷量最高</option>
                </select>
              </div>

              <div class="page-size-selector">
                <label for="page-size"> 每頁顯示： </label>

                <select id="page-size" v-model.number="pageSize" @change="changePageSize">
                  <option :value="12">12</option>
                  <option :value="24">24</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="text-center py-5">商品載入中...</div>

          <!-- Error -->
          <div v-else-if="errorMessage" class="error-message text-center py-5">
            {{ errorMessage }}
          </div>

          <!-- 無商品 -->
          <div v-else-if="products.length === 0" class="empty-message text-center py-5">
            目前沒有符合條件的商品
          </div>

          <!-- 商品 -->
          <div v-else class="row g-4">
            <div
              v-for="product in products"
              :key="product.productId"
              class="col-6 col-md-4 col-xl-3"
            >
              <ProductCard :product="product" />
            </div>
          </div>

          <!-- 分頁 -->
          <div v-if="totalPages > 1" class="pagination-wrapper">
            <button
              type="button"
              class="page-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              上一頁
            </button>

            <button
              v-for="page in totalPages"
              :key="page"
              type="button"
              class="page-button"
              :class="{
                active: currentPage === page - 1,
              }"
              @click="goToPage(page - 1)"
            >
              {{ page }}
            </button>

            <button
              type="button"
              class="page-button"
              :disabled="currentPage === totalPages - 1"
              @click="goToPage(currentPage + 1)"
            >
              下一頁
            </button>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.product-list-page {
  min-height: 100vh;

  background: var(--color-bg);
  color: var(--color-text);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.store-banner {
  display: grid;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--color-surface);
}

.store-identity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.store-avatar-placeholder {
  width: 80px;
  height: 80px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: 28px;
}

.store-copy {
  display: grid;
  gap: var(--space-1);
  min-width: 0;
}

.store-copy p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.store-copy h1,
.store-copy p {
  margin: 0;
}

.store-copy h1 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.store-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-4);
}

.store-stat {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 2px var(--space-2);
  min-width: 0;
  padding-inline: var(--space-4);
  border-right: 1px solid var(--color-border);
}

.store-stat:first-child {
  padding-left: 0;
}

.store-stat:last-child {
  padding-right: 0;
  border-right: 0;
}

.store-stat i {
  grid-row: 1 / span 2;
  color: var(--color-primary);
  font-size: var(--font-size-lg);
}

.store-stat span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.store-stat strong {
  color: var(--color-text-900);
  font-size: var(--font-size-md);
}

.store-coupon-section {
  display: grid;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  background: var(--color-surface);
}

.store-coupon-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.store-coupon-heading h2,
.store-coupon-heading p,
.store-coupon-card h3,
.store-coupon-card p {
  margin: 0;
}

.store-coupon-heading h2 {
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.store-coupon-heading p,
.store-coupon-card p,
.store-coupon-card small,
.store-coupon-empty {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.store-coupon-heading a {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-decoration: none;
}

.store-coupon-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.store-coupon-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-primary-50);
}

.store-coupon-card > strong {
  grid-column: 1 / -1;
  color: var(--color-primary);
  font-size: var(--font-size-md);
}

.store-coupon-card h3 {
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.store-coupon-card button {
  min-height: 36px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 700;
  cursor: pointer;
}

.store-coupon-card button:disabled {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  cursor: default;
}

.coupon-notice {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
}

.coupon-notice--success {
  background: #ecfdf3;
  color: #166534;
}

.coupon-notice--error {
  background: #fff1f2;
  color: #b42318;
}

.error-message {
  color: var(--color-danger);
}

.empty-message {
  color: var(--color-text-muted);
}

.product-list-layout {
  display: grid;

  grid-template-columns:
    220px
    minmax(0, 1fr);

  gap: 28px;

  align-items: start;
}

/* =========================
   左側篩選
   ========================= */

.filter-sidebar {
  padding: 20px;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.filter-sidebar-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.filter-sidebar-header h2 {
  margin: 0;

  color: var(--color-text);

  font-size: 18px;
  font-weight: 700;
}

.filter-reset-text {
  padding: 0;

  color: var(--color-text-muted);

  font-size: 13px;

  background: transparent;

  border: 0;

  cursor: pointer;
}

.filter-reset-text:hover {
  color: var(--color-primary);
}

/* =========================
   每個篩選區塊
   ========================= */

.filter-section {
  margin-bottom: 20px;
  padding-bottom: 20px;

  border-bottom: 1px solid var(--color-border);
}

.filter-section h3 {
  margin: 0 0 12px;

  color: var(--color-text);

  font-size: 15px;
  font-weight: 600;
}

/* =========================
   Select
   ========================= */

.filter-select {
  width: 100%;
  height: 40px;

  padding: 0 10px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.filter-select:hover,
.filter-select:focus {
  border-color: var(--color-primary);

  outline: none;
}

/* =========================
   價格
   ========================= */

.price-filter {
  display: grid;

  grid-template-columns:
    minmax(0, 1fr)
    auto
    minmax(0, 1fr);

  align-items: center;

  gap: 6px;
}

.price-filter input {
  width: 100%;
  min-width: 0;
  height: 38px;

  padding: 0 8px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.price-filter input:focus {
  outline: none;

  border-color: var(--color-primary);
}

.price-filter span {
  color: var(--color-text-muted);
}

/* =========================
   評價
   ========================= */

.rating-option {
  display: flex;

  align-items: center;

  gap: 7px;

  margin-bottom: 10px;

  color: var(--color-text);

  font-size: 13px;

  cursor: pointer;
}

.rating-option input {
  accent-color: var(--color-primary);
}

.filter-stars {
  display: inline-flex;

  gap: 1px;

  color: var(--color-warning);

  font-size: 12px;
}

/* =========================
   套用
   ========================= */

.apply-filter-button {
  width: 100%;

  padding: 10px 12px;

  color: #fff;

  font-size: 14px;
  font-weight: 600;

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.apply-filter-button:hover {
  opacity: 0.9;
}

/* =========================
   右側
   ========================= */

.product-results {
  min-width: 0;
}

.product-results-header {
  display: flex;

  align-items: flex-end;
  justify-content: space-between;

  gap: 24px;

  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;

  color: var(--color-text);

  font-family: var(--font-heading);

  font-size: 26px;
  font-weight: 700;
}

.product-count {
  color: var(--color-text-muted);

  font-size: 14px;
}

/* =========================
   排序 / 每頁
   ========================= */

.toolbar-actions {
  display: flex;

  align-items: center;

  gap: 18px;
}

.sort-selector,
.page-size-selector {
  display: flex;

  align-items: center;

  gap: 8px;
}

.sort-selector label,
.page-size-selector label {
  color: var(--color-text);

  font-size: 14px;
}

.sort-selector select,
.page-size-selector select {
  height: 40px;

  padding: 0 10px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.sort-selector select:hover,
.sort-selector select:focus,
.page-size-selector select:hover,
.page-size-selector select:focus {
  outline: none;

  border-color: var(--color-primary);
}

/* =========================
   分頁
   ========================= */

.pagination-wrapper {
  display: flex;

  justify-content: center;

  gap: 8px;

  margin-top: 40px;
}

.page-button {
  min-width: 40px;
  height: 40px;

  padding: 0 12px;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.page-button:hover:not(:disabled) {
  color: var(--color-primary);

  background: var(--color-primary-soft);

  border-color: var(--color-primary);
}

.page-button.active {
  color: #fff;

  background: var(--color-primary);

  border-color: var(--color-primary);
}

.page-button:disabled {
  cursor: not-allowed;

  opacity: 0.5;
}
.filter-select:disabled {
  color: var(--color-text-muted);
  background: var(--color-surface-soft);
  cursor: not-allowed;
  opacity: 0.7;
}
/* =========================
   RWD
   ========================= */

@media (max-width: 991.98px) {
  .store-stats,
  .store-coupon-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .store-stat:nth-child(2) {
    border-right: 0;
  }

  .store-stat:nth-child(n + 3) {
    margin-top: var(--space-3);
    border-top: 1px solid var(--color-border);
    padding-top: var(--space-3);
  }

  .product-list-layout {
    grid-template-columns: 1fr;
  }

  .filter-sidebar {
    width: 100%;
  }

  .product-results-header {
    align-items: stretch;

    flex-direction: column;
  }
}

@media (max-width: 575.98px) {
  .toolbar-actions {
    align-items: stretch;

    flex-direction: column;
  }

  .sort-selector,
  .page-size-selector {
    justify-content: space-between;
  }

  .sort-selector select,
  .page-size-selector select {
    flex: 1;
  }
}
@media (max-width: 680px) {
  .store-identity {
    align-items: flex-start;
    flex-direction: column;
  }

  .store-stats,
  .store-coupon-list {
    grid-template-columns: 1fr;
  }

  .store-stat,
  .store-stat:first-child,
  .store-stat:last-child {
    margin-top: 0;
    border-top: 1px solid var(--color-border);
    border-right: 0;
    padding: var(--space-3) 0 0;
  }

  .store-stat:first-child {
    border-top: 0;
    padding-top: 0;
  }

  .store-coupon-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
.store-avatar-image {
  width: 80px;
  height: 80px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  object-fit: cover;
  border: 1px solid var(--color-border);
}
.price-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  width: 100%;
}

.price-input {
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
/* Chrome、Edge、Safari：隱藏 number 上下箭頭 */
.price-input::-webkit-outer-spin-button,
.price-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Firefox */
.price-input[type='number'] {
  -moz-appearance: textfield;
  appearance: textfield;
}
</style>
