# 聊天与本周通知接入说明

## 已实现

- `/conversations`：当前有效会话列表，支持全部/私聊/队伍群筛选，条件保存到 URL。
- `/conversations/:id`：群聊和私聊共用页面，展示最新一页和可向前加载的历史消息。
- 可见聊天页每 4 秒按 afterMessageId 增量读取，hasMore 分批补齐；列表页每 5 秒刷新。
- 页面隐藏时不继续发起轮询，恢复可见后同步；离开页面停止定时器，离开聊天页取消消息读取。
- 发送文本、发送中状态、失败草稿及原消息重试。发送内容和 clientMessageId 保持一致，不伪造成功。
- 发送回执与历史/轮询结果按服务端消息 ID 合并；发送回执不跳过尚未读取的并发消息。
- 只有视口到最新消息、页面可见且新消息补齐后才推进已读位置。失败可重试，不后台自动全部已读。
- 聊天未读数单独存储，顶部和侧栏展示；不与站内通知数量相加。
- 读取会话返回 403/404 后清理消息、停读停发并刷新有效会话列表；私聊发送被拒绝时保留失败草稿并提示核对屏蔽/成员状态。
- READ_ONLY 会话可看历史不可发送。换账号、切换会话和卸载页面隔离迟到响应。

页面与请求均使用真实 API；脚本中的受控服务仅供测试，应用没有 Mock 回退。

## 其他两人的接入方式

### 联系负责人

```ts
const conversation = await createPrivateConversation(recruitment.ownerId)
await router.push({
  path: teamRoutePath('conversation', conversation.id),
  query: { sourceRecruitmentId: String(recruitment.id) },
})
```

`sourceRecruitmentId` 是可选上下文，不是自动发送。聊天页提供勾选项，发送失败后原草稿重试不改变其来源。
来源招募必须公开；不要传草稿 ID。创建私聊会复用已有会话，详情页仍需处理游客登录返回、联系自己、屏蔽和用户不存在等反馈。

### 队伍群入口

```ts
const conversation = await getTeamConversation(recruitmentId)
await router.push(teamRoutePath('conversation', conversation.id))
```

必须以服务端返回的会话 ID 进入，不能把 recruitmentId 当作 conversationId。
我的队伍/成队/退出/转交页面仍由对应负责人实现；本次未替他们创建占位页。

### 业务刷新

`useTeamSyncStore().afterTeamMutation(id, capturedSession)` 已会刷新通知未读数，
公共层也订阅 conversationsRevision 刷新会话列表。我的队伍和管理页继续按基础约定重查自身数据。
会话已读和消息轮询会更新聊天未读数，业务页面无需手动加减计数。

## 本周通知

新增文案：RECRUITMENT_TEAMED、MEMBER_EXIT_REQUESTED/APPROVED/REJECTED、
LEADER_TRANSFER_REQUESTED/ACCEPTED/REJECTED/EXPIRED。保留原申请通知及未知事件的通用显示。

- 成队和退出处理结果：按 RECRUITMENT 目标进入招募详情。
- 收到普通退出申请：有队伍管理路由时进入对应管理页，否则先到我的招募并提示入口待接入。
- 负责人转交：有 `/my-leader-transfers` 时进入并携带 transferId，否则保留内容并提示待接入。
- 申请通知沿用上一周的行为，不猜测缺失的 recruitmentId。

所有链接只指向当前已注册页面，不用通知内容生成任意 URL；标记已读失败不阻止查看。
目前可以独立使用会话列表和聊天页；新的成员/管理页面完成后，需再完成通知精准定位和全流程联调。

## 验证

```sh
node scripts/check-chat.mjs
node scripts/check-notifications.mjs
node scripts/check-team-foundation.mjs
npm run type-check
npm run lint:check
npm run format:check
npm run build
```

check-chat 使用受控接口与真实 Vue 响应逻辑验证最新历史定位、多批轮询、视口已读、超时幂等重试、
并发消息不遗漏、迟到响应隔离、只读和权限撤销及通知目标。它不代替真实后端和浏览器验收。

实际联调需两个聊天账号以及负责人/成员/申请人：

1. 群聊与私聊打开正确，历史分页完整；两个账号互发消息，轮询不重复、不漏消息。
2. 向上看历史和切换后台不错误清除未读，回到最新消息才推进已读位置。
3. 发送超时后原消息重试不重复落库；只读、屏蔽、403/404/409 均显示错误而非假成功。
4. 退出/移除/负责人退出转交后，原账号不能继续读取发送群消息。
5. 成队、退出审批、转交通知能显示，目标页面实际接入后跳转到正确业务位置。

失败草稿仅保存在当前聊天页面内存；切换会话或账号会清理，手动重新加载同一会话会保留失败草稿。
本次浏览器预览工具连接本地服务超时，未记录浏览器或真实后端验收通过。
