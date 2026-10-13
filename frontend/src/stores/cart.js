import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import api from '@/api/axios'

export const useCartStore = defineStore('cart', () => {
  // Cart state is shared by CartView and the global Header badge.
  const cart = ref(null)

  const totalQuantity = computed(() => {
    const items = cart.value?.items || []
    const skuIds = new Set(items.map((item) => item.skuId).filter((skuId) => skuId != null))
    return skuIds.size
  })

  const totalAmount = computed(
    () =>
      cart.value?.items?.reduce(
        (total, item) => total + Number(item.price) * Number(item.quantity),
        0,
      ) || 0,
  )

  async function fetchCart() {
    // axios.js owns the API base URL, so the store only supplies the endpoint.
    const { data } = await api.get('/cart')
    cart.value = data
    return data
  }

  async function updateQuantity(item, quantity) {
    if (quantity < 1) return

    await api.put(`/cart/items/${item.cartItemId}`, { quantity })
    item.quantity = quantity
  }

  async function removeItem(item) {
    await api.delete(`/cart/items/${item.cartItemId}`)
    cart.value.items = cart.value.items.filter(
      (cartItem) => cartItem.cartItemId !== item.cartItemId,
    )
  }

  return {
    cart,
    totalQuantity,
    totalAmount,
    fetchCart,
    updateQuantity,
    removeItem,
  }
})
