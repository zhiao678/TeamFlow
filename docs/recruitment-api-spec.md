# 大学生组队招募平台 API 规范

本文档基于 `recruitment-decisions.md` 和 `recruitment-er.md`，用于前后端共同开发。

截至 2026-11-08，第 3 至 5 节的用户、资料和招募接口已实现。第 6 节是申请、成员和成队的后续接口约定，第 7 节列出尚待补充的接口。

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
- 新用户默认账号状态为 `ACTIVE`。

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
      "displayName": "Alice",
      "platformRole": "USER",
      "accountStatus": "ACTIVE",
      "skills": []
    }
  }
}
```

### 3.3 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

需要登录。首版采用无状态 JWT：接口返回成功后，前端删除本地 Token 和用户状态。后端暂不维护 Token 黑名单，因此旧 Token 在到期前不会被服务端主动撤销。

### 3.4 当前用户

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
    "platformRole": "USER",
    "accountStatus": "ACTIVE",
    "skills": ["Vue", "TypeScript"]
  }
}
```

平台角色：

```text
USER
ADMIN
```

账号状态：

```text
ACTIVE
RESTRICTED
DISABLED
```

### 3.5 修改当前用户资料

```http
PATCH /api/users/me
Content-Type: application/json
```

需要登录。`username`、`platformRole` 和 `accountStatus` 不允许通过该接口修改；`displayName`、头像、院系、年级、简介和技能可以修改。

请求示例：

```json
{
  "displayName": "Alice",
  "avatarUrl": null,
  "department": "计算机学院",
  "grade": "2025级",
  "bio": "喜欢前端开发",
  "tagIds": [1, 2]
}
```

规则：

- 所有字段均为可选；未传字段保持不变。
- `tagIds` 传空数组表示清空技能标签，传 `null` 或不传表示保持不变。
- `tagIds` 最多 20 个，服务端会去重并校验标签存在且已启用。
- 返回更新后的完整当前用户资料。

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
| `closeReason` | string | 是 | 关闭原因 |
| `tags` | string[] | 否 | 技能标签名称，例如 `["Java", "Vue"]` |
| `createdAt` | string | 否 | 创建时间 |
| `updatedAt` | string | 否 | 更新时间 |

截止时间到达后不新增状态，前端显示“已截止”，服务端禁止提交新申请。

## 5. 招募大厅和详情

### 5.1 招募列表

```http
GET /api/recruitments
```

允许游客访问。默认只返回 `RECRUITING`。查询 `TEAMED` 或 `CLOSED` 返回对应公开招募，查询 `DRAFT` 只返回当前登录负责人的草稿，游客查询草稿得到空分页。登录不会向默认大厅混入本人的其他状态招募。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 最长 200 字符，搜索标题、目的、课程、教师的任一字段 |
| `categoryId` | number | 否 | 正整数，分类 ID 精确匹配 |
| `teacherName` | string | 否 | 最长 100 字符，教师名称包含匹配 |
| `courseName` | string | 否 | 最长 100 字符，课程名称包含匹配 |
| `status` | string | 否 | `DRAFT`、`RECRUITING`、`TEAMED`、`CLOSED`，默认 `RECRUITING` |
| `page` | number | 否 | 从 1 开始，默认 1 |
| `pageSize` | number | 否 | 1 至 100，默认 20 |
| `sort` | string | 否 | `LATEST`、`DEADLINE`，默认 `LATEST` |

筛选条件取交集。`keyword`、`courseName` 和 `teacherName` 去除首尾空格，空字符串等同未筛选，`%` 和 `_` 按普通字符匹配。`status` 和 `sort` 须严格使用表中的枚举值。分类不存在时返回空结果。非法分页、状态或排序参数返回 `40000`。

`LATEST` 按创建时间降序、ID 降序排列。`DEADLINE` 按截止时间升序、ID 降序排列，没有截止时间的记录放在最后，已过期记录仍参与排序。截止时间不会自动将招募改为关闭。

技能组合、最少剩余名额、`POPULAR` 和收藏列入 11 月 9 日至 15 日阶段，本周客户端只发送上表参数。

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

允许游客访问，返回第 4 节的招募对象。草稿只允许负责人读取，游客和其他用户访问均返回 `40400`。不存在的招募也返回 `40400`。

成员列表、申请权限和收藏/申请/加入标记随对应模块后续补充。

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
  "description": "希望寻找两名队友",
  "requirements": "熟悉 Java 或者愿意负责材料整理",
  "maxMembers": 3,
  "deadlineAt": "2026-11-15T18:00:00",
  "tagIds": [1, 2],
  "publish": true
}
```

规则：

- 负责人从当前登录用户获取。
- 创建时状态为 `DRAFT` 或直接为 `RECRUITING`，由前端操作决定。
- 负责人自动成为第一名 `LEADER` 成员。
- `maxMembers` 至少为 2，不能小于当前成员数。
- `publish` 默认 `false`，保存为草稿，传 `true` 直接发布。
- 标题最长 150、目的最长 200、课程和教师各最长 100 字符。
- `description` 和 `requirements` 各最长 20000 个 UTF-16 代码单元，更新时使用相同限制。
- `tagIds` 最多 20 个，使用启用的正整数标签 ID，重复 ID 去重保存。
- 分类必须存在且启用，标签必须存在且启用。
- 创建草稿也需要填写标题、分类、目的和人数上限。

成功状态：`201 Created`。

### 5.4 编辑招募

```http
PATCH /api/recruitments/{id}
Content-Type: application/json
```

只有负责人可以编辑。请求字段与创建接口相同，但不接收 `publish`，所有字段均可选。

- `DRAFT` 允许编辑全部业务字段，标题和目的不能改成空白。
- `RECRUITING` 只接受 `description`、`requirements`、`tagIds`、`deadlineAt` 和 `maxMembers`。提交非空的标题、分类、课程、教师或目的字段返回 `40900`，即使字段值与当前值相同也应省略。
- `TEAMED` 和 `CLOSED` 禁止编辑，返回 `40900`。
- 人数上限不能低于当前 `ACTIVE` 成员数量。
- 未传字段和 `null` 保持原值，`tagIds: []` 清空标签，空字符串可清空描述、要求、课程或教师，但课程和教师仅草稿可编辑。当前无法通过 `null` 清除截止时间。
- 返回更新后的第 4 节招募对象。

### 5.5 发布招募

```http
POST /api/recruitments/{id}/publish
```

只有负责人可以操作，必须处于 `DRAFT`。发布时重新校验非空白标题和目的、启用分类和标签、人数上限及当前成员数量，成功后进入 `RECRUITING`，返回招募对象。其他状态返回 `40900`。

### 5.6 关闭招募

```http
POST /api/recruitments/{id}/close
Content-Type: application/json
```

只有负责人可以关闭 `RECRUITING` 招募。请求体可以省略，也可以传入关闭原因。

```json
{
  "reason": "已经通过其他渠道完成组队"
}
```

`reason` 最长 255 字符。关闭后状态变为 `CLOSED`，返回招募对象并保留成员和标签历史。`DRAFT`、`TEAMED`、`CLOSED` 均返回 `40900`。本周关闭操作只处理招募状态，申请失效和通知将在对应模块实现时纳入同一事务。

### 5.7 删除草稿

```http
DELETE /api/recruitments/{id}
Authorization: Bearer <token>
```

只有负责人可以删除 `DRAFT`。删除招募、标签关联和负责人成员记录在同一事务内完成。`RECRUITING`、`TEAMED`、`CLOSED` 返回 `40900`。成功返回 HTTP 200，响应为 `{"code":0,"message":"success","data":null}`。重复删除返回 `40400`。

所有招募管理接口均从 JWT 获取当前身份，未登录返回 `40100`，其他用户操作已有招募返回 `40300`。管理操作先锁定招募行，再判断负责人、状态和人数。

### 5.8 我的招募

```http
GET /api/recruitments/mine
Authorization: Bearer <token>
```

需要登录。当前按 `ownerId` 返回本人负责的全部招募，包含四种状态，按创建时间降序、ID 降序排列。响应 `data` 是第 4 节对象的数组，无数据时为 `[]`。不接受大厅分页或筛选参数。当前尚无转交功能，创建者即负责人，后续转交时需按业务决策明确创建者和负责人的查询口径。

### 5.9 分类与技能标签

```http
GET /api/categories
GET /api/tags
```

允许游客访问，只返回已启用的条目，无数据时 `data` 为 `[]`。

分类按 `sortOrder` 升序、ID 升序返回。

```json
{"code":0,"message":"success","data":[{"id":1,"name":"课堂组队","parentId":null,"sortOrder":1}]}
```

标签按名称升序、ID 升序返回。

```json
{"code":0,"message":"success","data":[{"id":1,"name":"Java"},{"id":2,"name":"Vue"}]}
```

## 6. 申请和成员（后续接口约定，尚未实现）

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
