import api from './axios.js'
import { retryPaymentCreationOnce } from './payment-retry.js'

export function getMemberOrders() {
  return api.get('/orders/member')
}

export function getOrder(orderId) {
  return api.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId, request) {
  return api.patch(`/orders/${orderId}/cancel`, request)
}

export function createPayment(orderId, paymentMethodCode) {
  const idempotencyKey = crypto.randomUUID()
  return retryPaymentCreationOnce(() =>
    api.post(
      `/orders/${orderId}/payments`,
      { paymentMethodCode },
      { headers: { 'Idempotency-Key': idempotencyKey } },
    ),
  )
}

export function submitEcpayCheckout(checkout) {
  const form = document.createElement('form')
  form.method = 'post'
  form.action = checkout.action
  Object.entries(checkout.fields).forEach(([name, value]) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  })
  document.body.appendChild(form)
  form.submit()
}

export function getPaymentCapabilities() {
  return api.get('/payments/capabilities')
}

export function getPaymentMethods() {
  return api.get('/payments/methods')
}

export function simulatePayment(orderId, paymentId, status = 'SUCCESS', failureReason = null) {
  return api.post(`/orders/${orderId}/payments/${paymentId}/simulate`, {
    status,
    failureReason,
  })
}

export function confirmDelivery(orderId) {
  return api.patch(`/orders/${orderId}/shipment/confirm-delivery`)
}

export function getShipmentEvents(orderId) {
  return api.get(`/orders/${orderId}/shipment/events`)
}
