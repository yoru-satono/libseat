<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { PlusIcon, PencilIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AppCard from '@/components/common/AppCard.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import { adminLibrariesApi } from '@/api/admin/libraries'
import { useToast } from 'vue-toastification'
import type { Library } from '@/types/api'

const toast = useToast()
const libraries = ref<Library[]>([])
const loading = ref(false)
const modal = ref(false)
const editTarget = ref<Library | null>(null)
const form = ref({ name: '', address: '' })

async function fetchLibraries() {
  loading.value = true
  try {
    const res = await adminLibrariesApi.list()
    libraries.value = res.data.data
  } catch {}
  loading.value = false
}

onMounted(fetchLibraries)

function openCreate() { editTarget.value = null; form.value = { name: '', address: '' }; modal.value = true }
function openEdit(lib: Library) { editTarget.value = lib; form.value = { name: lib.name, address: lib.address || '' }; modal.value = true }

async function save() {
  try {
    if (editTarget.value) {
      await adminLibrariesApi.update(editTarget.value.id, form.value)
      toast.success('已更新')
    } else {
      await adminLibrariesApi.create(form.value)
      toast.success('已创建')
    }
    modal.value = false
    fetchLibraries()
  } catch (e: any) {
    toast.error(e?.message || '操作失败')
  }
}

async function remove(id: string, name: string) {
  if (!confirm(`确认删除图书馆「${name}」？有关联座位时不可删除。`)) return
  try {
    await adminLibrariesApi.remove(id)
    toast.success('已删除')
    fetchLibraries()
  } catch (e: any) {
    toast.error(e?.message || '删除失败')
  }
}
</script>

<template>

    <div class="flex justify-end mb-4">
      <AppButton variant="accent" size="sm" @click="openCreate">
        <PlusIcon class="w-4 h-4" /> 新增图书馆
      </AppButton>
    </div>

    <AppEmptyState v-if="!loading && libraries.length === 0" title="暂无图书馆" />

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <AppCard
        v-for="(lib, i) in libraries"
        :key="lib.id"
        hover
        :style="{ animationDelay: `${0.05 * i}s` }"
        class="animate-fade-in-up"
      >
        <div class="flex items-start justify-between mb-2">
          <h3 class="font-semibold text-surface-900 font-display">{{ lib.name }}</h3>
          <div class="flex gap-0.5">
            <button
              aria-label="编辑"
              @click="openEdit(lib)"
              class="p-1.5 text-surface-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
            ><PencilIcon class="w-4 h-4" /></button>
            <button
              aria-label="删除"
              @click="remove(lib.id, lib.name)"
              class="p-1.5 text-surface-400 hover:text-danger-500 hover:bg-danger-50 rounded-lg transition-colors"
            ><TrashIcon class="w-4 h-4" /></button>
          </div>
        </div>
        <p v-if="lib.address" class="text-sm text-surface-500">{{ lib.address }}</p>
        <p class="text-xs text-surface-400 mt-2">创建于 {{ lib.createdAt.slice(0,10) }}</p>
      </AppCard>
    </div>

    <!-- 新建/编辑弹窗 -->
    <AppModal :open="modal" :title="editTarget ? '编辑图书馆' : '新增图书馆'" @close="modal = false">
      <div class="space-y-4">
        <div>
          <label for="lib-name" class="block text-sm font-medium text-surface-700 mb-1.5">名称 <span class="text-danger-500">*</span></label>
          <input id="lib-name" v-model="form.name" type="text" required class="input-field" />
        </div>
        <div>
          <label for="lib-address" class="block text-sm font-medium text-surface-700 mb-1.5">地址</label>
          <input id="lib-address" v-model="form.address" type="text" class="input-field" />
        </div>
      </div>
      <template #footer>
        <div class="flex gap-3">
          <AppButton variant="secondary" class="flex-1" @click="modal = false">取消</AppButton>
          <AppButton variant="primary" class="flex-1" :disabled="!form.name" @click="save">保存</AppButton>
        </div>
      </template>
    </AppModal>
</template>
