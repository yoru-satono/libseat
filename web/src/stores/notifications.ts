import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationsApi } from '@/api/notifications'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) return
    try {
      const res = await notificationsApi.unreadCount()
      unreadCount.value = res.data.data.count
    } catch {
      // silent
    }
  }

  function startPolling() {
    fetchUnreadCount()
    if (!pollTimer) {
      pollTimer = setInterval(fetchUnreadCount, 60_000)
    }
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, startPolling, stopPolling }
})
