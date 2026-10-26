import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { notifications } from '@/mocks/data'

export const useNotificationsStore = defineStore('notifications', () => {
  const items = ref(notifications.map((item) => ({ ...item })))
  const unreadCount = computed(() => items.value.filter((item) => !item.read).length)
  function markRead(id: number) {
    const item = items.value.find((item) => item.id === id)
    if (item) item.read = true
  }
  function markAllRead() {
    items.value.forEach((item) => {
      item.read = true
    })
  }
  return { items, unreadCount, markRead, markAllRead }
})
