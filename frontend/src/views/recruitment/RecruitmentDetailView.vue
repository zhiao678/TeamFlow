<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getRecruitment } from '@/api/recruitment-query'
import { useRecruitmentRead, useBrowseClock } from '@/composables/useRecruitmentRead'
import { displayStatus, displayDate } from '@/types/recruitment'
const route = useRoute()
const id = computed(() => String(route.params.id))
const valid = computed(() => /^[1-9]\d*$/.test(id.value) && Number.isSafeInteger(Number(id.value)))
const {
  data: item,
  error,
  loading,
  retry,
} = useRecruitmentRead(
  () => id.value,
  (value, signal) => (valid.value ? getRecruitment(Number(value), signal) : Promise.resolve(null)),
)
const now = useBrowseClock()
</script>
<template>
  <main class="recruit">
    <RouterLink :to="{ name: 'recruitment-hall', query: route.query }">← 返回招募大厅</RouterLink>
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
      <p>申请、收藏和私聊功能暂未开放。</p>
    </article>
  </main>
</template>
