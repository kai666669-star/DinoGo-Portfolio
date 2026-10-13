import api from './axios.js'

export const getSellerSalesInsight = (params = {}) => {
  return api.get('/seller/sales-insight', { params })
}

export const analyzeSellerSalesInsight = (params = {}) => {
  return api.post('/seller/sales-insight/analyze', null, { params })
}
