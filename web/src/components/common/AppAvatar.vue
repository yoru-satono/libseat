<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  name: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
}>()

// 根据名字取前两个字作为初始字母
const initials = computed(() => {
  if (!props.name) return '?'
  const chars = props.name.trim()
  return chars.length >= 2 ? chars.slice(0, 2) : chars.slice(0, 1)
})

// 基于名字哈希生成稳定的背景色
const colors = [
  'bg-primary-500 text-white',
  'bg-accent-500 text-white',
  'bg-success-500 text-white',
  'bg-danger-400 text-white',
  'bg-warning-500 text-white',
  'bg-primary-700 text-white',
  'bg-accent-600 text-white',
  'bg-success-600 text-white',
]
const colorIndex = computed(() => {
  let hash = 0
  for (let i = 0; i < props.name.length; i++) {
    hash = props.name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return Math.abs(hash) % colors.length
})

const sizeClasses = {
  sm: 'w-6 h-6 text-[10px]',
  md: 'w-9 h-9 text-xs',
  lg: 'w-12 h-12 text-sm',
  xl: 'w-20 h-20 text-xl',
}
</script>

<template>
  <span
    class="inline-flex items-center justify-center rounded-full font-semibold select-none shrink-0"
    :class="[colors[colorIndex], sizeClasses[size ?? 'md']]"
  >
    {{ initials }}
  </span>
</template>
