# 前端说明

## 技术栈

暂定为 Vue3 + Vite  

完整技术栈：Vue 3、TypeScript、Vite、Vue Router、Pinia 和 Element Plus。

## 相关指引

### 当前页面与真实接口

- `/login`、`/register` 调用真实注册和登录接口。Token 保存在浏览器本地存储，
  刷新后通过 `GET /api/users/me` 恢复身份；网络失败不会主动清除 Token。
- `/profile` 支持查看和修改昵称、头像 URL、院系、年级和简介。技能通过 `GET /api/tags` 选择，并通过资料接口的 `tagIds` 保存。
- 工作台 `/dashboard` 和我的任务 `/tasks` 需要登录。项目来自真实项目列表，
  个人任务临时逐个查询可见项目任务，并按当前用户 `assigneeId` 筛选。
  当前项目列表并非“我参与的项目”，因此统计称为“可见项目”。
  任一任务查询失败会显示错误和重试，不显示不完整统计。项目较多时汇总可能较慢，
  后续应替换为后端个人任务聚合接口。
- 项目列表、详情、项目任务保留原有真实接口实现；项目内任务创建仍沿用原模块的临时身份约定。
- 消息接口尚未实现，消息页面和工作台明确显示暂未开放，不生成模拟未读数。
  现有 `mocks/data.ts` 和通知 Store 留作旧代码参考，当前页面不再引用。
- 退出先调用 `POST /api/auth/logout`，无论请求成功或失败都清理本地身份；无状态 JWT 的旧 Token 不会因此立即在服务端失效。

所有请求复用 `src/api/request.ts`，登录后携带 Bearer Token。受限请求返回 `401`
会清理身份并跳转登录；错误密码在登录表单提示。受限路由设置 `meta.requiresAuth`，
登录后返回原站内地址。公共 Axios 保留完整响应，业务数据使用 `response.data.data`。

需要启动同时支持新版用户接口和旧版项目接口的后端及对应数据库；连接失败不回退到 Mock。
用户接口约定见 [招募平台 API 规范](../docs/recruitment-api-spec.md)，
项目和任务见 [原 API 规范](../docs/api-spec.md)。
公共布局位于 `src/layouts/MainLayout.vue`，全局样式位于 `src/assets/styles/index.css`。
正式部署使用 History 路由，需要服务器将前端页面路径回退到 `index.html`。

验收：注册/登录 → 刷新保持身份 → 修改资料并检查顶部昵称 → 查看真实工作台和任务 → 退出。
另外检查错误密码、过期 Token、接口失败重试和消息不可用提示。
可运行 `node scripts/check-auth.mjs` 验证前端鉴权逻辑；该脚本使用临时本地契约服务，
不代表真实后端联调通过。

### 用户模块接入边界

已接入 `POST /api/auth/register`、`POST /api/auth/login`、
`GET /api/users/me`、`PATCH /api/users/me`。注册页提供确认密码，
用户名和昵称不能仅为空格；个人资料加载或验证失败时禁用保存，支持重试。
恢复身份遇到网络或服务器错误时保留 Token 并显示重试入口，只有明确的 401 清理身份。
退出后重新登录，即使旧请求迟到，也不会覆盖新账号的资料。

当前已接入分类、标签、我的招募、删除草稿及退出接口，不使用本地硬编码选项或公开列表回退。
头像当前支持保存 URL，不提供上传接口。旧项目、任务和消息路由保留。

### 招募接口适配

- 大厅使用服务端分页 `{ records, page, pageSize, total }`，搜索、分类、课程、教师、状态和排序通过请求参数传递；默认状态为 `RECRUITING`，不发送 `ALL`。
- 排序支持 `LATEST/DEADLINE`；页码从 1 开始，默认每页 20 条，最大 100 条。筛选和页码保存在 URL，改变筛选回到第一页。
- 分类和标签统一调用 `src/api/dictionaries.ts`。空选项与加载失败分别展示，不再回退到硬编码数据。
- 我的招募使用 `GET /api/recruitments/mine` 返回的数组；按状态切换属于该列表的本地展示，不是大厅筛选分页。
- 草稿允许编辑全部字段；招募中只提交 `description/requirements/tagIds/deadlineAt/maxMembers`，核心字段只读；已成队和已关闭不可编辑。
- 删除草稿会确认后调用 `DELETE /api/recruitments/{id}`，失败保留记录；发布和关闭使用正式接口。
- 技能标签最多 20 项，取消全部选择并保存可清空。旧技能名称无法唯一匹配选项时保留原值，避免错误覆盖。
- 申请、收藏、私聊与消息仍未开放，本次没有实现这些功能。

招募验收：检查 Network 中大厅查询参数和分页结果；返回列表保留筛选；
创建草稿 → 编辑 → 发布 → 招募中受限编辑 → 关闭；另建草稿测试删除及失败反馈。
检查字典查询失败显示重试且没有假选项，401/403/404/409 按实际错误处理。
可运行 `node scripts/check-adaptation.mjs` 验证 API 契约及请求字段；它使用本地契约服务，不代表真实后端联调通过。

用户模块手动验收：

1. 访问 `/profile`，未登录应跳转 `/login?redirect=/profile`。
2. 注册时检查空白昵称、密码长度和两次密码一致性；注册成功进入目标页。
3. 登录错误密码应留在表单显示错误；正确密码登录后刷新，身份保持。
4. 修改个人资料后保存，顶部昵称同步更新，再刷新确认后端已保存。
5. 中断服务后刷新，身份验证失败显示重试，不能将失败解释为成功或清除 Token。
6. 使用过期 Token 访问受限接口，应清理登录并返回登录页；退出后再次访问个人资料也需登录。

### 首次启动

环境要求：Node.js `^22.18.0 || >=24.12.0`，使用 npm 管理依赖。

```powershell
git clone <仓库地址>
cd TeamFlow/frontend
npm ci
npm run dev
```

### 常用命令

以下命令均在 `frontend/` 下执行：

| 命令 | 用途 |
|---|---|
| `npm ci` | 按锁文件安装依赖，首次克隆或依赖变更后使用 |
| `npm run dev` | 启动开发服务 |
| `npm run type-check` | 检查 TypeScript 类型 |
| `npm run lint:check` | 检查代码规范，不自动修改 |
| `npm run format:check` | 检查格式，不自动修改 |
| `npm run lint` | 自动修复可修复的代码规范问题 |
| `npm run format` | 自动格式化 `src/` |
| `npm run build` | 类型检查并构建 |
| `npm run preview` | 本地预览构建产物，需先运行构建 |

添加依赖使用 `npm install <包名>`，开发工具使用 `npm install -D <包名>`，同时提交 `package.json` 和 `package-lock.json`。

### 代码放置约定

| 目录 | 内容 |
|---|---|
| `src/views/` | 按业务模块组织的页面 |
| `src/components/` | 多个页面共用的组件 |
| `src/layouts/` | 导航、侧边栏等公共布局 |
| `src/router/` | 路由配置 |
| `src/stores/` | Pinia 共享状态 |
| `src/api/` | Axios 实例与业务接口 |
| `src/types/` | 公共 TypeScript 类型 |
| `src/utils/` | 通用工具函数 |
