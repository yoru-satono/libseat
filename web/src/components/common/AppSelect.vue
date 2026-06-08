<script setup lang="ts">
import { computed } from 'vue'
import { Listbox, ListboxButton, ListboxOptions, ListboxOption, TransitionRoot, TransitionChild } from '@headlessui/vue'
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/vue/24/outline'

interface Option { label: string; value: string | number | undefined }

const props = defineProps<{
  modelValue: string | number | undefined
  options: Option[]
  placeholder?: string
  error?: boolean
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [v: string | number | undefined] }>()

const selected = computed(() => props.options.find(o => o.value === props.modelValue))
</script>

<template>
  <Listbox
    :model-value="modelValue"
    :disabled="disabled"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="relative">
      <ListboxButton
        class="relative w-full rounded-input border bg-white py-2 pl-3 pr-8 text-left text-sm transition-all duration-200
               focus:outline-none focus:ring-2 focus:ring-offset-0
               disabled:bg-surface-100 disabled:text-surface-400 disabled:cursor-not-allowed"
        :class="error
          ? 'border-danger-300 focus:ring-danger-400/50 focus:border-danger-400'
          : 'border-surface-300 focus:ring-primary-400/50 focus:border-primary-400'"
      >
        <span :class="selected ? 'text-surface-900' : 'text-surface-400'">
          {{ selected?.label ?? placeholder ?? '请选择' }}
        </span>
        <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
          <ChevronUpDownIcon class="w-4 h-4 text-surface-400" />
        </span>
      </ListboxButton>

      <TransitionRoot
        leave="transition ease-in duration-100"
        leaveFrom="opacity-100 scale-100"
        leaveTo="opacity-0 scale-95"
      >
        <ListboxOptions
          class="absolute z-10 mt-1 w-full rounded-lg bg-white shadow-dropdown border border-surface-200 py-1 text-sm max-h-56 overflow-auto focus:outline-none origin-top"
        >
          <ListboxOption
            v-for="opt in options"
            :key="String(opt.value)"
            :value="opt.value"
            v-slot="{ active, selected: sel }"
          >
            <li
              class="relative cursor-pointer select-none py-2 pl-8 pr-4 transition-colors duration-100"
              :class="active ? 'bg-primary-50 text-primary-700' : 'text-surface-900'"
            >
              <span :class="sel ? 'font-medium' : 'font-normal'">{{ opt.label }}</span>
              <span v-if="sel" class="absolute inset-y-0 left-0 flex items-center pl-2 text-primary-600">
                <CheckIcon class="w-4 h-4" />
              </span>
            </li>
          </ListboxOption>
        </ListboxOptions>
      </TransitionRoot>
    </div>
  </Listbox>
</template>
