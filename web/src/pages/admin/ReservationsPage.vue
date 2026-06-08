<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminReservationsApi } from '@/api/admin/reservations'
import { useToast } from 'vue-toastification'
import type { AdminReservation, ReservationStatus } from '@/types/api'

const toast = useToast()
const items = ref<AdminReservation[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20

const filter = reactive<{ status: string; dateFrom: string; dateTo: string }>({
  status: '', dateFrom: '', dateTo: '',
})

async function fetchItems() {
  loading.value = true
  try {
    const res = await adminReservationsApi.list({
      status: filter.status ? filter.status as ReservationStatus : undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
      page: page.value, pageSize,
    })
    items.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchItems)

async function cancelReservation(id: string) {
  if (!confirm('确认取消此预约？')) return
  try {
    await adminReservationsApi.cancel(id)
    toast.success('已取消')
    fetchItems()
  } catch (e: any) {
    toast.error(e?.message || '操作失败')
  }
}

async function exportData() {
  try {
    const res = await adminReservationsApi.export({
      status: filter.status ? filter.status as ReservationStatus : undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
    })
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `admin-reservations-${new Date().toISOString().slice(0,10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    toast.error(e?.message || '导出失败')
  }
}
</script>

<template>

    <!-- 筛选栏 -->
    <div class="flex flex-wrap gap-2 mb-4 items-center">
      <select v-model="filter.status" class="input-field w-28 py-2">
        <option value="">全部状态</option>
        <option value="ACTIVE">待签到</option>
        <option value="CHECKED_IN">已签到</option>
        <option value="COMPLETED">已完成</option>
        <option value="CANCELLED">已取消</option>
        <option value="NO_SHOW">爽约</option>
      </select>
      <input v-model="filter.dateFrom" type="date" class="input-field w-36 py-2" />
      <span class="text-surface-400 text-sm">至</span>
      <input v-model="filter.dateTo" type="date" class="input-field w-36 py-2" />
      <AppButton variant="primary" size="sm" @click="() => { page = 1; fetchItems() }">查询</AppButton>
      <AppButton variant="secondary" size="sm" @click="exportData">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
        </svg>
        导出 Excel
      </AppButton>
    </div>

    <AppEmptyState v-if="!loading && items.length === 0" type="calendar" title="暂无记录" />

    <div v-else>
      <!-- 桌面端表格 -->
      <div class="hidden md:block card overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-surface-100 bg-surface-50 text-surface-600">
              <th class="text-left px-4 py-3 font-medium">用户</th>
              <th class="text-left px-4 py-3 font-medium">座位</th>
              <th class="text-left px-4 py-3 font-medium">日期</th>
              <th class="text-left px-4 py-3 font-medium">时间</th>
              <th class="text-left px-4 py-3 font-medium">状态</th>
              <th class="text-right px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-surface-50">
            <tr v-for="r in items" :key="r.id" class="hover:bg-surface-50 transition-colors">
              <td class="px-4 py-3">
                <p class="text-surface-900 font-medium">{{ r.realName }}</p>
                <p class="text-xs text-surface-500 font-mono">{{ r.userNo }}</p>
              </td>
              <td class="px-4 py-3 text-surface-700">{{ r.seatNo }}</td>
              <td class="px-4 py-3 text-surface-700">{{ r.date }}</td>
              <td class="px-4 py-3 text-surface-700 text-xs">{{ r.startTime }}–{{ r.endTime }}</td>
              <td class="px-4 py-3"><AppBadge :status="r.status" /></td>
              <td class="px-4 py-3 text-right">
                <button v-if="r.status === 'ACTIVE'" @click="cancelReservation(r.id)" class="text-danger-500 hover:text-danger-600 text-xs font-medium">取消</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 移动端卡片 -->
      <div class="md:hidden space-y-2">
        <div v-for="r in items" :key="r.id" class="card p-4">
          <div class="flex justify-between mb-1">
            <p class="font-semibold text-sm">{{ r.realName }} <span class="text-surface-500 font-normal text-xs">({{ r.userNo }})</span></p>
            <AppBadge :status="r.status" />
          </div>
          <p class="text-sm text-surface-600">{{ r.seatNo }} · {{ r.date }} · {{ r.startTime }}–{{ r.endTime }}</p>
          <button v-if="r.status === 'ACTIVE'" @click="cancelReservation(r.id)" class="mt-2 text-xs text-danger-500 font-medium">取消预约</button>
        </div>
      </div>
    </div>

    <AppPagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchItems() }" />
</template>
