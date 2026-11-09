import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './assets/styles/index.css'
import './assets/styles/recruitment.css'
import './assets/styles/projects.css'

const app = createApp(App)

app.use(createPinia())
window.addEventListener('teamflow:unauthorized', () => {
  useAuthStore().clear()
  const current = router.currentRoute.value
  if (current.path !== '/login' && current.path !== '/register') {
    void router.replace({ path: '/login', query: { redirect: current.fullPath, expired: '1' } })
  }
})
app.use(router)

app.mount('#app')
