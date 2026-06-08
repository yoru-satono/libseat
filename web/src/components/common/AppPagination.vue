<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/outline'

const props = defineProps<{
  page: number
  totalPages: number
  total: number
  pageSize?: number
}>()
const emit = defineEmits<{ 'update:page': [page: number] }>()

const showJump = ref(false)

const pages = computed(() => {
  const delta = 2
  const range: number[] = []
  for (let i = Math.max(1, props.page - delta); i <= Math.min(props.totalPages, props.page + delta); i++) {
    range.push(i)
  }
  return range
})

function jump(val: string | number) {
  const n = Number(val)
  if (n >= 1 && n <= props.totalPages) {
    emit('update:page', n)
  }
  showJump.value = false
}
</script>

<template>
  <div class="flex items-center justify-between mt-4 gap-2">
    <div class="flex items-center gap-2 text-sm text-surface-500">
      <span>共 <span class="font-medium text-surface-700">{{ total }}</span> 条</span>
      <button
        v-if="totalPages > 7"
        class="text-primary-600 hover:text-primary-700 text-xs underline underline-offset-2"
        @click="showJump = !showJump"
      >
        跳转
      </button>
      <form v-if="showJump" @submit.prevent="jump(($el as HTMLFormElement).pageno.value)" class="flex items-center gap-1">
        <input
          name="pageno"
          type="number"
          :min="1"
          :max="totalPages"
          class="w-14 input-field py-1 px-2 text-xs"
          placeholder="页"
        />
      </form>
    </div>
    <div class="flex items-center gap-1">
      <button
        :disabled="page <= 1"
        @click="emit('update:page', page - 1)"
        class="p-1.5 rounded-lg text-surface-500 hover:bg-surface-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      >
        <ChevronLeftIcon class="w-4 h-4" />
      </button>
      <template v-if="(pages[0] ?? 0) > 1">
        <button @click="emit('update:page', 1)" class="w-8 h-8 rounded-lg text-sm text-surface-600 hover:bg-surface-100 transition-colors">1</button>
        <span v-if="(pages[0] ?? 0) > 2" class="text-surface-400 px-0.5 select-none">…</span>
      </template>
      <button
        v-for="p in pages"
        :key="p"
        @click="emit('update:page', p)"
        class="w-8 h-8 rounded-lg text-sm transition-all duration-200"
        :class="p === page
          ? 'bg-primary-600 text-white font-semibold shadow-sm'
          : 'text-surface-600 hover:bg-surface-100'"
      >{{ p }}</button>
      <template v-if="(pages[pages.length - 1] ?? 0) < totalPages">
        <span v-if="(pages[pages.length - 1] ?? 0) < totalPages - 1" class="text-surface-400 px-0.5 select-none">…</span>
        <button @click="emit('update:page', totalPages)" class="w-8 h-8 rounded-lg text-sm text-surface-600 hover:bg-surface-100 transition-colors">{{ totalPages }}</button>
      </template>
      <button
        :disabled="page >= totalPages"
        @click="emit('update:page', page + 1)"
        class="p-1.5 rounded-lg text-surface-500 hover:bg-surface-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      >
        <ChevronRightIcon class="w-4 h-4" />
      </button>
    </div>
  </div>
</template>
