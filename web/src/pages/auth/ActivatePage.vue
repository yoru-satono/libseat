<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { authApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const state = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('')

onMounted(async () => {
  const token = route.query.token as string
  if (!token) { state.value = 'error'; message.value = '缺少激活令牌'; return }
  try {
    await authApi.activate(token)
    state.value = 'success'
    setTimeout(() => router.push('/login'), 2000)
  } catch (e: any) {
    state.value = 'error'
    message.value = e?.message || '激活失败，链接可能已过期'
  }
})
</script>

<template>
  <AuthLayout>
    <div class="text-center py-6">
      <!-- 加载中 -->
      <div v-if="state === 'loading'" class="space-y-3">
        <div class="w-10 h-10 border-2 border-primary-200 border-t-primary-500 rounded-full animate-spin mx-auto" />
        <p class="text-surface-500 text-sm">正在激活账号…</p>
      </div>

      <!-- 成功 -->
      <div v-else-if="state === 'success'" class="animate-scale-in">
        <div class="w-14 h-14 bg-success-50 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-success-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        </div>
        <p class="font-semibold text-surface-900 text-lg font-display">账号激活成功！</p>
        <p class="text-sm text-surface-500 mt-1">即将跳转到登录页…</p>
      </div>

      <!-- 失败 -->
      <div v-else class="animate-fade-in">
        <div class="w-14 h-14 bg-danger-50 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-danger-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <p class="font-semibold text-surface-900 text-lg font-display">激活失败</p>
        <p class="text-sm text-surface-500 mt-1">{{ message }}</p>
        <RouterLink to="/login" class="mt-5 inline-block">
          <span class="px-5 py-2 text-sm font-medium bg-primary-600 text-white rounded-button hover:bg-primary-700 transition-colors">返回登录</span>
        </RouterLink>
      </div>
    </div>
  </AuthLayout>
</template>
