import assert from 'node:assert/strict'
import test from 'node:test'

import { getSafeErrorSummary, logSafeError } from '../src/utils/safeError.js'

test('safe error summary excludes Axios request data and authorization headers', () => {
  const error = {
    code: 'ERR_BAD_RESPONSE',
    message: 'Request failed with status code 502',
    config: {
      headers: { Authorization: 'Bearer jwt-must-not-be-logged' },
    },
    request: { sensitiveRequestData: true },
    response: {
      status: 502,
      data: { message: 'Payment service unavailable' },
      config: { headers: { Authorization: 'Bearer response-token' } },
    },
  }
  error.circularReference = error

  const summary = getSafeErrorSummary(error)

  assert.deepEqual(summary, {
    status: 502,
    code: 'ERR_BAD_RESPONSE',
    message: 'Payment service unavailable',
  })
  assert.equal(JSON.stringify(summary).includes('jwt-must-not-be-logged'), false)
  assert.equal(JSON.stringify(summary).includes('response-token'), false)
})

test('safe error logger redacts credentials embedded in messages', () => {
  const jwt = 'eyJhbGciOiJIUzI1NiJ9.payload.signature'
  const calls = []

  logSafeError(
    'request failed',
    {
      code: `Bearer code-secret`,
      message: `Authorization failed for Bearer access-secret and ${jwt}`,
    },
    (...args) => calls.push(args),
  )

  assert.deepEqual(calls, [
    [
      'request failed',
      {
        status: null,
        code: 'Bearer [REDACTED]',
        message: 'Authorization failed for Bearer [REDACTED] and [REDACTED_JWT]',
      },
    ],
  ])
})

test('safe error summary limits untrusted message length', () => {
  const summary = getSafeErrorSummary({ message: 'x'.repeat(1000) })

  assert.equal(summary.message.length, 500)
})
