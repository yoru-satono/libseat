<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftIcon, CalendarIcon, ClockIcon, MapPinIcon } from '@heroicons/vue/24/outline'
import UserLayout from '@/layouts/UserLayout.vue'
import AppBadge from '@/components/common/AppBadge.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { reservationsApi } from '@/api/reservations'
import { useToast } from 'vue-toastification'
import type { Reservation } from '@/types/api'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const reservation = ref<Reservation | null>(null)
const loading = ref(true)
const cancelModal = ref(false)
const cancelReason = ref('')
const renewModal = ref(false)
const renewTime = ref('')
const renewLoading = ref(false)

onMounted(async () => {
  try {
    const res = await reservationsApi.get(route.params.id as string)
    reservation.value = res.data.data
  } catch (e: any) {
    toast.error(e?.message || '获取失败')
    router.back()
  } finally {
    loading.value = false
  }
})

async function doCancel() {
  if (!reservation.value) return
  try {
    await reservationsApi.cancel(reservation.value.id, cancelReason.value || undefined)
    toast.success('预约已取消')
    const res = await reservationsApi.get(reservation.value.id)
    reservation.value = res.data.data
    cancelModal.value = false
  } catch (e: any) {
    toast.error(e?.message || '取消失败')
  }
}

async function doRenew() {
  if (!reservation.value || !renewTime.value) return
  renewLoading.value = true
  try {
    const res = await reservationsApi.renew(reservation.value.id, renewTime.value)
    toast.success('续约成功！')
    router.push(`/reservations/${res.data.data.id}`)
    renewModal.value = false
  } catch (e: any) {
    toast.error(e?.message || '续约失败')
  } finally {
    renewLoading.value = false
  }
}

const AREA_LABEL: Record<string, string> = { QUIET: '安静区', DISCUSSION: '讨论区', COMPUTER: '电脑区' }
</script>

<template>
  <UserLayout>
    <div class="max-w-lg mx-auto">
      <!-- 返回按钮 -->
      <button @click="router.back()" class="inline-flex items-center gap-1.5 text-sm text-surface-500 hover:text-surface-700 mb-4 transition-colors">
        <ArrowLeftIcon class="w-4 h-4" /> 返回
      </button>

      <!-- 骨架加载 -->
      <div v-if="loading" class="skeleton h-64 rounded-2xl" />

      <!-- 详情卡片 -->
      <div v-else-if="reservation" class="card overflow-hidden animate-fade-in-up">
        <!-- 头部 -->
        <div class="bg-gradient-to-br from-primary-50 to-surface-50 p-5 border-b border-surface-100">
          <div class="flex items-start justify-between">
            <div>
              <h2 class="font-display text-lg font-bold text-surface-900">{{ reservation.seatNo }}</h2>
              <p class="text-sm text-surface-600">{{ reservation.libraryName }} · {{ reservation.floor }}F · {{ AREA_LABEL[reservation.area] }}</p>
            </div>
            <AppBadge :status="reservation.status" />
          </div>
        </div>

        <!-- 详情区 -->
        <div class="p-5 space-y-3.5">
          <div class="flex items-center gap-3 text-sm">
            <div class="w-8 h-8 rounded-lg bg-primary-50 flex items-center justify-center shrink-0">
              <CalendarIcon class="w-4 h-4 text-primary-500" />
            </div>
            <span class="text-surface-700 font-medium">{{ reservation.date }}</span>
          </div>
          <div class="flex items-center gap-3 text-sm">
            <div class="w-8 h-8 rounded-lg bg-accent-50 flex items-center justify-center shrink-0">
              <ClockIcon class="w-4 h-4 text-accent-500" />
            </div>
            <span class="text-surface-700 font-medium">{{ reservation.startTime }} – {{ reservation.endTime }}</span>
          </div>

          <!-- 签到时间 -->
          <div v-if="reservation.checkinAt" class="flex items-center gap-3 text-sm ml-11">
            <span class="text-surface-500">签到时间：</span>
            <span class="text-success-600 font-medium">{{ reservation.checkinAt }}</span>
          </div>

          <!-- 取消原因 -->
          <div v-if="reservation.cancelReason" class="bg-surface-50 rounded-lg px-3 py-2.5 text-sm text-surface-600 ml-11">
            取消原因：{{ reservation.cancelReason }}
          </div>

          <div class="text-xs text-surface-400 pt-2 border-t border-surface-100">
            预约号：{{ reservation.id }}
          </div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="reservation.status === 'ACTIVE'" class="px-5 pb-5 flex gap-2 border-t border-surface-100 pt-4">
          <AppButton variant="accent" class="flex-1" @click="renewModal = true">续约</AppButton>
          <AppButton variant="danger" class="flex-1" @click="cancelModal = true">取消预约</AppButton>
        </div>
      </div>
    </div>

    <!-- 取消弹窗 -->
    <AppModal :open="cancelModal" title="取消预约" @close="cancelModal = false">
      <div class="space-y-3">
        <p class="text-sm text-surface-600">确认取消本次预约？</p>
        <textarea v-model="cancelReason" rows="2" placeholder="取消原因（选填）" class="input-field resize-none" />
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="cancelModal = false">返回</AppButton>
          <AppButton variant="danger" class="flex-1" @click="doCancel">确认取消</AppButton>
        </div>
      </template>
    </AppModal>

    <!-- 续约弹窗 -->
    <AppModal :open="renewModal" title="续约" @close="renewModal = false">
      <div class="space-y-3">
        <p class="text-sm text-surface-600">请选择新的结束时间</p>
        <input v-model="renewTime" type="time" class="input-field" />
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="renewModal = false">返回</AppButton>
          <AppButton variant="accent" class="flex-1" :loading="renewLoading" :disabled="!renewTime" @click="doRenew">确认续约</AppButton>
        </div>
      </template>
    </AppModal>
  </UserLayout>
</template>
