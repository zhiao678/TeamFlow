<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFavoritesStore } from '@/stores/favorites'
import type { Recruitment } from '@/types/recruitment'
const props = defineProps<{ item: Recruitment }>()
const auth = useAuthStore(),
  favorites = useFavoritesStore(),
  route = useRoute(),
  router = useRouter()
const active = computed(
  () => !!auth.token && (favorites.states[props.item.id] ?? props.item.favorited),
)
function toggle() {
  if (!auth.token) {
    void router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  void favorites.toggle(props.item.id, active.value)
}
</script>
<template>
  <div>
    <button
      type="button"
      :aria-pressed="active"
      :disabled="favorites.busy[item.id]"
      @click="toggle"
    >
      {{ favorites.busy[item.id] ? '处理中…' : active ? '★ 取消收藏' : '☆ 收藏' }}
    </button>
    <p v-if="favorites.errors[item.id]" class="form-error" role="alert">
      {{ favorites.errors[item.id] }} 可点击按钮重试。
    </p>
  </div>
</template>
