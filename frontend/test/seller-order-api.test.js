import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [
  { createSellerShipment, updateSellerShipmentStatus, updateSellerShipmentTrackingInfo },
  { default: api },
] = await Promise.all([
  import('../src/api/sellerOrderApi.js'),
  import('../src/api/axios.js'),
])

test('createSellerShipment posts shipment details for an order', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { shipmentId: 3, status: 'PREPARING' },
      status: 201,
      statusText: 'Created',
      headers: {},
      config,
    }
  }

  const shipment = { carrierName: '黑貓宅急便', trackingNo: 'TRACK-1' }
  const response = await createSellerShipment(10, shipment)

  assert.equal(request.method, 'post')
  assert.equal(request.url, '/orders/10/shipment')
  assert.deepEqual(JSON.parse(request.data), shipment)
  assert.equal(response.data.status, 'PREPARING')
})

test('updateSellerShipmentStatus patches the next shipment status', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { shipmentId: 3, status: 'SHIPPED' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const response = await updateSellerShipmentStatus(10, 'SHIPPED')

  assert.equal(request.method, 'patch')
  assert.equal(request.url, '/orders/10/shipment/status')
  assert.deepEqual(JSON.parse(request.data), { status: 'SHIPPED' })
  assert.equal(response.data.status, 'SHIPPED')
})

test('updateSellerShipmentTrackingInfo patches editable shipment details', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { shipmentId: 3, status: 'PREPARING' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const shipment = { carrierName: '新竹物流', trackingNo: 'NEW-001' }
  await updateSellerShipmentTrackingInfo(10, shipment)

  assert.equal(request.method, 'patch')
  assert.equal(request.url, '/orders/10/shipment/tracking-info')
  assert.deepEqual(JSON.parse(request.data), shipment)
})
