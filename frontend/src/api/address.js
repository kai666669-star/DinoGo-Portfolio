import api from './axios'

// 取得目前登入會員的所有收件地址。
export function getAddresses() {
  return api.get('/addresses')
}

// 新增目前登入會員的收件地址。
export function createAddress(request) {
  return api.post('/addresses', request)
}

// 修改目前登入會員擁有的收件地址。
export function updateAddress(addressId, request) {
  return api.put(`/addresses/${addressId}`, request)
}

// 刪除目前登入會員擁有的收件地址。
export function deleteAddress(addressId) {
  return api.delete(`/addresses/${addressId}`)
}
