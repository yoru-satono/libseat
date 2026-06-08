<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CalendarIcon, ClockIcon } from '@heroicons/vue/24/outline'
import UserLayout from '@/layouts/UserLayout.vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppButton from '@/components/common/AppButton.vue'
import { waitlistsApi } from '@/api/waitlists'
import { useToast } from 'vue-toastification'
import type { Waitlist } from '@/types/api'

const toast = useToast()
const waitlists = ref<Waitlist[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 10

async function fetchWaitlists() {
  loading.value = true
  try {
    const res = await waitlistsApi.list(undefined, page.value, pageSize)
    waitlists.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchWaitlists)

async function cancelWait(id: string) {
  try {
    await waitlistsApi.cancel(id)
    toast.success('已取消等待')
    fetchWaitlists()
  } catch (e: any) {
    toast.error(e?.message || '取消失败')
  }
}
</script>

<template>
  <UserLayout>
    <h1 class="page-heading mb-4">等待队列</h1>

    <!-- 骨架加载 -->
    <div v-if="loading" class="space-y-3">
      <div v-for="n in 3" :key="n" class="skeleton h-24 rounded-card" />
    </div>

    <!-- 空状态 -->
    <AppEmptyState v-else-if="waitlists.length === 0" type="seat" title="暂无等待记录" description="当心仪的座位被预约后，您可以加入等待队列" />

    <!-- 等待列表 -->
    <div v-else class="space-y-3">
      <AppCard
        v-for="(w, i) in waitlists"
        :key="w.id"
        :style="{ animationDelay: `${0.05 * i}s` }"
        class="animate-fade-in-up"
      >
        <div class="flex items-start justify-between mb-2">
          <div>
            <p class="font-semibold text-surface-900">{{ w.seatNo }}</p>
            <p class="text-xs text-surface-500">{{ w.libraryName }}</p>
          </div>
          <AppBadge :status="w.status" />
        </div>
        <div class="flex flex-wrap gap-3 text-sm text-surface-600 mb-3">
          <span class="inline-flex items-center gap-1.5">
            <CalendarIcon class="w-4 h-4 text-surface-400" />{{ w.date }}
          </span>
          <span class="inline-flex items-center gap-1.5">
            <ClockIcon class="w-4 h-4 text-surface-400" />{{ w.startTime }} – {{ w.endTime }}
          </span>
        </div>
        <AppButton
          v-if="w.status === 'WAITING' || w.status === 'NOTIFIED'"
          variant="ghost"
          size="sm"
          class="!text-danger-600 hover:!bg-danger-50"
          @click="cancelWait(w.id)"
        >取消等待</AppButton>
      </AppCard>
    </div>

    <AppPagination
      v-if="totalPages > 1"
      :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchWaitlists() }"
    />
  </UserLayout>
</template>
