<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UserLayout from '@/layouts/UserLayout.vue'
import NotificationItem from '@/components/notifications/NotificationItem.vue'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppButton from '@/components/common/AppButton.vue'
import { notificationsApi } from '@/api/notifications'
import { useNotificationStore } from '@/stores/notifications'
import { useToast } from 'vue-toastification'
import type { Notification } from '@/types/api'

const notifStore = useNotificationStore()
const toast = useToast()
const notifications = ref<Notification[]>([])
const total = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const page = ref(1)
const pageSize = 20

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await notificationsApi.list(undefined, page.value, pageSize)
    notifications.value = res.data.data.items
    total.value = res.data.data.total
    totalPages.value = res.data.data.totalPages
  } catch {}
  loading.value = false
}

onMounted(fetchNotifications)

async function markRead(id: string) {
  await notificationsApi.markRead(id)
  const n = notifications.value.find(x => x.id === id)
  if (n) n.isRead = true
  notifStore.fetchUnreadCount()
}

async function remove(id: string) {
  try {
    await notificationsApi.remove(id)
    notifications.value = notifications.value.filter(x => x.id !== id)
    total.value--
    notifStore.fetchUnreadCount()
  } catch (e: any) {
    toast.error(e?.message || '删除失败')
  }
}

async function markAllRead() {
  try {
    await notificationsApi.markAllRead()
    notifications.value.forEach(n => (n.isRead = true))
    notifStore.fetchUnreadCount()
    toast.success('已全部标为已读')
  } catch (e: any) {
    toast.error(e?.message || '操作失败')
  }
}
</script>

<template>
  <UserLayout>
    <div class="max-w-2xl mx-auto animate-fade-in-up">
      <div class="flex items-center justify-between mb-4">
        <h1 class="page-heading">通知中心</h1>
        <AppButton variant="ghost" size="sm" @click="markAllRead">全部标为已读</AppButton>
      </div>

      <!-- 骨架加载 -->
      <div v-if="loading" class="space-y-1">
        <div v-for="n in 5" :key="n" class="skeleton h-16 rounded-lg" />
      </div>

      <!-- 空状态 -->
      <AppEmptyState v-else-if="notifications.length === 0" type="bell" title="暂无通知" />

      <!-- 通知列表 -->
      <div v-else class="card overflow-hidden divide-y divide-surface-100">
        <TransitionGroup name="page">
          <NotificationItem
            v-for="n in notifications"
            :key="n.id"
            :notification="n"
            @read="markRead"
            @remove="remove"
          />
        </TransitionGroup>
      </div>

      <AppPagination
        v-if="totalPages > 1"
        :page="page" :total-pages="totalPages" :total="total" :page-size="pageSize"
        @update:page="p => { page = p; fetchNotifications() }"
      />
    </div>
  </UserLayout>
</template>
