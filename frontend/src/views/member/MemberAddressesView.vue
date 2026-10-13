<script setup>
import { onMounted, ref } from 'vue'
import AddressForm from '@/components/member/AddressForm.vue'
import { createAddress, deleteAddress, getAddresses, updateAddress } from '@/api/address'

// 地址清單與頁面操作狀態。
const addresses = ref([])
const isLoading = ref(true)
const deletingAddressId = ref(null)
const pageError = ref('')

// 新增與編輯共用同一個 AddressForm modal。
const isFormOpen = ref(false)
const editingAddress = ref(null)
const isSaving = ref(false)
const formError = ref('')

// 將 API 錯誤轉為頁面可讀訊息。
function getErrorMessage(error, fallback) {
  if (!error.response) return '目前無法連線，請稍後再試。'
  if (typeof error.response.data === 'string' && error.response.data.trim()) {
    return error.response.data
  }
  return error.response.data?.message || fallback
}

// 讀取登入會員的地址，後端已將預設地址排在第一筆。
async function loadAddresses() {
  isLoading.value = true
  pageError.value = ''
  try {
    const { data } = await getAddresses()
    addresses.value = Array.isArray(data) ? data : []
    return true
  } catch (error) {
    pageError.value = getErrorMessage(error, '地址載入失敗。')
    return false
  } finally {
    isLoading.value = false
  }
}

// 開啟新增或編輯表單。
function openCreateForm() {
  editingAddress.value = null
  formError.value = ''
  isFormOpen.value = true
}

function openEditForm(address) {
  editingAddress.value = { ...address }
  formError.value = ''
  isFormOpen.value = true
}

function closeForm() {
  if (isSaving.value) return
  isFormOpen.value = false
  editingAddress.value = null
  formError.value = ''
}

// 刪除成功但清單重載失敗時，先同步本地清單避免顯示已刪除的地址。
function removeAddressLocally(address) {
  const remainingAddresses = addresses.value.filter((item) => item.addressId !== address.addressId)

  // 後端刪除預設地址後會將剩餘第一筆設為預設，本地備援狀態保持相同結果。
  if (address.isDefault && remainingAddresses.length) {
    remainingAddresses[0] = { ...remainingAddresses[0], isDefault: true }
  }

  addresses.value = remainingAddresses
}

// 依目前模式呼叫新增或修改 API，成功後重新整理地址清單。
async function saveAddress(request) {
  isSaving.value = true
  formError.value = ''
  try {
    if (editingAddress.value?.addressId) {
      await updateAddress(editingAddress.value.addressId, request)
    } else {
      await createAddress(request)
    }
  } catch (error) {
    formError.value = getErrorMessage(error, '地址儲存失敗。')
    return
  } finally {
    isSaving.value = false
  }

  // 寫入成功就先關閉表單，避免後續清單載入失敗時讓使用者重複送出。
  closeForm()
  await loadAddresses()
}

// 刪除前讓使用者確認；訂單引用衝突會直接顯示後端的 409 訊息。
async function removeAddress(address) {
  const confirmed = window.confirm(`確定要刪除「${address.receiverName}」的收件地址嗎？`)
  if (!confirmed) return

  deletingAddressId.value = address.addressId
  pageError.value = ''
  try {
    await deleteAddress(address.addressId)
    const hasReloaded = await loadAddresses()
    if (!hasReloaded) removeAddressLocally(address)
  } catch (error) {
    pageError.value = getErrorMessage(error, '地址刪除失敗。')
  } finally {
    deletingAddressId.value = null
  }
}

// 依後端欄位組合畫面上的完整配送地址。
function formatAddress(address) {
  return [address.postalCode, address.city, address.district, address.detailAddress]
    .filter(Boolean)
    .join(' ')
}

onMounted(loadAddresses)
</script>

<template>
  <main class="address-page" aria-labelledby="address-page-title">
    <div class="container address-page-inner">
      <!-- 頁面標題與新增地址入口。 -->
      <header class="address-page-header">
        <div>
          <h1 id="address-page-title">地址管理</h1>
          <p>管理訂單使用的收件人與配送地址</p>
        </div>
        <button class="address-add-button" type="button" @click="openCreateForm">新增地址</button>
      </header>

      <!-- API 失敗時提供重新載入。 -->
      <div v-if="pageError" class="address-alert" role="alert">
        <span>{{ pageError }}</span>
        <button type="button" @click="loadAddresses">重新載入</button>
      </div>

      <!-- 等待 API 回應時顯示卡片骨架。 -->
      <section v-if="isLoading" class="address-loading" aria-live="polite" aria-busy="true">
        <article v-for="index in 2" :key="index" class="address-skeleton"></article>
        <span>地址載入中…</span>
      </section>

      <!-- 依預設狀態套用卡片框線與地址標籤。 -->
      <section v-else-if="addresses.length" class="address-grid" aria-label="收件地址清單">
        <article
          v-for="address in addresses"
          :key="address.addressId"
          class="address-card"
          :class="{ 'address-card-default': address.isDefault }"
        >
          <div class="address-card-header">
            <h2>{{ address.receiverName }}</h2>
            <span class="address-badge" :class="{ 'address-badge-default': address.isDefault }">
              {{ address.isDefault ? '預設地址' : '其他地址' }}
            </span>
          </div>

          <!-- 地址管理頁完整顯示電話，方便會員核對收件資料。 -->
          <p class="address-phone">{{ address.receiverPhone || '未提供電話' }}</p>
          <p class="address-detail">{{ formatAddress(address) }}</p>

          <div class="address-card-actions">
            <button type="button" @click="openEditForm(address)">編輯</button>
            <button
              class="address-delete-button"
              type="button"
              :disabled="deletingAddressId === address.addressId"
              @click="removeAddress(address)"
            >
              {{ deletingAddressId === address.addressId ? '刪除中…' : '刪除' }}
            </button>
          </div>
        </article>
      </section>

      <!-- 只有成功載入且沒有任何地址時才顯示空狀態。 -->
      <section
        v-if="!isLoading && !pageError && addresses.length === 0"
        class="address-empty-state"
        aria-live="polite"
      >
        <i class="bi bi-geo-alt" aria-hidden="true"></i>
        <h2>尚未建立地址</h2>
        <p>新增地址後可在結帳時快速選取。</p>
      </section>
    </div>

    <!-- 新增與編輯共用同一個表單元件。 -->
    <AddressForm
      :open="isFormOpen"
      :address="editingAddress"
      :is-saving="isSaving"
      :error-message="formError"
      @close="closeForm"
      @submit="saveAddress"
    />
  </main>
</template>

<style scoped>
/* 地址管理頁的版面容器。 */
.address-page {
  min-height: 520px;
  background: var(--color-bg);
}

.address-page-inner {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
  padding-top: 40px;
  padding-bottom: 40px;
}

/* 頁面標題與新增按鈕。 */
.address-page-header {
  display: flex;
  min-height: 68px;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.address-page-header h1 {
  margin: 0 0 var(--space-1);
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.address-page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.address-add-button {
  min-height: 42px;
  padding: 0 var(--space-4);
  color: var(--color-surface);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.address-add-button:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.address-add-button:focus-visible,
.address-card-actions button:focus-visible,
.address-alert button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

/* API 錯誤提示。 */
.address-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  background: var(--color-danger-soft);
  border-radius: var(--radius-md);
}

.address-alert button {
  flex: 0 0 auto;
  padding: var(--space-1) var(--space-3);
  color: var(--color-danger);
  font-weight: 600;
  background: transparent;
  border: 1px solid currentColor;
  border-radius: var(--radius-sm);
}

/* 地址卡片清單。 */
.address-grid,
.address-loading {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.address-card,
.address-skeleton {
  min-height: 180px;
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.address-card-default {
  border-color: var(--color-primary);
}

.address-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.address-card-header h2 {
  overflow: hidden;
  margin: 0;
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-badge {
  display: inline-flex;
  min-height: 30px;
  flex: 0 0 auto;
  align-items: center;
  padding: var(--space-1) var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
}

/* 預設地址以卡片框線與標籤突顯，其他標籤保留相同高度維持內容對齊。 */
.address-badge-default {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
}

.address-phone {
  margin: 17px 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
}

.address-detail {
  margin: 10px 0 0;
  color: var(--color-text);
  font-size: var(--font-size-base);
}

.address-card-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 13px;
}

.address-card-actions button {
  padding: 0;
  color: var(--color-primary-active);
  font-size: var(--font-size-base);
  font-weight: 600;
  background: transparent;
  border: 0;
}

.address-card-actions .address-delete-button {
  color: var(--color-danger);
}

.address-card-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* 沒有地址時的提示區塊。 */
.address-empty-state {
  display: flex;
  min-height: 340px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-6);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.address-empty-state i {
  color: var(--color-primary);
  font-size: 24px;
}

.address-empty-state h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}

.address-empty-state p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

/* 載入中的骨架卡片。 */
.address-loading {
  position: relative;
}

.address-loading span {
  position: absolute;
  inset: 0;
  display: grid;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  place-items: center;
}

.address-skeleton {
  overflow: hidden;
  background: linear-gradient(
    100deg,
    var(--color-surface) 30%,
    var(--color-primary-50) 50%,
    var(--color-surface) 70%
  );
  background-size: 220% 100%;
  animation: address-shimmer 1.4s infinite linear;
}

@keyframes address-shimmer {
  to {
    background-position-x: -220%;
  }
}

@media (max-width: 767.98px) {
  .address-page-header {
    align-items: stretch;
    flex-direction: column;
    margin-bottom: var(--space-5);
  }

  .address-add-button {
    align-self: flex-start;
  }

  .address-grid,
  .address-loading {
    grid-template-columns: 1fr;
  }

  .address-alert {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 575.98px) {
  .address-page-inner {
    padding-top: var(--space-6);
    padding-bottom: var(--space-6);
  }
}
</style>
