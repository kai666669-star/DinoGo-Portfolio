<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'
import { useCartStore } from '@/stores/cart'

// ================================
// 收藏商品
// ================================

const favorites = ref([])

const loading = ref(false)
const errorMessage = ref('')
const removingId = ref(null)
const addingCartId = ref(null)
const showCartSuccess = ref(false)
const cartStore = useCartStore()
// ================================
// 規格選擇
// ================================

const showSkuModal = ref(false)
const selectedFavorite = ref(null)
const selectedSkuId = ref(null)

// ================================
// 取得收藏
// ================================

const loadFavorites = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get('/favorites')

    favorites.value = (response.data || []).sort((a, b) => {
      // 1. 可購買商品排前面
      if (a.available !== b.available) {
        return a.available ? -1 : 1
      }

      // 2. 同樣狀態下，按照 productId 小到大
      return Number(a.productId) - Number(b.productId)
    })
  } catch (error) {
    logSafeError('取得收藏失敗：', error)

    errorMessage.value = error.response?.data?.message || '無法取得收藏商品'
  } finally {
    loading.value = false
  }
}

// ================================
// 移除收藏
// ================================

const removeFavorite = async (productId) => {
  try {
    removingId.value = productId

    await api.delete(`/favorites/${productId}`)

    favorites.value = favorites.value.filter((item) => item.productId !== productId)
  } catch (error) {
    logSafeError('移除收藏失敗：', error)

    alert(error.response?.data?.message || '移除收藏失敗，請稍後再試')
  } finally {
    removingId.value = null
  }
}

// ================================
// 商品是否下架
// ================================

const isProductUnavailable = (favorite) => {
  return Number(favorite.productStatus) !== 1
}

const isSkuAvailable = (sku) => {
  if (!sku) {
    return false
  }

  return sku.available === true
}

const isFavoriteAvailable = (favorite) => {
  return favorite?.available === true
}

const getUnavailableText = (favorite) => {
  if (Number(favorite.productStatus) !== 1) {
    return '已下架'
  }

  if (Number(favorite.skuStatus) !== 1) {
    return '商品無法購買'
  }

  if (Number(favorite.skuStock || 0) <= 0) {
    return '庫存不足'
  }

  return ''
}

// ================================
// 取得可購買 SKU
// ================================

// const getAvailableSkus = (favorite) => {
//   if (!favorite?.skus) {
//     return []
//   }

//   return favorite.skus.filter((sku) => isSkuAvailable(sku))
// }

// ================================
// 開啟規格選擇
// ================================
const openSkuSelector = (favorite) => {
  if (!isFavoriteAvailable(favorite)) {
    return
  }

  selectedFavorite.value = favorite

  // 預設選第一個有庫存的 SKU
  const firstAvailableSku = favorite.skus?.find((sku) => sku.available === true)

  selectedSkuId.value = firstAvailableSku?.skuId ?? null

  showSkuModal.value = true
}

// ================================
// 關閉規格選擇
// ================================

const closeSkuSelector = () => {
  if (addingCartId.value !== null) {
    return
  }

  showSkuModal.value = false
  selectedFavorite.value = null
  selectedSkuId.value = null
}
const getUnavailableReason = (favorite) => {
  if (Number(favorite.productStatus) !== 1) {
    return '已下架'
  }

  if (Number(favorite.skuStatus) !== 1) {
    return '商品無法購買'
  }

  if (Number(favorite.skuStock || 0) <= 0) {
    return '庫存不足'
  }

  return ''
}
// ================================
// 加入購物車
// ================================

const addToCart = async () => {
  if (!selectedFavorite.value || !selectedSkuId.value) {
    return
  }

  const favorite = selectedFavorite.value

  // 商品下架
  if (Number(favorite.productStatus) !== 1) {
    alert('此商品目前已下架，無法加入購物車')
    return
  }

  // 找目前選擇的 SKU
  const selectedSku = favorite.skus?.find((sku) => sku.skuId === Number(selectedSkuId.value))

  if (!selectedSku) {
    alert('找不到商品規格')
    return
  }

  // SKU 不可購買
  if (!isSkuAvailable(selectedSku)) {
    if (Number(selectedSku.skuStatus) !== 1) {
      alert('此商品規格目前未啟用')
    } else if (Number(selectedSku.skuStock || 0) <= 0) {
      alert('此規格目前庫存不足')
    }

    return
  }

  try {
    addingCartId.value = favorite.productId

    await api.post('/cart/items', {
      skuId: Number(selectedSku.skuId),
      quantity: 1,
    })

    // 同步更新 Header 購物車數量
    await cartStore.fetchCart()

    // 先結束加入購物車狀態
    addingCartId.value = null

    // 關閉規格選擇視窗
    closeSkuSelector()

    // 顯示中央成功提示
    showCartSuccess.value = true

    // 1 秒後自動關閉
    setTimeout(() => {
      showCartSuccess.value = false
    }, 1000)
  } catch (error) {
    logSafeError('加入購物車失敗：', error)

    if (error.response?.status === 401) {
      alert('請先登入會員')
      return
    }

    alert(error.response?.data?.message || '加入購物車失敗，請稍後再試')
  } finally {
    addingCartId.value = null
  }
}

// ================================
// 金額格式
// ================================

const formatPrice = (price) => {
  return Number(price || 0).toLocaleString('zh-TW')
}

// ================================
// 初始化
// ================================

onMounted(() => {
  loadFavorites()
})
</script>
<template>
  <main class="favorite-page">
    <div class="container favorite-container">
      <!-- ================================
           Page Header
      ================================= -->

      <header class="favorite-header">
        <div>
          <h1 class="favorite-title">我的收藏</h1>

          <p class="favorite-description">管理收藏商品並快速加入購物車</p>
        </div>

        <span class="favorite-count"> {{ favorites.length }} 件商品 </span>
      </header>

      <!-- ================================
           Loading
      ================================= -->

      <div v-if="loading" class="state-message" role="status">
        <i class="bi bi-arrow-repeat loading-icon"></i>
        <span>正在載入收藏...</span>
      </div>

      <!-- ================================
           Error
      ================================= -->

      <div v-else-if="errorMessage" class="state-message state-message-error" role="alert">
        <i class="bi bi-exclamation-circle"></i>

        <span>
          {{ errorMessage }}
        </span>
      </div>

      <!-- ================================
           收藏內容
      ================================= -->

      <div v-else-if="favorites.length > 0" class="favorite-grid">
        <!-- 收藏商品 -->

        <article
          v-for="favorite in favorites"
          :key="favorite.favoriteId"
          class="favorite-card"
          :class="{
            unavailable: !isFavoriteAvailable(favorite),
          }"
        >
          <!-- 商品圖片 -->

          <RouterLink
            v-if="isFavoriteAvailable(favorite)"
            :to="{
              name: 'ProductDetail',
              params: {
                id: favorite.productId,
              },
            }"
            class="product-link"
          >
            <div class="product-image">
              <img
                v-if="favorite.imageUrl"
                :src="getImageUrl(favorite.imageUrl)"
                :alt="favorite.productName"
              />

              <div v-else class="image-placeholder" aria-label="沒有商品圖片">
                <i class="bi bi-image"></i>
              </div>
            </div>

            <div class="product-info">
              <h2 class="product-name">
                {{ favorite.productName }}
              </h2>

              <p class="product-price">NT$ {{ formatPrice(favorite.basePrice) }}</p>
            </div>
          </RouterLink>

          <!-- 不可購買商品 -->

          <div v-else class="product-link product-link-disabled">
            <div class="product-image">
              <img
                v-if="favorite.imageUrl"
                :src="getImageUrl(favorite.imageUrl)"
                :alt="favorite.productName"
              />

              <div v-else class="image-placeholder" aria-label="沒有商品圖片">
                <i class="bi bi-image"></i>
              </div>

              <div class="unavailable-overlay">
                <span class="unavailable-line"></span>

                <span class="unavailable-text">
                  {{ getUnavailableText(favorite) }}
                </span>

                <span class="unavailable-line"></span>
              </div>
            </div>

            <div class="product-info">
              <h2 class="product-name">
                {{ favorite.productName }}
              </h2>

              <p class="product-price">NT$ {{ formatPrice(favorite.basePrice) }}</p>
            </div>
          </div>

          <!-- 商品操作 -->

          <div class="product-actions">
            <!-- 收藏愛心 -->

            <button
              type="button"
              class="favorite-button"
              :disabled="removingId === favorite.productId"
              title="移除收藏"
              aria-label="移除收藏"
              @click="removeFavorite(favorite.productId)"
            >
              <i
                class="bi"
                :class="removingId === favorite.productId ? 'bi-arrow-repeat' : 'bi-heart-fill'"
              ></i>
            </button>

            <!-- 加入購物車 -->

            <button
              type="button"
              class="cart-button"
              :disabled="!isFavoriteAvailable(favorite) || addingCartId === favorite.productId"
              @click="openSkuSelector(favorite)"
            >
              <i
                class="bi"
                :class="addingCartId === favorite.productId ? 'bi-arrow-repeat' : 'bi-cart-plus'"
              ></i>

              <span>
                {{
                  addingCartId === favorite.productId
                    ? '加入中...'
                    : !isFavoriteAvailable(favorite)
                      ? getUnavailableText(favorite)
                      : '加入購物車'
                }}
              </span>
            </button>
          </div>
        </article>
      </div>
      <!-- ================================
           沒有收藏商品
      ================================= -->

      <div v-else class="empty-favorite">
        <div class="empty-icon">
          <i class="bi bi-heart"></i>
        </div>

        <h2>收藏商品會顯示在這裡</h2>

        <p>立刻去逛逛，找到喜歡的商品吧</p>
      </div>
      <!-- ========================================
     SKU 選擇 Modal
======================================== -->

      <div
        v-if="showSkuModal && selectedFavorite"
        class="sku-modal-backdrop"
        @click.self="closeSkuSelector"
      >
        <div class="sku-modal">
          <!-- Header -->

          <div class="sku-modal-header">
            <h2>選擇商品規格</h2>

            <button
              type="button"
              class="sku-close-button"
              :disabled="addingCartId !== null"
              @click="closeSkuSelector"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <!-- 商品 -->

          <div class="sku-product">
            <img
              v-if="selectedFavorite.imageUrl"
              :src="selectedFavorite.imageUrl"
              :alt="selectedFavorite.productName"
            />

            <div>
              <strong>
                {{ selectedFavorite.productName }}
              </strong>

              <span>
                NT$
                {{ formatPrice(selectedFavorite.basePrice) }}
              </span>
            </div>
          </div>

          <!-- 規格 -->

          <div class="sku-selection">
            <label class="sku-modal-label">商品規格</label>

            <div class="sku-option-list">
              <label
                v-for="sku in selectedFavorite.skus"
                :key="sku.skuId"
                class="sku-option"
                :class="{
                  selected: selectedSkuId === sku.skuId,
                  disabled: !isSkuAvailable(sku),
                }"
              >
                <input
                  v-model="selectedSkuId"
                  type="radio"
                  name="favoriteSku"
                  :value="sku.skuId"
                  :disabled="!isSkuAvailable(sku)"
                />

                <span class="sku-radio"></span>

                <span class="sku-option-content">
                  <strong>
                    {{ sku.skuName }}
                  </strong>

                  <span v-if="Number(sku.skuStatus) !== 1"> 商品無法購買 </span>

                  <span v-else-if="Number(sku.skuStock || 0) <= 0"> 庫存不足 </span>

                  <span v-else> 庫存 {{ sku.skuStock }} 件 </span>
                </span>
              </label>
            </div>
          </div>

          <!-- Footer -->

          <div class="sku-modal-footer">
            <button
              type="button"
              class="sku-cancel-button"
              :disabled="addingCartId !== null"
              @click="closeSkuSelector"
            >
              取消
            </button>

            <button
              type="button"
              class="sku-confirm-button"
              :disabled="!selectedSkuId || addingCartId !== null"
              @click="addToCart"
            >
              <span v-if="addingCartId !== null">
                <span class="spinner-border spinner-border-sm" role="status"></span>

                加入中...
              </span>

              <span v-else>
                <i class="bi bi-cart-plus"></i>
                加入購物車
              </span>
            </button>
          </div>
        </div>
      </div>
      <!-- ================================
           加入購物車成功提示
      ================================ -->

      <div v-if="showCartSuccess" class="cart-success-backdrop">
        <div class="cart-success-message">
          <div class="cart-success-icon">
            <i class="bi bi-check-lg"></i>
          </div>

          <div class="cart-success-content">
            <strong>已加入購物車</strong>
            <span>商品已成功加入購物車</span>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ========================================
   Favorite Page
======================================== */

.favorite-page {
  width: 100%;
  min-height: 500px;

  background: var(--color-bg);
  color: var(--color-text);
}

.favorite-container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
  padding-block: 40px;

  box-sizing: border-box;
}

/* ========================================
   Page Header
======================================== */

.favorite-header {
  display: flex;
  min-height: 68px;
  align-items: flex-end;
  justify-content: space-between;

  gap: var(--space-5);

  margin-bottom: var(--space-5);
}

.favorite-title {
  margin: 0 0 var(--space-1);

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;

  line-height: var(--line-height-heading);
}

.favorite-description {
  margin: 0;

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  line-height: 1.5;
}

.favorite-count {
  flex-shrink: 0;

  padding: var(--space-2) var(--space-3);

  color: var(--color-primary);

  background: var(--color-primary-soft);

  border-radius: var(--radius-pill);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

/* ========================================
   Favorite Grid
======================================== */

.favorite-grid {
  display: grid;

  grid-template-columns: repeat(4, minmax(0, 1fr));

  gap: var(--space-5);
}

/* ========================================
   Product Card
======================================== */

.favorite-card {
  min-width: 0;

  display: flex;
  flex-direction: column;

  padding: var(--space-3);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.favorite-card:hover {
  transform: translateY(-2px);

  box-shadow: var(--shadow-sm);
}

/* ========================================
   Product Image
======================================== */

.product-image {
  width: 100%;
  aspect-ratio: 1 / 1;

  display: flex;

  align-items: center;
  justify-content: center;

  overflow: hidden;

  background: var(--color-surface-soft);

  border-radius: var(--radius-md);
}

.product-image img {
  width: 100%;
  height: 100%;

  display: block;

  object-fit: contain;
}

.image-placeholder {
  width: 100%;
  height: 100%;

  display: flex;

  align-items: center;
  justify-content: center;

  color: var(--color-text-subtle);

  font-size: var(--font-size-xl);
}

/* ========================================
   Product Info
======================================== */

.product-info {
  padding: var(--space-4) var(--space-1) var(--space-3);
}

.product-name {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  line-height: 1.5;

  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;

  overflow: hidden;
}

.product-price {
  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 700;

  line-height: 1.4;
}

/* ========================================
   Product Actions
======================================== */

.product-actions {
  display: flex;
  align-items: center;

  gap: var(--space-2);

  margin-top: auto;
}

/* ========================================
   Favorite Button
======================================== */

.favorite-button {
  width: 40px;
  min-width: 40px;
  height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  padding: 0;

  color: var(--color-primary);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  font-size: 18px;

  cursor: pointer;

  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.favorite-button:hover:not(:disabled) {
  color: var(--color-primary-hover);

  background: var(--color-primary-soft);
}

.favorite-button:active:not(:disabled) {
  color: var(--color-primary-active);
}

.favorite-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.favorite-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

/* ========================================
   Cart Button
======================================== */

.cart-button {
  flex: 1;

  min-width: 0;
  height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: 0 var(--space-3);

  color: var(--color-surface);

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.cart-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.cart-button:active:not(:disabled) {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.cart-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.cart-button:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

/* ========================================
   Empty
======================================== */

.empty-favorite {
  min-height: 360px;

  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  padding: var(--space-7);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  text-align: center;
}

.empty-icon {
  margin-bottom: var(--space-4);

  color: var(--color-primary);

  font-size: 48px;
}

.empty-favorite h2 {
  margin: 0 0 var(--space-2);

  color: var(--color-text);

  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.empty-favorite p {
  margin: 0;

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

/* ========================================
   Loading / Error
======================================== */

.state-message {
  min-height: 240px;

  display: flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: var(--space-6);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-base);

  text-align: center;
}

.state-message-error {
  color: var(--color-danger);

  background: var(--color-surface);
}

.loading-icon {
  animation: loading-spin 1s linear infinite;
}

@keyframes loading-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

/* ========================================
   1024px
======================================== */

@media (max-width: 1024px) {
  .favorite-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* ========================================
   768px
======================================== */

@media (max-width: 768px) {
  .favorite-header {
    align-items: flex-start;

    flex-direction: column;

    margin-bottom: var(--space-5);
  }

  .favorite-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));

    gap: var(--space-4);
  }
}

/* ========================================
   375px
======================================== */

@media (max-width: 480px) {
  .favorite-grid {
    grid-template-columns: 1fr;

    gap: var(--space-4);
  }

  .product-image {
    aspect-ratio: 1 / 1;
  }

  .favorite-card {
    padding: var(--space-3);
  }
}

@media (max-width: 575.98px) {
  .favorite-container {
    padding-block: var(--space-6);
  }
}
.product-link {
  display: block;

  color: inherit;
  text-decoration: none;
}

.product-link:hover {
  color: inherit;
  text-decoration: none;
}

.product-link .product-name {
  color: var(--color-text);
}

.product-link .product-price {
  color: var(--color-primary);
}
/* ========================================
   商品不可購買
======================================== */

.favorite-card.unavailable {
  opacity: 0.6;
}

.favorite-card.unavailable:hover {
  transform: none;
  box-shadow: none;
}

.favorite-card.unavailable .product-link {
  cursor: default;
}

/* ========================================
   商品圖片失效遮罩
======================================== */

.product-image {
  position: relative;
}

.unavailable-overlay {
  position: absolute;
  inset: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  background: rgba(128, 128, 128, 0.35);

  pointer-events: none;
}

.unavailable-text {
  position: relative;
  z-index: 2;

  padding: var(--space-1) var(--space-3);

  color: #444;

  background: rgba(255, 255, 255, 0.9);

  border-radius: var(--radius-sm);

  font-size: var(--font-size-sm);
  font-weight: 700;

  white-space: nowrap;
}

.unavailable-line {
  width: 30px;
  height: 1px;

  background: #555;
}

/* ========================================
   不可購買按鈕
======================================== */

.favorite-card.unavailable .cart-button {
  color: var(--color-text-muted);

  background: var(--color-surface-soft);

  border-color: var(--color-border);

  cursor: not-allowed;
}

/* ========================================
   SKU Modal
======================================== */

.sku-modal-backdrop {
  position: fixed;

  inset: 0;

  z-index: 1000;

  display: flex;

  align-items: center;
  justify-content: center;

  padding: var(--space-4);

  background: rgba(0, 0, 0, 0.45);
}

.sku-modal {
  width: 100%;
  max-width: 520px;

  max-height: 90vh;

  overflow-y: auto;

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-lg);

  box-shadow: var(--shadow-lg);
}

/* ========================================
   SKU Modal Header
======================================== */

.sku-modal-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  padding: var(--space-5);

  border-bottom: 1px solid var(--color-border);
}

.sku-modal-header h2 {
  margin: 0;

  color: var(--color-text);

  font-size: var(--font-size-md);
  font-weight: 700;
}

.sku-close-button {
  width: 36px;
  height: 36px;

  display: flex;

  align-items: center;
  justify-content: center;

  padding: 0;

  color: var(--color-text-muted);

  background: transparent;

  border: 0;

  border-radius: var(--radius-md);

  cursor: pointer;
}

.sku-close-button:hover:not(:disabled) {
  color: var(--color-text);

  background: var(--color-surface-soft);
}

/* ========================================
   SKU Product
======================================== */

.sku-product {
  display: flex;

  align-items: center;

  gap: var(--space-3);

  padding: var(--space-4) var(--space-5);

  background: var(--color-surface-soft);
}

.sku-product img {
  width: 64px;
  height: 64px;

  object-fit: contain;

  border-radius: var(--radius-md);

  background: var(--color-surface);
}

.sku-product > div {
  display: flex;

  flex-direction: column;

  gap: var(--space-1);

  min-width: 0;
}

.sku-product strong {
  color: var(--color-text);

  font-size: var(--font-size-sm);

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.sku-product span {
  color: var(--color-primary);

  font-size: var(--font-size-sm);
  font-weight: 600;
}

/* ========================================
   SKU Selection
======================================== */

.sku-selection {
  padding: var(--space-5);
}

.sku-modal-label {
  display: block;

  margin-bottom: var(--space-3);

  color: var(--color-text);

  font-size: var(--font-size-sm);
  font-weight: 600;
}

.sku-option-list {
  display: flex;

  flex-direction: column;

  gap: var(--space-2);
}

.sku-option {
  position: relative;

  display: flex;

  align-items: center;

  gap: var(--space-3);

  padding: var(--space-3);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-md);

  background: var(--color-surface);

  cursor: pointer;

  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.sku-option:hover:not(.disabled) {
  border-color: var(--color-primary);

  background: var(--color-primary-soft);
}

.sku-option.selected {
  border-color: var(--color-primary);

  background: var(--color-primary-soft);
}

.sku-option.disabled {
  opacity: 0.45;

  cursor: not-allowed;
}

.sku-option input {
  position: absolute;

  width: 1px;
  height: 1px;

  opacity: 0;
}

.sku-radio {
  position: relative;

  width: 18px;
  height: 18px;

  flex: 0 0 18px;

  border: 2px solid var(--color-border);

  border-radius: 50%;
}

.sku-option.selected .sku-radio {
  border-color: var(--color-primary);
}

.sku-option.selected .sku-radio::after {
  content: '';

  position: absolute;

  top: 3px;
  left: 3px;

  width: 8px;
  height: 8px;

  border-radius: 50%;

  background: var(--color-primary);
}

.sku-option-content {
  display: flex;

  flex-direction: column;

  gap: var(--space-1);
}

.sku-option-content strong {
  color: var(--color-text);

  font-size: var(--font-size-sm);
}

.sku-option-content span {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

/* ========================================
   SKU Modal Footer
======================================== */

.sku-modal-footer {
  display: flex;

  gap: var(--space-2);

  padding: var(--space-4) var(--space-5);

  border-top: 1px solid var(--color-border);
}

.sku-cancel-button,
.sku-confirm-button {
  flex: 1;

  min-height: 44px;

  padding: var(--space-2) var(--space-4);

  border-radius: var(--radius-md);

  font-size: var(--font-size-sm);
  font-weight: 600;

  cursor: pointer;
}

.sku-cancel-button {
  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
}

.sku-cancel-button:hover:not(:disabled) {
  background: var(--color-surface-soft);
}

.sku-confirm-button {
  color: var(--color-surface);

  background: var(--color-primary);

  border: 1px solid var(--color-primary);
}

.sku-confirm-button:hover:not(:disabled) {
  background: var(--color-primary-hover);

  border-color: var(--color-primary-hover);
}

.sku-confirm-button:disabled,
.sku-cancel-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

/* ========================================
   Mobile
======================================== */

@media (max-width: 480px) {
  .sku-modal-backdrop {
    align-items: flex-end;

    padding: 0;
  }

  .sku-modal {
    max-width: none;

    max-height: 90vh;

    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }

  .sku-modal-header {
    padding: var(--space-4);
  }

  .sku-selection {
    padding: var(--space-4);
  }

  .sku-modal-footer {
    padding: var(--space-4);
  }
}
.product-link-disabled {
  cursor: default;
}
/* ========================================
   加入購物車成功提示
======================================== */

.cart-success-backdrop {
  position: fixed;

  inset: 0;

  z-index: 2000;

  display: flex;

  align-items: center;
  justify-content: center;

  padding: var(--space-4);

  background: rgba(0, 0, 0, 0.15);

  pointer-events: none;
}

.cart-success-message {
  display: flex;

  align-items: center;

  gap: var(--space-3);

  min-width: 280px;

  padding: var(--space-4) var(--space-5);

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-lg);

  box-shadow: var(--shadow-lg);

  animation: cart-success-show 0.2s ease-out;
}

.cart-success-icon {
  width: 42px;
  height: 42px;

  flex: 0 0 42px;

  display: flex;

  align-items: center;
  justify-content: center;

  color: var(--color-surface);

  background: var(--color-primary);

  border-radius: 50%;

  font-size: 22px;
}

.cart-success-content {
  display: flex;

  flex-direction: column;

  gap: var(--space-1);
}

.cart-success-content strong {
  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 700;
}

.cart-success-content span {
  color: var(--color-text-muted);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

@keyframes cart-success-show {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.96);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
