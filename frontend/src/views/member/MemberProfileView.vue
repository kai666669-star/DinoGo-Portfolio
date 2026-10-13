<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import flatpickr from 'flatpickr'
import { MandarinTraditional } from 'flatpickr/dist/l10n/zh-tw'
import 'flatpickr/dist/flatpickr.css'
import { getMemberProfile, updateMemberProfile } from '@/api/member'
import { useAuthStore } from '@/stores/auth'

const form = ref({
  email: '',
  lastName: '',
  firstName: '',
  birthDate: '',
  phone: '',
  emailOrderNotifications: false,
  emailMarketingNotifications: false,
})
const isLoading = ref(true)
const hasLoadedProfile = ref(false)
const isSaving = ref(false)
const isEditing = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const initialForm = ref(null)
const memberStatus = ref('')
const memberCreatedAt = ref('')
const memberUpdatedAt = ref('')
const authStore = useAuthStore()
const birthDateInput = ref(null)
let birthDatePicker = null

// 將 API 資料整理成表單欄位，並保留取消編輯時可還原的版本。
function applyProfile(profile) {
  const profileForm = {
    email: profile.email ?? '',
    lastName: profile.lastName ?? '',
    firstName: profile.firstName ?? '',
    birthDate: profile.birthDate ?? '',
    phone: profile.phone ?? '',
    emailOrderNotifications: profile.emailOrderNotifications ?? true,
    emailMarketingNotifications: profile.emailMarketingNotifications ?? false,
  }
  form.value = { ...profileForm }
  initialForm.value = { ...profileForm }
  memberStatus.value = profile.status ?? ''
  memberCreatedAt.value = profile.createdAt ?? ''
  memberUpdatedAt.value = profile.updatedAt ?? ''
}

// 帳戶狀態只使用後端回傳值，避免在畫面上寫死會員資料。
const memberStatusLabel = computed(() =>
  memberStatus.value === 'ACTIVE' ? '帳戶正常' : '帳戶狀態異常',
)

// 將後端的 LocalDateTime 顯示為會員頁一致的日期格式。
function formatMemberTimestamp(value, includeTime = false) {
  if (!value) return '—'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'

  const dateText = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join(' / ')
  if (!includeTime) return dateText

  return `${dateText} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function getErrorMessage(error, fallback) {
  if (!error.response) return '目前無法連線，請稍後再試。'
  if (typeof error.response.data === 'string' && error.response.data.trim()) {
    return error.response.data
  }
  return error.response.data?.message || fallback
}

// 載入會員基本資料與帳戶狀態。
async function loadProfile() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const { data } = await getMemberProfile()
    applyProfile(data)
    hasLoadedProfile.value = true
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '會員資料載入失敗。')
  } finally {
    isLoading.value = false
  }
}

// 儲存可編輯欄位後，同步更新登入狀態中的會員資料。
async function saveProfile() {
  errorMessage.value = ''
  successMessage.value = ''
  isSaving.value = true
  try {
    const { data } = await updateMemberProfile({
      lastName: form.value.lastName,
      firstName: form.value.firstName,
      birthDate: form.value.birthDate || null,
      phone: form.value.phone || null,
      emailOrderNotifications: form.value.emailOrderNotifications,
      emailMarketingNotifications: form.value.emailMarketingNotifications,
    })
    applyProfile(data)
    authStore.updateMember(data)
    isEditing.value = false
    successMessage.value = '會員資料已儲存。'
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '會員資料儲存失敗。')
  } finally {
    isSaving.value = false
  }
}

// 開始編輯前清除舊訊息，讓使用者專注目前的欄位內容。
function startEditing() {
  errorMessage.value = ''
  successMessage.value = ''
  isEditing.value = true
}

// 取消時還原最近一次成功讀取或儲存後的資料。
function cancelEditing() {
  if (initialForm.value) form.value = { ...initialForm.value }
  errorMessage.value = ''
  isEditing.value = false
}

function syncBirthDatePickerDisabledState() {
  if (!birthDatePicker) return

  const isDisabled = !isEditing.value
  birthDatePicker.set('clickOpens', !isDisabled)
  birthDatePicker.input.disabled = isDisabled
  birthDatePicker.altInput.disabled = isDisabled
}

function initializeBirthDatePicker() {
  if (!birthDateInput.value) return

  birthDatePicker = flatpickr(birthDateInput.value, {
    altInput: true,
    altInputClass: 'profile-birth-date-picker__input',
    altFormat: 'Y/m/d',
    allowInput: true,
    dateFormat: 'Y-m-d',
    defaultDate: form.value.birthDate || null,
    disableMobile: true,
    locale: MandarinTraditional,
    maxDate: new Date(),
    monthSelectorType: 'dropdown',
    onChange: (_, dateString) => {
      form.value.birthDate = dateString
    },
    onReady: (_, __, instance) => {
      instance.calendarContainer.classList.add('profile-birth-date-calendar')
    },
  })
  syncBirthDatePickerDisabledState()
}

watch(
  () => form.value.birthDate,
  (birthDate) => {
    if (!birthDatePicker || birthDatePicker.input.value === birthDate) return
    birthDatePicker.setDate(birthDate || null, false, 'Y-m-d')
  },
)

watch(isEditing, syncBirthDatePickerDisabledState)

onMounted(async () => {
  await loadProfile()
  await nextTick()
  initializeBirthDatePicker()
})

onBeforeUnmount(() => {
  birthDatePicker?.destroy()
  birthDatePicker = null
})
</script>

<template>
  <main class="profile-page" aria-labelledby="profile-title">
    <div class="container profile-page-inner">
      <!-- 頁面標題與編輯入口。 -->
      <header class="profile-page-header">
        <div>
          <h1 id="profile-title">個人資料</h1>
          <p>管理會員基本資料與聯絡方式</p>
        </div>
        <button
          class="profile-edit-button"
          type="button"
          :disabled="isLoading || !hasLoadedProfile || isEditing"
          @click="startEditing"
        >
          {{ isEditing ? '編輯中' : '編輯資料' }}
        </button>
      </header>

      <!-- 載入期間保留頁面資訊層級。 -->
      <div v-if="isLoading" class="profile-loading" role="status">載入會員資料中…</div>

      <!-- 成功載入後顯示基本資料與帳戶狀態。 -->
      <div v-else-if="hasLoadedProfile" class="profile-content">
        <section class="dg-card profile-basic-card" aria-labelledby="profile-basic-title">
          <h2 id="profile-basic-title">基本資料</h2>

          <div v-if="errorMessage" class="profile-alert profile-alert-error" role="alert">
            {{ errorMessage }}
          </div>
          <div v-if="successMessage" class="profile-alert profile-alert-success" role="status">
            {{ successMessage }}
          </div>

          <form @submit.prevent="saveProfile">
            <div class="profile-form-grid">
              <label class="profile-field" for="profile-email">
                <span>電子信箱</span>
                <input id="profile-email" v-model="form.email" type="email" readonly />
              </label>
              <label class="profile-field" for="profile-last-name">
                <span>姓氏</span>
                <input
                  id="profile-last-name"
                  v-model.trim="form.lastName"
                  :disabled="!isEditing"
                  required
                  maxlength="50"
                />
              </label>
              <label class="profile-field" for="profile-first-name">
                <span>名字</span>
                <input
                  id="profile-first-name"
                  v-model.trim="form.firstName"
                  :disabled="!isEditing"
                  required
                  maxlength="50"
                />
              </label>
              <label class="profile-field" for="profile-phone">
                <span>電話</span>
                <input
                  id="profile-phone"
                  v-model.trim="form.phone"
                  :disabled="!isEditing"
                  maxlength="20"
                />
              </label>
              <label class="profile-field" for="profile-birth-date">
                <span>生日</span>
                <input
                  id="profile-birth-date"
                  ref="birthDateInput"
                  class="profile-birth-date-picker"
                  type="text"
                  autocomplete="bday"
                />
              </label>
            </div>

            <!-- 未編輯時保留操作列空間，避免切換編輯狀態造成卡片高度跳動。 -->
            <fieldset class="profile-email-preferences" :disabled="!isEditing">
              <legend>電子郵件通知</legend>
              <p class="profile-email-preferences__hint">
                請選擇您同意接收的電子郵件訊息類型。
              </p>

              <label class="profile-checkbox-field" for="profile-email-order-notifications">
                <input
                  id="profile-email-order-notifications"
                  v-model="form.emailOrderNotifications"
                  type="checkbox"
                />
                <span>
                  <strong>訂單訊息</strong>
                  <small>接收訂單成立、付款、出貨與配送狀態通知。</small>
                </span>
              </label>

              <label class="profile-checkbox-field" for="profile-email-marketing-notifications">
                <input
                  id="profile-email-marketing-notifications"
                  v-model="form.emailMarketingNotifications"
                  type="checkbox"
                />
                <span>
                  <strong>行銷訊息</strong>
                  <small>接收優惠活動、新品與推薦商品資訊。</small>
                </span>
              </label>
            </fieldset>

            <footer
              class="profile-form-actions"
              :class="{ 'profile-form-actions-placeholder': !isEditing }"
            >
              <button
                class="profile-cancel-button"
                type="button"
                :disabled="!isEditing || isSaving"
                @click="cancelEditing"
              >
                取消
              </button>
              <button class="profile-save-button" type="submit" :disabled="!isEditing || isSaving">
                {{ isSaving ? '儲存中…' : '儲存變更' }}
              </button>
            </footer>
          </form>
        </section>

        <!-- 狀態卡只顯示目前 API 已提供的會員資訊。 -->
        <aside class="dg-card profile-status-card" aria-labelledby="profile-status-title">
          <h2 id="profile-status-title">會員狀態</h2>
          <p class="profile-status-badge">
            <span aria-hidden="true"></span>
            {{ memberStatusLabel }}
          </p>
          <!-- 日期由會員 profile API 動態提供，儲存後會同步更新上次修改時間。 -->
          <p class="profile-member-meta">
            加入日期<br />{{ formatMemberTimestamp(memberCreatedAt) }}
          </p>
          <p class="profile-member-meta">
            上次修改<br />{{ formatMemberTimestamp(memberUpdatedAt, true) }}
          </p>
        </aside>
      </div>

      <!-- 載入失敗時提供重新嘗試。 -->
      <section v-else class="dg-card profile-retry" aria-live="polite">
        <p v-if="errorMessage" class="profile-alert profile-alert-error" role="alert">
          {{ errorMessage }}
        </p>
        <button class="profile-cancel-button" type="button" @click="loadProfile">重新載入</button>
      </section>
    </div>
  </main>
</template>

<style scoped>
/* 個人資料頁與會員地址頁共用 Header 的容器寬度。 */
.profile-page {
  min-height: 520px;
  background: var(--color-bg);
}

.profile-page-inner {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
  padding-top: 40px;
  padding-bottom: 40px;
}

/* 頁面標題與編輯按鈕。 */
.profile-page-header {
  display: flex;
  min-height: 68px;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.profile-page-header h1 {
  margin: 0 0 var(--space-1);
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.profile-page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.profile-edit-button,
.profile-save-button,
.profile-cancel-button {
  min-height: 42px;
  padding: 0 var(--space-4);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border-radius: var(--radius-md);
}

.profile-edit-button,
.profile-save-button {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.profile-edit-button:hover:not(:disabled),
.profile-save-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.profile-cancel-button {
  color: var(--color-text-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
}

/* 成功載入後的左右欄位。 */
.profile-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
}

.profile-basic-card,
.profile-status-card,
.profile-retry {
  padding: 22px;
}

.profile-basic-card h2,
.profile-status-card h2 {
  margin: 0 0 14px;
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}

/* 表單欄位與唯讀狀態。 */
.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  color: var(--color-text);
  font-size: 15px;
  font-weight: 600;
}

.profile-field:first-child {
  grid-column: span 2;
}

.profile-field input {
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: 400;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.profile-field input:disabled,
.profile-field input[readonly] {
  color: var(--color-text-muted);
  cursor: default;
  background: var(--color-bg-muted);
}

.profile-field input:focus {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
  outline: none;
}

/* 編輯狀態的表單操作。 */
.profile-email-preferences {
  display: grid;
  gap: 10px;
  margin: 22px 0 0;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.profile-email-preferences:disabled {
  background: var(--color-bg-muted);
}

.profile-email-preferences legend {
  width: auto;
  float: none;
  margin: 0;
  padding: 0 4px;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.profile-email-preferences__hint {
  margin: 0 0 2px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.profile-checkbox-field {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  color: var(--color-text);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.profile-checkbox-field:hover {
  background: var(--color-bg-muted);
}

.profile-email-preferences:disabled .profile-checkbox-field {
  cursor: default;
}

.profile-checkbox-field input {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
  margin: 2px 0 0;
  accent-color: var(--color-primary);
}

.profile-checkbox-field span {
  display: grid;
  gap: 2px;
}

.profile-checkbox-field strong {
  font-size: var(--font-size-sm);
}

.profile-checkbox-field small {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.profile-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.profile-form-actions-placeholder {
  visibility: hidden;
}

.profile-form-actions button:focus-visible,
.profile-edit-button:focus-visible,
.profile-retry button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.profile-edit-button:disabled,
.profile-form-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

/* 會員帳戶狀態。 */
.profile-status-badge {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 0 var(--space-3);
  color: var(--color-success);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-success-soft);
  border-radius: var(--radius-sm);
}

.profile-status-badge span {
  width: 7px;
  height: 7px;
  background: currentColor;
  border-radius: var(--radius-pill);
}

.profile-member-meta {
  margin: 14px 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.7;
}

/* 成功、錯誤與載入狀態。 */
.profile-alert {
  margin: 0 0 14px;
  padding: var(--space-3) var(--space-4);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

.profile-alert-error {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.profile-alert-success {
  color: var(--color-success);
  background: var(--color-success-soft);
}

.profile-loading,
.profile-retry {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 991.98px) {
  .profile-content {
    grid-template-columns: 1fr;
  }

  .profile-status-card {
    width: 100%;
  }
}

@media (max-width: 767.98px) {
  .profile-page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-edit-button {
    align-self: flex-start;
  }

  .profile-form-grid {
    grid-template-columns: 1fr;
  }

  .profile-field:first-child {
    grid-column: 1;
  }
}

.profile-field :deep(.profile-birth-date-picker__input) {
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: 400;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  outline: none;
}

.profile-field :deep(.profile-birth-date-picker__input:hover:not(:disabled)) {
  border-color: var(--color-primary);
}

.profile-field :deep(.profile-birth-date-picker__input:focus) {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.profile-field :deep(.profile-birth-date-picker__input:disabled) {
  color: var(--color-text-muted);
  cursor: default;
  background: var(--color-bg-muted);
}

:global(.flatpickr-calendar.profile-birth-date-calendar) {
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

:global(.flatpickr-calendar.profile-birth-date-calendar::before),
:global(.flatpickr-calendar.profile-birth-date-calendar::after) {
  display: none;
}

:global(.profile-birth-date-calendar .flatpickr-months),
:global(.profile-birth-date-calendar .flatpickr-weekdays),
:global(.profile-birth-date-calendar .flatpickr-current-month),
:global(.profile-birth-date-calendar .flatpickr-monthDropdown-months),
:global(.profile-birth-date-calendar .numInputWrapper span),
:global(.profile-birth-date-calendar .flatpickr-prev-month),
:global(.profile-birth-date-calendar .flatpickr-next-month) {
  color: var(--color-text);
}

:global(.profile-birth-date-calendar .flatpickr-monthDropdown-months),
:global(.profile-birth-date-calendar .numInput) {
  background: var(--color-surface);
}

:global(.profile-birth-date-calendar .flatpickr-day:hover),
:global(.profile-birth-date-calendar .flatpickr-day:focus),
:global(.profile-birth-date-calendar .flatpickr-prev-month:hover),
:global(.profile-birth-date-calendar .flatpickr-next-month:hover) {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  border-color: var(--color-primary-soft);
}

:global(.profile-birth-date-calendar .flatpickr-day.today) {
  border-color: var(--color-primary-active);
}

:global(.profile-birth-date-calendar .flatpickr-day.selected),
:global(.profile-birth-date-calendar .flatpickr-day.selected:hover),
:global(.profile-birth-date-calendar .flatpickr-day.selected:focus) {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

@media (max-width: 575.98px) {
  .profile-page-inner {
    padding-top: var(--space-6);
    padding-bottom: var(--space-6);
  }
}
</style>
