import assert from 'node:assert/strict'
import test from 'node:test'

import { retryPaymentCreationOnce } from '../src/api/payment-retry.js'

test('retries payment creation once after a network timeout', async () => {
  let attempts = 0
  const response = { data: { paymentId: 20 } }

  const result = await retryPaymentCreationOnce(async () => {
    attempts += 1
    if (attempts === 1) {
      throw { code: 'ECONNABORTED', request: {} }
    }
    return response
  })

  assert.equal(attempts, 2)
  assert.equal(result, response)
})

test('retries payment creation once after a temporary gateway error', async () => {
  let attempts = 0

  await assert.rejects(
    retryPaymentCreationOnce(async () => {
      attempts += 1
      throw { response: { status: 503 } }
    }),
  )

  assert.equal(attempts, 2)
})

test('does not retry a rejected payment request', async () => {
  let attempts = 0
  const error = { response: { status: 400 } }

  await assert.rejects(
    retryPaymentCreationOnce(async () => {
      attempts += 1
      throw error
    }),
    (actual) => actual === error,
  )

  assert.equal(attempts, 1)
})
