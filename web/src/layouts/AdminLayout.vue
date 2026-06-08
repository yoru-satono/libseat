<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import {
  HomeIcon,
  UsersIcon,
  CalendarDaysIcon,
  DocumentCheckIcon,
  BuildingLibraryIcon,
  QueueListIcon,
  CogIcon,
  ClipboardDocumentListIcon,
  Bars3Icon,
  XMarkIcon,
  ArrowLeftEndOnRectangleIcon,
} from '@heroicons/vue/24/outline'
import { Dialog, DialogPanel } from '@headlessui/vue'
import { useAuthStore } from '@/stores/auth'
import AppAvatar from '@/components/common/AppAvatar.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const sidebarOpen = ref(false)

async function logout() {
  await auth.logout()
  router.push('/login')
}

const navItems = [
  { to: '/admin', label: '仪表盘', icon: HomeIcon },
  { to: '/admin/users', label: '用户管理', icon: UsersIcon },
  { to: '/admin/reservations', label: '预约管理', icon: CalendarDaysIcon },
  { to: '/admin/change-requests', label: '信息修改申请', icon: DocumentCheckIcon },
  { to: '/admin/libraries', label: '图书馆管理', icon: BuildingLibraryIcon },
  { to: '/admin/seats', label: '座位管理', icon: QueueListIcon },
  { to: '/admin/system-rules', label: '系统规则', icon: CogIcon },
  { to: '/admin/audit-logs', label: '审计日志', icon: ClipboardDocumentListIcon },
]

function isActive(to: string): boolean {
  if (to === '/admin') return router.currentRoute.value.path === '/admin'
  return router.currentRoute.value.path.startsWith(to)
}
</script>

<template>
  <div class="min-h-screen flex bg-surface-50">
    <!-- ===== 桌面端侧边栏 ===== -->
    <aside class="hidden lg:flex lg:flex-col w-60 bg-surface-900 shrink-0 relative overflow-hidden">
      <!-- 微妙的纹理叠加 -->
      <div class="absolute inset-0 opacity-[0.03] pointer-events-none"
        style="background-image: repeating-linear-gradient(0deg, transparent, transparent 2px, rgb(255 255 255) 2px, rgb(255 255 255) 3px);" />

      <!-- Logo -->
      <div class="relative z-10 h-14 flex items-center gap-2.5 px-5 border-b border-surface-700">
        <RouterLink to="/" class="flex items-center gap-2.5 text-white group">
          <svg class="w-6 h-6 text-primary-400 transition-transform duration-300 group-hover:scale-110" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
          </svg>
          <span class="font-bold text-sm font-display">LibSeat 管理后台</span>
        </RouterLink>
      </div>

      <!-- 导航 -->
      <nav class="relative z-10 flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-surface-800 hover:text-white transition-all duration-200 relative"
          :class="isActive(item.to) ? 'bg-surface-800 text-white' : ''"
        >
          <!-- 活跃状态左侧强调条 -->
          <span
            class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-accent-500 rounded-r-full transition-all duration-300 ease-out-back"
            :class="isActive(item.to) ? 'opacity-100 h-8' : 'opacity-0 h-0'"
          />
          <component :is="item.icon" class="w-4 h-4 shrink-0 transition-colors duration-200"
            :class="isActive(item.to) ? 'text-accent-400' : 'text-surface-400 group-hover:text-surface-300'" />
          <span :class="isActive(item.to) ? 'font-medium' : ''">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 底部用户信息 -->
      <div class="relative z-10 px-3 py-3 border-t border-surface-700 space-y-2">
        <div class="flex items-center gap-2.5 px-1">
          <AppAvatar :name="auth.currentUser?.realName ?? '?'" size="sm" />
          <div class="min-w-0 flex-1">
            <p class="text-xs text-surface-300 truncate font-medium">{{ auth.currentUser?.realName }}</p>
            <p class="text-[10px] text-surface-500 truncate">{{ auth.currentUser?.userNo }}</p>
          </div>
        </div>
        <button
          @click="logout"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-surface-400 hover:text-danger-400 hover:bg-surface-800 transition-all duration-200"
        >
          <ArrowLeftEndOnRectangleIcon class="w-4 h-4" />
          退出登录
        </button>
      </div>
    </aside>

    <!-- ===== 移动端侧边抽屉 ===== -->
    <Dialog :open="sidebarOpen" @close="sidebarOpen = false" class="lg:hidden">
      <Transition
        enter="ease-out duration-200"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-150"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div v-show="sidebarOpen" class="fixed inset-0 bg-black/50 backdrop-blur-[2px] z-40" aria-hidden="true" />
      </Transition>
      <DialogPanel class="fixed inset-y-0 left-0 w-64 bg-surface-900 z-50 flex flex-col">
        <div class="h-14 flex items-center justify-between px-4 border-b border-surface-700">
          <span class="text-white font-bold text-sm font-display">管理后台</span>
          <button @click="sidebarOpen = false" class="text-surface-400 hover:text-white p-1.5 rounded-lg hover:bg-surface-800 transition-colors">
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>
        <nav class="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-surface-300 hover:bg-surface-800 hover:text-white transition-all duration-200 relative"
            :class="isActive(item.to) ? 'bg-surface-800 text-white' : ''"
            @click="sidebarOpen = false"
          >
            <span
              class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] bg-accent-500 rounded-r-full transition-all duration-300"
              :class="isActive(item.to) ? 'opacity-100 h-8' : 'opacity-0 h-0'"
            />
            <component :is="item.icon" class="w-4 h-4 shrink-0"
              :class="isActive(item.to) ? 'text-accent-400' : 'text-surface-400'" />
            {{ item.label }}
          </RouterLink>
        </nav>
        <div class="px-3 py-3 border-t border-surface-700">
          <button @click="logout" class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-surface-400 hover:text-danger-400 hover:bg-surface-800 transition-colors">
            <ArrowLeftEndOnRectangleIcon class="w-4 h-4" />退出登录
          </button>
        </div>
      </DialogPanel>
    </Dialog>

    <!-- ===== 主内容区 ===== -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶部栏 -->
      <header class="bg-white/80 backdrop-blur-md border-b border-surface-200 h-14 flex items-center px-4 gap-3 sticky top-0 z-20 shadow-sm">
        <button
          @click="sidebarOpen = true"
          class="lg:hidden p-2 rounded-lg text-surface-500 hover:text-surface-700 hover:bg-surface-100 transition-colors"
        >
          <Bars3Icon class="w-5 h-5" />
        </button>
        <div class="flex-1 min-w-0">
          <span class="text-sm font-medium text-surface-700">{{ route.meta?.title || '' }}</span>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="flex-1 p-4 lg:p-6 overflow-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>
