import api from './axios'

export function login(request) {
  return api.post('/auth/login', request)
}

export function googleLogin(request) {
  return api.post('/auth/google', request)
}

export function linkGoogleAccount(request) {
  return api.post('/auth/google/link', request)
}

export function register(request) {
  return api.post('/auth/register', request)
}

export function requestPasswordReset(request) {
  return api.post('/auth/password-reset-requests', request)
}

export function resetPassword(request) {
  return api.post('/auth/password-resets', request)
}
