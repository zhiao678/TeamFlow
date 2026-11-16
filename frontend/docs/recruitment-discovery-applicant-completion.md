# 招募发现与申请人侧交付说明

## 依据与实施顺序

工作分支：`feat/frontend/recruitment-and-applyment`，起点 `c795e54`。
任务来源为本分支 `recruitment-discovery-applicant-handoff.md` 与根目录 API 规范。
用户提供的 `feat/frontend/user-and-recruitment` 分支已无法从远端获取，当前工作分支包含完整交接要求。

1. 大厅增强：扩展参数类型、URL 解析、技能字典和服务端排序。
2. 收藏闭环：真实 API、Pinia 状态、共用按钮、我的收藏和登录返回。
3. 申请闭环：共用申请接口/类型、详情申请区域、撤回确认、我的申请与通知定位。
4. 接入成员组件及通知刷新；运行契约回归、构建和真实后端双账号浏览器验收。

## 已完成功能

- 大厅保留原有关键词、分类、课程、教师、状态、分页及请求取消。新增技能多选、最少剩余名额与热门排序；`tagIds` 逗号分隔，`0` 名额有效，筛选排序在服务端执行。筛选变更回第一页，详情返回保留 URL 条件。
- 卡片和详情共用收藏按钮，新增 `/my-favorites`；从 `favorited` 初始化、操作成功后更新、失败保留状态。每条招募防重复请求，旧读取不能覆盖新写入，退出/换账号清空状态并忽略旧响应。游客先登录并返回，不自动收藏。
- 详情申请表三个字段均可选，长度分别为 1000/1000/255。通过本人申请历史选出当前招募最新申请；正式成员由成员接口判断。负责人、正式成员不显示提交表单，满员、截止、关闭、成队和已有待处理申请显示原因。申请/成员查询失败时不能提交。
- 撤回有确认/取消，撤回及拒绝后可重新申请。写入成功或 409 后重新读取真实状态；刷新失败明确区分于写入失败，不使用模拟成功回退。
- `/my-applications` 保留历史，展示标题、申请时间、状态、处理时间及拒绝原因，支持待处理撤回和进入详情。`applicationId` 高亮并滚动定位，找不到时提示且保留其他记录。
- 申请与撤回成功调用现有 `refreshUnreadCount()`，未修改通知生成或公共鉴权逻辑。两个新增路由受鉴权保护，原导航自动展示入口。
- 复用 `RecruitmentMembers`，增加暴露加载状态、错误及成员数据，切换会话后重新读取。保留原审核调用及 `reload()` 约定。

## 接口与协作边界

接入大厅、详情、分类、技能、收藏 POST/DELETE、收藏列表、申请 POST、本人申请列表、撤回 PATCH、正式成员列表。
审核接口仍集中在 `src/api/applications.ts`，兼容审核模块已有导出。
未新增后端业务字段；申请列表尚无负责人字段，按交接约定不展示，也不逐条追加详情查询。
项目、任务和发布管理页面保留。无定时轮询；重新进入页面会读取最新状态。

## 验证

2026-11-16，WSL Ubuntu-24.04，Node 22.22.2：

- `npm run lint:check`、`npm run format:check`、`npm run build`。
- `node scripts/check-discovery-applicant.mjs`：筛选序列化、0 名额、历史接受与正式成员区分、旧响应隔离、账号切换、写入失败保留、重复提交、409 与成功后刷新失败。
- 原有 `check-auth.mjs`、`check-adaptation.mjs`、`check-notifications.mjs` 回归通过。
- Playwright + 本分支真实 Spring Boot 服务 + 独立 H2：新建负责人/申请人账号，验证游客收藏登录返回、筛选恢复、卡片/详情/收藏页同步、提交、取消撤回、撤回重申、拒绝重申、接受后正式成员阻止申请、三条历史记录、通知定位、成员失败重试和 390px 移动端布局。浏览器无未处理脚本异常。
- `bash mvnw test`：43 项通过，失败/错误/跳过均为 0。
- 正式 MySQL 环境未在本次联调中使用。H2 原测试表缺少正式库已有的 `application.rejection_reason`，已在测试 schema 补齐一个字段；没有修改后端业务代码或现有数据库。
- 本机后端验证使用可用的 Java 21（项目编译目标 17）；小组正式环境仍按后端 README 使用 Java 17。

### 复跑浏览器验收

脚本 `scripts/check-discovery-applicant-e2e.mjs` 需要 Playwright 与 Chromium，并会在目标后端新建测试账号/招募，请只指向独立测试数据库。
默认后端 `http://127.0.0.1:18080/api`、前端 `http://127.0.0.1:5173`；前端通过 `API_PROXY_TARGET=http://127.0.0.1:18080` 代理。
后端 CORS 当前允许 5173/4173，使用其他前端端口可能收到 403。
可设置 `API_BASE`、`FRONTEND_URL`、`PLAYWRIGHT_MODULE`（已有模块的绝对入口）、`CHROMIUM_EXECUTABLE` 和 `OUTPUT_DIR`。
脚本截图默认写入操作系统临时目录 `teamflow-applicant-e2e/`，不提交截图或测试账号 Token。

```bash
node scripts/check-discovery-applicant.mjs
node scripts/check-discovery-applicant-e2e.mjs
```
