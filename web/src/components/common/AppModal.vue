<script setup lang="ts">
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'

const props = defineProps<{
  open: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg'
  backdropClose?: boolean
}>()
const emit = defineEmits<{ close: [] }>()

function handleClose() {
  if (props.backdropClose !== false) {
    emit('close')
  }
}
</script>

<template>
  <TransitionRoot appear :show="open" as="template">
    <Dialog @close="handleClose" class="relative z-50">
      <!-- 背景遮罩 -->
      <TransitionChild
        as="template"
        enter="ease-out duration-250" enter-from="opacity-0" enter-to="opacity-100"
        leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black/45 backdrop-blur-[2px]" />
      </TransitionChild>

      <!-- 面板容器 -->
      <div class="fixed inset-0 flex items-end sm:items-center justify-center p-0 sm:p-4">
        <TransitionChild
          as="template"
          enter="ease-out duration-300" enter-from="opacity-0 translate-y-6 sm:translate-y-0 sm:scale-95"
          enter-to="opacity-100 translate-y-0 sm:scale-100"
          leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100"
          leave-to="opacity-0 translate-y-6 sm:translate-y-0 sm:scale-95"
        >
          <DialogPanel
            class="w-full bg-white rounded-t-2xl sm:rounded-2xl shadow-xl max-h-[90dvh] flex flex-col overflow-hidden"
            :class="{
              'sm:max-w-sm': size === 'sm',
              'sm:max-w-md': !size || size === 'md',
              'sm:max-w-2xl': size === 'lg',
            }"
          >
            <!-- 标题栏 -->
            <div
              v-if="title || $slots.title"
              class="flex items-center justify-between px-6 py-4 border-b border-surface-100 shrink-0"
            >
              <DialogTitle class="text-base font-semibold text-surface-900 font-display">
                <slot name="title">{{ title }}</slot>
              </DialogTitle>
              <button
                @click="emit('close')"
                class="p-1.5 rounded-lg text-surface-400 hover:text-surface-600 hover:bg-surface-100 transition-colors"
              >
                <XMarkIcon class="w-5 h-5" />
              </button>
            </div>

            <!-- 内容区 -->
            <div class="flex-1 overflow-y-auto px-6 py-4">
              <slot />
            </div>

            <!-- 底部操作区 -->
            <div v-if="$slots.footer" class="px-6 py-4 border-t border-surface-100 shrink-0">
              <slot name="footer" />
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>
