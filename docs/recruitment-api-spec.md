# 大学生组队招募平台 API 规范

本文档描述新版招募平台的第一批接口。它基于 `recruitment-decisions.md` 和 `recruitment-er.md`，用于前后端共同开发。

当前只定义核心组队流程：用户、个人资料、招募、申请、成员和成队。聊天、通知、举报和社区接口在核心流程确认后继续补充。

## 1. 基础约定

```text
开发地址：http://localhost:8080
接口前缀：/api
Content-Type：application/json
```

前端通过 Axios 访问 `/api`，不在页面中写死完整后端地址。

字段命名规则：

- JSON 使用小驼峰，例如 `categoryId`、`ownerId`、`deadlineAt`。
- 数据库使用下划线，例如 `category_id`、`owner_id`、`deadline_at`。
- ID 使用数字。
- 时间格式为 `yyyy-MM-dd'T'HH:mm:ss`。

## 2. 认证和统一响应

### 2.1 认证

登录成功后返回 Token，后续需要登录的接口携带：

```text
Authorization: Bearer <token>
```

服务端从 Token 获取当前用户 ID，不信任请求体中的 `ownerId`、`applicantId` 或 `userId`。

### 2.2 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 2.3 错误响应

```json
{
  "code": 40000,
  "message": "invalid request",
  "data": null
}
```

错误码：

| HTTP 状态 | 业务码 | 含义 |
|---|---:|---|
| `400 Bad Request` | `40000` | 参数或业务规则错误 |
| `401 Unauthorized` | `40100` | 未登录或 Token 无效 |
| `403 Forbidden` | `40300` | 无权操作 |
| `404 Not Found` | `40400` | 数据不存在 |
| `409 Conflict` | `40900` | 状态或并发冲突 |
| `500 Internal Server Error` | `50000` | 服务器内部错误 |

## 3. 用户和个人资料

### 3.1 注册

```http
POST /api/auth/register
Content-Type: application/json
```

请求体：

```json
{
  "username": "alice",
  "password": "password123",
  "displayName": "Alice"
}
```

规则：

- `username` 必填且唯一。
- `password` 必填，服务端使用 BCrypt 哈希保存。
- 不保存明文密码。
- 新用户默认平台角色为 `USER`。

成功状态：`201 Created`。

### 3.2 登录

```http
POST /api/auth/login
Content-Type: application/json
```

请求体：

```json
{
  "username": "alice",
  "password": "password123"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "<token>",
    "user": {
      "id": 1,
      "username": "alice",
      "displayName": "Alice"
    }
  }
}
```

### 3.3 当前用户

```http
GET /api/users/me
```

需要登录。返回当前用户和公开资料：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice",
    "displayName": "Alice",
    "avatarUrl": null,
    "department": "计算机学院",
    "grade": "2025级",
    "bio": "喜欢前端开发",
    "skills": ["Vue", "TypeScript"]
  }
}
```

### 3.4 修改当前用户资料

```http
PATCH /api/users/me
Content-Type: application/json
```

需要登录。`username` 不允许修改，`displayName`、头像、院系、年级、简介和技能可以修改。

## 4. 招募数据结构

招募状态：

```text
DRAFT
RECRUITING
TEAMED
CLOSED
```

招募响应对象：

| 字段 | 类型 | 可为空 | 说明 |
|---|---|---|---|
| `id` | number | 否 | 招募 ID |
| `ownerId` | number | 否 | 负责人 ID |
| `ownerDisplayName` | string | 否 | 负责人昵称 |
| `categoryId` | number | 否 | 分类 ID |
| `categoryName` | string | 否 | 分类名称 |
| `title` | string | 否 | 招募标题 |
| `courseName` | string | 是 | 课程名称 |
| `teacherName` | string | 是 | 教师名称 |
| `purpose` | string | 否 | 招募目的或活动名称 |
| `description` | string | 是 | 详细说明 |
| `requirements` | string | 是 | 招募要求 |
| `maxMembers` | number | 否 | 总人数上限，包含负责人 |
| `currentMembers` | number | 否 | 当前正式成员数量 |
| `remainingSlots` | number | 否 | 剩余名额 |
| `deadlineAt` | string | 是 | 截止时间 |
| `status` | string | 否 | 招募状态 |
| `tags` | array | 否 | 技能标签 |
| `createdAt` | string | 否 | 创建时间 |
| `updatedAt` | string | 否 | 更新时间 |

截止时间到达后不新增状态，前端显示“已截止”，服务端禁止提交新申请。

## 5. 招募大厅和详情

### 5.1 招募列表

```http
GET /api/recruitments
```

需要登录或允许游客访问，最终权限以团队决定为准。当前建议公开招募列表允许游客浏览。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 搜索标题、目的、课程、教师 |
| `categoryId` | number | 否 | 分类筛选 |
| `teacherName` | string | 否 | 教师名称模糊匹配 |
| `courseName` | string | 否 | 课程名称模糊匹配 |
| `tagIds` | string | 否 | 逗号分隔的技能标签 ID |
| `minRemainingSlots` | number | 否 | 最少剩余名额 |
| `status` | string | 否 | 默认 `RECRUITING` |
| `page` | number | 否 | 默认 1 |
| `pageSize` | number | 否 | 默认 20，最大 100 |
| `sort` | string | 否 | `LATEST`、`DEADLINE`、`POPULAR` |

响应使用分页结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "pageSize": 20,
    "total": 0
  }
}
```

### 5.2 招募详情

```http
GET /api/recruitments/{id}
```

返回招募完整信息、正式成员列表、申请权限和当前用户是否已收藏/申请/加入。

### 5.3 创建招募

```http
POST /api/recruitments
Content-Type: application/json
```

需要登录。请求体：

```json
{
  "categoryId": 1,
  "title": "寻找 Java 课程汇报队友",
  "courseName": "Java 程序设计",
  "teacherName": "张老师",
  "purpose": "完成课程汇报",
  "description": "已有两名成员，希望寻找一名队友",
  "requirements": "熟悉 Java 或者愿意负责材料整理",
  "maxMembers": 3,
  "deadlineAt": "2026-11-15T18:00:00",
  "tagIds": [1, 2]
}
```

规则：

- 负责人从当前登录用户获取。
- 创建时状态为 `DRAFT` 或直接为 `RECRUITING`，由前端操作决定。
- 负责人自动成为第一名 `LEADER` 成员。
- `maxMembers` 至少为 2，不能小于当前成员数。

成功状态：`201 Created`。

### 5.4 编辑招募

```http
PATCH /api/recruitments/{id}
Content-Type: application/json
```

只有负责人可以编辑。`TEAMED` 和 `CLOSED` 招募不能编辑核心内容；人数上限不能小于当前正式成员数。

### 5.5 发布招募

```http
POST /api/recruitments/{id}/publish
```

只有负责人可以操作。草稿发布后进入 `RECRUITING`，发布前校验必填字段。

### 5.6 关闭招募

```http
POST /api/recruitments/{id}/close
Content-Type: application/json
```

只有负责人可以操作。可选请求体：

```json
{
  "reason": "已经通过其他渠道完成组队"
}
```

关闭后不再接受申请，状态变为 `CLOSED`。

## 6. 申请和成员

申请状态：

```text
PENDING
ACCEPTED
REJECTED
WITHDRAWN
EXPIRED
```

成员角色：

```text
LEADER
MEMBER
```

### 6.1 提交申请

```http
POST /api/recruitments/{id}/applications
Content-Type: application/json
```

需要登录。申请人从当前登录用户获取，请求体：

```json
{
  "introduction": "我有 Java 开发经验",
  "skillsDescription": "熟悉 Java 和文档整理",
  "availableTime": "工作日晚 7 点后"
}
```

规则：

- 招募必须处于 `RECRUITING` 且未截止。
- 当前用户不能是该招募的正式成员。
- 同一用户最多存在一条 `PENDING` 申请。
- `PENDING` 申请不占用正式名额。

成功状态：`201 Created`。

### 6.2 查询招募申请

```http
GET /api/recruitments/{id}/applications
```

只有招募负责人可以查看全部申请。申请人只能查看自己的申请状态。

### 6.3 接受申请

```http
PATCH /api/applications/{id}/accept
```

只有负责人可以操作。服务端必须在事务中重新检查招募状态、申请状态和剩余名额；接受最后一个名额时只能有一个请求成功。

成功后：

- 申请变为 `ACCEPTED`。
- 创建 `team_member` 记录。
- 当前成员数加一。
- 发送通知。

### 6.4 拒绝申请

```http
PATCH /api/applications/{id}/reject
Content-Type: application/json
```

请求体可包含：

```json
{
  "reason": "当前队伍方向不匹配"
}
```

申请变为 `REJECTED`，并通知申请人。

### 6.5 撤回申请

```http
PATCH /api/applications/{id}/withdraw
```

只有申请人可以撤回自己的 `PENDING` 申请。撤回后状态变为 `WITHDRAWN`，后续允许重新提交申请。

### 6.6 查询成员

```http
GET /api/recruitments/{id}/members
```

公开招募详情可以返回公开成员信息；完整成员管理信息仅对负责人和正式成员开放。

### 6.7 确认成队

```http
POST /api/recruitments/{id}/form-team
```

只有负责人可以操作。允许未满员成队。服务端在同一事务中：

- 将招募状态改为 `TEAMED`。
- 将剩余 `PENDING` 申请改为 `EXPIRED`。
- 创建唯一的队伍群聊。
- 发送相关通知。

## 7. 当前不在本文件中冻结的接口

以下接口等核心招募流程确认后补充：

```text
GET  /api/conversations
POST /api/conversations/private
GET  /api/conversations/{id}/messages
POST /api/conversations/{id}/messages

GET   /api/notifications
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all

POST  /api/reports
GET   /api/admin/reports
PATCH /api/admin/reports/{id}
```

这些接口不能改变本文件中已确认的用户、招募、申请、成员和成队规则。

## 8. 第一批实现顺序

1. 用户注册、登录和当前用户。
2. 用户资料和技能标签。
3. 分类和招募 CRUD。
4. 招募列表筛选和详情。
5. 申请提交、撤回、接受和拒绝。
6. 成员查询和确认成队。
7. 使用演示数据和自动化测试验证核心流程。
