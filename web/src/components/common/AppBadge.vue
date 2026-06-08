<script setup lang="ts">
import type { ReservationStatus, SeatStatus, WaitlistStatus } from '@/types/api'

type UserStatus = 'INACTIVE' | 'ACTIVE' | 'LOCKED' | 'SUSPENDED'
type Status = ReservationStatus | SeatStatus | WaitlistStatus | UserStatus | 'PENDING' | 'APPROVED' | 'REJECTED'

const props = defineProps<{
  status: Status
  variant?: 'default' | 'dot' | 'outline' | 'pill'
  dotColor?: string
}>()

const MAP: Record<string, { label: string; cls: string }> = {
  ACTIVE:      { label: '待签到', cls: 'bg-primary-50 text-primary-700' },
  CHECKED_IN:  { label: '已签到', cls: 'bg-success-50 text-success-700' },
  IN_USE:      { label: '使用中', cls: 'bg-accent-50 text-accent-700' },
  COMPLETED:   { label: '已完成', cls: 'bg-surface-100 text-surface-600' },
  CANCELLED:   { label: '已取消', cls: 'bg-danger-50 text-danger-600' },
  NO_SHOW:     { label: '爽约',   cls: 'bg-warning-50 text-warning-700' },
  AVAILABLE:   { label: '可预约', cls: 'bg-success-50 text-success-700' },
  UNAVAILABLE: { label: '不可用', cls: 'bg-surface-100 text-surface-500' },
  WAITING:     { label: '等待中', cls: 'bg-warning-50 text-warning-700' },
  NOTIFIED:    { label: '已通知', cls: 'bg-primary-50 text-primary-600' },
  EXPIRED:     { label: '已过期', cls: 'bg-surface-100 text-surface-500' },
  CONVERTED:   { label: '已转换', cls: 'bg-success-50 text-success-700' },
  PENDING:     { label: '待审核',  cls: 'bg-warning-50 text-warning-700' },
  APPROVED:    { label: '已批准',  cls: 'bg-success-50 text-success-700' },
  REJECTED:    { label: '已拒绝',  cls: 'bg-danger-50 text-danger-600' },
  INACTIVE:    { label: '未激活',  cls: 'bg-surface-100 text-surface-500' },
  LOCKED:      { label: '已锁定',  cls: 'bg-danger-50 text-danger-600' },
  SUSPENDED:   { label: '已暂停',  cls: 'bg-warning-50 text-warning-700' },
}

const info = MAP[props.status] ?? { label: props.status, cls: 'bg-surface-100 text-surface-600' }
</script>

<template>
  <!-- 默认：实心标签 -->
  <span
    v-if="!variant || variant === 'default'"
    class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium"
    :class="info.cls"
  >
    {{ info.label }}
  </span>

  <!-- 圆点指示器 -->
  <span
    v-else-if="variant === 'dot'"
    class="inline-flex items-center gap-1.5 text-xs font-medium text-surface-600"
  >
    <span class="w-2 h-2 rounded-full inline-block" :class="dotColor ?? 'bg-primary-500'" />
    {{ info.label }}
  </span>

  <!-- 描边标签 -->
  <span
    v-else-if="variant === 'outline'"
    class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border"
    :class="info.cls.replace(/bg-\w+-\d+/, 'bg-transparent').replace(/text-/, 'border-current text-')"
  >
    {{ info.label }}
  </span>

  <!-- 药丸标签 -->
  <span
    v-else-if="variant === 'pill'"
    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
    :class="info.cls"
  >
    {{ info.label }}
  </span>
</template>
