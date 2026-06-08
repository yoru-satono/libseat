<script setup lang="ts">
defineProps<{
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'accent' | 'success'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  fullWidth?: boolean
  type?: 'button' | 'submit' | 'reset'
}>()
</script>

<template>
  <button
    :type="type ?? 'button'"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center gap-2 font-medium transition-all duration-200
           focus:outline-none focus:ring-2 focus:ring-offset-1
           disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none
           rounded-button"
    :class="[
      // 尺寸
      size === 'sm' ? 'px-3 py-1.5 text-xs gap-1.5' :
      size === 'lg' ? 'px-6 py-3 text-base gap-2.5' :
      'px-4 py-2.5 text-sm',
      // 变体
      variant === 'secondary' ? 'border border-surface-300 text-surface-700 bg-white hover:bg-surface-50 focus:ring-surface-300' :
      variant === 'danger' ? 'text-white bg-danger-500 hover:bg-danger-600 focus:ring-danger-400' :
      variant === 'ghost' ? 'text-surface-600 hover:text-surface-900 hover:bg-surface-100 focus:ring-surface-300' :
      variant === 'accent' ? 'text-white bg-accent-500 hover:bg-accent-600 focus:ring-accent-400' :
      variant === 'success' ? 'text-white bg-success-500 hover:bg-success-600 focus:ring-success-400' :
      // 默认：primary
      'text-white bg-primary-600 hover:bg-primary-700 focus:ring-primary-400 shadow-sm',
      // 全宽
      fullWidth ? 'w-full' : '',
    ]"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin -ml-0.5"
      :class="size === 'sm' ? 'w-3.5 h-3.5' : size === 'lg' ? 'w-5 h-5' : 'w-4 h-4'"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
    <slot />
  </button>
</template>
