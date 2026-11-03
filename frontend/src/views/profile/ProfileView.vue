<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, errorMessage } from '@/api/users'
const auth = useAuthStore()
onMounted(() => auth.restore(true))
const form = reactive({ displayName: '', avatarUrl: '', department: '', grade: '', bio: '' })
const busy = ref(false)
const error = ref('')
const success = ref(false)
watch(
  () => auth.user,
  (user) => {
    if (user)
      Object.assign(form, {
        displayName: user.displayName,
        avatarUrl: user.avatarUrl || '',
        department: user.department || '',
        grade: user.grade || '',
        bio: user.bio || '',
      })
  },
  { immediate: true },
)
async function save() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  success.value = false
  const id = auth.user?.id
  try {
    const user = await updateProfile({ ...form, displayName: form.displayName.trim() })
    if (auth.user?.id === id) {
      auth.user = user
      success.value = true
    }
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>个人资料</h1>
      <p>维护你的公开资料；技能修改接口暂未开放。</p>
    </div>
  </div>
  <section v-if="auth.user" class="panel profile-panel">
    <p>用户名：{{ auth.user.username }}（不可修改）</p>
    <p v-if="auth.error" role="alert">
      {{ auth.error }} <button @click="auth.restore(true)">重新加载资料</button>
    </p>
    <form @submit.prevent="save">
      <label
        >昵称<input v-model="form.displayName" required maxlength="50" :disabled="busy" /></label
      ><label
        >头像 URL<input
          v-model="form.avatarUrl"
          type="url"
          maxlength="255"
          :disabled="busy" /></label
      ><label>院系<input v-model="form.department" maxlength="100" :disabled="busy" /></label
      ><label>年级<input v-model="form.grade" maxlength="30" :disabled="busy" /></label
      ><label>简介<textarea v-model="form.bio" maxlength="500" :disabled="busy"></textarea></label>
      <p>技能：{{ auth.user.skills?.join('、') || '暂无技能标签' }}</p>
      <p v-if="error" role="alert" class="form-error">{{ error }}</p>
      <p v-if="success" role="status">资料已保存。</p>
      <el-button native-type="submit" type="primary" :loading="busy">保存资料</el-button>
    </form>
  </section>
</template>
