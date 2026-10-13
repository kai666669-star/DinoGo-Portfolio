<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getMySellerApplication, submitSellerApplication } from '@/api/sellerApplicationApi'

const storeName = ref('')
const storeDescription = ref('')
const storeLogoUrl = ref('')
const storeNameError = ref('')
const apiError = ref('')
const isSubmitting = ref(false)
const router = useRouter()

function validateStoreName() {
  storeNameError.value = storeName.value.trim() ? '' : '請輸入店鋪名稱。'
  return !storeNameError.value
}

async function submitApplication() {
  apiError.value = ''
  if (!validateStoreName() || isSubmitting.value) return

  isSubmitting.value = true
  try {
    await submitSellerApplication({
      storeName: storeName.value.trim(),
      storeDescription: storeDescription.value.trim() || null,
      storeLogoUrl: storeLogoUrl.value.trim() || null,
    })
    await router.replace({ name: 'SellerApplicationStatus' })
  } catch (error) {
    apiError.value = error.response?.data?.message || '送出申請失敗，請稍後再試。'
  } finally {
    isSubmitting.value = false
  }
}

async function redirectWhenApplicationIsActive() {
  try {
    const { data } = await getMySellerApplication()
    if (data.status !== 'REJECTED') {
      await router.replace({ name: 'SellerApplicationStatus' })
    }
  } catch (error) {
    if (error.response?.status !== 404) {
      apiError.value = error.response?.data?.message || '申請狀態載入失敗，請稍後再試。'
    }
  }
}

onMounted(redirectWhenApplicationIsActive)
</script>

<template>
  <section class="seller-application" aria-labelledby="seller-application-title">
    <div class="container seller-application__container">
      <header class="seller-application__intro">
        <div class="seller-application__intro-copy">
          <p class="seller-application__eyebrow">商家會員申請</p>
          <h1 id="seller-application-title">準備好，開始經營你的店鋪</h1>
          <p>填寫公開店鋪資訊後送出申請；平台將以通知告知審核結果。</p>
        </div>
        <RouterLink class="seller-application__back dg-focus-ring" to="/member/overview">
          <i class="bi bi-arrow-left" aria-hidden="true"></i>
          返回會員中心
        </RouterLink>
      </header>

      <div class="seller-application__review-notice" role="note">
        <i class="bi bi-shield-check" aria-hidden="true"></i>
        <p>送出後將進入審核中；審核通過後，請重新登入以啟用商家中心權限。</p>
      </div>

      <div class="seller-application__content">
        <form class="seller-application__form dg-card" @submit.prevent="submitApplication">
          <header class="seller-application__form-header">
            <h2>店鋪公開資訊</h2>
            <p>核准後會顯示於顧客可瀏覽的店鋪頁面。</p>
          </header>

          <label class="seller-application__field" for="store-name">
            <span>店鋪名稱 <b aria-hidden="true">*</b></span>
            <input
              id="store-name"
              v-model="storeName"
              name="storeName"
              type="text"
              maxlength="100"
              placeholder="例如：森野選物所"
              required
              :aria-invalid="Boolean(storeNameError)"
              :aria-describedby="
                storeNameError ? 'store-name-help store-name-error' : 'store-name-help'
              "
              @blur="validateStoreName"
            />
            <small
              v-if="storeNameError"
              id="store-name-error"
              class="seller-application__field-error"
              role="alert"
            >
              {{ storeNameError }}
            </small>
          </label>

          <label class="seller-application__field" for="store-description">
            <span>店鋪介紹</span>
            <textarea
              id="store-description"
              v-model="storeDescription"
              name="storeDescription"
              rows="4"
              maxlength="500"
              placeholder="介紹你的品牌、商品特色與服務方式"
            ></textarea>
          </label>

          <label class="seller-application__field" for="store-logo-url">
            <span>店鋪 Logo 圖片網址</span>
            <input
              id="store-logo-url"
              v-model="storeLogoUrl"
              name="storeLogoUrl"
              type="url"
              placeholder="https://res.cloudinary.com/demo/image/upload/sample.jpg"
            />
          </label>

          <label class="seller-application__agreement" for="seller-application-agreement">
            <input id="seller-application-agreement" type="checkbox" />
            <span>我已閱讀商家服務條款，並確認以上公開資訊正確。</span>
          </label>

          <p v-if="apiError" class="seller-application__field-error" role="alert">{{ apiError }}</p>

          <footer class="seller-application__actions">
            <button class="seller-application__draft dg-focus-ring" type="button">暫存</button>
            <button class="seller-application__submit dg-btn-primary dg-focus-ring" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '送出中…' : '送出申請' }}
            </button>
          </footer>
        </form>

        <aside class="seller-application__sidebar" aria-label="申請說明">
          <section
            class="seller-application__process dg-card"
            aria-labelledby="seller-process-title"
          >
            <h2 id="seller-process-title" class="seller-application__process-title">申請流程</h2>
            <ol>
              <li class="seller-application__process-step">
                <span class="seller-application__process-badge">1</span>
                <div class="seller-application__process-copy">
                  <strong class="seller-application__process-step-title">填寫申請</strong>
                  <small class="seller-application__process-step-detail">確認店鋪公開資訊</small>
                </div>
              </li>
              <li class="seller-application__process-step">
                <span class="seller-application__process-badge">2</span>
                <div class="seller-application__process-copy">
                  <strong class="seller-application__process-step-title">平台審核</strong>
                  <small class="seller-application__process-step-detail">審核資格與資料</small>
                </div>
              </li>
              <li class="seller-application__process-step">
                <span class="seller-application__process-badge">3</span>
                <div class="seller-application__process-copy">
                  <strong class="seller-application__process-step-title">啟用商家</strong>
                  <small class="seller-application__process-step-detail">重新登入後開始營運</small>
                </div>
              </li>
            </ol>
          </section>

          <section class="seller-application__support" aria-labelledby="seller-support-title">
            <h2 id="seller-support-title">需要協助嗎？</h2>
            <p>若資料被駁回，請依通知中的原因修正後再次送件。</p>
          </section>
        </aside>
      </div>
    </div>
  </section>
</template>

<style scoped>
.seller-application {
  padding: 40px 0;
  color: var(--color-text);
  font-family: var(--font-body);
}

.seller-application__container {
  --bs-gutter-x: var(--space-6);
  display: grid;
  gap: var(--space-5);
  max-width: 1232px;
}

.seller-application__intro,
.seller-application__intro-copy,
.seller-application__content,
.seller-application__form,
.seller-application__sidebar,
.seller-application__form-header,
.seller-application__field,
.seller-application__actions,
.seller-application__support {
  display: grid;
}

.seller-application__intro {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: var(--space-5);
}

.seller-application__intro-copy {
  gap: var(--space-2);
}

.seller-application__eyebrow,
h1,
h2,
p {
  margin: 0;
}

.seller-application__eyebrow {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

h1,
h2,
strong {
  color: var(--color-text);
  font-family: var(--font-body);
  font-weight: 700;
}

h1 {
  font-size: var(--font-size-xl);
  line-height: var(--line-height-heading);
}

h2 {
  font-size: var(--font-size-md);
  line-height: var(--line-height-heading);
}

.seller-application__intro-copy > p:last-child,
.seller-application__form-header p,
.seller-application__support p,
.seller-application__process-step-detail {
  color: var(--color-text-muted);
  font-size: 13px;
}

.seller-application__back,
.seller-application__draft {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  color: var(--color-text);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-decoration: none;
}

.seller-application__back:hover,
.seller-application__draft:hover {
  color: var(--color-primary-active);
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.seller-application__review-notice {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  background: var(--color-primary-notice);
}

.seller-application__review-notice i {
  color: var(--color-primary-active);
  font-size: 22px;
}

.seller-application__review-notice p {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.seller-application__content {
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
  gap: var(--space-5);
}

.seller-application__form {
  gap: 20px;
  padding: 28px;
}

.seller-application__form-header {
  gap: var(--space-1);
}

.seller-application__form-header h2 {
  font-size: 20px;
}

.seller-application__field {
  gap: var(--space-2);
}

.seller-application__field > span {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.seller-application__field > span b {
  color: var(--color-danger);
}

.seller-application input:not([type='checkbox']),
.seller-application textarea {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-sm);
}

.seller-application textarea {
  min-height: 92px;
  padding-block: var(--space-3);
  resize: vertical;
}

.seller-application input::placeholder,
.seller-application textarea::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}

.seller-application__field > small {
  color: var(--color-text-muted);
  font-size: 13px;
}

.seller-application__field > .seller-application__field-error {
  color: var(--color-danger);
}

.seller-application input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.seller-application input:focus-visible,
.seller-application textarea:focus-visible,
.seller-application__agreement input:focus-visible {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.seller-application__agreement {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.seller-application__agreement input {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: var(--color-primary);
}

.seller-application__actions {
  grid-template-columns: auto auto;
  justify-content: end;
  gap: var(--space-3);
}

.seller-application__submit {
  min-height: 40px;
  border-radius: var(--radius-md);
  padding-inline: var(--space-4);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.seller-application__sidebar {
  gap: var(--space-4);
}

.seller-application__process {
  gap: var(--space-4);
  padding: var(--space-5);
}

.seller-application__process-title {
  font-size: var(--font-size-md);
  line-height: normal;
  padding-bottom: var(--space-3);
}

.seller-application__process ol {
  display: grid;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

.seller-application__process-step {
  display: flex;
  gap: var(--space-3);
}

.seller-application__process-badge {
  display: grid;
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  place-items: center;
  border-radius: var(--radius-pill);
  color: var(--color-primary-active);
  background: var(--color-primary-notice);
  font-size: 13px;
  font-weight: 700;
  line-height: normal;
}

.seller-application__process-copy {
  display: grid;
  align-content: start;
  gap: 3px;
}

.seller-application__process-step-title {
  font-size: var(--font-size-sm);
  line-height: normal;
}

.seller-application__process-step-detail {
  line-height: normal;
}

.seller-application__support {
  gap: 6px;
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--color-bg);
}

.seller-application__support h2 {
  font-size: 15px;
}

@media (max-width: 991.98px) {
  .seller-application__content {
    grid-template-columns: 1fr;
  }

  .seller-application__sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 575.98px) {
  .seller-application {
    padding: var(--space-6) 0;
  }

  .seller-application__intro,
  .seller-application__sidebar {
    grid-template-columns: 1fr;
  }

  .seller-application__intro {
    align-items: start;
  }

  .seller-application__back {
    width: fit-content;
  }

  .seller-application__review-notice {
    align-items: start;
  }

  .seller-application__form {
    padding: var(--space-5);
  }

  .seller-application__actions {
    grid-template-columns: 1fr;
  }

  .seller-application__actions button {
    width: 100%;
  }
}
</style>
