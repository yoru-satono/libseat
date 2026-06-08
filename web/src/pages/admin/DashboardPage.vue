<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { UsersIcon, CalendarDaysIcon, DocumentCheckIcon } from '@heroicons/vue/24/outline'
import AppStatCard from '@/components/common/AppStatCard.vue'
import { adminReservationsApi } from '@/api/admin/reservations'
import { adminUsersApi } from '@/api/admin/users'
import { adminSystemApi } from '@/api/admin/system'

const stats = ref({ activeReservations: 0, pendingRequests: 0, totalUsers: 0 })

onMounted(async () => {
  try {
    const [rRes, uRes, crRes] = await Promise.allSettled([
      adminReservationsApi.list({ status: 'ACTIVE', pageSize: 1 }),
      adminUsersApi.list({ pageSize: 1 }),
      adminSystemApi.listChangeRequests({ pageSize: 1 }),
    ])
    if (rRes.status === 'fulfilled') stats.value.activeReservations = rRes.value.data.data.total
    if (uRes.status === 'fulfilled') stats.value.totalUsers = uRes.value.data.data.total
    if (crRes.status === 'fulfilled') stats.value.pendingRequests = crRes.value.data.data.total
  } catch {}
})
</script>

<template>
  <h1 class="page-heading mb-6">概览</h1>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
      <AppStatCard
        label="活跃预约"
        :value="stats.activeReservations"
        variant="primary"
        class="stagger-1"
      >
        <template #icon>
          <CalendarDaysIcon class="w-5 h-5 text-white" />
        </template>
      </AppStatCard>
      <AppStatCard
        label="待审核申请"
        :value="stats.pendingRequests"
        variant="warning"
        class="stagger-2"
      >
        <template #icon>
          <DocumentCheckIcon class="w-5 h-5 text-white" />
        </template>
      </AppStatCard>
      <AppStatCard
        label="注册用户"
        :value="stats.totalUsers"
        variant="accent"
        class="stagger-3"
      >
        <template #icon>
          <UsersIcon class="w-5 h-5 text-white" />
        </template>
      </AppStatCard>
    </div>

    <!-- 快速链接 -->
    <h2 class="section-heading mb-3">快速导航</h2>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <RouterLink
        v-for="(link, i) in [
          { to: '/admin/users', label: '用户管理', color: 'bg-primary-50 border-primary-100 text-primary-700 hover:border-primary-300' },
          { to: '/admin/reservations', label: '预约管理', color: 'bg-accent-50 border-accent-100 text-accent-700 hover:border-accent-300' },
          { to: '/admin/change-requests', label: '信息修改申请', color: 'bg-warning-50 border-warning-100 text-warning-700 hover:border-warning-300' },
          { to: '/admin/system-rules', label: '系统规则', color: 'bg-success-50 border-success-100 text-success-700 hover:border-success-300' },
        ]"
        :key="link.to"
        :to="link.to"
        class="card-hover border rounded-card p-4 text-sm font-semibold transition-all duration-200 text-center"
        :class="[link.color, `stagger-${i + 4}`]"
      >
        {{ link.label }}
        <span class="block text-xs font-normal opacity-60 mt-1">查看详情 →</span>
      </RouterLink>
    </div>
</template>
