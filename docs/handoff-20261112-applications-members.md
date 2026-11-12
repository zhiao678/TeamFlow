# 申请与成员模块交接说明

更新日期为 2026 年 11 月 12 日，开发负责人为李锐。本轮从主分支 `6104bc2` 继续开发，交付分支为 `feat/backend-application-members`。

## 一、完成范围

复用已有 JWT、Controller / Service / Mapper、DTO / VO、统一响应及 `NotificationService.create(...)`，完成入队申请、正式成员管理、成员退出审核和负责人继任退出。职责及工作内容交接通过 `handoverHint` 提供简短沟通提示。

### 入队申请与正式成员

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/recruitments/{id}/applications` | 当前登录用户提交入队申请 |
| GET | `/api/recruitments/{id}/applications` | 当前负责人查询招募申请 |
| GET | `/api/applications/me` | 查询本人全部入队申请 |
| PATCH | `/api/applications/{id}/withdraw` | 撤回本人的待处理申请 |
| PATCH | `/api/applications/{id}/accept` | 负责人接受申请并加入正式成员 |
| PATCH | `/api/applications/{id}/reject` | 负责人拒绝申请，可选拒绝原因 |
| GET | `/api/recruitments/{id}/members` | 查询 ACTIVE 成员的公开信息 |
| DELETE | `/api/recruitments/{id}/members/{userId}` | 负责人移除其他正式成员 |

### 退出审核与负责人继任

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/recruitments/{id}/exit-requests` | 普通成员申请退出 |
| GET | `/api/recruitments/{id}/exit-requests` | 当前负责人查看退出申请 |
| GET | `/api/member-exit-requests/me` | 查询本人的退出申请 |
| PATCH | `/api/member-exit-requests/{id}/approve` | 负责人批准退出 |
| PATCH | `/api/member-exit-requests/{id}/reject` | 负责人拒绝退出，可选拒绝原因 |
| POST | `/api/recruitments/{id}/leader-exit-requests` | 负责人指定继任者并申请退出 |
| GET | `/api/leader-transfers/me` | 查询本人发起或收到的继任记录 |
| PATCH | `/api/leader-transfers/{id}/accept` | 指定继任者确认接任 |
| PATCH | `/api/leader-transfers/{id}/reject` | 指定继任者拒绝接任 |

请求示例、字段长度、返回对象、登录和权限规则详见 API 规范第 6 节。[招募 API 规范](recruitment-api-spec.md)

## 二、关键业务规则

- 入队申请仅允许在 RECRUITING、未截止且有空位时提交。负责人及 ACTIVE 正式成员不能申请，同一用户同一招募至多一条 PENDING 申请。
- 负责人接受申请后才写入正式成员并占用名额，接受前再次检查状态和人数。截止前已提交的申请在截止后仍可审核。
- 撤回、拒绝后可重新申请。接受已退出或被移除的历史成员时复用原关系，恢复 ACTIVE / MEMBER 并清除 leftAt。
- 普通成员退出须经当前负责人批准。提交退出申请后继续保持 ACTIVE 并计入人数，批准后转为 LEFT、记录 leftAt 并撤销对应队伍群资格。
- 负责人可以将其他正式成员移除为 REMOVED，自行调用删除成员接口会提示使用退出审批或继任流程。成员历史关系保留。
- 负责人先选择本队其他 ACTIVE 普通成员，对方确认前保留原负责人权限和成员身份。确认后在同一事务中完成角色变更、ownerId 更新及原负责人离队。
- 新负责人退出记录写入 exitAfterTransfer=true。历史转交记录默认 false，确认后原负责人作为普通成员继续留队。
- 移除成员或批准其退出时，关联待确认继任记录失效。继任者确认接任时，其待处理普通退出申请失效，避免旧申请影响新身份。
- 关闭招募会将 PENDING 入队申请转为 EXPIRED 并通知申请人。现有成员关系继续保留，成员退出审核和负责人继任仍可办理。
- 所有名额和成员写操作先锁招募行。申请、成员、退出和继任状态与通知共同提交，通知失败时业务数据整体回滚。

入队及退出申请创建成功返回 HTTP 201。列表返回数组，空结果为 `[]`。权限错误使用 40300，对象缺失使用 40400，重复待处理、重复终态操作、满员和截止等冲突使用 40900。未再满足有效成员条件的待处理退出或继任记录，在审核时可返回 EXPIRED 对象并提交失效状态。[业务规则](recruitment-decisions.md)

## 三、通知接入

| 事件 | 接收者 | 目标 |
| --- | --- | --- |
| APPLICATION_SUBMITTED | 当前招募负责人 | APPLICATION / applicationId |
| APPLICATION_ACCEPTED、APPLICATION_REJECTED | 申请人 | APPLICATION / applicationId |
| APPLICATION_WITHDRAWN | 当前招募负责人 | APPLICATION / applicationId |
| APPLICATION_EXPIRED | 申请人 | APPLICATION / applicationId |
| MEMBER_EXIT_REQUESTED | 当前招募负责人 | RECRUITMENT / recruitmentId |
| MEMBER_EXIT_APPROVED、MEMBER_EXIT_REJECTED | 退出申请人 | RECRUITMENT / recruitmentId |
| LEADER_TRANSFER_REQUESTED | 指定继任者 | LEADER_TRANSFER / leaderTransferId |
| LEADER_TRANSFER_ACCEPTED、LEADER_TRANSFER_REJECTED | 原负责人 | LEADER_TRANSFER / leaderTransferId |

事件键分别采用 `application:{id}:{event}`、`member-exit:{id}:{event}` 和 `leader-transfer:{id}:{event}`，事件后缀为小写。同一接收者与事件键唯一，重试创建通知保持幂等。

通知服务保留已有 create 签名，并增加支持 leaderTransferId 的重载。通知返回对象新增可空 leaderTransferId，前端按 targetType 读取对应目标。关联失效通过 EXPIRED 业务状态展示。

## 四、数据库升级

完整初始化更新为 19 张表。已有招募数据库执行以下增量升级，先在 MySQL 客户端选择实际目标数据库，再从仓库根目录执行脚本。

```sql
USE teamflow_recruitment;
SOURCE database/migrations/20261112-application-member-exit.sql;
```

升级内容包括 application.rejection_reason、leader_transfer.exit_after_transfer、member_exit_request 表及退出审核通知枚举。脚本支持重复执行并保留现有业务数据，新建开发库可使用完整初始化脚本。[增量升级 SQL](../database/migrations/20261112-application-member-exit.sql) · [完整 ER 与约束](recruitment-er.md)

## 五、验证结果与提交范围

本地完整验证版本共 89 项测试通过，失败、错误和跳过均为 0，其中本轮新增 46 项。覆盖重复和越权申请、撤回、截止和满员、最后名额竞争、接受与撤回或关闭争用、成员人数与名额、退出审核、继任确认、历史转交兼容、通知幂等及通知失败后的事务回滚。

真实 MySQL 8.0.18 验证通过。最后名额竞争、重复批准退出、重复确认继任均仅有一次成功，另一请求返回 409。通知目标、撤回接收者、负责人权限切换、关闭失效和截止规则已通过实际 HTTP 请求验证。

数据库升级从上一版结构执行两次后保持 19 表、3 个种子用户、2 条招募和 2 条成员关系，数据数量与升级前一致。

按已确认的提交范围，本轮生产代码、SQL 和正式文档进入交付，新增测试与测试 schema 保留在开发本地。89 项结果对应包含本地测试扩展的完整验证版本。

按实际提交文件独立导出后，Maven `verify` 的 43 项仓库原有测试全部通过，并完成可运行 JAR 打包。新增申请、退出与继任测试使用配套的本地扩展 H2 表结构。

## 六、前端待联调

- 接入招募详情申请表单、我的申请及负责人审核列表，核对 201 响应、状态和 409 错误提示。
- 接入普通成员退出申请及负责人批准、拒绝页面，审核前继续展示成员资格和原人数。
- 接入负责人选择继任者、继任者接受或拒绝页面。确认成功后刷新 ownerId、负责人管理入口、正式成员数和剩余名额。
- 接入通知跳转，支持 APPLICATION、RECRUITMENT 和 LEADER_TRANSFER 的目标字段。
- 展示 handoverHint，提示双方沟通职责与工作内容。

## 七、后续开发与小组讨论

- “我的招募”沿用当前 ownerId 查询口径，独立的“我创建的招募”历史展示待确认。
- 队伍仅剩负责人时的结束或解散方式，以及待确认继任邀请的撤销、超时规则，待小组确定。
- 确认成队、自动建群及聊天按后续阶段推进，成队时需将申请失效、唯一群聊和通知纳入同一事务。
