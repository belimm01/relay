import { defineStore } from 'pinia'
import { ref } from 'vue'
import { clearApiKey, getApiKey, setApiKey } from '@/api/session'

export const useAuthStore = defineStore('auth', () => {
  const apiKey = ref<string | null>(getApiKey())

  function signIn(key: string) {
    const trimmed = key.trim()
    setApiKey(trimmed)
    apiKey.value = trimmed
  }

  function signOut() {
    clearApiKey()
    apiKey.value = null
  }

  return { apiKey, signIn, signOut }
})
