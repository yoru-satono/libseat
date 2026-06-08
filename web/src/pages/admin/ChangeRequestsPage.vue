<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminSystemApi } from '@/api/admin/system'
import { useToast } from 'vue-toastification'
import type { AdminChangeRequest } from '@/types/api'

const toast = useToast()
const items = ref<AdminChangeRequest[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20

const reviewModal = ref(false)
const reviewTarget = ref<AdminChangeRequest | null>(null)
const reviewNote = ref('')
const reviewAction = ref<'APPROVED' | 'REJECTED'>('APPROVED')

async function fetchItems() {
  loading.value = true
  try {
    const res = await adminSystemApi.listChangeRequests({ page: page.value, pageSize })
    items.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchItems)

function openReview(item: AdminChangeRequest, action: 'APPROVED' | 'REJECTED') {
  reviewTarget.value = item
  reviewAction.value = action
  reviewNote.value = ''
  reviewModal.value = true
}

async function confirmReview() {
  if (!reviewTarget.value) return
  try {
    await adminSystemApi.reviewChangeRequest(reviewTarget.value.id, reviewAction.value, reviewNote.value || undefined)
    toast.success(reviewAction.value === 'APPROVED' ? '已批准' : '已拒绝')
    reviewModal.value = false
    fetchItems()
  } catch (e: any) {
    toast.error(e?.message || '操作失败')
  }
}

const FIELD_LABEL: Record<string, string> = { realName: '姓名', userNo: '学号/工号', department: '院系' }
</script>

<template>

    <AppEmptyState v-if="!loading && items.length === 0" title="暂无待审核申请" />

    <div v-else class="space-y-3">
      <div v-for="(item, i) in items" :key="item.id"
        class="card p-4 animate-fade-in-up"
        :style="{ animationDelay: `${0.05 * i}s` }"
      >
        <div class="flex items-start justify-between mb-2">
          <div class="flex-1 min-w-0">
            <p class="font-semibold text-sm text-surface-900">{{ item.realName }} <span class="text-surface-500 font-normal text-xs">({{ item.userNo }})</span></p>
            <p class="text-xs text-surface-500 mt-1">
              申请修改 <strong class="text-surface-700">{{ FIELD_LABEL[item.fieldName] || item.fieldName }}</strong>：
              <span class="line-through text-surface-400">{{ item.oldValue }}</span>
              →
              <span class="text-primary-700 font-semibold">{{ item.newValue }}</span>
            </p>
            <p class="text-xs text-surface-400 mt-0.5">{{ item.createdAt.slice(0, 10) }}</p>
          </div>
          <AppBadge :status="item.status" />
        </div>

        <div v-if="item.status === 'PENDING'" class="flex gap-2 mt-3 pt-3 border-t border-surface-100">
          <AppButton variant="success" size="sm" class="flex-1" @click="openReview(item, 'APPROVED')">批准</AppButton>
          <AppButton variant="danger" size="sm" class="flex-1" @click="openReview(item, 'REJECTED')">拒绝</AppButton>
        </div>
      </div>
    </div>

    <AppPagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
      @update:page="p => { page = p; fetchItems() }" />

    <!-- 审核弹窗 -->
    <AppModal :open="reviewModal" :title="reviewAction === 'APPROVED' ? '批准申请' : '拒绝申请'" @close="reviewModal = false">
      <div v-if="reviewTarget" class="space-y-3">
        <p class="text-sm text-surface-600">
          {{ reviewAction === 'APPROVED' ? '确认批准' : '确认拒绝' }}
          「{{ reviewTarget.realName }}」修改 {{ FIELD_LABEL[reviewTarget.fieldName] }} 为「{{ reviewTarget.newValue }}」？
        </p>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1.5">审核备注（选填）</label>
          <textarea v-model="reviewNote" rows="2" class="input-field resize-none" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="reviewModal = false">返回</AppButton>
          <AppButton :variant="reviewAction === 'APPROVED' ? 'success' : 'danger'" class="flex-1" @click="confirmReview">
            {{ reviewAction === 'APPROVED' ? '确认批准' : '确认拒绝' }}
          </AppButton>
        </div>
      </template>
    </AppModal>
</template>
