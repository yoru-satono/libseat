import http from './http'
import type { ApiResponse, TokenResponse } from '@/types/api'

export const authApi = {
  login(userNo: string, password: string) {
    return http.post<ApiResponse<TokenResponse>>('/auth/login', { userNo, password })
  },
  register(data: {
    userNo: string
    realName: string
    password: string
    email: string
    phone?: string
    department?: string
  }) {
    return http.post<ApiResponse<null>>('/auth/register', data)
  },
  activate(token: string) {
    return http.post<ApiResponse<null>>('/auth/activate', { token })
  },
  logout() {
    return http.post<ApiResponse<null>>('/auth/logout')
  },
  refresh(refreshToken: string) {
    return http.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken })
  },
  resetRequest(email: string) {
    return http.post<ApiResponse<null>>('/auth/password/reset-request', { email })
  },
  resetPassword(token: string, newPassword: string) {
    return http.post<ApiResponse<null>>('/auth/password/reset', { token, newPassword })
  },
  confirmEmail(token: string) {
    return http.post<ApiResponse<null>>('/auth/email/confirm', { token })
  },
}
