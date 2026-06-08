<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import AppBadge from '@/components/common/AppBadge.vue'
import AppAvatar from '@/components/common/AppAvatar.vue'
import AppCard from '@/components/common/AppCard.vue'
import { adminUsersApi } from '@/api/admin/users'
import type { AdminUser } from '@/types/api'

const route = useRoute()
const user = ref<AdminUser | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await adminUsersApi.get(route.params.id as string)
    user.value = res.data.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
    <!-- 返回链接 -->
    <RouterLink
      :to="{ name: 'admin-users' }"
      class="inline-flex items-center gap-1.5 text-sm text-surface-500 hover:text-surface-700 transition-colors mb-4"
    >
      <ArrowLeftIcon class="w-4 h-4" /> 用户管理
    </RouterLink>

    <!-- 骨架加载 -->
    <div v-if="loading" class="skeleton h-64 rounded-2xl max-w-lg" />

    <div v-else-if="user" class="max-w-lg space-y-4 animate-fade-in-up">
      <!-- 用户头像 + 姓名 -->
      <div class="flex items-center gap-4">
        <AppAvatar :name="user.realName" size="xl" />
        <div>
          <h1 class="font-display text-xl font-bold text-surface-900">{{ user.realName }}</h1>
          <p class="text-sm text-surface-500 font-mono">{{ user.userNo }}</p>
        </div>
      </div>

      <!-- 信息卡片 -->
      <AppCard padding="lg">
        <div class="flex items-center gap-2 mb-4">
          <AppBadge :status="user.status" />
          <span class="text-sm text-surface-600">{{ user.role === 'STUDENT' ? '学生' : user.role === 'TEACHER' ? '教师' : '管理员' }}</span>
        </div>

        <div class="grid grid-cols-2 gap-y-3 text-sm">
          <span class="text-surface-500">邮箱</span>
          <span class="text-surface-900 truncate">{{ user.email }}</span>

          <span class="text-surface-500">手机</span>
          <span class="text-surface-900">{{ user.phone || '—' }}</span>

          <span class="text-surface-500">院系</span>
          <span class="text-surface-900">{{ user.department || '—' }}</span>

          <span class="text-surface-500">爽约次数</span>
          <span class="text-surface-900 font-medium">{{ user.noShowCount }}</span>

          <span class="text-surface-500">失败登录</span>
          <span class="text-surface-900">{{ user.failedLoginCount }}</span>

          <span class="text-surface-500">注册时间</span>
          <span class="text-surface-900">{{ user.createdAt.slice(0,10) }}</span>

          <span class="text-surface-500">最后登录</span>
          <span class="text-surface-900">{{ user.lastLoginAt?.slice(0,10) || '—' }}</span>
        </div>
      </AppCard>
    </div>
</template>
