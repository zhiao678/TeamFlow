# 项目空间交接

## 开发起点

本周公共分支为 `feat/frontend/space-and-navi`。开始前确认开发分支已包含 `d5febe7` 和 `816475d`。现有侧栏从「项目协作」进入项目列表，项目路由已接入登录守卫。

| 路由名称           | 路径                            | 页面                   |
| ------------------ | ------------------------------- | ---------------------- |
| `projects`         | `/projects`                     | 项目列表               |
| `project-overview` | `/projects/:projectId/overview` | 项目概览，详情默认页   |
| `project-members`  | `/projects/:projectId/members`  | 项目成员               |
| `project-tasks`    | `/projects/:projectId/tasks`    | 项目任务，目前为占位页 |

项目空间负责列表、详情容器、概览、成员页，以及它们共用的类型和接口。任务页由项目任务模块负责。

## 已落地的接口

| 请求                                    | `data`              | 用途                       |
| --------------------------------------- | ------------------- | -------------------------- |
| `GET /api/projects`                     | `ProjectDetailVO[]` | 当前账号可访问的项目       |
| `GET /api/projects/{projectId}`         | `ProjectDetailVO`   | 项目详情、当前用户项目角色 |
| `GET /api/projects/{projectId}/members` | `ProjectMemberVO[]` | 有效项目成员               |

- 项目字段以 `ProjectDetailVO` 为准。本次重点同步 `recruitmentId`、`recruitmentTitle`、`memberCount` 和 `currentUserRole`。项目角色为 `ADMIN` 或 `MEMBER`。
- 成员字段为 `id`、`displayName`、`avatarUrl`、`projectRole`、`teamRole` 和 `joinedAt`。其中 `id` 是用户 ID，任务页用它作为负责人选项的值。
- 接口用登录凭据识别当前用户。前端直接使用返回的列表和角色，无需附带 `ownerId` 或当前用户 ID。项目归属、成队和成员资格继续由服务端管理。

以当前实现和测试为准。[项目控制器](../../backend/src/main/java/com/teamflow/backend/controller/ProjectController.java) · [项目响应](../../backend/src/main/java/com/teamflow/backend/vo/ProjectDetailVO.java) · [成员响应](../../backend/src/main/java/com/teamflow/backend/vo/ProjectMemberVO.java) · [成员查询](../../backend/src/main/java/com/teamflow/backend/mapper/ProjectMemberMapper.java) · [接口测试](../../backend/src/test/java/com/teamflow/backend/controller/ProjectApiTests.java)

## 待完成

1. 更新 `src/api/projects.ts` 和 `src/types/project.ts`，继续复用现有 Axios 和业务响应结构。增加 `getProjectMembers(projectId, signal?)`，供成员页和任务页共用。
2. 完善 `ProjectListView.vue`。展示当前账号可访问的项目，空数组显示空状态，请求失败提供重试。
3. 完善 `ProjectDetailView.vue` 和 `ProjectOverviewView.vue`。按后端内容展示项目说明、负责人、成员数和角色。负责人转交后以刷新得到的最新详情为准。
4. 完善 `ProjectMembersView.vue`。使用成员接口展示完整的有效成员和角色，区分项目角色与队伍角色。加载中、空数组和请求失败分别显示。
5. `401` 走统一登录及回跳流程。对 `403` 显示无权限并提供返回项目列表入口。对 `404` 提示项目可能已失效。网络或服务异常提供重试。切换项目时取消旧请求或隔离迟到响应。

## 交给项目任务模块

- 现有详情容器在详情读取成功后向子页传入 `project`。保留这一入口，由 `project.id` 和 `project.currentUserRole` 分别提供项目 ID 和角色。
- 在项目 API 层提供带类型的成员读取方法。任务页按需读取有效成员并处理加载与失败，用同一份成员契约生成负责人选项。
- 保留 `project-tasks` 路由名和路径。任务接口完成后由任务模块替换占位页，有公共路由改动时与导航负责人协调。

## 验收

- 负责人和普通成员登录后均可在列表看到所属项目，且可进入概览和成员页。角色、说明和有效成员与接口一致。
- 成员退出获批或被移除后，刷新列表便看不到该项目。直接访问旧详情或成员链接时能看到无权限提示及返回入口。
- 无项目账号显示空状态。验证游客登录并回跳、无效项目 ID、真实 `404`、断网后重试，以及连续切换项目时的数据隔离。
- 在 `frontend` 下运行 `npm run type-check`、`npm run lint:check`、`npm run format:check` 和 `npm run build`。
