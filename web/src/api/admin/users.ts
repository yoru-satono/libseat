import http from '../http'
import type { ApiResponse, PageResult, AdminUser } from '@/types/api'

export const adminUsersApi = {
  list(params: { role?: string; status?: string; page?: number; pageSize?: number } = {}) {
    return http.get<ApiResponse<PageResult<AdminUser>>>('/admin/users', { params })
  },
  get(userId: string) {
    return http.get<ApiResponse<AdminUser>>(`/admin/users/${userId}`)
  },
  update(
    userId: string,
    data: {
      role?: string
      status?: string
      newPassword?: string
      resetNoShowCount?: boolean
    },
  ) {
    return http.patch<ApiResponse<AdminUser>>(`/admin/users/${userId}`, data)
  },
}
