# 招募发布与管理模块任务说明

## 当前状态

- 登录注册、当前用户、个人资料、Token 与 401 处理已有公共实现。
- 当前用户使用 `useAuthStore()`，请求复用 `src/api/request.ts`，不从表单传 `ownerId`。
- 创建、编辑、发布和关闭已有后端代码；删除草稿、“我的招募”和选项查询尚有缺口。
- 以[招募平台 API 规范](../../docs/recruitment-api-spec.md)为目标约定，实际差异见下表。

## 本周待完成

- 共用的新建/编辑表单：分类、标题、课程、教师、组队目的、详细说明、要求、人数上限、截止时间、技能标签。
- 保存草稿、直接发布、编辑保存、发布已有草稿、删除草稿、关闭招募。
- “我的招募”列表及状态筛选，提供合法的管理操作入口。
- 表单校验、加载、提交中、防重复提交、成功反馈、失败保留输入。
- 无权限、记录不存在、状态冲突及当前状态不允许编辑等反馈。
- 本周不做加入申请、审核、成员管理、聊天、收藏、通知或管理员功能。

## 路由和文件建议

```text
/recruitments/new        新建招募
/recruitments/:id/edit   编辑招募
/my-recruitments         我的招募

src/views/recruitment/RecruitmentEditorView.vue
src/views/recruitment/MyRecruitmentsView.vue
src/components/recruitment/RecruitmentForm.vue
src/api/recruitment-management.ts
```

以上页面设置 `meta.requiresAuth: true`，放到现有公共布局内。
`/recruitments/new` 与浏览模块的 `/recruitments/:id` 要共同验证，避免把 `new` 当成 ID。
公共路由和导航改动提前协调，不重复实现登录弹窗或 Token 存储。

## 接口现状与缺口

以下为完整服务端路径；Axios 调用时省略 `/api`：

| 接口 | 当前代码状态 | 接入要求 |
| --- | --- | --- |
| `POST /api/recruitments` | 已实现 | `publish: false` 保存草稿，`true` 直接发布 |
| `PATCH /api/recruitments/{id}` | 已实现 | 仅负责人编辑；`TEAMED/CLOSED` 不允许编辑 |
| `POST /api/recruitments/{id}/publish` | 已实现 | 仅允许发布草稿 |
| `POST /api/recruitments/{id}/close` | 已实现 | 可选 `{ reason }`；UI 本周仅向正在招募记录提供关闭入口 |
| `DELETE /api/recruitments/{id}` | 尚未实现 | 删除草稿待接，不用关闭接口代替删除 |
| `GET /api/recruitments/mine` | 尚未实现 | 不可用公开大厅列表冒充正式“我的招募”接口 |
| `GET /api/recruitments/{id}` | 已实现 | 编辑回填复用浏览模块的查询封装 |
| `GET /api/categories`、`GET /api/tags` | 尚未实现 | 分类及技能选项待公共模块与后端补齐 |

当前草稿也要求分类、标题、组队目的、人数上限完整，不能宣称支持任意不完整草稿。
标题最长 150 字符、目的 200、课程和教师各 100；总人数至少为 2，包含负责人。
人数上限不能低于已有正式成员数。创建成功后负责人由后端自动加入成员。
时间使用 `yyyy-MM-dd'T'HH:mm:ss`，不要未经转换直接提交带 `Z` 的 UTC 字符串。

编辑还有两个待确认点：当前详情只返回标签名称，表单需要标签 ID；
当前 PATCH 对部分可空字段的 `null` 表示不修改，清空截止时间等字段的语义需与后端确认。
不要自行推断或静默丢弃原有标签。

## 与浏览及公共模块的对接

- 复用浏览负责人维护的 `src/types/recruitment.ts`、状态文案及详情查询，不各自复制不同类型。
- 分类/标签类型与公共查询封装提前约定，接口未完成可使用明确标注的契约 Mock。
- Axios 保留完整响应，先检查 `response.data.code`，业务对象取 `response.data.data`。
- 创建成功可进入 `/my-recruitments` 或对应详情；草稿详情仅负责人可见。
- 操作成功后用服务端结果更新展示或重新查询；失败保留原状态与输入，不能展示假成功。
- 按钮显隐只是界面提示，403/404/409 等仍按服务端结果处理。
- 删除、关闭提供确认交互；取消确认不发请求。按钮按记录禁用，避免重复操作。

## 开发和验收顺序

1. 与浏览负责人确认共享类型、详情字段、路由及标签回填方案。
2. 优先完成创建草稿、直接发布、编辑、发布草稿、关闭的可联调链路。
3. “我的招募”、删除草稿和选项接口缺失期间明确标注阻塞，不将 Mock 当作真实接入。
4. 验证表单校验、重复点击、失败保留输入、非本人访问、状态冲突和退出后访问受限页。
5. 验证保存后刷新数据仍一致，页面正确显示截止时间和技能标签。
6. 运行 `npm run lint:check`、`npm run format:check`、`npm run build`。

