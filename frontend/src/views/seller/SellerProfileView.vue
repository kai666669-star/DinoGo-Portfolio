<script setup>
import {
  uploadSellerLogo,
  resolveSellerLogoUrl,
} from '@/api/sellerProfileApi'
import { useSellerProfileStore } from '@/stores/sellerProfile'
import { computed, onMounted, reactive, ref } from 'vue'

const sellerProfileStore = useSellerProfileStore()
const isSaving = ref(false)
const savedMessage = ref('')
const logoUrl = ref('')
const logoLoadFailed = ref(false)

const logoFileInput = ref(null)
const isUploadingLogo = ref(false)

const openLogoPicker = () => {
  logoFileInput.value?.click()
}

const handleLogoSelect = async (event) => {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  isUploadingLogo.value = true
  savedMessage.value = ''
  logoLoadFailed.value = false

  try {
    const response = await uploadSellerLogo(file)
    sellerProfileStore.setProfile(response.data)
    applyProfileToForm(response.data)
    savedMessage.value = '店鋪 Logo 已更新。'
  } catch (error) {
    console.error('Upload seller logo failed:', error)
    savedMessage.value =
      error.code === 'ECONNABORTED'
        ? '店鋪 Logo 上傳逾時，請確認網路連線後再試一次。'
        : '店鋪 Logo 上傳失敗，請確認檔案格式與大小。'
  } finally {
    isUploadingLogo.value = false
    event.target.value = ''
  }
}

const form = reactive({
  storeName: '',
  status: 'ACTIVE',
  description: '',
})

const statusOptions = [
  { label: '營運中', value: 'ACTIVE' },
  { label: '暫停接單', value: 'PAUSED' },
  { label: '審核中', value: 'REVIEWING' },
]

const statusText = {
  ACTIVE: '營運中',
  PAUSED: '暫停接單',
  REVIEWING: '審核中',
}

const timeUnitOptions = {
  hours: Array.from({ length: 24 }, (_, index) => String(index).padStart(2, '0')),
  minutes: Array.from({ length: 60 }, (_, index) => String(index).padStart(2, '0')),
}

const serviceTime = reactive({
  startHour: '09',
  startMinute: '00',
  endHour: '18',
  endMinute: '00',
})

const normalizeTimeParts = (value, fallback) => {
  const [fallbackHour, fallbackMinute] = fallback.split(':')
  const [hour = fallbackHour, minute = fallbackMinute] = String(value || '').split(':')

  return {
    hour: String(hour).padStart(2, '0'),
    minute: String(minute).padStart(2, '0'),
  }
}

const formatServiceTime = (prefix) =>
  `${serviceTime[`${prefix}Hour`]}:${serviceTime[`${prefix}Minute`]}`

const formatServiceTimeForApi = (prefix) => `${formatServiceTime(prefix)}:00`

const selectedServiceTimeLabel = computed(
  () => `${formatServiceTime('start')} - ${formatServiceTime('end')}`,
)

const activeTimePicker = ref('')
const timePickerDraft = reactive({
  hour: '09',
  minute: '00',
})

const timePickerTitle = computed(() =>
  activeTimePicker.value === 'end' ? '選擇結束時間' : '選擇開始時間',
)

const openTimePicker = (prefix) => {
  activeTimePicker.value = prefix
  timePickerDraft.hour = serviceTime[`${prefix}Hour`]
  timePickerDraft.minute = serviceTime[`${prefix}Minute`]
}

const closeTimePicker = () => {
  activeTimePicker.value = ''
}

const confirmTimePicker = () => {
  if (!activeTimePicker.value) return

  serviceTime[`${activeTimePicker.value}Hour`] = timePickerDraft.hour
  serviceTime[`${activeTimePicker.value}Minute`] = timePickerDraft.minute
  closeTimePicker()
}

const applyProfileToForm = (profile) => {
  form.storeName = profile.storeName ?? ''
  form.description = profile.storeDescription ?? ''
  form.status = profile.status ?? 'ACTIVE'

  if (profile.serviceStartTime && profile.serviceEndTime) {
    const startTime = normalizeTimeParts(profile.serviceStartTime, '09:00:00')
    const endTime = normalizeTimeParts(profile.serviceEndTime, '18:00:00')
    serviceTime.startHour = startTime.hour
    serviceTime.startMinute = startTime.minute
    serviceTime.endHour = endTime.hour
    serviceTime.endMinute = endTime.minute
  }

  logoUrl.value = profile.storeLogoUrl ?? ''
  logoLoadFailed.value = false
}

const handleSave = async () => {
  isSaving.value = true
  savedMessage.value = ''
  const serviceStartTime = formatServiceTimeForApi('start')
  const serviceEndTime = formatServiceTimeForApi('end')

  try {
    const profile = await sellerProfileStore.saveProfile({
      storeName: form.storeName,
      storeDescription: form.description,
      storeLogoUrl: logoUrl.value.trim() || null,
      status: form.status,
      serviceStartTime,
      serviceEndTime,
    })

    applyProfileToForm(profile)
    savedMessage.value = '店鋪資料已更新。'
  } catch (error) {
    console.error('Update seller profile failed:', error)
    savedMessage.value = '店鋪資料儲存失敗，請稍後再試。'
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  try {
    const profile = await sellerProfileStore.fetchProfile({ force: true })
    applyProfileToForm(profile)
  } catch (error) {
    console.error('Load seller profile failed:', error)
    savedMessage.value = '店鋪資料載入失敗，請確認是否已登入賣家帳號。'
  }
})
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">店鋪資料</p>
        <h1>店鋪資料</h1>
        <p class="page-description">管理店鋪公開資訊與營業設定。</p>
      </div>
    </header>

    <section class="status-panel" :class="`status-${form.status.toLowerCase()}`">
      <div class="status-copy">
        <span class="status-dot" aria-hidden="true"></span>
        <div>
          <p class="section-label">店鋪目前狀態</p>
          <strong>{{ statusText[form.status] }}</strong>
          <span>{{
            form.status === 'ACTIVE' ? '顧客可以瀏覽並購買你的商品。' : '目前不會接收新的訂單。'
          }}</span>
        </div>
      </div>
    </section>

    <form class="profile-grid" @submit.prevent="handleSave">
      <section class="preview-card store-banner-preview">
        <div class="preview-brand">
          <img
            v-if="resolveSellerLogoUrl(logoUrl) && !logoLoadFailed"
            class="store-avatar-image"
            :src="resolveSellerLogoUrl(logoUrl)"
            :alt="`${form.storeName} Logo`"
            @error="logoLoadFailed = true"
          />
          <span v-else class="store-avatar">{{ form.storeName.slice(0, 1) }}</span>
          <div>
            <p class="section-kicker">前台店鋪橫幅預覽</p>
            <strong>{{ form.storeName }}</strong>
            <span>{{ form.description }}</span>
          </div>
        </div>
        <div class="preview-meta">
          <span>{{ statusText[form.status] }}</span>
          <span>{{ selectedServiceTimeLabel }}</span>
        </div>
      </section>

      <section class="profile-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">公開資料</p>
            <h2>店鋪公開資訊</h2>
          </div>
          <span class="visibility-note">顧客可見</span>
        </div>

        <label class="form-field full-width">
          店鋪名稱
          <input v-model="form.storeName" type="text" />
        </label>

        <section class="logo-section full-width" aria-labelledby="store-logo-title">
          <h3 id="store-logo-title">店鋪 Logo</h3>

          <button
            class="logo-upload-button"
            type="button"
            :disabled="isUploadingLogo"
            @click="openLogoPicker"
          >
            <img
              v-if="resolveSellerLogoUrl(logoUrl) && !logoLoadFailed"
              class="logo-preview"
              :src="resolveSellerLogoUrl(logoUrl)"
              :alt="`${form.storeName} Logo 預覽`"
              @error="logoLoadFailed = true"
            />

            <div v-else class="logo-placeholder">
              <i class="bi bi-image" aria-hidden="true"></i>
              <span>{{ isUploadingLogo ? '上傳中...' : '點選上傳' }}</span>
              <small>建議使用正方形圖片</small>
            </div>
          </button>

          <input
            ref="logoFileInput"
            class="logo-file-input"
            type="file"
            accept="image/*"
            @change="handleLogoSelect"
          />

          <p class="logo-help">上傳後會自動同步到前台店鋪頁。</p>
        </section>

        <label class="form-field full-width">
          店鋪介紹
          <textarea v-model="form.description"></textarea>
        </label>
      </section>

      <aside class="profile-side">
        <section class="settings-card">
          <div class="card-heading compact-heading">
            <div>
              <p class="section-kicker">營運設定</p>
              <h2>營業狀態</h2>
            </div>
          </div>

          <label class="form-field">
            店鋪狀態
            <select v-model="form.status">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <fieldset class="form-field time-field">
            <legend>客服時間</legend>

            <div class="time-inputs">
              <button
                type="button"
                class="time-input-button"
                :class="{ 'is-active': activeTimePicker === 'start' }"
                @click="openTimePicker('start')"
              >
                <i class="bi bi-clock" aria-hidden="true"></i>
                <span>{{ formatServiceTime('start') }}</span>
              </button>

              <span class="time-separator">-</span>

              <button
                type="button"
                class="time-input-button"
                :class="{ 'is-active': activeTimePicker === 'end' }"
                @click="openTimePicker('end')"
              >
                <i class="bi bi-clock" aria-hidden="true"></i>
                <span>{{ formatServiceTime('end') }}</span>
              </button>
            </div>

            <div v-if="activeTimePicker" class="time-picker-panel">
              <div class="time-picker-header">
                <strong>{{ timePickerTitle }}</strong>
                <button type="button" aria-label="關閉時間選單" @click="closeTimePicker">
                  <i class="bi bi-x-lg" aria-hidden="true"></i>
                </button>
              </div>

              <div class="time-picker-columns">
                <div class="time-picker-column" aria-label="小時">
                  <button
                    v-for="hour in timeUnitOptions.hours"
                    :key="`picker-hour-${hour}`"
                    type="button"
                    :class="{ 'is-selected': timePickerDraft.hour === hour }"
                    @click="timePickerDraft.hour = hour"
                  >
                    {{ hour }}
                  </button>
                </div>

                <span class="time-colon">:</span>

                <div class="time-picker-column" aria-label="分鐘">
                  <button
                    v-for="minute in timeUnitOptions.minutes"
                    :key="`picker-minute-${minute}`"
                    type="button"
                    :class="{ 'is-selected': timePickerDraft.minute === minute }"
                    @click="timePickerDraft.minute = minute"
                  >
                    {{ minute }}
                  </button>
                </div>

              </div>

              <div class="time-picker-actions">
                <button type="button" class="secondary-button" @click="closeTimePicker">取消</button>
                <button type="button" class="primary-button" @click="confirmTimePicker">確認</button>
              </div>
            </div>
          </fieldset>
        </section>
      </aside>

      <p v-if="savedMessage" class="saved-message">{{ savedMessage }}</p>

      <div class="form-actions">
        <button class="secondary-button" type="button">取消</button>
        <button class="primary-button" type="submit" :disabled="isSaving">
          {{ isSaving ? '儲存中...' : '儲存變更' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
  max-width: 1160px;
}

.page-header {
  display: flex;
  align-items: flex-start;
}

.eyebrow,
.page-description,
.section-label,
.section-kicker,
.preview-meta,
.preview-brand span,
.status-panel span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow,
.section-kicker,
.section-label {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin: var(--space-1) 0 0;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

h2 {
  margin-bottom: 0;
  color: var(--color-text-800);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}

h3 {
  margin: 0;
  color: var(--color-text-800);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
}

.status-panel,
.profile-card,
.preview-card,
.settings-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.status-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-left: 4px solid var(--color-success);
}

.status-paused {
  border-left-color: var(--color-warning);
}

.status-reviewing {
  border-left-color: var(--color-text-muted);
}

.status-copy {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.status-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--color-success);
}

.status-paused .status-dot {
  background: var(--color-warning);
}

.status-reviewing .status-dot {
  background: var(--color-text-muted);
}

.status-copy div {
  display: grid;
  gap: 2px;
}

.status-copy strong {
  color: var(--color-text-900);
  font-size: var(--font-size-lg);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: var(--space-5);
}

.profile-card,
.preview-card,
.settings-card {
  padding: var(--space-5);
}

.profile-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  grid-column: 1 / -1;
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.compact-heading {
  padding-bottom: var(--space-3);
}

.visibility-note {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.profile-side {
  display: grid;
  align-content: start;
  gap: var(--space-4);
}

.preview-card {
  border-left: 4px solid var(--color-primary);
}

.store-banner-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.preview-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.store-avatar {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.store-avatar-image {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  object-fit: cover;
}

.preview-brand div {
  display: grid;
  gap: 2px;
}

.preview-brand strong {
  color: var(--color-text-900);
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.preview-meta span {
  border-radius: var(--radius-pill);
  padding: 4px 10px;
  background: var(--color-bg-muted);
}

.settings-card {
  display: grid;
  gap: var(--space-4);
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 700;
}

.time-field {
  min-width: 0;
  margin: 0;
  border: 0;
  padding: 0;
}

.time-field legend {
  margin-bottom: var(--space-2);
  padding: 0;
}

.time-inputs {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: var(--space-2);
}

.time-input-button {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-weight: 400;
  text-align: left;
  cursor: pointer;
}

.time-input-button i {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.time-input-button:hover,
.time-input-button.is-active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-soft);
}

.time-separator {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 400;
}

.time-picker-panel {
  position: relative;
  z-index: 5;
  display: grid;
  gap: var(--space-3);
  width: min(100%, 280px);
  margin-top: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-lg);
}

.time-picker-header,
.time-picker-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
}

.time-picker-header {
  border-bottom: 1px solid var(--color-border);
}

.time-picker-header strong {
  color: var(--color-text-800);
  font-size: var(--font-size-sm);
}

.time-picker-header button {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
}

.time-picker-header button:hover {
  background: var(--color-bg-muted);
  color: var(--color-text-800);
}

.time-picker-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  padding: 0 var(--space-4);
}

.time-picker-column {
  display: grid;
  gap: 2px;
  max-height: 188px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: var(--space-2) 0;
}

.time-picker-column button {
  min-height: 32px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
  font-weight: 500;
  cursor: pointer;
}

.time-picker-column button:hover {
  background: var(--color-bg-muted);
  color: var(--color-text-900);
}

.time-picker-column button.is-selected {
  background: var(--color-danger);
  color: var(--color-surface);
  font-weight: 800;
}

.time-colon {
  width: 20px;
  color: var(--color-danger);
  text-align: center;
}

.time-picker-actions {
  justify-content: flex-end;
  border-top: 1px solid var(--color-border);
}

.logo-section {
  display: grid;
  gap: var(--space-3);
}

.logo-upload-button {
  display: grid;
  width: min(100%, 360px);
  min-height: 220px;
  overflow: hidden;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  cursor: pointer;
  font: inherit;
  text-align: center;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.logo-upload-button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.logo-upload-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.logo-preview {
  width: 100%;
  height: 220px;
  border-radius: calc(var(--radius-md) - 2px);
  object-fit: cover;
}

.logo-placeholder {
  display: grid;
  place-items: center;
  align-content: center;
  gap: var(--space-2);
  min-height: 188px;
}
.logo-placeholder i {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 24px;
}

.logo-placeholder span {
  color: var(--color-text-900);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.logo-placeholder small {
  max-width: 240px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 600;
  line-height: 1.5;
}

.logo-file-input {
  display: none;
}

.full-width {
  grid-column: 1 / -1;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
  color: var(--color-text-800);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.section-divider small {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.section-divider::after {
  height: 1px;
  flex: 1;
  background: var(--color-border);
  content: '';
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-weight: 400;
}

input:focus,
select:focus,
textarea:focus {
  outline: 2px solid var(--color-primary-soft);
  border-color: var(--color-primary);
}

textarea {
  min-height: 112px;
  padding: var(--space-3);
  resize: vertical;
}

.settings-card textarea {
  min-height: 88px;
}

.secondary-button,
.primary-button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.secondary-button {
  background: var(--color-surface);
  color: var(--color-text-700);
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.saved-message {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--color-success);
  font-weight: 700;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  grid-column: 1 / -1;
  gap: var(--space-3);
}

@media (max-width: 1000px) {
  .store-banner-preview {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .status-panel,
  .profile-side {
    grid-template-columns: 1fr;
  }

  .status-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-card {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions button,
  .secondary-button {
    width: 100%;
  }
}
</style>
