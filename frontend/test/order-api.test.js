import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ createPayment, getPaymentCapabilities, simulatePayment }, { default: api }] = await Promise.all([
  import('../src/api/order.js'),
  import('../src/api/axios.js'),
])

test('createPayment retries once after timeout with the same request', async (context) => {
  const originalAdapter = api.defaults.adapter
  const requests = []
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async (config) => {
    requests.push(config)
    if (requests.length === 1) {
      throw {
        code: 'ECONNABORTED',
        config,
        request: {},
      }
    }

    return {
      data: { paymentId: 20, status: 'PENDING' },
      status: 201,
      statusText: 'Created',
      headers: {},
      config,
    }
  }

  const response = await createPayment(10, 'CREDIT_CARD')

  assert.equal(requests.length, 2)
  for (const request of requests) {
    assert.equal(request.method, 'post')
    assert.equal(request.url, '/orders/10/payments')
    assert.deepEqual(JSON.parse(request.data), { paymentMethodCode: 'CREDIT_CARD' })
    assert.equal(request.headers['Idempotency-Key'], requests[0].headers['Idempotency-Key'])
    assert.match(request.headers['Idempotency-Key'], /^[0-9a-f-]{36}$/i)
  }
  assert.equal(response.data.paymentId, 20)
})

test('simulatePayment posts a successful MVP payment result', async (context) => {
  const originalAdapter = api.defaults.adapter
  let capturedRequest
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async (config) => {
    capturedRequest = config
    return {
      data: { paymentId: 20, status: 'SUCCESS' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const response = await simulatePayment(10, 20)

  assert.equal(capturedRequest.method, 'post')
  assert.equal(capturedRequest.url, '/orders/10/payments/20/simulate')
  assert.deepEqual(JSON.parse(capturedRequest.data), {
    status: 'SUCCESS',
    failureReason: null,
  })
  assert.equal(response.data.status, 'SUCCESS')
})

test('getPaymentCapabilities reads the runtime payment configuration', async (context) => {
  const originalAdapter = api.defaults.adapter
  let capturedRequest
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async (config) => {
    capturedRequest = config
    return {
      data: { simulationEnabled: false },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const response = await getPaymentCapabilities()

  assert.equal(capturedRequest.method, 'get')
  assert.equal(capturedRequest.url, '/payments/capabilities')
  assert.equal(response.data.simulationEnabled, false)
})
