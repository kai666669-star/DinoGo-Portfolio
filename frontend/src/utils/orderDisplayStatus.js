export const ORDER_DISPLAY_STATUS = Object.freeze({
  PENDING_PAYMENT: Object.freeze({ key: 'PENDING_PAYMENT', label: '待付款' }),
  PENDING_SHIPMENT: Object.freeze({ key: 'PENDING_SHIPMENT', label: '待出貨' }),
  IN_TRANSIT: Object.freeze({ key: 'IN_TRANSIT', label: '運送中' }),
  PENDING_PICKUP: Object.freeze({ key: 'PENDING_PICKUP', label: '待收貨' }),
  COMPLETED: Object.freeze({ key: 'COMPLETED', label: '已完成' }),
  CANCELLED: Object.freeze({ key: 'CANCELLED', label: '不成立' }),
})

export function getOrderDisplayStatus(order) {
  if (order?.status === 'CANCELLED') return ORDER_DISPLAY_STATUS.CANCELLED
  if (order?.status === 'COMPLETED') return ORDER_DISPLAY_STATUS.COMPLETED

  if (order?.status === 'SHIPPED') {
    if (['AVAILABLE_FOR_PICKUP', 'DELIVERED'].includes(order.shipment?.status)) {
      return ORDER_DISPLAY_STATUS.PENDING_PICKUP
    }
    return ORDER_DISPLAY_STATUS.IN_TRANSIT
  }

  if (order?.status === 'PAID' || order?.status === 'PROCESSING') {
    return ORDER_DISPLAY_STATUS.PENDING_SHIPMENT
  }

  if (order?.status === 'PENDING_PAYMENT') return ORDER_DISPLAY_STATUS.PENDING_PAYMENT

  return Object.freeze({
    key: order?.status ?? 'UNKNOWN',
    label: order?.status ?? '狀態未知',
  })
}

export function isOrderInDisplayGroup(order, group) {
  const displayStatus = getOrderDisplayStatus(order)

  if (group === 'PENDING_RECEIPT') {
    return ['IN_TRANSIT', 'PENDING_PICKUP'].includes(displayStatus.key)
  }

  return displayStatus.key === group
}
