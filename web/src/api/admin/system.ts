import http from '../http'
import type { ApiResponse, SystemRules, AuditLog, PageResult, AdminChangeRequest } from '@/types/api'

export const adminSystemApi = {
  getRules() {
    return http.get<ApiResponse<SystemRules>>('/admin/system-rules')
  },
  updateRules(data: Partial<Omit<SystemRules, 'ruleId' | 'libraryId' | 'updatedAt'>>) {
    return http.put<ApiResponse<SystemRules>>('/admin/system-rules', data)
  },
  listChangeRequests(params: { page?: number; pageSize?: number } = {}) {
    return http.get<ApiResponse<PageResult<AdminChangeRequest>>>('/admin/change-requests', {
      params,
    })
  },
  reviewChangeRequest(id: string, action: 'APPROVED' | 'REJECTED', handleNote?: string) {
    return http.patch<ApiResponse<AdminChangeRequest>>(`/admin/change-requests/${id}`, {
      action,
      handleNote,
    })
  },
  listAuditLogs(
    params: {
      adminId?: string
      targetType?: string
      targetId?: string
      dateFrom?: string
      dateTo?: string
      page?: number
      pageSize?: number
    } = {},
  ) {
    return http.get<ApiResponse<PageResult<AuditLog>>>('/admin/audit-logs', { params })
  },
}
