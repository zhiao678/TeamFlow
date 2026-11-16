# 通知模块与业务页面对接

## 已实现范围

- `/notifications` 使用真实后端接口，路由需要登录。
- 服务端分页、全部/未读筛选、单条和全部标记已读，URL 保存页码、每页数量及筛选。
- 未读列表末页标记完成后回到有效页；操作成功但刷新失败时单独提示，不要求重复操作。
- 顶部、侧栏和工作台共用服务端 `unreadCount`，不是当前页未读条数。
- 登录或恢复身份后刷新未读数；退出、Token 清理和切换账号时清除通知状态并隔离旧请求。
- 标记失败保留原状态，未知事件使用通用文案。暂不使用轮询、WebSocket 或模拟通知。

## 文件与接口

| 文件 | 用途 |
| --- | --- |
| `src/api/notifications.ts` | 列表、单条已读、全部已读及错误映射 |
| `src/types/notification.ts` | 通知与分页类型，事件字符串允许后续扩展 |
| `src/stores/notifications.ts` | 真实列表、全局未读数及会话隔离 |
| `src/views/notification/NotificationView.vue` | 通知中心 |
| `src/utils/notification-target.ts` | 集中决定通知跳转目标 |

接口为 `GET /api/notifications?page=1&pageSize=20&unreadOnly=false`、
`PATCH /api/notifications/{id}/read`、`PATCH /api/notifications/read-all`。
`unreadOnly=false` 表示全部，当前没有“仅已读”接口，不在浏览器对单页做伪筛选。

## 申请/审核模块如何刷新未读数

```ts
import { useNotificationsStore } from '@/stores/notifications'

const notifications = useNotificationsStore()
// 提交、撤回、接受或拒绝申请的服务端写请求成功后：
void notifications.refreshUnreadCount()
```

该方法只更新全局计数，不覆盖通知页列表和分页条件；内部处理失败，不影响业务操作结果。
通知由后端创建，前端不要插入模拟消息。需要刷新通知列表时在通知页使用 `load()`；
通常离开业务页后重新进入通知中心即会查询，无需业务模块直接操作列表。
写操作实际通知的接收人由后端决定；当前账号计数不一定变化，不手动加减。

## 跳转与路由约定

| 通知 | 当前行为 |
| --- | --- |
| `APPLICATION_SUBMITTED / APPLICATION_WITHDRAWN` | 跳已有 `/my-recruitments`，提示用户从对应招募进入管理 |
| `APPLICATION_ACCEPTED / APPLICATION_REJECTED / APPLICATION_EXPIRED` | 存在 `/my-applications` 路由时跳转，携带 `applicationId` query |
| RECRUITMENT 目标且有合法招募 ID | 跳已有招募详情 |
| 未知目标、缺少合法 ID 或未接入的页面 | 保留通知内容并明确说明暂不支持跳转 |

当前申请通知只有 `applicationId`，`recruitmentId` 为 null，因此不猜测单条审核页面地址，
不增加未约定的申请详情查询接口。我的申请未注册时，结果通知不生成无效链接。
点击查看会尝试标记已读，标记失败仍可查看目标；会话失效则由公共鉴权跳转登录。
单条已读也可以独立操作，不要求用户必须进入目标页面。

业务模块新增 `/my-applications`、`/my-favorites` 后，侧栏会显示对应入口。
两页应设置 `meta.requiresAuth: true`；建议“我的申请”支持 `applicationId` query 定位。
目前不为另外两人的页面创建占位路由。后续若调整路由地址，需同步侧栏与跳转函数。

## 验证与限制

- 已有 `node scripts/check-notifications.mjs` 本地契约检查，覆盖分页、全局计数、失败反馈、末页回退、重复提交和账号隔离。
- 此脚本不等于真实后端联调；还需和业务模块使用两个账号验证“提交 → 通知 → 审核 → 结果通知”。
- 手动检查：游客访问跳登录、直接访问筛选 URL、刷新恢复筛选、分页、标记、空结果、失败重试、账号切换。
- 运行 `npm run lint:check`、`npm run format:check` 和 `npm run build`。
