import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import SellerOrderDetailView from '../../src/views/seller/SellerOrderDetailView.vue'
import {
  createSellerShipment,
  getSellerOrder,
  getShipmentEvents,
  simulateTcatEvent,
  updateSellerShipmentTrackingInfo,
  updateSellerShipmentStatus,
} from '../../src/api/sellerOrderApi.js'

enableAutoUnmount(afterEach)

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ params: { id: '10' } }),
}))

vi.mock('../../src/api/sellerOrderApi.js', () => ({
  createSellerShipment: vi.fn(),
  getSellerOrder: vi.fn(),
  getShipmentEvents: vi.fn(),
  simulateTcatEvent: vi.fn(),
  updateSellerShipmentTrackingInfo: vi.fn(),
  updateSellerShipmentStatus: vi.fn(),
}))

const orderFixture = (overrides = {}) => ({
  orderId: 10,
  orderNo: 'ORD-10',
  status: 'PAID',
  createdAt: '2026-08-19T08:00:00Z',
  receiverName: '王小明',
  receiverPhone: '0912345678',
  shippingPostalCode: '100',
  shippingCity: '台北市',
  shippingDistrict: '中正區',
  shippingDetailAddress: '測試路 1 號',
  buyerRemark: null,
  subtotalAmount: 1000,
  shippingFee: 60,
  discountAmount: 0,
  totalAmount: 1060,
  items: [{ orderItemId: 1, productName: '測試商品', skuSpec: '標準', quantity: 1, subtotal: 1000 }],
  payment: { status: 'PAID', paymentMethodCode: 'CREDIT_CARD', paymentMethodName: '信用卡' },
  shipment: null,
  ...overrides,
})

async function mountView(order = orderFixture()) {
  getSellerOrder.mockResolvedValue({ data: order })
  const wrapper = mount(SellerOrderDetailView)
  await flushPromises()
  return wrapper
}

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

describe('seller shipment operation flow', () => {
  test('loads the order, accepts form input, and replaces the form with created shipment data', async () => {
    createSellerShipment.mockResolvedValue({
      data: { shipmentId: 3, status: 'PREPARING', carrierName: '黑貓宅急便', trackingNo: 'TRACK-001' },
    })
    const wrapper = await mountView()

    expect(getSellerOrder).toHaveBeenCalledWith(10)
    await wrapper.get('[name="carrierName"]').setValue('黑貓宅急便')
    await wrapper.get('[name="trackingNo"]').setValue(' TRACK-001 ')
    await wrapper.get('form.shipment-form').trigger('submit')
    await flushPromises()

    expect(createSellerShipment).toHaveBeenCalledWith(10, {
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    })
    expect(wrapper.find('form.shipment-form').exists()).toBe(false)
    expect(wrapper.text()).toContain('TRACK-001')
  })

  test('rejects blank shipment fields before calling the API', async () => {
    const wrapper = await mountView()

    await wrapper.get('[name="carrierName"]').setValue('')
    await wrapper.get('[name="trackingNo"]').setValue('')
    await wrapper.get('form.shipment-form').trigger('submit')
    await flushPromises()

    expect(createSellerShipment).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toBe('物流商與物流單號皆為必填。')
    expect(wrapper.get('[name="carrierName"]').attributes('aria-invalid')).toBe('true')
    expect(wrapper.get('[name="trackingNo"]').attributes('aria-invalid')).toBe('true')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  test('offers approved carriers and updates the tracking number template', async () => {
    const wrapper = await mountView()
    const carrierSelect = wrapper.get('[name="carrierName"]')
    const trackingInput = wrapper.get('[name="trackingNo"]')

    expect(carrierSelect.element.tagName).toBe('SELECT')
    expect(carrierSelect.text()).toContain('黑貓宅急便')
    expect(carrierSelect.text()).toContain('新竹物流')
    expect(carrierSelect.text()).toContain('嘉里大榮物流')

    await carrierSelect.setValue('新竹物流')
    expect(trackingInput.attributes('placeholder')).toBe('範例：1234567890（10 碼）')

    await carrierSelect.setValue('黑貓宅急便')
    expect(trackingInput.attributes('placeholder')).toBe('範例：1234-5678-9012（12 碼）')
  })

  test('confirms shipment through the page controls', async () => {
    const preparingShipment = {
      shipmentId: 3,
      status: 'PREPARING',
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    }
    updateSellerShipmentStatus.mockResolvedValue({
      data: { ...preparingShipment, status: 'SHIPPED', shippedAt: '2026-08-19T10:00:00Z' },
    })
    const wrapper = await mountView(orderFixture({ status: 'PROCESSING', shipment: preparingShipment }))

    const shipmentButton = wrapper.get('button.shipment-submit')
    expect(shipmentButton.text()).toBe('確認出貨')
    await shipmentButton.trigger('click')
    expect(wrapper.get('[role="dialog"]').text()).toContain('確認此筆訂單已出貨？')
    await wrapper.get('[role="dialog"] button.shipment-submit').trigger('click')
    await flushPromises()

    expect(updateSellerShipmentStatus).toHaveBeenCalledWith(10, 'SHIPPED')
    expect(wrapper.text()).toContain('已出貨')
  })

  test('does not expose a manual available-for-pickup action for cash-on-delivery orders', async () => {
    const shippedShipment = {
      shipmentId: 3,
      status: 'SHIPPED',
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    }
    updateSellerShipmentStatus.mockResolvedValue({
      data: {
        ...shippedShipment,
        status: 'AVAILABLE_FOR_PICKUP',
        availablePickupAt: '2026-08-20T08:16:00Z',
      },
    })
    const wrapper = await mountView(orderFixture({
      status: 'SHIPPED',
      payment: {
        status: 'PENDING',
        paymentMethodCode: 'CASH_ON_DELIVERY',
        paymentMethodName: '貨到付款',
      },
      shipment: shippedShipment,
    }))

    let progressItems = wrapper.findAll('.progress-item')
    expect(progressItems[3].classes()).not.toContain('completed')
    expect(wrapper.find('button.shipment-submit').exists()).toBe(false)
    progressItems = wrapper.findAll('.progress-item')
    expect(progressItems.map((item) => item.text())).toEqual([
      '訂單成立',
      '備貨中',
      '已出貨',
      '已送達',
      '已完成',
    ])
    expect(progressItems.slice(0, 3).every((item) => item.classes().includes('completed'))).toBe(true)
    expect(progressItems[3].classes()).not.toContain('completed')
    expect(progressItems[4].classes()).not.toContain('completed')
  })

  test('refreshes the seller progress to completed after the buyer confirms receipt', async () => {
    const cashOnDeliveryPayment = {
      status: 'PENDING',
      paymentMethodCode: 'CASH_ON_DELIVERY',
      paymentMethodName: '貨到付款',
    }
    const availableShipment = {
      shipmentId: 3,
      status: 'AVAILABLE_FOR_PICKUP',
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    }
    getSellerOrder
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'SHIPPED',
          payment: cashOnDeliveryPayment,
          shipment: availableShipment,
        }),
      })
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'COMPLETED',
          payment: { ...cashOnDeliveryPayment, status: 'SUCCESS' },
          shipment: {
            ...availableShipment,
            status: 'DELIVERED',
            deliveredAt: '2026-08-20T08:18:00Z',
          },
        }),
      })

    const wrapper = mount(SellerOrderDetailView)
    await flushPromises()

    let progressItems = wrapper.findAll('.progress-item')
    expect(progressItems[4].classes()).not.toContain('completed')

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(getSellerOrder).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.status-copy > strong').text()).toBe('已完成')
    progressItems = wrapper.findAll('.progress-item')
    expect(progressItems.every((item) => item.classes().includes('completed'))).toBe(true)
    expect(wrapper.text()).toContain('已送達')
    expect(wrapper.find('button.shipment-submit').exists()).toBe(false)
  })

  test('does not expose an order acceptance action in the detail view', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('button.accept-button').exists()).toBe(false)
  })

  test('allows HCT Logistics to simulate shipment progress', async () => {
    getShipmentEvents.mockResolvedValue({ data: [{ eventType: 'HANDED_OVER' }] })
    simulateTcatEvent.mockResolvedValue({ data: {} })
    const wrapper = await mountView(orderFixture({
      status: 'SHIPPED',
      shipment: { shipmentId: 3, status: 'SHIPPED', carrierName: '新竹物流', trackingNo: '1234567890' },
    }))

    await wrapper.get('button.secondary-button').trigger('click')
    await flushPromises()

    expect(simulateTcatEvent).toHaveBeenCalledWith(10, 'IN_TRANSIT')
  })

  test('keeps the next shipment action visible while a silent refresh loads events', async () => {
    getShipmentEvents.mockResolvedValue({ data: [{ eventType: 'HANDED_OVER' }] })
    const wrapper = await mountView(orderFixture({
      status: 'SHIPPED',
      shipment: { shipmentId: 3, status: 'SHIPPED', carrierName: '新竹物流', trackingNo: '1234567890' },
    }))
    const pendingEvents = deferred()
    getShipmentEvents.mockImplementationOnce(() => pendingEvents.promise)

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(wrapper.get('button.secondary-button').text()).toBe('運送中')

    pendingEvents.resolve({ data: [{ eventType: 'HANDED_OVER' }] })
    await flushPromises()
  })

  test('shows the following shipment action before the post-simulation refresh completes', async () => {
    const shipment = { shipmentId: 3, status: 'SHIPPED', carrierName: '新竹物流', trackingNo: '1234567890' }
    getShipmentEvents.mockResolvedValue({ data: [{ eventType: 'HANDED_OVER' }] })
    simulateTcatEvent.mockResolvedValue({ data: shipment })
    const wrapper = await mountView(orderFixture({ status: 'SHIPPED', shipment }))
    const pendingEvents = deferred()
    getShipmentEvents.mockImplementationOnce(() => pendingEvents.promise)

    await wrapper.get('button.secondary-button').trigger('click')
    await flushPromises()

    expect(simulateTcatEvent).toHaveBeenCalledWith(10, 'IN_TRANSIT')
    expect(wrapper.get('button.secondary-button').text()).toContain('配送中')
    expect(wrapper.get('button.secondary-button').attributes('disabled')).toBeUndefined()

    pendingEvents.resolve({ data: [{ eventType: 'HANDED_OVER' }, { eventType: 'IN_TRANSIT' }] })
    await flushPromises()

    expect(wrapper.get('button.secondary-button').text()).toBe('配送中')
    expect(wrapper.get('button.secondary-button').attributes('disabled')).toBeUndefined()
  })

  test('lets the seller revise tracking information instead of confirming shipment', async () => {
    updateSellerShipmentTrackingInfo.mockResolvedValue({
      data: { shipmentId: 3, status: 'PREPARING', carrierName: '新竹物流', trackingNo: 'NEW-001' },
    })
    const wrapper = await mountView(orderFixture({
      status: 'PROCESSING',
      shipment: { shipmentId: 3, status: 'PREPARING', carrierName: '黑貓宅急便', trackingNo: 'OLD-001' },
    }))

    await wrapper.get('button.shipment-submit').trigger('click')
    await wrapper.get('[role="dialog"] button.secondary-button').trigger('click')
    await wrapper.get('[name="carrierName"]').setValue('新竹物流')
    await wrapper.get('[name="trackingNo"]').setValue('NEW-001')
    await wrapper.get('form.shipment-form').trigger('submit')
    await flushPromises()

    expect(updateSellerShipmentTrackingInfo).toHaveBeenCalledWith(10, {
      carrierName: '新竹物流',
      trackingNo: 'NEW-001',
    })
    expect(updateSellerShipmentStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('確認出貨')
  })
})
