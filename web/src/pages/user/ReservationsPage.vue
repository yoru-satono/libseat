<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UserLayout from '@/layouts/UserLayout.vue'
import ReservationCard from '@/components/reservations/ReservationCard.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { reservationsApi } from '@/api/reservations'
import { useToast } from 'vue-toastification'
import type { Reservation, ReservationStatus } from '@/types/api'

const toast = useToast()
const reservations = ref<Reservation[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const activeTab = ref<ReservationStatus | 'ALL'>('ALL')

const tabs: { label: string; value: ReservationStatus | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '待签到', value: 'ACTIVE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '爽约', value: 'NO_SHOW' },
]

const cancelModal = ref(false)
const cancelId = ref('')
const cancelReason = ref('')
const cancelLoading = ref(false)

const renewModal = ref(false)
const renewId = ref('')
const renewTime = ref('')
const renewLoading = ref(false)

async function fetchReservations() {
  loading.value = true
  try {
    const res = await reservationsApi.list({
      status: activeTab.value === 'ALL' ? undefined : activeTab.value,
      page: page.value,
      pageSize,
    })
    reservations.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchReservations)

function switchTab(tab: ReservationStatus | 'ALL') {
  activeTab.value = tab
  page.value = 1
  fetchReservations()
}

function openCancel(id: string) {
  cancelId.value = id; cancelReason.value = ''; cancelModal.value = true
}
async function confirmCancel() {
  cancelLoading.value = true
  try {
    await reservationsApi.cancel(cancelId.value, cancelReason.value || undefined)
    toast.success('预约已取消')
    cancelModal.value = false
    fetchReservations()
  } catch (e: any) {
    toast.error(e?.message || '取消失败')
  } finally {
    cancelLoading.value = false
  }
}

function openRenew(id: string) {
  renewId.value = id; renewTime.value = ''; renewModal.value = true
}
async function confirmRenew() {
  if (!renewTime.value) return
  renewLoading.value = true
  try {
    await reservationsApi.renew(renewId.value, renewTime.value)
    toast.success('续约成功！')
    renewModal.value = false
    fetchReservations()
  } catch (e: any) {
    toast.error(e?.message || '续约失败')
  } finally {
    renewLoading.value = false
  }
}

async function handleCheckin(id: string) {
  const qrToken = prompt('请输入座位 QR 码令牌（通常扫描获得）:')
  if (!qrToken) return
  try {
    await reservationsApi.checkin(id, qrToken)
    toast.success('签到成功！')
    fetchReservations()
  } catch (e: any) {
    toast.error(e?.message || '签到失败')
  }
}
</script>

<template>
  <UserLayout>
    <h1 class="page-heading mb-4">我的预约</h1>

    <!-- 状态标签栏 - 下划线风格 -->
    <div class="flex gap-0 overflow-x-auto border-b border-surface-200 mb-4">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="switchTab(tab.value)"
        class="relative shrink-0 px-4 py-2.5 text-sm font-medium transition-all duration-200"
        :class="activeTab === tab.value
          ? 'text-primary-600'
          : 'text-surface-500 hover:text-surface-700'"
      >
        {{ tab.label }}
        <span
          class="absolute bottom-0 left-1/2 -translate-x-1/2 h-0.5 bg-primary-500 rounded-full transition-all duration-300"
          :class="activeTab === tab.value ? 'w-full' : 'w-0'"
        />
      </button>
    </div>

    <!-- 骨架加载 -->
    <div v-if="loading" class="space-y-3">
      <div v-for="n in 3" :key="n" class="skeleton h-28 rounded-card" />
    </div>

    <!-- 空状态 -->
    <AppEmptyState v-else-if="reservations.length === 0" type="calendar" title="暂无预约记录" />

    <!-- 卡片列表 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-3">
      <ReservationCard
        v-for="(r, i) in reservations"
        :key="r.id"
        :reservation="r"
        :style="{ animationDelay: `${0.05 * (i % 6)}s` }"
        class="animate-fade-in-up"
        @cancel="openCancel"
        @checkin="handleCheckin"
        @renew="openRenew"
      />
    </div>

    <AppPagination
      v-if="totalPages > 1"
      :page="page"
      :total-pages="totalPages"
      :total="total"
      :page-size="pageSize"
      @update:page="p => { page = p; fetchReservations() }"
    />

    <!-- 取消弹窗 -->
    <AppModal :open="cancelModal" title="取消预约" @close="cancelModal = false">
      <div class="space-y-3">
        <p class="text-sm text-surface-600">确认要取消此预约吗？</p>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">取消原因（选填）</label>
          <textarea v-model="cancelReason" rows="2" placeholder="临时有事…" class="input-field resize-none" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="cancelModal = false">返回</AppButton>
          <AppButton variant="danger" class="flex-1" :loading="cancelLoading" @click="confirmCancel">确认取消</AppButton>
        </div>
      </template>
    </AppModal>

    <!-- 续约弹窗 -->
    <AppModal :open="renewModal" title="续约" @close="renewModal = false">
      <div class="space-y-3">
        <p class="text-sm text-surface-600">请选择新的结束时间（需晚于当前结束时间）</p>
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1">新结束时间</label>
          <input v-model="renewTime" type="time" class="input-field" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="renewModal = false">返回</AppButton>
          <AppButton variant="accent" class="flex-1" :loading="renewLoading" :disabled="!renewTime" @click="confirmRenew">确认续约</AppButton>
        </div>
      </template>
    </AppModal>
  </UserLayout>
</template>
