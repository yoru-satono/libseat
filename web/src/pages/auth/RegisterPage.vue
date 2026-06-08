<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { authApi } from '@/api/auth'
import { useToast } from 'vue-toastification'
import AppButton from '@/components/common/AppButton.vue'

const router = useRouter()
const toast = useToast()

const form = ref({
  userNo: '', realName: '', email: '', password: '', phone: '', department: '',
})
const loading = ref(false)
const error = ref('')

const ERROR_MSG: Record<string, string> = {
  A0500: '该学号/工号已注册',
  A0501: '该邮箱已被注册',
  A0400: '请检查输入格式',
}

// 密码强度指示器
const passwordStrength = computed(() => {
  const pw = form.value.password
  if (!pw) return null
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/[a-zA-Z]/.test(pw) && /\d/.test(pw)) score++
  if (/[^a-zA-Z0-9]/.test(pw)) score++
  if (score <= 1) return { level: '弱', color: 'bg-danger-400', width: 'w-1/4' }
  if (score === 2) return { level: '一般', color: 'bg-warning-400', width: 'w-2/4' }
  if (score === 3) return { level: '较好', color: 'bg-accent-400', width: 'w-3/4' }
  return { level: '强', color: 'bg-success-400', width: 'w-full' }
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await authApi.register({
      userNo: form.value.userNo.trim(),
      realName: form.value.realName.trim(),
      email: form.value.email.trim(),
      password: form.value.password,
      phone: form.value.phone || undefined,
      department: form.value.department || undefined,
    })
    toast.success('注册成功！请前往邮箱激活账号。')
    router.push('/login')
  } catch (e: any) {
    error.value = ERROR_MSG[e?.code] || e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h2 class="font-display text-xl font-semibold text-surface-900 mb-6">注册账号</h2>

    <form @submit.prevent="submit" class="space-y-3">
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">学号 / 工号 <span class="text-danger-500">*</span></label>
          <input v-model="form.userNo" type="text" placeholder="S20240001" class="input-field" required />
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">姓名 <span class="text-danger-500">*</span></label>
          <input v-model="form.realName" type="text" class="input-field" required />
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-surface-700 mb-1">邮箱 <span class="text-danger-500">*</span></label>
        <input v-model="form.email" type="email" class="input-field" required />
      </div>
      <div>
        <label class="block text-sm font-medium text-surface-700 mb-1">密码 <span class="text-danger-500">*</span></label>
        <input v-model="form.password" type="password" placeholder="至少8位，含字母和数字"
          class="input-field" minlength="8" required />
        <!-- 密码强度指示器 -->
        <div v-if="passwordStrength && form.password" class="mt-1.5 flex items-center gap-2">
          <div class="flex-1 h-1 bg-surface-200 rounded-full overflow-hidden">
            <div class="h-full rounded-full transition-all duration-300" :class="[passwordStrength.color, passwordStrength.width]" />
          </div>
          <span class="text-xs text-surface-500">{{ passwordStrength.level }}</span>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">手机（选填）</label>
          <input v-model="form.phone" type="tel" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">院系（选填）</label>
          <input v-model="form.department" type="text" class="input-field" />
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="flex items-center gap-2 bg-danger-50 border border-danger-200 rounded-lg px-3 py-2.5 text-sm text-danger-600">
        <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <AppButton type="submit" variant="primary" :loading="loading" full-width size="lg">
        注册
      </AppButton>
    </form>

    <p class="mt-4 text-center text-sm text-surface-500">
      已有账号？<RouterLink to="/login" class="text-primary-600 hover:text-primary-700 font-medium transition-colors">去登录</RouterLink>
    </p>
  </AuthLayout>
</template>
