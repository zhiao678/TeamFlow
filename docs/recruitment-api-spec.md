# 大学生组队招募平台 API 规范

本文档基于 `recruitment-decisions.md` 和 `recruitment-er.md`，用于前后端共同开发。

截至 2026-11-12，第 3 至 7 节的用户、资料、招募、申请、成员、退出审核、负责人继任及通知接口已实现。第 6.7 节保留后续确认成队约定。

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
| `favorited` | boolean | 否 | 当前登录用户是否已收藏；游客恒为 `false` |
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
| `tagIds` | string | 否 | 逗号分隔的技能标签 ID，按任一标签匹配 |
| `minRemainingSlots` | number | 否 | 最少剩余名额 |
| `status` | string | 否 | `DRAFT`、`RECRUITING`、`TEAMED`、`CLOSED`，默认 `RECRUITING` |
| `page` | number | 否 | 从 1 开始，默认 1 |
| `pageSize` | number | 否 | 1 至 100，默认 20 |
| `sort` | string | 否 | `LATEST`、`DEADLINE`、`POPULAR`，默认 `LATEST` |

筛选条件取交集。`keyword`、`courseName` 和 `teacherName` 去除首尾空格，空字符串等同未筛选，`%` 和 `_` 按普通字符匹配。`status` 和 `sort` 须严格使用表中的枚举值。分类不存在时返回空结果。非法分页、状态或排序参数返回 `40000`。

`LATEST` 按创建时间降序、ID 降序排列。`DEADLINE` 按截止时间升序、ID 降序排列，没有截止时间的记录放在最后，已过期记录仍参与排序。截止时间不会自动将招募改为关闭。

`tagIds`、`minRemainingSlots` 和 `POPULAR` 用于招募发现增强。`POPULAR` 按收藏数、已接受申请数、创建时间和 ID 进行规则排序；没有行为数据时仍能正常返回结果。

`tagIds` 使用逗号分隔，例如 `tagIds=1,2`，匹配任一标签。`minRemainingSlots` 为大于等于 0 的整数。

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

`reason` 最长 255 字符。关闭后状态变为 `CLOSED`，返回招募对象并保留成员和标签历史。`DRAFT`、`TEAMED`、`CLOSED` 均返回 `40900`。关闭同时将该招募的 PENDING 入队申请置为 EXPIRED，向申请人发送 APPLICATION_EXPIRED 通知，整体在同一事务提交。

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

### 5.10 招募收藏

```http
POST   /api/recruitments/{id}/favorite
DELETE /api/recruitments/{id}/favorite
GET    /api/recruitments/favorites
```

上述接口均需要登录。收藏和取消收藏成功时返回统一空数据响应。重复收藏以及取消未收藏的招募均保持幂等。其他用户不能收藏草稿。`GET /api/recruitments/favorites` 按收藏时间倒序返回当前用户收藏的招募数组；无收藏时返回 `[]`。招募详情和大厅列表中的 `favorited` 表示当前用户收藏状态，游客恒为 `false`。

## 6. 申请、成员和退出审核

本节写操作使用 JWT 当前身份。权限不足返回 `40300`，对象不存在返回 `40400`，重复待处理申请、终态操作、满员、截止等状态冲突返回 `40900`。写操作先锁招募行，再锁申请或转交记录，成员、状态和通知一起提交。

申请响应对象包含 `id`、`recruitmentId`、`recruitmentTitle`、`applicantId`、`applicantDisplayName`、`introduction`、`skillsDescription`、`availableTime`、`status`、`rejectionReason`、`createdAt` 和 `processedAt`。列表返回该对象的数组，按创建时间降序、ID 降序排列，无数据时为 `[]`。

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
- 负责人不能申请本人招募，招募必须有剩余名额。
- 同一用户最多存在一条 `PENDING` 申请。
- `PENDING` 申请不占用正式名额。
- `introduction` 和 `skillsDescription` 各最多 1000 字符，`availableTime` 最多 255 字符，均可选。
- 同一用户撤回或被拒后可重新提交，历史申请保留。其他用户的草稿返回 `40400`。

成功状态：`201 Created`。

### 6.2 查询招募申请

```http
GET /api/recruitments/{id}/applications
```

只有招募负责人可以查看全部申请。申请人只能查看自己的申请状态。

申请人使用以下接口读取本人的全部历史申请。

```http
GET /api/applications/me
```

需要登录，返回申请数组。负责人查看招募申请列表也需要登录。

### 6.3 接受申请

```http
PATCH /api/applications/{id}/accept
```

只有负责人可以操作。服务端必须在事务中重新检查招募状态、申请状态和剩余名额；接受最后一个名额时只能有一个请求成功。

只有 RECRUITING 招募的 PENDING 申请可接受或拒绝。截止前已提交的申请在截止后仍可处理。重复处理返回 `40900`。成功返回更新后的申请对象。

成功后：

- 申请变为 `ACCEPTED`。
- 创建 `team_member` 记录。
- 已有 LEFT / REMOVED 历史关系时复用该记录，恢复 ACTIVE / MEMBER 并清除 leftAt。
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

`reason` 可省略或为空，最多 255 字符。申请变为 `REJECTED`，保存为 `rejectionReason`，并通知申请人，返回更新后的申请对象。

### 6.5 撤回申请

```http
PATCH /api/applications/{id}/withdraw
```

只有申请人可以撤回自己的 `PENDING` 申请。撤回后状态变为 `WITHDRAWN`，通知当前招募负责人，返回更新后的申请对象，后续允许重新提交申请。

### 6.6 查询成员

```http
GET /api/recruitments/{id}/members
```

允许游客查询公开招募的 ACTIVE 正式成员。草稿沿用详情可见性，其他用户访问返回 `40400`。返回数组，每项为 `{id, displayName, avatarUrl, role, joinedAt}`，其中 id 是用户 ID。负责人优先，其余按加入时间、用户 ID 升序排列。

```http
DELETE /api/recruitments/{id}/members/{userId}
```

只有当前负责人可移除其他 ACTIVE 成员。成功返回 HTTP 200 与统一空数据响应，关系更新为 REMOVED 并记录 leftAt，相关队伍群成员资格与待处理退出、继任记录同时失效。人数和剩余名额由 ACTIVE 成员实时计算。

自行调用 DELETE 返回状态冲突，普通成员使用退出申请，负责人使用选定继任者流程。重复移除返回 `40900`。

### 6.7 确认成队（后续接口约定）

```http
POST /api/recruitments/{id}/form-team
```

只有负责人可以操作。允许未满员成队。服务端在同一事务中：

- 将招募状态改为 `TEAMED`。
- 将剩余 `PENDING` 申请改为 `EXPIRED`。
- 创建唯一的队伍群聊。
- 发送相关通知。

### 6.8 成员申请退出

```http
POST /api/recruitments/{id}/exit-requests
GET  /api/recruitments/{id}/exit-requests
GET  /api/member-exit-requests/me
PATCH /api/member-exit-requests/{id}/approve
PATCH /api/member-exit-requests/{id}/reject
```

所有接口需要登录。普通 ACTIVE 成员提交退出申请，可选请求体为 `{"reason":"时间安排变化"}`，原因最多 255 字符。POST 返回 HTTP 201 和申请对象，同一成员同一招募至多一条 PENDING 退出申请。负责人通过继任流程退出。

招募退出申请列表仅当前负责人可读，我的退出申请列表仅返回本人记录。列表按创建时间和 ID 降序返回数组。

退出申请对象包含 `id`、`recruitmentId`、`recruitmentTitle`、`userId`、`userDisplayName`、`status`、`reason`、`rejectionReason`、`createdAt`、`processedAt` 和 `handoverHint`。

- 提交后保持 ACTIVE 和原人数，通知负责人 MEMBER_EXIT_REQUESTED。
- 只有当前负责人可批准或拒绝 PENDING 申请，负责人不能审核自己的退出。
- 批准后更新为 APPROVED，成员转 LEFT、记录 leftAt、撤销队伍群资格，并使涉及该成员的待确认继任记录失效，通知本人 MEMBER_EXIT_APPROVED。
- 拒绝可选 `{"reason":"请先沟通分工"}`，最多 255 字符，状态转 REJECTED，成员保留资格，可重新申请，通知本人 MEMBER_EXIT_REJECTED。
- 已非 ACTIVE 的待处理申请审核时返回 EXPIRED 对象并提交失效状态，避免旧申请影响重新入队后的资格。移除成员时也会使旧申请失效。
- 成员已离队或申请已处理后重复操作返回 `40900`。成功审核返回最新申请对象。

### 6.9 负责人选择继任者后退出

```http
POST /api/recruitments/{id}/leader-exit-requests
GET /api/leader-transfers/me
PATCH /api/leader-transfers/{id}/accept
PATCH /api/leader-transfers/{id}/reject
```

需要登录。当前负责人提交 `{"successorId":2}`，指定本队其他 ACTIVE 普通成员。POST 返回 HTTP 201 的转交对象，同一招募至多一条 PENDING 转交。

转交对象包含 `id`、`recruitmentId`、`recruitmentTitle`、`fromUserId`、`fromDisplayName`、`toUserId`、`toDisplayName`、`status`、`exitAfterTransfer`、`createdAt`、`processedAt` 和 `handoverHint`。我的转交返回本人作为发起人或接收人的记录数组。

接收者确认前，原负责人保留负责人权限与成员资格。只有指定继任者可接受或拒绝。接受时重新检查双方身份和有效成员资格，在同一事务中降原负责人角色、提升继任者为 LEADER、修改 ownerId，再将原负责人置 LEFT 并撤销其群资格，最后记录 ACCEPTED 和通知。确认后人数减少一人。

新退出流程写入 `exitAfterTransfer=true`。既有数据默认 false，确认这类记录时只完成负责人转交，原负责人作为普通成员留队。继任者的待处理普通退出申请在接受时失效。被移除或已退出的继任者不能接任，相关转交转 EXPIRED。拒绝转 REJECTED，原负责人继续留队并可重选。

职责与工作内容交接通过 `handoverHint` 提供简短提示。转交记录处理后重复接受或拒绝返回 `40900`。

## 7. 通知接口

申请模块通过后端 `NotificationService.create(...)` 创建通知，不由客户端直接创建。`eventKey` 对同一接收者唯一，用于防止事务重试产生重复通知。本阶段通知事件类型包括：

```text
APPLICATION_SUBMITTED
APPLICATION_ACCEPTED
APPLICATION_REJECTED
APPLICATION_WITHDRAWN
```

对应的 `eventKey` 使用稳定的业务对象键，例如 `application:{applicationId}:accepted`。提交和撤回申请通知当前招募负责人，接受和拒绝通知申请人。通知目标为 APPLICATION，仅填 applicationId，recruitmentId 为空。关闭造成的申请失效使用 APPLICATION_EXPIRED 和 `application:{id}:expired`。申请与通知在同一事务中写入。

退出审核事件使用 MEMBER_EXIT_REQUESTED / APPROVED / REJECTED，目标为 RECRUITMENT，事件键为 `member-exit:{id}:requested|approved|rejected`。继任邀请、接受、拒绝使用 LEADER_TRANSFER_REQUESTED / ACCEPTED / REJECTED，目标为 LEADER_TRANSFER，仅填 leaderTransferId。过期时更新业务状态。NotificationService.create 保留原签名并提供支持转交 ID 的重载，通知去重继续由接收者和 eventKey 唯一键保证。

### 7.1 通知列表

```http
GET /api/notifications?page=1&pageSize=20&unreadOnly=false
```

需要登录。`page` 从 1 开始，默认 1；`pageSize` 默认 20，最大 100；`unreadOnly` 默认 `false`。`total` 是当前筛选条件下的记录数，`unreadCount` 是当前用户全部未读通知数。

响应数据结构：

```json
{
  "records": [
    {
      "id": 1,
      "eventType": "APPLICATION_SUBMITTED",
      "targetType": "APPLICATION",
      "recruitmentId": null,
      "applicationId": 12,
      "leaderTransferId": null,
      "title": "收到新的入队申请",
      "content": "有学生申请加入你的招募",
      "read": false,
      "createdAt": "2026-11-11T10:30:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "unreadCount": 1
}
```

### 7.2 标记已读

```http
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all
```

需要登录，只能修改当前用户自己的通知。单条通知不存在或不属于当前用户时返回 `40400`；全部已读只影响当前用户。成功时返回统一空数据响应。

## 8. 当前不在本文件中冻结的接口

以下接口等核心招募流程确认后补充：

```text
GET  /api/conversations
POST /api/conversations/private
GET  /api/conversations/{id}/messages
POST /api/conversations/{id}/messages

POST  /api/reports
GET   /api/admin/reports
PATCH /api/admin/reports/{id}
```

这些接口不能改变本文件中已确认的用户、招募、申请、成员和成队规则。

## 9. 第一批实现顺序

1. 用户注册、登录和当前用户。
2. 用户资料和技能标签。
3. 分类和招募 CRUD。
4. 招募列表筛选和详情。
5. 申请提交、撤回、接受和拒绝。
6. 成员查询和确认成队。
7. 使用演示数据和自动化测试验证核心流程。
