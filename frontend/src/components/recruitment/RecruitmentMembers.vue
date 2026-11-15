<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { listRecruitmentMembers, membersError } from '@/api/members'
import { displayDate } from '@/types/recruitment'
import { memberRoleLabels } from '@/types/application'
import type { TeamMember } from '@/types/application'

const props = defineProps<{ recruitmentId: number }>()

const members = ref<TeamMember[]>([])
const loading = ref(true)
const error = ref('')
let controller: AbortController | null = null

async function reload() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const data = await listRecruitmentMembers(props.recruitmentId, current.signal)
    if (!current.signal.aborted) members.value = data
  } catch (cause) {
    if (!current.signal.aborted) error.value = membersError(cause)
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}

onMounted(reload)
watch(() => props.recruitmentId, reload)
onBeforeUnmount(() => controller?.abort())

defineExpose({ reload })
</script>

<template>
  <section class="recruit-members" aria-live="polite">
    <header class="recruit-members-head">
      <h2>队伍成员</h2>
      <span v-if="!loading && !error" class="recruit-members-count">{{ members.length }} 人</span>
    </header>
    <p v-if="loading" role="status">正在加载成员…</p>
    <p v-else-if="error" class="form-error" role="alert">
      {{ error }}
      <button type="button" @click="reload">重试</button>
    </p>
    <p v-else-if="!members.length" class="hint">还没有正式成员。</p>
    <ul v-else class="recruit-member-list">
      <li v-for="member in members" :key="member.id" class="recruit-member">
        <img
          v-if="member.avatarUrl"
          class="recruit-avatar"
          :src="member.avatarUrl"
          :alt="`${member.displayName}的头像`"
        />
        <span v-else class="recruit-avatar recruit-avatar-text" aria-hidden="true">{{
          member.displayName.slice(0, 1) || '用'
        }}</span>
        <span class="recruit-member-name">{{ member.displayName }}</span>
        <span class="recruit-member-role">{{ memberRoleLabels[member.role] }}</span>
        <span class="recruit-member-date">{{ displayDate(member.joinedAt) }} 加入</span>
      </li>
    </ul>
  </section>
</template>
