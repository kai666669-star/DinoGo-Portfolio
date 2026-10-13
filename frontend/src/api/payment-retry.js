const RETRYABLE_STATUS_CODES = new Set([502, 503, 504])
const RETRYABLE_NETWORK_CODES = new Set(['ECONNABORTED', 'ERR_NETWORK', 'ETIMEDOUT'])

export function isRetryablePaymentCreationError(error) {
  if (RETRYABLE_STATUS_CODES.has(error?.response?.status)) {
    return true
  }

  return (
    !error?.response &&
    (Boolean(error?.request) || RETRYABLE_NETWORK_CODES.has(error?.code))
  )
}

export async function retryPaymentCreationOnce(createPaymentRequest) {
  try {
    return await createPaymentRequest()
  } catch (error) {
    if (!isRetryablePaymentCreationError(error)) {
      throw error
    }
    return createPaymentRequest()
  }
}
