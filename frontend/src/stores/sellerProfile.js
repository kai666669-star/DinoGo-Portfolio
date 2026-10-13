import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getSellerProfile, updateSellerProfile } from '@/api/sellerProfileApi'

export const useSellerProfileStore = defineStore('sellerProfile', () => {
  const profile = ref(null)
  const isLoading = ref(false)
  const hasLoaded = ref(false)
  const error = ref(null)

  const storeName = computed(() => profile.value?.storeName?.trim() || '店鋪名稱')

  function setProfile(nextProfile) {
    profile.value = nextProfile || null
    hasLoaded.value = true
    error.value = null
  }

  async function fetchProfile({ force = false } = {}) {
    if (hasLoaded.value && !force) {
      return profile.value
    }

    isLoading.value = true
    error.value = null

    try {
      const { data } = await getSellerProfile()
      setProfile(data)
      return data
    } catch (fetchError) {
      error.value = fetchError
      throw fetchError
    } finally {
      isLoading.value = false
    }
  }

  async function saveProfile(payload) {
    const { data } = await updateSellerProfile(payload)
    setProfile(data)
    return data
  }

  return {
    profile,
    isLoading,
    hasLoaded,
    error,
    storeName,
    setProfile,
    fetchProfile,
    saveProfile,
  }
})
