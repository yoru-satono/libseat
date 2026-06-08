<script setup lang="ts">
defineProps<{
  hover?: boolean
  padding?: 'none' | 'sm' | 'md' | 'lg'
  variant?: 'default' | 'bordered' | 'elevated' | 'flat'
  as?: string
}>()
</script>

<template>
  <component
    :is="as ?? 'div'"
    class="bg-white transition-all duration-200"
    :class="[
      // 圆角
      'rounded-card',
      // 变体
      variant === 'elevated' ? 'shadow-card' :
      variant === 'flat' ? '' :
      'border border-surface-200',
      // 悬停效果
      hover ? 'hover:shadow-card-hover hover:-translate-y-0.5' : '',
      // 内边距
      padding === 'none' ? '' :
      padding === 'sm' ? 'p-3' :
      padding === 'lg' ? 'p-6' :
      'p-4 sm:p-5',
    ]"
  >
    <slot />
  </component>
</template>
