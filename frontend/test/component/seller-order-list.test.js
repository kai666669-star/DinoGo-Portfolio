import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, test, vi } from 'vitest'

import SellerOrderListView from '../../src/views/seller/SellerOrderListView.vue'
import { getSellerOrders } from '../../src/api/sellerOrderApi.js'

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('../../src/api/sellerOrderApi.js', () => ({
  getSellerOrders: vi.fn(),
}))

afterEach(() => vi.clearAllMocks())

const order = (orderNo) => ({
  orderId: 10,
  orderNo,
  buyerId: 20,
  totalAmount: 1000,
  status: 'PAID',
  createdAt: '2026-08-21T00:00:00Z',
  items: [],
})

describe('seller order list refresh', () => {
  test('refreshes orders when the seller returns to the browser tab', async () => {
    getSellerOrders
      .mockResolvedValueOnce({ data: [order('ORD-OLD')] })
      .mockResolvedValueOnce({ data: [order('ORD-NEW')] })

    const wrapper = mount(SellerOrderListView)
    await flushPromises()
    expect(wrapper.text()).toContain('ORD-OLD')

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(getSellerOrders).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('ORD-NEW')
    wrapper.unmount()
  })
})
