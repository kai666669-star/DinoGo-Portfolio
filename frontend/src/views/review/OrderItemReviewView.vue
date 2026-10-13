<script setup>
//review-start，總共3次修改，第1次//
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder } from '@/api/order'
import {
  clearStar,
  getOrderStars,
  updateStar,
  uploadReviewImages,
} from '@/api/review'
import { getImageUrl } from '@/utils/imageUrl'

const props = defineProps({
  orderData: { type: Object, default: null },
  initialOrderItemId: { type: Number, default: null },
  modal: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'updated'])
const route = useRoute()
const router = useRouter()
const order = ref(props.orderData)
const selectedOrderItemId = ref(Number(props.initialOrderItemId ?? route.params.orderItemId))
const star = ref(null)
const orderStars = ref([])
const rating = ref(0)
const feedback = ref('')
const images = ref([])
const loading = ref(true)
const saving = ref(false)
const clearing = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const feedbackTextarea = ref(null)
const isClosing = ref(false)
let closeTimer = null

const orderId = computed(() => Number(props.orderData?.orderId ?? props.orderData?.id ?? route.params.orderId ?? route.params.id))
const orderItemId = computed(() => selectedOrderItemId.value)
const item = computed(() =>
  order.value?.items?.find((candidate) => candidate.orderItemId === orderItemId.value) ?? null,
)
const productImage = computed(() => getImageUrl(star.value?.imageUrl || item.value?.productImageUrl || ''))
const productName = computed(() => star.value?.productName || item.value?.productName || '商品')
const productSpec = computed(() => item.value?.skuSpec || '單一規格')

function hydrateForm(nextStar) {
  rating.value = Number(nextStar?.fiveStar ?? 0)
  feedback.value = nextStar?.feedback ?? ''
  revokeLocalUrls()
  images.value = [
    [nextStar?.imgOne, nextStar?.imgOnePublicId],
    [nextStar?.imgTwo, nextStar?.imgTwoPublicId],
    [nextStar?.imgThree, nextStar?.imgThreePublicId],
  ]
    .filter(([secureUrl]) => Boolean(secureUrl))
    .map(([secureUrl, publicId]) => ({ secureUrl, publicId, preview: secureUrl, local: false }))
  void nextTick(resizeFeedbackToContent)
}

function resizeFeedbackToContent() {
  const textarea = feedbackTextarea.value
  if (!textarea) return
  textarea.style.height = ''
  if (textarea.scrollHeight > textarea.clientHeight) {
    textarea.style.height = `${textarea.scrollHeight}px`
  }
}

function growFeedback(event) {
  const textarea = event.currentTarget
  if (textarea.scrollHeight > textarea.clientHeight) {
    textarea.style.height = `${textarea.scrollHeight}px`
  }
}

async function loadReview() {
  loading.value = true
  errorMessage.value = ''
  if (!Number.isInteger(orderId.value) || !Number.isInteger(orderItemId.value)) {
    errorMessage.value = '訂單商品編號格式不正確。'
    loading.value = false
    return
  }
  try {
    if (!props.orderData) order.value = (await getOrder(orderId.value)).data
    if (!item.value) throw new Error('REVIEW_ITEM_NOT_FOUND')
    orderStars.value = (await getOrderStars(orderId.value)).data ?? []
    star.value = orderStars.value.find(
      (candidate) => candidate.orderItemId === orderItemId.value,
    ) ?? null
    if (!star.value) throw new Error('REVIEW_STAR_NOT_FOUND')
    hydrateForm(star.value)
  } catch (error) {
    errorMessage.value = error.response?.data?.message
      ?? (error.message === 'REVIEW_ITEM_NOT_FOUND' ? '找不到可評價的訂單商品。' : '評價資料載入失敗，請稍後再試。')
  } finally {
    loading.value = false
  }
}

function selectReviewItem() {
  star.value = orderStars.value.find(
    (candidate) => candidate.orderItemId === orderItemId.value,
  ) ?? null
  errorMessage.value = ''
  successMessage.value = ''
  if (star.value) hydrateForm(star.value)
  else errorMessage.value = '找不到此訂單品項的評價資料。'
}

async function addImages(event) {
  const files = Array.from(event.target.files ?? [])
  const remaining = 3 - images.value.length
  if (files.length > remaining) errorMessage.value = '評價照片至多上傳 3 張。'
  for (const file of files.slice(0, remaining)) {
    if (!file.type.startsWith('image/')) continue
    images.value.push({
      file,
      preview: URL.createObjectURL(file),
      local: true,
    })
  }
  event.target.value = ''
}

function removeImage(index) {
  const [removed] = images.value.splice(index, 1)
  if (removed?.local) URL.revokeObjectURL(removed.preview)
}

function revokeLocalUrls() {
  images.value.forEach((image) => {
    if (image.local) URL.revokeObjectURL(image.preview)
  })
}

async function handleClear() {
  if (!star.value || clearing.value || saving.value) return
  if (!window.confirm('是否確認清除全部')) return
  clearing.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    star.value = (await clearStar(star.value.starId)).data
    orderStars.value = orderStars.value.map((candidate) =>
      candidate.starId === star.value.starId ? star.value : candidate,
    )
    hydrateForm(star.value)
    emit('updated', star.value)
    successMessage.value = '評價內容已清空。'
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '清空失敗，請稍後再試。'
  } finally {
    clearing.value = false
  }
}

async function handleSubmit() {
  if (!star.value || saving.value || clearing.value) return
  if (rating.value < 1 || rating.value > 5) {
    errorMessage.value = '請選擇 1 至 5 顆星。'
    return
  }
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const remoteAssets = images.value.filter((image) => !image.local)
    const localFiles = images.value.filter((image) => image.local).map((image) => image.file)
    const uploadedAssets = localFiles.length
      ? ((await uploadReviewImages(localFiles)).data?.assets ?? []).map((asset) => ({
          secureUrl: asset.secureUrl,
          publicId: asset.publicId,
        }))
      : []
    const assets = [...remoteAssets, ...uploadedAssets].slice(0, 3)
    star.value = (await updateStar(star.value.starId, {
      fiveStar: rating.value,
      feedback: feedback.value.trim() || null,
      imgOne: assets[0]?.secureUrl ?? null,
      imgOnePublicId: assets[0]?.publicId ?? null,
      imgTwo: assets[1]?.secureUrl ?? null,
      imgTwoPublicId: assets[1]?.publicId ?? null,
      imgThree: assets[2]?.secureUrl ?? null,
      imgThreePublicId: assets[2]?.publicId ?? null,
    })).data
    orderStars.value = orderStars.value.map((candidate) =>
      candidate.starId === star.value.starId ? star.value : candidate,
    )
    hydrateForm(star.value)
    emit('updated', star.value)
    successMessage.value = '評價已送出。'
    if (props.modal) closeModalSmoothly()
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '評價送出失敗，請稍後再試。'
  } finally {
    saving.value = false
  }
}

function closeWithoutChanges() {
  if (props.modal) {
    closeModalSmoothly()
    return
  }
  router.push({ name: 'MemberOrderDetail', params: { id: orderId.value } })
}

function closeModalSmoothly() {
  if (isClosing.value) return
  isClosing.value = true
  closeTimer = window.setTimeout(() => emit('close'), 260)
}

function handleEscape(event) {
  if (event.key === 'Escape') closeWithoutChanges()
}

onMounted(() => {
  void loadReview()
  window.addEventListener('keydown', handleEscape)
  if (props.modal) document.body.style.overflow = 'hidden'
})
onBeforeUnmount(() => {
  revokeLocalUrls()
  if (closeTimer !== null) window.clearTimeout(closeTimer)
  window.removeEventListener('keydown', handleEscape)
  if (props.modal) document.body.style.overflow = ''
})
//review-end，總共3次修改，第1次//
</script>

<template>
  <!-- //review-start，總共3次修改，第2次// -->
  <section
    class="review-page"
    :class="{ 'review-page--modal': modal, 'review-page--closing': isClosing }"
    @click.self="closeWithoutChanges"
  >
    <div class="review-panel" role="dialog" :aria-modal="modal ? 'true' : undefined" aria-labelledby="review-title">
      <header class="review-header">
        <div>
          <h1 id="review-title">產品評價</h1>
        </div>
        <button type="button" class="close-button" aria-label="關閉且不做變更" @click="closeWithoutChanges">×</button>
      </header>

      <label v-if="order?.items?.length" class="review-item-select">
        <span>評價對象</span>
        <select v-model.number="selectedOrderItemId" @change="selectReviewItem">
          <option v-for="candidate in order.items" :key="candidate.orderItemId" :value="candidate.orderItemId">
            {{ candidate.productName }} · {{ candidate.skuSpec || '單一規格' }}
          </option>
        </select>
      </label>

      <div v-if="loading" class="review-state">正在載入評價資料...</div>
      <div v-else-if="errorMessage && !star" class="review-state review-state--error" role="alert">{{ errorMessage }}</div>

      <form v-else class="review-form" @submit.prevent="handleSubmit">
        <div class="product-summary">
          <div class="product-photo">
            <img v-if="productImage" :src="productImage" :alt="productName" />
            <i v-else class="bi bi-image" aria-hidden="true"></i>
          </div>
          <div>
            <h2>{{ productName }}</h2>
            <small>{{ productSpec }}</small>
          </div>
        </div>

        <fieldset class="rating-field" aria-required="true">
          <legend><span aria-hidden="true">*</span>五星評價(必填)</legend>
          <div class="star-picker">
            <button
              v-for="value in 5"
              :key="value"
              type="button"
              :aria-label="`${value} 顆星`"
              :aria-pressed="rating === value"
              @click="rating = value"
            >
              <i class="bi" :class="value <= rating ? 'bi-star-fill' : 'bi-star'" aria-hidden="true"></i>
            </button>
          </div>
        </fieldset>

        <label class="feedback-field">
          <span>評價內容</span>
          <textarea
            ref="feedbackTextarea"
            v-model="feedback"
            maxlength="500"
            rows="3"
            placeholder="分享這項商品的使用感受"
            @input="growFeedback"
          ></textarea>
          <small>{{ feedback.length }} / 500</small>
        </label>

        <section class="upload-field">
          <div class="upload-heading">
            <strong>上傳照片</strong>
          </div>
          <div class="image-grid">
            <div v-for="(image, index) in images" :key="image.preview" class="image-preview">
              <img :src="image.preview" :alt="`評價照片 ${index + 1}`" />
              <button type="button" :aria-label="`移除評價照片 ${index + 1}`" @click="removeImage(index)">×</button>
            </div>
            <label v-if="images.length < 3" class="upload-button">
              <i class="bi bi-camera" aria-hidden="true"></i>
              <span>上傳照片</span>
              <input type="file" accept="image/*" multiple @change="addImages" />
            </label>
          </div>
        </section>

        <p v-if="errorMessage" class="form-message form-message--error" role="alert">{{ errorMessage }}</p>
        <p v-if="successMessage" class="form-message form-message--success" role="status">{{ successMessage }}</p>

        <footer class="form-actions">
          <button type="button" class="clear-button" :disabled="clearing || saving" @click="handleClear">
            {{ clearing ? '清空中...' : '清空' }}
          </button>
          <button type="submit" class="submit-button" :disabled="saving || clearing">
            {{ saving ? '送出中...' : '送出' }}
          </button>
        </footer>
      </form>
    </div>
  </section>
  <!-- //review-end，總共3次修改，第2次// -->
</template>

<style scoped>
/* //review-start，總共3次修改，第3次// */
.review-page { min-height: calc(var(--space-8) * 10); padding: var(--space-6) var(--space-4); background: var(--color-bg); }
.review-page--modal { position: fixed; z-index: 1080; inset: 0; display: grid; overflow-y: auto; place-items: center; background: rgb(0 0 0 / 45%); opacity: 1; transition: opacity 260ms ease; }
.review-page--modal .review-panel { transition: opacity 260ms ease, transform 260ms ease; }
.review-page--closing { opacity: 0; pointer-events: none; }
.review-page--closing .review-panel { opacity: 0; transform: translateY(var(--space-2)) scale(.99); }
.review-page--modal .review-panel { width: min(calc(100% - var(--space-6)), calc(var(--space-8) * 11)); max-height: calc(100vh - var(--space-6)); overflow-y: auto; }
.review-panel { max-width: calc(var(--space-8) * 13); margin: 0 auto; overflow: hidden; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.review-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4); border-bottom: 1px solid var(--color-border); }
.review-header h1 { margin: 0; font-family: var(--font-heading); font-size: var(--font-size-xl); }
.close-button { width: calc(var(--space-5) + var(--space-4)); height: calc(var(--space-5) + var(--space-4)); color: var(--color-text-muted); font-size: var(--font-size-xl); line-height: 1; background: transparent; border: 0; border-radius: var(--radius-pill); cursor: pointer; }
.close-button:hover { color: var(--color-text); background: var(--color-bg-muted); }
.review-item-select { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: var(--space-3); padding: var(--space-3) var(--space-4); background: var(--color-bg-muted); border-bottom: 1px solid var(--color-border); }.review-item-select span { color: var(--color-text-muted); font-size: var(--font-size-xs); font-weight: 700; }.review-item-select select { min-width: 0; padding: var(--space-2) var(--space-3); color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); }
.review-state { padding: var(--space-8); color: var(--color-text-muted); text-align: center; }.review-state--error { color: var(--color-danger); }
.review-form { display: grid; gap: var(--space-3); padding: var(--space-4); }
.product-summary { display: grid; grid-template-columns: calc(var(--space-8) + var(--space-4)) minmax(0, 1fr); align-items: center; gap: var(--space-4); }
.product-photo { display: grid; width: calc(var(--space-8) + var(--space-4)); height: calc(var(--space-8) + var(--space-4)); overflow: hidden; place-items: center; color: var(--color-text-subtle); background: var(--color-bg-muted); border-radius: var(--radius-md); }.product-photo img { width: 100%; height: 100%; object-fit: cover; }
.product-summary span, .product-summary small { color: var(--color-text-muted); font-size: var(--font-size-xs); }.product-summary h2 { margin: var(--space-1) 0; font-size: var(--font-size-lg); }
.rating-field { margin: 0; padding: 0; border: 0; }.rating-field legend, .feedback-field > span, .upload-heading strong { margin-bottom: var(--space-2); font-size: var(--font-size-sm); font-weight: 700; }
.rating-field legend span { color: var(--color-danger); }
.star-picker { display: flex; gap: var(--space-2); }.star-picker button { padding: 0; color: var(--color-warning); font-size: var(--font-size-2xl); background: transparent; border: 0; cursor: pointer; }.star-picker button:hover { color: var(--color-primary-hover); transform: translateY(-1px); }.star-picker button:active { color: var(--color-primary-active); }
.feedback-field { display: grid; }.feedback-field textarea { min-height: calc(var(--space-8) + var(--space-4)); overflow-y: hidden; resize: vertical; padding: var(--space-3); color: var(--color-text); font: inherit; background: var(--color-surface); border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); }.feedback-field small { margin-top: var(--space-1); color: var(--color-text-muted); text-align: right; }
.upload-heading { display: flex; justify-content: space-between; }.image-grid { display: flex; flex-wrap: wrap; gap: var(--space-3); }.image-preview, .upload-button { position: relative; width: calc(var(--space-8) + var(--space-5)); height: calc(var(--space-8) + var(--space-5)); overflow: hidden; border-radius: var(--radius-md); }.image-preview img { width: 100%; height: 100%; object-fit: cover; }.image-preview button { position: absolute; top: var(--space-1); right: var(--space-1); width: var(--space-6); height: var(--space-6); color: var(--color-surface); background: var(--color-text-muted); border: 0; border-radius: var(--radius-pill); cursor: pointer; }.upload-button { display: grid; place-items: center; align-content: center; gap: var(--space-1); color: var(--color-text-muted); font-size: var(--font-size-xs); border: 1px dashed var(--color-border-strong); cursor: pointer; }.upload-button i { font-size: var(--font-size-lg); }.upload-button input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.form-message { margin: 0; padding: var(--space-3); border-radius: var(--radius-md); }.form-message--error { color: var(--color-danger); background: var(--color-danger-soft); }.form-message--success { color: var(--color-success); background: var(--color-success-soft); }
.form-actions { display: flex; justify-content: center; gap: var(--space-3); padding-top: var(--space-3); border-top: 1px solid var(--color-border); }.form-actions button { min-width: calc(var(--space-8) + var(--space-6)); min-height: calc(var(--space-6) + var(--space-3)); font: inherit; font-weight: 700; border-radius: var(--radius-md); cursor: pointer; }.form-actions button:hover:not(:disabled) { border-color: var(--color-primary-hover); }.form-actions button:active:not(:disabled) { border-color: var(--color-primary-active); }.form-actions button:disabled { color: var(--color-text-subtle); background: var(--color-disabled-bg); border-color: var(--color-disabled); cursor: not-allowed; }.clear-button { color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border-strong); }.submit-button { color: var(--color-surface); background: var(--color-primary); border: 1px solid var(--color-primary); }.submit-button:hover:not(:disabled) { color: var(--color-surface); background: var(--color-primary-hover); }.submit-button:active:not(:disabled) { background: var(--color-primary-active); }
button:focus-visible, textarea:focus-visible, .upload-button:focus-within { outline: none; box-shadow: var(--shadow-focus); }
@media (max-width: 575.98px) { .review-page { padding: 0; }.review-panel { border-inline: 0; border-radius: 0; }.product-summary { grid-template-columns: calc(var(--space-8) + var(--space-3)) minmax(0, 1fr); }.product-photo { width: calc(var(--space-8) + var(--space-3)); height: calc(var(--space-8) + var(--space-3)); }.form-actions { display: grid; grid-template-columns: 1fr 1fr; }.form-actions button { width: 100%; } }
/* //review-end，總共3次修改，第3次// */
</style>
