import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { watch } from 'vue'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useNotificationsStore } from './stores/notifications'
import './assets/styles/index.css'
import './assets/styles/recruitment.css'
import './assets/styles/projects.css'

const app = createApp(App)

app.use(createPinia())
const auth = useAuthStore()
const notifications = useNotificationsStore()
watch(
  () => [auth.user?.id, auth.token],
  () => {
    if (auth.user && auth.token) void notifications.refreshUnreadCount()
  },
  { immediate: true },
)
window.addEventListener('teamflow:unauthorized', () => {
  useAuthStore().clear()
  const current = router.currentRoute.value
  if (current.path !== '/login' && current.path !== '/register') {
    void router.replace({ path: '/login', query: { redirect: current.fullPath, expired: '1' } })
  }
})
app.use(router)

app.mount('#app')
