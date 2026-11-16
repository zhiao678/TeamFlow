<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecruitmentCard from '@/components/recruitment/RecruitmentCard.vue'
import { getRecruitments } from '@/api/recruitment-query'
import { useRecruitmentRead, useBrowseClock } from '@/composables/useRecruitmentRead'
import { listCategories, listTags } from '@/api/dictionaries'
import type { RecruitmentQuery } from '@/types/recruitment'
import { blankDiscovery, discoveryFilters, discoveryQuery } from '@/utils/recruitment-discovery'
import { useFavoritesStore } from '@/stores/favorites'
import { statusLabels } from '@/types/recruitment'
const route = useRoute(),
  router = useRouter(),
  now = useBrowseClock()
const blank = blankDiscovery
const form = reactive(blank())
const applied = computed(() => discoveryFilters(route.query))
const selectedTags = computed({
  get: () => (form.tagIds ? form.tagIds.split(',') : []),
  set: (ids: string[]) => {
    form.tagIds = ids.join(',')
  },
})
watch(applied, (f) => Object.assign(form, f), { immediate: true })
function apply(reset = false) {
  if (reset) Object.assign(form, blank())
  const query = Object.fromEntries(
    Object.entries(form)
      .map(([k, v]) => [k, v.trim()])
      .filter(([k, v]) => v && v !== blank()[k as keyof typeof form]),
  )
  void router.push({
    name: 'recruitment-hall',
    query: { ...query, pageSize: route.query.pageSize },
  })
}
const query = computed<RecruitmentQuery>(() => discoveryQuery(route.query))
const favorites = useFavoritesStore()
const { data, loading, error, retry } = useRecruitmentRead(
  () => JSON.stringify(query.value),
  async (key, signal) => {
    const ingest = favorites.reader()
    const result = await getRecruitments(JSON.parse(key) as RecruitmentQuery, signal)
    if (!signal.aborted) ingest(result.records)
    return result
  },
)
const {
  data: categories,
  error: categoryError,
  retry: retryCategories,
} = useRecruitmentRead(
  () => 'categories',
  () => listCategories(),
)
const {
  data: tags,
  error: tagError,
  retry: retryTags,
} = useRecruitmentRead(
  () => 'tags',
  () => listTags(),
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
        >分类<select v-model="form.categoryId" @change="apply()">
          <option value="">全部分类</option>
          <option v-for="item in categories" :key="item.id" :value="String(item.id)">
            {{ item.name }}
          </option>
        </select></label
      >
      <label>课程<input v-model="form.courseName" placeholder="课程名称" /></label
      ><label>教师<input v-model="form.teacherName" placeholder="教师姓名" /></label>
      <label
        >状态<select v-model="form.status" aria-label="状态" @change="apply()">
          <option v-for="s in ['RECRUITING', 'TEAMED', 'CLOSED'] as const" :key="s" :value="s">
            {{ statusLabels[s] }}
          </option>
        </select></label
      >
      <label
        >排序<select v-model="form.sort" @change="apply()">
          <option value="LATEST">最新发布</option>
          <option value="DEADLINE">即将截止</option>
          <option value="POPULAR">热门招募</option>
        </select></label
      >
      <label
        >技能标签（可多选，匹配任一项）<select v-model="selectedTags" multiple @change="apply()">
          <option v-for="tag in tags" :key="tag.id" :value="String(tag.id)">{{ tag.name }}</option>
        </select></label
      >
      <label
        >最少剩余名额<input
          v-model="form.minRemainingSlots"
          type="number"
          min="0"
          step="1"
          placeholder="不限"
          @change="apply()"
      /></label>
      <div class="recruit-actions">
        <button class="primary">搜索</button
        ><button type="button" @click="apply(true)">清除条件</button>
      </div>
    </form>
    <p v-if="tagError" role="alert">
      技能标签加载失败：{{ tagError }} <button @click="retryTags">重试标签</button>
    </p>
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
