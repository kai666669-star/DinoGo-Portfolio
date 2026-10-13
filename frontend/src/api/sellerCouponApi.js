import api from './axios'

export const getSellerCoupons = (sellerId) => {
  return api.get('/seller/coupons', {
    params: { sellerId },
  })
}

export const createSellerCoupon = (sellerId, payload) => {
  return api.post('/seller/coupons', payload, {
    params: { sellerId },
  })
}

export const updateSellerCoupon = (sellerId, couponId, payload) => {
  return api.put(`/seller/coupons/${couponId}`, payload, {
    params: { sellerId },
  })
}

export const activateSellerCoupon = (sellerId, couponId) => {
  return api.patch(`/seller/coupons/${couponId}/activate`, null, {
    params: { sellerId },
  })
}

export const disableSellerCoupon = (sellerId, couponId) => {
  return api.patch(`/seller/coupons/${couponId}/disable`, null, {
    params: { sellerId },
  })
}
