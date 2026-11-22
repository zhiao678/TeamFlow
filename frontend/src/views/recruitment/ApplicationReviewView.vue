<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import RecruitmentMembers from '@/components/recruitment/RecruitmentMembers.vue'
import { browseError, getRecruitment } from '@/api/recruitment-query'
import {
  acceptApplication,
  applicationStatus,
  listRecruitmentApplications,
  rejectApplication,
  reviewMessage,
} from '@/api/applications'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import { displayDate, displayStatus, timestamp } from '@/types/recruitment'
import { applicationStatusLabels, canReview } from '@/types/application'
import type { Application } from '@/types/application'
import type { Recruitment } from '@/types/recruitment'

const route = useRoute()
const auth = useAuthStore()
const notifications = useNotificationsStore()

const rawId = computed(() => String(route.params.id ?? ''))
const id = computed(() => Number(rawId.value))
const validId = computed(() => /^[1-9]\d*$/.test(rawId.value) && Number.isSafeInteger(id.value))

const recruitment = ref<Recruitment | null>(null)
const applications = ref<Application[]>([])
const loading = ref(true)
const error = ref('')
const listError = ref('')
const failure = ref('')
const notice = ref('')
const staleAfterSuccess = ref(false)
const busy = ref<{ id: number; kind: 'accept' | 'reject' } | null>(null)
const confirming = ref<{ id: number; kind: 'accept' | 'reject' } | null>(null)
const reason = ref('')
const membersRef = ref<InstanceType<typeof RecruitmentMembers> | null>(null)

const isOwner = computed(() => !!recruitment.value && auth.user?.id === recruitment.value.ownerId)
const pendingCount = computed(
  () => applications.value.filter((item) => item.status === 'PENDING').length,
)
const deadlinePassed = computed(() => {
  const deadline = recruitment.value?.deadlineAt
  return !!deadline && timestamp(deadline) <= Date.now()
})

async function loadRecruitment() {
  recruitment.value = await getRecruitment(id.value)
}

async function loadApplications() {
  applications.value = await listRecruitmentApplications(id.value)
}

async function loadAll() {
  if (!validId.value) {
    loading.value = false
    error.value = '招募编号无效，请从“我的招募”重新进入。'
    return
  }
  loading.value = true
  error.value = ''
  listError.value = ''
  try {
    await loadRecruitment()
  } catch (cause) {
    error.value = browseError(cause)
    loading.value = false
    return
  }
  try {
    await loadApplications()
  } catch (cause) {
    listError.value = reviewMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await auth.restore()
  await loadAll()
})

/** 操作成功后统一刷新：招募人数、申请状态、成员列表。 */
async function refreshAfterAction() {
  try {
    await loadRecruitment()
    await loadApplications()
    await membersRef.value?.reload()
    staleAfterSuccess.value = false
  } catch {
    staleAfterSuccess.value = true
  }
}

function ask(app: Application, kind: 'accept' | 'reject') {
  if (busy.value) return
  failure.value = ''
  notice.value = ''
  confirming.value = { id: app.id, kind }
  reason.value = ''
}

function cancelAsk() {
  confirming.value = null
  reason.value = ''
}

async function run(app: Application, kind: 'accept' | 'reject') {
  if (busy.value || !canReview(app.status, recruitment.value)) return
  busy.value = { id: app.id, kind }
  failure.value = ''
  notice.value = ''
  staleAfterSuccess.value = false
  try {
    const updated =
      kind === 'accept'
        ? await acceptApplication(app.id)
        : await rejectApplication(app.id, reason.value.trim())
    applications.value = applications.value.map((item) => (item.id === updated.id ? updated : item))
    notice.value =
      kind === 'accept'
        ? `已接受 ${app.applicantDisplayName} 的申请，名额已更新。`
        : `已拒绝 ${app.applicantDisplayName} 的申请。`
    confirming.value = null
    reason.value = ''
    void notifications.refreshUnreadCount()
    await refreshAfterAction()
  } catch (cause) {
    if (applicationStatus(cause) === 409) {
      failure.value = '名额或状态已被其他人改变，已重新加载最新数据，请确认后再操作。'
      await refreshAfterAction()
    } else {
      failure.value = reviewMessage(cause)
    }
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <section class="recruit">
    <p>
      <RouterLink :to="{ name: 'my-recruitments' }">← 返回我的招募</RouterLink>
      <template v-if="validId">
        ·
        <RouterLink :to="{ name: 'team-management', params: { id } }">队伍管理</RouterLink>
      </template>
      <template v-if="validId">
        ·
        <RouterLink :to="{ name: 'recruitment-detail', params: { id } }">查看招募详情</RouterLink>
      </template>
    </p>

    <header>
      <p class="eyebrow">招募管理</p>
      <h1>申请管理</h1>
      <p v-if="recruitment">
        《{{ recruitment.title }}》 · {{ displayStatus(recruitment) }} · 当前
        {{ recruitment.currentMembers }} / {{ recruitment.maxMembers }} 人，剩余
        {{ recruitment.remainingSlots }} 席
      </p>
    </header>

    <p v-if="loading" role="status">正在加载申请与成员…</p>
    <section v-else-if="error" class="panel recruit-card" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="loadAll">重试</button>
    </section>

    <section v-else-if="!isOwner" class="panel recruit-card" role="alert">
      <h2>无权查看申请</h2>
      <p>只有招募负责人可以查看和处理申请。</p>
    </section>

    <template v-else>
      <p v-if="deadlinePassed && pendingCount" class="notice" role="note">
        申请已截止，但截止前提交的 {{ pendingCount }} 条待处理申请仍可继续审核。
      </p>

      <p v-if="staleAfterSuccess" class="notice" role="alert">
        操作已成功，但刷新最新数据失败，页面上的名额可能不是最新的。
        <button type="button" @click="refreshAfterAction">重新刷新</button>
      </p>
      <p v-if="failure" class="form-error" role="alert">{{ failure }}</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>

      <RecruitmentMembers ref="membersRef" :recruitment-id="id" />

      <section class="panel recruit-card">
        <header class="recruit-members-head">
          <h2>收到的申请</h2>
          <span class="recruit-members-count">{{ applications.length }} 条</span>
        </header>

        <p v-if="listError" class="form-error" role="alert">
          {{ listError }}
          <button type="button" @click="refreshAfterAction">重试</button>
        </p>

        <template v-else>
          <div v-if="applications.length" class="recruit-app-list">
            <article v-for="app in applications" :key="app.id" class="recruit-app">
              <div class="recruit-meta">
                <strong>{{ app.applicantDisplayName }}</strong>
                <span class="recruit-status">{{ applicationStatusLabels[app.status] }}</span>
              </div>
              <dl class="recruit-facts">
                <div>
                  <dt>自我介绍</dt>
                  <dd>{{ app.introduction || '未填写' }}</dd>
                </div>
                <div>
                  <dt>技能说明</dt>
                  <dd>{{ app.skillsDescription || '未填写' }}</dd>
                </div>
                <div>
                  <dt>可投入时间</dt>
                  <dd>{{ app.availableTime || '未填写' }}</dd>
                </div>
                <div>
                  <dt>申请时间</dt>
                  <dd>{{ displayDate(app.createdAt) }}</dd>
                </div>
                <div v-if="app.processedAt">
                  <dt>处理时间</dt>
                  <dd>{{ displayDate(app.processedAt) }}</dd>
                </div>
                <div v-if="app.status === 'REJECTED'">
                  <dt>拒绝原因</dt>
                  <dd>{{ app.rejectionReason || '未填写原因' }}</dd>
                </div>
              </dl>

              <div v-if="canReview(app.status, recruitment)" class="recruit-actions">
                <button
                  class="primary"
                  type="button"
                  :disabled="!!busy"
                  @click="ask(app, 'accept')"
                >
                  接受
                </button>
                <button type="button" :disabled="!!busy" @click="ask(app, 'reject')">拒绝</button>
                <span v-if="busy?.id === app.id" class="hint">处理中…</span>
              </div>
              <p v-else-if="app.status === 'PENDING'" class="hint">
                该招募当前不是“招募中”，不能处理这条申请。
              </p>

              <div
                v-if="confirming?.id === app.id"
                class="recruit-confirm"
                role="group"
                aria-label="确认处理申请"
              >
                <template v-if="confirming.kind === 'accept'">
                  <p>确认接受 {{ app.applicantDisplayName }} 的申请？接受后会立即占用一个名额。</p>
                  <p v-if="recruitment && recruitment.remainingSlots <= 0" class="form-error">
                    当前已没有剩余名额，接受会失败，请先确认人数设置。
                  </p>
                  <div class="recruit-actions">
                    <button
                      class="primary"
                      type="button"
                      :disabled="!!busy"
                      @click="run(app, 'accept')"
                    >
                      确认接受
                    </button>
                    <button type="button" :disabled="!!busy" @click="cancelAsk">取消</button>
                  </div>
                </template>
                <template v-else>
                  <p>
                    拒绝 {{ app.applicantDisplayName }} 的申请。拒绝原因会展示给申请人，可留空。
                  </p>
                  <label>
                    拒绝原因（可选）
                    <input
                      v-model="reason"
                      maxlength="255"
                      placeholder="例如：当前队伍方向不匹配"
                    />
                  </label>
                  <small class="hint">{{ reason.length }} / 255</small>
                  <div class="recruit-actions">
                    <button
                      class="danger"
                      type="button"
                      :disabled="!!busy"
                      @click="run(app, 'reject')"
                    >
                      确认拒绝
                    </button>
                    <button type="button" :disabled="!!busy" @click="cancelAsk">取消</button>
                  </div>
                </template>
              </div>
            </article>
          </div>
          <div v-else class="recruit-app-empty">
            <p>还没有收到申请。</p>
            <p class="hint">学生提交申请后会出现在这里，可以再等一下或检查招募是否还在招募中。</p>
          </div>
        </template>
      </section>

      <p class="hint">
        申请者的院系、年级和技能标签目前后端未返回，这里只展示申请时填写的内容，不显示为“未填写”。
      </p>
    </template>
  </section>
</template>
