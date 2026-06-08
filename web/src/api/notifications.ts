import http from './http'
import type { ApiResponse, PageResult, Notification } from '@/types/api'

export const notificationsApi = {
  list(isRead?: boolean, page = 1, pageSize = 20) {
    return http.get<ApiResponse<PageResult<Notification>>>('/notifications', {
      params: { isRead, page, pageSize },
    })
  },
  unreadCount() {
    return http.get<ApiResponse<{ count: number }>>('/notifications/unread-count')
  },
  markRead(id: string) {
    return http.patch<ApiResponse<null>>(`/notifications/${id}/read`)
  },
  markAllRead() {
    return http.patch<ApiResponse<null>>('/notifications/read-all')
  },
  remove(id: string) {
    return http.delete<ApiResponse<null>>(`/notifications/${id}`)
  },
}
