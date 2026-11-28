# 项目任务交接

2026 年 11 月 28 日，基于主分支 `e79825d` 开发，交付分支为 `feat/backend-project-tasks`。

## 已有项目基础

确认成队在同一事务内创建项目、项目成员和队伍群，处理待入队申请及通知。项目与招募通过唯一的 recruitmentId 关联，项目列表、详情和成员查询按登录身份校验队伍及项目成员资格。成员离队后两处资格同步失效，负责人转交同步切换项目负责人和项目角色。

原仓库 28 项测试通过，包含成队建项目的并发唯一性、故障回滚、项目访问权限、成员撤权和负责人转交。真实 MySQL 8.0.18 核验时，项目列表和详情因使用保留字 member 作为别名报错。替换别名后，两处查询均通过。成队建项目、队伍群消息及单条和全部通知已读均通过实际请求验证。

## 接口与权限

| 请求 | 成功响应 | 权限 |
| --- | --- | --- |
| `GET /api/projects/{projectId}/tasks` | 200，任务分页与统计 | 当前有效成员 |
| `POST /api/projects/{projectId}/tasks` | 201，新任务 | 项目 ADMIN |
| `PATCH /api/tasks/{taskId}` | 200，更新后的任务 | 项目 ADMIN |
| `DELETE /api/tasks/{taskId}` | 200，data 为 null | 项目 ADMIN |
| `PATCH /api/tasks/{taskId}/status` | 200，更新后的任务 | 项目 ADMIN，或该任务当前有效的负责人 |

统一使用 `Authorization: Bearer <token>`。项目成员和关联队伍成员均为 ACTIVE 时才有访问权限，退出、移除和继任时实时使用当前成员状态及角色。项目 ADMIN 权限来自 project_member.project_role。普通成员可查看项目任务，只能改自己负责的任务状态。任务缺少负责人时仅 ADMIN 可改变其状态。

40100 表示未登录，40300 表示缺少当前项目权限，40400 表示项目或任务不存在。参数非法或指派给无效成员时返回 40000。

## 任务对象与写入

`data` 中单条任务包含 id、projectId、title、description、creatorId、creatorDisplayName、assigneeId、assigneeDisplayName、status、priority、dueAt、completedAt、createdAt 和 updatedAt。ID 为数字，未指派时 assigneeId 与 assigneeDisplayName 为空。时间使用不带偏移量的 ISO 本地日期时间，例如 `2026-12-09T12:00:00`。

创建请求只需任务表单字段，projectId 从路径取得，creatorId 从登录身份取得。标题不能为空，请求中的标题最长 150 字符，写入时去除首尾空白。描述最多 2000 字符。创建状态为 TODO，优先级默认 MEDIUM，允许 LOW、MEDIUM、HIGH。负责人和截止时间可空，指派时负责人必须是该项目当前有效成员，截止时间年份范围为 1000 至 9999。

```json
{
  "title": "整理答辩材料",
  "description": "补齐项目演示步骤",
  "assigneeId": 12,
  "priority": "HIGH",
  "dueAt": "2026-12-09T12:00:00"
}
```

编辑接口接受相同的五个业务字段，省略字段则保留当前值。将 description、assigneeId 或 dueAt 设为 null 可清空对应字段，title 和 priority 提交时必须有效。已离队成员的历史指派可查看，重新分派须选择当前有效成员或取消指派。

状态更新通过独立接口提交 `{"status":"DONE"}`。状态支持 TODO、IN_PROGRESS、DONE。进入 DONE 时记录 completedAt，重复提交 DONE 保留原完成时间，改为其他状态时清空。编辑任务内容时不改变任务状态。

## 分页与统计

| 参数 | 规则 |
| --- | --- |
| status | TODO、IN_PROGRESS 或 DONE |
| assigneeId | 正整数，按负责人用户 ID 匹配 |
| priority | LOW、MEDIUM 或 HIGH |
| dueFrom、dueTo | 不带偏移量的 ISO 本地日期时间，含边界，同时传入时起点须不晚于终点 |
| page | 从 1 开始，默认 1 |
| pageSize | 默认 20，允许 1 至 100 |

筛选条件取交集，按创建时间和 ID 降序返回。设置截止时间范围后，未设置截止的任务不参与筛选。total 与 statistics 按全部筛选条件计算，包含 status 条件，计算范围为分页前的所有命中任务。无命中时返回以下 data。超出最后一页时 records 为空，total 和 statistics 仍反映命中数据。

```json
{
  "records": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "statistics": { "todo": 0, "inProgress": 0, "done": 0 }
}
```

## 数据库升级

已运行项目基础层迁移的招募库选择实际目标数据库后，执行任务迁移。脚本可重复运行并保留已有项目及任务。新建开发库可执行完整初始化脚本。[增量迁移](../database/migrations/20261128-project-task.sql) · [后端启动说明](../backend/README.md)

```sql
USE teamflow_recruitment;
SOURCE database/migrations/20261128-project-task.sql;
```

project_task 复用 project_member 的项目与用户组合唯一键校验所属关系。成员退出后保留任务及原指派记录，服务端重新校验当前角色和可指派资格。任务写入先锁关联招募，使退出、移除、继任和任务操作按同一顺序执行。状态与完成时间在一次更新中提交。

## 验证结果

本地 Maven `verify` 共 50 项测试通过，失败、错误和跳过均为零。其中 22 项新增测试覆盖任务接口、权限、双成员资格、筛选统计、完成时间以及与移除、退出、继任和删除的并发操作。按此前交付约定，新增测试及配套 H2 schema 留在本地。

拟提交版本的 28 项原有测试与 Maven `verify` 打包均通过。真实 MySQL 8.0.18 中，已成队项目迁移后仍可查询并使用任务接口。验证了管理与指派、普通成员权限、完成时间、取消指派、分页筛选与统计、成员移除及继任后的权限，以及删除后的 404。指派与成员移除并发时，两次操作按先后顺序完成，移除后原任务负责人失去访问资格。

独立测试库连续执行完整初始化两次，均成功生成 22 张表。已有任务后再次执行增量迁移，两条任务记录逐行比对一致。

## 待前端联调

- 在项目空间 `project-tasks` 页签接入任务列表、筛选、分页和状态统计，筛选变化时回到第一页。
- 通过现有项目成员接口加载负责人选项，使用成员响应的 id。提交表单时允许将 assigneeId 和 dueAt 设为 null。
- 使用项目详情的 currentUserRole 控制操作入口，普通成员仅显示本人负责的任务状态入口。成员资格变化后刷新项目与任务数据，根据接口处理 403。
- 核对加载、空列表、失败重试、删除确认、末页回退及不同项目间的请求隔离。当前远端前端分支的任务页为占位页。
