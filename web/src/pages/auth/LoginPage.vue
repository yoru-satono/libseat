<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import AppButton from '@/components/common/AppButton.vue'

const auth = useAuthStore()
const notif = useNotificationStore()
const router = useRouter()
const route = useRoute()

const userNo = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const ERROR_MSG: Record<string, string> = {
  A0600: '账号或密码错误',
  A0601: '账号已锁定，请稍后重试',
  A0602: '账号已暂停预约权限',
  A0603: '账号未激活，请先激活邮箱',
}

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(userNo.value.trim(), password.value)
    notif.startPolling()
    const redirect = route.query.redirect as string
    router.push(redirect || (auth.isAdmin ? '/admin' : '/'))
  } catch (e: any) {
    error.value = ERROR_MSG[e?.code] || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h2 class="font-display text-xl font-semibold text-surface-900 mb-6">登录账号</h2>

    <form @submit.prevent="submit" class="space-y-4">
      <div>
        <label for="userNo" class="block text-sm font-medium text-surface-700 mb-1.5">学号 / 工号</label>
        <input
          id="userNo"
          v-model="userNo"
          type="text"
          autocomplete="username"
          placeholder="如 S20240001"
          class="input-field"
          required
        />
      </div>
      <div>
        <div class="flex items-center justify-between mb-1.5">
          <label for="password" class="block text-sm font-medium text-surface-700">密码</label>
          <RouterLink to="/forgot-password" class="text-xs text-primary-600 hover:text-primary-700 transition-colors">忘记密码？</RouterLink>
        </div>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          class="input-field"
          required
        />
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="flex items-center gap-2 bg-danger-50 border border-danger-200 rounded-lg px-3 py-2.5 text-sm text-danger-600">
        <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <AppButton type="submit" variant="primary" :loading="loading" full-width size="lg">
        登录
      </AppButton>
    </form>

    <div class="mt-4 text-center text-sm text-surface-500">
      没有账号？
      <RouterLink to="/register" class="text-primary-600 hover:text-primary-700 font-medium transition-colors">立即注册</RouterLink>
    </div>
  </AuthLayout>
</template>
