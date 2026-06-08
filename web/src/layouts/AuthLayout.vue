<template>
  <div class="min-h-screen flex items-center justify-center px-4 py-12 relative overflow-hidden bg-gradient-to-br from-primary-50 via-surface-50 to-accent-50">
    <!-- 装饰性背景元素 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
      <!-- 模糊渐变色块 -->
      <div class="absolute -top-48 -right-48 w-96 h-96 rounded-full bg-primary-200/30 blur-3xl" />
      <div class="absolute -bottom-32 -left-32 w-80 h-80 rounded-full bg-accent-200/25 blur-3xl" />
      <div class="absolute top-1/3 left-1/4 w-64 h-64 rounded-full bg-primary-300/15 blur-3xl" />
      <!-- CSS 几何图案叠加 -->
      <div class="absolute inset-0 opacity-[0.03]"
        style="background-image: repeating-linear-gradient(45deg, currentColor 0 1px, transparent 0 12px), repeating-linear-gradient(-45deg, currentColor 0 1px, transparent 0 12px); color: var(--color-primary-900);" />
    </div>

    <div class="w-full max-w-md relative z-10">
      <!-- Logo -->
      <div class="text-center mb-8 animate-fade-in-up">
        <div class="inline-flex items-center gap-2.5 text-primary-600">
          <svg class="w-9 h-9" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
          </svg>
          <span class="text-3xl font-bold tracking-tight font-display">LibSeat</span>
        </div>
        <p class="text-surface-500 text-sm mt-1.5">图书馆座位预约管理系统</p>
      </div>

      <!-- 卡片 -->
      <div class="bg-white rounded-2xl shadow-card p-8 border border-surface-100 animate-fade-in-up stagger-1">
        <slot />
      </div>

      <!-- 底部版权 -->
      <p class="text-center text-xs text-surface-400 mt-8">
        &copy; {{ new Date().getFullYear() }} LibSeat. All rights reserved.
      </p>
    </div>
  </div>
</template>
