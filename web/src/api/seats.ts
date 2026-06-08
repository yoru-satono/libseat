import http from './http'
import type { ApiResponse, PageResult, Seat, SeatArea } from '@/types/api'

export interface SeatFilter {
  libraryId?: string
  floor?: number
  area?: SeatArea
  hasComputer?: boolean
  hasPower?: boolean
  hasWindow?: boolean
  date?: string
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}

export const seatsApi = {
  list(filter: SeatFilter = {}) {
    return http.get<ApiResponse<PageResult<Seat>>>('/seats', { params: filter })
  },
  get(seatId: string) {
    return http.get<ApiResponse<Seat>>(`/seats/${seatId}`)
  },
}
