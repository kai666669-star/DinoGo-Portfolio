import api from './axios.js'

const SELLER_LOGO_UPLOAD_TIMEOUT_MS = 60000

export const getSellerProfile = () => {
  return api.get('/seller/profile')
}

export const updateSellerProfile = (payload) => {
  return api.put('/seller/profile', payload)
}

export const getPublicStore = (sellerId) => {
  return api.get(`/stores/${sellerId}`)
}

export const getPublicStoreSummary = (sellerId) => {
  return api.get(`/stores/${sellerId}/summary`)
}
export const uploadSellerLogo = (file) => {
  const formData = new FormData()
  formData.append('file', file)

  return api.post('/seller/profile/logo', formData, {
    timeout: SELLER_LOGO_UPLOAD_TIMEOUT_MS,
  })
}

export const resolveSellerLogoUrl = (url) => {
  if (!url) {
    return ''
  }

  return url.startsWith('https://res.cloudinary.com/') ? url : ''
}
