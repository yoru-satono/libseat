import http from './http'
import type { ApiResponse, PageResult, Reservation, ReservationStatus } from '@/types/api'

export interface ReservationFilter {
  status?: ReservationStatus
  dateFrom?: string
  dateTo?: string
  page?: number
  pageSize?: number
}

export const reservationsApi = {
  list(filter: ReservationFilter = {}) {
    return http.get<ApiResponse<PageResult<Reservation>>>('/reservations', { params: filter })
  },
  get(id: string) {
    return http.get<ApiResponse<Reservation>>(`/reservations/${id}`)
  },
  create(data: { seatId: string; date: string; startTime: string; endTime: string }) {
    return http.post<ApiResponse<Reservation>>('/reservations', data)
  },
  cancel(id: string, cancelReason?: string) {
    return http.delete<ApiResponse<null>>(`/reservations/${id}`, {
      data: cancelReason ? { cancelReason } : undefined,
    })
  },
  checkin(id: string, qrToken: string) {
    return http.post<ApiResponse<Reservation>>(`/reservations/${id}/checkin`, { qrToken })
  },
  renew(id: string, newEndTime: string) {
    return http.post<ApiResponse<Reservation>>(`/reservations/${id}/renew`, { newEndTime })
  },
  export(filter: Omit<ReservationFilter, 'page' | 'pageSize'> = {}) {
    return http.get('/reservations/export', {
      params: filter,
      responseType: 'blob',
    })
  },
}
