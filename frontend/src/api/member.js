import api from './axios'

export function getMemberProfile() {
  return api.get('/member/profile')
}

export function updateMemberProfile(request) {
  return api.put('/member/profile', request)
}

export function changePassword(request) {
  return api.put('/member/password', request)
}

export function deactivateMemberAccount(request) {
  return api.post('/member/account/deactivate', request)
}
