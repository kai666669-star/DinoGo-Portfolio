<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  getSellerProducts,
  publishSellerProduct,
  unpublishSellerProduct,
  deleteSellerProduct,
} from '../../api/sellerProductApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import { getImageUrl } from '@/utils/imageUrl'

const sellerId = computed(() => getCurrentSellerId())
const products = ref([])

const isLoading = ref(false)
const isChangingStatus = ref(false)
const errorMessage = ref('')
const pendingStatusProduct = ref(null)
const pendingDeleteProduct = ref(null)
const isDeleting = ref(false)

const sellerRequiredMessage = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'

const loadProducts = async () => {
  if (!sellerId.value) {
    products.value = []
    errorMessage.value = sellerRequiredMessage
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await getSellerProducts(sellerId.value)

    products.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端商品 API 是否已啟動。'
  } finally {
    isLoading.value = false
  }
}

const openStatusDialog = (product) => {
  pendingStatusProduct.value = product
}

const closeStatusDialog = () => {
  if (isChangingStatus.value) {
    return
  }

  pendingStatusProduct.value = null
}

const confirmStatusChange = async () => {
  if (!pendingStatusProduct.value) {
    return
  }

  if (!sellerId.value) {
    errorMessage.value = sellerRequiredMessage
    pendingStatusProduct.value = null
    return
  }

  const product = pendingStatusProduct.value
  isChangingStatus.value = true
  errorMessage.value = ''

  try {
    if (product.status === 'ACTIVE') {
      await unpublishSellerProduct(product.productId)
    } else {
      await publishSellerProduct(product.productId)
    }

    pendingStatusProduct.value = null
    await loadProducts()
  } catch (error) {
    errorMessage.value =
      product.status === 'ACTIVE'
        ? '商品下架失敗，請稍後再試。'
        : '商品上架失敗，請確認商品資料是否完整。'
  } finally {
    isChangingStatus.value = false
  }
}

const statusActionLabel = (status) => (status === 'ACTIVE' ? '下架' : '上架')

const statusDialogTitle = (product) => {
  if (!product) return ''
  return product.status === 'ACTIVE' ? '確認下架商品' : '確認上架商品'
}

const statusDialogDescription = (product) => {
  if (!product) return ''

  if (product.status === 'ACTIVE') {
    return '下架後，買家將不會在前台看到這項商品；你仍可在賣家中心再次上架。'
  }

  return '上架後，買家將可以在前台瀏覽並購買這項商品。'
}

const formatCurrency = (price) => `NT$${Number(price ?? 0).toLocaleString()}`

const stockText = (stock) => stock ?? 0

const statusLabel = (status) => {
  if (status === 'ACTIVE') {
    return '上架中'
  }
  if (status === 'INACTIVE') {
    return '已下架'
  }
  return '草稿'
}

const statusClass = (status) => {
  if (status === 'ACTIVE') {
    return 'is-active'
  }
  if (status === 'INACTIVE') {
    return 'is-disabled'
  }
  return 'is-warning'
}

const openDeleteDialog = (product) => {
  pendingDeleteProduct.value = product
}

const closeDeleteDialog = () => {
  if (isDeleting.value) {
    return
  }

  pendingDeleteProduct.value = null
}

// 刪除商品
const confirmDelete = async () => {
  if (!pendingDeleteProduct.value) {
    return
  }

  const product = pendingDeleteProduct.value

  isDeleting.value = true
  errorMessage.value = ''

  try {
    await deleteSellerProduct(product.productId)

    pendingDeleteProduct.value = null

    await loadProducts()
  } catch (error) {
    errorMessage.value = '商品刪除失敗，請稍後再試。'
  } finally {
    isDeleting.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>賣家商品列表</h1>
      </div>

      <RouterLink class="primary-action" to="/seller/products/new">
        <i class="bi bi-plus-lg" aria-hidden="true"></i>
        新增商品
      </RouterLink>
    </header>

    <p v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </p>

    <section class="table-panel">
      <div class="table-header">
        <span>商品名稱</span>
        <span>價格</span>
        <span>庫存</span>
        <span>狀態</span>
        <span>操作</span>
      </div>

      <p v-if="isLoading" class="state-message">商品資料載入中...</p>

      <p v-else-if="products.length === 0" class="state-message">目前沒有商品資料。</p>

      <div v-for="product in products" v-else :key="product.productId" class="product-row">
        <div class="product-info">
          <div class="product-thumb" aria-hidden="true">
            <img
              v-if="product.imageUrl"
              :src="getImageUrl(product.imageUrl)"
              :alt="product.productName"
            />
            <span v-else>{{ product.productName?.slice(0, 1) || '商' }}</span>
          </div>
          <div class="product-name">
            <RouterLink :to="`/seller/products/${product.productId}/edit`">
              {{ product.productName }}
            </RouterLink>
            <span>#SP-{{ String(product.productId).padStart(4, '0') }}</span>
          </div>
        </div>
        <span class="product-price">{{ formatCurrency(product.basePrice) }}</span>
        <span class="product-stock">{{ stockText(product.stock) }}</span>
        <span class="status-badge" :class="statusClass(product.status)">
          {{ statusLabel(product.status) }}
        </span>
        <div class="row-actions">
          <button
            class="status-action"
            type="button"
            :class="product.status === 'ACTIVE' ? 'is-danger' : 'is-primary'"
            @click="openStatusDialog(product)"
          >
            {{ statusActionLabel(product.status) }}
          </button>

          <button class="delete-action" type="button" @click="openDeleteDialog(product)">
            刪除
          </button>
        </div>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="pendingStatusProduct" class="modal-backdrop" @click.self="closeStatusDialog">
        <section
          class="status-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="status-modal-title"
        >
          <div
            class="modal-icon"
            :class="pendingStatusProduct.status === 'ACTIVE' ? 'is-danger' : 'is-primary'"
          >
            <i
              class="bi"
              :class="pendingStatusProduct.status === 'ACTIVE' ? 'bi-box-arrow-down' : 'bi-shop'"
              aria-hidden="true"
            ></i>
          </div>

          <div class="modal-copy">
            <h2 id="status-modal-title">{{ statusDialogTitle(pendingStatusProduct) }}</h2>

            <p>
              確定要{{ statusActionLabel(pendingStatusProduct.status) }}「{{
                pendingStatusProduct.productName
              }}」嗎？
              {{ statusDialogDescription(pendingStatusProduct) }}
            </p>
          </div>

          <div class="modal-actions">
            <button
              class="confirm-action"
              :class="pendingStatusProduct.status === 'ACTIVE' ? 'is-danger' : 'is-primary'"
              type="button"
              :disabled="isChangingStatus"
              @click="confirmStatusChange"
            >
              {{
                isChangingStatus
                  ? `${statusActionLabel(pendingStatusProduct.status)}中...`
                  : `確認${statusActionLabel(pendingStatusProduct.status)}`
              }}
            </button>

            <button
              class="ghost-action"
              type="button"
              :disabled="isChangingStatus"
              @click="closeStatusDialog"
            >
              取消
            </button>
          </div>
        </section>
      </div>

      <div v-if="pendingDeleteProduct" class="modal-backdrop" @click.self="closeDeleteDialog">
        <section
          class="status-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-modal-title"
        >
          <div class="modal-icon is-danger">
            <i class="bi bi-trash" aria-hidden="true"></i>
          </div>

          <div class="modal-copy">
            <h2 id="delete-modal-title">確認刪除商品</h2>

            <p>確定要刪除「{{ pendingDeleteProduct.productName }}」嗎？ 刪除後將無法復原。</p>
          </div>

          <div class="modal-actions">
            <button
              class="confirm-action is-danger"
              type="button"
              :disabled="isDeleting"
              @click="confirmDelete"
            >
              {{ isDeleting ? '刪除中...' : '確認刪除' }}
            </button>

            <button
              class="ghost-action"
              type="button"
              :disabled="isDeleting"
              @click="closeDeleteDialog"
            >
              取消
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.primary-action,
.status-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  font-weight: 600;
  text-decoration: none;
}

.primary-action {
  gap: var(--space-2);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.status-action.is-danger {
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.status-action.is-primary {
  border: 1px solid var(--color-primary);
  color: var(--color-surface);
  background: var(--color-primary);
}

.primary-action:focus-visible,
.status-action:focus-visible,
.ghost-action:focus-visible,
.confirm-action:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.error-message {
  margin: 0;
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.table-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-header,
.product-row {
  display: grid;
  grid-template-columns: minmax(220px, 2fr) 1fr 1fr 1fr 160px;
  gap: var(--space-3);
  align-items: center;
}

.table-header {
  padding: var(--space-4) var(--space-5);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.product-row {
  min-height: 72px;
  padding: 0 var(--space-5);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
}

.product-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.product-thumb {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-name {
  display: grid;
  gap: var(--space-1);
  min-width: 0;
}

.product-name a {
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: 800;
  text-decoration: none;
}

.product-name a:hover {
  color: var(--color-primary);
}

.product-price,
.product-stock {
  color: var(--color-text-900);
  font-size: var(--font-size-base);
  font-weight: 800;
}

.product-name span,
.state-message {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.state-message {
  margin: 0;
  padding: var(--space-6);
}

.status-badge {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-pill);
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-badge.is-active {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.status-badge.is-disabled {
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
}

.row-actions {
  display: flex;
  gap: var(--space-2);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: rgba(15, 23, 42, 0.42);
}

.status-modal {
  width: min(420px, 100%);
  display: grid;
  gap: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  padding: var(--space-5);
  box-shadow: var(--shadow-lg);
}

.modal-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  font-size: var(--font-size-lg);
}

.modal-icon.is-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.modal-icon.is-primary {
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
}

.modal-copy {
  display: grid;
  gap: var(--space-2);
}

.modal-copy h2,
.modal-copy p {
  margin: 0;
}

.modal-copy h2 {
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
}

.modal-copy p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.7;
}

.modal-actions {
  display: flex;
  justify-content: flex-start;
  gap: var(--space-3);
}

.ghost-action,
.confirm-action {
  min-height: 38px;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font: inherit;
  font-weight: 700;
}

.ghost-action {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-700);
}

.confirm-action.is-danger {
  border: 1px solid var(--color-danger);
  background: var(--color-danger);
  color: var(--color-surface);
}

.confirm-action.is-primary {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.ghost-action:disabled,
.confirm-action:disabled {
  cursor: wait;
  opacity: 0.72;
}

.delete-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-danger);
  font-weight: 600;
}

.delete-action:hover {
  background: var(--color-danger-soft);
}

.delete-action:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

@media (max-width: 900px) {
  .table-header {
    display: none;
  }

  .product-row {
    grid-template-columns: 1fr 1fr;
    gap: var(--space-2);
    padding: var(--space-4) var(--space-5);
  }

  .product-info,
  .row-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 680px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
