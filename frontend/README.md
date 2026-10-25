# 前端说明

## 技术栈

暂定为 Vue3 + Vite  

完整技术栈：Vue 3、TypeScript、Vite、Vue Router、Pinia 和 Element Plus。

## 相关指引

### 当前页面与模拟数据

项目模块已接入真实后端接口，访问 /projects 需要后端服务；支持项目详情及概览、成员、任务入口。成员完整列表和正式任务子页面尚待接入。详见 [项目模块交接](docs/project-handoff.md)。以下为初始骨架说明，项目页面的数据来源以此更新为准。

首页跳转到 `/dashboard` 工作台。左侧导航可切换 `/projects` 项目、
`/tasks` 我的任务、`/notifications` 消息；这些页面无需启动后端即可展示。
工作台统计来自同一份模拟数据，任务页支持按状态筛选，消息页支持单条或全部标为已读，
顶部与工作台未读数量同步更新。刷新后消息状态重置。

公共布局位于 `src/layouts/MainLayout.vue`，侧栏与顶部栏位于
`src/components/layout/`；业务页面位于 `src/views/`，模拟数据统一位于
`src/mocks/data.ts`。项目和任务类型遵循 `docs/api-spec.md`，消息类型暂为前端展示约定。
后续联调时替换页面的数据来源，并复用 `src/api/request.ts`，当前页面不发送业务请求。
全局样式位于 `src/assets/styles/index.css`。

手动验收：依次切换四个导航，检查菜单高亮与顶部标题；直接访问并刷新各路由；
在任务页切换状态筛选；在消息页标记已读后返回工作台检查未读数；
使用顶部菜单按钮折叠侧栏，并在窄屏下检查布局。未知路径提供返回工作台入口。
正式部署使用 History 路由，需要服务器将前端页面路径回退到 `index.html`。

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
