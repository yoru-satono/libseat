export interface ApiResponse<T = null> {
  code: string
  message: string
  data: T
  timestamp: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// Auth
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

// User
export interface UserProfile {
  id: string
  userNo: string
  realName: string
  email: string
  phone: string | null
  department: string | null
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  status: 'INACTIVE' | 'ACTIVE' | 'LOCKED' | 'SUSPENDED'
  noShowCount: number
  lastLoginAt: string | null
  createdAt: string
}

export interface ChangeRequest {
  id: string
  fieldName: 'userNo' | 'realName' | 'department'
  oldValue: string
  newValue: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  handleNote: string | null
  createdAt: string
  handledAt: string | null
}

// Library
export interface Library {
  id: string
  name: string
  address: string | null
  logoUrl: string | null
  createdAt: string
}

// Seat
export type SeatArea = 'QUIET' | 'DISCUSSION' | 'COMPUTER'
export type SeatStatus = 'AVAILABLE' | 'UNAVAILABLE'

export interface Seat {
  id: string
  libraryId: string
  libraryName: string
  seatNo: string
  floor: number
  area: SeatArea
  hasComputer: boolean
  hasPower: boolean
  hasWindow: boolean
  status: SeatStatus
  posX: number | null
  posY: number | null
}

// Reservation
export type ReservationStatus =
  | 'ACTIVE'
  | 'CHECKED_IN'
  | 'IN_USE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export interface Reservation {
  id: string
  seatId: string
  seatNo: string
  libraryName: string
  floor: number
  area: SeatArea
  date: string
  startTime: string
  endTime: string
  status: ReservationStatus
  checkinAt: string | null
  cancelledAt: string | null
  cancelReason: string | null
  completedAt: string | null
  createdAt: string
}

export interface AdminReservation extends Reservation {
  userId: string
  userNo: string
  realName: string
}

// Waitlist
export type WaitlistStatus = 'WAITING' | 'NOTIFIED' | 'EXPIRED' | 'CONVERTED'

export interface Waitlist {
  id: string
  seatId: string
  seatNo: string
  libraryName: string
  date: string
  startTime: string
  endTime: string
  status: WaitlistStatus
  notifiedAt: string | null
  expiresAt: string
  createdAt: string
}

// Notification
export type NotificationType =
  | 'RESERVATION_SUCCESS'
  | 'RESERVATION_CANCELLED'
  | 'CHECKIN_REMINDER'
  | 'NO_SHOW_WARNING'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_SUSPENDED'
  | 'WAITLIST_AVAILABLE'
  | 'RENEWAL_SUCCESS'
  | 'SYSTEM'

export interface Notification {
  id: string
  type: NotificationType
  title: string
  content: string
  relatedId: string | null
  isRead: boolean
  readAt: string | null
  createdAt: string
}

// Admin user
export interface AdminUser extends UserProfile {
  failedLoginCount: number
  lockedUntil: string | null
  suspendedUntil: string | null
  emailVerifiedAt: string | null
}

// System rules
export interface SystemRules {
  ruleId: number
  libraryId: string | null
  openTimeStart: string
  openTimeEnd: string
  advanceDaysMax: number
  singleMinMinutes: number
  singleMaxHours: number
  dailyMaxHours: number
  checkinEarlyMinutes: number
  checkinLateMinutes: number
  noShowThreshold: number
  suspendDays: number
  updatedAt: string
}

// Audit log
export interface AuditLog {
  logId: number
  adminId: string
  adminName: string
  actionType: string
  targetType: string
  targetId: string
  detail: Record<string, unknown> | null
  ipAddress: string | null
  createdAt: string
}

// Admin change request
export interface AdminChangeRequest extends ChangeRequest {
  userId: string
  userNo: string
  realName: string
}
