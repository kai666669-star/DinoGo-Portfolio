<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { getImageUrl } from '@/utils/imageUrl'

const route = useRoute()
const router = useRouter()

const stores = ref([])
const loading = ref(false)
const errorMessage = ref('')

const fetchStores = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get('/stores/search', {
      params: {
        keyword: route.query.keyword || '',
      },
    })

    stores.value = response.data
  } catch (error) {
    console.error('搜尋賣家失敗：', error)
    errorMessage.value = '搜尋賣家失敗'
  } finally {
    loading.value = false
  }
}

const goToStore = (sellerId) => {
  router.push({
    path: '/products',
    query: {
      sellerId,
    },
  })
}

onMounted(() => {
  fetchStores()
})
watch(
  () => route.query.keyword,
  () => {
    fetchStores()
  },
)
</script>

<template>
  <main class="store-search-page">
    <div class="container py-5">
      <h1 class="page-title">賣家搜尋結果</h1>

      <p v-if="route.query.keyword" class="search-keyword">搜尋「{{ route.query.keyword }}」</p>

      <div v-if="loading">搜尋中...</div>

      <div v-else-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <div v-else-if="stores.length === 0">找不到符合條件的賣家。</div>

      <div v-else class="store-list">
        <button
          v-for="store in stores"
          :key="store.sellerId"
          type="button"
          class="store-card"
          @click="goToStore(store.sellerId)"
        >
          <div class="store-logo-wrapper">
            <img
              v-if="store.storeLogoUrl"
              :src="getImageUrl(store.storeLogoUrl)"
              :alt="store.storeName"
              class="store-logo"
            />

            <div v-else class="store-logo-placeholder">
              <i class="bi bi-shop"></i>
            </div>
          </div>

          <h2 class="store-name">
            {{ store.storeName }}
          </h2>

          <p class="store-description">
            {{ store.storeDescription || '目前尚無商店介紹' }}
          </p>
        </button>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* 賣家列表 */
.store-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 240px));
  gap: 28px;
  justify-content: start;
  margin-top: 28px;
}

/* 整張 Card */

.store-card {
  position: relative;

  display: flex;
  width: 100%;
  min-height: 200px;
  flex-direction: column;
  align-items: center;

  padding: 28px 26px 24px;

  color: var(--color-text);
  text-align: center;

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: 16px;

  box-shadow: 0 4px 14px rgb(0 0 0 / 6%);

  cursor: pointer;

  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.store-card:hover {
  transform: translateY(-5px);

  border-color: var(--color-primary);

  box-shadow: 0 10px 26px rgb(0 0 0 / 12%);
}

/* 頂部小裝飾 */
.store-card::before {
  position: absolute;

  top: 0;
  right: 0;
  left: 0;

  height: 6px;

  content: '';

  background: var(--color-primary);

  border-radius: 16px 16px 0 0;
}

/* =========================
   Logo
   ========================= */

.store-logo-wrapper {
  display: flex;
  width: 100%;
  justify-content: center;

  margin-bottom: 22px;
}

.store-logo,
.store-logo-placeholder {
  width: 96px;
  height: 96px;

  border-radius: 50%;
}

.store-logo {
  object-fit: cover;

  background: var(--color-surface);

  border: 4px solid var(--color-surface);

  box-shadow:
    0 0 0 2px var(--color-border),
    0 5px 14px rgb(0 0 0 / 12%);
}

.store-logo-placeholder {
  display: flex;

  align-items: center;
  justify-content: center;

  color: var(--color-text-muted);
  font-size: 36px;

  background: var(--color-surface-soft);

  border: 2px solid var(--color-border);
}

/* 賣家名稱 */
.store-name {
  width: 100%;

  margin: 0 0 18px;

  padding-bottom: 16px;

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;

  text-align: center;

  border-bottom: 1px solid var(--color-border);
}

/* 賣家介紹 */
.store-description {
  display: -webkit-box;

  width: 100%;

  overflow: hidden;

  margin: 0;

  color: var(--color-text-muted);

  font-size: 14px;
  line-height: 1.8;
  text-align: center;

  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

/* 手機版 */
@media (max-width: 575.98px) {
  .store-list {
    grid-template-columns: 1fr;
  }

  .store-card {
    max-width: 360px;
    margin-inline: auto;
  }
}
</style>
