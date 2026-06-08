import http from './http'
import type { ApiResponse, PageResult, Waitlist, WaitlistStatus } from '@/types/api'

export const waitlistsApi = {
  list(status?: WaitlistStatus, page = 1, pageSize = 20) {
    return http.get<ApiResponse<PageResult<Waitlist>>>('/waitlists', {
      params: { status, page, pageSize },
    })
  },
  join(data: { seatId: string; date: string; startTime: string; endTime: string }) {
    return http.post<ApiResponse<Waitlist>>('/waitlists', data)
  },
  cancel(id: string) {
    return http.delete<ApiResponse<null>>(`/waitlists/${id}`)
  },
}
