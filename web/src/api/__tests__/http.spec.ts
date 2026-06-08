import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'

// http.ts uses module-level state (isRefreshing, failedQueue).
// We isolate each test by resetting localStorage and the mock adapter.
const http = (await import('@/api/http')).default
const { ApiError } = await import('@/api/http')

const mock = new MockAdapter(http, { onNoMatch: 'throwException' })

// Mock the outer axios.post used for token refresh
const axiosSpy = vi.spyOn(axios, 'post')

describe('HTTP 拦截器', () => {
  beforeEach(() => {
    mock.reset()
    localStorage.clear()
    vi.clearAllMocks()
    // Restore window.location mock
    Object.defineProperty(window, 'location', {
      value: { href: '' },
      writable: true,
    })
  })

  afterEach(() => {
    mock.reset()
  })

  describe('请求拦截器', () => {
    it('存在 token 时注入 Authorization 头', async () => {
      localStorage.setItem('accessToken', 'my-token')
      mock.onGet('/test').reply(200, { code: '00000', data: null })

      const res = await http.get('/test')
      const reqConfig = mock.history.get[0]
      expect(reqConfig.headers?.Authorization).toBe('Bearer my-token')
      expect(res.data.code).toBe('00000')
    })

    it('无 token 时不注入 Authorization 头', async () => {
      mock.onGet('/test').reply(200, { code: '00000', data: null })

      await http.get('/test')
      const reqConfig = mock.history.get[0]
      expect(reqConfig.headers?.Authorization).toBeUndefined()
    })
  })

  describe('响应拦截器 — 业务错误', () => {
    it('code 为 00000 时正常返回响应', async () => {
      mock.onGet('/ok').reply(200, { code: '00000', data: { value: 42 } })

      const res = await http.get('/ok')
      expect(res.data.data.value).toBe(42)
    })

    it('code 不为 00000 时抛出 ApiError，携带 code 和 message', async () => {
      mock.onGet('/fail').reply(200, { code: 'B0200', message: '时段冲突' })

      await expect(http.get('/fail')).rejects.toBeInstanceOf(ApiError)

      try {
        await http.get('/fail')
      } catch (e: any) {
        expect(e.code).toBe('B0200')
        expect(e.message).toBe('时段冲突')
      }
    })

    it('Blob 响应直接透传，不走业务拦截', async () => {
      const blob = new Blob(['data'], { type: 'application/octet-stream' })
      mock.onGet('/export').reply(200, blob)

      const res = await http.get('/export', { responseType: 'blob' })
      expect(res.data).toBeInstanceOf(Blob)
    })
  })

  describe('响应拦截器 — A0202 (token 无效)', () => {
    it('A0202 清空 localStorage 并跳转 /login', async () => {
      localStorage.setItem('accessToken', 'invalid')
      localStorage.setItem('refreshToken', 'invalid')
      mock.onGet('/protected').reply(200, { code: 'A0202', message: 'token 无效' })

      await expect(http.get('/protected')).rejects.toBeInstanceOf(ApiError)

      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
      expect(window.location.href).toBe('/login')
    })

    it('A0202 不触发 /auth/refresh', async () => {
      mock.onGet('/protected').reply(200, { code: 'A0202', message: 'invalid' })
      await expect(http.get('/protected')).rejects.toBeDefined()
      expect(axiosSpy).not.toHaveBeenCalled()
    })
  })

  describe('响应拦截器 — A0100 (未登录)', () => {
    it('A0100 清空 localStorage 并跳转 /login', async () => {
      localStorage.setItem('accessToken', 'at')
      mock.onGet('/me').reply(200, { code: 'A0100', message: '未登录' })

      await expect(http.get('/me')).rejects.toBeDefined()
      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(window.location.href).toBe('/login')
    })
  })

  describe('响应拦截器 — A0201 (token 过期)', () => {
    it('A0201 + 刷新成功：重试原请求并使用新 token', async () => {
      localStorage.setItem('accessToken', 'expired-at')
      localStorage.setItem('refreshToken', 'rt-valid')

      // 第一次调用返回 A0201，第二次（重试）返回成功
      mock
        .onGet('/api-call')
        .replyOnce(200, { code: 'A0201', message: 'token expired' })
        .onGet('/api-call')
        .replyOnce(200, { code: '00000', data: { ok: true } })

      axiosSpy.mockResolvedValueOnce({
        data: {
          code: '00000',
          data: { accessToken: 'new-at', refreshToken: 'new-rt' },
        },
      })

      const res = await http.get('/api-call')

      expect(res.data.data.ok).toBe(true)
      expect(localStorage.getItem('accessToken')).toBe('new-at')
      expect(axiosSpy).toHaveBeenCalledTimes(1)
    })

    it('A0201 + 刷新失败：清空 token 并跳转 /login', async () => {
      localStorage.setItem('accessToken', 'expired')
      localStorage.setItem('refreshToken', 'rt')

      mock.onGet('/secure').reply(200, { code: 'A0201', message: 'expired' })

      axiosSpy.mockRejectedValueOnce(new Error('refresh failed'))

      await expect(http.get('/secure')).rejects.toBeDefined()

      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(window.location.href).toBe('/login')
    })

    it('A0201 + 无 refreshToken：直接清空跳转，不调 refresh', async () => {
      localStorage.setItem('accessToken', 'expired')
      // 不设置 refreshToken

      mock.onGet('/secure').reply(200, { code: 'A0201', message: 'expired' })

      await expect(http.get('/secure')).rejects.toBeDefined()

      expect(axiosSpy).not.toHaveBeenCalled()
      expect(window.location.href).toBe('/login')
    })
  })

  describe('网络错误', () => {
    it('网络超时直接 reject', async () => {
      mock.onGet('/timeout').networkError()
      await expect(http.get('/timeout')).rejects.toBeDefined()
    })
  })
})
