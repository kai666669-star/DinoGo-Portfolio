<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { requestPasswordReset } from '@/api/auth'

const email = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const isSubmitted = ref(false)

async function submit() {
  errorMessage.value = ''
  if (!email.value.trim()) {
    errorMessage.value = '請輸入 Email。'
    return
  }

  isSubmitting.value = true
  try {
    await requestPasswordReset({ email: email.value })
    isSubmitted.value = true
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '目前無法寄送重設信，請稍後再試。'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page container py-5">
    <section class="auth-card dg-card mx-auto p-4 p-md-5" aria-labelledby="forgot-password-title">
      <p class="auth-eyebrow mb-2">DINO-GO MEMBER</p>
      <h1 id="forgot-password-title" class="dg-heading mb-2">忘記密碼</h1>
      <p class="text-muted mb-4">輸入帳號 Email，我們會寄送重設密碼連結給您。</p>

      <div v-if="isSubmitted" class="alert alert-success" role="status">
        若此 Email 已註冊，重設密碼說明已寄出。請查看收件匣與垃圾郵件匣。
      </div>
      <p v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</p>

      <form v-if="!isSubmitted" novalidate @submit.prevent="submit">
        <label class="form-label" for="reset-email">電子信箱</label>
        <input
          id="reset-email"
          v-model="email"
          class="form-control"
          type="email"
          autocomplete="email"
          maxlength="100"
          placeholder="name@example.com"
          :disabled="isSubmitting"
          required
        />
        <button class="btn dg-btn-primary mt-4 w-100" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '寄送中…' : '寄送重設連結' }}
        </button>
      </form>

      <p class="mt-4 mb-0 text-center text-muted">
        想起密碼了？ <RouterLink to="/login" class="auth-link">返回登入</RouterLink>
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
