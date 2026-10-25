# TeamFlow API 规范

本文档是 TeamFlow 前后端共同遵守的接口契约。接口字段、枚举或响应结构发生变化时，应在同一个提交中同步更新后端代码、前端类型和本文档。

## 1. 实现状态

| 模块 | 接口 | 状态 |
|---|---|---|
| 项目 | `GET /api/projects` | 已实现 |
| 项目 | `GET /api/projects/{id}` | 已实现 |
| 项目 | `POST /api/projects` | 已实现 |
| 任务 | `GET /api/projects/{projectId}/tasks` | 待实现 |
| 任务 | `POST /api/projects/{projectId}/tasks` | 待实现 |
| 任务 | `PATCH /api/tasks/{taskId}/status` | 待实现 |

## 2. 基础约定

### 2.1 开发地址

```text
后端地址：http://localhost:8080
接口前缀：/api
Content-Type：application/json
```

前端统一通过 Axios 访问 `/api`，由 Vite 将请求代理到后端。页面和业务接口代码中不得写死完整后端地址。

### 2.2 JSON 命名

请求和响应字段统一使用小驼峰：

```text
projectId
ownerId
creatorId
assigneeId
dueAt
completedAt
createdAt
updatedAt
```

数据库字段使用下划线：

```text
project_id
owner_id
creator_id
assignee_id
due_at
completed_at
created_at
updated_at
```

后端通过 MyBatis-Plus 自动完成下划线与小驼峰转换。

### 2.3 ID 和时间

- ID 使用 JSON 数字，对应后端 `Long` 和数据库 `BIGINT`。
- 日期时间使用 `yyyy-MM-dd'T'HH:mm:ss`，不携带时区偏移。
- 当前开发环境按 `Asia/Shanghai` 时区解释日期时间。

示例：

```text
2026-11-15T18:00:00
```

### 2.4 枚举

前后端传输英文枚举值，前端负责显示中文文案。

| 类型 | 值 | 中文显示 |
|---|---|---|
| 项目状态 | `ACTIVE` | 进行中 |
| 项目状态 | `ARCHIVED` | 已归档 |
| 项目角色 | `ADMIN` | 项目管理员 |
| 项目角色 | `MEMBER` | 项目成员 |
| 任务状态 | `TODO` | 待处理 |
| 任务状态 | `IN_PROGRESS` | 进行中 |
| 任务状态 | `DONE` | 已完成 |
| 任务优先级 | `LOW` | 低 |
| 任务优先级 | `MEDIUM` | 中 |
| 任务优先级 | `HIGH` | 高 |

### 2.5 空值和默认值

| 字段 | 规则 |
|---|---|
| 项目名称 `name` | 必填，不能为空，最长 100 个字符 |
| 任务标题 `title` | 必填，不能为空，最长 200 个字符 |
| `ownerId` | 当前未接入登录，创建项目时必填 |
| `creatorId` | 当前未接入登录，创建任务时必填 |
| `assigneeId` | 可省略或传 `null`，表示暂未分配 |
| `description` | 可省略或传 `null` |
| `priority` | 可省略，默认 `MEDIUM` |
| `dueAt` | 可省略或传 `null` |
| `completedAt` | 由后端维护，不由创建任务请求传入 |

### 2.6 当前身份约定

当前阶段尚未实现登录和 Token：

- 创建项目时由请求体临时传递 `ownerId`。
- 创建任务时由请求体临时传递 `creatorId`。
- 接入登录后，以上用户 ID 改为从当前登录身份获取，并从请求体删除。
- 当前 `GET /api/projects` 返回全部项目；接入登录后再改为当前用户参与的项目。

## 3. 统一响应

所有接口均返回统一 JSON 结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- `code` 是业务码，不等同于 HTTP 状态码。
- 成功时 `code` 固定为 `0`。
- 没有返回数据或请求失败时，`data` 为 `null`。

### 3.1 错误码

| HTTP 状态 | 业务码 | 含义 |
|---|---:|---|
| `400 Bad Request` | `40000` | 请求体、路径参数或字段校验失败 |
| `404 Not Found` | `40400` | 项目、任务、用户等数据不存在 |
| `500 Internal Server Error` | `50000` | 未处理的服务器内部错误 |

参数错误示例：

```json
{
  "code": 40000,
  "message": "name: must not be blank",
  "data": null
}
```

数据不存在示例：

```json
{
  "code": 40400,
  "message": "project not found",
  "data": null
}
```

## 4. 项目数据结构

项目响应对象：

| 字段 | 类型 | 可为空 | 说明 |
|---|---|---|---|
| `id` | number | 否 | 项目 ID |
| `name` | string | 否 | 项目名称 |
| `description` | string | 是 | 项目描述 |
| `ownerId` | number | 否 | 项目创建者 ID |
| `ownerDisplayName` | string | 否 | 项目创建者显示名称 |
| `status` | string | 否 | `ACTIVE` 或 `ARCHIVED` |
| `createdAt` | string | 否 | 创建时间 |
| `updatedAt` | string | 否 | 更新时间 |

### 4.1 查询项目列表

```http
GET /api/projects
```

成功状态：`200 OK`

当前不分页，`data` 直接返回数组，并按创建时间倒序排列。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "TeamFlow",
      "description": "团队项目与任务协同管理系统",
      "ownerId": 1,
      "ownerDisplayName": "Alice",
      "status": "ACTIVE",
      "createdAt": "2026-10-22T20:03:18",
      "updatedAt": "2026-10-22T20:03:18"
    }
  ]
}
```

### 4.2 查询项目详情

```http
GET /api/projects/{id}
```

路径参数：

| 参数 | 类型 | 规则 |
|---|---|---|
| `id` | number | 必须大于 0 |

成功状态：`200 OK`

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "TeamFlow",
    "description": "团队项目与任务协同管理系统",
    "ownerId": 1,
    "ownerDisplayName": "Alice",
    "status": "ACTIVE",
    "createdAt": "2026-10-22T20:03:18",
    "updatedAt": "2026-10-22T20:03:18"
  }
}
```

项目不存在时返回 `404 / 40400`。

### 4.3 创建项目

```http
POST /api/projects
Content-Type: application/json
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 非空，最长 100 个字符 |
| `description` | string | 否 | 项目描述 |
| `ownerId` | number | 是 | 当前阶段临时传递，必须是已存在用户 |

请求示例：

```json
{
  "name": "中级实训协同平台",
  "description": "团队项目与任务协同管理系统",
  "ownerId": 1
}
```

成功状态：`201 Created`

成功响应的 `data` 为完整项目响应对象。创建成功时，后端必须在同一事务中把创建者插入 `project_member`，并将项目角色设置为 `ADMIN`。

失败情况：

- 请求字段不合法：`400 / 40000`。
- `ownerId` 对应用户不存在：`404 / 40400`，消息为 `owner user not found`。

## 5. 任务数据结构

本节是前后端已确认、后端待实现的任务接口契约。

任务响应对象：

| 字段 | 类型 | 可为空 | 说明 |
|---|---|---|---|
| `id` | number | 否 | 任务 ID |
| `projectId` | number | 否 | 当前接口只处理项目任务 |
| `creatorId` | number | 否 | 创建者 ID |
| `assigneeId` | number | 是 | 负责人 ID，`null` 表示未分配 |
| `title` | string | 否 | 任务标题 |
| `description` | string | 是 | 任务描述 |
| `status` | string | 否 | `TODO`、`IN_PROGRESS` 或 `DONE` |
| `priority` | string | 否 | `LOW`、`MEDIUM` 或 `HIGH` |
| `dueAt` | string | 是 | 截止时间，对应数据库 `due_at` |
| `completedAt` | string | 是 | 完成时间，由后端维护 |
| `createdAt` | string | 否 | 创建时间 |
| `updatedAt` | string | 否 | 更新时间 |

逻辑删除字段 `deleted` 仅供后端和数据库使用，不返回给前端。

### 5.1 查询项目任务列表

```http
GET /api/projects/{projectId}/tasks
```

路径参数：

| 参数 | 类型 | 规则 |
|---|---|---|
| `projectId` | number | 必须大于 0，项目必须存在 |

成功状态：`200 OK`

当前不分页，`data` 直接返回任务数组。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "creatorId": 1,
      "assigneeId": 2,
      "title": "完成登录页面",
      "description": "完成页面布局和表单校验",
      "status": "TODO",
      "priority": "HIGH",
      "dueAt": "2026-11-15T18:00:00",
      "completedAt": null,
      "createdAt": "2026-10-25T10:00:00",
      "updatedAt": "2026-10-25T10:00:00"
    }
  ]
}
```

项目不存在时返回 `404 / 40400`。

### 5.2 创建项目任务

```http
POST /api/projects/{projectId}/tasks
Content-Type: application/json
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | 是 | 非空，最长 200 个字符 |
| `description` | string | 否 | 任务描述 |
| `creatorId` | number | 是 | 当前阶段临时传递，必须是项目成员 |
| `assigneeId` | number | 否 | 为空表示未分配；有值时必须是项目成员 |
| `priority` | string | 否 | 默认 `MEDIUM` |
| `dueAt` | string | 否 | 截止时间，对应数据库 `due_at` |

请求示例：

```json
{
  "title": "完成登录页面",
  "description": "完成页面布局和表单校验",
  "creatorId": 1,
  "assigneeId": 2,
  "priority": "HIGH",
  "dueAt": "2026-11-15T18:00:00"
}
```

成功状态：`201 Created`

新任务默认状态为 `TODO`。成功响应的 `data` 为完整任务响应对象。

失败情况：

- 请求字段或枚举值不合法：`400 / 40000`。
- 项目、创建者或负责人不存在：`404 / 40400`。
- 创建者不是项目成员，或负责人不是项目成员：当前统一按 `400 / 40000` 处理。

### 5.3 修改任务状态

```http
PATCH /api/tasks/{taskId}/status
Content-Type: application/json
```

路径参数：

| 参数 | 类型 | 规则 |
|---|---|---|
| `taskId` | number | 必须大于 0，任务必须存在 |

请求体：

```json
{
  "status": "IN_PROGRESS"
}
```

成功状态：`200 OK`

成功响应的 `data` 为更新后的完整任务响应对象。

状态时间规则：

- 状态更新为 `DONE` 时，后端写入 `completedAt`。
- 状态从 `DONE` 改为其他状态时，后端清空 `completedAt`。

任务不存在时返回 `404 / 40400`，状态值不合法时返回 `400 / 40000`。

## 6. 后续调整原则

- 接入登录后，`ownerId` 和 `creatorId` 不再由前端传递。
- 当前列表接口均不分页；引入分页时需要前后端共同更新响应结构。
- Knife4j 接入后，在线文档必须与本文档保持一致。
- 新增、删除或重命名字段时，必须同时修改数据库、后端 DTO/VO、前端 TypeScript 类型和本文档。
