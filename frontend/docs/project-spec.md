# 项目模块说明

## 当前状态

- 项目列表已调用真实后端，支持进入对应详情及加载、空数据、失败重试状态。
- 已实现详情外壳、概览、成员信息展示和任务入口，详见 [项目模块交接](./project-handoff.md)。
- `/projects` 已接入公共布局，侧栏、顶部栏由公共组件维护，页面只需实现内容区域。
- 项目类型位于 `src/types/project.ts`；项目 API 位于 `src/api/projects.ts`。公共模拟数据仍供工作台等页面使用。

## 本次已完成内容

- 完成正式项目列表，支持进入对应项目详情。
- 完成项目详情外壳，提供概览、成员、任务入口。
- 完成项目概览及成员区域的创建者展示；完整成员列表待后端接口提供。
- 接入项目列表与项目详情接口，补充加载、空数据和失败状态。

## 与任务模块的对接

建议采用以下路由：

```text
/projects                         项目列表（已有）
/projects/:projectId              项目详情，默认进入概览
/projects/:projectId/overview     项目概览
/projects/:projectId/members      项目成员
/projects/:projectId/tasks        项目任务
```

项目模块负责详情外壳、页签导航和子路由出口 `<RouterView>`。
任务模块负责任务子页面及其交互，从路由参数取得 `projectId`。
双方先约定页面入口和参数，再分别开发，避免重复实现任务区域。
公共路由文件 `src/router/index.ts` 的修改需协调合并；新增路由维护好 `meta.title`。

## 接口与数据约定

- 以[前后端 API 规范](../../docs/api-spec.md)为准。
- 建议新增 `src/api/projects.ts`，复用 `src/api/request.ts`，调用 `GET /projects` 和 `GET /projects/{id}`。
- Axios 已配置 `/api` 前缀，不要重复拼接或写死后端地址。
- 当前实例返回完整 Axios 响应，业务数据位于 `response.data.data`；同时检查业务码 `response.data.code`。
- 成员列表接口尚未在规范中定义，需与后端确认；临时使用模拟数据时明确标注。
- 工作台仍使用公共模拟数据，接入项目接口时不要直接删除 `src/mocks/data.ts` 或修改公共请求行为。

## 交付检查

- 项目列表能进入正确详情，概览、成员、任务入口可切换。
- 直接访问与刷新详情路径可正常展示，项目不存在时有明确提示。
- 接口加载、失败和空数据状态清晰，不将请求失败显示为正常空列表。
- 运行 `npm run lint:check`、`npm run format:check`、`npm run build`，并手动验证页面。
- 交接时说明已接入接口、仍使用模拟数据的部分及未解决事项。
