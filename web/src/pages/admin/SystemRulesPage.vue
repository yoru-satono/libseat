<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminSystemApi } from '@/api/admin/system'
import { useToast } from 'vue-toastification'

const toast = useToast()
const loading = ref(true)
const saving = ref(false)
const rules = reactive({
  openTimeStart: '', openTimeEnd: '',
  advanceDaysMax: 7, singleMinMinutes: 30, singleMaxHours: 4, dailyMaxHours: 8,
  checkinEarlyMinutes: 10, checkinLateMinutes: 15,
  noShowThreshold: 3, suspendDays: 7,
})

onMounted(async () => {
  try {
    const res = await adminSystemApi.getRules()
    const d = res.data.data
    Object.assign(rules, {
      openTimeStart: d.openTimeStart,
      openTimeEnd: d.openTimeEnd,
      advanceDaysMax: d.advanceDaysMax,
      singleMinMinutes: d.singleMinMinutes,
      singleMaxHours: d.singleMaxHours,
      dailyMaxHours: d.dailyMaxHours,
      checkinEarlyMinutes: d.checkinEarlyMinutes,
      checkinLateMinutes: d.checkinLateMinutes,
      noShowThreshold: d.noShowThreshold,
      suspendDays: d.suspendDays,
    })
  } catch {}
  loading.value = false
})

async function save() {
  saving.value = true
  try {
    await adminSystemApi.updateRules(rules)
    toast.success('规则已保存')
  } catch (e: any) {
    toast.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const fields = [
  { key: 'openTimeStart', label: '开馆时间', type: 'time', unit: '' },
  { key: 'openTimeEnd', label: '闭馆时间', type: 'time', unit: '' },
  { key: 'advanceDaysMax', label: '最大提前预约天数', type: 'number', unit: '天' },
  { key: 'singleMinMinutes', label: '单次最短时长', type: 'number', unit: '分钟' },
  { key: 'singleMaxHours', label: '单次最长时长', type: 'number', unit: '小时' },
  { key: 'dailyMaxHours', label: '每日最长累计时长', type: 'number', unit: '小时' },
  { key: 'checkinEarlyMinutes', label: '签到可提前', type: 'number', unit: '分钟' },
  { key: 'checkinLateMinutes', label: '签到可延迟', type: 'number', unit: '分钟' },
  { key: 'noShowThreshold', label: '爽约阈值', type: 'number', unit: '次' },
  { key: 'suspendDays', label: '爽约暂停天数', type: 'number', unit: '天' },
] as const
</script>

<template>

    <!-- 骨架加载 -->
    <div v-if="loading" class="skeleton h-96 rounded-2xl max-w-lg" />

    <div v-else class="max-w-lg animate-fade-in-up">
      <AppCard padding="lg">
        <h2 class="font-display text-lg font-semibold text-surface-900 mb-5">全局规则配置</h2>

        <div class="space-y-4">
          <div v-for="f in fields" :key="f.key" class="flex items-center gap-3">
            <label class="w-48 text-sm text-surface-700 shrink-0">{{ f.label }}</label>
            <div class="flex items-center gap-2 flex-1">
              <input
                v-model="(rules as any)[f.key]"
                :type="f.type"
                :min="f.type === 'number' ? 0 : undefined"
                class="input-field py-2 flex-1"
              />
              <span v-if="f.unit" class="text-sm text-surface-400 shrink-0 w-10">{{ f.unit }}</span>
            </div>
          </div>
        </div>
      </AppCard>

      <div class="mt-4 flex justify-end">
        <AppButton variant="primary" size="lg" :loading="saving" @click="save">保存规则</AppButton>
      </div>
    </div>
</template>
