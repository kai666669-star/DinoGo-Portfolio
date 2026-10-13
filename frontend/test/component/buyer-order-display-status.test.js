import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import OrderDetail from '../../src/views/sales/OrderDetail.vue'
import OrderList from '../../src/views/sales/OrderList.vue'
import {
  cancelOrder,
  confirmDelivery,
  createPayment,
  getMemberOrders,
  getOrder,
  getShipmentEvents,
  submitEcpayCheckout,
} from '../../src/api/order.js'

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ params: { id: '10' } }),
}))

vi.mock('../../src/api/order.js', () => ({
  cancelOrder: vi.fn(),
  confirmDelivery: vi.fn(),
  createPayment: vi.fn(),
  getMemberOrders: vi.fn(),
  getOrder: vi.fn(),
  getShipmentEvents: vi.fn(),
  submitEcpayCheckout: vi.fn(),
}))

const itemFixture = {
  orderItemId: 1,
  productName: '測試商品',
  skuSpec: '標準規格',
  quantity: 1,
  unitPrice: 500,
  subtotal: 500,
}

const orderFixture = (overrides = {}) => ({
  orderId: 10,
  orderNo: 'ORD-10',
  sellerId: 30,
  status: 'SHIPPED',
  createdAt: '2026-08-20T02:00:00Z',
  receiverName: '王小明',
  receiverPhone: '0912345678',
  shippingPostalCode: '100',
  shippingCity: '台北市',
  shippingDistrict: '中正區',
  shippingDetailAddress: '測試路 1 號',
  buyerRemark: null,
  subtotalAmount: 500,
  shippingFee: 0,
  discountAmount: 0,
  totalAmount: 500,
  items: [itemFixture],
  payment: null,
  shipment: { status: 'SHIPPED' },
  ...overrides,
})

function deferred() {
  let resolve
  const promise = new Promise((promiseResolve) => {
    resolve = promiseResolve
  })
  return { promise, resolve }
}

beforeEach(() => {
  vi.clearAllMocks()
  getShipmentEvents.mockResolvedValue({ data: [] })
})

describe('buyer order list aggregate display status', () => {
  test('renders aggregate badges and filters the pending-receipt tab by aggregate group', async () => {
    getMemberOrders.mockResolvedValue({
      data: [
        orderFixture({
          orderId: 10,
          orderNo: 'ORD-IN-TRANSIT',
          shipment: { status: 'SHIPPED' },
        }),
        orderFixture({
          orderId: 11,
          orderNo: 'ORD-PICKUP',
          shipment: { status: 'AVAILABLE_FOR_PICKUP' },
        }),
        orderFixture({
          orderId: 12,
          orderNo: 'ORD-PROCESSING',
          status: 'PROCESSING',
          shipment: null,
        }),
      ],
    })

    const wrapper = mount(OrderList)
    await flushPromises()

    const badges = wrapper.findAll('.status-badge')
    expect(badges.map((badge) => badge.text())).toEqual([
      '運送中',
      '待收貨',
      '待出貨',
    ])
    expect(badges[0].classes()).toContain('status-in_transit')
    expect(badges[1].classes()).toContain('status-pending_pickup')

    const pendingReceiptTab = wrapper
      .findAll('.order-tabs button')
      .find((button) => button.text() === '待收貨')
    await pendingReceiptTab.trigger('click')

    const visibleOrderIds = wrapper
      .findAll('.order-card')
      .map((card) => card.attributes('data-order-id'))
    expect(visibleOrderIds).toEqual(['10', '11'])
  })
})

describe('buyer order detail aggregate display status', () => {
  test('renders the detail badge from order and shipment status together', async () => {
    getOrder.mockResolvedValue({
      data: orderFixture({
        shipment: {
          status: 'DELIVERED',
          carrierName: '黑貓宅急便',
          trackingNo: 'TRACK-001',
        },
      }),
    })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    expect(getOrder).toHaveBeenCalledWith(10)
    const badge = wrapper.get('.status-badge')
    expect(badge.text()).toBe('待收貨')
    expect(badge.classes()).toContain('status-pending_pickup')
    expect(wrapper.text()).toContain('已送達')
    expect(wrapper.get('button.btn-primary').text()).toBe('完成訂單')
    wrapper.unmount()
  })

  test('refreshes an active order and advances the progress after the seller ships it', async () => {
    getOrder
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'PROCESSING',
          shipment: null,
        }),
      })
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'SHIPPED',
          shipment: {
            status: 'SHIPPED',
            carrierName: '黑貓宅急便',
            trackingNo: 'TRACK-001',
          },
        }),
      })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    let progressSteps = wrapper.findAll('.progress-step')
    expect(progressSteps[2].classes()).not.toContain('complete')
    expect(wrapper.text()).toContain('尚未建立物流資料')

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(getOrder).toHaveBeenCalledTimes(2)
    progressSteps = wrapper.findAll('.progress-step')
    expect(progressSteps[2].text()).toBe('商品出貨')
    expect(progressSteps[2].classes()).toContain('complete')
    expect(wrapper.get('.status-badge').text()).toBe('運送中')
    expect(wrapper.text()).toContain('黑貓宅急便')
    expect(wrapper.text()).toContain('TRACK-001')
    expect(wrapper.find('button.btn-primary').exists()).toBe(false)
    wrapper.unmount()
  })

  test('keeps existing shipment events visible while a silent refresh loads new events', async () => {
    getOrder.mockResolvedValue({ data: orderFixture() })
    getShipmentEvents.mockResolvedValue({ data: [{ shipmentEventId: 1, eventType: 'HANDED_OVER' }] })
    const wrapper = mount(OrderDetail)
    await flushPromises()
    const pendingEvents = deferred()
    getShipmentEvents.mockImplementationOnce(() => pendingEvents.promise)

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(wrapper.find('.shipment-timeline').exists()).toBe(true)
    expect(wrapper.get('.shipment-timeline').text()).toContain('賣家已交寄')

    pendingEvents.resolve({ data: [{ shipmentEventId: 1, eventType: 'HANDED_OVER' }] })
    await flushPromises()
    wrapper.unmount()
  })

  test('keeps the order detail visible while refreshing after delivery confirmation', async () => {
    const availableOrder = orderFixture({ shipment: { status: 'AVAILABLE_FOR_PICKUP' } })
    const completedOrder = orderFixture({ status: 'COMPLETED', shipment: { status: 'DELIVERED' } })
    const pendingRefresh = deferred()
    getOrder
      .mockResolvedValueOnce({ data: availableOrder })
      .mockImplementationOnce(() => pendingRefresh.promise)
    confirmDelivery.mockResolvedValue({ data: null })
    const wrapper = mount(OrderDetail)
    await flushPromises()

    await wrapper.get('button.btn-primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.state-card').exists()).toBe(false)
    expect(wrapper.get('button.btn-primary').text()).toBe('確認中...')

    pendingRefresh.resolve({ data: completedOrder })
    await flushPromises()
    wrapper.unmount()
  })

  test('shows cancellation for an unshipped cash-on-delivery order being processed', async () => {
    getOrder.mockResolvedValue({
      data: orderFixture({
        status: 'PROCESSING',
        payment: {
          status: 'PENDING',
          paymentMethodCode: 'CASH_ON_DELIVERY',
          paymentMethodName: '貨到付款',
        },
        shipment: { status: 'PREPARING' },
      }),
    })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    expect(wrapper.get('button.cancel-order-button').text()).toBe('取消訂單')
    wrapper.unmount()
  })

  test('retries a pending credit-card payment and redirects to ECPay', async () => {
    getOrder.mockResolvedValue({
      data: orderFixture({
        status: 'PENDING_PAYMENT',
        shipment: null,
        payment: {
          status: 'PENDING',
          paymentMethodCode: 'CREDIT_CARD',
          paymentMethodName: '信用卡',
        },
      }),
    })
    const checkout = { action: 'https://payment.example.test', fields: { MerchantTradeNo: 'PAY-2' } }
    createPayment.mockResolvedValue({ data: { paymentId: 21, ecpayCheckout: checkout } })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    expect(wrapper.get('routerlink.payment-back-button').text()).toBe('返回訂單列表')
    await wrapper.get('button.retry-payment-button').trigger('click')
    await flushPromises()

    expect(createPayment).toHaveBeenCalledWith(10, 'CREDIT_CARD')
    expect(submitEcpayCheckout).toHaveBeenCalledWith(checkout)
    wrapper.unmount()
  })

  test('collects a cancellation reason in a modal before cancelling the order', async () => {
    const cancellableOrder = orderFixture({ status: 'PENDING_PAYMENT', shipment: null })
    getOrder.mockResolvedValue({ data: cancellableOrder })
    cancelOrder.mockResolvedValue({
      data: orderFixture({ status: 'CANCELLED', shipment: null, cancelReason: '改變心意' }),
    })
    const wrapper = mount(OrderDetail)
    await flushPromises()

    await wrapper.get('button.cancel-order-button').trigger('click')
    expect(wrapper.get('[role="dialog"]').text()).toContain('取消此筆訂單？')
    await wrapper.get('#cancellation-reason').setValue('改變心意')
    await wrapper.get('[role="dialog"]').trigger('submit')
    await flushPromises()

    expect(cancelOrder).toHaveBeenCalledWith(10, { reason: '改變心意' })
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('此訂單已取消')
    wrapper.unmount()
  })

  test('keeps the confirmed state when an older silent refresh resolves later', async () => {
    let resolveStaleRefresh
    const availableOrder = orderFixture({
      shipment: { status: 'AVAILABLE_FOR_PICKUP' },
    })
    const completedOrder = orderFixture({
      status: 'COMPLETED',
      shipment: { status: 'DELIVERED' },
    })
    getOrder
      .mockResolvedValueOnce({ data: availableOrder })
      .mockReturnValueOnce(new Promise((resolve) => { resolveStaleRefresh = resolve }))
      .mockResolvedValueOnce({ data: completedOrder })
    confirmDelivery.mockResolvedValue({ data: null })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    window.dispatchEvent(new Event('focus'))
    await Promise.resolve()
    await wrapper.get('button.btn-primary').trigger('click')
    await flushPromises()

    expect(confirmDelivery).toHaveBeenCalledWith(10)
    expect(getOrder).toHaveBeenCalledTimes(3)
    expect(wrapper.find('button.btn-primary').exists()).toBe(false)

    resolveStaleRefresh({ data: availableOrder })
    await flushPromises()

    expect(wrapper.find('button.btn-primary').exists()).toBe(false)
    expect(wrapper.findAll('.progress-step').every((step) => step.classes().includes('complete'))).toBe(true)
    wrapper.unmount()
  })
})
