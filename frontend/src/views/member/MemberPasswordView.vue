<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/member'
import { useAuthStore } from '@/stores/auth'
import { isPasswordWithinUtf8ByteLimit } from '@/utils/password'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})
const isSaving = ref(false)
const errorMessage = ref('')
const submitted = ref(false)

const hasMinimumLength = computed(() => form.value.newPassword.length >= 8)
const isWithinMaximumLength = computed(
  () =>
    isPasswordWithinUtf8ByteLimit(form.value.newPassword) &&
    isPasswordWithinUtf8ByteLimit(form.value.confirmNewPassword),
)
const hasEnglishAndNumber = computed(
  () => /[A-Za-z]/.test(form.value.newPassword) && /\d/.test(form.value.newPassword),
)
const isDifferentFromCurrent = computed(
  () => Boolean(form.value.newPassword) && form.value.newPassword !== form.value.currentPassword,
)
const passwordsMatch = computed(
  () => form.value.newPassword === form.value.confirmNewPassword,
)
const confirmPasswordError = computed(
  () =>
    (submitted.value || Boolean(form.value.confirmNewPassword)) &&
    !passwordsMatch.value &&
    '兩次輸入的密碼不一致。',
)
const isFormValid = computed(
  () =>
    Boolean(form.value.currentPassword) &&
    hasMinimumLength.value &&
    isWithinMaximumLength.value &&
    hasEnglishAndNumber.value &&
    isDifferentFromCurrent.value &&
    passwordsMatch.value,
)

function getErrorMessage(error) {
  if (!error.response) return '目前無法連線，請稍後再試。'
  if (typeof error.response.data === 'string' && error.response.data.trim()) {
    return error.response.data
  }
  return error.response.data?.message || '修改密碼失敗，請確認輸入內容後再試。'
}

async function savePassword() {
  submitted.value = true
  errorMessage.value = ''
  if (!isFormValid.value) return

  isSaving.value = true
  try {
    await changePassword({ ...form.value })
    // 後端會使所有既有 JWT 失效，成功後立即清除目前工作階段並返回登入頁。
    authStore.signOut()
    await router.replace({ name: 'Login' })
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <main class="password-page" aria-labelledby="password-title">
    <div class="container password-page-inner">
      <header class="password-page-header">
        <div>
          <h1 id="password-title">修改密碼</h1>
          <p>定期更新密碼以維持帳戶安全</p>
        </div>
      </header>

      <div class="password-layout">
        <section class="dg-card password-form-card" aria-labelledby="password-form-title">
          <h2 id="password-form-title" class="visually-hidden">修改密碼表單</h2>
          <p v-if="errorMessage" class="password-alert" role="alert">{{ errorMessage }}</p>

          <form @submit.prevent="savePassword">
            <label class="password-field" for="current-password">
              <span>舊密碼</span>
              <input
                id="current-password"
                v-model="form.currentPassword"
                type="password"
                autocomplete="current-password"
                :disabled="isSaving"
                required
              />
            </label>

            <label class="password-field" for="new-password">
              <span>新密碼</span>
              <input
                id="new-password"
                v-model="form.newPassword"
                type="password"
                autocomplete="new-password"
                placeholder="至少 8 個字元"
                maxlength="72"
                :disabled="isSaving"
                required
              />
            </label>

            <label class="password-field" for="confirm-new-password">
              <span>確認新密碼</span>
              <input
                id="confirm-new-password"
                v-model="form.confirmNewPassword"
                type="password"
                autocomplete="new-password"
                maxlength="72"
                :class="{ 'is-invalid': confirmPasswordError }"
                :aria-invalid="Boolean(confirmPasswordError)"
                :aria-describedby="confirmPasswordError ? 'confirm-password-error' : undefined"
                :disabled="isSaving"
                required
              />
              <small v-if="confirmPasswordError" id="confirm-password-error" class="password-error">
                {{ confirmPasswordError }}
              </small>
            </label>

            <button class="password-save-button" type="submit" :disabled="!isFormValid || isSaving">
              {{ isSaving ? '儲存中…' : '儲存新密碼' }}
            </button>
          </form>
        </section>

        <aside class="dg-card password-rules-card" aria-labelledby="password-rules-title">
          <h2 id="password-rules-title">密碼規則</h2>
          <ul>
            <li :class="{ 'is-satisfied': hasMinimumLength }">
              <i class="bi" :class="hasMinimumLength ? 'bi-check-circle-fill' : 'bi-circle'" aria-hidden="true"></i>
              <span>至少 8 個字元</span>
            </li>
            <li :class="{ 'is-satisfied': hasEnglishAndNumber }">
              <i class="bi" :class="hasEnglishAndNumber ? 'bi-check-circle-fill' : 'bi-circle'" aria-hidden="true"></i>
              <span>包含英文與數字</span>
            </li>
            <li :class="{ 'is-satisfied': isWithinMaximumLength }">
              <i class="bi" :class="isWithinMaximumLength ? 'bi-check-circle-fill' : 'bi-circle'" aria-hidden="true"></i>
              <span>最多 72 個 UTF-8 位元組</span>
            </li>
            <li :class="{ 'is-satisfied': isDifferentFromCurrent }">
              <i class="bi" :class="isDifferentFromCurrent ? 'bi-check-circle-fill' : 'bi-circle'" aria-hidden="true"></i>
              <span>不可與舊密碼相同</span>
            </li>
          </ul>
        </aside>
      </div>
    </div>
  </main>
</template>

<style scoped>
.password-page {
  min-height: 520px;
  background: var(--color-bg);
}

.password-page-inner {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
  padding-top: 40px;
  padding-bottom: 40px;
}

.password-page-header {
  min-height: 68px;
  margin-bottom: var(--space-5);
}

.password-page-header h1 {
  margin: 0 0 var(--space-1);
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.password-page-header p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.password-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 20px;
  align-items: start;
}

.password-form-card {
  padding: 24px;
}

.password-form-card form {
  display: grid;
  gap: 14px;
}

.password-field {
  display: grid;
  gap: 6px;
  color: var(--color-text);
  font-size: 15px;
  font-weight: 600;
}

.password-field input {
  width: 100%;
  height: 42px;
  padding: 0 var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: 400;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.password-field input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}

.password-field input:focus {
  border-color: var(--color-primary);
  outline: none;
  box-shadow: var(--shadow-focus);
}

.password-field input.is-invalid {
  border-color: var(--color-danger);
}

.password-field input:disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}

.password-error {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
  font-weight: 400;
}

.password-save-button {
  width: 140px;
  height: 42px;
  margin-top: 2px;
  color: var(--color-surface);
  font-size: var(--font-size-sm);
  font-weight: 600;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
}

.password-save-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.password-save-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.password-save-button:disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
  background: var(--color-disabled-bg);
  border-color: var(--color-disabled);
}

.password-alert {
  margin: 0 0 14px;
  padding: var(--space-3) var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  background: var(--color-danger-soft);
  border-radius: var(--radius-md);
}

.password-rules-card {
  padding: 22px;
}

.password-rules-card h2 {
  margin: 0 0 var(--space-3);
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}

.password-rules-card ul {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-rules-card li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}

.password-rules-card li .bi {
  color: var(--color-text-subtle);
  font-size: 15px;
}

.password-rules-card li.is-satisfied .bi {
  color: var(--color-success);
}

@media (max-width: 991.98px) {
  .password-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767.98px) {
  .password-form-card,
  .password-rules-card {
    padding: var(--space-5);
  }

  .password-save-button {
    width: 100%;
  }
}

@media (max-width: 575.98px) {
  .password-page-inner {
    padding-top: var(--space-6);
    padding-bottom: var(--space-6);
  }
}
</style>
