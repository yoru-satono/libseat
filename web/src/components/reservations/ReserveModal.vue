<script setup lang="ts">
import { ref } from 'vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { reservationsApi } from '@/api/reservations'
import { useToast } from 'vue-toastification'
import type { Seat } from '@/types/api'

const props = defineProps<{ seat: Seat | null }>()
const emit = defineEmits<{ close: []; success: [] }>()
const toast = useToast()

const date = ref('')
const startTime = ref('09:00')
const endTime = ref('11:00')
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!props.seat || !date.value) return
  error.value = ''
  loading.value = true
  try {
    await reservationsApi.create({
      seatId: props.seat.id,
      date: date.value,
      startTime: startTime.value,
      endTime: endTime.value,
    })
    toast.success('预约成功！')
    emit('success')
    emit('close')
  } catch (e: any) {
    error.value = e?.message || '预约失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AppModal :open="!!seat" title="预约座位" @close="emit('close')">
    <div v-if="seat">
      <!-- 座位信息预览 -->
      <div class="bg-surface-50 rounded-card border border-surface-100 p-4 mb-4">
        <p class="font-semibold text-surface-900 text-sm">{{ seat.seatNo }}</p>
        <p class="text-surface-500 text-xs mt-0.5">{{ seat.libraryName }} · {{ seat.floor }}F</p>
      </div>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-surface-700 mb-1.5">预约日期 <span class="text-danger-500">*</span></label>
          <input
            v-model="date"
            type="date"
            :min="new Date().toISOString().slice(0,10)"
            class="input-field"
          />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-surface-700 mb-1.5">开始时间</label>
            <input v-model="startTime" type="time" class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-surface-700 mb-1.5">结束时间</label>
            <input v-model="endTime" type="time" class="input-field" />
          </div>
        </div>
        <p v-if="error" class="text-sm text-danger-500 flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
          </svg>
          {{ error }}
        </p>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <AppButton variant="secondary" class="flex-1" @click="emit('close')">取消</AppButton>
        <AppButton variant="primary" class="flex-1" :loading="loading" :disabled="!date" @click="submit">确认预约</AppButton>
      </div>
    </template>
  </AppModal>
</template>
