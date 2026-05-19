import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getInfo, login, logout, type AdminProfile, type LoginPayload } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const profile = ref<AdminProfile | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => profile.value?.nickname || profile.value?.username || '管理员')

  async function loginAction(payload: LoginPayload) {
    const { data } = await login(payload)
    token.value = data.data.token
    profile.value = data.data.adminInfo
    initialized.value = true
    setToken(data.data.token)
  }

  async function fetchProfile() {
    if (!token.value) {
      profile.value = null
      initialized.value = true
      return null
    }
    const { data } = await getInfo()
    profile.value = data.data
    initialized.value = true
    return data.data
  }

  async function logoutAction() {
    try {
      if (token.value) {
        await logout()
      }
    } finally {
      resetAuth()
    }
  }

  function resetAuth() {
    token.value = ''
    profile.value = null
    initialized.value = false
    clearToken()
  }

  return {
    token,
    profile,
    initialized,
    isAuthenticated,
    displayName,
    loginAction,
    fetchProfile,
    logoutAction,
    resetAuth,
  }
})
