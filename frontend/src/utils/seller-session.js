import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

export const getCurrentSellerId = () => {
  const authStore = useAuthStore(pinia)
  return authStore.sellerId ?? null
}

export const hasCurrentSellerSession = () => {
  const authStore = useAuthStore(pinia)
  return authStore.isSeller && authStore.sellerId !== null
}
