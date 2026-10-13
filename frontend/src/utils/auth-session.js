// Keep the persisted auth storage key in one place for the store and Axios.
export const AUTH_STORAGE_KEY = 'auth'

export function readPersistedAuth() {
  const rawAuth = sessionStorage.getItem(AUTH_STORAGE_KEY)
  if (!rawAuth) return null

  try {
    return JSON.parse(rawAuth)
  } catch {
    clearPersistedAuth()
    return null
  }
}

export function getPersistedToken() {
  return readPersistedAuth()?.token || ''
}

export function clearPersistedAuth() {
  sessionStorage.removeItem(AUTH_STORAGE_KEY)
}
