<script setup lang="ts">
import { computed } from 'vue'
import { ComputerDesktopIcon, BoltIcon, SunIcon } from '@heroicons/vue/24/outline'
import AppBadge from '@/components/common/AppBadge.vue'
import AppButton from '@/components/common/AppButton.vue'
import type { Seat } from '@/types/api'

const props = defineProps<{ seat: Seat }>()
const emit = defineEmits<{ reserve: [seat: Seat] }>()

const AREA_LABEL: Record<string, string> = {
  QUIET: '安静区', DISCUSSION: '讨论区', COMPUTER: '电脑区',
}

// 区域颜色指示
const areaColor = computed(() => {
  return props.seat.area === 'QUIET' ? 'bg-primary-400' :
         props.seat.area === 'DISCUSSION' ? 'bg-warning-400' :
         'bg-accent-400'
})
</script>

<template>
  <div
    data-testid="seat-card"
    :data-seat-no="seat.seatNo"
    class="bg-white rounded-card border border-surface-200 p-4 hover:shadow-card-hover hover:-translate-y-0.5 transition-all duration-200 relative overflow-hidden"
  >
    <!-- 左侧区域彩色条 -->
    <div class="absolute left-0 top-3 bottom-3 w-1 rounded-r-full" :class="areaColor" />

    <div class="flex items-start justify-between mb-2 pl-2">
      <div>
        <p class="font-semibold text-surface-900">{{ seat.seatNo }}</p>
        <p class="text-xs text-surface-500">{{ seat.libraryName }} · {{ seat.floor }}F · {{ AREA_LABEL[seat.area] ?? seat.area }}</p>
      </div>
      <AppBadge :status="seat.status" />
    </div>

    <!-- 设施图标 -->
    <div class="flex items-center gap-3 mt-3 pl-2">
      <span v-if="seat.hasComputer" title="有电脑" class="flex items-center gap-1 text-xs text-success-600">
        <ComputerDesktopIcon class="w-4 h-4" /> 电脑
      </span>
      <span v-if="seat.hasPower" title="有电源" class="flex items-center gap-1 text-xs text-warning-600">
        <BoltIcon class="w-4 h-4" /> 电源
      </span>
      <span v-if="seat.hasWindow" title="靠窗" class="flex items-center gap-1 text-xs text-primary-600">
        <SunIcon class="w-4 h-4" /> 靠窗
      </span>
      <span v-if="!seat.hasComputer && !seat.hasPower && !seat.hasWindow" class="text-xs text-surface-400">-</span>
    </div>

    <!-- 预约按钮 -->
    <div class="mt-3 pl-2">
      <AppButton
        v-if="seat.status === 'AVAILABLE'"
        variant="primary"
        size="sm"
        full-width
        @click="emit('reserve', seat)"
      >预约</AppButton>
      <div
        v-else
        class="w-full py-1.5 text-sm text-center text-surface-400 border border-dashed border-surface-200 rounded-button"
      >暂不可预约</div>
    </div>
  </div>
</template>
