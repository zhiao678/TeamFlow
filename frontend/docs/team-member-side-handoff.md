# 我的队伍与成员侧操作交接

## 已有公共基础

先阅读 [队伍与聊天公共接入约定](team-chat-foundation.md)。公共基础提交为 `98ff7bf`，
当前本周分支为 `feat/frontend-team-management-chat`。开发前确认自己的分支已包含该基础。

- 路由：`src/router/team-routes.ts`，使用 `createTeamRoutes` 注册实际页面。
- 类型：`src/types/conversation.ts`、`src/types/team-management.ts`。
- 接口：`src/api/conversations.ts`、`src/api/team-management.ts`。
- 共享刷新：`src/stores/team-sync.ts`；统一错误：`src/api/team-result.ts`。
- 成员继续使用 `src/api/members.ts` 和现有 TeamMember 类型，不另写重复封装。
- 现有招募详情：`src/views/recruitment/RecruitmentDetailView.vue`；正式成员组件：`RecruitmentMembers.vue`。

## 待完成页面和操作

### 1. 我的队伍

- 通过 `listConversations()` 查询当前账号可访问的会话，筛选 `type === 'TEAM'`。
- 结合 `recruitmentId` 查询招募详情和正式成员，展示队伍名称、角色、人数及群聊入口。
- 本周仅覆盖确认成队后已经有队伍会话的队伍；不从申请历史推断正式成员身份。
- 不把 `READ_ONLY` 会话当成不存在；按服务端状态展示只读说明，由聊天页限制发送。
- 列表及单个队伍加载失败分别反馈，不能把请求失败显示为“未加入任何队伍”。
- 处理会话无权限或成员资格撤销后刷新列表，移除失效入口，不沿用旧缓存。
- 多条详情和成员查询避免无限并发，支持取消请求或旧响应隔离。

### 2. 普通成员退出

- 在本人所属队伍/招募详情的合适位置提供退出申请入口。
- 只有当前 ACTIVE 普通成员可以申请；负责人显示转交引导，不调用普通退出接口。
- 原因可选，最多 255 字符；提交中防重复点击。
- 提交后成员仍保留资格和人数，展示“等待负责人审批”，不能立即移出队伍或关闭群入口。
- 新增“我的退出申请”，展示队伍、原因、状态、处理时间、拒绝原因及 handoverHint。
- 状态统一使用 PENDING/APPROVED/REJECTED/EXPIRED；被拒后可重新申请，已处理记录不能再次提交审核动作。
- 批准后重新读取会话和成员资格，移除无权访问的群入口；刷新失败需明确提示。

### 3. 负责人转交收件箱

- 新增“负责人转交”页面，通过 `listMyLeaderTransfers()` 展示本人发起或收到的记录。
- 区分发起方与接收方，只有当前指定继任者对 PENDING 记录显示接受/拒绝。
- 接受前二次确认并展示 handoverHint 与 exitAfterTransfer 的实际含义。
- 接受后重新加载招募负责人、成员、会话和通知，不能直接在前端修改角色。
- `exitAfterTransfer=true` 表示原负责人转交后退出；历史 false 记录表示原负责人继续作为普通成员留队。
- 被移除、已退出或重复处理遇到 409，刷新真实记录并说明冲突，不自动重试写请求。
- 支持 `transferId` query 定位记录。找不到时提示记录不可用，不隐藏其他记录或伪造详情。

### 4. 详情页入口

- “联系负责人”复用 `createPrivateConversation(ownerId)`，成功后进入统一聊天页；不重复开发私聊窗口。
- 游客引导登录并返回，当前负责人不显示“联系自己”；屏蔽、用户不存在和网络失败需提示。
- 可用的队伍群通过 `getTeamConversation(recruitmentId)` 获取；仅取得合法会话后展示/进入群聊。
- 招募详情加载成功不等于有群权限；403/404 应与普通详情错误区分，不影响公开招募内容展示。
- 若聊天页尚未注册，不提供无效链接，明确入口待接入；不要为了接线创建空页面。

## 路由与接口对应

| route key | 路径 | 主要封装 |
| --- | --- | --- |
| myTeams | `/my-teams` | listConversations、招募详情、listRecruitmentMembers |
| memberExits | `/my-member-exits` | listMyMemberExits、requestMemberExit |
| leaderTransfers | `/my-leader-transfers` | listMyLeaderTransfers、acceptLeaderTransfer、rejectLeaderTransfer |
| conversation（只跳转） | `/conversations/:id` | createPrivateConversation、getTeamConversation |

在现有 `createTeamRoutes({})` 中补自己完成页面的 loader，工厂自动添加登录保护和标题。
与公共模块协调侧栏入口；动态会话 ID 与招募 ID 不可混用。接口结构以
[最新 API 规范](../../docs/recruitment-api-spec.md)为准，不自行增删字段。

## 与其他模块对接

- 本模块维护详情页面外壳；负责人管理模块优先通过队伍管理页/独立组件提供操作，不同时大幅改详情文件。
- 退出/转交成功前捕获 `sync.sessionRevision`，成功后调用 `afterTeamMutation(recruitmentId, session)`。
- 我的队伍订阅 `conversationsRevision` 和 `sessionRevision`；具体队伍数据订阅 `revisions[id]`，并监听路由 ID。
- 刷新信号不代替数据加载；订阅页面自己重查成员、角色和入口权限，取消旧账号/旧路由请求。
- 联系负责人成功不代表已经发送消息；来源招募上下文和聊天页的传参先与公共聊天负责人协调。
- 不修改公共 Token、401 或通知计数算法，不手动加减队伍人数或未读数。

## 交付验收

- 游客访问受限页跳登录；普通成员、负责人、非成员看到的操作不同。
- 我的队伍只显示有权访问的已成队队伍；空数据、单条失败、列表失败可区分并重试。
- 退出申请提交后仍可进群；批准/移除后失去群权限；拒绝后仍留队且可再次申请。
- 继任者接受后 ownerId、角色、原负责人去留与服务端结果一致；重复操作正确处理 409。
- 联系负责人重复进入同一私聊，不能联系自己；未注册页面没有无效导航。
- 所有页面有加载、空数据、失败、重试和无权限状态，写请求失败不假成功。
- 运行 type-check、lint:check、format:check、build，与管理及聊天模块完成三账号真实联调。
- 交付时列出已注册页面、真实接口、临时入口限制和验证结果。
