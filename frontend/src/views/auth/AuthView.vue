<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register, errorMessage } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const registering = computed(() => route.path === '/register')
const form = reactive({ username: '', password: '', displayName: '' })
const busy = ref(false)
const error = ref('')
async function submit() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const payload = {
      ...form,
      username: form.username.trim(),
      displayName: form.displayName.trim(),
    }
    const result = registering.value ? await register(payload) : await login(payload)
    auth.accept(result)
    form.password = ''
    const redirect = route.query.redirect
    const safe =
      typeof redirect === 'string' &&
      redirect.startsWith('/') &&
      !redirect.startsWith('//') &&
      !redirect.includes('\\') &&
      !/^\/(login|register)([/?#]|$)/.test(redirect)
    await router.replace(safe ? redirect : '/dashboard')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <main class="auth-page">
    <section class="panel auth-panel">
      <h1>TeamFlow · {{ registering ? '注册' : '登录' }}</h1>
      <p>登录后查看工作台并维护个人资料。</p>
      <p v-if="route.query.expired" role="alert">登录已失效，请重新登录。</p>
      <p v-if="auth.error" role="alert">{{ auth.error }}；可重新登录后重试。</p>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <form @submit.prevent="submit">
        <label
          >用户名<input
            v-model="form.username"
            required
            maxlength="50"
            autocomplete="username"
            :disabled="busy"
        /></label>
        <label v-if="registering"
          >昵称<input
            v-model="form.displayName"
            required
            maxlength="50"
            autocomplete="nickname"
            :disabled="busy"
        /></label>
        <label
          >密码<input
            v-model="form.password"
            required
            type="password"
            :minlength="registering ? 6 : 1"
            maxlength="100"
            :autocomplete="registering ? 'new-password' : 'current-password'"
            :disabled="busy"
        /></label>
        <el-button type="primary" native-type="submit" :loading="busy">{{
          registering ? '注册并登录' : '登录'
        }}</el-button>
      </form>
      <div class="actions">
        <RouterLink
          :to="{
            path: registering ? '/login' : '/register',
            query: { redirect: route.query.redirect },
          }"
          >{{ registering ? '已有账号，去登录' : '没有账号，去注册' }}</RouterLink
        ><RouterLink to="/projects">浏览项目</RouterLink>
      </div>
    </section>
  </main>
</template>
