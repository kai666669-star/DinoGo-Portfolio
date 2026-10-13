import assert from 'node:assert/strict'
import test from 'node:test'

import {
  getOrderDisplayStatus,
  isOrderInDisplayGroup,
} from '../src/utils/orderDisplayStatus.js'

const cases = [
  [{ status: 'PENDING_PAYMENT' }, 'PENDING_PAYMENT', '待付款'],
  [{ status: 'PAID' }, 'PENDING_SHIPMENT', '待出貨'],
  [{ status: 'PROCESSING', payment: { status: 'PENDING' } }, 'PENDING_SHIPMENT', '待出貨'],
  [{ status: 'SHIPPED', shipment: { status: 'SHIPPED' } }, 'IN_TRANSIT', '運送中'],
  [
    { status: 'SHIPPED', shipment: { status: 'AVAILABLE_FOR_PICKUP' } },
    'PENDING_PICKUP',
    '待收貨',
  ],
  [{ status: 'COMPLETED', shipment: { status: 'DELIVERED' } }, 'COMPLETED', '已完成'],
  [{ status: 'CANCELLED', shipment: { status: 'SHIPPED' } }, 'CANCELLED', '不成立'],
]

for (const [order, expectedKey, expectedLabel] of cases) {
  test(`${order.status}/${order.shipment?.status ?? 'NONE'} displays ${expectedLabel}`, () => {
    assert.deepEqual(getOrderDisplayStatus(order), {
      key: expectedKey,
      label: expectedLabel,
    })
  })
}

test('pending receipt groups every active delivery state', () => {
  for (const status of ['SHIPPED', 'AVAILABLE_FOR_PICKUP', 'DELIVERED']) {
    assert.equal(
      isOrderInDisplayGroup(
        { status: 'SHIPPED', shipment: { status } },
        'PENDING_RECEIPT',
      ),
      true,
    )
  }
  assert.equal(
    isOrderInDisplayGroup(
      { status: 'COMPLETED', shipment: { status: 'DELIVERED' } },
      'PENDING_RECEIPT',
    ),
    false,
  )
  assert.equal(isOrderInDisplayGroup({ status: 'PROCESSING' }, 'PENDING_RECEIPT'), false)
})
