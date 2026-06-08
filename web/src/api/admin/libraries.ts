import http from '../http'
import type { ApiResponse, Library } from '@/types/api'

export const adminLibrariesApi = {
  list() {
    return http.get<ApiResponse<Library[]>>('/admin/libraries')
  },
  get(id: string) {
    return http.get<ApiResponse<Library>>(`/admin/libraries/${id}`)
  },
  create(data: { name: string; address?: string }) {
    return http.post<ApiResponse<Library>>('/admin/libraries', data)
  },
  update(id: string, data: { name?: string; address?: string }) {
    return http.put<ApiResponse<Library>>(`/admin/libraries/${id}`, data)
  },
  remove(id: string) {
    return http.delete<ApiResponse<null>>(`/admin/libraries/${id}`)
  },
}
