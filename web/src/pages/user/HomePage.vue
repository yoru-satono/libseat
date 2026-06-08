<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { BuildingLibraryIcon, CalendarDaysIcon, BellIcon } from '@heroicons/vue/24/outline'
import UserLayout from '@/layouts/UserLayout.vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppButton from '@/components/common/AppButton.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import { reservationsApi } from '@/api/reservations'
import type { Reservation } from '@/types/api'

const auth = useAuthStore()
const notif = useNotificationStore()
const activeReservations = ref<Reservation[]>([])

onMounted(async () => {
  if (auth.isAuthenticated) {
    try {
      const res = await reservationsApi.list({ status: 'ACTIVE', pageSize: 3 })
      activeReservations.value = res.data.data.items
    } catch {}
  }
})
</script>

<template>
  <UserLayout>
    <!-- 英雄区 -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary-600 via-primary-700 to-primary-800 p-6 sm:p-8 text-white mb-6">
      <!-- 装饰性几何图案 -->
      <div class="absolute inset-0 opacity-10 pointer-events-none"
        style="background-image: radial-gradient(circle at 20% 80%, rgb(255 255 255) 0 1px, transparent 1px 25px), radial-gradient(circle at 80% 20%, rgb(255 255 255) 0 1px, transparent 1px 20px);" />
      <div class="absolute -top-24 -right-24 w-64 h-64 rounded-full bg-white/5 blur-2xl pointer-events-none" />

      <div class="relative z-10">
        <h1 class="text-2xl sm:text-3xl font-bold font-display mb-2">图书馆座位预约</h1>
        <p class="text-primary-200 text-sm sm:text-base mb-5 max-w-md">随时随地预约您的专属学习空间，高效、便捷、有序</p>
        <div class="flex flex-wrap gap-3">
          <RouterLink
            to="/seats"
            class="inline-flex items-center gap-2 px-5 py-2.5 bg-white text-primary-700 rounded-button text-sm font-semibold hover:bg-primary-50 hover:shadow-md transition-all duration-200"
          >
            <BuildingLibraryIcon class="w-4 h-4" />
            查找座位
          </RouterLink>
          <RouterLink
            v-if="auth.isAuthenticated"
            to="/reservations"
            class="inline-flex items-center gap-2 px-5 py-2.5 bg-white/15 text-white backdrop-blur-sm rounded-button text-sm font-medium hover:bg-white/25 transition-all duration-200 border border-white/20"
          >
            <CalendarDaysIcon class="w-4 h-4" />
            我的预约
          </RouterLink>
        </div>
      </div>
    </div>

    <!-- 已认证用户内容 -->
    <template v-if="auth.isAuthenticated">
      <!-- 未读通知提示 -->
      <RouterLink
        v-if="notif.unreadCount > 0"
        to="/notifications"
        class="flex items-center gap-3 bg-warning-50 border border-warning-200 rounded-card px-4 py-3 mb-4 hover:bg-warning-100 transition-colors animate-fade-in-up"
      >
        <div class="w-9 h-9 rounded-lg bg-warning-100 flex items-center justify-center shrink-0">
          <BellIcon class="w-5 h-5 text-warning-600" :class="{ 'animate-shake': notif.unreadCount > 0 }" />
        </div>
        <div class="flex-1">
          <p class="text-sm text-warning-800">您有 <strong class="font-semibold">{{ notif.unreadCount }}</strong> 条未读通知</p>
        </div>
        <svg class="w-4 h-4 text-warning-400 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
        </svg>
      </RouterLink>

      <!-- 当前进行中的预约 -->
      <div v-if="activeReservations.length > 0" class="animate-fade-in-up stagger-1">
        <h2 class="section-heading mb-3">当前预约</h2>
        <div class="space-y-2">
          <RouterLink
            v-for="(r, i) in activeReservations"
            :key="r.id"
            :to="`/reservations/${r.id}`"
            class="flex items-center justify-between bg-white rounded-card border border-surface-200 px-4 py-3.5 hover:border-primary-300 hover:shadow-card-hover transition-all duration-200 card-hover"
            :style="{ animationDelay: `${0.05 * (i + 2)}s` }"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-primary-50 flex items-center justify-center shrink-0">
                <CalendarDaysIcon class="w-4 h-4 text-primary-500" />
              </div>
              <div>
                <p class="text-sm font-semibold text-surface-900">{{ r.seatNo }}</p>
                <p class="text-xs text-surface-500">{{ r.date }} · {{ r.startTime }}–{{ r.endTime }}</p>
              </div>
            </div>
            <AppBadge :status="r.status" />
          </RouterLink>
        </div>
      </div>

      <!-- 无预约空状态 -->
      <div v-else class="text-center py-10 animate-fade-in-up">
        <div class="w-16 h-16 bg-surface-100 rounded-full flex items-center justify-center mx-auto mb-3">
          <CalendarDaysIcon class="w-8 h-8 text-surface-400" />
        </div>
        <p class="text-surface-500 text-sm">暂无进行中的预约</p>
        <RouterLink to="/seats" class="mt-3 inline-block">
          <AppButton variant="primary" size="sm">现在去预约</AppButton>
        </RouterLink>
      </div>
    </template>

    <!-- 未登录 -->
    <template v-else>
      <div class="text-center py-14 animate-fade-in-up">
        <div class="w-20 h-20 bg-surface-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-10 h-10 text-surface-400" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
          </svg>
        </div>
        <p class="text-surface-700 font-medium mb-1">欢迎使用 LibSeat</p>
        <p class="text-surface-400 text-sm mb-5">登录后可预约座位、查看历史记录和通知</p>
        <div class="flex justify-center gap-3">
          <RouterLink to="/login">
            <AppButton variant="primary">立即登录</AppButton>
          </RouterLink>
          <RouterLink to="/register">
            <AppButton variant="secondary">注册账号</AppButton>
          </RouterLink>
        </div>
      </div>
    </template>
  </UserLayout>
</template>
