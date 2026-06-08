import http from '../http'
import type { ApiResponse, PageResult, AdminReservation, ReservationStatus } from '@/types/api'

export const adminReservationsApi = {
  list(
    params: {
      status?: ReservationStatus
      dateFrom?: string
      dateTo?: string
      page?: number
      pageSize?: number
    } = {},
  ) {
    return http.get<ApiResponse<PageResult<AdminReservation>>>('/admin/reservations', { params })
  },
  get(id: string) {
    return http.get<ApiResponse<AdminReservation>>(`/admin/reservations/${id}`)
  },
  cancel(id: string) {
    return http.delete<ApiResponse<null>>(`/admin/reservations/${id}`)
  },
  export(
    params: {
      userId?: string
      status?: ReservationStatus
      dateFrom?: string
      dateTo?: string
    } = {},
  ) {
    return http.get('/admin/reservations/export', { params, responseType: 'blob' })
  },
}
