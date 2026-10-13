import api from './axios.js'

export const getSellerOrders = (params = {}) => {
  return api.get('/seller/orders', { params })
}

export const getSellerOrder = (orderId) => {
  return api.get(`/seller/orders/${orderId}`)
}

export const createSellerShipment = (orderId, shipment) => {
  return api.post(`/orders/${orderId}/shipment`, shipment)
}

export const updateSellerShipmentStatus = (orderId, status) => {
  return api.patch(`/orders/${orderId}/shipment/status`, { status })
}

export const updateSellerShipmentTrackingInfo = (orderId, shipment) => {
  return api.patch(`/orders/${orderId}/shipment/tracking-info`, shipment)
}

export const getShipmentEvents = (orderId) => api.get(`/orders/${orderId}/shipment/events`)

export const simulateTcatEvent = (orderId, eventType) =>
  api.post(`/orders/${orderId}/shipment/simulate-tcat-event`, { eventType })
