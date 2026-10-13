import { beforeEach, describe, expect, test, vi } from 'vitest'

import { login } from '../../src/api/auth.js'
import { pinia } from '../../src/stores/index.js'
import { useAuthStore } from '../../src/stores/auth.js'
import { getCurrentSellerId } from '../../src/utils/seller-session.js'

vi.mock('../../src/api/auth.js', () => ({
  googleLogin: vi.fn(),
  linkGoogleAccount: vi.fn(),
  login: vi.fn(),
}))

describe('seller auth session', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  test('stores sellerId from login response for seller center pages', async () => {
    login.mockResolvedValue({
      data: {
        token: 'token-1',
        member: { memberId: 7, email: 'seller@example.com' },
        roles: ['seller'],
        sellerId: 42,
      },
    })

    const authStore = useAuthStore(pinia)
    await authStore.signIn({ email: 'seller@example.com', password: 'password' })

    expect(authStore.sellerId).toBe(42)
    expect(authStore.hasSellerSession).toBe(true)
    expect(getCurrentSellerId()).toBe(42)
  })

  test('keeps sellerId null for non-seller login responses', async () => {
    login.mockResolvedValue({
      data: {
        token: 'token-2',
        member: { memberId: 8, email: 'buyer@example.com' },
        roles: ['buyer'],
        sellerId: null,
      },
    })

    const authStore = useAuthStore(pinia)
    await authStore.signIn({ email: 'buyer@example.com', password: 'password' })

    expect(authStore.sellerId).toBeNull()
    expect(authStore.hasSellerSession).toBe(false)
    expect(getCurrentSellerId()).toBeNull()
  })
})
