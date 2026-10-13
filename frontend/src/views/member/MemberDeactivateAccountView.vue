<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { deactivateMemberAccount } from '@/api/member'
import { useAuthStore } from '@/stores/auth'
const password = ref('')
const confirmed = ref(false)
const feedback = ref('')
const isSubmitting = ref(false)
const router = useRouter()
const authStore = useAuthStore()
async function submit() {
  if (!confirmed.value || !password.value || isSubmitting.value) return
  isSubmitting.value = true
  feedback.value = ''
  try {
    await deactivateMemberAccount({ currentPassword: password.value })
    authStore.signOut()
    await router.replace({ name: 'Home' })
  } catch (error) {
    feedback.value = error.response?.data?.message || '帳號註銷失敗。'
  } finally {
    isSubmitting.value = false
  }
}
</script>
<template>
  <main class="deactivate">
    <div class="container">
      <section class="card">
        <p>帳戶設定</p>
        <h1>註銷帳號</h1>
        <div class="notice">
          <strong><i class="bi bi-exclamation-triangle-fill"></i>註銷後無法登入</strong
          ><span>歷史訂單將依法保留；有未完成訂單或商家身分的帳號不可自行註銷。</span>
        </div>
        <form @submit.prevent="submit">
          <label
            >目前密碼<input
              v-model="password"
              type="password"
              autocomplete="current-password"
              required /></label
          ><label class="check"
            ><input v-model="confirmed" type="checkbox" /><span
              >我了解註銷後帳號將無法恢復。</span
            ></label
          >
          <p v-if="feedback" role="alert">{{ feedback }}</p>
          <div>
            <RouterLink :to="{ name: 'MemberProfile' }">返回個人資料</RouterLink
            ><button class="danger" :disabled="!confirmed || !password || isSubmitting">
              確認註銷
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
<style scoped>
.deactivate {
  min-height: 520px;
  background: var(--color-bg);
  padding: 40px 0;
}
.deactivate > .container {
  --bs-gutter-x: var(--space-6);
  max-width: 1232px;
}
.card {
  display: grid;
  gap: 18px;
  max-width: 620px;
  margin-inline: auto;
  padding: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.card > p,
h1 {
  margin: 0;
}
.card > p {
  color: var(--color-primary-active);
  font-size: 15px;
  font-weight: 700;
}
h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.notice {
  display: grid;
  gap: 5px;
  padding: 16px;
  border-radius: var(--radius-md);
  color: var(--color-danger);
  background: var(--color-danger-soft);
}
.notice strong {
  font-size: var(--font-size-md);
  line-height: 1.4;
}
.notice span {
  color: var(--color-text-700);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}
form {
  display: grid;
  gap: 16px;
}
label {
  display: grid;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
}
input[type='password'] {
  height: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font: inherit;
  font-size: var(--font-size-base);
}
.check {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-weight: 400;
  font-size: var(--font-size-sm);
  line-height: var(--line-height-base);
}
.check input {
  margin-top: 4px;
}
form p {
  margin: 0;
  color: var(--color-danger);
}
form div {
  display: flex;
  justify-content: end;
  gap: 10px;
  align-items: center;
}
a,
button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 600;
}
a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text);
  text-decoration: none;
  border: 1px solid var(--color-border);
}
button {
  border: 1px solid var(--color-danger);
  color: #fff;
  background: var(--color-danger);
}
button:disabled {
  border-color: var(--color-disabled);
  color: var(--color-text-subtle);
  background: var(--color-disabled-bg);
}
@media (max-width: 575.98px) {
  .deactivate {
    padding: var(--space-6) 0;
  }
}
</style>
