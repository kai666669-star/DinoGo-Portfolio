import axios from 'axios'
import { clearPersistedAuth, getPersistedToken } from '../utils/auth-session.js'

const api = axios.create({
  baseURL: import.meta.env?.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = getPersistedToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || ''

    if (
      error.response?.status === 401 &&
      !requestUrl.startsWith('/auth/')
    ) {
      clearPersistedAuth()
      window.location.href = '/login'
    }

    return Promise.reject(error)
  },
)

export default api
