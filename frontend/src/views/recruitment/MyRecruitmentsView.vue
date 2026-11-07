<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  closeRecruitment,
  isMyRecruitmentsUnavailable,
  listMyRecruitments,
  listRecruitmentsForOwnerFallback,
  managementMessage,
  publishRecruitment,
} from '@/api/recruitment-management'
import { useAuthStore } from '@/stores/auth'
import { useBrowseClock } from '@/composables/useRecruitmentRead'
import { displayDate, displayStatus, statusLabels, timestamp } from '@/types/recruitment'
import type { Recruitment } from '@/types/recruitment'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const now = useBrowseClock()

const filters = ['ALL', 'DRAFT', 'RECRUITING', 'TEAMED', 'CLOSED'] as const
type Filter = (typeof filters)[number]

const rows = ref<Recruitment[]>([])
const loading = ref(true)
const error = ref('')
const failure = ref('')
const notice = ref('')
const degraded = ref(false)
const busyId = ref(0)
const closingId = ref(0)
const closeReason = ref('')

const current = computed<Filter>(() => {
  const value = String(route.query.status ?? 'ALL')
  return (filters as readonly string[]).includes(value) ? (value as Filter) : 'ALL'
})
const counts = computed(() => {
  const map = { ALL: rows.value.length } as Record<Filter, number>
  for (const status of filters) if (status !== 'ALL') map[status] = 0
  for (const row of rows.value) map[row.status] += 1
  return map
})
const visible = computed(() =>
  [...rows.value]
    .filter((row) => current.value === 'ALL' || row.status === current.value)
    .sort((a, b) => timestamp(b.createdAt) - timestamp(a.createdAt) || b.id - a.id),
)

function cancelClose() {
  closingId.value = 0
  closeReason.value = ''
}

const filterLabel = (value: Filter) => (value === 'ALL' ? '全部' : statusLabels[value])

function setFilter(value: Filter) {
  void router.replace({
    name: 'my-recruitments',
    query: value === 'ALL' ? {} : { status: value },
  })
}

async function load() {
  loading.value = true
  error.value = ''
  failure.value = ''
  try {
    rows.value = await listMyRecruitments()
    degraded.value = false
  } catch (cause) {
    if (!isMyRecruitmentsUnavailable(cause)) {
      error.value = managementMessage(cause)
      loading.value = false
      return
    }
    try {
      const all = await listRecruitmentsForOwnerFallback()
      rows.value = all.filter((row) => row.ownerId === auth.user?.id)
      degraded.value = true
    } catch (fallbackCause) {
      error.value = managementMessage(fallbackCause)
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await auth.restore()
  await load()
})

function replace(updated: Recruitment) {
  rows.value = rows.value.map((row) => (row.id === updated.id ? updated : row))
}

async function publish(row: Recruitment) {
  if (busyId.value) return
  busyId.value = row.id
  failure.value = ''
  notice.value = ''
  try {
    replace(await publishRecruitment(row.id))
    notice.value = `《${row.title}》已发布。`
  } catch (cause) {
    failure.value = managementMessage(cause)
  } finally {
    busyId.value = 0
  }
}

async function confirmClose(row: Recruitment) {
  if (busyId.value) return
  busyId.value = row.id
  failure.value = ''
  notice.value = ''
  try {
    replace(await closeRecruitment(row.id, closeReason.value.trim()))
    closingId.value = 0
    closeReason.value = ''
    notice.value = `《${row.title}》已关闭。`
  } catch (cause) {
    failure.value = managementMessage(cause)
  } finally {
    busyId.value = 0
  }
}
</script>

<template>
  <section class="recruit">
    <header>
      <p class="eyebrow">招募管理</p>
      <h1>我的招募</h1>
      <p>管理自己发布的招募：编辑、发布草稿、关闭招募。</p>
    </header>

    <div class="recruit-actions">
      <RouterLink class="recruit-button primary" :to="{ name: 'recruitment-new' }"
        >+ 发布新招募</RouterLink
      >
      <button type="button" :disabled="loading" @click="load">刷新</button>
    </div>

    <p v-if="degraded" class="notice" role="note">
      正式接口 <code>GET /api/recruitments/mine</code> 尚未提供，当前从公开列表中按负责人过滤，
      属于临时方案；接口补齐后需要替换数据来源。
    </p>

    <nav class="recruit-actions" aria-label="按状态筛选">
      <button
        v-for="value in filters"
        :key="value"
        type="button"
        :class="{ primary: current === value }"
        :aria-pressed="current === value"
        @click="setFilter(value)"
      >
        {{ filterLabel(value) }}（{{ counts[value] }}）
      </button>
    </nav>

    <p v-if="loading" role="status">正在加载我的招募…</p>
    <section v-else-if="error" class="panel recruit-card" role="alert">
      <p>{{ error }}</p>
      <button @click="load">重试</button>
    </section>

    <template v-else>
      <p v-if="failure" class="form-error" role="alert">{{ failure }}</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>

      <div v-if="visible.length" class="recruit-list">
        <article v-for="row in visible" :key="row.id" class="panel recruit-card">
          <div class="recruit-meta">
            <span>{{ row.categoryName }}</span>
            <strong>{{ displayStatus(row, now) }}</strong>
          </div>
          <h2>
            <RouterLink :to="{ name: 'recruitment-detail', params: { id: row.id } }">{{
              row.title
            }}</RouterLink>
          </h2>
          <p>{{ row.purpose }}</p>
          <dl class="recruit-facts">
            <div>
              <dt>人数</dt>
              <dd>
                {{ row.currentMembers }} / {{ row.maxMembers }} 人，剩余 {{ row.remainingSlots }} 席
              </dd>
            </div>
            <div>
              <dt>截止时间</dt>
              <dd>{{ displayDate(row.deadlineAt) }}</dd>
            </div>
          </dl>
          <div class="recruit-actions">
            <RouterLink
              v-if="row.status === 'DRAFT' || row.status === 'RECRUITING'"
              class="recruit-button"
              :to="{ name: 'recruitment-edit', params: { id: row.id } }"
              >编辑</RouterLink
            >
            <button
              v-if="row.status === 'DRAFT'"
              type="button"
              :disabled="busyId === row.id"
              @click="publish(row)"
            >
              发布
            </button>
            <button
              v-if="row.status === 'RECRUITING'"
              type="button"
              class="danger"
              :disabled="busyId === row.id"
              @click="closingId = row.id"
            >
              关闭
            </button>
            <button
              v-if="row.status === 'DRAFT'"
              type="button"
              disabled
              title="删除草稿接口（DELETE /api/recruitments/{id}）尚未提供"
            >
              删除（接口未提供）
            </button>
          </div>
          <div
            v-if="closingId === row.id"
            class="recruit-confirm"
            role="group"
            aria-label="确认关闭招募"
          >
            <p>关闭后不再接受申请，且不能再编辑。确定关闭《{{ row.title }}》吗？</p>
            <label
              >关闭原因（可选）
              <input
                v-model="closeReason"
                maxlength="200"
                placeholder="例如：已通过其他渠道完成组队"
            /></label>
            <div class="recruit-actions">
              <button
                class="danger"
                type="button"
                :disabled="busyId === row.id"
                @click="confirmClose(row)"
              >
                确认关闭
              </button>
              <button type="button" :disabled="busyId === row.id" @click="cancelClose">取消</button>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="panel recruit-card">
        <h2>这里还没有招募</h2>
        <p v-if="current === 'ALL'">发布一条招募，开始寻找队友。</p>
        <p v-else>当前筛选条件下没有记录，试试切换到“全部”。</p>
        <RouterLink class="recruit-button primary" :to="{ name: 'recruitment-new' }"
          >发布招募</RouterLink
        >
      </div>
    </template>
  </section>
</template>
