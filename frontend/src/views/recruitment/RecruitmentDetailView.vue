<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import FavoriteButton from '@/components/recruitment/FavoriteButton.vue'
import RecruitmentApplicant from '@/components/recruitment/RecruitmentApplicant.vue'
import RecruitmentMembers from '@/components/recruitment/RecruitmentMembers.vue'
import { useFavoritesStore } from '@/stores/favorites'
import { useApplicationsStore } from '@/stores/applications'
import { getRecruitment } from '@/api/recruitment-query'
import { useRecruitmentRead, useBrowseClock } from '@/composables/useRecruitmentRead'
import MemberExitPanel from '@/components/team/MemberExitPanel.vue'
import RecruitmentConnections from '@/components/team/RecruitmentConnections.vue'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useAuthStore } from '@/stores/auth'
import { displayStatus, displayDate } from '@/types/recruitment'
const route = useRoute()
const auth = useAuthStore()
const sync = useTeamSyncStore()
const favorites = useFavoritesStore(),
  applications = useApplicationsStore()
const members = ref<InstanceType<typeof RecruitmentMembers> | null>(null)
const id = computed(() => String(route.params.id))
const valid = computed(() => /^[1-9]\d*$/.test(id.value) && Number.isSafeInteger(Number(id.value)))
const {
  data: item,
  error,
  loading,
  retry,
} = useRecruitmentRead(
  () => id.value,
  async (value, signal) => {
    if (!valid.value) return null
    const ingest = favorites.reader()
    const row = await getRecruitment(Number(value), signal)
    if (!signal.aborted) ingest([row])
    return row
  },
)
watch(() => sync.revisions[Number(id.value)], retry)
function refreshTeam() {
  if (!valid.value) return
  if (!auth.token || !sync.invalidateTeam(Number(id.value), sync.sessionRevision)) retry()
}
const now = useBrowseClock()
const owned = computed(() => !!item.value && auth.user?.id === item.value.ownerId)
</script>
<template>
  <main class="recruit">
    <RouterLink :to="{ name: 'recruitment-hall', query: route.query }">← 返回招募大厅</RouterLink>
    <button v-if="valid" :disabled="loading" @click="refreshTeam">刷新详情与成员</button>
    <p v-if="applications.busy" role="status">正在处理申请，请稍候…</p>
    <p v-if="auth.token && applications.notice" role="status">{{ applications.notice }}</p>
    <p v-if="auth.token && applications.actionError" class="form-error" role="alert">
      {{ applications.actionError }}
    </p>
    <p v-if="!valid" role="alert">招募编号无效，请返回大厅重新选择。</p>
    <p v-else-if="loading" role="status">正在加载招募详情…</p>
    <section v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button @click="retry">重试</button>
    </section>
    <article v-else-if="item" class="panel recruit-card">
      <div class="recruit-meta">
        <span>{{ item.categoryName }}</span
        ><strong>{{ displayStatus(item, now) }}</strong>
      </div>
      <h1>{{ item.title }}</h1>
      <p>负责人：{{ item.ownerDisplayName }}</p>
      <p v-if="owned" class="recruit-actions">
        <RouterLink
          class="recruit-button"
          :to="{ name: 'recruitment-edit', params: { id: item.id } }"
          >编辑这条招募</RouterLink
        >
        <RouterLink class="recruit-button" :to="{ name: 'my-recruitments' }">我的招募</RouterLink>
      </p>
      <dl class="recruit-facts">
        <div>
          <dt>组队目的／活动</dt>
          <dd>{{ item.purpose }}</dd>
        </div>
        <div>
          <dt>课程 · 教师</dt>
          <dd>{{ item.courseName || '未填写课程' }} · {{ item.teacherName || '未填写教师' }}</dd>
        </div>
        <div>
          <dt>队伍人数</dt>
          <dd>
            {{ item.currentMembers }} / {{ item.maxMembers }} 人（含负责人），剩余
            {{ item.remainingSlots }} 席
          </dd>
        </div>
        <div>
          <dt>截止时间（北京时间）</dt>
          <dd>{{ displayDate(item.deadlineAt) }}</dd>
        </div>
      </dl>
      <h2>详细说明</h2>
      <p>{{ item.description || '暂无补充说明' }}</p>
      <h2>招募要求</h2>
      <p>{{ item.requirements || '未填写额外要求' }}</p>
      <h2>技能标签</h2>
      <div class="recruit-tags">
        <span v-for="tag in item.tags" :key="tag">{{ tag }}</span
        ><small v-if="!item.tags.length">未指定技能</small>
      </div>
      <p v-if="item.status === 'CLOSED' && item.closeReason">关闭原因：{{ item.closeReason }}</p>
      <p v-if="displayStatus(item, now) !== '招募中'">
        当前为{{ displayStatus(item, now) }}，不能提交申请。
      </p>
      <FavoriteButton :item="item" />
      <RecruitmentConnections :item="item" />
      <RecruitmentMembers ref="members" :recruitment-id="item.id" />
      <RecruitmentApplicant
        :item="item"
        :members-ready="!!members && !members.loading && !members.error"
        :member-ids="members?.members.map((member) => member.id) ?? []"
        @refresh="retry"
      />
    </article>
    <MemberExitPanel
      v-if="valid && auth.token"
      :recruitment-id="Number(id)"
      :owner-id="item?.ownerId"
      :members="members?.members ?? []"
      :ready="!!item && !loading && !error && !!members && !members.loading && !members.error"
    />
  </main>
</template>
