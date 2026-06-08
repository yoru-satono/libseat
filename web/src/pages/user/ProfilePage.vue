<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UserLayout from '@/layouts/UserLayout.vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppAvatar from '@/components/common/AppAvatar.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppButton from '@/components/common/AppButton.vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { reservationsApi } from '@/api/reservations'
import { useToast } from 'vue-toastification'
import type { ChangeRequest } from '@/types/api'

const auth = useAuthStore()
const toast = useToast()
const changeRequests = ref<ChangeRequest[]>([])

onMounted(async () => {
  if (!auth.currentUser) await auth.fetchCurrentUser()
  try {
    const res = await usersApi.listChangeRequests()
    changeRequests.value = res.data.data.items
  } catch {}
})

// 编辑资料
const editModal = ref(false)
const editForm = ref({ phone: '', oldPassword: '', newPassword: '' })
async function saveProfile() {
  try {
    const data: Record<string, string> = {}
    if (editForm.value.phone) data.phone = editForm.value.phone
    if (editForm.value.newPassword) {
      data.oldPassword = editForm.value.oldPassword
      data.newPassword = editForm.value.newPassword
    }
    await usersApi.updateMe(data)
    await auth.fetchCurrentUser()
    toast.success('已保存')
    editModal.value = false
  } catch (e: any) {
    toast.error(e?.message || '保存失败')
  }
}

// 变更申请
const crModal = ref(false)
const crField = ref<'realName' | 'userNo' | 'department'>('realName')
const crValue = ref('')
const CR_LABEL: Record<string, string> = { realName: '姓名', userNo: '学号/工号', department: '院系' }

async function submitCR() {
  try {
    await usersApi.createChangeRequest(crField.value, crValue.value)
    toast.success('申请已提交，等待管理员审核')
    crModal.value = false
    crValue.value = ''
    const res = await usersApi.listChangeRequests()
    changeRequests.value = res.data.data.items
  } catch (e: any) {
    toast.error(e?.message || '提交失败')
  }
}

// 导出
async function exportReservations() {
  try {
    const res = await reservationsApi.export()
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `reservations-${new Date().toISOString().slice(0,10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    toast.error(e?.message || '导出失败')
  }
}
</script>

<template>
  <UserLayout>
    <div class="max-w-lg mx-auto space-y-4 animate-fade-in-up">
      <h1 class="page-heading">个人资料</h1>

      <!-- 用户信息卡片 -->
      <AppCard v-if="auth.currentUser" padding="lg">
        <div class="flex items-start gap-4">
          <AppAvatar :name="auth.currentUser.realName" size="xl" />
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <p class="text-lg font-semibold text-surface-900 font-display">{{ auth.currentUser.realName }}</p>
              <AppBadge :status="auth.currentUser.status" />
            </div>
            <p class="text-sm text-surface-500">{{ auth.currentUser.userNo }}</p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 mt-5 pt-5 border-t border-surface-100 text-sm">
          <div>
            <p class="text-surface-500 text-xs mb-0.5">邮箱</p>
            <p class="text-surface-900">{{ auth.currentUser.email }}</p>
          </div>
          <div>
            <p class="text-surface-500 text-xs mb-0.5">手机</p>
            <p class="text-surface-900">{{ auth.currentUser.phone || '未填写' }}</p>
          </div>
          <div>
            <p class="text-surface-500 text-xs mb-0.5">院系</p>
            <p class="text-surface-900">{{ auth.currentUser.department || '未填写' }}</p>
          </div>
          <div>
            <p class="text-surface-500 text-xs mb-0.5">角色</p>
            <p class="text-surface-900">{{ auth.currentUser.role === 'STUDENT' ? '学生' : auth.currentUser.role === 'TEACHER' ? '教师' : '管理员' }}</p>
          </div>
          <div v-if="auth.currentUser.noShowCount > 0" class="col-span-2">
            <p class="text-surface-500 text-xs mb-0.5">爽约次数</p>
            <p class="text-warning-600 font-semibold">{{ auth.currentUser.noShowCount }}</p>
          </div>
        </div>

        <div class="flex gap-2 mt-5 pt-4 border-t border-surface-100">
          <AppButton variant="secondary" size="sm" class="flex-1" @click="editModal = true">编辑手机/密码</AppButton>
          <AppButton variant="ghost" size="sm" class="flex-1" @click="crModal = true">申请修改关键信息</AppButton>
        </div>
      </AppCard>

      <!-- 导出按钮 -->
      <AppButton variant="secondary" full-width @click="exportReservations">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
        </svg>
        导出预约历史（Excel）
      </AppButton>

      <!-- 信息修改申请 -->
      <AppCard padding="lg">
        <h2 class="font-semibold text-surface-900 mb-3">信息修改申请</h2>
        <AppEmptyState v-if="changeRequests.length === 0" title="暂无申请记录" />
        <div v-else class="space-y-2">
          <div v-for="cr in changeRequests" :key="cr.id" class="flex items-center justify-between py-2.5 border-b border-surface-50 last:border-0">
            <div class="text-sm flex-1 min-w-0">
              <p class="text-surface-700 truncate">{{ CR_LABEL[cr.fieldName] || cr.fieldName }}：{{ cr.oldValue }} → {{ cr.newValue }}</p>
              <p class="text-xs text-surface-400 mt-0.5">{{ cr.createdAt.slice(0,10) }}</p>
            </div>
            <AppBadge :status="cr.status" />
          </div>
        </div>
      </AppCard>
    </div>

    <!-- 编辑弹窗 -->
    <AppModal :open="editModal" title="编辑资料" @close="editModal = false">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">手机号</label>
          <input v-model="editForm.phone" type="tel" :placeholder="auth.currentUser?.phone || '请输入手机号'" class="input-field" />
        </div>
        <hr class="border-surface-100" />
        <p class="text-xs text-surface-500">修改密码（不修改则留空）</p>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">当前密码</label>
          <input v-model="editForm.oldPassword" type="password" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">新密码</label>
          <input v-model="editForm.newPassword" type="password" minlength="8" class="input-field" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="editModal = false">取消</AppButton>
          <AppButton variant="primary" class="flex-1" @click="saveProfile">保存</AppButton>
        </div>
      </template>
    </AppModal>

    <!-- 变更申请弹窗 -->
    <AppModal :open="crModal" title="申请修改关键信息" @close="crModal = false">
      <div class="space-y-4">
        <p class="text-sm text-surface-500">姓名、学号/工号、院系的修改需要管理员审核</p>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">修改字段</label>
          <select v-model="crField" class="input-field">
            <option value="realName">姓名</option>
            <option value="userNo">学号/工号</option>
            <option value="department">院系</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">新的值</label>
          <input v-model="crValue" type="text" class="input-field" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="crModal = false">取消</AppButton>
          <AppButton variant="primary" class="flex-1" :disabled="!crValue" @click="submitCR">提交申请</AppButton>
        </div>
      </template>
    </AppModal>
  </UserLayout>
</template>
