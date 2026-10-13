import api from './axios'

//查詢目前賣家商品列表
//sellerId 目前由前端傳入，之後可改成登入狀態取得
export const getSellerProducts = (sellerId) => {
  return api.get('/seller/products', {
    params: { sellerId },
  })
}

//指定商品下架
//後端依照sellerId與productId判斷是否為該賣家商品，若是則下架，若否則回傳錯誤
export const disableSellerProduct = (sellerId, productId) => {
  return api.patch(`/seller/products/${productId}/disable`, null, {
    params: { sellerId },
  })
}

// B 模組商品上架 API
export const publishSellerProduct = (productId) => {
  return api.patch(`/products/${productId}/publish`)
}

// B 模組商品下架 API
export const unpublishSellerProduct = (productId) => {
  return api.patch(`/products/${productId}/unpublish`)
}

// 取得商品詳情，編輯商品頁用來回填既有資料
export const getProductDetail = (productId) => {
  return api.get(`/seller/products/${productId}`)
}

// 建立新商品，串接 B 模組商品新增 API
export const createSellerProduct = (payload) => {
  return api.post('/products', payload)
}

// 修改商品基本資料，串接 B 模組商品修改 API
export const updateSellerProduct = (productId, payload) => {
  return api.put(`/products/${productId}`, payload)
}

// 修改既有 SKU
export const updateSellerProductSku = (productId, skuId, payload) => {
  return api.put(`/products/${productId}/skus/${skuId}`, payload)
}

// 批次新增 SKU
export const createSellerProductSkus = (productId, payload) => {
  return api.post(`/products/${productId}/skus/batch`, payload)
}

// 停用既有 SKU
export const disableSellerProductSku = (productId, skuId) => {
  return api.patch(`/products/${productId}/skus/${skuId}/disable`)
}

// 修改商品主圖
export const updateSellerProductMainImage = (productId, imageId) => {
  return api.put(`/products/${productId}/images/main`, {
    imageId,
  })
}

// 修改商品圖片排序
export const updateSellerProductImageSort = (
  productId,
  images,
) => {
  return api.put(
    `/products/${productId}/images/sort`,
    images,
  )
}

// 刪除商品圖片
export const deleteSellerProductImage = (
  productId,
  imageId,
) => {
  return api.delete(
    `/products/${productId}/images/${imageId}`,
  )
}

// 上傳多張商品圖片
export const uploadSellerProductImages = (productId, files) => {
  const formData = new FormData()

  files.forEach((file) => {
    formData.append('files', file)
  })

  return api.post(
    `/products/${productId}/images/upload`,
    formData,
  )
}

// 軟刪除商品
export const deleteSellerProduct = (productId) => {
  return api.delete(`/products/${productId}`)
}
