import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
  ) {
    super(message)
  }
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false
type QueueItem = { resolve: (token: string) => void; reject: (err: unknown) => void }
let failedQueue: QueueItem[] = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token!)
  })
  failedQueue = []
}

http.interceptors.response.use(
  (response: AxiosResponse) => {
    // Blob responses (file downloads) pass through directly
    if (response.config.responseType === 'blob') return response

    const { code, message } = response.data
    if (code === '00000') return response

    // Access token expired — try to refresh
    if (code === 'A0201') {
      const originalConfig = response.config as InternalAxiosRequestConfig & { _retry?: boolean }
      if (originalConfig._retry) {
        clearAuthAndRedirect()
        return Promise.reject(new ApiError(code, message))
      }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalConfig.headers.Authorization = `Bearer ${token}`
            return http(originalConfig)
          })
          .catch((err) => Promise.reject(err))
      }

      originalConfig._retry = true
      isRefreshing = true

      return new Promise((resolve, reject) => {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
          clearAuthAndRedirect()
          reject(new ApiError(code, message))
          return
        }

        axios
          .post(`${http.defaults.baseURL}/auth/refresh`, { refreshToken })
          .then((res) => {
            if (res.data.code !== '00000') throw new Error(res.data.message)
            const { accessToken, refreshToken: newRefresh } = res.data.data
            localStorage.setItem('accessToken', accessToken)
            localStorage.setItem('refreshToken', newRefresh)
            http.defaults.headers.common.Authorization = `Bearer ${accessToken}`
            processQueue(null, accessToken)
            originalConfig.headers.Authorization = `Bearer ${accessToken}`
            resolve(http(originalConfig))
          })
          .catch((err) => {
            processQueue(err, null)
            clearAuthAndRedirect()
            reject(err)
          })
          .finally(() => {
            isRefreshing = false
          })
      })
    }

    // Not authenticated or token invalid
    if (code === 'A0100' || code === 'A0202') {
      clearAuthAndRedirect()
      return Promise.reject(new ApiError(code, message))
    }

    return Promise.reject(new ApiError(code, message))
  },
  (error) => {
    return Promise.reject(error)
  },
)

function clearAuthAndRedirect() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  window.location.href = '/login'
}

export default http
