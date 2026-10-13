<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { isPasswordWithinUtf8ByteLimit } from '@/utils/password'

const route = useRoute()
const router = useRouter()
const form = ref({ newPassword: '', confirmNewPassword: '' })
const errorMessage = ref('')
const isSubmitting = ref(false)
const isSubmitted = ref(false)

const hasValidToken = computed(
  () => typeof route.query.token === 'string' && route.query.token.trim().length > 0,
)

const passwordsMatch = computed(
  () => form.value.newPassword === form.value.confirmNewPassword,
)

async function submit() {
  errorMessage.value = ''
  if (!hasValidToken.value) {
    errorMessage.value = '重設連結無效或已過期，請重新申請。'
    return
  }
  if (form.value.newPassword.length < 8 || !isPasswordWithinUtf8ByteLimit(form.value.newPassword)) {
    errorMessage.value = '新密碼須至少 8 個字元，且不可超過 72 個 UTF-8 位元組。'
    return
  }
  if (!/[A-Za-z]/.test(form.value.newPassword) || !/\d/.test(form.value.newPassword)) {
    errorMessage.value = '新密碼須包含英文與數字。'
    return
  }
  if (!passwordsMatch.value) {
    errorMessage.value = '新密碼與確認密碼不一致。'
    return
  }

  isSubmitting.value = true
  try {
    await resetPassword({ token: route.query.token, ...form.value })
    isSubmitted.value = true
    window.setTimeout(() => router.replace({ name: 'Login' }), 1500)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '重設密碼失敗，請重新申請連結。'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page container py-5">
    <section class="auth-card dg-card mx-auto p-4 p-md-5" aria-labelledby="reset-password-title">
      <p class="auth-eyebrow mb-2">DINO-GO MEMBER</p>
      <h1 id="reset-password-title" class="dg-heading mb-2">設定新密碼</h1>
      <p class="text-muted mb-4">請設定至少 8 個字元、包含英文與數字的新密碼。</p>

      <div v-if="isSubmitted" class="alert alert-success" role="status">
        密碼已更新，正在帶您返回登入頁。
      </div>
      <p v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</p>

      <form v-if="!isSubmitted" novalidate @submit.prevent="submit">
        <div class="mb-3">
          <label class="form-label" for="new-password">新密碼</label>
          <input
            id="new-password"
            v-model="form.newPassword"
            class="form-control"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="72"
            :disabled="isSubmitting"
            required
          />
        </div>
        <div class="mb-4">
          <label class="form-label" for="confirm-new-password">確認新密碼</label>
          <input
            id="confirm-new-password"
            v-model="form.confirmNewPassword"
            class="form-control"
            :class="{ 'is-invalid': form.confirmNewPassword && !passwordsMatch }"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="72"
            :disabled="isSubmitting"
            required
          />
          <div v-if="form.confirmNewPassword && !passwordsMatch" class="invalid-feedback">
            新密碼與確認密碼不一致。
          </div>
        </div>
        <button class="btn dg-btn-primary w-100" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '更新中…' : '更新密碼' }}
        </button>
      </form>

      <p class="mt-4 mb-0 text-center text-muted">
        <RouterLink to="/forgot-password" class="auth-link">重新申請重設連結</RouterLink>
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
</style>
