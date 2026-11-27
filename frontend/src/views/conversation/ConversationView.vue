<script setup lang="ts">
import { computed, nextTick, onMounted, onScopeDispose, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useConversation } from '@/composables/useConversation'
import { teamRoutePath } from '@/router/team-routes'
const route = useRoute(),
  auth = useAuthStore()
const id = computed(() => (/^\d+$/.test(String(route.params.id)) ? Number(route.params.id) : NaN))
const scroller = ref<HTMLElement>(),
  atBottom = ref(true),
  documentVisible = ref(document.visibilityState === 'visible')
const visible = computed(() => atBottom.value && documentVisible.value)
const chat = useConversation(id, visible)
const content = ref(''),
  formError = ref(''),
  includeSource = ref(true)
const sourceId = computed(() => {
  const n = Number(route.query.sourceRecruitmentId)
  return Number.isSafeInteger(n) && n > 0 ? n : undefined
})
const title = computed(() =>
  chat.conversation.value?.type === 'TEAM'
    ? `队伍群 · 招募 #${chat.conversation.value.recruitmentId}`
    : chat.conversation.value?.otherUserDisplayName || '聊天',
)
const sending = computed(() => chat.outgoing.value.some((item) => item.state === 'sending'))
function checkScroll() {
  const el = scroller.value
  atBottom.value = !!el && el.scrollHeight - el.scrollTop - el.clientHeight < 32
  if (atBottom.value) void chat.markVisibleRead()
}
async function bottom() {
  await nextTick()
  const el = scroller.value
  if (el) el.scrollTop = el.scrollHeight
  checkScroll()
}
async function older() {
  const el = scroller.value
  if (!el) return
  atBottom.value = false
  const height = el.scrollHeight,
    top = el.scrollTop
  const conversationId = id.value
  await chat.loadOlder()
  await nextTick()
  if (conversationId !== id.value) return
  el.scrollTop = top + el.scrollHeight - height
  checkScroll()
}
async function send() {
  if (sending.value || !chat.canSend.value) return
  formError.value = ''
  try {
    const text = content.value
    const promise = chat.send(text, includeSource.value ? sourceId.value : undefined)
    content.value = ''
    atBottom.value = true
    await bottom()
    await promise
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '无法发送消息'
  }
}
watch(
  () => [route.params.id, auth.token],
  () => {
    content.value = ''
    formError.value = ''
    includeSource.value = true
    atBottom.value = true
  },
)
watch(
  () => [chat.messages.value.at(-1)?.id, chat.outgoing.value.length, chat.loading.value],
  () => {
    if (atBottom.value && !chat.loading.value) void bottom()
  },
  { flush: 'post' },
)
function visibilityChanged() {
  documentVisible.value = document.visibilityState === 'visible'
  if (documentVisible.value) checkScroll()
}
onMounted(() => document.addEventListener('visibilitychange', visibilityChanged))
onScopeDispose(() => document.removeEventListener('visibilitychange', visibilityChanged))
</script>
<template>
  <section class="chat-page">
    <RouterLink :to="teamRoutePath('conversations')">← 返回会话列表</RouterLink>
    <div class="page-heading">
      <div>
        <h1>{{ title }}</h1>
        <p>可见时每 4 秒获取增量消息；消息时间为北京时间。</p>
      </div>
      <el-button :loading="chat.loading.value" :disabled="sending" @click="chat.reload()"
        >重新加载</el-button
      >
    </div>
    <p v-if="chat.loading.value" role="status">正在加载会话与最新消息…</p>
    <div v-else-if="chat.error.value" class="panel profile-panel" role="alert">
      <p>{{ chat.error.value }}</p>
      <el-button @click="chat.reload()">重试</el-button
      ><RouterLink to="/conversations">查看有效会话</RouterLink>
    </div>
    <template v-else>
      <p v-if="chat.pollError.value" role="alert" class="form-error">
        {{ chat.pollError.value }}
        <el-button :loading="chat.polling.value" @click="chat.poll">重试同步</el-button>
      </p>
      <p v-if="chat.readError.value" role="alert">
        已读位置更新失败：{{ chat.readError.value }}
        <el-button :loading="chat.reading.value" @click="chat.markVisibleRead">重试已读</el-button>
      </p>
      <p v-if="chat.sendBlocked.value" role="alert">
        当前发送被拒绝，可能存在屏蔽关系或成员资格已变化；请核对后重新加载。
      </p>
      <p v-if="chat.conversation.value?.status === 'READ_ONLY'" role="status">
        此会话已只读，可查看历史消息，不能发送。
      </p>
      <div
        ref="scroller"
        class="chat-messages panel"
        role="region"
        aria-label="聊天消息"
        tabindex="0"
        @scroll="checkScroll"
      >
        <div class="history">
          <el-button
            v-if="chat.historyPage.value > 1"
            :loading="chat.historyLoading.value"
            @click="older"
            >加载更早消息</el-button
          >
          <p v-else>已到历史消息起点</p>
          <p v-if="chat.historyError.value" role="alert">{{ chat.historyError.value }}</p>
        </div>
        <el-empty
          v-if="!chat.messages.value.length && !chat.outgoing.value.length"
          description="暂无消息，开始交流吧。"
        />
        <article
          v-for="item in chat.messages.value"
          :key="item.id"
          class="chat-message"
          :class="{ mine: item.senderId === auth.user?.id }"
        >
          <div class="message-meta">
            {{ item.senderDisplayName }} · {{ item.createdAt.replace('T', ' ').slice(0, 19) }}
          </div>
          <p class="message-bubble">{{ item.content }}</p>
          <RouterLink
            v-if="item.sourceRecruitmentId"
            :to="`/recruitments/${item.sourceRecruitmentId}`"
            class="source-link"
            >来源招募 #{{ item.sourceRecruitmentId }}</RouterLink
          >
        </article>
        <article
          v-for="item in chat.outgoing.value"
          :key="item.input.clientMessageId"
          class="chat-message mine"
        >
          <p class="message-bubble">{{ item.input.content }}</p>
          <p v-if="item.state === 'sending'" role="status">正在发送…</p>
          <div v-else role="alert">
            <p class="form-error">发送未确认：{{ item.error }}</p>
            <el-button
              size="small"
              :disabled="sending || !chat.canSend.value"
              @click="chat.retry(item)"
              >原消息重试</el-button
            >
          </div>
        </article>
      </div>
      <div class="actions">
        <el-button v-if="!atBottom" @click="bottom">回到最新消息</el-button
        ><span v-if="!chat.caughtUp.value">正在补齐新消息…</span>
      </div>
      <form class="chat-composer panel" @submit.prevent="send">
        <label for="message-content">发送文本消息</label
        ><textarea
          id="message-content"
          v-model="content"
          rows="3"
          maxlength="2000"
          :disabled="!chat.canSend.value || sending"
          placeholder="输入消息，点击发送。失败消息可用相同编号重试。"
        ></textarea>
        <label v-if="sourceId" class="source-choice"
          ><input v-model="includeSource" type="checkbox" :disabled="sending" />附带来源招募 #{{
            sourceId
          }}</label
        >
        <div class="composer-actions">
          <span>{{ content.length }} / 2000</span
          ><el-button
            native-type="submit"
            type="primary"
            :loading="sending"
            :disabled="!chat.canSend.value || !content.trim()"
            >发送</el-button
          >
        </div>
        <p v-if="formError" role="alert" class="form-error">{{ formError }}</p>
      </form>
    </template>
  </section>
</template>
<style scoped>
.chat-page {
  max-width: 1000px;
  margin: auto;
}
.page-heading {
  margin-top: 18px;
}
.chat-messages {
  height: min(55vh, 560px);
  min-height: 260px;
  overflow-y: auto;
  padding: 20px;
  overflow-anchor: none;
}
.history {
  text-align: center;
  margin-bottom: 22px;
}
.chat-message {
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.chat-message.mine {
  align-items: flex-end;
}
.message-meta,
.source-link {
  font-size: 11px;
  color: #78849a;
  margin: 4px 0;
}
.message-bubble {
  color: #25334b;
  background: #f0f3f8;
  padding: 10px 14px;
  border-radius: 10px;
  max-width: 85%;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.mine .message-bubble {
  background: #eaf1ff;
}
.chat-composer {
  padding: 18px;
  display: grid;
  gap: 10px;
}
.chat-composer textarea {
  width: 100%;
  resize: vertical;
  padding: 12px;
  font: inherit;
  border: 1px solid #dce3ef;
  border-radius: 8px;
}
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #78849a;
}
.source-choice {
  font-size: 12px;
}
</style>
