import api from './axios.js'

export const getSellerInbox = (category, page = 0) =>
  api.get('/sysmsg/seller/inbox', { params: { category, page } })

export const getSellerUnreadCounts = () => api.get('/sysmsg/seller/inbox/unread-counts')

export const getSellerInboxMessage = (recordId) =>
  api.get(`/sysmsg/seller/inbox/${recordId}`)

export const markSellerInboxMessageRead = (recordId) =>
  api.patch(`/sysmsg/seller/inbox/${recordId}/status`, { targetStatus: 'READ' })

export const deleteSellerInboxMessage = (recordId) =>
  api.delete(`/sysmsg/seller/inbox/${recordId}`)

export const createSellerMessage = (payload) => api.post('/sysmsg/seller/messages', payload)

export const getSellerOutbox = (page = 0) =>
  api.get('/sysmsg/seller/messages/outbox', { params: { page } })

export const deleteSellerOutboxMessage = (sendId) =>
  api.delete(`/sysmsg/seller/messages/outbox/${sendId}`)

export const uploadSellerMessageImages = (files) => {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return api.post('/sysmsg/seller/messages/images', formData)
}

export const getSellerTemplates = (page = 0) =>
  api.get('/sysmsg/seller/templates', { params: { page } })

export const createSellerTemplate = (payload) => api.post('/sysmsg/seller/templates', payload)

export const updateSellerTemplate = (sendId, payload) =>
  api.put(`/sysmsg/seller/templates/${sendId}`, payload)

export const deleteSellerTemplate = (sendId) =>
  api.delete(`/sysmsg/seller/templates/${sendId}`)

export const getSellerMessages = ({ category, page = 0, size = 20 } = {}) => {
  return api.get('/seller/messages', {
    params: {
      ...(category && category !== 'ALL' ? { category } : {}),
      page,
      size,
    },
  })
}

export const markSellerMessageRead = (messageId) => {
  return api.patch(`/seller/messages/${messageId}/read`)
}

export const markAllSellerMessagesRead = () => {
  return api.patch('/seller/messages/read-all')
}
