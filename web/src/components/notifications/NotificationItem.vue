<script setup lang="ts">
import { computed } from 'vue'
import {
  CheckCircleIcon,
  XCircleIcon,
  BellAlertIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
} from '@heroicons/vue/24/outline'
import { fromNow } from '@/utils/date'
import type { Notification, NotificationType } from '@/types/api'

const props = defineProps<{ notification: Notification }>()
const emit = defineEmits<{ read: [id: string]; remove: [id: string] }>()

const ICON_MAP: Record<NotificationType, any> = {
  RESERVATION_SUCCESS: CheckCircleIcon,
  RESERVATION_CANCELLED: XCircleIcon,
  CHECKIN_REMINDER: BellAlertIcon,
  NO_SHOW_WARNING: ExclamationTriangleIcon,
  ACCOUNT_LOCKED: ExclamationTriangleIcon,
  ACCOUNT_SUSPENDED: ExclamationTriangleIcon,
  WAITLIST_AVAILABLE: BellAlertIcon,
  RENEWAL_SUCCESS: CheckCircleIcon,
  SYSTEM: InformationCircleIcon,
}

const icon = computed(() => ICON_MAP[props.notification.type] ?? InformationCircleIcon)

// 图标颜色
const iconColor = computed(() => {
  switch (props.notification.type) {
    case 'RESERVATION_SUCCESS': case 'RENEWAL_SUCCESS': return 'text-success-500'
    case 'RESERVATION_CANCELLED': return 'text-danger-500'
    case 'NO_SHOW_WARNING': case 'ACCOUNT_LOCKED': case 'ACCOUNT_SUSPENDED': return 'text-warning-500'
    case 'CHECKIN_REMINDER': case 'WAITLIST_AVAILABLE': return 'text-primary-500'
    default: return 'text-surface-400'
  }
})
</script>

<template>
  <div
    :data-testid="notification.isRead ? undefined : 'unread-item'"
    class="flex gap-3 px-4 py-3.5 hover:bg-surface-50 transition-colors duration-150 cursor-pointer"
    :class="{
      'bg-primary-50/50 border-l-[3px] border-primary-500': !notification.isRead,
      'border-l-[3px] border-transparent': notification.isRead,
    }"
    @click="!notification.isRead && emit('read', notification.id)"
  >
    <!-- 图标 -->
    <div
      class="w-9 h-9 rounded-lg flex items-center justify-center shrink-0 mt-0.5"
      :class="notification.isRead ? 'bg-surface-100' : iconColor.replace('text-', 'bg-').replace('500', '50')"
    >
      <component :is="icon" class="w-4.5 h-4.5" :class="notification.isRead ? 'text-surface-400' : iconColor" />
    </div>

    <!-- 内容 -->
    <div class="flex-1 min-w-0">
      <p
        class="text-sm text-surface-900"
        :class="notification.isRead ? 'font-normal text-surface-600' : 'font-semibold'"
      >
        {{ notification.title }}
      </p>
      <p class="text-xs text-surface-500 mt-0.5 line-clamp-2">{{ notification.content }}</p>
      <p class="text-[11px] text-surface-400 mt-1.5">{{ fromNow(notification.createdAt) }}</p>
    </div>

    <!-- 删除按钮 -->
    <button
      @click.stop="emit('remove', notification.id)"
      class="shrink-0 p-1.5 text-surface-400 hover:text-danger-500 hover:bg-danger-50 rounded-lg transition-colors"
      title="删除"
    >
      <XCircleIcon class="w-4 h-4" />
    </button>
  </div>
</template>
