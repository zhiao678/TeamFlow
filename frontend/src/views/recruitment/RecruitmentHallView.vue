<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecruitmentCard from '@/components/recruitment/RecruitmentCard.vue'
import { getRecruitments } from '@/api/recruitment-query'
import { useRecruitmentRead, useBrowseClock } from '@/composables/useRecruitmentRead'
import { listCategories } from '@/api/dictionaries'
import type { RecruitmentQuery } from '@/types/recruitment'
import { statusLabels } from '@/types/recruitment'
const route = useRoute(),
  router = useRouter(),
  now = useBrowseClock()
const blank = () => ({
  keyword: '',
  categoryId: '',
  courseName: '',
  teacherName: '',
  status: 'RECRUITING',
  sort: 'LATEST',
})
const form = reactive(blank())
const applied = computed(() => {
  const f = blank()
  for (const k of Object.keys(f) as (keyof typeof f)[]) {
    const v = route.query[k]
    if (typeof v === 'string' && v.trim()) f[k] = v.trim()
  }
  if (!['RECRUITING', 'TEAMED', 'CLOSED'].includes(f.status)) f.status = 'RECRUITING'
  if (f.sort !== 'DEADLINE') f.sort = 'LATEST'
  return f
})
watch(applied, (f) => Object.assign(form, f), { immediate: true })
function apply(reset = false) {
  if (reset) Object.assign(form, blank())
  const query = Object.fromEntries(
    Object.entries(form)
      .map(([k, v]) => [k, v.trim()])
      .filter(([k, v]) => v && v !== blank()[k as keyof typeof form]),
  )
  void router.push({ name: 'recruitment-hall', query })
}
const positive = (value: unknown, fallback: number) => {
  const n = Number(value)
  return Number.isSafeInteger(n) && n > 0 ? n : fallback
}
const query = computed<RecruitmentQuery>(() => ({
  ...applied.value,
  categoryId: applied.value.categoryId
    ? positive(applied.value.categoryId, 0) || undefined
    : undefined,
  status: applied.value.status as RecruitmentQuery['status'],
  sort: applied.value.sort as RecruitmentQuery['sort'],
  page: positive(route.query.page, 1),
  pageSize: Math.min(100, positive(route.query.pageSize, 20)),
}))
const { data, loading, error, retry } = useRecruitmentRead(
  () => JSON.stringify(query.value),
  (key, signal) => getRecruitments(JSON.parse(key) as RecruitmentQuery, signal),
)
const {
  data: categories,
  error: categoryError,
  retry: retryCategories,
} = useRecruitmentRead(
  () => 'categories',
  () => listCategories(),
)
const pages = computed(() =>
  Math.max(1, Math.ceil((data.value?.total ?? 0) / (data.value?.pageSize ?? query.value.pageSize))),
)
const page = computed(() => data.value?.page ?? query.value.page)
const visible = computed(() => data.value?.records ?? [])
function go(n: number) {
  void router.push({
    name: 'recruitment-hall',
    query: { ...route.query, page: n === 1 ? undefined : String(n) },
  })
}
</script>
<template>
  <section class="recruit">
    <header>
      <p class="eyebrow">校园组队</p>
      <h1>招募大厅</h1>
      <p>寻找同一门课、同一场比赛，或想一起做事的队友。</p>
    </header>
    <form class="panel recruit-filters" @submit.prevent="apply()">
      <label
        >关键词<input v-model="form.keyword" type="search" placeholder="标题、活动、课程或教师"
      /></label>
      <label
        >分类<select v-model="form.categoryId">
          <option value="">全部分类</option>
          <option v-for="item in categories" :key="item.id" :value="String(item.id)">
            {{ item.name }}
          </option>
        </select></label
      >
      <label>课程<input v-model="form.courseName" placeholder="课程名称" /></label
      ><label>教师<input v-model="form.teacherName" placeholder="教师姓名" /></label>
      <label
        >状态<select v-model="form.status" aria-label="状态">
          <option v-for="s in ['RECRUITING', 'TEAMED', 'CLOSED'] as const" :key="s" :value="s">
            {{ statusLabels[s] }}
          </option>
        </select></label
      >
      <label
        >排序<select v-model="form.sort">
          <option value="LATEST">最新发布</option>
          <option value="DEADLINE">即将截止</option>
        </select></label
      >
      <div class="recruit-actions">
        <button class="primary">搜索</button
        ><button type="button" @click="apply(true)">清除条件</button>
      </div>
    </form>
    <p v-if="categoryError" role="alert">
      分类加载失败：{{ categoryError }} <button @click="retryCategories">重试分类</button>
    </p>
    <p v-if="loading" role="status">正在加载招募…</p>
    <section v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button @click="retry">重试</button>
    </section>
    <template v-else
      ><p>共 {{ data?.total ?? 0 }} 条结果</p>
      <div v-if="visible.length" class="recruit-grid">
        <RecruitmentCard v-for="row in visible" :key="row.id" :item="row" :now="now" />
      </div>
      <div v-else class="panel recruit-card">
        <h2>没有符合条件的招募</h2>
        <p>试试其他关键词，或清除筛选条件。</p>
        <button @click="apply(true)">重置筛选</button>
      </div>
      <nav v-if="pages > 1" class="recruit-actions" aria-label="结果分页">
        <button :disabled="page <= 1" @click="go(page - 1)">上一页</button
        ><span>{{ page }} / {{ pages }}</span
        ><button :disabled="page >= pages" @click="go(page + 1)">下一页</button>
      </nav>
    </template>
  </section>
</template>
