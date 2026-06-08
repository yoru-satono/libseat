<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { authApi } from '@/api/auth'
import { useToast } from 'vue-toastification'
import AppButton from '@/components/common/AppButton.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const token = ref('')
const newPassword = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => { token.value = route.query.token as string || '' })

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await authApi.resetPassword(token.value, newPassword.value)
    toast.success('密码重置成功！')
    router.push('/login')
  } catch (e: any) {
    error.value = e?.message || '重置失败，链接可能已过期'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h2 class="font-display text-xl font-semibold text-surface-900 mb-6">重置密码</h2>
    <form @submit.prevent="submit" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-surface-700 mb-1.5">新密码</label>
        <input v-model="newPassword" type="password" minlength="8" placeholder="至少8位，含字母和数字"
          class="input-field" required />
      </div>
      <div v-if="error" class="flex items-center gap-2 bg-danger-50 border border-danger-200 rounded-lg px-3 py-2.5 text-sm text-danger-600">
        <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
        </svg>
        <span>{{ error }}</span>
      </div>
      <AppButton type="submit" variant="primary" :loading="loading" :disabled="!token" full-width>
        确认重置
      </AppButton>
    </form>
  </AuthLayout>
</template>
