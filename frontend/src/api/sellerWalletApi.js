import api from './axios'

export const getSellerWallet = () => {
  return api.get('/seller/wallet')
}

export const getSellerWalletTransactions = (params = {}) => {
  return api.get('/seller/wallet/transactions', { params })
}

export const createSellerWalletWithdrawal = () => {
  return api.post('/seller/wallet/withdrawals')
}
