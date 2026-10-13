<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { taiwanAddressOptions } from '@/data/taiwanAddressData'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  address: {
    type: Object,
    default: null,
  },
  isSaving: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'submit'])

// 表單狀態同時支援新增與編輯地址。
const dialogRef = ref(null)
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  postalCode: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false,
})
const validationMessage = ref('')

const cities = taiwanAddressOptions.map((item) => item.city)
const availableDistricts = computed(
  () => taiwanAddressOptions.find((item) => item.city === form.city)?.districts ?? [],
)

const isEditing = computed(() => Boolean(props.address?.addressId))
const dialogTitle = computed(() => (isEditing.value ? '編輯地址' : '新增地址'))

// 每次開啟 modal 時，將目前地址資料帶入表單。
function resetForm() {
  form.receiverName = props.address?.receiverName ?? ''
  form.receiverPhone = props.address?.receiverPhone ?? ''
  form.postalCode = props.address?.postalCode ?? ''
  form.city = (props.address?.city ?? '').replace('臺', '台')
  form.district = props.address?.district ?? ''
  form.detailAddress = props.address?.detailAddress ?? ''
  form.isDefault = Boolean(props.address?.isDefault)
  validationMessage.value = ''
}

function handleCityChange() {
  form.district = ''
}

function normalizePostalCode() {
  form.postalCode = form.postalCode.replace(/\D/g, '').slice(0, 6)
  if (form.postalCode.length < 3) return

  const matchedDistricts = taiwanAddressOptions.flatMap((item) =>
    item.districts
      .filter((district) => district.postalCode === form.postalCode.slice(0, 3))
      .map((district) => ({ city: item.city, ...district })),
  )

  const matchedCities = [...new Set(matchedDistricts.map((item) => item.city))]
  if (matchedCities.length !== 1) return

  form.city = matchedCities[0]
  form.district = matchedDistricts.length === 1 ? matchedDistricts[0].district : ''
}

watch(
  [() => props.open, () => props.address],
  async ([open]) => {
    if (!open) return
    resetForm()
    await nextTick()
    dialogRef.value?.focus()
  },
  { immediate: true },
)

// 儲存前先檢查畫面上的必填欄位，再交由父層呼叫 API。
function submitForm() {
  const requiredValues = [
    form.receiverName,
    form.receiverPhone,
    form.city,
    form.district,
    form.detailAddress,
  ]

  if (requiredValues.some((value) => !value.trim())) {
    validationMessage.value = '請填寫所有必填欄位。'
    return
  }

  validationMessage.value = ''
  emit('submit', {
    receiverName: form.receiverName.trim(),
    receiverPhone: form.receiverPhone.trim(),
    postalCode: form.postalCode.trim() || null,
    city: form.city.trim(),
    district: form.district.trim(),
    detailAddress: form.detailAddress.trim(),
    isDefault: Boolean(form.isDefault),
  })
}

// 儲存中不允許關閉，避免使用者重複操作。
function closeDialog() {
  if (!props.isSaving) emit('close')
}
</script>

<template>
  <Teleport to="body">
    <!-- 使用 backdrop 將表單覆蓋在會員頁面上。 -->
    <div
      v-if="open"
      class="address-form-backdrop"
      role="presentation"
      @click.self="closeDialog"
      @keydown.esc="closeDialog"
    >
      <section
        ref="dialogRef"
        class="address-form-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="address-form-title"
        tabindex="-1"
      >
        <!-- 表單標題與關閉按鈕。 -->
        <header class="address-form-header">
          <div>
            <p class="address-form-eyebrow">DELIVERY ADDRESS</p>
            <h2 id="address-form-title">{{ dialogTitle }}</h2>
          </div>
          <button
            class="address-form-close"
            type="button"
            aria-label="關閉地址表單"
            :disabled="isSaving"
            @click="closeDialog"
          >
            <i class="bi bi-x-lg" aria-hidden="true"></i>
          </button>
        </header>

        <!-- 收件資料與預設地址設定。 -->
        <form class="address-form-body" @submit.prevent="submitForm">
          <div v-if="validationMessage || errorMessage" class="address-form-alert" role="alert">
            {{ validationMessage || errorMessage }}
          </div>

          <div class="address-form-grid">
            <label class="address-form-field">
              <span>收件人姓名 <b aria-hidden="true">*</b></span>
              <input v-model.trim="form.receiverName" type="text" maxlength="100" required />
            </label>

            <label class="address-form-field">
              <span>收件人電話 <b aria-hidden="true">*</b></span>
              <input v-model.trim="form.receiverPhone" type="tel" maxlength="20" required />
            </label>

            <label class="address-form-field address-form-field-small">
              <span>郵遞區號</span>
              <input
                v-model="form.postalCode"
                inputmode="numeric"
                type="text"
                maxlength="6"
                @input="normalizePostalCode"
              />
            </label>

            <label class="address-form-field">
              <span>縣市 <b aria-hidden="true">*</b></span>
              <select v-model="form.city" required @change="handleCityChange">
                <option value="">請選擇縣市</option>
                <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
              </select>
            </label>

            <label class="address-form-field">
              <span>行政區 <b aria-hidden="true">*</b></span>
              <select v-model="form.district" required :disabled="!form.city">
                <option value="">{{ form.city ? '請選擇行政區' : '請先選擇縣市' }}</option>
                <option
                  v-for="item in availableDistricts"
                  :key="item.district"
                  :value="item.district"
                >
                  {{ item.district }}
                </option>
              </select>
            </label>

            <label class="address-form-field address-form-field-wide">
              <span>詳細地址 <b aria-hidden="true">*</b></span>
              <input v-model.trim="form.detailAddress" type="text" maxlength="255" required />
            </label>
          </div>

          <label class="address-form-default">
            <input v-model="form.isDefault" type="checkbox" />
            <span>設為預設地址</span>
          </label>

          <footer class="address-form-actions">
            <button
              class="address-form-cancel"
              type="button"
              :disabled="isSaving"
              @click="closeDialog"
            >
              取消
            </button>
            <button class="address-form-submit" type="submit" :disabled="isSaving">
              {{ isSaving ? '儲存中…' : '儲存地址' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
/* Modal 背景與定位。 */
.address-form-backdrop {
  position: fixed;
  z-index: 1080;
  inset: 0;
  display: grid;
  overflow-y: auto;
  padding: var(--space-5);
  background: rgb(15 20 29 / 46%);
  place-items: center;
}

.address-form-dialog {
  width: min(680px, 100%);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 54px rgb(26 31 46 / 18%);
}

.address-form-dialog:focus {
  outline: none;
}

/* Modal 標題區。 */
.address-form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.address-form-eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.address-form-header h2 {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-xl);
  font-weight: 700;
}

.address-form-close {
  display: inline-grid;
  width: 36px;
  height: 36px;
  color: var(--color-text-muted);
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
  place-items: center;
}

.address-form-close:hover,
.address-form-close:focus-visible {
  color: var(--color-primary-active);
  background: var(--color-primary-soft);
  outline: none;
}

/* 地址欄位排列。 */
.address-form-body {
  padding: var(--space-6);
}

.address-form-alert {
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-base);
  background: var(--color-danger-soft);
  border-radius: var(--radius-md);
}

.address-form-grid {
  display: grid;
  grid-template-columns: 0.7fr 1fr 1fr;
  gap: var(--space-4);
}

.address-form-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.address-form-field:first-child,
.address-form-field:nth-child(2) {
  grid-column: span 1;
}

.address-form-field:first-child {
  grid-column: 1 / span 2;
}

.address-form-field:nth-child(2) {
  grid-column: 3;
}

.address-form-field b {
  color: var(--color-danger);
}

.address-form-field input,
.address-form-field select {
  width: 100%;
  height: 48px;
  padding: 0 var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}

.address-form-field input:focus,
.address-form-field select:focus {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
  outline: none;
}

.address-form-field select:disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
  background: var(--color-bg-muted);
}

.address-form-field-wide {
  grid-column: 1 / -1;
}

.address-form-default {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-5);
  color: var(--color-text-700);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.address-form-default input {
  width: 17px;
  height: 17px;
  accent-color: var(--color-primary);
}

/* 取消與儲存操作。 */
.address-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

.address-form-actions button {
  min-width: 104px;
  height: 48px;
  padding: 0 var(--space-4);
  font-size: var(--font-size-base);
  font-weight: 600;
  border-radius: var(--radius-md);
}

.address-form-cancel {
  color: var(--color-text-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
}

.address-form-submit {
  color: var(--color-surface);
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.address-form-submit:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.address-form-actions button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.address-form-actions button:disabled,
.address-form-close:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

@media (max-width: 767.98px) {
  .address-form-backdrop {
    align-items: start;
    padding: var(--space-3);
  }

  .address-form-header,
  .address-form-body {
    padding: var(--space-5);
  }

  .address-form-grid {
    grid-template-columns: 1fr;
  }

  .address-form-field,
  .address-form-field:first-child,
  .address-form-field:nth-child(2),
  .address-form-field-wide {
    grid-column: 1;
  }
}
</style>
