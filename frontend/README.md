# 前端说明

## 技术栈

暂定为 Vue3 + Vite  

完整技术栈：Vue 3、TypeScript、Vite、Vue Router、Pinia 和 Element Plus。

## 相关指引

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
