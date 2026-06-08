<script setup lang="ts">
import { ref } from 'vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { authApi } from '@/api/auth'
import { useToast } from 'vue-toastification'
import AppButton from '@/components/common/AppButton.vue'

const toast = useToast()
const email = ref('')
const loading = ref(false)
const sent = ref(false)

async function submit() {
  loading.value = true
  try {
    await authApi.resetRequest(email.value.trim())
    sent.value = true
    toast.success('重置邮件已发送，请查收')
  } catch (e: any) {
    toast.error(e?.message || '发送失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h2 class="font-display text-xl font-semibold text-surface-900 mb-2">找回密码</h2>
    <p class="text-sm text-surface-500 mb-6">我们将向您的注册邮箱发送重置链接</p>

    <div v-if="sent" class="text-center py-6">
      <div class="w-12 h-12 bg-success-50 rounded-full flex items-center justify-center mx-auto mb-4 animate-scale-in">
        <svg class="w-6 h-6 text-success-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
        </svg>
      </div>
      <p class="text-surface-700 text-sm">重置邮件已发送至 <strong>{{ email }}</strong></p>
      <p class="text-xs text-surface-400 mt-1">请查收邮件并点击链接重置密码</p>
      <RouterLink to="/login" class="mt-4 inline-block text-sm text-primary-600 hover:text-primary-700 font-medium transition-colors">返回登录</RouterLink>
    </div>
    <form v-else @submit.prevent="submit" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-surface-700 mb-1.5">注册邮箱</label>
        <input v-model="email" type="email" placeholder="yourname@example.com" class="input-field" required />
      </div>
      <AppButton type="submit" variant="primary" :loading="loading" full-width>
        发送重置邮件
      </AppButton>
      <RouterLink to="/login" class="block text-center text-sm text-surface-500 hover:text-surface-700 transition-colors">返回登录</RouterLink>
    </form>
  </AuthLayout>
</template>
