<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/api/users'
import { listTags, type TagOption } from '@/api/dictionaries'
const auth = useAuthStore()
const tags = ref<TagOption[]>([])
const selectedTags = ref<number[]>([])
const tagsDirty = ref(false)
const tagsLoading = ref(false)
const tagsError = ref('')
const unmatched = computed(() =>
  (auth.user?.skills ?? []).filter(
    (name) => tags.value.filter((tag) => tag.name === name).length !== 1,
  ),
)
function fillTags() {
  if (tagsDirty.value) return
  selectedTags.value = tags.value
    .filter((tag) => auth.user?.skills.includes(tag.name))
    .map((tag) => tag.id)
}
async function loadTags() {
  tagsLoading.value = true
  tagsError.value = ''
  try {
    tags.value = await listTags()
    fillTags()
  } catch (cause) {
    tagsError.value = errorMessage(cause)
  } finally {
    tagsLoading.value = false
  }
}
onMounted(loadTags)
watch([() => auth.user, tags], fillTags)
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
  if (busy.value || auth.loading || auth.error || !auth.user) return
  if (!form.displayName.trim()) {
    error.value = '昵称不能只包含空格'
    return
  }
  if (tagsDirty.value && selectedTags.value.length > 20) {
    error.value = '最多选择 20 个技能标签'
    return
  }
  busy.value = true
  error.value = ''
  success.value = false
  try {
    success.value = await auth.saveProfile({
      ...form,
      displayName: form.displayName.trim(),
      ...(tagsDirty.value && !tagsError.value && !unmatched.value.length
        ? { tagIds: selectedTags.value }
        : {}),
    })
    if (success.value) {
      tagsDirty.value = false
      fillTags()
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
      <p>维护公开资料与技能标签，让队友更了解你。</p>
    </div>
  </div>
  <section class="panel profile-panel" :aria-busy="auth.loading">
    <p v-if="auth.loading" role="status">正在加载最新资料…</p>
    <p v-if="auth.error" role="alert">
      {{ auth.error }}
      <el-button :loading="auth.loading" @click="auth.restore(true)">重新加载资料</el-button>
    </p>
    <template v-if="auth.user">
      <div class="profile-summary">
        <span class="avatar">{{ auth.user.displayName.slice(0, 1) || '用' }}</span>
        <div>
          <h2>{{ auth.user.displayName }}</h2>
          <p>用户名：{{ auth.user.username }}（不可修改）</p>
        </div>
      </div>
      <form @submit.prevent="save">
        <fieldset :disabled="busy || auth.loading || !!auth.error">
          <label
            >昵称<input
              v-model="form.displayName"
              required
              maxlength="50"
              :disabled="busy" /></label
          ><label
            >头像 URL<input
              v-model="form.avatarUrl"
              type="url"
              maxlength="255"
              :disabled="busy" /></label
          ><label>院系<input v-model="form.department" maxlength="100" :disabled="busy" /></label
          ><label>年级<input v-model="form.grade" maxlength="30" :disabled="busy" /></label
          ><label
            >简介<textarea v-model="form.bio" maxlength="500" :disabled="busy"></textarea>
          </label>
        </fieldset>
        <div>
          <h2>技能标签</h2>
          <div class="actions">
            <el-tag v-for="skill in auth.user.skills" :key="skill">{{ skill }}</el-tag>
            <p v-if="!auth.user.skills.length">暂无技能标签</p>
          </div>
          <p v-if="tagsLoading" role="status">正在加载技能选项…</p>
          <p v-if="tagsError" role="alert">
            {{ tagsError }} <el-button @click="loadTags">重试技能选项</el-button>
          </p>
          <p v-if="unmatched.length" role="alert">
            有技能无法唯一对应当前选项：{{ unmatched.join('、') }}。为保留原技能，暂不允许修改技能。
          </p>
          <fieldset
            :disabled="busy || auth.loading || tagsLoading || !!tagsError || !!unmatched.length"
          >
            <label v-for="tag in tags" :key="tag.id" class="recruit-check"
              ><input
                v-model="selectedTags"
                type="checkbox"
                :value="tag.id"
                @change="tagsDirty = true"
              />{{ tag.name }}</label
            >
          </fieldset>
          <p v-if="!tagsLoading && !tagsError && !tags.length">暂无可选技能。</p>
          <p>最多选择 20 项；取消全部选择并保存可清空技能。</p>
        </div>
        <p v-if="error" role="alert" class="form-error">{{ error }}</p>
        <p v-if="success" role="status">资料已保存。</p>
        <el-button
          native-type="submit"
          type="primary"
          :loading="busy"
          :disabled="auth.loading || !!auth.error"
          >保存资料</el-button
        >
      </form>
    </template>
  </section>
</template>
