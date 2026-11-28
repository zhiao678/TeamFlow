# 项目任务交接

## 开发起点

本周公共分支为 `feat/frontend/space-and-navi`。开始前确认开发分支已包含 `d5febe7` 和 `816475d`。入口为项目详情里的「任务」页签，路由名称是 `project-tasks`，路径为 `/projects/:projectId/tasks`。当前路由挂载 `ProjectTasksEntryView.vue`，页面只展示筹备状态。

新版任务 API 已随远端 `main` 的 `0281555` 合入本分支，可着手替换占位页和联调。已有开发库需先执行任务表迁移。完整契约见 [后端任务交接](../../docs/project-tasks-handoff.md) · [数据库迁移](../../database/migrations/20261128-project-task.sql)。

## 与项目空间对接

- 从详情容器传入的 `project` 读取 `project.id` 和 `project.currentUserRole`。保留原路由名称、路径和登录保护。
- 复用项目空间提供的 `getProjectMembers(projectId, signal?)` 和成员类型。负责人选项来自有效成员，选项值为成员响应的 `id`，显示名为 `displayName`。成员请求失败时提示并提供重试。
- 本模块维护任务页、任务类型及新版任务接口封装。项目详情容器和成员接口由项目空间负责，约定见 [项目空间交接](project-space-handoff.md)。

## 已实现的后端接口

| 请求                                   | 用途                       |
| -------------------------------------- | -------------------------- |
| `GET /api/projects/{projectId}/tasks`  | 查询任务、总数及各状态统计 |
| `POST /api/projects/{projectId}/tasks` | 创建并分配任务             |
| `PATCH /api/tasks/{taskId}`            | 编辑任务信息或重新分配     |
| `DELETE /api/tasks/{taskId}`           | 删除任务                   |
| `PATCH /api/tasks/{taskId}/status`     | 更新任务状态               |

- 列表参数为 `status`、`assigneeId`、`priority`、`dueFrom`、`dueTo`、`page`、`pageSize`。页码从 1 开始，每页默认 20 条，范围为 1–100 条。截止时间范围含边界，使用无时区偏移的 ISO 本地日期时间。
- 列表返回 `{ records, page, pageSize, total, statistics }`。`statistics` 包含 `todo`、`inProgress`、`done`，与 `total` 共同按全部筛选条件在分页前计算，包含 `status` 筛选。
- 创建与编辑使用 `title`、`description`、`assigneeId`、`priority`、`dueAt`。`projectId` 取自路径，`creatorId` 由登录身份确定。编辑时省略字段保留原值，传 `null` 可清空描述、负责人或截止时间。
- 状态支持 `TODO`、`IN_PROGRESS`、`DONE`，通过状态接口单独更新。优先级支持 `LOW`、`MEDIUM`、`HIGH`，创建默认 `MEDIUM`。删除成功返回 `data: null`。

## 待完成页面和操作

1. 列表支持按状态、负责人、优先级和截止时间筛选，并支持服务端分页。筛选变化回到首页。任务总数和状态统计按服务端返回展示，空项目显示空列表和零统计。
2. 项目 `ADMIN` 可创建、编辑、删除和分配任务。普通成员只看得到分配给自己的任务的状态修改操作，仍可按服务端权限查看项目任务列表。最终权限以接口响应为准。
3. 提交时锁定对应按钮，删除前确认。成功后刷新当前分页和统计，删除最后一页最后一条时回到仍有效的页码。失败时保留输入或原状态，再给出反馈。
4. 区分加载、空数据、失败、重试和无权限。复用现有 Axios 和 `401` 登录逻辑。对 `400` 提示参数错误或指派对象失效，对 `403` 处理成员撤权或角色变化，对 `404` 提示项目或任务已不存在。网络失败后可重试读取。

## 验收

- 负责人能创建、编辑、分配和删除任务。负责人下拉框与项目成员一致，检查创建请求体仅包含约定字段。
- 普通成员能修改本人任务状态，不能修改他人任务或调用管理操作。退出或被移除的成员失去项目任务访问权限。
- 验证筛选、分页、总数、状态统计、零任务、失败重试及快速重复提交。检查切换项目后前一个项目的响应不会覆盖当前数据。
- 验证取消指派、清空截止时间、旧指派成员离队后重新分派，以及带状态筛选时统计的口径。确认任务只从对应项目空间访问。
- 在 `frontend` 下运行 `npm run type-check`、`npm run lint:check`、`npm run format:check` 和 `npm run build`，并用负责人、普通成员和已退出或被移除成员完成联调。
