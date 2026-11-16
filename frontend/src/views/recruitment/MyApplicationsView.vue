<script setup lang="ts">
import { computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApplicationsStore } from '@/stores/applications'
import { applicationStatusLabels } from '@/types/application'
import { displayDate } from '@/types/recruitment'
import WithdrawApplication from '@/components/recruitment/WithdrawApplication.vue'
const route = useRoute(),
  auth = useAuthStore(),
  applications = useApplicationsStore()
watch(
  () => auth.token,
  () => {
    if (auth.token) void applications.load()
  },
  { immediate: true },
)
const target = computed(() =>
  typeof route.query.applicationId === 'string' ? route.query.applicationId : '',
)
const found = computed(() => applications.items.some((item) => String(item.id) === target.value))
watch(
  [target, () => applications.ready],
  async () => {
    if (!applications.ready || !found.value) return
    await nextTick()
    document.getElementById(`application-${target.value}`)?.scrollIntoView({ block: 'center' })
  },
  { immediate: true },
)
</script>
<template>
  <section class="recruit">
    <header>
      <p class="eyebrow">我的组队</p>
      <h1>我的申请</h1>
      <p>保留每一次申请记录，查看结果或撤回待处理申请。</p>
    </header>
    <p v-if="applications.busy" role="status">正在处理申请，请稍候…</p>
    <p v-if="applications.notice" role="status">{{ applications.notice }}</p>
    <p v-if="applications.actionError" class="form-error" role="alert">
      {{ applications.actionError }}
    </p>
    <p v-if="applications.loading" role="status">正在加载申请…</p>
    <section v-else-if="applications.error" role="alert">
      <p>{{ applications.error }}</p>
      <button @click="applications.load">重试</button>
    </section>
    <template v-else-if="applications.ready">
      <p v-if="target && !found" role="status">
        未找到通知对应的申请，以下仍展示你的全部申请记录。
      </p>
      <div v-if="!applications.items.length" class="panel recruit-card">
        <h2>还没有申请</h2>
        <RouterLink to="/recruitments">到招募大厅寻找队伍</RouterLink>
      </div>
      <article
        v-for="item in applications.items"
        :id="`application-${item.id}`"
        :key="item.id"
        class="panel recruit-card"
        :class="{ 'application-highlight': String(item.id) === target }"
      >
        <div class="recruit-meta">
          <h2>
            <RouterLink :to="{ name: 'recruitment-detail', params: { id: item.recruitmentId } }">{{
              item.recruitmentTitle
            }}</RouterLink>
          </h2>
          <strong>{{ applicationStatusLabels[item.status] }}</strong>
        </div>
        <p>申请时间：{{ displayDate(item.createdAt) }}</p>
        <p v-if="item.processedAt">处理时间：{{ displayDate(item.processedAt) }}</p>
        <p v-if="item.rejectionReason">拒绝原因：{{ item.rejectionReason }}</p>
        <details>
          <summary>查看申请内容</summary>
          <p>自我介绍：{{ item.introduction || '未填写' }}</p>
          <p>技能说明：{{ item.skillsDescription || '未填写' }}</p>
          <p>可投入时间：{{ item.availableTime || '未填写' }}</p>
        </details>
        <WithdrawApplication v-if="item.status === 'PENDING'" :id="item.id" />
      </article>
    </template>
  </section>
</template>
