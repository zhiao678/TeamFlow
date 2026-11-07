<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecruitmentForm from '@/components/recruitment/RecruitmentForm.vue'
import { browseError, getRecruitment } from '@/api/recruitment-query'
import {
  FALLBACK_CATEGORIES,
  FALLBACK_TAGS,
  closeRecruitment,
  createRecruitment,
  listCategories,
  listTags,
  managementMessage,
  publishRecruitment,
  updateRecruitment,
} from '@/api/recruitment-management'
import { useAuthStore } from '@/stores/auth'
import { displayDate, displayStatus, timestamp } from '@/types/recruitment'
import type {
  Recruitment,
  RecruitmentFormState,
  RecruitmentInput,
  RecruitmentTagOption,
} from '@/types/recruitment'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const editing = computed(() => route.name === 'recruitment-edit')
const rawId = computed(() => String(route.params.id ?? ''))
const id = computed(() => Number(rawId.value))
const validId = computed(() => /^[1-9]\d*$/.test(rawId.value) && Number.isSafeInteger(id.value))

const blank = (): RecruitmentFormState => ({
  categoryId: null,
  title: '',
  courseName: '',
  teacherName: '',
  purpose: '',
  description: '',
  requirements: '',
  maxMembers: 3,
  deadlineAt: '',
  tagIds: [],
})
const form = reactive<RecruitmentFormState>(blank())

const record = ref<Recruitment | null>(null)
const loading = ref(editing.value)
const loadError = ref('')
const submitting = ref(false)
const formError = ref('')
const notice = ref('')
const errors = ref<Record<string, string>>({})
const categories = ref<RecruitmentTagOption[]>([])
const tags = ref<RecruitmentTagOption[]>([])
const optionsFallback = ref(false)
const unmatchedTags = ref<string[]>([])
const tagsDirty = ref(false)

const locked = computed(
  () => record.value?.status === 'TEAMED' || record.value?.status === 'CLOSED',
)
const isOwner = computed(() => !!record.value && auth.user?.id === record.value.ownerId)
const currentMembers = computed(() => record.value?.currentMembers ?? 1)

// 用户一开始修改，就清掉上一次提交留下的字段级报错，避免“已经填了还提示必填”。
watch(
  form,
  () => {
    if (Object.keys(errors.value).length) errors.value = {}
  },
  { deep: true },
)

async function loadOptions() {
  const [category, tag] = await Promise.allSettled([listCategories(), listTags()])
  const loadedCategories = category.status === 'fulfilled' ? category.value : []
  const loadedTags = tag.status === 'fulfilled' ? tag.value : []
  categories.value = loadedCategories.length ? loadedCategories : FALLBACK_CATEGORIES
  tags.value = loadedTags.length ? loadedTags : FALLBACK_TAGS
  optionsFallback.value = !loadedCategories.length || !loadedTags.length
}

function applyTags(names: string[]) {
  const matched = names
    .map((name) => tags.value.find((item) => item.name === name))
    .filter((item): item is RecruitmentTagOption => !!item)
  form.tagIds = matched.map((item) => item.id)
  unmatchedTags.value = names.filter((name) => !tags.value.some((item) => item.name === name))
}

async function loadRecord() {
  if (!validId.value) {
    loading.value = false
    loadError.value = '招募编号无效，请从“我的招募”重新进入。'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await getRecruitment(id.value)
    record.value = data
    Object.assign(form, {
      categoryId: data.categoryId,
      title: data.title,
      courseName: data.courseName ?? '',
      teacherName: data.teacherName ?? '',
      purpose: data.purpose,
      description: data.description ?? '',
      requirements: data.requirements ?? '',
      maxMembers: data.maxMembers,
      deadlineAt: data.deadlineAt ?? '',
      tagIds: [],
    })
    applyTags(data.tags)
  } catch (cause) {
    loadError.value = browseError(cause)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await auth.restore()
  await loadOptions()
  if (editing.value) await loadRecord()
})

/** 后端只接受 `yyyy-MM-ddTHH:mm:ss`，浏览器控件可能省略秒位。 */
function normaliseDeadline(value: string) {
  if (!value) return null
  const withSeconds = value.length === 16 ? `${value}:00` : value
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(withSeconds) ? withSeconds : null
}

function validate(publishing: boolean) {
  const next: Record<string, string> = {}
  if (form.categoryId === null) next.categoryId = '请选择分类。'
  const title = form.title.trim()
  if (!title) next.title = '请填写招募标题。'
  else if (title.length > 150) next.title = '标题最多 150 个字符。'
  const purpose = form.purpose.trim()
  if (!purpose) next.purpose = '请填写组队目的或活动名称。'
  else if (purpose.length > 200) next.purpose = '组队目的最多 200 个字符。'
  if (form.courseName.trim().length > 100) next.courseName = '课程名称最多 100 个字符。'
  if (form.teacherName.trim().length > 100) next.teacherName = '教师名称最多 100 个字符。'
  if (typeof form.maxMembers !== 'number' || Number.isNaN(form.maxMembers))
    next.maxMembers = '请填写总人数上限。'
  else if (form.maxMembers < 2) next.maxMembers = '总人数至少为 2（含负责人）。'
  else if (form.maxMembers < currentMembers.value)
    next.maxMembers = `不能小于当前在队人数（${currentMembers.value} 人）。`
  const deadline = normaliseDeadline(form.deadlineAt)
  if (form.deadlineAt && !deadline) next.deadlineAt = '截止时间格式不正确，请重新选择。'
  if (publishing && deadline && timestamp(deadline) <= Date.now())
    next.deadlineAt = '发布时截止时间需要晚于当前时间。'
  errors.value = next
  return Object.keys(next).length === 0
}

function toInput(): RecruitmentInput {
  return {
    categoryId: Number(form.categoryId),
    title: form.title.trim(),
    courseName: form.courseName.trim(),
    teacherName: form.teacherName.trim(),
    purpose: form.purpose.trim(),
    description: form.description.trim(),
    requirements: form.requirements.trim(),
    maxMembers: Number(form.maxMembers),
    deadlineAt: normaliseDeadline(form.deadlineAt),
    tagIds: [...form.tagIds],
  }
}

async function save(publish: boolean) {
  if (submitting.value || locked.value) return
  if (editing.value && !isOwner.value) return
  formError.value = ''
  notice.value = ''
  if (!validate(publish)) return
  submitting.value = true
  try {
    const input = toInput()
    if (!editing.value) {
      await createRecruitment(input, publish)
      await router.push({ name: 'my-recruitments' })
      return
    }
    const saved = await updateRecruitment(id.value, input, {
      includeTagIds: !unmatchedTags.value.length || tagsDirty.value,
    })
    record.value = saved
    const publishDraft = publish && saved.status === 'DRAFT'
    if (publishDraft) record.value = await publishRecruitment(id.value)
    notice.value = publishDraft ? '招募已发布。' : '修改已保存。'
  } catch (cause) {
    formError.value = managementMessage(cause)
  } finally {
    submitting.value = false
  }
}

const confirmingClose = ref(false)
const closeReason = ref('')

async function confirmClose() {
  if (submitting.value) return
  submitting.value = true
  formError.value = ''
  try {
    record.value = await closeRecruitment(id.value, closeReason.value.trim())
    confirmingClose.value = false
    closeReason.value = ''
    notice.value = '招募已关闭，不再接受新的申请。'
  } catch (cause) {
    formError.value = managementMessage(cause)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="recruit">
    <p>
      <RouterLink v-if="editing && validId" :to="{ name: 'recruitment-detail', params: { id } }"
        >← 返回招募详情</RouterLink
      >
      <RouterLink v-else :to="{ name: 'recruitment-hall' }">← 返回招募大厅</RouterLink>
    </p>

    <header>
      <p class="eyebrow">招募管理</p>
      <h1>{{ editing ? '编辑招募' : '发布招募' }}</h1>
      <p v-if="editing">
        当前状态：{{ record ? displayStatus(record) : '加载中' }}
        <template v-if="record"> · 截止 {{ displayDate(record.deadlineAt) }}</template>
      </p>
      <p v-else>负责人由当前登录账号自动担任，并计入第一个成员名额。</p>
    </header>

    <p v-if="optionsFallback" class="notice" role="note">
      分类与技能选项接口（<code>GET /api/categories</code>、<code>GET /api/tags</code>）尚未提供，
      当前使用与演示数据一致的临时选项，接口补齐后需替换。
    </p>

    <p v-if="loading" role="status">正在加载招募…</p>
    <section v-else-if="loadError" role="alert" class="panel recruit-card">
      <p>{{ loadError }}</p>
      <button @click="loadRecord">重试</button>
    </section>

    <section v-else-if="editing && !isOwner" role="alert" class="panel recruit-card">
      <h2>无权编辑此招募</h2>
      <p>只有招募负责人可以编辑、发布或关闭。若这是你的招募，请确认登录账号是否一致。</p>
      <RouterLink v-if="validId" :to="{ name: 'recruitment-detail', params: { id } }"
        >查看招募详情</RouterLink
      >
    </section>

    <template v-else>
      <p v-if="locked" class="notice" role="note">
        该招募已{{ record?.status === 'TEAMED' ? '成队' : '关闭' }}，内容不能再修改。
        <RouterLink v-if="validId" :to="{ name: 'recruitment-detail', params: { id } }"
          >查看详情</RouterLink
        >
      </p>

      <section class="panel" :aria-busy="submitting">
        <RecruitmentForm
          v-model="form"
          :categories="categories"
          :tags="tags"
          :current-members="currentMembers"
          :errors="errors"
          :unmatched-tags="unmatchedTags"
          :disabled="submitting || locked"
          @tags-change="tagsDirty = true"
        />

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <p v-if="notice" class="notice" role="status">{{ notice }}</p>

        <div v-if="!locked" class="recruit-actions recruit-form-actions">
          <button
            class="primary"
            type="button"
            :disabled="submitting || (editing && !isOwner)"
            @click="save(false)"
          >
            {{ submitting ? '处理中…' : editing ? '保存修改' : '保存草稿' }}
          </button>
          <button
            v-if="!editing || record?.status === 'DRAFT'"
            type="button"
            :disabled="submitting || (editing && !isOwner)"
            @click="save(true)"
          >
            {{ editing ? '发布招募' : '直接发布' }}
          </button>
          <button
            v-if="editing && record?.status === 'RECRUITING'"
            type="button"
            class="danger"
            :disabled="submitting"
            @click="confirmingClose = true"
          >
            关闭招募
          </button>
          <button
            v-if="editing && record?.status === 'DRAFT'"
            type="button"
            disabled
            title="删除草稿接口（DELETE /api/recruitments/{id}）尚未提供"
          >
            删除草稿（接口未提供）
          </button>
        </div>

        <div v-if="confirmingClose" class="recruit-confirm" role="group" aria-label="确认关闭招募">
          <p>关闭后不再接受申请，且不能再编辑。确定关闭这条招募吗？</p>
          <label
            >关闭原因（可选）
            <input v-model="closeReason" maxlength="200" placeholder="例如：已通过其他渠道完成组队"
          /></label>
          <div class="recruit-actions">
            <button class="danger" type="button" :disabled="submitting" @click="confirmClose">
              确认关闭
            </button>
            <button type="button" :disabled="submitting" @click="confirmingClose = false">
              取消
            </button>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>
