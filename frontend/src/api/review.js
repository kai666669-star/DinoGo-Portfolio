//review-start，總共1次修改，第1次//
import api from './axios.js'

/** 完成訂單商品明細使用：取得 starId、fiveStar 與 reviewed。 */
export function getOrderStars(orderId) {
  return api.get(`/reviews/orders/${orderId}/stars`)
}

/** 單項產品評價使用：一次上傳目前新增的評論照片，最多三張。 */
export function uploadReviewImages(files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return api.post('/reviews/stars/images', formData)
}

/** 單項產品評價頁使用：送出五星、內容及至多三張圖片。 */
export function updateStar(starId, request) {
  return api.put(`/reviews/stars/${starId}`, request)
}

/** 清空單項產品評價內容。 */
export function clearStar(starId) {
  return api.delete(`/reviews/stars/${starId}/content`)
}

/** 產品明細頁使用：以後端 Offset 分頁每次載入 10 筆公開評價。 */
export function getProductReviews(productId, page = 1, filters = {}) {
  return api.get(`/reviews/products/${productId}`, {
    params: {
      page,
      rating: filters.rating || undefined,
      content: filters.content || 'ALL',
    },
  })
}
//review-end，總共1次修改，第1次//
