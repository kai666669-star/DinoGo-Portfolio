<script>
const googleIdentityServicesUrl = 'https://accounts.google.com/gsi/client'
let googleIdentityServicesPromise
let initializedGoogleClientId
let activeGoogleCredentialHandler

function loadGoogleIdentityServices() {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (googleIdentityServicesPromise) return googleIdentityServicesPromise

  googleIdentityServicesPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector(`script[src="${googleIdentityServicesUrl}"]`)
    const script = existingScript || document.createElement('script')

    script.addEventListener('load', resolve, { once: true })
    script.addEventListener(
      'error',
      () => {
        googleIdentityServicesPromise = undefined
        reject(new Error('Google Identity Services 載入失敗'))
      },
      { once: true },
    )

    if (existingScript) return

    script.src = googleIdentityServicesUrl
    script.async = true
    script.defer = true
    document.head.appendChild(script)
  })

  return googleIdentityServicesPromise
}

function initializeGoogleIdentityServices(clientId, credentialHandler) {
  activeGoogleCredentialHandler = credentialHandler

  if (initializedGoogleClientId) {
    if (initializedGoogleClientId !== clientId) {
      throw new Error('Google Identity Services Client ID 不一致')
    }
    return
  }

  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: (response) => activeGoogleCredentialHandler?.(response),
    use_fedcm_for_button: true,
  })
  initializedGoogleClientId = clientId
}
</script>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const form = ref({ email: '', password: '' })
const fieldErrors = ref({})
const apiError = ref('')
const isSubmitting = ref(false)
const googleButton = ref(null)
const googleCredential = ref('')
const googleLinkPassword = ref('')
const isGoogleLinking = ref(false)
const isGoogleReady = ref(false)

const registeredMessage = route.query.registered ? '註冊成功，請使用新帳號登入。' : ''

function validate() {
  const errors = {}
  if (!form.value.email.trim()) errors.email = '請輸入 Email。'
  else if (!/^\S+@\S+\.\S+$/.test(form.value.email)) errors.email = '請輸入有效的 Email。'
  if (!form.value.password) errors.password = '請輸入密碼。'
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function getErrorMessage(error) {
  if (!error.response) {
    return '無法連線到伺服器，請確認後端是否已啟動。'
  }

  const { data, status } = error.response
  if (status === 401) return 'Email 或密碼錯誤。'
  if (typeof data === 'string' && data.trim()) return data
  return data?.message || '登入失敗，請稍後再試。'
}

async function submit() {
  apiError.value = ''
  if (!validate()) return

  isSubmitting.value = true
  try {
    // 登入資料交給 authStore 統一保存，其他元件會同步取得最新狀態。
    await authStore.signIn(form.value)
    await router.push(resolvePostLoginRoute())
  } catch (error) {
    apiError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function redirectAfterLogin() {
  return router.push(resolvePostLoginRoute())
}

function resolvePostLoginRoute() {
  const redirect =
    typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : authStore.hasRole('admin')
        ? '/admin/dashboard'
        : authStore.isSeller
          ? '/seller/dashboard'
          : '/member/overview'
  return redirect
}

async function handleGoogleCredential(response) {
  apiError.value = ''
  googleCredential.value = response.credential
  try {
    await authStore.signInWithGoogle(response.credential)
    await redirectAfterLogin()
  } catch (error) {
    if (error.response?.status === 409) {
      isGoogleLinking.value = true
      apiError.value = '此 Email 已有密碼帳號，請輸入原密碼完成 Google 帳號綁定。'
      return
    }
    apiError.value = getErrorMessage(error)
  }
}

async function linkGoogle() {
  apiError.value = ''
  if (!googleLinkPassword.value) {
    apiError.value = '請輸入原密碼以完成帳號綁定。'
    return
  }

  isSubmitting.value = true
  try {
    await authStore.linkGoogleSignIn(googleCredential.value, googleLinkPassword.value)
    await redirectAfterLogin()
  } catch (error) {
    apiError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

let isMounted = false

onMounted(async () => {
  isMounted = true
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  if (!clientId) {
    apiError.value = 'Google 登入尚未設定。'
    return
  }

  try {
    await loadGoogleIdentityServices()
    if (!isMounted) return

    initializeGoogleIdentityServices(clientId, handleGoogleCredential)
    await nextTick()
    if (!isMounted || !googleButton.value) return

    const buttonWidth = Math.min(360, googleButton.value.clientWidth || 360)
    window.google.accounts.id.renderButton(googleButton.value, {
      theme: 'outline',
      size: 'large',
      text: 'signin_with',
      width: buttonWidth,
      locale: 'zh_TW',
    })
    isGoogleReady.value = true
  } catch (error) {
    if (isMounted) apiError.value = 'Google 登入元件載入失敗，請稍後再試。'
  }
})

onUnmounted(() => {
  isMounted = false
  if (activeGoogleCredentialHandler === handleGoogleCredential) {
    activeGoogleCredentialHandler = undefined
  }
})
</script>

<template>
  <main class="auth-page container py-5">
    <section class="auth-card dg-card mx-auto p-4 p-md-5" aria-labelledby="login-title">
      <p class="auth-eyebrow mb-2">DINO-GO MEMBER</p>
      <h1 id="login-title" class="dg-heading mb-2">登入頁</h1>
      <p class="text-muted mb-4">登入後查看訂單、收藏與會員資訊。</p>

      <div v-if="registeredMessage" class="alert alert-success" role="status">
        {{ registeredMessage }}
      </div>
      <div v-if="apiError" class="alert alert-danger" role="alert">{{ apiError }}</div>

      <form novalidate @submit.prevent="submit">
        <div class="mb-3">
          <label class="form-label" for="login-email">電子信箱</label>
          <input
            id="login-email"
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

        <div class="mb-4">
          <label class="form-label" for="login-password">密碼</label>
          <input
            id="login-password"
            v-model="form.password"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.password }"
            type="password"
            autocomplete="current-password"
            placeholder="請輸入密碼"
            @input="delete fieldErrors.password"
          />
          <div v-if="fieldErrors.password" class="invalid-feedback">{{ fieldErrors.password }}</div>
        </div>

        <button
          class="btn dg-btn-primary login-submit w-100"
          type="submit"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '登入中…' : '登入' }}
        </button>
      </form>

      <p class="mt-3 mb-0 text-end">
        <RouterLink to="/forgot-password" class="auth-link">忘記密碼？</RouterLink>
      </p>

      <div class="login-divider" aria-hidden="true"><span>或</span></div>

      <div ref="googleButton" class="google-button" :aria-busy="!isGoogleReady"></div>

      <form v-if="isGoogleLinking" class="mt-4" novalidate @submit.prevent="linkGoogle">
        <label class="form-label" for="google-link-password">原帳號密碼</label>
        <input
          id="google-link-password"
          v-model="googleLinkPassword"
          class="form-control"
          type="password"
          autocomplete="current-password"
          placeholder="請輸入原密碼以綁定 Google"
        />
        <button class="btn dg-btn-primary mt-3 w-100" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '綁定中…' : '綁定 Google 並登入' }}
        </button>
      </form>

      <p class="mt-4 mb-0 text-muted">
        還沒有帳號？
        <RouterLink to="/register" class="auth-link">前往註冊</RouterLink>
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
.login-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-4) 0;
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
}
.login-divider::before,
.login-divider::after {
  height: 1px;
  flex: 1;
  content: '';
  background: var(--color-border);
}
.google-button {
  display: flex;
  justify-content: center;
  min-height: 40px;
}
</style>
