import api from './axios.js'

export const getSystemTemplates = (page = 0) =>
  api.get('/sysmsg/system/templates', { params: { page } })

export const createSystemTemplate = (payload) =>
  api.post('/sysmsg/system/templates', payload)

export const createSystemMessage = (payload) =>
  api.post('/sysmsg/system/messages', payload)

export const getSystemRecords = (page = 0) =>
  api.get('/sysmsg/system/messages/records', { params: { page } })

export const updateSystemTemplate = (sendId, payload) =>
  api.put(`/sysmsg/system/templates/${sendId}`, payload)

export const deleteSystemTemplate = (sendId) =>
  api.delete(`/sysmsg/system/templates/${sendId}`)
