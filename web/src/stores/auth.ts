import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import type { UserProfile } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const currentUser = ref<UserProfile | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  function clearTokens() {
    accessToken.value = null
    refreshToken.value = null
    currentUser.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function login(userNo: string, password: string) {
    const res = await authApi.login(userNo, password)
    const { accessToken: at, refreshToken: rt } = res.data.data
    setTokens(at, rt)
    await fetchCurrentUser()
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearTokens()
    }
  }

  async function fetchCurrentUser() {
    if (!accessToken.value) return
    const res = await usersApi.getMe()
    currentUser.value = res.data.data
  }

  return {
    accessToken,
    refreshToken,
    currentUser,
    isAuthenticated,
    isAdmin,
    setTokens,
    clearTokens,
    login,
    logout,
    fetchCurrentUser,
  }
})
