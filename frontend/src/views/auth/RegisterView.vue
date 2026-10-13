<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { isPasswordWithinUtf8ByteLimit } from '@/utils/password'

const router = useRouter()
const form = ref({
  lastName: '',
  firstName: '',
  email: '',
  password: '',
  confirmPassword: '',
})
const fieldErrors = ref({})
const apiError = ref('')
const isSubmitting = ref(false)

function validate() {
  const errors = {}
  if (!form.value.lastName.trim()) errors.lastName = '請輸入姓氏。'
  if (!form.value.firstName.trim()) errors.firstName = '請輸入名字。'
  if (!form.value.email.trim()) errors.email = '請輸入 Email。'
  else if (!/^\S+@\S+\.\S+$/.test(form.value.email)) errors.email = '請輸入有效的 Email。'
  if (!form.value.password) errors.password = '請輸入密碼。'
  else if (form.value.password.length < 8) errors.password = '密碼至少需要 8 個字元。'
  else if (!isPasswordWithinUtf8ByteLimit(form.value.password)) errors.password = '密碼不可超過 72 個 UTF-8 位元組。'
  else if (!/[A-Za-z]/.test(form.value.password) || !/\d/.test(form.value.password)) {
    errors.password = '密碼須包含英文與數字。'
  }
  if (!form.value.confirmPassword) errors.confirmPassword = '請再次輸入密碼。'
  else if (form.value.password !== form.value.confirmPassword) {
    errors.confirmPassword = '兩次輸入的密碼不一致。'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function getErrorMessage(error) {
  if (!error.response) {
    return '無法連線到伺服器，請確認後端是否已啟動。'
  }

  const { data, status } = error.response
  if (status === 409) return '此 Email 已經註冊。'
  if (typeof data === 'string' && data.trim()) return data
  if (status === 400) return data?.message || '請確認註冊資料是否正確。'
  return data?.message || '註冊失敗，請稍後再試。'
}

async function submit() {
  apiError.value = ''
  if (!validate()) return

  isSubmitting.value = true
  try {
    await register(form.value)
    await router.push({ name: 'Login', query: { registered: '1' } })
  } catch (error) {
    apiError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page container py-5">
    <section class="auth-card dg-card mx-auto p-4 p-md-5" aria-labelledby="register-title">
      <p class="auth-eyebrow mb-2">DINO-GO MEMBER</p>
      <h1 id="register-title" class="dg-heading mb-2">註冊頁</h1>
      <p class="text-muted mb-4">建立帳號，開始使用平台保障交易。</p>

      <div v-if="apiError" class="alert alert-danger" role="alert">{{ apiError }}</div>

      <form novalidate @submit.prevent="submit">
        <div class="row g-3 mb-3">
          <div class="col-6">
            <label class="form-label" for="register-last-name">姓氏</label>
            <input
              id="register-last-name"
              v-model="form.lastName"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.lastName }"
              autocomplete="family-name"
              placeholder="請輸入姓氏"
              @input="delete fieldErrors.lastName"
            />
            <div v-if="fieldErrors.lastName" class="invalid-feedback">
              {{ fieldErrors.lastName }}
            </div>
          </div>
          <div class="col-6">
            <label class="form-label" for="register-first-name">名字</label>
            <input
              id="register-first-name"
              v-model="form.firstName"
              class="form-control"
              :class="{ 'is-invalid': fieldErrors.firstName }"
              autocomplete="given-name"
              placeholder="請輸入名字"
              @input="delete fieldErrors.firstName"
            />
            <div v-if="fieldErrors.firstName" class="invalid-feedback">
              {{ fieldErrors.firstName }}
            </div>
          </div>
        </div>

        <div class="mb-3">
          <label class="form-label" for="register-email">電子信箱</label>
          <input
            id="register-email"
            v-model="form.email"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.email }"
            type="email"
            autocomplete="email"
            placeholder="name@example.com"
            @input="delete fieldErrors.email"
          />
          <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
        </div>

        <div class="mb-3">
          <label class="form-label" for="register-password">密碼</label>
          <input
            id="register-password"
            v-model="form.password"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.password }"
            type="password"
            autocomplete="new-password"
            placeholder="至少 8 個字元，包含英文與數字"
            @input="delete fieldErrors.password"
          />
          <div v-if="fieldErrors.password" class="invalid-feedback">{{ fieldErrors.password }}</div>
        </div>

        <div class="mb-4">
          <label class="form-label" for="register-confirm-password">確認密碼</label>
          <input
            id="register-confirm-password"
            v-model="form.confirmPassword"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.confirmPassword }"
            type="password"
            autocomplete="new-password"
            placeholder="請再次輸入"
            @input="delete fieldErrors.confirmPassword"
          />
          <div v-if="fieldErrors.confirmPassword" class="invalid-feedback">
            {{ fieldErrors.confirmPassword }}
          </div>
        </div>

        <button class="btn dg-btn-primary w-100" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '建立中…' : '建立帳號' }}
        </button>
      </form>

      <p class="mt-4 mb-0 text-muted">
        已有帳號？
        <RouterLink to="/login" class="auth-link">返回登入</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 640px;
  display: grid;
  place-items: center;
}
.auth-card {
  width: min(100%, 440px);
  box-shadow: var(--shadow-card);
}
.auth-eyebrow {
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.04em;
}
.auth-link {
  color: var(--color-primary-active);
  font-weight: 600;
  text-decoration: none;
}
.auth-link:hover {
  text-decoration: underline;
}
</style>
