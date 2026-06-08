<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  HomeIcon,
  BuildingLibraryIcon,
  CalendarDaysIcon,
  ClockIcon,
  UserCircleIcon,
  BellIcon,
  ArrowRightEndOnRectangleIcon,
} from '@heroicons/vue/24/outline'
import {
  HomeIcon as HomeSolid,
  BuildingLibraryIcon as BuildingLibrarySolid,
  CalendarDaysIcon as CalendarDaysSolid,
  ClockIcon as ClockSolid,
  UserCircleIcon as UserCircleSolid,
} from '@heroicons/vue/24/solid'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const auth = useAuthStore()
const notif = useNotificationStore()
const router = useRouter()
const bellShaking = ref(false)

onMounted(() => {
  if (auth.isAuthenticated) notif.startPolling()
})

onUnmounted(() => notif.stopPolling())

// 新通知时铃铛抖动
watch(() => notif.unreadCount, (now, prev) => {
  if (now > 0 && (prev === 0 || now > prev)) {
    bellShaking.value = true
    setTimeout(() => (bellShaking.value = false), 500)
  }
})

async function logout() {
  await auth.logout()
  router.push('/login')
}

const navLinks = computed(() => [
  { to: '/', label: '首页', icon: HomeIcon, solid: HomeSolid, name: 'home' },
  { to: '/seats', label: '找座位', icon: BuildingLibraryIcon, solid: BuildingLibrarySolid, name: 'seats' },
  ...(auth.isAuthenticated
    ? [
        { to: '/reservations', label: '我的预约', icon: CalendarDaysIcon, solid: CalendarDaysSolid, name: 'reservations' },
        { to: '/waitlists', label: '等待队列', icon: ClockIcon, solid: ClockSolid, name: 'waitlists' },
        { to: '/profile', label: '我的', icon: UserCircleIcon, solid: UserCircleSolid, name: 'profile' },
      ]
    : []),
])

// 路由名到图标索引的映射
const activeIndex = computed(() => {
  const name = router.currentRoute.value.name as string
  return navLinks.value.findIndex(l => l.name === name || router.currentRoute.value.path.startsWith(l.to))
})
</script>

<template>
  <div class="min-h-screen flex flex-col bg-surface-50">
    <!-- 顶部导航栏 -->
    <header class="bg-white/80 backdrop-blur-md border-b border-surface-200 sticky top-0 z-30 shadow-sm">
      <div class="max-w-6xl mx-auto px-4 h-14 flex items-center gap-6">
        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 text-primary-600 shrink-0 group">
          <svg class="w-6 h-6 transition-transform duration-300 group-hover:scale-110" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
          </svg>
          <span class="font-bold text-lg font-display">LibSeat</span>
        </RouterLink>

        <!-- 桌面端导航链接 -->
        <nav class="hidden lg:flex items-center gap-0.5 flex-1">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="relative flex items-center gap-1.5 px-3 py-2 text-sm text-surface-600 hover:text-surface-900 hover:bg-surface-100 rounded-lg transition-all duration-200"
            active-class="text-primary-700 bg-primary-50/60 font-medium"
          >
            <component :is="link.icon" class="w-4 h-4" />
            {{ link.label }}
            <!-- 活跃指示条 -->
            <span class="absolute bottom-0 left-1/2 -translate-x-1/2 w-6 h-0.5 bg-primary-500 rounded-full opacity-0 transition-all duration-200"
              :class="{ '!opacity-100 w-full': router.currentRoute.value.path.startsWith(link.to) && link.to !== '/' ? true : router.currentRoute.value.path === link.to }" />
          </RouterLink>
        </nav>

        <!-- 右侧操作区 -->
        <div class="ml-auto flex items-center gap-1">
          <template v-if="auth.isAuthenticated">
            <!-- 通知铃铛 -->
            <RouterLink
              to="/notifications"
              class="relative p-2 rounded-lg text-surface-500 hover:text-surface-700 hover:bg-surface-100 transition-all duration-200"
              :class="{ 'animate-shake': bellShaking }"
            >
              <BellIcon class="w-5 h-5" />
              <span
                v-if="notif.unreadCount > 0"
                class="absolute top-1 right-1 min-w-[18px] h-[18px] bg-danger-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1 animate-scale-in"
              >{{ notif.unreadCount > 9 ? '9+' : notif.unreadCount }}</span>
            </RouterLink>

            <!-- 管理后台按钮 -->
            <span v-if="auth.isAdmin" class="hidden lg:block">
              <RouterLink
                to="/admin"
                class="px-3 py-1.5 text-sm font-medium bg-accent-500 text-white rounded-button hover:bg-accent-600 transition-all duration-200 shadow-sm"
              >管理后台</RouterLink>
            </span>

            <!-- 退出登录 -->
            <button
              @click="logout"
              class="hidden lg:flex items-center gap-1 p-2 rounded-lg text-surface-400 hover:text-danger-500 hover:bg-danger-50 transition-all duration-200"
              title="退出登录"
            >
              <ArrowRightEndOnRectangleIcon class="w-5 h-5" />
            </button>
          </template>
          <template v-else>
            <RouterLink
              to="/login"
              class="hidden lg:block px-4 py-1.5 text-sm font-medium text-primary-600 hover:text-primary-700 transition-colors"
            >登录</RouterLink>
            <RouterLink
              to="/register"
              class="hidden lg:block px-4 py-1.5 text-sm font-medium bg-primary-600 text-white rounded-button hover:bg-primary-700 transition-all duration-200 shadow-sm"
            >注册</RouterLink>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="flex-1 max-w-6xl w-full mx-auto px-4 py-6 pb-24 lg:pb-6">
      <slot />
    </main>

    <!-- 移动端底部导航栏 -->
    <nav class="lg:hidden fixed bottom-0 left-0 right-0 z-30 bg-white/80 backdrop-blur-md border-t border-surface-200 shadow-[0_-1px_3px_rgba(0,0,0,0.04)]">
      <div class="flex max-w-lg mx-auto">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="flex-1 flex flex-col items-center gap-0.5 py-2 text-[10px] text-surface-400 hover:text-surface-600 transition-colors duration-200"
          active-class="text-primary-600"
        >
          <component :is="$route.path.startsWith(link.to) && link.to !== '/' ? link.solid : ($route.path === link.to ? link.solid : link.icon)" class="w-5 h-5 transition-transform duration-200"
            :class="{ 'scale-110': $route.path === link.to || (link.to !== '/' && $route.path.startsWith(link.to)) }" />
          <span>{{ link.label }}</span>
        </RouterLink>
        <!-- 未登录时显示登录入口 -->
        <RouterLink
          v-if="!auth.isAuthenticated"
          to="/login"
          class="flex-1 flex flex-col items-center gap-0.5 py-2 text-[10px] text-surface-400 hover:text-surface-600 transition-colors"
          active-class="text-primary-600"
        >
          <UserCircleIcon class="w-5 h-5" />
          <span>登录</span>
        </RouterLink>
      </div>
    </nav>
  </div>
</template>
