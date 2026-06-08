<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: number | string
  trend?: 'up' | 'down'
  trendLabel?: string
  variant?: 'primary' | 'accent' | 'success' | 'warning' | 'danger'
  icon?: boolean
}>()

const colorMap = {
  primary: { bg: 'bg-primary-50', iconBg: 'bg-primary-500', text: 'text-primary-600' },
  accent: { bg: 'bg-accent-50', iconBg: 'bg-accent-500', text: 'text-accent-600' },
  success: { bg: 'bg-success-50', iconBg: 'bg-success-500', text: 'text-success-600' },
  warning: { bg: 'bg-warning-50', iconBg: 'bg-warning-500', text: 'text-warning-600' },
  danger: { bg: 'bg-danger-50', iconBg: 'bg-danger-500', text: 'text-danger-600' },
}

const colors = computed(() => colorMap[props.variant ?? 'primary'])
</script>

<template>
  <div class="card p-4 sm:p-5 animate-fade-in-up">
    <div class="flex items-start justify-between">
      <div class="flex-1 min-w-0">
        <p class="text-xs sm:text-sm text-surface-500 font-medium mb-1">{{ label }}</p>
        <p class="text-2xl sm:text-3xl font-bold text-surface-900 font-display tabular-nums">
          {{ typeof value === 'number' ? value.toLocaleString() : value }}
        </p>
        <div v-if="trend && trendLabel" class="flex items-center gap-1 mt-2">
          <svg
            class="w-3.5 h-3.5"
            :class="trend === 'up' ? 'text-success-500' : 'text-danger-500'"
            fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"
          >
            <path v-if="trend === 'up'" stroke-linecap="round" stroke-linejoin="round" d="M4.5 19.5l7.5-7.5 3.75 3.75L22.5 9" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" d="M4.5 4.5l7.5 7.5 3.75-3.75L22.5 15" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M22.5 9v6h-6" />
            <path v-if="trend === 'down'" stroke-linecap="round" stroke-linejoin="round" d="M22.5 15v-6h-6" />
          </svg>
          <span
            class="text-xs font-medium"
            :class="trend === 'up' ? 'text-success-600' : 'text-danger-600'"
          >{{ trendLabel }}</span>
        </div>
      </div>
      <div
        v-if="icon !== false"
        class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center shrink-0"
        :class="[colors.iconBg, colors.bg]"
      >
        <slot name="icon" />
      </div>
    </div>
    <!-- CSS 迷你趋势线 -->
    <div v-if="$slots.sparkline" class="mt-3">
      <slot name="sparkline" />
    </div>
  </div>
</template>
