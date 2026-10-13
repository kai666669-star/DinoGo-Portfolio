<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
import {
  createPayment,
  getPaymentCapabilities,
  simulatePayment,
  submitEcpayCheckout,
} from '@/api/order'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'

const pageTitle = '結帳'
const router = useRouter()

// ========================================
// 狀態
// ========================================

const loading = ref(false)
const submitting = ref(false)
const couponLoading = ref(false)
const errorMessage = ref('')
const couponErrorMessage = ref('')
// ========================================
// 商品購買狀態
// ========================================

const stockErrorMessage = ref('')
const stockErrorItems = ref([])
// ========================================
// 判斷商品是否有規格
// ========================================

const hasSkuOptions = (item) => {
  return item.skus?.some(
    (sku) => sku.skuName !== null && sku.skuName !== undefined && sku.skuName !== '',
  )
}
// ========================================
// 結帳商品
// ========================================

const checkoutItems = ref([])
const checkoutSellerId = ref(null)
// ========================================
// 收件地址
// ========================================

const addresses = ref([])
const selectedAddressId = ref(null)

// ========================================
// 配送方式
// ========================================

const shippingMethod = ref('HOME_DELIVERY')

// ========================================
// 付款方式
// ========================================
const paymentMethod = ref('CASH_ON_DELIVERY')
const paymentSimulationEnabled = ref(false)
const simulatedPaymentStatus = ref('SUCCESS')
const simulatedPaymentFailureReason = ref('')

const loadPaymentCapabilities = async () => {
  try {
    const response = await getPaymentCapabilities()

    const data = response.data

    console.log('付款能力：', data)

    paymentSimulationEnabled.value = data?.simulationEnabled === true || data?.enabled === true
  } catch (error) {
    logSafeError('取得付款能力失敗：', error)

    paymentSimulationEnabled.value = false
  }
}

// ========================================
// 訂單備註
// ========================================

const buyerRemark = ref('')
// ========================================
// 優惠券
// ========================================

const coupons = ref([])
const selectedMemberCouponId = ref(null)

// ========================================
// 金額
// ========================================

const subtotal = ref(0)
const shippingFee = ref(0)
const discount = ref(0)
const totalAmount = ref(0)

// ========================================
// 目前選擇的地址
// ========================================

const selectedAddress = computed(() => {
  return addresses.value.find((address) => address.addressId === selectedAddressId.value)
})

// ========================================
// 目前選擇的優惠券
// ========================================

const selectedCoupon = computed(() => {
  return coupons.value.find(
    (coupon) => Number(coupon.memberCouponId) === Number(selectedMemberCouponId.value),
  )
})

const onlinePaymentAvailable = computed(() => true)

// ========================================
// 金額格式
// ========================================

const formatPrice = (price) => {
  const number = Number(price)

  if (Number.isNaN(number)) {
    return '0'
  }

  return number.toLocaleString('zh-TW')
}

// ========================================
// 優惠券折扣文字
// ========================================

const getCouponDiscountText = (coupon) => {
  if (!coupon) {
    return ''
  }

  // 如果後端是固定金額折扣
  if (coupon.discountType === 'FIXED' || coupon.discountType === 'AMOUNT') {
    return `折 NT$ ${formatPrice(coupon.discountValue)}`
  }

  // 如果後端是百分比折扣
  if (coupon.discountType === 'PERCENT' || coupon.discountType === 'PERCENTAGE') {
    return `${coupon.discountValue}% 折扣`
  }

  // 如果後端直接提供 discountAmount
  if (coupon.discountAmount != null) {
    return `折 NT$ ${formatPrice(coupon.discountAmount)}`
  }

  return '優惠券'
}

// ========================================
// 取得購物車勾選商品
// ========================================

const loadCheckoutItems = () => {
  try {
    const data = localStorage.getItem('checkoutData')

    // 沒有 checkoutData
    if (!data) {
      alert('沒有可結帳的商品')
      router.push('/cart')
      return false
    }

    const parsedData = JSON.parse(data)

    // ========================================
    // 基本資料檢查
    // ========================================

    if (!parsedData.sellerId || !Array.isArray(parsedData.items) || parsedData.items.length === 0) {
      alert('結帳資料不完整，請重新選擇商品')

      localStorage.removeItem('checkoutData')
      router.push('/cart')

      return false
    }

    // ========================================
    // 驗證所有商品是否為同一個賣家
    // ========================================

    const sellerIds = [...new Set(parsedData.items.map((item) => Number(item.sellerId)))]

    if (sellerIds.length > 1) {
      alert('不同賣家的商品不能一起結帳，請重新選擇商品')

      localStorage.removeItem('checkoutData')
      router.push('/cart')

      return false
    }

    // ========================================
    // 驗證 sellerId 是否一致
    // ========================================

    if (Number(parsedData.sellerId) !== sellerIds[0]) {
      alert('結帳賣家資料不一致，請重新選擇商品')

      localStorage.removeItem('checkoutData')
      router.push('/cart')

      return false
    }

    // ========================================
    // ⭐ 把商品真正放進 checkoutItems
    // ========================================

    checkoutItems.value = parsedData.items

    // 記住這次結帳的賣家
    checkoutSellerId.value = Number(parsedData.sellerId)

    console.log('Checkout 商品：', checkoutItems.value)
    console.log('Checkout Seller ID：', checkoutSellerId.value)

    // ========================================
    // 成功
    // ========================================

    return true
  } catch (error) {
    logSafeError('讀取結帳資料失敗：', error)

    alert('結帳資料錯誤，請重新選擇商品')

    localStorage.removeItem('checkoutData')

    router.push('/cart')

    return false
  }
}

// ========================================
// 取得會員地址
// ========================================

const loadAddresses = async () => {
  try {
    const response = await api.get('/addresses')

    addresses.value = response.data || []

    // 優先選擇預設地址
    const defaultAddress = addresses.value.find((address) => address.isDefault)

    selectedAddressId.value = defaultAddress?.addressId ?? addresses.value[0]?.addressId ?? null
  } catch (error) {
    logSafeError('取得地址失敗：', error)

    errorMessage.value = error.response?.data?.message || '無法取得收件地址'
  }
}

// ========================================
// 取得可用優惠券
// ========================================

const loadCoupons = async () => {
  try {
    couponLoading.value = true
    couponErrorMessage.value = ''

    const response = await api.get('/member/coupons')

    coupons.value = (response.data || []).filter((coupon) => coupon.status === 'AVAILABLE')
  } catch (error) {
    logSafeError('取得優惠券失敗：', error)

    couponErrorMessage.value = error.response?.data?.message || '無法取得可用優惠券'

    coupons.value = []
  } finally {
    couponLoading.value = false
  }
}

const getCouponApplicableSubtotal = (coupon) => {
  const itemSubtotal = (item) => Number(item.price || 0) * Number(item.quantity || 0)

  if (coupon.scopeType === 'PRODUCT') {
    return checkoutItems.value
      .filter((item) => Number(item.productId) === Number(coupon.productId))
      .reduce((sum, item) => sum + itemSubtotal(item), 0)
  }

  return checkoutItems.value.reduce((sum, item) => sum + itemSubtotal(item), 0)
}

const getCouponUnavailableReason = (coupon) => {
  // 不同賣家
  if (Number(coupon.sellerId) !== Number(checkoutSellerId.value)) {
    return '此優惠券不適用於目前商品'
  }

  if (coupon.scopeType === 'PRODUCT' && getCouponApplicableSubtotal(coupon) <= 0) {
    return '此商品優惠券不適用於本次商品'
  }

  // 最低消費
  const minPurchaseAmount = Number(coupon.minPurchaseAmount ?? 0)
  const currentSubtotal = getCouponApplicableSubtotal(coupon)

  if (currentSubtotal < minPurchaseAmount) {
    return `未滿 NT$ ${formatPrice(minPurchaseAmount)}，不可使用`
  }

  return ''
}
//判斷是否該賣家的優惠券
const isCouponAvailable = (coupon) => {
  const sameSeller = Number(coupon.sellerId) === Number(checkoutSellerId.value)
  const hasApplicableProduct =
    coupon.scopeType !== 'PRODUCT' || getCouponApplicableSubtotal(coupon) > 0

  const minPurchaseAmount = Number(coupon.minPurchaseAmount ?? 0)
  const currentSubtotal = getCouponApplicableSubtotal(coupon)

  // console.log('優惠券判斷：', {
  //   couponName: coupon.couponName,
  //   sellerId: coupon.sellerId,
  //   checkoutSellerId: checkoutSellerId.value,
  //   minPurchaseAmount,
  //   currentSubtotal,
  //   result: sameSeller && currentSubtotal >= minPurchaseAmount,
  // })

  return sameSeller && hasApplicableProduct && currentSubtotal >= minPurchaseAmount
}
const getCouponSellerName = (coupon) => {
  return coupon.sellerName || `賣家 #${coupon.sellerId}`
}
// ========================================
// 建立 Checkout Preview Request
// ========================================

const buildPreviewRequest = () => {
  return {
    items: checkoutItems.value.map((item) => ({
      skuId: item.skuId,
      quantity: Number(item.quantity),
    })),

    addressId: selectedAddressId.value,

    shippingMethod: shippingMethod.value,

    paymentMethod: paymentMethod.value,

    // 只傳會員已領取的優惠券識別碼
    memberCouponId: selectedMemberCouponId.value,
  }
}

// ========================================
// 取得結帳金額
// ========================================

const loadCheckoutPreview = async () => {
  if (!selectedAddressId.value) {
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    const request = buildPreviewRequest()

    console.log('================================')
    console.log('Checkout Preview Request')
    console.log(request)
    console.log('memberCouponId: ', request.memberCouponId)
    console.log('================================')

    const response = await api.post('/checkout/preview', request)

    const data = response.data

    console.log('================================')
    console.log('Checkout Preview Response')
    console.log(data)
    console.log('subtotal：', data.subtotal)
    console.log('shippingFee：', data.shippingFee)
    console.log('discount：', data.discount)
    console.log('totalAmount：', data.totalAmount)
    console.log('================================')

    subtotal.value = Number(data.subtotal ?? 0)
    shippingFee.value = Number(data.shippingFee ?? 0)
    discount.value = Number(data.discount ?? 0)
    totalAmount.value = Number(data.totalAmount ?? 0)
  } catch (error) {
    logSafeError('取得結帳金額失敗：', error)

    errorMessage.value = error.response?.data?.message || '無法取得訂單金額'

    // 優惠券無效才清除優惠券
    if (selectedMemberCouponId.value !== null) {
      selectedMemberCouponId.value = null
      discount.value = 0
    }
  } finally {
    loading.value = false
  }
}

// ========================================
// 地址改變
// ========================================

const changeAddress = async () => {
  await loadCheckoutPreview()
}

// ========================================
// 配送方式改變
// ========================================

const changeShippingMethod = async () => {
  await loadCheckoutPreview()
}

// ========================================
// 付款方式改變
// ========================================

const changePaymentMethod = async () => {
  await loadCheckoutPreview()
}

// ========================================
// 優惠券改變
// ========================================
const changeCoupon = async () => {
  couponErrorMessage.value = ''
  errorMessage.value = ''

  const coupon = selectedCoupon.value

  // 防止其他賣家的優惠券被套用
  if (coupon && !isCouponAvailable(coupon)) {
    selectedMemberCouponId.value = null

    console.warn('此優惠券不屬於目前結帳賣家')

    return
  }

  console.log('選擇優惠券 ID：', selectedMemberCouponId.value)
  console.log('選擇優惠券：', selectedCoupon.value)

  if (!selectedAddressId.value) {
    couponErrorMessage.value = '請先新增或選擇收件地址，再套用優惠券。'
    return
  }

  await loadCheckoutPreview()
}

// ========================================
// 取消優惠券
// ========================================

const removeCoupon = async () => {
  selectedMemberCouponId.value = null

  couponErrorMessage.value = ''

  await loadCheckoutPreview()
}

// ========================================
// 建立正式訂單 Request
// ========================================

const buildOrderRequest = () => {
  return {
    addressId: selectedAddressId.value,

    shippingMethod: shippingMethod.value,

    buyerRemark: buyerRemark.value.trim(),
    // 會員已領取的優惠券
    memberCouponId: selectedMemberCouponId.value,

    items: checkoutItems.value.map((item) => ({
      skuId: item.skuId,
      quantity: Number(item.quantity),
    })),
  }
}
// ========================================
// 處理商品無法購買錯誤
// ========================================

const handleOrderError = (error) => {
  logSafeError('建立訂單失敗：', error)

  const status = error.response?.status
  const data = error.response?.data

  stockErrorMessage.value = ''
  stockErrorItems.value = []

  // ========================================
  // 商品庫存不足 / 商品下架
  // ========================================

  if (status === 400 || status === 409) {
    const message = data?.message || data?.error || '部分商品目前無法購買，可能已下架或庫存不足'

    stockErrorMessage.value = message

    // 如果後端有回傳無法購買的商品
    if (Array.isArray(data?.items)) {
      stockErrorItems.value = data.items
    }

    return
  }

  // ========================================
  // 其他錯誤
  // ========================================

  errorMessage.value = data?.message || data?.error || '訂單建立失敗，請稍後再試'
}
// ========================================
// 送出訂單
// ========================================

const submitOrder = async () => {
  if (submitting.value) return

  // ========================================
  // 沒有地址
  // ========================================

  if (!selectedAddressId.value) {
    alert('請選擇收件地址')
    return
  }

  // ========================================
  // 沒有商品
  // ========================================

  if (checkoutItems.value.length === 0) {
    alert('沒有可結帳的商品')
    return
  }

  const submittedPaymentMethod = paymentMethod.value
  const submittedSimulationStatus = simulatedPaymentStatus.value
  const submittedFailureReason =
    submittedSimulationStatus === 'FAILED'
      ? simulatedPaymentFailureReason.value.trim() || null
      : null
  let createdOrderId = null

  if (submittedPaymentMethod === 'LINE_PAY' && !paymentSimulationEnabled.value) {
    errorMessage.value = '目前環境未啟用線上付款，請選擇貨到付款。'
    return
  }

  try {
    submitting.value = true

    errorMessage.value = ''
    stockErrorMessage.value = ''
    stockErrorItems.value = []

    const request = buildOrderRequest()

    // ========================================
    // 建立正式訂單
    //
    // 後端會再次檢查：
    // 1. 商品是否下架
    // 2. SKU 是否存在
    // 3. 庫存是否足夠
    // ========================================

    const response = await api.post('/orders', request)
    createdOrderId = response.data.orderId

    // ========================================
    // 訂單成功後才刪除購物車商品
    // ========================================

    // ========================================
    // 所有付款方式都建立付款紀錄；只有線上付款立即模擬成功結果
    // ========================================

    try {
      const paymentResponse = await createPayment(createdOrderId, submittedPaymentMethod)
      console.log('========== PAYMENT RESPONSE ==========')
      console.log(paymentResponse)
      console.log('payment data:', paymentResponse.data)
      console.log('ecpayCheckout:', paymentResponse.data?.ecpayCheckout)
      console.log('======================================')
      if (submittedPaymentMethod === 'CREDIT_CARD' && paymentResponse.data.ecpayCheckout) {
        submitEcpayCheckout(paymentResponse.data.ecpayCheckout)
        return
      }

      if (submittedPaymentMethod !== 'CASH_ON_DELIVERY' && onlinePaymentAvailable.value) {
        await simulatePayment(
          createdOrderId,
          paymentResponse.data.paymentId,
          submittedSimulationStatus,
          submittedFailureReason,
        )
      }
    } catch (paymentError) {
      logSafeError('付款失敗：', paymentError)

      await finalizeCreatedOrder()

      alert(
        paymentError.response?.data?.message ||
          '訂單已建立，但付款尚未完成。請至訂單詳情確認付款狀態。',
      )

      await router.push({
        name: 'MemberOrderDetail',
        params: { id: createdOrderId },
      })

      return
    }

    // ========================================
    // 2. 訂單成功後刪除購物車商品
    // 清除 checkoutData
    // ========================================

    await finalizeCreatedOrder()

    // ========================================
    // 回首頁
    // ========================================

    await router.push({ name: 'MemberOrderDetail', params: { id: createdOrderId } })
  } catch (error) {
    // ========================================
    // 訂單建立失敗
    // ========================================

    errorMessage.value =
      error.response?.data?.message ||
      (createdOrderId
        ? '訂單已建立，但後續處理失敗。請至會員訂單確認。'
        : '訂單建立失敗，請稍後再試')
    handleOrderError(error)
  } finally {
    submitting.value = false
  }
}

const finalizeCreatedOrder = async () => {
  localStorage.removeItem('checkoutData')
  checkoutItems.value = []
}

const clearCheckoutItemsFromCart = async () => {
  console.log('========== 清除結帳購物車 ==========')
  console.log('checkoutItems:', checkoutItems.value)

  const cartItemIds = checkoutItems.value.map((item) => item.cartItemId).filter((id) => id != null)

  console.log('準備刪除 cartItemIds:', cartItemIds)

  if (cartItemIds.length === 0) {
    console.log('沒有 cartItemId，不需要刪除')
    return
  }

  await Promise.all(
    cartItemIds.map(async (cartItemId) => {
      console.log(`準備 DELETE /cart/items/${cartItemId}`)

      const response = await api.delete(`/cart/items/${cartItemId}`)

      console.log(`DELETE /cart/items/${cartItemId} 成功`, response.data)

      return response
    }),
  )
}
// ========================================
// 初始化
// ========================================

const init = async () => {
  const hasItems = loadCheckoutItems()

  if (!hasItems) {
    return
  }

  // 同時取得地址與優惠券
  await Promise.all([loadAddresses(), loadCoupons(), loadPaymentCapabilities()])

  // 有地址才取得結帳金額
  if (selectedAddressId.value) {
    await loadCheckoutPreview()
  }
}

onMounted(() => {
  init()
})
</script>

<template>
  <main class="checkout-page">
    <div class="checkout-container">
      <!-- ========================================
            Header
        ======================================== -->

      <header class="checkout-header">
        <h1>{{ pageTitle }}</h1>

        <p>請確認收件、配送、付款方式與訂單摘要</p>
      </header>

      <!-- ========================================
            Error
        ======================================== -->

      <div v-if="errorMessage" class="error-message" role="alert">
        <i class="bi bi-exclamation-circle"></i>

        <span>{{ errorMessage }}</span>
      </div>

      <!-- ========================================
            Loading
        ======================================== -->

      <div v-if="loading && checkoutItems.length === 0" class="loading-state">
        <div class="spinner-border" role="status">
          <span class="visually-hidden"> Loading... </span>
        </div>

        <p>正在載入結帳資料...</p>
      </div>

      <!-- ========================================
            Checkout Layout
        ======================================== -->

      <div v-else class="checkout-layout">
        <!-- ========================================
              左側
          ======================================== -->

        <div class="checkout-left">
          <!-- ======================================
                商品
            ====================================== -->

          <section class="checkout-card">
            <!-- ======================================
      商品無法購買提示
  ====================================== -->

            <div v-if="stockErrorMessage" class="stock-error">
              <div class="stock-error-icon">
                <i class="bi bi-exclamation-triangle-fill"></i>
              </div>

              <div class="stock-error-content">
                <strong>部分商品目前無法購買</strong>

                <span>
                  {{ stockErrorMessage }}
                </span>

                <span class="stock-error-hint">
                  商品可能已下架或庫存不足，請返回購物車重新確認。
                </span>
              </div>
            </div>
            <div class="section-header">
              <h2>
                <i class="bi bi-bag"></i>
                訂購商品
              </h2>

              <span class="item-count"> {{ checkoutItems.length }} 項 </span>
            </div>

            <div class="checkout-items">
              <div
                v-for="item in checkoutItems"
                :key="item.cartItemId || item.skuId"
                class="checkout-item"
              >
                <!-- 商品圖片 -->

                <div class="item-image-wrapper">
                  <img
                    v-if="item.productImage"
                    :src="getImageUrl(item.productImage)"
                    :alt="item.productName"
                    class="item-image"
                  />

                  <div v-else class="image-placeholder">
                    <i class="bi bi-image"></i>
                  </div>
                </div>

                <!-- 商品資訊 -->

                <div class="item-info">
                  <h3>
                    {{ item.productName }}
                  </h3>

                  <div v-if="hasSkuOptions(item)" class="item-sku-select">
                    <label class="sku-label">規格</label>

                    <span class="sku-name">
                      {{
                        item.skus.find((sku) => Number(sku.skuId) === Number(item.skuId))?.skuName
                      }}
                    </span>
                  </div>

                  <span class="item-quantity"> 數量：{{ item.quantity }} </span>
                </div>

                <!-- 商品價格 -->

                <div class="item-price">
                  <span class="unit-price"> 單價 NT$ {{ formatPrice(item.price) }} </span>

                  <strong class="item-subtotal">
                    商品小計 NT$
                    {{ formatPrice(Number(item.price || 0) * Number(item.quantity || 0)) }}
                  </strong>
                </div>
              </div>
            </div>
          </section>
          <!-- ======================================
      訂單備註
  ====================================== -->

          <section class="checkout-card">
            <div class="section-header">
              <h2>
                <i class="bi bi-chat-left-text"></i>
                訂單備註
              </h2>
            </div>

            <div class="remark-wrapper">
              <textarea
                v-model="buyerRemark"
                class="remark-input"
                maxlength="500"
                rows="4"
                placeholder="有需要告訴賣家的事項嗎？例如：請下午送達、商品請小心包裝等"
              ></textarea>

              <div class="remark-footer">
                <span>選填</span>
                <span>{{ buyerRemark.length }}/500</span>
              </div>
            </div>
          </section>
          <!-- ======================================
                收件資料
            ====================================== -->

          <section class="checkout-card">
            <div class="section-header">
              <h2>
                <i class="bi bi-geo-alt"></i>
                收件資料
              </h2>
            </div>

            <!-- 沒有地址 -->

            <div v-if="addresses.length === 0" class="empty-address">
              <i class="bi bi-geo-alt"></i>

              <strong> 目前沒有收件地址 </strong>

              <span> 請先新增收件地址 </span>
            </div>

            <!-- 地址 -->

            <div v-else class="option-list">
              <label
                v-for="address in addresses"
                :key="address.addressId"
                class="option-card"
                :class="{
                  selected: selectedAddressId === address.addressId,
                }"
              >
                <input
                  v-model="selectedAddressId"
                  type="radio"
                  name="address"
                  :value="address.addressId"
                  @change="changeAddress"
                />

                <span class="radio-dot"></span>

                <span class="option-content">
                  <strong>
                    {{ address.receiverName }}
                  </strong>

                  <span>
                    {{ address.detailAddress }}
                  </span>

                  <span v-if="address.receiverPhone" class="option-phone">
                    {{ address.receiverPhone }}
                  </span>
                </span>

                <span v-if="address.isDefault" class="default-badge"> 預設 </span>
              </label>
            </div>
          </section>

          <!-- ======================================
                配送方式
            ====================================== -->

          <section class="checkout-card">
            <div class="section-header">
              <h2>
                <i class="bi bi-truck"></i>
                配送方式
              </h2>
            </div>

            <div class="option-list">
              <!-- 宅配 -->

              <label
                class="option-card"
                :class="{
                  selected: shippingMethod === 'HOME_DELIVERY',
                }"
              >
                <input
                  v-model="shippingMethod"
                  type="radio"
                  value="HOME_DELIVERY"
                  name="shipping"
                  @change="changeShippingMethod"
                />

                <span class="radio-dot"></span>

                <span class="option-content">
                  <strong> 宅配 </strong>

                  <span> 預計 2-3 個工作天 </span>
                </span>
              </label>

              <!-- 超商 -->

              <label class="option-card disabled" aria-disabled="true">
                <input type="radio" value="CONVENIENCE_STORE" name="shipping" disabled />

                <span class="radio-dot"></span>

                <span class="option-content">
                  <strong> 超商取貨 </strong>

                  <span> 即將開放 </span>
                </span>
              </label>
            </div>
          </section>

          <!-- ======================================
                付款方式
            ====================================== -->

          <section class="checkout-card">
            <div class="section-header">
              <h2>
                <i class="bi bi-credit-card"></i>
                付款方式
              </h2>
            </div>

            <div class="option-list">
              <!-- 貨到付款 -->

              <label
                class="option-card"
                :class="{
                  selected: paymentMethod === 'CASH_ON_DELIVERY',
                }"
              >
                <input
                  v-model="paymentMethod"
                  type="radio"
                  value="CASH_ON_DELIVERY"
                  name="payment"
                  :disabled="submitting"
                  @change="changePaymentMethod"
                />

                <span class="radio-dot"></span>

                <span class="option-content">
                  <strong>貨到付款</strong>

                  <span>收到商品時再付款</span>
                </span>
              </label>
              <!-- 信用卡 -->

              <label
                v-if="onlinePaymentAvailable"
                class="option-card"
                :class="{
                  selected: paymentMethod === 'CREDIT_CARD',
                }"
              >
                <input
                  v-model="paymentMethod"
                  type="radio"
                  value="CREDIT_CARD"
                  name="payment"
                  :disabled="submitting"
                  @change="changePaymentMethod"
                />

                <span class="radio-dot"></span>

                <span class="option-content">
                  <strong> 信用卡 </strong>

                  <span> 信用卡付款 </span>
                </span>
              </label>
            </div>
          </section>

          <!-- ======================================
                優惠券
            ====================================== -->

          <section class="checkout-card">
            <div class="section-header">
              <h2>
                <i class="bi bi-ticket-perforated"></i>
                優惠券
              </h2>

              <span v-if="coupons.length > 0" class="item-count">
                {{ coupons.length }} 張可用
              </span>
            </div>

            <!-- 優惠券載入中 -->

            <div v-if="couponLoading" class="coupon-loading">
              <span class="spinner-border spinner-border-sm" role="status"></span>

              正在取得可用優惠券...
            </div>

            <!-- 優惠券錯誤 -->

            <div v-else-if="couponErrorMessage" class="coupon-error">
              <i class="bi bi-exclamation-circle"></i>

              <span>
                {{ couponErrorMessage }}
              </span>

              <button type="button" class="coupon-retry-button" @click="loadCoupons">
                重新載入
              </button>
            </div>

            <!-- 沒有優惠券 -->

            <div v-else-if="coupons.length === 0" class="empty-coupon">
              <i class="bi bi-ticket-perforated"></i>

              <strong> 目前沒有可使用的優惠券 </strong>

              <span> 本次訂單將不使用優惠券 </span>
            </div>

            <!-- 優惠券 -->

            <div v-else class="coupon-list">
              <!-- 不使用優惠券 -->

              <label
                class="coupon-card"
                :class="{
                  selected: selectedMemberCouponId === null,
                }"
              >
                <input
                  v-model="selectedMemberCouponId"
                  type="radio"
                  name="coupon"
                  :value="null"
                  @change="changeCoupon"
                />

                <span class="coupon-radio"></span>

                <span class="coupon-content">
                  <strong> 不使用優惠券 </strong>

                  <span> 本次訂單不套用優惠 </span>
                </span>
              </label>

              <!-- 可使用優惠券 -->

              <label
                v-for="coupon in coupons"
                :key="coupon.memberCouponId"
                class="coupon-card"
                :class="{
                  selected:
                    selectedMemberCouponId === Number(coupon.memberCouponId) &&
                    isCouponAvailable(coupon),

                  disabled: !isCouponAvailable(coupon),
                }"
              >
                <input
                  v-model="selectedMemberCouponId"
                  type="radio"
                  name="coupon"
                  :value="Number(coupon.memberCouponId)"
                  :disabled="!isCouponAvailable(coupon)"
                  @change="changeCoupon"
                />

                <span class="coupon-radio"></span>

                <span class="coupon-content">
                  <!-- 優惠券名稱 -->
                  <strong>
                    {{ coupon.couponName || coupon.name || '優惠券' }}
                  </strong>

                  <!-- 優惠券代碼 -->
                  <span v-if="coupon.couponCode" class="coupon-code">
                    {{ coupon.couponCode }}
                  </span>

                  <!-- 所屬賣家 -->
                  <span class="coupon-seller">
                    <i class="bi bi-shop"></i>
                    {{ getCouponSellerName(coupon) }}
                  </span>

                  <!-- 折扣 -->
                  <span>
                    {{ getCouponDiscountText(coupon) }}
                  </span>

                  <!-- 最低消費 -->
                  <span v-if="coupon.minPurchaseAmount != null">
                    滿 NT$
                    {{ formatPrice(coupon.minPurchaseAmount) }}
                    使用
                  </span>

                  <!-- 有效期限 -->
                  <span v-if="coupon.expireDate">
                    有效期限：
                    {{ coupon.expireDate }}
                  </span>

                  <!-- 是否適用 -->
                  <span
                    class="coupon-availability"
                    :class="{
                      available: isCouponAvailable(coupon),
                      unavailable: !isCouponAvailable(coupon),
                    }"
                  >
                    <template v-if="isCouponAvailable(coupon)"> ✓ 可用於本次訂單 </template>

                    <template v-else>
                      {{ getCouponUnavailableReason(coupon) }}
                    </template>
                  </span>
                </span>

                <span
                  v-if="
                    selectedMemberCouponId === Number(coupon.memberCouponId) &&
                    isCouponAvailable(coupon)
                  "
                  class="coupon-selected-badge"
                >
                  已選
                </span>
              </label>
            </div>
          </section>
        </div>

        <!-- ========================================
              右側：訂單摘要
          ======================================== -->

        <aside class="summary-card">
          <div class="section-header">
            <h2>
              <i class="bi bi-receipt"></i>
              金額摘要
            </h2>
          </div>

          <!-- 商品小計 -->

          <div class="summary-row">
            <span> 商品小計 </span>

            <span>
              NT$
              {{ formatPrice(subtotal) }}
            </span>
          </div>

          <!-- 運費 -->

          <div class="summary-row">
            <span> 運費 </span>

            <span>
              NT$
              {{ formatPrice(shippingFee) }}
            </span>
          </div>

          <!-- 優惠券 -->

          <div v-if="selectedCoupon" class="summary-row coupon-summary">
            <span> 優惠券 </span>

            <span>
              {{ selectedCoupon.couponName || selectedCoupon.name || '優惠券' }}
            </span>
          </div>

          <!-- 折扣 -->

          <div v-if="discount > 0" class="summary-row discount">
            <span> 折扣 </span>

            <span>
              - NT$
              {{ formatPrice(discount) }}
            </span>
          </div>

          <div class="summary-divider"></div>

          <!-- 總額 -->

          <div class="summary-total">
            <span> 訂單總額 </span>

            <strong>
              NT$
              {{ formatPrice(totalAmount) }}
            </strong>
          </div>

          <!-- 更新金額 -->

          <div v-if="loading" class="summary-loading">
            <span class="spinner-border spinner-border-sm" role="status"></span>

            正在更新金額...
          </div>

          <!-- 錯誤 -->

          <div v-if="errorMessage" class="summary-error">
            <i class="bi bi-exclamation-circle"></i>

            {{ errorMessage }}
          </div>

          <!-- 送出訂單 -->

          <button
            v-if="stockErrorMessage"
            type="button"
            class="submit-button stock-error-button"
            @click="router.push('/cart')"
          >
            <i class="bi bi-cart3"></i>
            返回購物車重新選擇
          </button>

          <button
            v-else
            type="button"
            class="submit-button"
            :disabled="submitting || loading || !selectedAddressId || checkoutItems.length === 0"
            @click="submitOrder"
          >
            <span v-if="submitting">
              <span class="spinner-border spinner-border-sm" role="status"></span>

              建立訂單中...
            </span>

            <span v-else> 送出訂單 </span>
          </button>
        </aside>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ========================================
    Page
  ======================================== */

.checkout-page {
  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text);
}

.checkout-container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: var(--space-7) var(--space-6);
  box-sizing: border-box;
}

/* ========================================
    Header
  ======================================== */

.checkout-header {
  margin-bottom: var(--space-6);
}

.checkout-header h1 {
  margin: 0 0 var(--space-2);

  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: 1.3;
}

.checkout-header p {
  margin: 0;

  color: var(--color-text-muted);
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
}

/* ========================================
    Layout
  ======================================== */

.checkout-layout {
  display: grid;

  grid-template-columns:
    minmax(0, 1fr)
    360px;

  gap: var(--space-5);

  align-items: start;
}

/* ========================================
    Left
  ======================================== */

.checkout-left {
  display: flex;
  flex-direction: column;

  gap: var(--space-4);

  min-width: 0;
}

/* ========================================
    Card
  ======================================== */

.checkout-card,
.summary-card {
  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  box-sizing: border-box;
}

.checkout-card {
  padding: var(--space-5);
}

.summary-card {
  position: sticky;
  top: var(--space-5);

  padding: var(--space-5);
}

/* ========================================
    Section Header
  ======================================== */

.section-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: var(--space-4);
}

.section-header h2 {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.section-header h2 i {
  color: var(--color-primary);
  font-size: 20px;
}

.item-count {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

/* ========================================
    Checkout Items
  ======================================== */

.checkout-items {
  display: flex;
  flex-direction: column;

  gap: var(--space-3);
}

.checkout-item {
  display: grid;

  grid-template-columns:
    88px
    minmax(0, 1fr)
    auto;

  gap: var(--space-4);

  align-items: center;

  padding: var(--space-3);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface-soft);
}

.item-image-wrapper {
  width: 88px;
  height: 88px;

  overflow: hidden;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: var(--radius-md);

  background: var(--color-surface);
}

.item-image {
  width: 100%;
  height: 100%;

  display: block;

  object-fit: contain;
}

.image-placeholder {
  display: flex;

  align-items: center;
  justify-content: center;

  width: 100%;
  height: 100%;

  color: var(--color-text-subtle);

  font-size: 28px;
}

.item-info {
  min-width: 0;

  display: flex;
  flex-direction: column;

  gap: var(--space-1);
}

.item-info h3 {
  overflow: hidden;

  margin: 0;

  color: var(--color-text);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.item-sku,
.item-quantity {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

.item-price {
  display: flex;
  flex-direction: column;

  align-items: flex-end;

  gap: var(--space-1);

  white-space: nowrap;
}

.unit-price {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

.item-subtotal {
  color: var(--color-text);

  font-size: var(--font-size-base);
  font-weight: 700;
}

.item-price strong {
  color: var(--color-text);

  font-size: var(--font-size-base);
}

/* ========================================
    Option List
  ======================================== */

.option-list {
  display: flex;
  flex-direction: column;

  gap: var(--space-2);
}

.option-card {
  position: relative;

  display: flex;

  align-items: center;

  gap: var(--space-3);

  min-height: 64px;

  padding: var(--space-3);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface);

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.option-card:hover {
  background: var(--color-primary-soft);

  border-color: var(--color-primary);
}

.option-card:focus-within {
  box-shadow: var(--shadow-focus);
}

.option-card.selected {
  background: var(--color-primary-soft);

  border-color: var(--color-primary);
}

.option-card.disabled {
  background: var(--color-surface-soft);
  border-color: var(--color-border);
  cursor: not-allowed;
  opacity: 0.6;
}

.option-card.disabled:hover {
  background: var(--color-surface-soft);
  border-color: var(--color-border);
}

.option-card input {
  position: absolute;

  width: 1px;
  height: 1px;

  opacity: 0;
}

.radio-dot {
  position: relative;

  width: 18px;
  height: 18px;

  flex: 0 0 18px;

  border: 2px solid var(--color-border);

  border-radius: 50%;

  box-sizing: border-box;
}

.option-card.selected .radio-dot {
  border-color: var(--color-primary);
}

.option-card.selected .radio-dot::after {
  content: '';

  position: absolute;

  top: 3px;
  left: 3px;

  width: 8px;
  height: 8px;

  border-radius: 50%;

  background: var(--color-primary);
}

.option-content {
  display: flex;

  flex-direction: column;

  gap: var(--space-1);

  min-width: 0;
}

.option-content strong {
  color: var(--color-text);

  font-size: var(--font-size-sm);
  font-weight: 600;
}

.option-content span {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);

  line-height: 1.5;
}

.payment-simulation {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding: var(--space-3);
  color: var(--color-text-700);
  background: var(--color-warning-soft);
  border: 1px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.payment-simulation-title {
  margin: 0;
  color: var(--color-warning);
  font-weight: 700;
}

.payment-simulation label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
}

.payment-simulation input[type='radio'] {
  position: static;
  width: auto;
  height: auto;
  opacity: 1;
}

.payment-simulation .payment-failure-reason {
  align-items: stretch;
  flex-direction: column;
}

.payment-failure-reason input {
  min-height: 40px;
  padding: 0 var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
}

.payment-failure-reason input:focus-visible {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.option-phone {
  color: var(--color-text-subtle) !important;
}

.default-badge {
  margin-left: auto;

  padding: var(--space-1) var(--space-2);

  color: var(--color-primary);

  background: var(--color-primary-soft);

  border-radius: var(--radius-pill);

  font-size: var(--font-size-xs);
  white-space: nowrap;
}

/* ========================================
    優惠券
  ======================================== */

.coupon-list {
  display: flex;
  flex-direction: column;

  gap: var(--space-2);
}

.coupon-card {
  position: relative;

  display: flex;
  align-items: center;

  gap: var(--space-3);

  min-height: 70px;

  padding: var(--space-3);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  background: var(--color-surface);

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.coupon-card:hover {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.coupon-card.selected {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.coupon-card input {
  position: absolute;

  width: 1px;
  height: 1px;

  opacity: 0;
}

.coupon-radio {
  position: relative;

  width: 18px;
  height: 18px;

  flex: 0 0 18px;

  border: 2px solid var(--color-border);

  border-radius: 50%;
}

.coupon-card.selected .coupon-radio {
  border-color: var(--color-primary);
}

.coupon-card.selected .coupon-radio::after {
  content: '';

  position: absolute;

  top: 3px;
  left: 3px;

  width: 8px;
  height: 8px;

  border-radius: 50%;

  background: var(--color-primary);
}

.coupon-content {
  display: flex;
  flex-direction: column;

  gap: var(--space-1);

  min-width: 0;
}

.coupon-content strong {
  color: var(--color-text);

  font-size: var(--font-size-sm);
  font-weight: 600;
}

.coupon-content span {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

.coupon-code {
  color: var(--color-primary) !important;

  font-weight: 600;
}

.coupon-selected-badge {
  margin-left: auto;

  padding: var(--space-1) var(--space-2);

  color: var(--color-success);

  background: var(--color-primary-soft);

  border-radius: var(--radius-pill);

  font-size: var(--font-size-xs);

  white-space: nowrap;
}

.coupon-loading {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  padding: var(--space-4);

  color: var(--color-text-muted);

  font-size: var(--font-size-sm);
}

.coupon-loading .spinner-border {
  color: var(--color-primary);
}

.coupon-error {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  padding: var(--space-3);

  color: var(--color-danger);

  background: var(--color-surface-soft);

  border-radius: var(--radius-md);

  font-size: var(--font-size-xs);
}

.coupon-retry-button {
  margin-left: auto;

  padding: var(--space-1) var(--space-2);

  border: 1px solid var(--color-danger);
  border-radius: var(--radius-sm);

  color: var(--color-danger);

  background: var(--color-surface);

  font-size: var(--font-size-xs);

  cursor: pointer;
}

.empty-coupon {
  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: var(--space-6);

  color: var(--color-text-muted);

  text-align: center;
}

.empty-coupon i {
  color: var(--color-text-subtle);

  font-size: 32px;
}

.empty-coupon strong {
  color: var(--color-text);

  font-size: var(--font-size-base);
}

.empty-coupon span {
  font-size: var(--font-size-sm);
}

/* ========================================
    Empty Address
  ======================================== */

.empty-address {
  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: var(--space-6);

  color: var(--color-text-muted);

  text-align: center;
}

.empty-address i {
  color: var(--color-text-subtle);

  font-size: 32px;
}

.empty-address strong {
  color: var(--color-text);

  font-size: var(--font-size-base);
}

.empty-address span {
  font-size: var(--font-size-sm);
}

/* ========================================
    Summary
  ======================================== */

.summary-card {
  display: flex;
  flex-direction: column;
}

.summary-row {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: var(--space-4);

  color: var(--color-text-muted);

  font-size: var(--font-size-sm);
}

.summary-row span:last-child {
  color: var(--color-text);

  font-weight: 500;
}

.summary-row.discount span:last-child {
  color: var(--color-success);
}

.summary-row.coupon-summary span:last-child {
  max-width: 180px;

  overflow: hidden;

  color: var(--color-primary);

  text-align: right;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.summary-divider {
  width: 100%;
  height: 1px;

  margin: var(--space-2) 0 var(--space-4);

  background: var(--color-border);
}

.summary-total {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: var(--space-5);
}

.summary-total span {
  color: var(--color-text);

  font-size: var(--font-size-base);
  font-weight: 700;
}

.summary-total strong {
  color: var(--color-primary);

  font-size: var(--font-size-lg);
}

/* ========================================
    Loading
  ======================================== */

.loading-state {
  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  padding: var(--space-8);

  color: var(--color-text-muted);
}

.loading-state .spinner-border {
  color: var(--color-primary);
}

.loading-state p {
  margin: var(--space-3) 0 0;

  font-size: var(--font-size-sm);
}

.summary-loading {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  margin-bottom: var(--space-3);

  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

.summary-loading .spinner-border {
  color: var(--color-primary);
}

/* ========================================
    Error
  ======================================== */

.error-message {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  padding: var(--space-3);

  margin-bottom: var(--space-4);

  color: var(--color-danger);

  background: var(--color-surface-soft);

  border: 1px solid var(--color-danger);

  border-radius: var(--radius-md);

  font-size: var(--font-size-sm);
}

.error-message i {
  flex: 0 0 auto;
}

.summary-error {
  display: flex;

  align-items: center;

  gap: var(--space-2);

  padding: var(--space-2);

  margin-bottom: var(--space-3);

  color: var(--color-danger);

  background: var(--color-surface-soft);

  border-radius: var(--radius-sm);

  font-size: var(--font-size-xs);
}

/* ========================================
    Submit Button
  ======================================== */

.submit-button {
  width: 100%;

  min-height: 48px;

  display: flex;

  align-items: center;
  justify-content: center;

  gap: var(--space-2);

  padding: var(--space-3) var(--space-4);

  border: 0;
  border-radius: var(--radius-md);

  color: var(--color-surface);

  background: var(--color-primary);

  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.submit-button:hover:not(:disabled) {
  background: var(--color-primary-hover);

  transform: translateY(-1px);
}

.submit-button:active:not(:disabled) {
  background: var(--color-primary-active);

  transform: translateY(0);
}

.submit-button:focus-visible {
  outline: none;

  box-shadow: var(--shadow-focus);
}

.submit-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

/* ========================================
    RWD - 1024px
  ======================================== */

@media (max-width: 1024px) {
  .checkout-container {
    padding: var(--space-6) var(--space-5);
  }

  .checkout-layout {
    grid-template-columns:
      minmax(0, 1fr)
      320px;

    gap: var(--space-4);
  }
}

/* ========================================
    RWD - 768px
  ======================================== */

@media (max-width: 768px) {
  .checkout-container {
    padding: var(--space-5) var(--space-4);
  }

  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .summary-card {
    position: static;
  }

  .checkout-item {
    grid-template-columns:
      72px
      minmax(0, 1fr);
  }

  .item-image-wrapper {
    width: 72px;
    height: 72px;
  }

  .item-price {
    grid-column: 2;

    align-items: flex-start;

    padding-top: var(--space-1);
  }

  .coupon-selected-badge {
    display: none;
  }
}

/* ========================================
    RWD - 480px
  ======================================== */

@media (max-width: 480px) {
  .checkout-container {
    padding: var(--space-5) var(--space-3);
  }

  .checkout-header h1 {
    font-size: var(--font-size-lg);
  }

  .checkout-card,
  .summary-card {
    padding: var(--space-4);
  }

  .checkout-item {
    grid-template-columns:
      64px
      minmax(0, 1fr);

    gap: var(--space-3);
  }

  .item-image-wrapper {
    width: 64px;
    height: 64px;
  }

  .item-info h3 {
    font-size: var(--font-size-sm);
  }

  .option-card {
    min-height: 60px;

    padding: var(--space-3);
  }

  .default-badge {
    display: none;
  }

  .summary-total strong {
    font-size: var(--font-size-md);
  }

  .coupon-card {
    min-height: 64px;
  }

  .coupon-content strong {
    font-size: var(--font-size-xs);
  }
}
.item-sku-select {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.sku-label {
  color: var(--color-text-subtle);
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
}

.sku-name {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 500;
}
/* ========================================
    商品無法購買提示
  ======================================== */

.stock-error {
  display: flex;

  align-items: flex-start;

  gap: var(--space-3);

  margin-bottom: var(--space-4);

  padding: var(--space-4);

  color: var(--color-danger);

  background: var(--color-surface-soft);

  border: 1px solid var(--color-danger);

  border-radius: var(--radius-md);
}

.stock-error-icon {
  display: flex;

  align-items: center;
  justify-content: center;

  width: 36px;
  height: 36px;

  flex: 0 0 36px;

  color: var(--color-danger);

  font-size: 20px;
}

.stock-error-content {
  display: flex;

  flex-direction: column;

  gap: var(--space-1);

  min-width: 0;
}

.stock-error-content strong {
  color: var(--color-danger);

  font-size: var(--font-size-sm);

  font-weight: 700;
}

.stock-error-content span {
  color: var(--color-text);

  font-size: var(--font-size-sm);

  line-height: 1.5;
}

.stock-error-content .stock-error-hint {
  color: var(--color-text-muted);

  font-size: var(--font-size-xs);
}

/* ========================================
    庫存錯誤後的按鈕
  ======================================== */

.stock-error-button {
  background: var(--color-danger);
}

.stock-error-button:hover {
  background: var(--color-danger);

  transform: translateY(-1px);
}
/* ========================================
    訂單備註
  ======================================== */

.remark-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.remark-input {
  width: 100%;
  min-height: 100px;

  padding: var(--space-3);

  box-sizing: border-box;

  color: var(--color-text);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  font-family: var(--font-body);
  font-size: var(--font-size-sm);

  line-height: 1.6;

  resize: vertical;

  outline: none;

  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.remark-input::placeholder {
  color: var(--color-text-subtle);
}

.remark-input:focus {
  border-color: var(--color-primary);

  box-shadow: var(--shadow-focus);
}

.remark-footer {
  display: flex;

  justify-content: space-between;

  color: var(--color-text-subtle);

  font-size: var(--font-size-xs);
}
/* ========================================
    優惠券所屬賣家
  ======================================== */

.coupon-seller {
  display: flex;
  align-items: center;
  gap: 4px;

  color: var(--color-text-subtle) !important;

  font-size: var(--font-size-xs);
}

.coupon-seller i {
  font-size: 12px;
}

/* ========================================
    優惠券可用狀態
  ======================================== */

.coupon-availability {
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.coupon-availability.available {
  color: var(--color-success) !important;
}

.coupon-availability.unavailable {
  color: var(--color-text-subtle) !important;
}

/* ========================================
    其他賣家優惠券
  ======================================== */

.coupon-card.disabled {
  background: var(--color-surface-soft);

  border-color: var(--color-border);

  opacity: 0.5;

  cursor: not-allowed;
}

.coupon-card.disabled:hover {
  background: var(--color-surface-soft);

  border-color: var(--color-border);
}

.coupon-card.disabled .coupon-radio {
  border-color: var(--color-border);
}

.coupon-card.disabled .coupon-content strong {
  color: var(--color-text-muted);
}
</style>
