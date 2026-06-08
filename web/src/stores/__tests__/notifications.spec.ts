import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/notifications'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/notifications', () => ({
  notificationsApi: {
    unreadCount: vi.fn(),
  },
}))

vi.mock('@/api/auth', () => ({ authApi: { login: vi.fn(), logout: vi.fn() } }))
vi.mock('@/api/users', () => ({ usersApi: { getMe: vi.fn() } }))

const { notificationsApi } = await import('@/api/notifications')

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('初始状态：unreadCount 为 0', () => {
    const store = useNotificationStore()
    expect(store.unreadCount).toBe(0)
  })

  it('fetchUnreadCount：已登录时更新 unreadCount', async () => {
    // 设置已登录状态
    const authStore = useAuthStore()
    authStore.setTokens('at', 'rt')
    authStore.currentUser = { role: 'STUDENT' } as any

    vi.mocked(notificationsApi.unreadCount).mockResolvedValue({
      data: { code: '00000', data: { count: 5 } },
    } as any)

    const store = useNotificationStore()
    await store.fetchUnreadCount()

    expect(store.unreadCount).toBe(5)
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(1)
  })

  it('fetchUnreadCount：未登录时不发请求', async () => {
    const store = useNotificationStore()
    await store.fetchUnreadCount()

    expect(notificationsApi.unreadCount).not.toHaveBeenCalled()
    expect(store.unreadCount).toBe(0)
  })

  it('fetchUnreadCount：API 失败时静默处理，不抛错', async () => {
    const authStore = useAuthStore()
    authStore.setTokens('at', 'rt')
    authStore.currentUser = {} as any

    vi.mocked(notificationsApi.unreadCount).mockRejectedValue(new Error('network'))

    const store = useNotificationStore()
    await expect(store.fetchUnreadCount()).resolves.not.toThrow()
  })

  it('startPolling：每 60 秒调用 fetchUnreadCount', async () => {
    const authStore = useAuthStore()
    authStore.setTokens('at', 'rt')
    authStore.currentUser = {} as any

    vi.mocked(notificationsApi.unreadCount).mockResolvedValue({
      data: { code: '00000', data: { count: 0 } },
    } as any)

    const store = useNotificationStore()
    store.startPolling()

    // 初始调用
    await vi.runAllTicks()
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(1)

    // 推进 60 秒
    await vi.advanceTimersByTimeAsync(60_000)
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(2)

    // 推进再 60 秒
    await vi.advanceTimersByTimeAsync(60_000)
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(3)

    store.stopPolling()
  })

  it('startPolling：重复调用只创建 1 个 interval（不创建多个定时器）', async () => {
    const authStore = useAuthStore()
    authStore.setTokens('at', 'rt')
    authStore.currentUser = {} as any

    vi.mocked(notificationsApi.unreadCount).mockResolvedValue({
      data: { code: '00000', data: { count: 0 } },
    } as any)

    const store = useNotificationStore()
    // 调用 3 次：每次都会立即执行一次，但只创建 1 个 interval
    store.startPolling()
    store.startPolling()
    store.startPolling()

    await vi.runAllTicks()
    // 3 次立即调用
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(3)

    vi.clearAllMocks()
    // 推进 60s：只有 1 个 interval，所以只触发 1 次
    await vi.advanceTimersByTimeAsync(60_000)
    expect(notificationsApi.unreadCount).toHaveBeenCalledTimes(1)

    store.stopPolling()
  })

  it('stopPolling：停止轮询并重置 unreadCount', async () => {
    const authStore = useAuthStore()
    authStore.setTokens('at', 'rt')
    authStore.currentUser = {} as any

    vi.mocked(notificationsApi.unreadCount).mockResolvedValue({
      data: { code: '00000', data: { count: 3 } },
    } as any)

    const store = useNotificationStore()
    store.startPolling()
    await vi.runAllTicks()
    expect(store.unreadCount).toBe(3)

    store.stopPolling()
    expect(store.unreadCount).toBe(0)

    // 推进时间后不再调用
    vi.clearAllMocks()
    await vi.advanceTimersByTimeAsync(120_000)
    expect(notificationsApi.unreadCount).not.toHaveBeenCalled()
  })
})
