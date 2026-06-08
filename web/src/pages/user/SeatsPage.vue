<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { FunnelIcon } from '@heroicons/vue/24/outline'
import UserLayout from '@/layouts/UserLayout.vue'
import SeatCard from '@/components/seats/SeatCard.vue'
import ReserveModal from '@/components/reservations/ReserveModal.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { seatsApi } from '@/api/seats'
import type { Seat, Library } from '@/types/api'
import http from '@/api/http'
import type { ApiResponse } from '@/types/api'

const seats = ref<Seat[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const filterOpen = ref(false)
const reserveSeat = ref<Seat | null>(null)

const filter = reactive({
  libraryId: undefined as string | undefined,
  floor: undefined as number | undefined,
  area: undefined as string | undefined,
  hasComputer: undefined as boolean | undefined,
  hasPower: undefined as boolean | undefined,
  date: '',
  startTime: '',
  endTime: '',
  page: 1,
  pageSize: 12,
})

const libraries = ref<Library[]>([])

onMounted(async () => {
  try {
    const res = await http.get<ApiResponse<Library[]>>('/libraries')
    libraries.value = res.data.data
  } catch {}
  fetchSeats()
})

async function fetchSeats() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: filter.page, pageSize: filter.pageSize }
    if (filter.libraryId) params.libraryId = filter.libraryId
    if (filter.floor) params.floor = filter.floor
    if (filter.area) params.area = filter.area
    if (filter.hasComputer) params.hasComputer = true
    if (filter.hasPower) params.hasPower = true
    if (filter.date) params.date = filter.date
    if (filter.startTime) params.startTime = filter.startTime
    if (filter.endTime) params.endTime = filter.endTime
    const res = await seatsApi.list(params as any)
    seats.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

function applyFilter() {
  filter.page = 1
  fetchSeats()
  filterOpen.value = false
}

function resetFilter() {
  Object.assign(filter, {
    libraryId: undefined, floor: undefined, area: undefined,
    hasComputer: undefined, hasPower: undefined,
    date: '', startTime: '', endTime: '', page: 1,
  })
  fetchSeats()
}

const areaOptions = [
  { label: '全部区域', value: undefined },
  { label: '安静区', value: 'QUIET' },
  { label: '讨论区', value: 'DISCUSSION' },
  { label: '电脑区', value: 'COMPUTER' },
]

const libraryOptions = [
  { label: '全部图书馆', value: undefined },
  ...libraries.value.map(l => ({ label: l.name, value: l.id })),
]
</script>

<template>
  <UserLayout>
    <div class="flex gap-6">
      <!-- 桌面端筛选侧栏 -->
      <aside class="hidden lg:block w-64 shrink-0">
        <div class="card p-5 sticky top-20">
          <h3 class="font-display text-sm font-semibold text-surface-800 mb-4">筛选条件</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium text-surface-600 mb-1.5">图书馆</label>
              <AppSelect v-model="filter.libraryId" :options="libraryOptions" placeholder="全部图书馆" />
            </div>
            <div>
              <label class="block text-xs font-medium text-surface-600 mb-1.5">区域类型</label>
              <AppSelect v-model="filter.area" :options="areaOptions" />
            </div>
            <div>
              <label class="block text-xs font-medium text-surface-600 mb-1.5">楼层</label>
              <input v-model.number="filter.floor" type="number" min="1" placeholder="不限" class="input-field py-2" />
            </div>
            <div class="space-y-2">
              <label class="flex items-center gap-2 text-sm text-surface-700 cursor-pointer">
                <input v-model="filter.hasComputer" type="checkbox" class="rounded text-primary-600 focus:ring-primary-400" /> 有电脑
              </label>
              <label class="flex items-center gap-2 text-sm text-surface-700 cursor-pointer">
                <input v-model="filter.hasPower" type="checkbox" class="rounded text-primary-600 focus:ring-primary-400" /> 有电源
              </label>
            </div>
            <hr class="border-surface-100" />
            <div>
              <label class="block text-xs font-medium text-surface-600 mb-1.5">预约日期</label>
              <input v-model="filter.date" type="date" :min="new Date().toISOString().slice(0,10)" class="input-field py-2" />
            </div>
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="block text-xs font-medium text-surface-600 mb-1.5">开始</label>
                <input v-model="filter.startTime" type="time" class="input-field py-2" />
              </div>
              <div>
                <label class="block text-xs font-medium text-surface-600 mb-1.5">结束</label>
                <input v-model="filter.endTime" type="time" class="input-field py-2" />
              </div>
            </div>
            <div class="flex gap-2 pt-1">
              <AppButton variant="secondary" size="sm" class="flex-1" @click="resetFilter">重置</AppButton>
              <AppButton variant="primary" size="sm" class="flex-1" @click="applyFilter">查询</AppButton>
            </div>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- 移动端筛选按钮 -->
        <div class="lg:hidden flex items-center justify-between mb-4">
          <h1 class="page-heading">座位列表</h1>
          <AppButton variant="secondary" size="sm" @click="filterOpen = true">
            <FunnelIcon class="w-4 h-4" /> 筛选
          </AppButton>
        </div>

        <!-- 骨架加载 -->
        <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div v-for="n in 6" :key="n" class="skeleton h-36 rounded-card" />
        </div>

        <!-- 空状态 -->
        <AppEmptyState
          v-else-if="seats.length === 0"
          type="search"
          title="未找到符合条件的座位"
          description="请调整筛选条件后重试"
        />

        <!-- 座位卡片网格 -->
        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <SeatCard v-for="(s, i) in seats" :key="s.id" :seat="s" :style="{ animationDelay: `${0.05 * (i % 6)}s` }" class="animate-fade-in-up" @reserve="reserveSeat = $event" />
        </div>

        <AppPagination
          v-if="totalPages > 1"
          :page="filter.page"
          :total-pages="totalPages"
          :total="total"
          :page-size="filter.pageSize"
          @update:page="p => { filter.page = p; fetchSeats() }"
        />
      </div>
    </div>

    <!-- 移动端筛选弹窗 -->
    <AppModal :open="filterOpen" title="筛选条件" @close="filterOpen = false">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1.5">区域类型</label>
          <AppSelect v-model="filter.area" :options="areaOptions" />
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1.5">楼层</label>
          <input v-model.number="filter.floor" type="number" min="1" placeholder="不限" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1.5">预约日期</label>
          <input v-model="filter.date" type="date" :min="new Date().toISOString().slice(0,10)" class="input-field" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="resetFilter">重置</AppButton>
          <AppButton variant="primary" class="flex-1" @click="applyFilter">查询</AppButton>
        </div>
      </template>
    </AppModal>

    <!-- 预约弹窗 -->
    <ReserveModal :seat="reserveSeat" @close="reserveSeat = null" @success="fetchSeats" />
  </UserLayout>
</template>
