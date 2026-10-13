const MAX_ERROR_MESSAGE_LENGTH = 500
const MAX_ERROR_CODE_LENGTH = 100

const redactSensitiveText = (value) =>
  value
    .replace(/Bearer\s+[A-Za-z0-9._~+/=-]+/gi, 'Bearer [REDACTED]')
    .replace(/\beyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g, '[REDACTED_JWT]')

const getSafeString = (value, maxLength) => {
  if (typeof value !== 'string') return null
  return redactSensitiveText(value).slice(0, maxLength)
}

export const getSafeErrorSummary = (error) => {
  const responseMessage = error?.response?.data?.message || error?.response?.data?.error
  const message =
    getSafeString(responseMessage, MAX_ERROR_MESSAGE_LENGTH) ||
    getSafeString(error?.message, MAX_ERROR_MESSAGE_LENGTH) ||
    'Unknown error'

  return {
    status: Number.isInteger(error?.response?.status) ? error.response.status : null,
    code: getSafeString(error?.code, MAX_ERROR_CODE_LENGTH),
    message,
  }
}

export const logSafeError = (context, error, logger = console.error) => {
  logger(context, getSafeErrorSummary(error))
}
