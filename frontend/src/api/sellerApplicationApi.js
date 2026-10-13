import api from './axios.js'

export const submitSellerApplication = (payload) =>
  api.post('/member/seller-applications', payload)

export const getMySellerApplication = () => api.get('/member/seller-applications/me')

export const listSellerApplications = (status) =>
  api.get('/admin/seller-applications', { params: status ? { status } : {} })

export const getSellerApplication = (applicationId) =>
  api.get(`/admin/seller-applications/${applicationId}`)

export const approveSellerApplication = (applicationId) =>
  api.post(`/admin/seller-applications/${applicationId}/approve`)

export const rejectSellerApplication = (applicationId, rejectReason) =>
  api.post(`/admin/seller-applications/${applicationId}/reject`, { rejectReason })
