import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ uploadSellerLogo }, { default: api }] = await Promise.all([
  import('../src/api/sellerProfileApi.js'),
  import('../src/api/axios.js'),
])

test('uploadSellerLogo uses a longer timeout for Cloudinary uploads', async (context) => {
  const originalAdapter = api.defaults.adapter
  let capturedRequest
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async (config) => {
    capturedRequest = config
    return {
      data: { storeLogoUrl: 'https://res.cloudinary.com/demo/image/upload/logo.png' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const file = new File(['logo'], 'logo.png', { type: 'image/png' })
  await uploadSellerLogo(file)

  assert.equal(capturedRequest.method, 'post')
  assert.equal(capturedRequest.url, '/seller/profile/logo')
  assert.equal(capturedRequest.timeout, 60000)
  assert.ok(capturedRequest.data instanceof FormData)
})
