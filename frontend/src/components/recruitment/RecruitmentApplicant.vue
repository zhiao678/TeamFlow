<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApplicationsStore } from '@/stores/applications'
import { useBrowseClock } from '@/composables/useRecruitmentRead'
import { latestApplication, applicationBlock } from '@/utils/application-eligibility'
import { applicationStatusLabels } from '@/types/application'
import { displayDate, type Recruitment } from '@/types/recruitment'
import WithdrawApplication from './WithdrawApplication.vue'
const props = defineProps<{ item: Recruitment; membersReady: boolean; memberIds: number[] }>()
const emit = defineEmits<{ refresh: [] }>()
const route = useRoute(),
  auth = useAuthStore(),
  applications = useApplicationsStore(),
  now = useBrowseClock()
const form = reactive({ introduction: '', skillsDescription: '', availableTime: '' })
watch(
  [() => auth.token, () => props.item.id],
  () => {
    Object.assign(form, { introduction: '', skillsDescription: '', availableTime: '' })
    if (auth.token) void applications.load()
  },
  { immediate: true },
)
const latest = computed(() => latestApplication(applications.items, props.item.id))
const block = computed(() =>
  auth.user
    ? applicationBlock(props.item, auth.user.id, props.memberIds, latest.value, now.value)
    : '正在确认登录身份，请先完成身份验证。',
)
async function submit() {
  if (!applications.ready || !props.membersReady || block.value || applications.busy) return
  if (await applications.submit(props.item.id, { ...form })) emit('refresh')
}
</script>
<template>
  <section class="recruit-applicant" aria-labelledby="applicant-heading">
    <h2 id="applicant-heading">加入这支队伍</h2>
    <p v-if="!auth.token">
      <RouterLink :to="{ path: '/login', query: { redirect: route.fullPath } }"
        >登录后申请</RouterLink
      >
    </p>
    <template v-else>
      <p v-if="applications.loading" role="status">正在查询你的申请状态…</p>
      <div v-else-if="applications.error" role="alert">
        <p>{{ applications.error }}</p>
        <button @click="applications.load">重试申请查询</button>
      </div>
      <template v-else-if="applications.ready">
        <div v-if="latest" class="application-summary">
          <p>
            最新申请：<strong>{{ applicationStatusLabels[latest.status] }}</strong> ·
            {{ displayDate(latest.createdAt) }}
          </p>
          <p v-if="latest.processedAt">处理时间：{{ displayDate(latest.processedAt) }}</p>
          <p v-if="latest.rejectionReason">拒绝原因：{{ latest.rejectionReason }}</p>
          <WithdrawApplication
            v-if="latest.status === 'PENDING'"
            :id="latest.id"
            @refresh="emit('refresh')"
          />
          <RouterLink :to="{ name: 'my-applications', query: { applicationId: latest.id } }"
            >查看全部申请记录</RouterLink
          >
        </div>
        <p v-if="!membersReady" role="status">
          成员信息尚未确认，请等待加载或使用上方成员区域的重试按钮。
        </p>
        <p v-else-if="block">{{ block }}</p>
        <form v-else class="recruit-application-form" @submit.prevent="submit">
          <p>以下信息均可选，用几句话让负责人了解你。</p>
          <label
            >自我介绍<textarea
              v-model="form.introduction"
              maxlength="1000"
              :disabled="applications.busy"
            /><small>{{ form.introduction.length }} / 1000</small></label
          >
          <label
            >技能说明<textarea
              v-model="form.skillsDescription"
              maxlength="1000"
              :disabled="applications.busy"
            /><small>{{ form.skillsDescription.length }} / 1000</small></label
          >
          <label
            >可投入时间<input
              v-model="form.availableTime"
              maxlength="255"
              placeholder="例如：每周三、周五晚间"
              :disabled="applications.busy"
            /><small>{{ form.availableTime.length }} / 255</small></label
          >
          <button class="primary" :disabled="applications.busy">
            {{ applications.busy ? '正在提交…' : '提交申请' }}
          </button>
        </form>
      </template>
    </template>
  </section>
</template>
