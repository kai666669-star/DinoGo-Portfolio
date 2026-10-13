<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showCategoryFilter = ref(false)
const primaryNav = ref(null)

const categories = ref([])
const subcategories = ref([])
const brands = ref([])

const selectedCategoryId = ref('')
const selectedSubcategoryId = ref('')
const selectedBrandId = ref('')

const navItems = [
  {
    label: '新品上市',
    to: { name: 'ProductList', query: { sort: 'newest' } },
    activeKey: 'newest',
  },
  {
    label: '熱門推薦',
    to: { name: 'ProductList', query: { sort: 'salesDesc' } },
    activeKey: 'salesDesc',
  },

  {
    label: '優惠活動',
    to: { name: 'CouponCenter' },
    activeKey: 'offers',
  },
  {
    label: '主題企劃',
    to: { name: 'ProductList', query: { filter: 'themes' } },
    activeKey: 'themes',
  },
  {
    label: '商家中心',
    to: { name: 'SellerDashboard' },
    icon: 'bi-shop',
    activeKey: 'seller',
    requiresSeller: true,
  },
]

const toggleCategoryFilter = () => {
  showCategoryFilter.value = !showCategoryFilter.value
}

// 分類面板開啟時，點擊導覽以外的區域應關閉，避免浮層遮住頁面內容。
const closeCategoryFilterOnOutsideClick = (event) => {
  if (showCategoryFilter.value && !primaryNav.value?.contains(event.target)) {
    showCategoryFilter.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data
  } catch (error) {
    logSafeError('取得分類失敗：', error)
  }
}

const fetchBrands = async () => {
  try {
    const response = await api.get('/brands')
    brands.value = response.data
  } catch (error) {
    logSafeError('取得品牌失敗：', error)
  }
}

const fetchSubcategories = async () => {
  try {
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
    logSafeError('取得子分類失敗：', error)
  }
}

const updateProductQuery = () => {
  const query = {}

  // 大分類
  if (selectedCategoryId.value) {
    query.categoryId = selectedCategoryId.value
  }

  // 子分類
  if (selectedSubcategoryId.value) {
    query.subcategoryId = selectedSubcategoryId.value
  }

  // 品牌
  if (selectedBrandId.value) {
    query.brandId = selectedBrandId.value
  }

  router.push({
    name: 'ProductList',
    query,
  })
}

const selectCategory = (categoryId) => {
  selectedCategoryId.value = categoryId
}

const selectSubcategory = (subcategoryId) => {
  selectedSubcategoryId.value = subcategoryId
}

const selectBrand = (brandId) => {
  selectedBrandId.value = brandId
}

const clearFilters = () => {
  selectedCategoryId.value = ''
  selectedSubcategoryId.value = ''
  selectedBrandId.value = ''
  subcategories.value = []

  router.push({
    name: 'ProductList',
  })
}

watch(selectedCategoryId, async () => {
  selectedSubcategoryId.value = ''

  if (selectedCategoryId.value) {
    await fetchSubcategories()
  } else {
    subcategories.value = []
  }

  updateProductQuery()
})

watch(selectedSubcategoryId, () => {
  updateProductQuery()
})

watch(selectedBrandId, () => {
  updateProductQuery()
})

const isActive = (item) => {
  if (item.activeKey === 'seller') {
    return route.name === 'SellerDashboard'
  }

  if (item.activeKey === 'offers') {
    return route.name === 'CouponCenter'
  }

  if (route.name !== 'ProductList') {
    return false
  }

  if (item.activeKey === 'newest') {
    return route.query.sort === 'newest'
  }

  if (item.activeKey === 'salesDesc') {
    return route.query.sort === 'salesDesc'
  }

  return route.query.filter === item.activeKey
}

onMounted(async () => {
  document.addEventListener('click', closeCategoryFilterOnOutsideClick)
  await Promise.all([fetchCategories(), fetchBrands()])
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeCategoryFilterOnOutsideClick)
})
</script>

<template>
  <nav
    ref="primaryNav"
    class="primary-nav"
    aria-label="Primary navigation"
    @keydown.esc="showCategoryFilter = false"
  >
    <div class="container primary-nav__inner">
      <button
        class="primary-nav__toggle d-flex d-lg-none align-items-center justify-content-between"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#primary-nav-menu"
        aria-controls="primary-nav-menu"
        aria-expanded="false"
      >
        <span>
          <i class="bi bi-list me-2" aria-hidden="true"></i>
          商城導覽
        </span>

        <i class="bi bi-chevron-down" aria-hidden="true"></i>
      </button>

      <div
        id="primary-nav-menu"
        class="primary-nav__menu collapse d-lg-flex align-items-lg-center gap-lg-1"
      >
        <!-- 全部分類 -->
        <div class="category-menu" @mouseleave="showCategoryFilter = false">
          <button
            type="button"
            class="primary-nav__link primary-nav__link--all"
            :class="{
              'primary-nav__link--active-filter': selectedSubcategoryId || selectedBrandId,
            }"
            :aria-expanded="showCategoryFilter"
            @click="toggleCategoryFilter"
          >
            <i class="bi bi-list" aria-hidden="true"></i>
            <span>全部分類</span>
          </button>

          <!-- 展開選單 -->
          <div v-if="showCategoryFilter" class="category-filter-bridge">
            <div class="category-filter-panel">
              <!-- 分類 -->
              <div class="filter-column">
                <h4 class="filter-title">分類</h4>

                <button
                  type="button"
                  class="filter-option"
                  :class="{
                    active: selectedCategoryId === '',
                  }"
                  @click="clearFilters"
                >
                  全部分類
                </button>

                <button
                  v-for="category in categories"
                  :key="category.categoryId"
                  type="button"
                  class="filter-option"
                  :class="{
                    active: selectedCategoryId === category.categoryId,
                  }"
                  @click="selectCategory(category.categoryId)"
                >
                  {{ category.categoryName }}
                </button>
              </div>

              <!-- 子分類 -->
              <div class="filter-column">
                <h4 class="filter-title">子分類</h4>

                <template v-if="selectedCategoryId">
                  <button
                    type="button"
                    class="filter-option"
                    :class="{
                      active: selectedSubcategoryId === '',
                    }"
                    @click="selectSubcategory('')"
                  >
                    全部子分類
                  </button>

                  <button
                    v-for="subcategory in subcategories"
                    :key="subcategory.subcategoryId"
                    type="button"
                    class="filter-option"
                    :class="{
                      active: selectedSubcategoryId === subcategory.subcategoryId,
                    }"
                    @click="selectSubcategory(subcategory.subcategoryId)"
                  >
                    {{ subcategory.subcategoryName }}
                  </button>
                </template>

                <p v-else class="filter-hint">請先選擇分類</p>
              </div>

              <!-- 品牌 -->
              <div class="filter-column">
                <h4 class="filter-title">品牌</h4>

                <button
                  type="button"
                  class="filter-option"
                  :class="{
                    active: selectedBrandId === '',
                  }"
                  @click="selectBrand('')"
                >
                  全部品牌
                </button>

                <button
                  v-for="brand in brands"
                  :key="brand.brandId"
                  type="button"
                  class="filter-option"
                  :class="{
                    active: selectedBrandId === brand.brandId,
                  }"
                  @click="selectBrand(brand.brandId)"
                >
                  {{ brand.brandName }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 其他導覽 -->
        <template v-for="item in navItems" :key="item.label">
          <RouterLink
            v-if="!item.requiresSeller || authStore.isSeller"
            class="primary-nav__link"
            :class="{
              'primary-nav__link--seller': item.label === '商家中心',
              'primary-nav__link--active': isActive(item),
            }"
            :to="item.to"
          >
            <i v-if="item.icon" class="bi" :class="item.icon" aria-hidden="true"></i>
            <span>{{ item.label }}</span>
          </RouterLink>
        </template>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.primary-nav {
  position: relative;
  min-height: 54px;
  margin: 0;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.primary-nav__inner {
  display: flex;
  width: 100%;
  min-height: inherit;
  align-items: center;
  max-width: 1440px;
}

.primary-nav__menu {
  width: 100%;
  gap: var(--space-3);
}

.primary-nav__toggle {
  width: 100%;
  min-height: 46px;
  padding: 0;
  color: var(--color-primary-800);
  border: 0;
  background: transparent;
}

.primary-nav__link {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-1);
  min-height: 40px;
  padding: 0 var(--space-4);

  color: inherit;
  font-size: 14px;
  font-weight: 400;
  text-decoration: none;

  border: 0;
  border-bottom: 2px solid transparent;

  background: transparent;

  cursor: pointer;
}

/* =========================
   全部分類
   ========================= */

.category-menu {
  position: relative;
  flex: 0 0 auto;
}

.primary-nav__link--all {
  width: 116px;
  min-height: 40px;

  color: var(--color-surface);

  border-bottom-color: transparent;
  border-radius: var(--radius-md);

  background: var(--color-primary);
}

.primary-nav__link--all:hover,
.primary-nav__link--all:focus-visible {
  color: var(--color-surface);
  background: var(--color-primary-hover);
}

/* =========================
   分類展開面板
   ========================= */

.category-filter-bridge {
  position: absolute;
  top: 100%;
  left: 0;

  z-index: 2000;

  width: 720px;
  padding-top: 10px;
}

.category-filter-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);

  width: 720px;
  min-height: 240px;

  padding: 24px;

  color: var(--color-text);
  background: #ffffff;

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
}

.filter-column {
  padding: 0 20px;

  border-right: 1px solid var(--color-border);
}

.filter-column:first-child {
  padding-left: 0;
}

.filter-column:last-child {
  padding-right: 0;
  border-right: 0;
}

.filter-title {
  margin: 0 0 14px;

  color: var(--color-primary-800);

  font-size: 16px;
  font-weight: 600;
}

.filter-option {
  display: block;

  width: 100%;

  margin-bottom: 4px;
  padding: 9px 10px;

  color: var(--color-text);

  font-size: 15px;
  text-align: left;

  border: 0;
  border-radius: var(--radius-md);

  background: transparent;

  cursor: pointer;
}

.filter-option:hover {
  background: var(--color-primary-soft);
}

.filter-option.active {
  color: var(--color-primary-800);
  font-weight: 600;

  background: var(--color-primary-soft);
}

.filter-hint {
  margin: 8px 10px;

  color: var(--color-text-muted);

  font-size: 14px;
}

/* =========================
   其他選單
   ========================= */

.primary-nav__link--seller {
  margin-left: auto;
  gap: 8px;
  padding: 0;
  color: var(--color-primary-active);
  font-weight: 600;
}

.primary-nav__link:hover,
.primary-nav__link:focus-visible {
  color: var(--color-primary-800);

  background: var(--color-primary-soft);

  border-bottom-color: var(--color-primary);
}

.primary-nav__link--active {
  color: var(--color-primary-800);

  background: var(--color-primary-soft);

  border-bottom-color: var(--color-primary);
}

/* =========================
   Mobile
   ========================= */

@media (max-width: 991.98px) {
  .primary-nav {
    height: auto;
    min-height: 0;
  }

  .primary-nav__inner {
    display: block;
    min-height: 0;
  }

  .primary-nav__menu {
    gap: 0;
    padding-bottom: var(--space-2);
  }

  .primary-nav__link {
    width: 100%;
    min-height: 42px;

    border-bottom: 0;
    border-radius: var(--radius-md);
  }

  .category-menu {
    width: 100%;
  }

  .primary-nav__link--all {
    width: 100%;
    margin: 0;
  }

  .primary-nav__link--seller {
    margin-left: 0;
  }

  .filter-column {
    padding: 16px 0;

    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .filter-column:first-child {
    padding-top: 0;
  }

  .filter-column:last-child {
    border-bottom: 0;
  }
}
</style>
