<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import { seatsApi } from '@/api/seats'
import type { Seat } from '@/types/api'

const seats = ref<Seat[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20

async function fetchSeats() {
  loading.value = true
  try {
    const res = await seatsApi.list({ page: page.value, pageSize })
    seats.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchSeats)

const AREA_LABEL: Record<string, string> = { QUIET: '安静区', DISCUSSION: '讨论区', COMPUTER: '电脑区' }
const AREA_DOT: Record<string, string> = { QUIET: 'bg-primary-400', DISCUSSION: 'bg-warning-400', COMPUTER: 'bg-accent-400' }
</script>

<template>

    <AppEmptyState v-if="!loading && seats.length === 0" type="seat" title="暂无座位数据" />

    <div v-else>
      <!-- 桌面端表格 -->
      <div class="hidden md:block card overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-surface-100 bg-surface-50 text-surface-600">
              <th class="text-left px-4 py-3 font-medium">座位号</th>
              <th class="text-left px-4 py-3 font-medium">图书馆</th>
              <th class="text-left px-4 py-3 font-medium">楼层</th>
              <th class="text-left px-4 py-3 font-medium">区域</th>
              <th class="text-left px-4 py-3 font-medium">设施</th>
              <th class="text-left px-4 py-3 font-medium">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-surface-50">
            <tr v-for="s in seats" :key="s.id" class="hover:bg-surface-50 transition-colors">
              <td class="px-4 py-3 font-semibold text-surface-900">{{ s.seatNo }}</td>
              <td class="px-4 py-3 text-surface-700">{{ s.libraryName }}</td>
              <td class="px-4 py-3 text-surface-700">{{ s.floor }}F</td>
              <td class="px-4 py-3">
                <span class="inline-flex items-center gap-1.5 text-surface-700">
                  <span class="w-2 h-2 rounded-full" :class="AREA_DOT[s.area]" />
                  {{ AREA_LABEL[s.area] }}
                </span>
              </td>
              <td class="px-4 py-3">
                <div class="flex gap-1.5">
                  <span v-if="s.hasComputer" class="px-1.5 py-0.5 text-[10px] bg-success-50 text-success-600 rounded font-medium">💻</span>
                  <span v-if="s.hasPower" class="px-1.5 py-0.5 text-[10px] bg-warning-50 text-warning-600 rounded font-medium">⚡</span>
                  <span v-if="s.hasWindow" class="px-1.5 py-0.5 text-[10px] bg-primary-50 text-primary-600 rounded font-medium">🪟</span>
                  <span v-if="!s.hasComputer && !s.hasPower && !s.hasWindow" class="text-surface-400">—</span>
                </div>
              </td>
              <td class="px-4 py-3"><AppBadge :status="s.status" /></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 移动端卡片 -->
      <div class="md:hidden space-y-2">
        <div v-for="s in seats" :key="s.id" class="card p-3">
          <div class="flex justify-between mb-1">
            <p class="font-semibold text-sm">{{ s.seatNo }}</p>
            <AppBadge :status="s.status" />
          </div>
          <p class="text-xs text-surface-500 flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full" :class="AREA_DOT[s.area]" />
            {{ s.libraryName }} · {{ s.floor }}F · {{ AREA_LABEL[s.area] }}
          </p>
        </div>
      </div>
    </div>

    <AppPagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchSeats() }" />
</template>
