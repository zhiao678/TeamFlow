# 负责人队伍管理交接

## 已有公共基础

先阅读 [队伍与聊天公共接入约定](team-chat-foundation.md)。公共基础提交为 `98ff7bf`，
本周分支为 `feat/frontend-team-management-chat`。开发分支应包含该提交再接入业务。

- `src/api/team-management.ts` 已封装本模块全部写操作和退出申请查询。
- `src/types/team-management.ts` 提供退出、转交类型及状态文案。
- `src/api/conversations.ts` 提供 getTeamConversation，不重复查全部会话寻找群。
- 成员、招募、申请查询复用现有 API；错误复用 teamErrorStatus/teamErrorMessage。
- `src/stores/team-sync.ts` 提供业务成功后的失效信号与通知未读数刷新。
- 现有“我的招募”、编辑页、申请审核页和成员组件可复用，不删除上周功能。

## 页面与职责

使用 route key `teamManagement`，路径 `/my-recruitments/:id/team-management`，id 为招募 ID。
在“我的招募”和合适的管理区域提供入口；页面只实现公共布局的内容区。
从统一 `createTeamRoutes` 工厂注册页面，不另定义同名或不同参数语义的路由。
本模块主要维护队伍管理页面及独立组件；公开详情外壳由成员侧负责人维护，通过入口/组件接入。

## 待完成操作

### 1. 确认成队

- 仅当前负责人且招募状态为 RECRUITING 时提供“确认成队”。
- 允许未满员、仅负责人一人时成队，不增加后端未要求的满员限制。
- 二次确认明确说明：待处理入队申请将变为 EXPIRED，并创建唯一队伍群。
- 调用 `formTeam(id)`；成功返回招募对象，不是会话对象。
- 成功后刷新招募、申请列表和成员，通过 `getTeamConversation(id)` 获取群聊入口。
- 409 时查询实际状态；可能已经由另一请求完成，不显示本次重复成队成功，不自动重发。
- 若写请求已成功但刷新失败，分别反馈并提供刷新，不让用户误以为需要再次确认。

### 2. 移除正式成员

- 加载正式成员列表，只允许负责人移除其他 ACTIVE 成员，不允许移除自己。
- 二次确认并说明对方会失去队伍群访问资格。
- 调用 `removeTeamMember(recruitmentId, userId)`，第二个参数是成员响应的用户 id，不是关系记录 id。
- 成功后重查成员、人数、名额、退出申请和相关转交状态，不能本地直接人数减一。
- 403/404/409 时保留明确错误并刷新，不把失败当作移除完成。

### 3. 普通成员退出审批

- 使用 `listRecruitmentExits(id)`，展示申请者、原因、状态、时间、拒绝原因及 handoverHint。
- 仅负责人可查询和审批；只对 PENDING 记录提供批准/拒绝。
- 拒绝原因可选，最多 255 字符；操作中防重复点击。
- 批准后成员离队、群资格撤销；拒绝则保留成员，均刷新实际记录。
- 审核可能成功返回 EXPIRED 对象，必须按返回状态显示，不能只因 HTTP 200 就提示“已批准”。
- 已处理、成员状态改变等 409 冲突时刷新列表；不能依据旧申请重新改变已失效的成员资格。

### 4. 发起负责人转交后退出

- 负责人不能走普通成员退出接口，引导选择本队其他 ACTIVE 普通成员作为继任者。
- 候选项来自当前正式成员；人数不足或没有候选人时显示清晰提示，不伪造候选用户。
- 二次确认后调用 `requestLeadershipExit(id, successorId)`，不要自行传 exitAfterTransfer。
- 显示服务端返回的待确认转交和 handoverHint，可复用 `listMyLeaderTransfers()` 查询本人发起记录。
- 继任者确认前原负责人仍保留权限与成员资格，不提前清掉按钮、退出群或更改 ownerId。
- 继任者接受/拒绝的页面由成员侧负责人实现；管理页提供统一“负责人转交”记录入口。
- 接受、拒绝或失效后重查招募与成员；历史 exitAfterTransfer=false 与新 true 记录展示不同提示。

## 封装对照

| 公共方法 | 对应接口 |
| --- | --- |
| formTeam | `POST /api/recruitments/{id}/form-team` |
| removeTeamMember | `DELETE /api/recruitments/{id}/members/{userId}` |
| listRecruitmentExits | `GET /api/recruitments/{id}/exit-requests` |
| approveMemberExit | `PATCH /api/member-exit-requests/{id}/approve` |
| rejectMemberExit | `PATCH /api/member-exit-requests/{id}/reject` |
| requestLeadershipExit | `POST /api/recruitments/{id}/leader-exit-requests` |
| listMyLeaderTransfers | `GET /api/leader-transfers/me` |
| getTeamConversation | `GET /api/recruitments/{id}/team-conversation` |

详情及规则见 [最新 API 规范](../../docs/recruitment-api-spec.md)。使用封装时不重复拼 `/api`。
负责人审批与成员侧复用同一份类型，普通退出成功枚举是 APPROVED，转交接受枚举是 ACCEPTED。

## 与成员侧及公共模块对接

- 对共享 API 的扩展先协调，保留其他模块已有方法和类型，不建立另一套退出或转交结构。
- 请求前捕获 `const session = sync.sessionRevision`，写成功后调用 `afterTeamMutation(id, session)`。
- 按公共文档订阅该招募 revisions 及 sessionRevision，并监听路由 id 重查数据；处理取消和旧响应隔离。
- 409 可使用 `invalidateTeam(id, session)` 广播重新查询，但不广播操作成功。
- 公共信号本身不查询队伍，不代替本页面刷新；页面负责按实际权限加载负责人专属数据。
- 群聊入口只有取到会话、且统一聊天路由已注册时启用；不在管理页面实现群聊窗口或轮询。
- 详情入口和正式成员展示与成员侧协调，避免同时大幅改 RecruitmentDetailView.vue 或成员组件。
- 通知由后端产生，成功后刷新未读数即可，不插入本地模拟通知。
- 保留招募编辑字段限制、已有申请审核和收藏等行为；成队后禁止继续编辑核心招募信息。

## 交付验收

- 负责人、普通成员、申请人三账号分别验证入口和权限，直接访问也正确处理 403/404。
- 未满员确认成队成功，剩余待处理申请失效，唯一队伍群可进入；重复确认返回 409 后刷新。
- 移除成员后人数更新，对方无法继续访问或发送群消息。
- 批准/拒绝退出和 EXPIRED 返回值正确展示；批准后成员资格撤销。
- 发起转交后原负责人仍有效；继任者接受后负责人、成员人数和原负责人去留正确变化。
- 所有写操作有防重复提交；页面有加载、空数据、失败、重试、无权限和状态冲突反馈。
- 运行 type-check、lint:check、format:check、build，配合成员侧和聊天页完成真实联调。
- 交付时列出已实现页面、接口、状态处理及测试结果；不以契约脚本替代完整业务验收。
