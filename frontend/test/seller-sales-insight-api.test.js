import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ analyzeSellerSalesInsight, getSellerSalesInsight }, { default: api }] = await Promise.all([
  import('../src/api/sellerSalesInsightApi.js'),
  import('../src/api/axios.js'),
])

test('getSellerSalesInsight reads sales insight stats with a date range', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { stats: { orderCount: 3 }, ai: null },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const params = { startDate: '2026-08-23', endDate: '2026-08-29' }
  const response = await getSellerSalesInsight(params)

  assert.equal(request.method, 'get')
  assert.equal(request.url, '/seller/sales-insight')
  assert.deepEqual(request.params, params)
  assert.equal(response.data.stats.orderCount, 3)
  assert.equal(response.data.ai, null)
})

test('analyzeSellerSalesInsight requests AI analysis only on demand', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { ai: { summary: '銷售穩定', recommendations: ['維持出貨效率'] } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const params = { startDate: '2026-08-23', endDate: '2026-08-29' }
  const response = await analyzeSellerSalesInsight(params)

  assert.equal(request.method, 'post')
  assert.equal(request.url, '/seller/sales-insight/analyze')
  assert.deepEqual(request.params, params)
  assert.equal(response.data.ai.summary, '銷售穩定')
})
