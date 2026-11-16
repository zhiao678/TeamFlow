<script setup lang="ts">
import { computed } from 'vue'
import RecruitmentCard from '@/components/recruitment/RecruitmentCard.vue'
import { listFavorites } from '@/api/favorites'
import { useFavoritesStore } from '@/stores/favorites'
import { useRecruitmentRead, useBrowseClock } from '@/composables/useRecruitmentRead'
const favorites = useFavoritesStore(),
  now = useBrowseClock()
const { data, loading, error, retry } = useRecruitmentRead(
  () => 'favorites',
  async (_, signal) => {
    const ingest = favorites.reader()
    const rows = await listFavorites(signal)
    if (!signal.aborted) ingest(rows)
    return rows
  },
)
const visible = computed(() =>
  (data.value ?? []).filter((row) => favorites.states[row.id] ?? row.favorited),
)
</script>
<template>
  <section class="recruit">
    <header>
      <p class="eyebrow">我的组队</p>
      <h1>我的收藏</h1>
      <p>按收藏时间倒序展示，随时回来寻找合适的队伍。</p>
    </header>
    <p v-if="loading" role="status">正在加载收藏…</p>
    <section v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button @click="retry">重试</button>
    </section>
    <div v-else-if="visible.length" class="recruit-grid">
      <RecruitmentCard v-for="item in visible" :key="item.id" :item="item" :now="now" />
    </div>
    <div v-else class="panel recruit-card">
      <h2>还没有收藏</h2>
      <p>到招募大厅看看，收藏感兴趣的队伍。</p>
      <RouterLink to="/recruitments">前往招募大厅</RouterLink>
    </div>
  </section>
</template>
