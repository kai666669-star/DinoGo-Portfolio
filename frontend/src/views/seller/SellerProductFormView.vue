<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import {
  createSellerProduct,
  getProductDetail,
  updateSellerProduct,
  updateSellerProductSku,
  createSellerProductSkus,
  disableSellerProductSku,
  updateSellerProductMainImage,
  updateSellerProductImageSort,
  deleteSellerProductImage,
  uploadSellerProductImages,
} from '@/api/sellerProductApi'
import { getCurrentSellerId } from '@/utils/seller-session'
import { logSafeError } from '@/utils/safeError'
import { getImageUrl } from '@/utils/imageUrl'
import ProductDescriptionEditor from '@/components/product/ProductDescriptionEditor.vue'

const sellerId = computed(() => getCurrentSellerId())
const route = useRoute()
const router = useRouter()

const productId = computed(() => Number(route.params.id))
const isEditMode = computed(() => route.name === 'SellerProductEdit')
const isSubmitting = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const imageFileInput = ref(null)
const imagePreviewUrl = ref('')
const selectedImageName = ref('')
const selectedMainImageId = ref(null)
const selectedImageIndex = ref(null)
const selectedImageId = ref(null)
const selectedImageFiles = ref([])
const newImagePreviews = ref([])
const selectedNewMainIndex = ref(null)
const selectedNewImageIndex = ref(0)
const categories = ref([])
const subcategories = ref([])
const brands = ref([])
const removedSkuIds = ref([])

const selectedCategoryId = ref('')
const PRODUCT_NAME_MAX_LENGTH = 50

const productNameLength = computed(() => form.productName.length)

const productNameTooLong = computed(() => productNameLength.value > PRODUCT_NAME_MAX_LENGTH)
const loadCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data
  } catch (error) {
    logSafeError('Load categories failed:', error)
  }
}

const loadSubcategories = async () => {
  try {
    const response = await api.get('/subcategories')
    subcategories.value = response.data
  } catch (error) {
    logSafeError('Load subcategories failed:', error)
  }
}

const loadBrands = async () => {
  try {
    const response = await api.get('/brands')
    brands.value = response.data
  } catch (error) {
    logSafeError('Load brands failed:', error)
  }
}

const filteredSubcategories = computed(() => {
  if (!selectedCategoryId.value) {
    return []
  }

  return subcategories.value.filter(
    (subcategory) => Number(subcategory.categoryId) === Number(selectedCategoryId.value),
  )
})

const handleCategoryChange = () => {
  form.subcategoryId = ''
}

const pageTitle = computed(() => (isEditMode.value ? '編輯商品' : '新增商品'))
const pageDescription = computed(() =>
  isEditMode.value ? '編輯既有商品資料與庫存' : '建立新的商品資料與庫存',
)
const submitText = computed(() => (isEditMode.value ? '儲存變更' : '送出商品'))
const sellerRequiredMessage = '尚未取得賣家身分，請重新登入賣家帳號後再操作。'

const createEmptySku = () => ({
  skuId: null,
  spec1Name: null,
  spec1Value: null,
  spec2Name: null,
  spec2Value: null,
  price: '',
  stock: '',
  enabled: true,
  status: 1,
})

const form = reactive({
  productName: '',
  subcategoryId: '',
  brandId: '',
  basePrice: '',
  description: '',
  skus: [createEmptySku()],
  status: 'ACTIVE',
  imageUrl: '',

  // 商品既有圖片
  images: [],
})

// 是否使用商品規格
// false = 無規格商品
// true = 有規格商品
const hasVariants = ref(false)

// 規格設定
const spec1Name = ref('')
const spec1Values = ref([''])

const hasSpec2 = ref(false)
const spec2Name = ref('')
const spec2Values = ref([''])

// 新增規格一的值
const addSpec1Value = () => {
  spec1Values.value.push('')
}

// 刪除規格一的值
const removeSpec1Value = (index) => {
  if (spec1Values.value.length <= 1) {
    return
  }

  spec1Values.value.splice(index, 1)
  generateSkuList()
}

// 新增第二規格
const addSpec2 = () => {
  hasSpec2.value = true
  spec2Values.value = ['']
  generateSkuList()
}

// 移除第二規格
const removeSpec2 = () => {
  hasSpec2.value = false
  spec2Name.value = ''
  spec2Values.value = ['']
  generateSkuList()
}

// 新增規格二的值
const addSpec2Value = () => {
  spec2Values.value.push('')
}

// 刪除規格二的值
const removeSpec2Value = (index) => {
  if (spec2Values.value.length <= 1) {
    return
  }

  spec2Values.value.splice(index, 1)
  generateSkuList()
}

const changeVariantMode = (useVariants) => {
  hasVariants.value = useVariants

  if (!useVariants) {
    // 切成無規格
    const existingNoSpecSku = form.skus.find(
      (sku) => !sku.spec1Name && !sku.spec1Value && !sku.spec2Name && !sku.spec2Value,
    )

    form.skus = [
      {
        skuId: existingNoSpecSku?.skuId ?? null,
        spec1Name: null,
        spec1Value: null,
        spec2Name: null,
        spec2Value: null,
        price: form.basePrice,
        stock: existingNoSpecSku?.stock ?? '',
        enabled: true,
        status: existingNoSpecSku?.status ?? 1,
      },
    ]

    spec1Name.value = ''
    spec1Values.value = ['']
    hasSpec2.value = false
    spec2Name.value = ''
    spec2Values.value = ['']

    return
  }

  // 切成有規格
  form.skus = []
  spec1Name.value = ''
  spec1Values.value = ['']
  hasSpec2.value = false
  spec2Name.value = ''
  spec2Values.value = ['']
}

// 自動產生 SKU
// 自動產生 SKU 候選組合
const generateSkuList = () => {
  const values1 = spec1Values.value.map((value) => value.trim()).filter((value) => value !== '')

  const values2 = spec2Values.value.map((value) => value.trim()).filter((value) => value !== '')

  const oldSkus = [...form.skus]
  const newSkus = []

  // 只有第一規格
  if (!hasSpec2.value) {
    for (const value1 of values1) {
      const oldSku = oldSkus.find((sku) => sku.spec1Value === value1 && !sku.spec2Value)

      newSkus.push({
        skuId: oldSku?.skuId,

        spec1Name: spec1Name.value.trim(),
        spec1Value: value1,

        spec2Name: null,
        spec2Value: null,

        // 新增商品：預設勾選
        // 編輯商品：資料庫原本存在才勾選
        enabled: oldSku?.enabled ?? (oldSku?.skuId ? true : !isEditMode.value),

        price: oldSku?.price ?? form.basePrice,
        stock: oldSku?.stock ?? '',
        status: oldSku?.status ?? 1,
      })
    }
  } else {
    // 兩種規格
    for (const value1 of values1) {
      for (const value2 of values2) {
        const oldSku = oldSkus.find((sku) => sku.spec1Value === value1 && sku.spec2Value === value2)

        newSkus.push({
          skuId: oldSku?.skuId,

          spec1Name: spec1Name.value.trim(),
          spec1Value: value1,

          spec2Name: spec2Name.value.trim(),
          spec2Value: value2,

          // 新增商品：預設勾選
          // 編輯商品：既有 SKU 勾選，不存在的組合不勾
          enabled: oldSku?.enabled ?? (oldSku?.skuId ? true : !isEditMode.value),

          price: oldSku?.price ?? form.basePrice,
          stock: oldSku?.stock ?? '',
          status: oldSku?.status ?? 1,
        })
      }
    }
  }
  // 找出原本存在於資料庫，但這次因刪除規格而消失的 SKU
  oldSkus.forEach((oldSku) => {
    if (!oldSku.skuId) {
      return
    }

    const stillExists = newSkus.some((newSku) => newSku.skuId === oldSku.skuId)

    if (!stillExists && !removedSkuIds.value.includes(oldSku.skuId)) {
      removedSkuIds.value.push(oldSku.skuId)
    }
  })

  form.skus = newSkus
}

const openImagePicker = () => {
  imageFileInput.value?.click()
}

const handleImageSelect = (event) => {
  const files = Array.from(event.target.files || [])

  if (files.length === 0) {
    return
  }

  newImagePreviews.value.forEach((preview) => {
    URL.revokeObjectURL(preview.url)
  })

  selectedImageFiles.value = files

  newImagePreviews.value = files.map((file) => ({
    file,
    url: URL.createObjectURL(file),
  }))

  imagePreviewUrl.value = newImagePreviews.value[0]?.url ?? ''

  selectedImageName.value = files.length === 1 ? files[0].name : `已選擇 ${files.length} 張圖片`

  selectedNewImageIndex.value = 0

  // 新增商品：預設第一張為主圖
  // 編輯商品且已經有主圖：新圖片不要自動變主圖
  // 編輯商品但完全沒有圖片：第一張預設當主圖
  if (!isEditMode.value || form.images.length === 0) {
    selectedNewMainIndex.value = 0
  } else {
    selectedNewMainIndex.value = null
  }
}

const moveNewImage = (index, direction) => {
  if (index === null) {
    return
  }

  const newIndex = index + direction

  if (newIndex < 0 || newIndex >= newImagePreviews.value.length) {
    return
  }

  const previews = [...newImagePreviews.value]

  const temp = previews[index]
  previews[index] = previews[newIndex]
  previews[newIndex] = temp

  newImagePreviews.value = previews

  // 上傳檔案順序同步
  selectedImageFiles.value = previews.map((preview) => preview.file)

  // 如果移動的是主圖，主圖 index 跟著圖片移動
  if (selectedNewMainIndex.value === index) {
    selectedNewMainIndex.value = newIndex
  } else if (selectedNewMainIndex.value === newIndex) {
    selectedNewMainIndex.value = index
  }

  // 目前選取圖片也跟著移動
  selectedNewImageIndex.value = newIndex
}
const setNewMainImage = () => {
  if (selectedNewImageIndex.value === null) {
    return
  }

  selectedNewMainIndex.value = selectedNewImageIndex.value
}

const changeMainImage = async (imageId) => {
  console.log('changeMainImage 開始：', imageId)
  try {
    await updateSellerProductMainImage(productId.value, imageId)

    selectedMainImageId.value = imageId

    // 同步前端目前主圖狀態
    form.images.forEach((image) => {
      image.isMain = image.imageId === imageId
    })

    // 更新上方主圖預覽
    const mainImage = form.images.find((image) => image.imageId === imageId)

    if (mainImage) {
      form.imageUrl = mainImage.imageUrl
    }
  } catch (error) {
    logSafeError('Update main image failed:', error)
    errorMessage.value = '設定主圖失敗。'
  }
}

const selectNewImage = (index) => {
  selectedNewImageIndex.value = index

  // 上方大圖顯示目前選取的待上傳圖片
  imagePreviewUrl.value = newImagePreviews.value[index]?.url ?? ''
}

const setSelectedImageAsMain = async () => {
  if (selectedImageIndex.value === null) {
    return
  }

  const image = form.images[selectedImageIndex.value]

  if (!image) {
    return
  }

  // 已經是主圖就不用再送 API
  if (image.imageId === selectedMainImageId.value) {
    return
  }

  await changeMainImage(image.imageId)
}

const selectImage = (index) => {
  selectedImageIndex.value = index

  const image = form.images[index]

  selectedImageId.value = image.imageId

  // 上方大圖預覽切成目前選取圖片
  form.imageUrl = image.imageUrl
  imagePreviewUrl.value = ''
}

// 調整商品圖片順序
const moveImage = (index, direction) => {
  if (index === null) {
    return
  }

  const newIndex = index + direction

  if (newIndex < 0 || newIndex >= form.images.length) {
    return
  }

  const images = [...form.images]

  const temp = images[index]
  images[index] = images[newIndex]
  images[newIndex] = temp

  // sortOrder 統一從 1 開始
  images.forEach((image, imageIndex) => {
    image.sortOrder = imageIndex + 1
  })

  form.images = images

  // 移動後，選取狀態跟著圖片移動
  selectedImageIndex.value = newIndex
}

const toFormStatus = (status) => {
  if (status === 1 || status === 'ACTIVE') {
    return 'ACTIVE'
  }
  return 'INACTIVE'
}

// 商品基本資料
const buildProductPayload = (status = null) => ({
  sellerId: sellerId.value,
  subcategoryId: Number(form.subcategoryId),
  brandId: Number(form.brandId),
  productName: form.productName.trim(),
  description: form.description.trim(),
  basePrice: Number(form.basePrice),

  status: status !== null ? status : form.status === 'ACTIVE' ? 1 : 2,
})
// 建立商品使用
const buildCreatePayload = (status = null) => ({
  ...buildProductPayload(status),

  skus: form.skus
    .filter((sku) => sku.enabled)
    .map((sku) => ({
      spec1Name: hasVariants.value ? sku.spec1Name?.trim() || null : null,

      spec1Value: hasVariants.value ? sku.spec1Value?.trim() || null : null,

      spec2Name: hasVariants.value && hasSpec2.value ? sku.spec2Name?.trim() || null : null,

      spec2Value: hasVariants.value && hasSpec2.value ? sku.spec2Value?.trim() || null : null,

      // 無規格商品直接使用基本售價
      price: hasVariants.value ? Number(sku.price) : Number(form.basePrice),

      stock: Number(sku.stock),
    })),

  images: [],
})

const fillProductForm = (product) => {
  form.productName = product.productName ?? ''

  // 設定目前商品的子分類
  form.subcategoryId = product.subcategoryId ? String(product.subcategoryId) : ''

  // 根據子分類找到它所屬的大分類
  const currentSubcategory = subcategories.value.find(
    (subcategory) => Number(subcategory.subcategoryId) === Number(product.subcategoryId),
  )

  // 編輯商品時，自動選回原本的大分類
  selectedCategoryId.value = currentSubcategory ? String(currentSubcategory.categoryId) : ''

  form.brandId = product.brandId ? String(product.brandId) : ''

  form.basePrice = product.basePrice ?? ''
  form.description = product.description ?? ''
  form.status = toFormStatus(product.status)

  // 商品圖片
  form.images = [...(product.images ?? [])].sort(
    (a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999),
  )

  const mainImage = form.images.find((image) => image.isMain === true) ?? form.images[0] ?? null

  form.imageUrl = mainImage?.imageUrl ?? product.imageUrl ?? ''

  imagePreviewUrl.value = ''

  selectedMainImageId.value = mainImage?.imageId ?? null

  selectedImageIndex.value = mainImage
    ? form.images.findIndex((image) => image.imageId === mainImage.imageId)
    : form.images.length > 0
      ? 0
      : null
  selectedImageId.value = mainImage?.imageId ?? form.images[0]?.imageId ?? null

  // SKU
  const skus = product.skus ?? []

  // 判斷是不是無規格商品：
  // 只有一筆 SKU，而且所有規格欄位都是空的
  const noVariantSku =
    skus.length === 1 &&
    !skus[0].spec1Name &&
    !skus[0].spec1Value &&
    !skus[0].spec2Name &&
    !skus[0].spec2Value

  if (noVariantSku) {
    hasVariants.value = false

    form.skus = [
      {
        skuId: skus[0].skuId,
        spec1Name: null,
        spec1Value: null,
        spec2Name: null,
        spec2Value: null,
        price: skus[0].price ?? product.basePrice ?? '',
        stock: skus[0].stock ?? '',
        enabled: (skus[0].status ?? 1) === 1,
        status: skus[0].status ?? 1,
      },
    ]

    spec1Name.value = ''
    spec1Values.value = ['']
    hasSpec2.value = false
    spec2Name.value = ''
    spec2Values.value = ['']

    return
  }

  // 沒有任何 SKU
  if (skus.length === 0) {
    hasVariants.value = false

    form.skus = [createEmptySku()]

    spec1Name.value = ''
    spec1Values.value = ['']
    hasSpec2.value = false
    spec2Name.value = ''
    spec2Values.value = ['']

    return
  }

  // 有規格商品
  hasVariants.value = true

  form.skus = skus.map((sku) => ({
    skuId: sku.skuId,
    spec1Name: sku.spec1Name ?? '',
    spec1Value: sku.spec1Value ?? '',
    spec2Name: sku.spec2Name ?? null,
    spec2Value: sku.spec2Value ?? null,
    enabled: (sku.status ?? 1) === 1,
    price: sku.price ?? '',
    stock: sku.stock ?? '',
    status: sku.status ?? 1,
  }))

  // 規格一
  spec1Name.value = skus[0].spec1Name ?? ''

  spec1Values.value = [...new Set(skus.map((sku) => sku.spec1Value).filter((value) => value))]

  // 是否有第二規格
  hasSpec2.value = skus.some((sku) => sku.spec2Name && sku.spec2Value)

  if (hasSpec2.value) {
    spec2Name.value = skus.find((sku) => sku.spec2Name)?.spec2Name ?? ''

    spec2Values.value = [...new Set(skus.map((sku) => sku.spec2Value).filter((value) => value))]
  } else {
    spec2Name.value = ''
    spec2Values.value = ['']
  }

  generateSkuList()
}

const loadProduct = async () => {
  if (!isEditMode.value) {
    return
  }
  if (!sellerId.value) {
    errorMessage.value = '找不到賣家身分，請重新登入後再進入賣家中心。'
    return
  }

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await getProductDetail(productId.value)
    fillProductForm(response.data)
  } catch (error) {
    logSafeError('Load seller product failed:', error)
    errorMessage.value = '商品資料載入失敗，請確認商品是否存在。'
  } finally {
    isLoading.value = false
  }
}

// 儲存商品 SKU
const saveProductSkus = async () => {
  // 既有而且目前有販售的 SKU
  const existingSkus = form.skus.filter((sku) => sku.skuId && sku.enabled)

  // 沒有 skuId，代表這次新產生的 SKU
  const newSkus = form.skus.filter((sku) => !sku.skuId && sku.enabled)

  // 原本存在，但現在取消販售
  const disabledSkus = form.skus.filter((sku) => sku.skuId && !sku.enabled && sku.status !== 0)

  // ① 修改既有 SKU
  for (const sku of existingSkus) {
    await updateSellerProductSku(productId.value, sku.skuId, {
      spec1Name: hasVariants.value ? sku.spec1Name || null : null,

      spec1Value: hasVariants.value ? sku.spec1Value || null : null,

      spec2Name: hasVariants.value && hasSpec2.value ? sku.spec2Name || null : null,

      spec2Value: hasVariants.value && hasSpec2.value ? sku.spec2Value || null : null,

      price: hasVariants.value ? Number(sku.price) : Number(form.basePrice),

      stock: Number(sku.stock),

      status: 1,
    })
  }

  // ② 批次新增新的 SKU
  if (newSkus.length > 0) {
    await createSellerProductSkus(
      productId.value,

      newSkus.map((sku) => ({
        spec1Name: hasVariants.value ? sku.spec1Name || null : null,

        spec1Value: hasVariants.value ? sku.spec1Value || null : null,

        spec2Name: hasVariants.value && hasSpec2.value ? sku.spec2Name || null : null,

        spec2Value: hasVariants.value && hasSpec2.value ? sku.spec2Value || null : null,

        price: hasVariants.value ? Number(sku.price) : Number(form.basePrice),

        stock: Number(sku.stock),
      })),
    )
  }

  // ③ 停用取消販售的 SKU
  for (const sku of disabledSkus) {
    await disableSellerProductSku(productId.value, sku.skuId)
  }
  // ④ 停用因「刪除規格值」而被移除的既有 SKU
  for (const skuId of removedSkuIds.value) {
    await disableSellerProductSku(productId.value, skuId)
  }

  removedSkuIds.value = []
}
const handleSaveDraft = async () => {
  if (!sellerId.value) {
    errorMessage.value = sellerRequiredMessage
    return
  }

  if (!form.productName.trim()) {
    errorMessage.value = '儲存草稿前請至少輸入商品名稱。'
    return
  }

  if (!form.subcategoryId || !form.brandId) {
    errorMessage.value = '請選擇商品分類與品牌。'
    return
  }

  if (Number(form.basePrice) < 1) {
    errorMessage.value = '商品價格不可小於 1 元。'
    return
  }

  errorMessage.value = ''

  try {
    isSubmitting.value = true

    if (isEditMode.value) {
      // 編輯既有商品
      await updateSellerProduct(productId.value, buildProductPayload(0))

      await saveProductSkus()
    } else {
      // 建立草稿商品
      const response = await createSellerProduct(buildCreatePayload(0))

      const newProductId = response.data.productId

      if (newProductId && selectedImageFiles.value.length > 0) {
        const uploadResponse = await uploadSellerProductImages(
          newProductId,
          selectedImageFiles.value,
        )

        const uploadedImages = uploadResponse.data

        if (selectedNewMainIndex.value !== null) {
          const mainImage = uploadedImages[selectedNewMainIndex.value]

          if (mainImage) {
            await updateSellerProductMainImage(newProductId, mainImage.imageId)
          }
        }
      }
    }

    router.push('/seller/products')
  } catch (error) {
    logSafeError('Save product draft failed:', error)
    errorMessage.value = '儲存草稿失敗，請稍後再試。'
  } finally {
    isSubmitting.value = false
  }
}

// 儲存商品
const handleSubmit = async () => {
  if (!sellerId.value) {
    errorMessage.value = sellerRequiredMessage
    return
  }

  // 原本程式繼續...
  // 商品名稱驗證
  if (!form.productName.trim()) {
    errorMessage.value = '請輸入商品名稱。'
    return
  }
  if (form.productName.trim().length > PRODUCT_NAME_MAX_LENGTH) {
    errorMessage.value = `商品標題不得超過 ${PRODUCT_NAME_MAX_LENGTH} 字。`
    return
  }

  // 分類、品牌驗證
  if (!form.subcategoryId || !form.brandId) {
    errorMessage.value = '請選擇商品分類與品牌。'
    return
  }

  // 商品基本價格驗證
  if (Number(form.basePrice) < 1) {
    errorMessage.value = '商品價格不可小於 1 元。'
    return
  }

  // 只驗證目前有勾選販售的 SKU
  const activeSkus = form.skus.filter((sku) => sku.enabled)

  if (activeSkus.length === 0) {
    errorMessage.value = '商品至少需要一筆可販售的庫存資料。'
    return
  }

  // 無規格商品
  if (!hasVariants.value) {
    const sku = activeSkus[0]

    if (sku.stock === '' || Number(sku.stock) < 0) {
      errorMessage.value = '請輸入正確的商品庫存。'
      return
    }
  }

  // 有規格商品
  if (hasVariants.value) {
    if (activeSkus.some((sku) => Number(sku.price) < 1 || Number(sku.stock) < 0)) {
      errorMessage.value = 'SKU 價格不可小於 1 元，庫存不可小於 0。'
      return
    }
  }

  errorMessage.value = ''

  try {
    isSubmitting.value = true

    if (isEditMode.value) {
      // ① 編輯商品基本資料
      await updateSellerProduct(productId.value, buildProductPayload())

      // ② 修改 / 新增 / 停用 SKU
      await saveProductSkus()

      // ③ 儲存商品圖片排序
      if (form.images.length > 0) {
        await updateSellerProductImageSort(
          productId.value,
          form.images.map((image, index) => ({
            imageId: image.imageId,
            sortOrder: index + 1,
          })),
        )
      }
      // 上傳新選擇的商品圖片
      if (selectedImageFiles.value.length > 0) {
        const uploadResponse = await uploadSellerProductImages(
          productId.value,
          selectedImageFiles.value,
        )

        const uploadedImages = uploadResponse.data

        // 使用者有指定新上傳圖片為主圖
        if (selectedNewMainIndex.value !== null) {
          const newMainImage = uploadedImages[selectedNewMainIndex.value]

          if (newMainImage) {
            await updateSellerProductMainImage(productId.value, newMainImage.imageId)
          }
        }

        newImagePreviews.value.forEach((preview) => {
          URL.revokeObjectURL(preview.url)
        })

        selectedImageFiles.value = []
        newImagePreviews.value = []
        imagePreviewUrl.value = ''
        selectedImageName.value = ''

        selectedNewMainIndex.value = null
        selectedNewImageIndex.value = null
      }
    } else {
      // ① 先建立商品
      const response = await createSellerProduct(buildCreatePayload())

      const newProductId = response.data.productId

      // ② 商品建立完成後，再上傳剛才選的多張圖片
      if (newProductId && selectedImageFiles.value.length > 0) {
        // 上傳圖片
        const uploadResponse = await uploadSellerProductImages(
          newProductId,
          selectedImageFiles.value,
        )

        // 取得剛剛上傳完成的圖片
        const uploadedImages = uploadResponse.data

        // 找出使用者選擇的主圖
        const mainImage = uploadedImages[selectedNewMainIndex.value]

        // 設定主圖
        if (mainImage) {
          await updateSellerProductMainImage(newProductId, mainImage.imageId)
        }
      }
    }

    router.push('/seller/products')
  } catch (error) {
    logSafeError('Save seller product failed:', error)
    errorMessage.value = isEditMode.value
      ? '編輯商品失敗，請確認商品資料與 SKU 是否正確。'
      : '新增商品失敗，請確認欄位是否正常。'
  } finally {
    isSubmitting.value = false
  }
}
const removeExistingImage = async () => {
  if (selectedImageId.value === null) {
    return
  }

  const selectedImage = form.images.find((image) => image.imageId === selectedImageId.value)

  if (!selectedImage) {
    return
  }

  // 前端先擋主圖
  if (selectedImage.imageId === selectedMainImageId.value) {
    errorMessage.value = '主圖不可直接刪除，請先設定其他圖片為主圖。'
    return
  }

  try {
    await deleteSellerProductImage(productId.value, selectedImage.imageId)

    form.images = form.images.filter((image) => image.imageId !== selectedImage.imageId)

    // 重新整理 sortOrder
    form.images.forEach((image, index) => {
      image.sortOrder = index + 1
    })

    selectedImageIndex.value = null
    selectedImageId.value = null
  } catch (error) {
    logSafeError('Delete product image failed:', error)
    errorMessage.value = '刪除圖片失敗。'
  }
}
onMounted(async () => {
  await Promise.all([loadCategories(), loadSubcategories(), loadBrands()])

  await loadProduct()
})
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>{{ pageTitle }}</h1>
        <p class="page-description">{{ pageDescription }}</p>
      </div>
    </header>

    <div class="product-form" @submit.prevent="handleSubmit" @keydown.enter.prevent>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <p v-if="isLoading" class="state-message">商品資料載入中...</p>

      <div class="product-main-fields">
        <label class="form-field">
          商品分類

          <select v-model="selectedCategoryId" @change="handleCategoryChange">
            <option value="">請選擇商品分類</option>

            <option
              v-for="category in categories"
              :key="category.categoryId"
              :value="category.categoryId"
            >
              {{ category.categoryName }}
            </option>
          </select>
        </label>

        <label class="form-field">
          子分類

          <select v-model="form.subcategoryId" :disabled="!selectedCategoryId">
            <option value="">請選擇子分類</option>

            <option
              v-for="subcategory in filteredSubcategories"
              :key="subcategory.subcategoryId"
              :value="subcategory.subcategoryId"
            >
              {{ subcategory.subcategoryName }}
            </option>
          </select>
        </label>

        <label class="form-field">
          品牌

          <select v-model="form.brandId">
            <option value="">請選擇品牌</option>

            <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
              {{ brand.brandName }}
            </option>
          </select>
        </label>

        <label class="form-field">
          基本售價
          <input v-model="form.basePrice" type="number" min="1" placeholder="1" />
        </label>

        <label class="form-field full-width">
          <div class="field-label-row">
            <span>商品名稱</span>

            <span class="character-count" :class="{ error: productNameTooLong }">
              {{ productNameLength }} / {{ PRODUCT_NAME_MAX_LENGTH }}
            </span>
          </div>

          <input v-model="form.productName" type="text" placeholder="請輸入商品名稱" />

          <small v-if="productNameTooLong" class="field-error">
            商品標題不得超過 {{ PRODUCT_NAME_MAX_LENGTH }} 字
          </small>
        </label>
        <div class="form-field full-width">
          <span>商品描述</span>

          <ProductDescriptionEditor v-model="form.description" />
        </div>
        <section class="sku-section full-width">
          <div class="section-header">
            <h2>商品規格 SKU</h2>
          </div>
          <div class="variant-mode">
            <label class="variant-option">
              <input type="radio" :checked="!hasVariants" @change="changeVariantMode(false)" />
              無規格
            </label>

            <label class="variant-option">
              <input type="radio" :checked="hasVariants" @change="changeVariantMode(true)" />
              有規格
            </label>
          </div>
          <!-- 無規格商品 -->
          <div v-if="!hasVariants" class="spec-block">
            <div class="spec-title">
              <strong>商品庫存</strong>
            </div>

            <label class="form-field">
              庫存數量

              <input v-model="form.skus[0].stock" type="number" min="0" placeholder="請輸入庫存" />
            </label>
          </div>

          <!-- 規格一 -->
          <div v-if="hasVariants" class="spec-block">
            <div class="spec-title">
              <strong>規格一</strong>
            </div>

            <label class="form-field">
              規格名稱
              <input v-model="spec1Name" placeholder="例如：顏色" @input="generateSkuList" />
            </label>

            <div class="spec-values">
              <label>規格值</label>

              <div
                v-for="(value, index) in spec1Values"
                :key="`spec1-${index}`"
                class="spec-value-row"
              >
                <input
                  v-model="spec1Values[index]"
                  placeholder="例如：白色"
                  @input="generateSkuList"
                />

                <button
                  v-if="spec1Values.length > 1"
                  type="button"
                  @click="removeSpec1Value(index)"
                >
                  刪除
                </button>
              </div>

              <button type="button" @click="addSpec1Value">＋ 新增規格值</button>
            </div>
          </div>

          <!-- 新增第二規格 -->
          <button
            v-if="hasVariants && !hasSpec2"
            type="button"
            class="add-spec-button"
            @click="addSpec2"
          >
            ＋ 新增第二規格
          </button>

          <!-- 規格二 -->
          <div v-if="hasVariants && hasSpec2" class="spec-block">
            <div class="spec-title">
              <strong>規格二</strong>

              <button type="button" @click="removeSpec2">移除規格二</button>
            </div>

            <label class="form-field">
              規格名稱
              <input v-model="spec2Name" placeholder="例如：容量" @input="generateSkuList" />
            </label>

            <div class="spec-values">
              <label>規格值</label>

              <div
                v-for="(value, index) in spec2Values"
                :key="`spec2-${index}`"
                class="spec-value-row"
              >
                <input
                  v-model="spec2Values[index]"
                  placeholder="例如：256GB"
                  @input="generateSkuList"
                />

                <button
                  v-if="spec2Values.length > 1"
                  type="button"
                  @click="removeSpec2Value(index)"
                >
                  刪除
                </button>
              </div>

              <button type="button" @click="addSpec2Value">＋ 新增規格值</button>
            </div>
          </div>

          <!-- 自動產生 SKU 組合 -->
          <div v-if="hasVariants && form.skus.length > 0" class="sku-combination-section">
            <h3>規格組合</h3>

            <div class="sku-table-wrapper">
              <table class="sku-table">
                <thead>
                  <tr>
                    <th>{{ spec1Name || '規格一' }}</th>

                    <th v-if="hasSpec2">
                      {{ spec2Name || '規格二' }}
                    </th>
                    <th class="checkbox-cell">販售</th>
                    <th class="checkbox-cell">價格</th>
                    <th class="checkbox-cell">庫存</th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="(sku, index) in form.skus"
                    :key="`${sku.spec1Value}-${sku.spec2Value || ''}`"
                  >
                    <td>{{ sku.spec1Value }}</td>

                    <td v-if="hasSpec2">
                      {{ sku.spec2Value }}
                    </td>
                    <td class="checkbox-cell">
                      <input v-model="form.skus[index].enabled" type="checkbox" />
                    </td>
                    <td>
                      <input
                        v-model="form.skus[index].price"
                        type="number"
                        min="1"
                        placeholder="價格"
                        :disabled="!sku.enabled"
                      />
                    </td>

                    <td>
                      <input
                        v-model="form.skus[index].stock"
                        type="number"
                        min="0"
                        placeholder="庫存"
                        :disabled="!sku.enabled"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <aside class="product-side-panel">
        <section class="image-section">
          <h2>商品圖片</h2>

          <button class="image-upload-button" type="button" @click="openImagePicker">
            <img
              v-if="imagePreviewUrl || form.imageUrl"
              class="image-preview"
              :src="imagePreviewUrl || getImageUrl(form.imageUrl)"
              :alt="selectedImageName || '商品主圖'"
            />
            <div v-else class="image-placeholder">
              <i class="bi bi-image" aria-hidden="true"></i>
              <span>點選上傳</span>
              <small>商品圖片 placeholder</small>
            </div>
          </button>

          <input
            ref="imageFileInput"
            class="image-file-input"
            type="file"
            accept="image/*"
            multiple
            @change="handleImageSelect"
          />
          <!-- 新選擇、尚未上傳的圖片 -->
          <div v-if="newImagePreviews.length > 0" class="new-image-preview-section">
            <div class="image-list-header">
              <p class="image-list-title">待上傳圖片（{{ newImagePreviews.length }} 張）</p>

              <small> 點選圖片後可調整順序或設定主圖 </small>
            </div>

            <div class="image-thumbnail-strip">
              <button
                v-for="(preview, index) in newImagePreviews"
                :key="preview.url"
                type="button"
                class="image-thumbnail-button"
                :class="{
                  active: selectedNewImageIndex === index,
                  'is-main': selectedNewMainIndex === index,
                }"
                @click="selectNewImage(index)"
              >
                <img :src="preview.url" alt="待上傳商品圖片" class="existing-image-thumbnail" />

                <span v-if="selectedNewMainIndex === index" class="thumbnail-main-badge">
                  主圖
                </span>

                <span class="thumbnail-order">
                  {{ index + 1 }}
                </span>
              </button>
            </div>

            <div class="image-order-actions">
              <button
                type="button"
                :disabled="selectedNewImageIndex === null || selectedNewImageIndex === 0"
                @click="moveNewImage(selectedNewImageIndex, -1)"
              >
                ← 往左
              </button>

              <button
                type="button"
                :disabled="
                  selectedNewImageIndex === null ||
                  selectedNewImageIndex === newImagePreviews.length - 1
                "
                @click="moveNewImage(selectedNewImageIndex, 1)"
              >
                往右 →
              </button>

              <button
                type="button"
                :disabled="
                  selectedNewImageIndex === null || selectedNewImageIndex === selectedNewMainIndex
                "
                @click="setNewMainImage"
              >
                {{ selectedNewImageIndex === selectedNewMainIndex ? '目前主圖' : '設為主圖' }}
              </button>
            </div>
          </div>
          <!-- 編輯商品：既有商品圖片 -->
          <div v-if="isEditMode && form.images.length > 0" class="existing-image-list">
            <div class="image-list-header">
              <p class="image-list-title">目前商品圖片</p>
              <small>點選圖片後可調整順序或設定主圖</small>
            </div>

            <!-- 橫向縮圖 -->
            <div class="image-thumbnail-strip">
              <button
                v-for="(image, index) in form.images"
                :key="image.imageId"
                type="button"
                class="image-thumbnail-button"
                :class="{
                  active: selectedImageIndex === index,
                  'is-main': selectedMainImageId === image.imageId,
                }"
                @click="selectImage(index)"
              >
                <img
                  :src="getImageUrl(image.imageUrl)"
                  alt="商品圖片"
                  class="existing-image-thumbnail"
                />

                <span v-if="selectedMainImageId === image.imageId" class="thumbnail-main-badge">
                  主圖
                </span>

                <span class="thumbnail-order">
                  {{ index + 1 }}
                </span>
              </button>
            </div>

            <!-- 選取圖片後的統一操作區 -->
            <div v-if="selectedImageIndex !== null" class="image-order-actions">
              <button
                type="button"
                :disabled="selectedImageIndex === 0"
                @click="moveImage(selectedImageIndex, -1)"
              >
                ← 往左
              </button>

              <button
                type="button"
                :disabled="selectedImageIndex === form.images.length - 1"
                @click="moveImage(selectedImageIndex, 1)"
              >
                往右 →
              </button>

              <button
                type="button"
                :disabled="form.images[selectedImageIndex]?.imageId === selectedMainImageId"
                @click="setSelectedImageAsMain"
              >
                {{
                  form.images[selectedImageIndex]?.imageId === selectedMainImageId
                    ? '目前主圖'
                    : '設為主圖'
                }}
              </button>

              <button
                type="button"
                :disabled="selectedImageId === null || selectedImageId === selectedMainImageId"
                @click="removeExistingImage"
              >
                刪除圖片
              </button>
            </div>
          </div>
        </section>

        <div class="form-actions side-actions">
          <button
            v-if="!isEditMode"
            type="button"
            :disabled="isSubmitting"
            @click="handleSaveDraft"
          >
            {{ isSubmitting ? '儲存中...' : '儲存草稿' }}
          </button>
          <button
            class="primary-button"
            type="button"
            :disabled="isSubmitting"
            @click="handleSubmit"
          >
            {{ isSubmitting ? '送出中...' : submitText }}
          </button>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.page-description {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1,
h2 {
  margin: 0;
  font-family: var(--font-heading);
}

h1 {
  font-size: var(--font-size-xl);
}

h2 {
  font-size: var(--font-size-lg);
}

.product-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  width: 100%;
  max-width: 1180px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.product-main-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  align-content: start;
}

.product-side-panel {
  display: grid;
  align-content: start;
  gap: var(--space-4);
}

.form-field {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 600;
}

.full-width {
  grid-column: 1 / -1;
}

.sku-section {
  display: grid;
  gap: var(--space-4);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.sku-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.image-section {
  display: grid;
  gap: var(--space-3);
}

.image-upload-button {
  width: 100%;
  min-height: 230px;
  border: 0;
  border-radius: var(--radius-md);
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-bg-muted);
  cursor: pointer;
  font: inherit;
  overflow: hidden;
}

.image-upload-button:hover {
  background: var(--color-disabled-bg);
}

.image-upload-button:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.image-preview {
  width: 100%;
  height: 230px;
  object-fit: contain;
  background: var(--color-bg-muted);
}

.image-placeholder {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: var(--space-2);
  min-height: 100%;
  font-family: var(--font-body);
}

.image-placeholder i {
  color: var(--color-primary);
  font-size: 32px;
}

.image-placeholder span,
.image-placeholder small {
  display: block;
}

.image-placeholder span {
  color: var(--color-text-700);
  font-weight: 700;
}

.image-placeholder small {
  font-size: var(--font-size-sm);
}

.image-file-input {
  display: none;
}

input,
select,
textarea {
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  font: inherit;
  font-weight: 400;
}

textarea {
  min-height: 96px;
  padding: var(--space-3);
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.side-actions {
  justify-content: stretch;
}

.side-actions button {
  flex: 1;
}

button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font: inherit;
  font-weight: 600;
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message,
.state-message {
  grid-column: 1 / -1;
  margin: 0;
}

.error-message {
  color: #b42318;
  font-weight: 600;
}

.state-message {
  color: var(--color-text-muted);
}

.spec-block {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.spec-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.spec-values {
  display: grid;
  gap: var(--space-2);
}

.spec-value-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-2);
}

.add-spec-button {
  justify-self: start;
}

.sku-combination-section {
  display: grid;
  gap: var(--space-3);
}

.sku-table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.sku-table {
  width: 100%;
  border-collapse: collapse;
}

.sku-table th,
.sku-table td {
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  text-align: left;
}

.sku-table th {
  background: var(--color-bg-muted);
}

.sku-table input {
  min-width: 120px;
}

.checkbox-cell {
  text-align: center !important;
}

.sku-table input[type='checkbox'] {
  width: 14px;
  height: 14px;
  min-width: 14px;
  margin: 0;
  cursor: pointer;
}

.existing-image-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.image-list-title {
  margin: 0;
  color: var(--color-text-700);
  font-weight: 700;
}

.image-list-header {
  display: grid;
  gap: 4px;
}

.image-list-header small {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

/* 橫向圖片縮圖列 */
.image-thumbnail-strip {
  display: flex;
  gap: var(--space-2);

  width: 100%;
  padding-bottom: var(--space-2);

  overflow-x: auto;
}

/* 每一張縮圖 */
.image-thumbnail-button {
  position: relative;

  flex: 0 0 82px;

  width: 82px;
  height: 82px;
  min-height: 82px;

  padding: 4px;

  border: 2px solid transparent;
  border-radius: var(--radius-md);

  background: var(--color-bg-muted);

  cursor: pointer;
}

.image-thumbnail-button.active {
  border-color: var(--color-primary);
}

.image-thumbnail-button.is-main {
  box-shadow: 0 0 0 1px var(--color-primary);
}

.existing-image-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: var(--color-bg-muted);
}

/* 主圖標記 */
.thumbnail-main-badge {
  position: absolute;
  top: 4px;
  left: 4px;

  padding: 2px 5px;

  border-radius: 4px;

  background: var(--color-primary);
  color: white;

  font-size: 11px;
  font-weight: 700;
}

/* 圖片順序 */
.thumbnail-order {
  position: absolute;
  right: 4px;
  bottom: 4px;

  display: grid;
  place-items: center;

  width: 20px;
  height: 20px;

  border-radius: 50%;

  background: rgba(0, 0, 0, 0.65);
  color: white;

  font-size: 11px;
}

/* 圖片操作按鈕 */
.image-order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.image-order-actions button {
  min-height: 34px;
  padding: 0 var(--space-3);
}

.image-order-actions button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.new-image-preview-section {
  display: grid;
  gap: var(--space-2);
}

.new-image-preview-item {
  position: relative;
  flex: 0 0 82px;
  width: 82px;
  height: 82px;
  padding: 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.new-image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

@media (max-width: 960px) {
  .product-form {
    grid-template-columns: 1fr;
  }

  .product-side-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .product-main-fields,
  .sku-row {
    grid-template-columns: 1fr;
  }

  .product-form {
    padding: var(--space-4);
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
.variant-mode {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.variant-option {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  font-weight: 600;
}

.variant-option input[type='radio'] {
  width: 16px;
  height: 16px;
  min-height: auto;
  margin: 0;
}

.no-spec-hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.character-count {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 400;
}

.character-count.error,
.field-error {
  color: #b42318;
}

.field-error {
  font-size: var(--font-size-sm);
  font-weight: 500;
}
</style>
