import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'

const {
  apiMock,
  createPaymentMock,
  getPaymentCapabilitiesMock,
  getPaymentMethodsMock,
  pushMock,
  simulatePaymentMock,
  submitEcpayCheckoutMock,
} = vi.hoisted(() => ({
  apiMock: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
  createPaymentMock: vi.fn(),
  getPaymentCapabilitiesMock: vi.fn(),
  getPaymentMethodsMock: vi.fn(),
  pushMock: vi.fn(),
  simulatePaymentMock: vi.fn(),
  submitEcpayCheckoutMock: vi.fn(),
}))

vi.mock('../../src/api/axios.js', () => ({ default: apiMock }))
vi.mock('../../src/api/order.js', () => ({
  createPayment: createPaymentMock,
  getPaymentCapabilities: getPaymentCapabilitiesMock,
  getPaymentMethods: getPaymentMethodsMock,
  simulatePayment: simulatePaymentMock,
  submitEcpayCheckout: submitEcpayCheckoutMock,
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

import CheckoutView from '../../src/views/cart/CheckoutView.vue'

const checkoutData = {
  sellerId: 30,
  items: [
    {
      cartItemId: 40,
      sellerId: 30,
      skuId: 50,
      quantity: 1,
      productName: 'Test product',
      price: 1000,
    },
  ],
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  localStorage.setItem('checkoutData', JSON.stringify(checkoutData))

  apiMock.get.mockImplementation(async (url) => {
    if (url === '/addresses') {
      return {
        data: [
          {
            addressId: 60,
            receiverName: 'Buyer',
            receiverPhone: '0912345678',
            city: 'Taipei',
            district: 'Zhongzheng',
            detailAddress: 'No. 1',
            isDefault: true,
          },
        ],
      }
    }
    if (url === '/member/coupons') return { data: [] }
    throw new Error(`Unexpected GET ${url}`)
  })
  apiMock.post.mockImplementation(async (url) => {
    if (url === '/checkout/preview') {
      return {
        data: {
          subtotal: 1000,
          shippingFee: 0,
          discount: 0,
          totalAmount: 1000,
        },
      }
    }
    if (url === '/orders') return { data: { orderId: 10 } }
    throw new Error(`Unexpected POST ${url}`)
  })
  apiMock.delete.mockResolvedValue({})
  createPaymentMock.mockResolvedValue({ data: { paymentId: 20, status: 'PENDING' } })
  getPaymentCapabilitiesMock.mockResolvedValue({ data: { simulationEnabled: false } })
  getPaymentMethodsMock.mockResolvedValue({
    data: [
      { paymentMethodCode: 'CASH_ON_DELIVERY', paymentMethodName: '貨到付款' },
      { paymentMethodCode: 'CREDIT_CARD', paymentMethodName: '信用卡' },
      { paymentMethodCode: 'LINE_PAY', paymentMethodName: 'LINE Pay' },
    ],
  })
  simulatePaymentMock.mockResolvedValue({ data: { paymentId: 20, status: 'SUCCESS' } })
})

describe('checkout payment flow', () => {
  test('keeps the credit-card option available when payment simulation is disabled', async () => {
    const wrapper = mount(CheckoutView)
    await flushPromises()

    expect(getPaymentCapabilitiesMock).toHaveBeenCalledTimes(1)
    expect(wrapper.get('input[value="CASH_ON_DELIVERY"]').exists()).toBe(true)
    expect(wrapper.find('input[value="CREDIT_CARD"]').exists()).toBe(true)
  })

  test('keeps the credit-card option available when loading capabilities fails', async () => {
    getPaymentCapabilitiesMock.mockRejectedValue(new Error('Payment capabilities are unavailable'))
    const wrapper = mount(CheckoutView)
    await flushPromises()

    expect(wrapper.get('input[value="CASH_ON_DELIVERY"]').exists()).toBe(true)
    expect(wrapper.find('input[value="CREDIT_CARD"]').exists()).toBe(true)
  })

  test('creates and simulates an online payment', async () => {
    getPaymentCapabilitiesMock.mockResolvedValue({ data: { simulationEnabled: true } })
    const wrapper = mount(CheckoutView)
    await flushPromises()

    await wrapper.get('input[value="CREDIT_CARD"]').setValue()
    await flushPromises()
    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')
    await flushPromises()

    expect(createPaymentMock).toHaveBeenCalledWith(10, 'CREDIT_CARD')
    expect(simulatePaymentMock).toHaveBeenCalledWith(10, 20, 'SUCCESS', null)
    expect(pushMock).toHaveBeenCalledWith({
      name: 'MemberOrderDetail',
      params: { id: 10 },
    })
  })

  test('submits the ECPay hosted checkout form instead of simulating a card payment', async () => {
    createPaymentMock.mockResolvedValue({
      data: {
        paymentId: 20,
        status: 'PENDING',
        ecpayCheckout: {
          action: 'https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5',
          fields: { MerchantTradeNo: 'PAY260825120000ABCDE' },
        },
      },
    })
    const wrapper = mount(CheckoutView)
    await flushPromises()

    await wrapper.get('input[value="CREDIT_CARD"]').setValue()
    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')
    await flushPromises()

    expect(submitEcpayCheckoutMock).toHaveBeenCalledWith({
      action: 'https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5',
      fields: { MerchantTradeNo: 'PAY260825120000ABCDE' },
    })
    expect(simulatePaymentMock).not.toHaveBeenCalled()
  })

  test('locks and snapshots the payment method while order creation is pending', async () => {
    getPaymentCapabilitiesMock.mockResolvedValue({ data: { simulationEnabled: true } })
    let resolveOrderRequest
    const defaultPostImplementation = apiMock.post.getMockImplementation()
    apiMock.post.mockImplementation((url, ...args) => {
      if (url === '/orders') {
        return new Promise((resolve) => {
          resolveOrderRequest = resolve
        })
      }
      return defaultPostImplementation(url, ...args)
    })

    const wrapper = mount(CheckoutView)
    await flushPromises()

    await wrapper.get('input[value="CREDIT_CARD"]').setValue()
    await flushPromises()
    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')

    const paymentInputs = wrapper.findAll('input[name="payment"]')
    expect(paymentInputs.every((input) => input.attributes('disabled') !== undefined)).toBe(true)

    const cashOnDeliveryInput = wrapper.get('input[value="CASH_ON_DELIVERY"]')
    cashOnDeliveryInput.element.checked = true
    await cashOnDeliveryInput.trigger('change')

    resolveOrderRequest({ data: { orderId: 10 } })
    await flushPromises()

    expect(createPaymentMock).toHaveBeenCalledWith(10, 'CREDIT_CARD')
    expect(simulatePaymentMock).toHaveBeenCalledWith(10, 20, 'SUCCESS', null)
  })

  test('prevents repeated orders while creation and navigation are pending', async () => {
    let resolveOrderRequest
    let resolveNavigation
    const defaultPostImplementation = apiMock.post.getMockImplementation()
    apiMock.post.mockImplementation((url, ...args) => {
      if (url === '/orders') {
        return new Promise((resolve) => {
          resolveOrderRequest = resolve
        })
      }
      return defaultPostImplementation(url, ...args)
    })
    pushMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveNavigation = resolve
        }),
    )

    const wrapper = mount(CheckoutView)
    await flushPromises()

    const submitButton = wrapper.get('button.submit-button:not(.stock-error-button)')
    await submitButton.trigger('click')
    await submitButton.trigger('click')

    expect(apiMock.post.mock.calls.filter(([url]) => url === '/orders')).toHaveLength(1)

    resolveOrderRequest({ data: { orderId: 10 } })
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({
      name: 'MemberOrderDetail',
      params: { id: 10 },
    })
    expect(submitButton.attributes('disabled')).toBeDefined()

    await submitButton.trigger('click')
    expect(apiMock.post.mock.calls.filter(([url]) => url === '/orders')).toHaveLength(1)

    resolveNavigation()
    await flushPromises()

    expect(submitButton.attributes('disabled')).toBeDefined()
  })

  test('creates a pending cash-on-delivery payment without simulating success', async () => {
    const wrapper = mount(CheckoutView)
    await flushPromises()

    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')
    await flushPromises()

    expect(createPaymentMock).toHaveBeenCalledWith(10, 'CASH_ON_DELIVERY')
    expect(simulatePaymentMock).not.toHaveBeenCalled()
    expect(wrapper.find('input[value="CREDIT_CARD"]').exists()).toBe(true)
    expect(wrapper.find('input[value="LINE_PAY"]').exists()).toBe(true)
    expect(wrapper.find('.payment-simulation').exists()).toBe(false)
  })

  test('submits a simulated failed online payment with an optional reason', async () => {
    getPaymentCapabilitiesMock.mockResolvedValue({ data: { simulationEnabled: true } })
    const wrapper = mount(CheckoutView)
    await flushPromises()

    await wrapper.get('input[value="CREDIT_CARD"]').setValue()
    await wrapper.get('input[value="FAILED"]').setValue()
    await wrapper.get('.payment-failure-reason input').setValue('銀行授權失敗')
    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')
    await flushPromises()

    expect(simulatePaymentMock).toHaveBeenCalledWith(10, 20, 'FAILED', '銀行授權失敗')
    expect(pushMock).toHaveBeenCalledWith({
      name: 'MemberOrderDetail',
      params: { id: 10 },
    })
  })

  test('logs only a safe summary when payment fails', async () => {
    const paymentError = {
      code: 'ERR_BAD_RESPONSE',
      message: 'Request failed with status code 502',
      config: {
        headers: {
          Authorization: 'Bearer jwt-must-not-be-logged',
        },
      },
      response: {
        status: 502,
        data: { message: 'Payment service unavailable' },
      },
    }
    createPaymentMock.mockRejectedValue(paymentError)
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {})

    const wrapper = mount(CheckoutView)
    await flushPromises()
    await wrapper.get('button.submit-button:not(.stock-error-button)').trigger('click')
    await flushPromises()

    expect(consoleErrorSpy).toHaveBeenCalledWith('付款失敗：', {
      status: 502,
      code: 'ERR_BAD_RESPONSE',
      message: 'Payment service unavailable',
    })
    expect(consoleErrorSpy.mock.calls.some((call) => call.includes(paymentError))).toBe(false)
    expect(JSON.stringify(consoleErrorSpy.mock.calls)).not.toContain('jwt-must-not-be-logged')
    expect(alertSpy).toHaveBeenCalledWith('Payment service unavailable')

    consoleErrorSpy.mockRestore()
    alertSpy.mockRestore()
  })
})
