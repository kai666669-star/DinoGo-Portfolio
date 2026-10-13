import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { googleLogin, linkGoogleAccount, login } from '@/api/auth'
import { AUTH_STORAGE_KEY, clearPersistedAuth } from '@/utils/auth-session'

export const useAuthStore = defineStore('auth', () => {
  // The persistence plugin restores these values from sessionStorage.
  const token = ref('')
  const member = ref(null)
  const roles = ref([])
  const sellerId = ref(null)

  // Components use this getter instead of reading localStorage directly.
  const isAuthenticated = computed(() => Boolean(token.value))

  const normalizedRoles = computed(() =>
    roles.value.map((role) => String(role).toLowerCase()),
  )

  const hasRole = (role) =>
    Boolean(role) && normalizedRoles.value.includes(String(role).toLowerCase())

  const isSeller = computed(() => hasRole('seller'))
  const hasSellerSession = computed(() => isSeller.value && sellerId.value !== null)

  const memberName = computed(() => {
    if (!member.value) return ''
    const fullName = [member.value.lastName, member.value.firstName]
      .filter(Boolean)
      .join('')
    return fullName || member.value.email || ''
  })

  function setSession(sessionToken, sessionMember, sessionRoles = [], sessionSellerId = null) {
    token.value = sessionToken || ''
    member.value = sessionMember || null
    roles.value = Array.isArray(sessionRoles) ? [...sessionRoles] : []
    sellerId.value = sessionSellerId ?? null
    if (!sessionToken) {
      clearPersistedAuth()
    }
  }

  async function signIn(credentials) {
    // Keep API and persistence logic in the store; the view handles UI only.
    const { data } = await login(credentials)
    setSession(data.token, data.member, data.roles, data.sellerId)
    return data
  }

  async function signInWithGoogle(credential) {
    const { data } = await googleLogin({ credential })
    setSession(data.token, data.member, data.roles, data.sellerId)
    return data
  }

  async function linkGoogleSignIn(credential, password) {
    const { data } = await linkGoogleAccount({ credential, password })
    setSession(data.token, data.member, data.roles, data.sellerId)
    return data
  }

  function updateMember(updatedMember) {
    // Keep the Header and other components synchronized after profile updates.
    member.value = updatedMember || null
  }

  function signOut() {
    setSession('', null, [], null)
    clearPersistedAuth()
  }

  return {
    token,
    member,
    roles,
    sellerId,
    isAuthenticated,
    isSeller,
    hasSellerSession,
    memberName,
    hasRole,
    signIn,
    signInWithGoogle,
    linkGoogleSignIn,
    signOut,
    updateMember,
  }
}, {
  // Keep only authentication data; page state and cart data are not persisted here.
  persist: {
    key: AUTH_STORAGE_KEY,
    storage: sessionStorage,
    pick: ['token', 'member', 'roles', 'sellerId'],
  },
})
