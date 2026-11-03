# 招募浏览模块任务说明

## 当前状态

- 工程已具备公共布局、登录注册、用户 Store、Token 持久化、路由守卫及统一 401 处理。
- 复用 `src/api/request.ts`，请求自动携带登录凭据，不在页面中另写鉴权逻辑。
- 当前尚无招募页面与导航入口，需要新增；不把旧项目页面直接改为招募页面。
- API 契约参考[招募平台 API 规范](../../docs/recruitment-api-spec.md)。文档是目标约定，不代表后端全部实现。

## 本周待完成

- 招募大厅：卡片列表、分页、关键词搜索、分类/课程/教师/状态筛选、最新与即将截止排序。
- 卡片展示标题、分类、课程或活动、负责人、技能标签、人数、剩余名额、截止时间及状态。
- 招募详情：展示完整说明、要求、负责人、课程/教师/活动、标签、名额与截止信息。
- 区分已截止、已成队、已关闭等不可申请情形。
- 加载、空结果、接口失败重试、详情不存在及无权限状态。
- 申请、收藏、私聊本周不实现；如预留入口，应禁用并说明暂未开放。

## 路由和文件建议

```text
/recruitments        招募大厅（公开浏览）
/recruitments/:id    招募详情（公开浏览，草稿受后端权限限制）

src/views/recruitment/RecruitmentHallView.vue
src/views/recruitment/RecruitmentDetailView.vue
src/components/recruitment/RecruitmentCard.vue
src/api/recruitment-query.ts
src/types/recruitment.ts
```

放到现有 `MainLayout` 内，只实现内容区。大厅筛选和分页建议保存在 URL query，
更换筛选或排序重置页码，返回大厅时保留条件。快速切换条件时防止旧请求覆盖新结果。
新增公共导航入口和路由时与公共框架负责人协调，维护 `meta.title`。

## 接口现状与缺口

以下为完整服务端路径；Axios 调用时省略 `/api`：

| 接口 | 当前代码状态 | 接入要求 |
| --- | --- | --- |
| `GET /api/recruitments` | 已有基础查询，但返回数组，无查询参数 | 可接基础展示；分页、搜索、筛选、排序需后端补齐 |
| `GET /api/recruitments/{id}` | 已有基础详情 | 先接现有字段，不假设存在成员列表或申请权限字段 |
| `GET /api/categories` | 尚未实现 | 与公共模块及后端约定选项类型和查询入口 |
| `GET /api/tags` | 尚未实现 | 与公共模块及后端约定选项类型和查询入口 |

目标列表响应为 `{ records, page, pageSize, total }`，页码从 1 开始；
查询参数为 `keyword/categoryId/courseName/teacherName/status/page/pageSize/sort`，
本周排序只接 `LATEST/DEADLINE`。不能将当前数组强行当作分页对象。
当前查询会混入登录用户自己的草稿等状态，不能直接宣称符合大厅默认筛选规则。
临时前端筛选或模拟分页必须标明，不能作为服务端筛选分页验收通过。

## 与招募管理模块的对接

- 浏览负责人维护共享 `src/types/recruitment.ts`，先与管理负责人确认，再让双方引用。
- 服务端当前 `tags` 是名称数组，提交表单却需要 `tagIds`；编辑回填方案需双方与后端确认。
- 详情可提供前往 `/recruitments/:id/edit` 的入口，仅登录负责人显示。
- 发布、编辑、关闭逻辑归管理模块，详情不另写一套操作请求。
- 列表、详情和管理页共用状态中文文案及截止判断。状态只用 `DRAFT/RECRUITING/TEAMED/CLOSED`，
  “已截止”是截止时间派生的显示，不增加传输枚举。时间按 `Asia/Shanghai` 解释。
- 草稿、404 和 403 以服务端结果为准；剩余名额大于零不代表一定可申请。

## 开发和验收顺序

1. 先统一招募类型、路由和模块边界，搭建大厅/详情及错误状态。
2. 接基础列表与详情；缺失接口期间可用符合契约、明确标注的 Mock，不能失败后静默回退。
3. 后端补齐后接筛选、分页和排序，完成大厅到详情再返回的流程。
4. 验证快速切换筛选、空结果、失败重试、非法 ID、未知招募及直接访问刷新。
5. 运行 `npm run lint:check`、`npm run format:check`、`npm run build`。

