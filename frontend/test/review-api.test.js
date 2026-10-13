import assert from 'node:assert/strict'
import test from 'node:test'
import api from '../src/api/axios.js'
import {
  clearStar,
  getOrderStars,
  updateStar,
  uploadReviewImages,
} from '../src/api/review.js'

test('review API uses the completed-order item review contract', async (t) => {
  const originalGet = api.get
  const originalPost = api.post
  const originalPut = api.put
  const originalDelete = api.delete
  const calls = []

  api.get = async (...args) => calls.push(['get', ...args])
  api.post = async (...args) => calls.push(['post', ...args])
  api.put = async (...args) => calls.push(['put', ...args])
  api.delete = async (...args) => calls.push(['delete', ...args])
  t.after(() => {
    api.get = originalGet
    api.post = originalPost
    api.put = originalPut
    api.delete = originalDelete
  })

  const file = new File(['image'], 'review.png', { type: 'image/png' })
  const request = { fiveStar: 5, feedback: '很好' }
  await getOrderStars(12)
  await uploadReviewImages([file])
  await updateStar(34, request)
  await clearStar(34)

  assert.equal(calls[0][1], '/reviews/orders/12/stars')
  assert.equal(calls[1][1], '/reviews/stars/images')
  assert.ok(calls[1][2] instanceof FormData)
  assert.equal(calls[1][2].get('files').name, 'review.png')
  assert.deepEqual(calls[2], ['put', '/reviews/stars/34', request])
  assert.deepEqual(calls[3], ['delete', '/reviews/stars/34/content'])
})
