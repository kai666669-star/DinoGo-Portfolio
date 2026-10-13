import api from './axios'
export const listAdminMembers = (params = {}) => api.get('/admin/members', { params })
export const suspendMember = (memberId, reason) => api.post(`/admin/members/${memberId}/suspend`, { reason })
export const restoreMember = (memberId) => api.post(`/admin/members/${memberId}/restore`)
