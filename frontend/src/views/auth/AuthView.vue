<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register, errorMessage } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const registering = computed(() => route.path === '/register')
const form = reactive({ username: '', password: '', displayName: '', confirmPassword: '' })
const busy = ref(false)
const error = ref('')
watch(registering, () => {
  form.password = ''
  form.confirmPassword = ''
  error.value = ''
})
async function submit() {
  if (busy.value) return
  if (!form.username.trim() || (registering.value && !form.displayName.trim())) {
    error.value = '用户名和昵称不能只包含空格'
    return
  }
  if (registering.value && form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const payload = {
      password: form.password,
      username: form.username.trim(),
      displayName: form.displayName.trim(),
    }
    const result = registering.value ? await register(payload) : await login(payload)
    auth.accept(result)
    form.password = ''
    form.confirmPassword = ''
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
      <p class="eyebrow">TEAMFLOW · 大学生组队招募平台</p>
      <h1>{{ registering ? '创建你的账号' : '欢迎回来' }}</h1>
      <p>{{ registering ? '完善个人资料，让合适的队友找到你。' : '登录后继续你的团队协作。' }}</p>
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
        <label v-if="registering"
          >确认密码<input
            v-model="form.confirmPassword"
            required
            type="password"
            minlength="6"
            maxlength="100"
            autocomplete="new-password"
            :disabled="busy"
        /></label>
        <p v-if="registering">密码长度为 6–100 个字符。</p>
        <el-button type="primary" native-type="submit" :loading="busy">{{
          registering ? '注册并登录' : '登录'
        }}</el-button>
      </form>
      <div v-if="!busy" class="actions">
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
