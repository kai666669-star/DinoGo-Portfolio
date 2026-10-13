<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import flatpickr from 'flatpickr'
import { MandarinTraditional } from 'flatpickr/dist/l10n/zh-tw'
import { Line } from 'vue-chartjs'
import 'flatpickr/dist/flatpickr.css'
import '@/assets/styles/seller-flatpickr.css'
import {
  createSellerCoupon,
  disableSellerCoupon,
  getSellerCoupons,
  updateSellerCoupon,
} from '@/api/sellerCouponApi'
import { getSellerProducts } from '@/api/sellerProductApi'
import { getSellerOrders } from '@/api/sellerOrderApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import {
  createSellerChartPalette,
  createSellerLineChartOptions,
  createSellerLineDataset,
  formatFullDateLabel,
  registerSellerLineChart,
} from './useSellerLineChart'

registerSellerLineChart()

const sellerId = computed(() => getCurrentSellerId())
const selectedStatus = ref('ALL')
const searchKeyword = ref('')
const message = ref('')
const formError = ref('')
const isLoadingCoupons = ref(false)
const isSubmitting = ref(false)
const sortState = ref({ key: '', direction: 'asc' })
const isCreateDrawerOpen = ref(false)
const selectedCouponType = ref('賣場優惠券')
const createDiscountType = ref('AMOUNT')
const createDiscountUnit = computed(() => (createDiscountType.value === 'PERCENT' ? '%' : 'NT$'))
const editingCouponId = ref(null)
const sellerProducts = ref([])
const sellerOrders = ref([])
const performanceStartDate = ref('')
const performanceEndDate = ref('')
const performanceRange = ref('7')
const performanceStartInput = ref(null)
const performanceEndInput = ref(null)
const chartPalette = ref(createSellerChartPalette())
let performanceStartPicker = null
let performanceEndPicker = null
const isProductPickerOpen = ref(false)
const startAtInput = ref(null)
const endAtInput = ref(null)
let startAtPicker = null
let endAtPicker = null

const fallbackProducts = [
  { productId: 101, productName: '恐龍造型保溫杯', basePrice: 300, status: 'ACTIVE' },
  { productId: 102, productName: 'DINO-GO 經典帆布袋', basePrice: 1000, status: 'ACTIVE' },
  { productId: 103, productName: '森日選物香氛蠟燭', basePrice: 650, status: 'ACTIVE' },
  { productId: 104, productName: '露營折疊收納箱', basePrice: 1200, status: 'ACTIVE' },
]

const couponForm = reactive({
  couponName: '',
  couponCode: '',
  discountType: 'AMOUNT',
  discountValue: '',
  minPurchaseAmount: '',
  startAt: '',
  endAt: '',
  limitCount: '',
  perMemberUsagePolicy: 'ONCE',
  productId: '',
})

const fallbackCoupons = [
  {
    couponId: 1,
    couponCode: 'AUG25-120',
    couponName: '八月新品滿額折抵',
    type: '賣場優惠券',
    productScope: '所有商品',
    discountType: 'AMOUNT',
    discountValue: 120,
    minPurchaseAmount: 999,
    startAt: '2026-08-25T10:00:00',
    endAt: '2026-09-08T23:59:00',
    limitCount: 300,
    usedCount: 18,
    status: 'ACTIVE',
  },
  {
    couponId: 2,
    couponCode: 'SUMMER88',
    couponName: '夏末精選 88 折',
    type: '商品優惠券',
    productId: 101,
    discountType: 'PERCENT',
    discountValue: 12,
    minPurchaseAmount: 1500,
    startAt: '2026-08-28T12:00:00',
    endAt: '2026-09-15T23:59:00',
    limitCount: 180,
    usedCount: 7,
    status: 'ACTIVE',
  },
  {
    couponId: 3,
    couponCode: 'HOME500',
    couponName: '居家大物折五百',
    type: '賣場優惠券',
    productScope: '所有商品',
    discountType: 'AMOUNT',
    discountValue: 500,
    minPurchaseAmount: 5000,
    startAt: '2026-09-01T09:00:00',
    endAt: '2026-09-30T23:59:00',
    limitCount: 80,
    usedCount: 2,
    status: 'ACTIVE',
  },
  {
    couponId: 4,
    couponCode: 'SNACK95',
    couponName: '零食補貨 95 折',
    type: '商品優惠券',
    productId: 102,
    discountType: 'PERCENT',
    discountValue: 5,
    minPurchaseAmount: 699,
    startAt: '2026-09-03T08:00:00',
    endAt: '2026-09-10T23:59:00',
    limitCount: 500,
    usedCount: 46,
    status: 'ACTIVE',
  },
  {
    couponId: 5,
    couponCode: 'VIP900',
    couponName: 'VIP 專屬折抵 900',
    type: '賣場優惠券',
    productScope: '所有商品',
    discountType: 'AMOUNT',
    discountValue: 900,
    minPurchaseAmount: 8000,
    startAt: '2026-09-06T00:00:00',
    endAt: '2026-10-06T23:59:00',
    limitCount: 50,
    usedCount: 4,
    status: 'ACTIVE',
  },
  {
    couponId: 6,
    couponCode: 'MIDSEP92',
    couponName: '九月中旬會員 92 折',
    type: '賣場優惠券',
    productScope: '所有商品',
    discountType: 'PERCENT',
    discountValue: 8,
    minPurchaseAmount: 2200,
    startAt: '2026-09-12T10:30:00',
    endAt: '2026-09-22T23:59:00',
    limitCount: 220,
    usedCount: 0,
    status: 'DRAFT',
  },
  {
    couponId: 7,
    couponCode: 'FALL250',
    couponName: '初秋選物折 250',
    type: '賣場優惠券',
    productScope: '所有商品',
    discountType: 'AMOUNT',
    discountValue: 250,
    minPurchaseAmount: 1800,
    startAt: '2026-09-20T11:00:00',
    endAt: '2026-10-12T23:59:00',
    limitCount: 160,
    usedCount: 0,
    status: 'DRAFT',
  },
  {
    couponId: 8,
    couponCode: 'SELECT85',
    couponName: '精選商品 85 折',
    type: '商品優惠券',
    productId: 103,
    discountType: 'PERCENT',
    discountValue: 15,
    minPurchaseAmount: 1200,
    startAt: '2026-09-26T14:00:00',
    endAt: '2026-10-20T23:59:00',
    limitCount: 90,
    usedCount: 0,
    status: 'ACTIVE',
  },
  {
    couponId: 9,
    couponCode: 'FLASH80',
    couponName: '限時快閃折 80',
    type: '商品優惠券',
    productId: 104,
    discountType: 'AMOUNT',
    discountValue: 80,
    minPurchaseAmount: 599,
    startAt: '2026-08-20T10:00:00',
    endAt: '2026-08-31T23:59:00',
    limitCount: 1000,
    usedCount: 132,
    status: 'DISABLED',
  },
  {
    couponId: 10,
    couponCode: 'SEASON90',
    couponName: '換季結清 9 折',
    type: '賣場優惠券',
    productScope: '換季商品',
    discountType: 'PERCENT',
    discountValue: 10,
    minPurchaseAmount: 1000,
    startAt: '2026-07-15T09:00:00',
    endAt: '2026-08-15T23:59:00',
    limitCount: 120,
    usedCount: 120,
    status: 'EXPIRED',
  },
]

const coupons = ref([])

const toDateInput = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const initializePerformanceDateRange = () => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - 29)
  performanceStartDate.value = toDateInput(start)
  performanceEndDate.value = toDateInput(today)
}

const creationCards = [
  {
    title: '賣場優惠券',
    description: '適用賣場所有商品，可有效提升全店銷售額',
    icon: 'bi-shop-window',
  },
  {
    title: '商品優惠券',
    description: '適用指定商品，可設定特定商品導購活動',
    icon: 'bi-bag-check',
  },
]

const productOptions = computed(() => {
  const activeProducts = sellerProducts.value.filter((product) => product.status === 'ACTIVE')
  return activeProducts.length > 0 ? activeProducts : fallbackProducts
})

const drawerTitle = computed(() => (editingCouponId.value ? '編輯優惠券' : '建立優惠券'))
const drawerActionText = computed(() => (editingCouponId.value ? '儲存修改' : '建立'))
const selectedProduct = computed(() =>
  productOptions.value.find(
    (product) => String(product.productId) === String(couponForm.productId),
  ),
)

const normalizeCoupon = (coupon) => ({
  couponId: coupon.couponId,
  sellerId: coupon.sellerId,
  couponCode: coupon.couponCode,
  couponName: coupon.couponName,
  type: coupon.scopeType === 'PRODUCT' ? '商品優惠券' : '賣場優惠券',
  productScope: coupon.scopeType === 'PRODUCT' ? undefined : '所有商品',
  productId: coupon.productId,
  discountType: coupon.discountType,
  discountValue: Number(coupon.discountValue ?? 0),
  minPurchaseAmount: Number(coupon.minPurchaseAmount ?? 0),
  startAt: coupon.startAt,
  endAt: coupon.endAt,
  limitCount: Number(coupon.limitCount ?? 0),
  usedCount: Number(coupon.usedCount ?? 0),
  perMemberUsagePolicy: coupon.perMemberUsagePolicy || 'ONCE',
  status: coupon.status,
})

const getFallbackCoupons = () => {
  const products = productOptions.value
  let productIndex = 0

  return fallbackCoupons.map((coupon) => {
    if (coupon.type !== '商品優惠券') return { ...coupon }
    const product = products[productIndex % products.length]
    productIndex += 1
    return { ...coupon, productId: product?.productId || coupon.productId }
  })
}

const metrics = computed(() => {
  const paidStatuses = ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED']
  const validOrders = sellerOrders.value.filter((order) => paidStatuses.includes(order.status))
  const couponOrders = validOrders.filter((order) => Number(order.discountAmount) > 0)
  const salesTotal = couponOrders.reduce((sum, order) => sum + Number(order.totalAmount), 0)
  const usageRate = validOrders.length === 0 ? 0 : (couponOrders.length / validOrders.length) * 100
  const buyerTotal = new Set(couponOrders.map((order) => order.buyerId)).size

  return [
    {
      label: '優惠券訂單銷售額',
      value: formatCurrency(salesTotal),
      compare: '依訂單金額加總',
      note: '統計期間內，使用優惠券且完成付款的訂單總金額。',
    },
    {
      label: '優惠券訂單數',
      value: String(couponOrders.length),
      compare: '使用優惠券完成付款',
      note: '統計期間內，使用優惠券完成付款的訂單筆數。',
    },
    {
      label: '優惠券使用率',
      value: `${usageRate.toFixed(2)}%`,
      compare: '優惠券訂單 / 有效訂單',
      note: '使用優惠券的已付款訂單數除以全部已付款訂單數；待付款與取消訂單不列入。',
    },
    {
      label: '使用買家數',
      value: String(buyerTotal),
      compare: '依訂單買家去重',
      note: '統計期間內，曾使用優惠券完成付款的不重複買家數。',
    },
  ]
})

const performanceChartData = computed(() => {
  const labels = []
  const salesByDate = new Map()
  const start = new Date(`${performanceStartDate.value}T00:00:00`)
  const end = new Date(`${performanceEndDate.value}T00:00:00`)

  for (const date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
    const dateKey = toDateInput(date)
    labels.push(dateKey)
    salesByDate.set(dateKey, 0)
  }

  const paidStatuses = ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED']
  sellerOrders.value
    .filter((order) => paidStatuses.includes(order.status) && Number(order.discountAmount) > 0)
    .forEach((order) => {
      const dateKey = order.createdAt?.slice(0, 10)
      if (salesByDate.has(dateKey)) {
        salesByDate.set(dateKey, salesByDate.get(dateKey) + Number(order.totalAmount || 0))
      }
    })

  return {
    labels,
    datasets: [
      createSellerLineDataset(
        { key: 'sales', label: '優惠券訂單銷售額' },
        [...salesByDate.values()],
        chartPalette.value,
        true,
      ),
    ],
  }
})

const performanceChartOptions = computed(() =>
  createSellerLineChartOptions({
    palette: chartPalette.value,
    formatTooltipTitle: formatFullDateLabel,
    formatValue: formatCurrency,
  }),
)

const performanceRangeLabel = computed(
  () =>
    `${performanceStartDate.value.replaceAll('-', '/')} - ${performanceEndDate.value.replaceAll('-', '/')}`,
)

const statusTabs = computed(() => [
  { label: '全部', value: 'ALL', count: coupons.value.length },
  {
    label: '進行中',
    value: 'ACTIVE',
    count: coupons.value.filter((coupon) => displayStatus(coupon) === '進行中').length,
  },
  {
    label: '接下來的活動',
    value: 'DRAFT',
    count: coupons.value.filter((coupon) => displayStatus(coupon) === '接下來').length,
  },
  {
    label: '已結束',
    value: 'ENDED',
    count: coupons.value.filter((coupon) => ['已結束', '已取消'].includes(displayStatus(coupon)))
      .length,
  },
])

const filteredCoupons = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  return coupons.value.filter((coupon) => {
    const currentStatus = displayStatus(coupon)
    const matchesStatus =
      selectedStatus.value === 'ALL' ||
      (selectedStatus.value === 'ACTIVE' && currentStatus === '進行中') ||
      (selectedStatus.value === 'DRAFT' && currentStatus === '接下來') ||
      (selectedStatus.value === 'ENDED' && ['已結束', '已取消'].includes(currentStatus))
    const matchesKeyword =
      !keyword ||
      coupon.couponCode.toLowerCase().includes(keyword) ||
      coupon.couponName.toLowerCase().includes(keyword)

    return matchesStatus && matchesKeyword
  })
})

const statusSortOrder = { ACTIVE: 1, DRAFT: 2, DISABLED: 3, EXPIRED: 4 }

const sortedCoupons = computed(() => {
  const direction = sortState.value.direction === 'desc' ? -1 : 1
  const list = [...filteredCoupons.value]

  if (sortState.value.key === 'period') {
    return list.sort(
      (left, right) =>
        (new Date(left.startAt).getTime() - new Date(right.startAt).getTime()) * direction,
    )
  }

  if (sortState.value.key === 'status') {
    return list.sort((left, right) => {
      const statusDiff = statusSortOrder[left.status] - statusSortOrder[right.status]
      if (statusDiff !== 0) return statusDiff * direction
      return (left.usedCount - right.usedCount) * direction
    })
  }

  return list
})

const formatCurrency = (value) => `NT$${Number(value).toLocaleString()}`

const discountText = (coupon) =>
  coupon.discountType === 'PERCENT'
    ? `${Number(coupon.discountValue)}% 折扣`
    : `折 ${formatCurrency(coupon.discountValue)}`

const productScopeText = (coupon) => {
  if (coupon.type === '賣場優惠券') return '所有商品'
  const product = productOptions.value.find(
    (item) => Number(item.productId) === Number(coupon.productId),
  )
  return product?.productName || '尚未選擇商品'
}

const perMemberUsageText = (coupon) =>
  coupon.perMemberUsagePolicy === 'REPEAT' ? '可重複使用' : '每人限用 1 次'

const formatDateTime = (value) =>
  new Date(value).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })

const statusClass = (status) => {
  if (status === '進行中') return 'is-active'
  if (status === '接下來') return 'is-draft'
  return 'is-ended'
}

const displayStatus = (coupon) => {
  if (coupon.status === 'DISABLED') return '已取消'
  const now = Date.now()
  if (new Date(coupon.endAt).getTime() <= now) return '已結束'
  if (new Date(coupon.startAt).getTime() > now) return '接下來'
  return '進行中'
}

const toggleSort = (key) => {
  if (sortState.value.key === key) {
    sortState.value.direction = sortState.value.direction === 'asc' ? 'desc' : 'asc'
    return
  }

  sortState.value = { key, direction: 'asc' }
}

const sortIcon = (key) => {
  if (sortState.value.key !== key) return 'bi-arrow-down-up'
  return sortState.value.direction === 'asc' ? 'bi-sort-up' : 'bi-sort-down'
}

const fakeAction = (action, couponName = '') => {
  message.value = couponName ? `${couponName}：已開啟${action}。` : `已開啟${action}。`
}

const chooseProduct = (product) => {
  couponForm.productId = String(product.productId)
  isProductPickerOpen.value = false
}

const toInputDateTime = (value) => value?.slice(0, 16) || ''

const destroyCouponDateTimePickers = () => {
  startAtPicker?.destroy()
  endAtPicker?.destroy()
  startAtPicker = null
  endAtPicker = null
}

const createCouponDateTimePicker = (input, field, calendarClass) =>
  flatpickr(input, {
    altInput: true,
    altInputClass: 'seller-flatpickr-input coupon-date-time-input',
    altFormat: 'Y/m/d H:i',
    allowInput: false,
    dateFormat: 'Y-m-d\\TH:i',
    defaultDate: couponForm[field] || null,
    disableMobile: true,
    enableTime: true,
    locale: MandarinTraditional,
    minuteIncrement: 1,
    monthSelectorType: 'dropdown',
    time_24hr: true,
    onChange: (_, dateString) => {
      couponForm[field] = dateString
    },
    onReady: (_, __, instance) => {
      instance.calendarContainer.classList.add('seller-module-flatpickr', calendarClass)
    },
  })

const initializeCouponDateTimePickers = async () => {
  await nextTick()
  destroyCouponDateTimePickers()
  if (startAtInput.value) {
    startAtPicker = createCouponDateTimePicker(
      startAtInput.value,
      'startAt',
      'seller-coupon-start-calendar',
    )
  }
  if (endAtInput.value) {
    endAtPicker = createCouponDateTimePicker(
      endAtInput.value,
      'endAt',
      'seller-coupon-end-calendar',
    )
  }
}

const buildCouponPayload = () => {
  const isProductCoupon = selectedCouponType.value === '商品優惠券'
  const discountValue = Number(couponForm.discountValue)
  const minPurchaseAmount =
    couponForm.minPurchaseAmount === '' ? null : Number(couponForm.minPurchaseAmount)
  const limitCount = couponForm.limitCount === '' ? null : Number(couponForm.limitCount)

  return {
    couponCode: couponForm.couponCode.trim(),
    couponName: couponForm.couponName.trim(),
    discountType: createDiscountType.value,
    discountValue,
    minPurchaseAmount,
    startAt: couponForm.startAt ? `${couponForm.startAt}:00` : '',
    endAt: couponForm.endAt ? `${couponForm.endAt}:00` : '',
    limitCount,
    perMemberUsagePolicy: couponForm.perMemberUsagePolicy,
    scopeType: isProductCoupon ? 'PRODUCT' : 'STORE',
    categoryId: null,
    productId: isProductCoupon ? Number(couponForm.productId) : null,
  }
}

const validateForm = () => {
  if (!couponForm.couponName.trim()) return '請輸入優惠券名稱。'
  if (!editingCouponId.value && !couponForm.couponCode.trim()) return '請輸入優惠券代碼。'
  if (selectedCouponType.value === '商品優惠券' && !couponForm.productId) return '請選擇適用商品。'

  const discountValue = Number(couponForm.discountValue)
  if (!Number.isFinite(discountValue) || discountValue <= 0) return '折扣額度必須大於 0。'
  if (createDiscountType.value === 'PERCENT' && discountValue > 100)
    return '百分比折扣不可超過 100%。'

  if (createDiscountType.value === 'AMOUNT' && selectedCouponType.value === '商品優惠券') {
    const productPrice = Number(
      selectedProduct.value?.minSkuPrice ?? selectedProduct.value?.basePrice,
    )
    if (!Number.isFinite(productPrice)) return '目前無法取得商品價格，請稍後再試。'
    if (discountValue >= productPrice) return '固定折扣金額必須小於商品價格。'
  }

  if (couponForm.minPurchaseAmount !== '') {
    const minPurchaseAmount = Number(couponForm.minPurchaseAmount)
    if (!Number.isFinite(minPurchaseAmount) || minPurchaseAmount < 0) return '使用門檻不可小於 0。'
  }

  if (couponForm.limitCount !== '') {
    const limitCount = Number(couponForm.limitCount)
    if (!Number.isInteger(limitCount) || limitCount <= 0) return '可使用數量必須為大於 0 的整數。'
  }

  if (!couponForm.startAt || !couponForm.endAt) return '請設定開始與結束時間。'
  if (new Date(couponForm.startAt) < new Date()) return '開始時間不可早於目前時間。'
  if (new Date(couponForm.endAt) <= new Date(couponForm.startAt))
    return '結束時間必須晚於開始時間。'

  return ''
}

const resetForm = (type = '賣場優惠券') => {
  formError.value = ''
  couponForm.couponName = ''
  couponForm.couponCode = ''
  couponForm.discountType = 'AMOUNT'
  couponForm.discountValue = ''
  couponForm.minPurchaseAmount = ''
  couponForm.startAt = ''
  couponForm.endAt = ''
  couponForm.limitCount = ''
  couponForm.perMemberUsagePolicy = 'ONCE'
  couponForm.productId =
    type === '商品優惠券' ? String(productOptions.value[0]?.productId || '') : ''
  createDiscountType.value = 'AMOUNT'
}

const openCreateDrawer = (type) => {
  selectedCouponType.value = type
  editingCouponId.value = null
  resetForm(type)
  isCreateDrawerOpen.value = true
  isProductPickerOpen.value = false
  message.value = ''
}

const editCoupon = (coupon) => {
  selectedCouponType.value = coupon.type
  editingCouponId.value = coupon.couponId
  couponForm.couponName = coupon.couponName
  couponForm.couponCode = coupon.couponCode
  couponForm.discountType = coupon.discountType
  couponForm.discountValue = String(coupon.discountValue)
  couponForm.minPurchaseAmount = String(coupon.minPurchaseAmount)
  couponForm.startAt = toInputDateTime(coupon.startAt)
  couponForm.endAt = toInputDateTime(coupon.endAt)
  couponForm.limitCount = String(coupon.limitCount)
  couponForm.perMemberUsagePolicy = coupon.perMemberUsagePolicy || 'ONCE'
  couponForm.productId = coupon.productId ? String(coupon.productId) : ''
  createDiscountType.value = coupon.discountType
  isCreateDrawerOpen.value = true
  isProductPickerOpen.value = false
  message.value = ''
  formError.value = ''
  void initializeCouponDateTimePickers()
}

const loadCoupons = async () => {
  if (!sellerId.value) {
    coupons.value = getFallbackCoupons()
    message.value = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'
    return
  }

  isLoadingCoupons.value = true
  try {
    const response = await getSellerCoupons(sellerId.value)
    const apiCoupons = Array.isArray(response.data) ? response.data.map(normalizeCoupon) : []
    coupons.value = apiCoupons.length > 0 ? apiCoupons : getFallbackCoupons()
  } catch (error) {
    coupons.value = getFallbackCoupons()
    message.value = '優惠券資料載入失敗，請確認後端 API 是否已啟動。'
  } finally {
    isLoadingCoupons.value = false
  }
}

const saveCoupon = async () => {
  const validationMessage = validateForm()
  if (validationMessage) {
    formError.value = validationMessage
    return
  }
  if (!sellerId.value) {
    formError.value = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'
    return
  }

  const payload = buildCouponPayload()
  const updatePayload = { ...payload }
  delete updatePayload.couponCode

  isSubmitting.value = true
  formError.value = ''
  try {
    if (editingCouponId.value) {
      const response = await updateSellerCoupon(
        sellerId.value,
        editingCouponId.value,
        updatePayload,
      )
      const updatedCoupon = normalizeCoupon(response.data)
      coupons.value = coupons.value.map((coupon) =>
        coupon.couponId === updatedCoupon.couponId ? updatedCoupon : coupon,
      )
      message.value = `${updatedCoupon.couponName}：已儲存修改。`
    } else {
      const response = await createSellerCoupon(sellerId.value, payload)
      const createdCoupon = normalizeCoupon(response.data)
      coupons.value = [createdCoupon, ...coupons.value]
      message.value = `${createdCoupon.couponName}：已建立。`
    }
    isCreateDrawerOpen.value = false
    editingCouponId.value = null
  } catch (error) {
    formError.value = error.response?.data?.message || '優惠券儲存失敗，請確認欄位內容。'
  } finally {
    isSubmitting.value = false
  }
}

const cancelCoupon = async (coupon) => {
  if (!sellerId.value) {
    message.value = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'
    return
  }

  try {
    const response = await disableSellerCoupon(sellerId.value, coupon.couponId)
    const updatedCoupon = normalizeCoupon(response.data)
    coupons.value = coupons.value.map((item) =>
      item.couponId === updatedCoupon.couponId ? updatedCoupon : item,
    )
    message.value = `${updatedCoupon.couponName}：已取消活動。`
  } catch (error) {
    message.value = '取消活動失敗，請稍後再試。'
  }
}

const loadSellerProducts = async () => {
  const sellerId = getCurrentSellerId()
  if (!sellerId) {
    sellerProducts.value = fallbackProducts
    return
  }

  try {
    const response = await getSellerProducts(sellerId)
    sellerProducts.value = Array.isArray(response.data) ? response.data : fallbackProducts
  } catch (error) {
    sellerProducts.value = fallbackProducts
  }
}

const loadSellerOrders = async () => {
  try {
    const response = await getSellerOrders({
      startDate: performanceStartDate.value,
      endDate: performanceEndDate.value,
    })
    sellerOrders.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    sellerOrders.value = []
  }
}

const applyPerformanceDateRange = async () => {
  if (!performanceStartDate.value || !performanceEndDate.value) return
  if (performanceEndDate.value < performanceStartDate.value) {
    message.value = '結束日期不可早於開始日期。'
    return
  }
  message.value = ''
  await loadSellerOrders()
}

const selectPerformancePreset = async (preset) => {
  const end = new Date()
  const start = new Date(end)
  if (preset === 'day') start.setDate(end.getDate())
  if (preset === '7') start.setDate(end.getDate() - 6)
  if (preset === '30') start.setDate(end.getDate() - 29)
  performanceRange.value = preset
  performanceStartDate.value = toDateInput(start)
  performanceEndDate.value = toDateInput(end)
  performanceStartPicker?.setDate(performanceStartDate.value, false)
  performanceEndPicker?.setDate(performanceEndDate.value, false)
  await applyPerformanceDateRange()
}

const createPerformanceDatePicker = (input, field, defaultDate) =>
  flatpickr(input, {
    altInput: true,
    altInputClass: 'seller-flatpickr-input coupon-date-input',
    altFormat: 'Y/m/d',
    dateFormat: 'Y-m-d',
    defaultDate,
    disableMobile: true,
    locale: MandarinTraditional,
    onChange: (_, dateString) => {
      performanceRange.value = 'custom'
      if (field === 'start') {
        performanceStartDate.value = dateString
        performanceEndPicker?.set('minDate', dateString)
      }
      if (field === 'end') {
        performanceEndDate.value = dateString
        performanceStartPicker?.set('maxDate', dateString)
      }
      void applyPerformanceDateRange()
    },
    onReady: (_, __, instance) =>
      instance.calendarContainer.classList.add('seller-module-flatpickr'),
  })

const initializePerformanceDatePickers = () => {
  performanceStartPicker?.destroy()
  performanceEndPicker?.destroy()
  performanceStartPicker = performanceStartInput.value
    ? createPerformanceDatePicker(performanceStartInput.value, 'start', performanceStartDate.value)
    : null
  performanceEndPicker = performanceEndInput.value
    ? createPerformanceDatePicker(performanceEndInput.value, 'end', performanceEndDate.value)
    : null
  performanceEndPicker?.set('minDate', performanceStartDate.value)
  performanceStartPicker?.set('maxDate', performanceEndDate.value)
}

onMounted(() => {
  chartPalette.value = createSellerChartPalette()
  initializePerformanceDateRange()
  void nextTick(initializePerformanceDatePickers)
  void loadSellerProducts()
  void loadCoupons()
  void loadSellerOrders()
})

watch(isCreateDrawerOpen, (isOpen) => {
  if (isOpen) {
    void initializeCouponDateTimePickers()
    return
  }

  destroyCouponDateTimePickers()
})

onUnmounted(() => {
  destroyCouponDateTimePickers()
  performanceStartPicker?.destroy()
  performanceEndPicker?.destroy()
})
</script>

<template>
  <section class="seller-page coupon-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">優惠券管理</p>
        <h1>優惠券管理</h1>
        <p class="page-description">建立、追蹤與管理店鋪優惠券，掌握買家使用狀態。</p>
      </div>
    </header>

    <section class="creation-panel">
      <div class="section-heading">
        <div>
          <h1>建立優惠券</h1>
          <p>建立優惠券可吸引買家下單。</p>
        </div>
      </div>

      <div>
        <p class="group-title">改善整體轉換率</p>
        <div class="creation-grid">
          <article v-for="card in creationCards" :key="card.title" class="creation-card">
            <i class="bi" :class="card.icon" aria-hidden="true"></i>
            <div>
              <strong>{{ card.title }}</strong>
              <span>{{ card.description }}</span>
            </div>
            <button type="button" @click="openCreateDrawer(card.title)">建立</button>
          </article>
        </div>
      </div>
    </section>

    <section class="performance-panel">
      <div class="section-heading compact">
        <h2>優惠券表現</h2>
        <button type="button" class="link-button" @click="fakeAction('查看更多')">查看更多</button>
      </div>

      <div class="performance-filter" aria-label="優惠券表現日期篩選">
        <div class="performance-presets">
          <button
            type="button"
            :class="{ active: performanceRange === 'day' }"
            @click="selectPerformancePreset('day')"
          >
            當天
          </button>
          <button
            type="button"
            :class="{ active: performanceRange === '7' }"
            @click="selectPerformancePreset('7')"
          >
            近 7 天
          </button>
          <button
            type="button"
            :class="{ active: performanceRange === '30' }"
            @click="selectPerformancePreset('30')"
          >
            近 30 天
          </button>
        </div>
        <div class="performance-dates">
          <label>
            開始日期
            <input ref="performanceStartInput" type="text" />
          </label>
          <span aria-hidden="true">至</span>
          <label>
            結束日期
            <input ref="performanceEndInput" type="text" />
          </label>
        </div>
      </div>

      <div class="metric-grid">
        <article v-for="metric in metrics" :key="metric.label" class="metric-card">
          <span class="metric-label">
            {{ metric.label }}
            <span class="metric-info" :data-tooltip="metric.note" tabindex="0">
              <i class="bi bi-info-circle" aria-hidden="true"></i>
            </span>
          </span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.compare }}</small>
        </article>
      </div>

      <div class="performance-chart">
        <Line :data="performanceChartData" :options="performanceChartOptions" />
      </div>
    </section>

    <section class="coupon-panel">
      <h2>我的優惠券</h2>

      <div class="status-tabs" aria-label="優惠券狀態篩選">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          :class="{ active: selectedStatus === tab.value }"
          @click="selectedStatus = tab.value"
        >
          {{ tab.label }}
          <span>{{ tab.count }}</span>
        </button>
      </div>

      <div class="coupon-toolbar">
        <label class="search-field">
          <span>搜尋</span>
          <input v-model="searchKeyword" type="search" placeholder="優惠券名稱或優惠券代碼" />
        </label>
        <button type="button" @click="fakeAction('搜尋')">搜尋</button>
      </div>

      <p v-if="message" class="notice-message">{{ message }}</p>
      <p v-if="isLoadingCoupons" class="state-message">優惠券載入中...</p>

      <div class="coupon-table-wrap">
        <div class="coupon-table" role="table" aria-label="店鋪優惠券列表">
          <div class="coupon-table-head" role="row">
            <span>優惠券名稱｜優惠券代碼</span>
            <span>優惠券類型</span>
            <span>適用商品</span>
            <span>折扣額度</span>
            <span>可使用數量</span>
            <span>買家使用限制</span>
            <span>已使用</span>
            <button type="button" class="sort-button" @click="toggleSort('status')">
              使用狀態 <i class="bi" :class="sortIcon('status')" aria-hidden="true"></i>
            </button>
            <button type="button" class="sort-button" @click="toggleSort('period')">
              優惠券可使用期間 <i class="bi" :class="sortIcon('period')" aria-hidden="true"></i>
            </button>
            <span>操作</span>
          </div>

          <article
            v-for="coupon in sortedCoupons"
            :key="coupon.couponId"
            class="coupon-row"
            role="row"
          >
            <div class="coupon-name-cell">
              <span class="coupon-icon">$</span>
              <div>
                <strong>{{ coupon.couponName }}</strong>
                <small>優惠券代碼：{{ coupon.couponCode }}</small>
              </div>
            </div>
            <span>{{ coupon.type }}</span>
            <span>{{ productScopeText(coupon) }}</span>
            <strong>
              {{ discountText(coupon) }}
              <small>滿 {{ formatCurrency(coupon.minPurchaseAmount) }} 可用</small>
            </strong>
            <span>{{ coupon.limitCount }}</span>
            <span>{{ perMemberUsageText(coupon) }}</span>
            <span>{{ coupon.usedCount }}</span>
            <span>
              <span class="status-badge" :class="statusClass(displayStatus(coupon))">
                {{ displayStatus(coupon) }}
              </span>
            </span>
            <span class="date-cell">
              {{ formatDateTime(coupon.startAt) }}
              <small>至 {{ formatDateTime(coupon.endAt) }}</small>
            </span>
            <div class="row-actions">
              <button type="button" @click="editCoupon(coupon)">編輯活動</button>
              <button
                type="button"
                class="cancel-action"
                :disabled="coupon.status === 'DISABLED' || coupon.status === 'EXPIRED'"
                @click="cancelCoupon(coupon)"
              >
                取消活動
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <div v-if="isCreateDrawerOpen" class="drawer-backdrop" @click.self="isCreateDrawerOpen = false">
      <aside class="coupon-drawer" aria-label="建立優惠券">
        <header class="drawer-header">
          <div>
            <span>{{ drawerTitle }}</span>
            <h2>{{ selectedCouponType }}</h2>
          </div>
          <button
            type="button"
            class="icon-button"
            aria-label="關閉"
            @click="isCreateDrawerOpen = false"
          >
            <i class="bi bi-x-lg" aria-hidden="true"></i>
          </button>
        </header>

        <div class="create-form">
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <label>
            優惠券名稱
            <input
              v-model="couponForm.couponName"
              :placeholder="selectedCouponType === '賣場優惠券' ? '全店滿額折抵' : '指定商品折扣'"
            />
          </label>
          <label>
            優惠券代碼
            <input
              v-model="couponForm.couponCode"
              placeholder="AUG2026"
              :disabled="Boolean(editingCouponId)"
            />
          </label>
          <label v-if="selectedCouponType === '商品優惠券'">
            適用商品
            <button type="button" class="product-select-button" @click="isProductPickerOpen = true">
              <span>{{ selectedProduct?.productName || '請選擇商品' }}</span>
              <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i>
            </button>
          </label>
          <label v-else>
            適用商品
            <input value="所有商品" disabled />
          </label>
          <div class="form-row">
            <label>
              折扣類型
              <select v-model="createDiscountType">
                <option value="AMOUNT">固定金額</option>
                <option value="PERCENT">百分比折扣</option>
              </select>
            </label>
            <label>
              折扣額度
              <span class="amount-input">
                <input
                  v-model="couponForm.discountValue"
                  :placeholder="createDiscountType === 'PERCENT' ? '20' : '100'"
                />
                <span>{{ createDiscountUnit }}</span>
              </span>
            </label>
          </div>
          <div class="form-row">
            <label>
              使用門檻
              <input v-model="couponForm.minPurchaseAmount" placeholder="1000" />
            </label>
            <label>
              可使用數量
              <input v-model="couponForm.limitCount" placeholder="100" />
            </label>
          </div>
          <label>
            買家使用限制
            <select v-model="couponForm.perMemberUsagePolicy">
              <option value="ONCE">每位買家限用 1 次</option>
              <option value="REPEAT">每位買家可重複使用</option>
            </select>
          </label>
          <div class="form-row">
            <label>
              開始時間
              <input ref="startAtInput" v-model="couponForm.startAt" type="text" />
            </label>
            <label>
              結束時間
              <input ref="endAtInput" v-model="couponForm.endAt" type="text" />
            </label>
          </div>
          <button type="button" class="submit-button" :disabled="isSubmitting" @click="saveCoupon">
            {{ isSubmitting ? '儲存中...' : drawerActionText }}
          </button>
        </div>
      </aside>
    </div>

    <div
      v-if="isProductPickerOpen"
      class="modal-backdrop"
      role="presentation"
      @click.self="isProductPickerOpen = false"
    >
      <section class="product-picker" role="dialog" aria-modal="true" aria-label="選擇適用商品">
        <header class="picker-header">
          <div>
            <span>適用商品</span>
            <h2>選擇活動商品</h2>
          </div>
          <button
            type="button"
            class="icon-button"
            aria-label="關閉"
            @click="isProductPickerOpen = false"
          >
            <i class="bi bi-x-lg" aria-hidden="true"></i>
          </button>
        </header>

        <p v-if="productOptions.length === 0" class="empty-picker">目前沒有可選擇的上架商品。</p>
        <div v-else class="product-list">
          <button
            v-for="product in productOptions"
            :key="product.productId"
            type="button"
            class="product-option"
            :class="{ selected: String(product.productId) === String(couponForm.productId) }"
            @click="chooseProduct(product)"
          >
            <span class="product-thumb">
              <i class="bi bi-box-seam" aria-hidden="true"></i>
            </span>
            <span>
              <strong>{{ product.productName }}</strong>
              <small>{{ formatCurrency(product.minSkuPrice ?? product.basePrice ?? 0) }}</small>
            </span>
            <i class="bi bi-check2" aria-hidden="true"></i>
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.coupon-page {
  display: grid;
  width: 100%;
  max-width: 100%;
  gap: var(--space-5);
  color: var(--color-text-800);
  font-size: 13px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.eyebrow,
.page-description {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.eyebrow {
  margin: 0 0 var(--space-1);
}

.page-description {
  margin-top: var(--space-1);
}

.page-header h1 {
  margin: 0;
  color: var(--color-text-900);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
}

.creation-panel,
.performance-panel,
.coupon-panel {
  display: grid;
  gap: 14px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 18px 20px;
  background: var(--color-surface);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-heading.compact {
  align-items: center;
  justify-content: flex-start;
}

.section-heading.compact .link-button {
  margin-left: auto;
}

.performance-filter {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 16px;
  padding: 2px 0;
}

.performance-presets,
.performance-dates {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.performance-presets button {
  min-height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 12px;
  background: var(--color-surface);
  color: var(--color-text-800);
  font: inherit;
  font-size: var(--font-size-sm);
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
}

.performance-presets button:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.performance-presets button.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 800;
}

.performance-dates label {
  display: grid;
  gap: 4px;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.performance-dates input {
  width: 132px;
  min-height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 8px;
  font: inherit;
}

.performance-dates > span {
  padding-bottom: 8px;
  color: var(--color-text-muted);
}

.performance-chart {
  height: 220px;
  min-height: 220px;
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}

h1,
h2,
p {
  margin: 0;
}

h1,
h2 {
  color: var(--color-text-900);
  font-size: 16px;
  font-weight: 600;
}

.section-heading p,
.section-heading span,
.creation-card span,
.metric-card span,
.metric-card small,
.coupon-row small {
  color: var(--color-text-muted);
  font-size: 12px;
}

.link-button,
.row-actions button {
  border: 0;
  background: transparent;
  color: var(--color-primary-active);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.group-title {
  margin-bottom: 10px;
  color: var(--color-text-800);
  font-weight: 600;
}

.creation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 12px;
}

.creation-card {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 76px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 12px 16px;
}

.creation-card i {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  color: var(--color-danger);
  font-size: 18px;
}

.creation-card div {
  display: grid;
  gap: 4px;
}

.creation-card strong {
  color: var(--color-text-900);
  font-size: 13px;
}

.creation-card button,
.coupon-toolbar button {
  min-height: 28px;
  border: 1px solid var(--color-danger);
  border-radius: 3px;
  padding: 0 18px;
  background: var(--color-surface);
  color: var(--color-danger);
  font: inherit;
  font-weight: 500;
  cursor: pointer;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--color-border);
}

.metric-card {
  display: grid;
  gap: 6px;
  min-height: 66px;
  padding: 12px 16px;
  border-right: 1px solid var(--color-border);
}

.metric-card:last-child {
  border-right: 0;
}

.metric-card strong {
  color: var(--color-text-900);
  font-size: 20px;
  font-weight: 500;
}

.metric-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  width: fit-content;
}

.metric-info {
  position: relative;
  display: inline-grid;
  place-items: center;
  color: var(--color-text-muted);
  cursor: help;
}

.metric-info::after {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  z-index: 3;
  width: max-content;
  max-width: 260px;
  padding: 8px 10px;
  border-radius: 3px;
  background: #1f2b24;
  color: #fff;
  font-size: 12px;
  line-height: 1.5;
  white-space: normal;
  content: attr(data-tooltip);
  opacity: 0;
  pointer-events: none;
  transform: translateX(-50%) translateY(4px);
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.metric-info::before {
  position: absolute;
  bottom: calc(100% + 3px);
  left: 50%;
  z-index: 4;
  border: 5px solid transparent;
  border-top-color: #1f2b24;
  content: '';
  opacity: 0;
  pointer-events: none;
  transform: translateX(-50%);
  transition: opacity 0.15s ease;
}

.metric-info:hover::after,
.metric-info:hover::before,
.metric-info:focus-visible::after,
.metric-info:focus-visible::before {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.status-tabs {
  display: flex;
  gap: 22px;
  border-bottom: 1px solid var(--color-border);
}

.status-tabs button {
  min-height: 38px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-text-800);
  font: inherit;
  cursor: pointer;
}

.status-tabs button.active {
  border-bottom-color: var(--color-danger);
  color: var(--color-danger);
  font-weight: 600;
}

.status-tabs span {
  display: inline-grid;
  min-width: 18px;
  height: 18px;
  margin-left: 4px;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  font-size: 11px;
}

.coupon-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-field {
  display: grid;
  grid-template-columns: auto 104px minmax(180px, 250px);
  align-items: center;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.search-field span {
  padding: 0 12px;
  color: var(--color-text-800);
}

.search-field::before {
  padding: 0 12px;
  border-left: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  color: var(--color-text-700);
  content: '優惠券名稱';
}

.search-field input {
  min-height: 32px;
  border: 0;
  padding: 0 10px;
  font: inherit;
}

.notice-message {
  margin: 0;
  border: 1px solid var(--color-primary);
  border-radius: 3px;
  padding: 8px 10px;
  background: var(--color-primary-soft);
  color: var(--color-primary-active);
  font-weight: 600;
}

.state-message,
.form-error {
  margin: 0;
  border-radius: 3px;
  padding: 8px 10px;
  font-weight: 600;
}

.state-message {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.form-error {
  border: 1px solid var(--color-danger);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.coupon-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border);
}

.coupon-table {
  min-width: 1120px;
}

.coupon-table-head,
.coupon-row {
  display: grid;
  grid-template-columns:
    minmax(190px, 1.5fr) minmax(82px, 0.62fr) minmax(84px, 0.62fr) minmax(100px, 0.72fr)
    minmax(82px, 0.55fr) minmax(112px, 0.75fr) minmax(64px, 0.42fr) minmax(86px, 0.55fr) minmax(
      170px,
      1.05fr
    )
    minmax(96px, 0.55fr);
  align-items: stretch;
}

.coupon-table-head {
  min-height: 42px;
  background: var(--color-bg-muted);
  color: var(--color-text-700);
  font-size: 12px;
  font-weight: 600;
}

.coupon-table-head > span,
.sort-button {
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-right: 1px solid var(--color-border);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-button {
  justify-content: space-between;
  border-top: 0;
  border-bottom: 0;
  border-left: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.coupon-row {
  min-height: 86px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.coupon-row > span,
.coupon-row > strong,
.coupon-name-cell,
.row-actions {
  display: grid;
  align-content: center;
  gap: 3px;
  min-width: 0;
  padding: 10px 12px;
  border-right: 1px solid var(--color-border);
  overflow: hidden;
  text-overflow: ellipsis;
}

.coupon-row > strong {
  color: var(--color-text-900);
  font-size: 13px;
  white-space: nowrap;
}

.coupon-row > strong small,
.date-cell small {
  display: block;
  margin-top: 3px;
}

.coupon-row > span,
.date-cell,
.date-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-name-cell {
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
}

.coupon-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  background: #80d8d8;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.coupon-name-cell div {
  display: grid;
  gap: 3px;
}

.coupon-name-cell strong {
  overflow: hidden;
  color: var(--color-text-900);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  width: fit-content;
  border-radius: 2px;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge.is-active {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status-badge.is-draft {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.status-badge.is-ended {
  background: var(--color-disabled-bg);
  color: var(--color-text-subtle);
}

.row-actions {
  align-content: center;
  gap: 2px;
  padding: 8px 10px;
}

.row-actions button {
  min-height: 22px;
  padding: 0;
  text-align: left;
  white-space: nowrap;
}

.row-actions .cancel-action {
  color: var(--color-danger);
}

.row-actions button:disabled {
  color: var(--color-text-subtle);
  cursor: default;
}

.drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  background: rgba(20, 30, 26, 0.24);
}

.coupon-drawer {
  width: min(100vw, 520px);
  height: 100vh;
  overflow-y: auto;
  background: var(--color-surface);
  box-shadow: -12px 0 28px rgba(14, 22, 18, 0.16);
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--color-border);
}

.drawer-header span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.icon-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  background: var(--color-surface);
  cursor: pointer;
}

.create-form {
  display: grid;
  gap: 14px;
  padding: 20px;
}

.create-form label {
  display: grid;
  gap: 6px;
  color: var(--color-text-800);
  font-weight: 600;
}

.create-form input,
.create-form select {
  min-height: 34px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 10px;
  font: inherit;
}

.create-form input:disabled {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.product-select-button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-height: 34px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 0 10px;
  background: var(--color-surface);
  color: var(--color-text-800);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.product-select-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.amount-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: stretch;
}

.amount-input input {
  min-width: 0;
  border-right: 0;
  border-radius: 3px 0 0 3px;
}

.amount-input span {
  display: grid;
  min-height: 34px;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 0 3px 3px 0;
  background: var(--color-bg-muted);
  color: var(--color-text-800);
  font-weight: 600;
}

.submit-button {
  justify-self: end;
  min-height: 34px;
  border: 1px solid var(--color-danger);
  border-radius: 3px;
  padding: 0 22px;
  background: var(--color-danger);
  color: #fff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.submit-button:disabled {
  opacity: 0.68;
  cursor: wait;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(20, 30, 26, 0.32);
}

.product-picker {
  width: min(100%, 560px);
  max-height: min(680px, calc(100vh - 48px));
  overflow: hidden;
  border-radius: 4px;
  background: var(--color-surface);
  box-shadow: 0 18px 40px rgba(14, 22, 18, 0.2);
}

.picker-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--color-border);
}

.picker-header span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.product-list {
  display: grid;
  max-height: 520px;
  overflow-y: auto;
  padding: 10px;
}

.product-option {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  border: 1px solid transparent;
  border-radius: 3px;
  padding: 8px 10px;
  background: var(--color-surface);
  color: var(--color-text-800);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.product-option:hover,
.product-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.product-option > i {
  color: var(--color-primary-active);
  opacity: 0;
}

.product-option.selected > i {
  opacity: 1;
}

.product-option span:not(.product-thumb) {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.product-option strong,
.product-option small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-option strong {
  color: var(--color-text-900);
  font-size: 13px;
}

.product-option small {
  color: var(--color-text-muted);
  font-size: 12px;
}

.product-thumb {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 3px;
  background: var(--color-bg-muted);
  color: var(--color-primary-active);
}

.empty-picker {
  padding: 22px;
  color: var(--color-text-muted);
}

button:focus-visible,
input:focus-visible,
select:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

@media (max-width: 900px) {
  .performance-filter {
    align-items: stretch;
    flex-direction: column;
  }

  .performance-dates {
    justify-content: space-between;
  }

  .creation-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .metric-card:last-child {
    border-bottom: 0;
  }

  .section-heading,
  .coupon-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-field {
    grid-template-columns: auto 100px minmax(0, 1fr);
  }

  .coupon-table-wrap {
    overflow-x: auto;
  }

  .coupon-table {
    min-width: 940px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
