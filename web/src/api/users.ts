import http from './http'
import type { ApiResponse, PageResult, UserProfile, ChangeRequest } from '@/types/api'

export const usersApi = {
  getMe() {
    return http.get<ApiResponse<UserProfile>>('/users/me')
  },
  updateMe(data: { email?: string; phone?: string; oldPassword?: string; newPassword?: string }) {
    return http.patch<ApiResponse<UserProfile>>('/users/me', data)
  },
  createChangeRequest(fieldName: string, newValue: string) {
    return http.post<ApiResponse<ChangeRequest>>('/users/me/change-requests', {
      fieldName,
      newValue,
    })
  },
  listChangeRequests(page = 1, pageSize = 20) {
    return http.get<ApiResponse<PageResult<ChangeRequest>>>('/users/me/change-requests', {
      params: { page, pageSize },
    })
  },
}
