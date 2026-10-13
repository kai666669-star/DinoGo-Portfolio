import api from './axios.js'

export const MEMBER_UNREAD_CHANGED_EVENT = 'dinogo:member-unread-changed'

export const announceMemberUnreadChanged = () => {
  window.dispatchEvent(new CustomEvent(MEMBER_UNREAD_CHANGED_EVENT))
}

export const getMemberInbox = (category, page = 0) =>
  api.get('/sysmsg/member/inbox', { params: { category, page } })

export const getMemberUnreadCount = () =>
  api.get('/sysmsg/member/inbox/unread-count')

export const getMemberMessage = (recordId) =>
  api.get(`/sysmsg/member/inbox/${recordId}`)

export const markMemberMessageRead = (recordId) =>
  api.patch(`/sysmsg/member/inbox/${recordId}/status`, { targetStatus: 'READ' })

export const deleteMemberMessage = (recordId) =>
  api.delete(`/sysmsg/member/inbox/${recordId}`)
