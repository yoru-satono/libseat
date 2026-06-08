<script setup lang="ts">
import { computed } from 'vue'
import { MapPinIcon, CalendarIcon, ClockIcon } from '@heroicons/vue/24/outline'
import AppBadge from '@/components/common/AppBadge.vue'
import AppButton from '@/components/common/AppButton.vue'
import type { Reservation } from '@/types/api'

const props = defineProps<{ reservation: Reservation }>()
const emit = defineEmits<{ cancel: [id: string]; checkin: [id: string]; renew: [id: string] }>()

const canCancel = computed(() => props.reservation.status === 'ACTIVE')
const canCheckin = computed(() => props.reservation.status === 'ACTIVE')
const canRenew = computed(
  () => props.reservation.status === 'ACTIVE' || props.reservation.status === 'CHECKED_IN' || props.reservation.status === 'IN_USE',
)

const AREA_LABEL: Record<string, string> = {
  QUIET: '安静区', DISCUSSION: '讨论区', COMPUTER: '电脑区',
}

// 状态左侧颜色条
const statusColor = computed(() => {
  switch (props.reservation.status) {
    case 'ACTIVE': return 'bg-primary-500'
    case 'CHECKED_IN': case 'IN_USE': return 'bg-success-500'
    case 'COMPLETED': return 'bg-surface-400'
    case 'CANCELLED': return 'bg-danger-400'
    case 'NO_SHOW': return 'bg-warning-400'
    default: return 'bg-surface-300'
  }
})
</script>

<template>
  <div class="bg-white rounded-card border border-surface-200 p-4 hover:shadow-card-hover transition-all duration-200 relative overflow-hidden">
    <!-- 左侧状态彩色条 -->
    <div class="absolute left-0 top-3 bottom-3 w-1 rounded-r-full" :class="statusColor" />

    <div class="pl-2">
      <!-- 头部 -->
      <div class="flex items-start justify-between gap-2 mb-3">
        <div>
          <p class="font-semibold text-surface-900">{{ reservation.seatNo }}</p>
          <p class="text-xs text-surface-500">{{ reservation.libraryName }} · {{ reservation.floor }}F · {{ AREA_LABEL[reservation.area] }}</p>
        </div>
        <AppBadge :status="reservation.status" />
      </div>

      <!-- 日期 + 时间 -->
      <div class="flex flex-wrap gap-3 text-sm text-surface-600 mb-3">
        <span class="inline-flex items-center gap-1.5">
          <CalendarIcon class="w-4 h-4 text-surface-400" />{{ reservation.date }}
        </span>
        <span class="inline-flex items-center gap-1.5">
          <ClockIcon class="w-4 h-4 text-surface-400" />{{ reservation.startTime }} – {{ reservation.endTime }}
        </span>
      </div>

      <!-- 操作按钮 -->
      <div v-if="canCancel || canCheckin || canRenew" class="flex gap-2 pt-3 border-t border-surface-100">
        <AppButton
          v-if="canCheckin"
          variant="primary"
          size="sm"
          class="flex-1"
          @click="emit('checkin', reservation.id)"
        >签到</AppButton>
        <AppButton
          v-if="canRenew"
          variant="accent"
          size="sm"
          class="flex-1"
          @click="emit('renew', reservation.id)"
        >续约</AppButton>
        <AppButton
          v-if="canCancel"
          variant="ghost"
          size="sm"
          class="flex-1 !text-danger-600 hover:!bg-danger-50"
          @click="emit('cancel', reservation.id)"
        >取消</AppButton>
      </div>
    </div>
  </div>
</template>
