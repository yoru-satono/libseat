import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/api'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('@/api/users', () => ({
  usersApi: {
    getMe: vi.fn(),
  },
}))

const { authApi } = await import('@/api/auth')
const { usersApi } = await import('@/api/users')

const mockUser: UserProfile = {
  id: '1',
  userNo: 'S001',
  realName: '张三',
  email: 'test@example.com',
  phone: '13800000000',
  department: '计算机学院',
  role: 'STUDENT',
  status: 'ACTIVE',
  noShowCount: 0,
  lastLoginAt: '2026-01-01T00:00:00',
  createdAt: '2025-01-01T00:00:00',
}

const mockAdminUser: UserProfile = { ...mockUser, role: 'ADMIN' }

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始状态：无 token 时 isAuthenticated 为 false', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.currentUser).toBeNull()
  })

  it('login 成功：写入 tokens 和 currentUser，isAuthenticated 变为 true', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      data: { code: '00000', data: { accessToken: 'at-123', refreshToken: 'rt-456' } },
    } as any)
    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: { code: '00000', data: mockUser },
    } as any)

    const store = useAuthStore()
    await store.login('S001', 'password')

    expect(store.isAuthenticated).toBe(true)
    expect(store.accessToken).toBe('at-123')
    expect(store.refreshToken).toBe('rt-456')
    expect(store.currentUser).toEqual(mockUser)
    expect(localStorage.getItem('accessToken')).toBe('at-123')
    expect(localStorage.getItem('refreshToken')).toBe('rt-456')
  })

  it('login 失败：抛出错误，tokens 不写入', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('账号或密码错误'))

    const store = useAuthStore()
    await expect(store.login('S001', 'wrong')).rejects.toThrow()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('logout：清空 tokens 和 currentUser', async () => {
    vi.mocked(authApi.logout).mockResolvedValue(undefined as any)

    const store = useAuthStore()
    store.setTokens('at-123', 'rt-456')
    store.currentUser = mockUser as any

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(store.currentUser).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('logout：即使 API 调用失败也清空 tokens（finally 保证执行）', async () => {
    vi.mocked(authApi.logout).mockRejectedValue(new Error('network error'))

    const store = useAuthStore()
    store.setTokens('at-123', 'rt-456')

    // logout 会 re-throw，但 finally 仍保证清空
    try { await store.logout() } catch {}

    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('isAdmin：role 为 ADMIN 时返回 true', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      data: { code: '00000', data: { accessToken: 'at', refreshToken: 'rt' } },
    } as any)
    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: { code: '00000', data: mockAdminUser },
    } as any)

    const store = useAuthStore()
    await store.login('A001', 'password')
    expect(store.isAdmin).toBe(true)
  })

  it('isAdmin：role 非 ADMIN 时返回 false', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      data: { code: '00000', data: { accessToken: 'at', refreshToken: 'rt' } },
    } as any)
    vi.mocked(usersApi.getMe).mockResolvedValue({
      data: { code: '00000', data: mockUser },
    } as any)

    const store = useAuthStore()
    await store.login('S001', 'password')
    expect(store.isAdmin).toBe(false)
  })

  it('刷新页面后从 localStorage 恢复 token', () => {
    localStorage.setItem('accessToken', 'persisted-at')
    localStorage.setItem('refreshToken', 'persisted-rt')

    // 重新创建 pinia 和 store，模拟页面刷新
    setActivePinia(createPinia())
    const store = useAuthStore()

    expect(store.accessToken).toBe('persisted-at')
    expect(store.isAuthenticated).toBe(true)
  })

  it('clearTokens：清除所有认证数据', () => {
    const store = useAuthStore()
    store.setTokens('at', 'rt')
    store.clearTokens()

    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })
})
