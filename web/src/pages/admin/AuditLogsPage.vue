<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminSystemApi } from '@/api/admin/system'
import type { AuditLog } from '@/types/api'

const items = ref<AuditLog[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const filter = reactive({ dateFrom: '', dateTo: '', targetType: '' })

async function fetchItems() {
  loading.value = true
  try {
    const res = await adminSystemApi.listAuditLogs({
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
      targetType: filter.targetType || undefined,
      page: page.value, pageSize,
    })
    items.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchItems)
</script>

<template>

    <!-- 筛选栏 -->
    <div class="flex flex-wrap gap-2 mb-4 items-center">
      <input v-model="filter.dateFrom" type="date" class="input-field w-36 py-2" />
      <span class="text-surface-400 text-sm">至</span>
      <input v-model="filter.dateTo" type="date" class="input-field w-36 py-2" />
      <select v-model="filter.targetType" class="input-field w-32 py-2">
        <option value="">全部类型</option>
        <option value="user">用户</option>
        <option value="change_request">修改申请</option>
        <option value="rule">系统规则</option>
      </select>
      <AppButton variant="primary" size="sm" @click="() => { page = 1; fetchItems() }">查询</AppButton>
    </div>

    <AppEmptyState v-if="!loading && items.length === 0" title="暂无审计记录" />

    <div v-else class="card overflow-hidden animate-fade-in-up">
      <!-- 桌面端表格 -->
      <table class="w-full text-sm hidden md:table">
        <thead>
          <tr class="border-b border-surface-100 bg-surface-50 text-surface-600">
            <th class="text-left px-4 py-3 font-medium">时间</th>
            <th class="text-left px-4 py-3 font-medium">管理员</th>
            <th class="text-left px-4 py-3 font-medium">操作</th>
            <th class="text-left px-4 py-3 font-medium">目标类型</th>
            <th class="text-left px-4 py-3 font-medium">IP</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-surface-50">
          <tr v-for="log in items" :key="log.logId" class="hover:bg-surface-50 transition-colors">
            <td class="px-4 py-3 text-surface-500 text-xs whitespace-nowrap">{{ log.createdAt.slice(0,16).replace('T',' ') }}</td>
            <td class="px-4 py-3 text-surface-900 font-medium">{{ log.adminName }}</td>
            <td class="px-4 py-3 text-surface-700">{{ log.actionType }}</td>
            <td class="px-4 py-3 text-surface-700">{{ log.targetType }}</td>
            <td class="px-4 py-3 text-surface-500 text-xs font-mono">{{ log.ipAddress || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 移动端 -->
      <div class="md:hidden divide-y divide-surface-100">
        <div v-for="log in items" :key="log.logId" class="px-4 py-3">
          <p class="text-sm font-semibold text-surface-900">{{ log.adminName }} · <span class="text-surface-600 font-normal">{{ log.actionType }}</span></p>
          <p class="text-xs text-surface-500 mt-0.5">{{ log.targetType }} · {{ log.createdAt.slice(0,16).replace('T',' ') }}</p>
        </div>
      </div>
    </div>

    <AppPagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchItems() }" />
</template>
