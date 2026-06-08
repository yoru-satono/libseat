<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import AppBadge from '@/components/common/AppBadge.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminUsersApi } from '@/api/admin/users'
import { useToast } from 'vue-toastification'
import type { AdminUser } from '@/types/api'

const toast = useToast()
const users = ref<AdminUser[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20

const filter = reactive({ role: '', status: '', keyword: '' })

const editModal = ref(false)
const editTarget = ref<AdminUser | null>(null)
const editForm = reactive({ status: '', newPassword: '', resetNoShowCount: false })

async function fetchUsers() {
  loading.value = true
  try {
    const res = await adminUsersApi.list({
      role: filter.role || undefined,
      status: filter.status || undefined,
      page: page.value, pageSize,
    })
    users.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchUsers)

function openEdit(user: AdminUser) {
  editTarget.value = user
  Object.assign(editForm, { status: user.status, newPassword: '', resetNoShowCount: false })
  editModal.value = true
}

async function saveEdit() {
  if (!editTarget.value) return
  try {
    const data: Record<string, unknown> = {}
    if (editForm.status !== editTarget.value.status) data.status = editForm.status
    if (editForm.newPassword) data.newPassword = editForm.newPassword
    if (editForm.resetNoShowCount) data.resetNoShowCount = true
    await adminUsersApi.update(editTarget.value.id, data)
    toast.success('已更新')
    editModal.value = false
    fetchUsers()
  } catch (e: any) {
    toast.error(e?.message || '更新失败')
  }
}
</script>

<template>

    <!-- 筛选栏 -->
    <div class="flex flex-wrap gap-2 mb-4">
      <input v-model="filter.keyword" type="text" placeholder="搜索学号/姓名" class="input-field w-44 py-2" />
      <select v-model="filter.role" class="input-field w-28 py-2">
        <option value="">全部角色</option>
        <option value="STUDENT">学生</option>
        <option value="TEACHER">教师</option>
        <option value="ADMIN">管理员</option>
      </select>
      <select v-model="filter.status" class="input-field w-28 py-2">
        <option value="">全部状态</option>
        <option value="ACTIVE">正常</option>
        <option value="INACTIVE">未激活</option>
        <option value="LOCKED">已锁定</option>
        <option value="SUSPENDED">已暂停</option>
      </select>
      <AppButton variant="primary" size="sm" @click="() => { page = 1; fetchUsers() }">查询</AppButton>
    </div>

    <!-- 内容 -->
    <div v-if="loading" class="space-y-2">
      <div v-for="n in 5" :key="n" class="skeleton h-12 rounded-lg" />
    </div>
    <AppEmptyState v-else-if="users.length === 0" type="search" title="未找到用户" />

    <div v-else>
      <!-- 桌面端表格 -->
      <div class="hidden md:block card overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-surface-100 bg-surface-50 text-surface-600">
              <th class="text-left px-4 py-3 font-medium">学号/工号</th>
              <th class="text-left px-4 py-3 font-medium">姓名</th>
              <th class="text-left px-4 py-3 font-medium">角色</th>
              <th class="text-left px-4 py-3 font-medium">状态</th>
              <th class="text-left px-4 py-3 font-medium">爽约</th>
              <th class="text-right px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-surface-50">
            <tr v-for="u in users" :key="u.id" class="hover:bg-surface-50 transition-colors">
              <td class="px-4 py-3 text-surface-900 font-mono text-xs">{{ u.userNo }}</td>
              <td class="px-4 py-3 text-surface-900">{{ u.realName }}</td>
              <td class="px-4 py-3 text-surface-600">{{ u.role === 'STUDENT' ? '学生' : u.role === 'TEACHER' ? '教师' : '管理员' }}</td>
              <td class="px-4 py-3"><AppBadge :status="u.status" /></td>
              <td class="px-4 py-3 text-surface-600">{{ u.noShowCount }}</td>
              <td class="px-4 py-3 text-right">
                <RouterLink :to="`/admin/users/${u.id}`" class="text-primary-600 hover:text-primary-700 text-xs font-medium mr-3">详情</RouterLink>
                <button @click="openEdit(u)" class="text-primary-600 hover:text-primary-700 text-xs font-medium">编辑</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 移动端卡片 -->
      <div class="md:hidden space-y-2">
        <div v-for="u in users" :key="u.id" class="card p-4 animate-fade-in-up">
          <div class="flex items-start justify-between mb-1">
            <div>
              <p class="font-semibold text-surface-900">{{ u.realName }}</p>
              <p class="text-xs text-surface-500 font-mono">{{ u.userNo }}</p>
            </div>
            <AppBadge :status="u.status" />
          </div>
          <div class="flex items-center justify-between mt-2 pt-2 border-t border-surface-50">
            <span class="text-xs text-surface-500">{{ u.role === 'STUDENT' ? '学生' : u.role === 'TEACHER' ? '教师' : '管理员' }} · 爽约 {{ u.noShowCount }} 次</span>
            <div class="flex gap-2">
              <RouterLink :to="`/admin/users/${u.id}`" class="text-xs text-primary-600 font-medium">详情</RouterLink>
              <button @click="openEdit(u)" class="text-xs text-primary-600 font-medium">编辑</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AppPagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchUsers() }" />

    <!-- 编辑弹窗 -->
    <AppModal :open="editModal" title="编辑用户" @close="editModal = false">
      <div v-if="editTarget" class="space-y-4">
        <div class="bg-surface-50 rounded-card border border-surface-100 px-3 py-2.5 text-sm">
          <p class="font-semibold">{{ editTarget.realName }}</p>
          <p class="text-surface-500 text-xs">{{ editTarget.userNo }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">账号状态</label>
          <select v-model="editForm.status" class="input-field">
            <option value="ACTIVE">正常 (ACTIVE)</option>
            <option value="LOCKED">锁定 (LOCKED)</option>
            <option value="SUSPENDED">暂停 (SUSPENDED)</option>
            <option value="INACTIVE">未激活 (INACTIVE)</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">重置密码（留空则不修改）</label>
          <input v-model="editForm.newPassword" type="password" class="input-field" />
        </div>
        <label class="flex items-center gap-2 text-sm text-surface-700 cursor-pointer">
          <input v-model="editForm.resetNoShowCount" type="checkbox" class="rounded text-primary-600 focus:ring-primary-400" />
          清零爽约次数
        </label>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="editModal = false">取消</AppButton>
          <AppButton variant="primary" class="flex-1" @click="saveEdit">保存</AppButton>
        </div>
      </template>
    </AppModal>
</template>
