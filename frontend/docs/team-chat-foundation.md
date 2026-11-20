# 队伍管理与聊天：公共接入约定

本次仅准备路由、类型、API 和共享方法，不实现页面、聊天轮询或导航入口。
所有字段已对照当前后端 DTO/VO；真实接口和规则见 [API 规范](../../docs/recruitment-api-spec.md)。

## 路由和页面分工

统一定义位于 `src/router/team-routes.ts`，路径与名称不要在各模块另行发明。

| key | 路径 | name | 页面归属 |
| --- | --- | --- | --- |
| myTeams | `/my-teams` | my-teams | 招募浏览/成员侧 |
| conversations | `/conversations` | conversations | 公共聊天 |
| conversation | `/conversations/:id` | conversation | 公共聊天，id 为会话 ID |
| memberExits | `/my-member-exits` | my-member-exits | 招募浏览/成员侧 |
| leaderTransfers | `/my-leader-transfers` | my-leader-transfers | 招募浏览/成员侧 |
| teamManagement | `/my-recruitments/:id/team-management` | team-management | 招募管理，id 为招募 ID |

`src/router/index.ts` 已预留 `createTeamRoutes({})` 接入点。页面完成后把 loader 加进去：

```ts
...createTeamRoutes({
  myTeams: () => import('@/views/team/MyTeamsView.vue'),
  // 其他已完成的页面按 key 补充
}),
```

工厂自动附带 `meta.title` 和 `meta.requiresAuth: true`，只注册实际传入的页面。
当前空对象不增加占位页、可点击空入口或可绕过的登录限制。
进入页面可使用 `teamRoutePath('conversation', conversation.id)`；动态 ID 必须为正整数。
调用方应确认路由已注册，例如 `router.hasRoute(teamRoutes.conversation.name)`，再展示入口。
转交通知后续约定跳 `leaderTransfers` 并传 `transferId` query；该页需支持记录定位。
本次不改变现有通知跳转，待页面实际接入后统一启用。

## 类型与 API 归属

- `src/types/conversation.ts`：Conversation、ConversationMessage、MessagePage、MessageQuery、SendMessageInput、ConversationRead。
- `src/types/team-management.ts`：MemberExitRequest、LeaderTransfer 及状态文案。普通退出使用 APPROVED，负责人转交使用 ACCEPTED，不能混用。
- 正式成员继续复用 `src/types/application.ts` 的 TeamMember；其 id 是用户 ID。
- `src/api/conversations.ts`：会话列表、创建私聊、招募对应队伍群、消息分页/增量、发送和已读位置。
- `src/api/team-management.ts`：成队、移除成员、退出申请/审批、负责人退出转交/接收；双方共同复用，不复制接口。
- 成员查询继续复用 `src/api/members.ts`，招募详情和申请查询继续使用已有封装。
- 新接口复用公共 Axios，通过 `unwrapTeamResult` 返回业务对象；不改变其他模块的完整 Axios 响应约定。
- `teamErrorStatus` 返回 HTTP 或业务码对应的状态，`teamErrorMessage` 提供统一反馈。业务页面用状态区分 403 撤销资格、404 不存在、409 冲突刷新等，不自动重试写请求。
- GET 方法支持 AbortSignal，切换账号、路由或聊天会话时由页面取消旧读取。

## 共享刷新方法

`useTeamSyncStore()` 是重新查询信号，不缓存队伍或消息，不主动替页面查询负责人专属接口。

```ts
const sync = useTeamSyncStore()
const session = sync.sessionRevision // 在发送写请求前捕获
await formTeam(recruitmentId) // 示例；其他写操作同理
const sameSession = await sync.afterTeamMutation(recruitmentId, session)
if (!sameSession) return // 不再给新账号显示旧操作结果或执行旧跳转
```

成功后会递增该招募 `revisions[id]`、全局 `conversationsRevision`，并刷新当前用户通知未读数。
订阅者负责重新查询：招募/申请/成员/退出记录、会话列表与群入口。不得据此本地加减人数或直接授予群权限。

```ts
watch(
  () => [sync.sessionRevision, sync.revisions[recruitmentId.value]],
  reloadTeamData, // 页面自行按当前身份加载；初次加载和旧响应隔离也由页面负责
)
```

聊天/我的队伍订阅 `conversationsRevision` 和 `sessionRevision`；切换招募还需监听路由 ID。
只应在页面挂载范围内订阅，避免多个页面遗留重复请求。
遇到 409 可调用 `sync.invalidateTeam(id, session)` 触发刷新，但不能调用成功提示。
写请求成功后若刷新失败，要分别显示成功与刷新错误，避免用户重复提交。
通知刷新失败不把已经成功的业务请求当失败；查看通知 Store 的 countError 可获知失败。
Token 变化会清理刷新状态，旧 session 的后续调用不会影响新账号。

## 消息公共方法与后续聊天约定

`src/utils/conversation-messages.ts` 提供：

- `createMessageDraft(content, sourceRecruitmentId?)`：生成一次 clientMessageId，保留正文。重试同条消息复用整个输入，不重新生成 ID；修改正文属于新的发送意图。
- `mergeConversationMessages(conversationId, current, incoming)`：按服务端 ID 去重升序，只允许合并同一会话。

发送请求不会自动生成 ID 或把消息标记为发送成功；页面需保存待发送/失败状态，成功后用服务端对象替换。
历史分页、增量短轮询 3–5 秒、停止轮询、权限失效和视口已读判断留给聊天页面实现。
没有当前会话的消息游标时省略 afterMessageId，不传 0 或其他会话的 ID。
增量 hasMore=true 时应继续取后续批次，避免一次只取一批造成漏消息。
聊天 unreadCount 与站内通知 unreadCount 独立维护；Conversation.status=READ_ONLY 时不得提供发送入口。

## 业务约束摘要

- formTeam 返回招募对象，不是会话；成队后通过 getTeamConversation 取得会话。
- 我的队伍以有权访问的 TEAM 会话为依据，不从申请历史推断成员身份。
- 负责人确认转交前保持原资格；接受后按服务端 exitAfterTransfer 区分原负责人是否离队。
- 普通退出提交后仍是成员，只有批准后才离队；移除、退出、转交后重新查询群权限。
- 原有筛选、收藏、申请与通知行为保持不变，本次未提前实现业务页面或成员状态转换。

## 检查

运行 `node scripts/check-team-foundation.mjs` 验证本地接口契约、路由工厂、消息去重与会话隔离。
该检查不代表真实后端或浏览器页面已验收；业务完成后仍需负责人、成员、申请人三账号联调。
提交前运行 type-check、lint:check、format:check、build。
