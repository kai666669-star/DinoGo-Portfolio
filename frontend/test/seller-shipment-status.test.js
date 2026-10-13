import assert from 'node:assert/strict'
import test from 'node:test'
import { ref } from 'vue'

import { useSellerShipmentStatus } from '../src/views/seller/useSellerShipmentStatus.js'

const preparingShipment = () => ({ shipmentId: 3, status: 'PREPARING' })

function setup(orderValue, overrides = {}) {
  const order = ref(orderValue)
  const calls = []
  const updateStatus = overrides.updateStatus ?? (async (status) => {
    calls.push(status)
    return { shipmentId: 3, status, shippedAt: '2026-08-19T10:00:00' }
  })
  const state = useSellerShipmentStatus({
    order,
    updateStatus,
  })
  return { calls, order, ...state }
}

test('paid order with a preparing shipment exposes confirm shipment for legacy orders', () => {
  const state = setup({ status: 'PAID', shipment: preparingShipment() })

  assert.equal(state.shipmentAction.value?.status, 'SHIPPED')
})

test('processing order with preparing shipment exposes confirm shipment', () => {
  const state = setup({ status: 'PROCESSING', shipment: preparingShipment() })

  assert.equal(state.shipmentAction.value?.status, 'SHIPPED')
  assert.equal(state.shipmentAction.value?.label, '確認出貨')
})

test('successful shipment confirmation updates shipment and order status', async () => {
  const state = setup({ status: 'PROCESSING', shipment: preparingShipment() })

  await state.updateShipmentStatus()

  assert.deepEqual(state.calls, ['SHIPPED'])
  assert.equal(state.order.value.shipment.status, 'SHIPPED')
  assert.equal(state.order.value.status, 'SHIPPED')
  assert.equal(state.shipmentActionError.value, '')
})

test('repeated clicks send only one request while shipment update is pending', async () => {
  let resolveUpdate
  let callCount = 0
  const pendingUpdate = new Promise((resolve) => { resolveUpdate = resolve })
  const state = setup(
    { status: 'PROCESSING', shipment: preparingShipment() },
    {
      updateStatus: async () => {
        callCount += 1
        return pendingUpdate
      },
    },
  )

  const firstUpdate = state.updateShipmentStatus()
  const secondUpdate = state.updateShipmentStatus()

  assert.equal(callCount, 1)
  assert.equal(state.updatingShipment.value, true)
  resolveUpdate({ shipmentId: 3, status: 'SHIPPED' })
  await Promise.all([firstUpdate, secondUpdate])
  assert.equal(state.updatingShipment.value, false)
})

test('failed shipment update preserves state and exposes the API error', async () => {
  const originalShipment = preparingShipment()
  const state = setup(
    { status: 'PROCESSING', shipment: originalShipment },
    {
      updateStatus: async () => {
        throw { response: { data: { message: '物流狀態衝突' } } }
      },
    },
  )

  await state.updateShipmentStatus()

  assert.equal(state.order.value.status, 'PROCESSING')
  assert.deepEqual(state.order.value.shipment, originalShipment)
  assert.equal(state.shipmentActionError.value, '物流狀態衝突')
  assert.equal(state.updatingShipment.value, false)
})

test('shipped shipment does not expose a manual available-for-pickup action', async () => {
  const state = setup({
    status: 'SHIPPED',
    shipment: { shipmentId: 3, status: 'SHIPPED' },
  })

  assert.equal(state.shipmentAction.value, null)
  await state.updateShipmentStatus()
  assert.deepEqual(state.calls, [])
  assert.equal(state.order.value.shipment.status, 'SHIPPED')
  assert.equal(state.order.value.status, 'SHIPPED')
})
