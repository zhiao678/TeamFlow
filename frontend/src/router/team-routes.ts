import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

export const teamRoutes = {
  myTeams: { name: 'my-teams', path: '/my-teams', title: '我的队伍' },
  conversations: { name: 'conversations', path: '/conversations', title: '会话列表' },
  conversation: { name: 'conversation', path: '/conversations/:id', title: '聊天' },
  memberExits: { name: 'my-member-exits', path: '/my-member-exits', title: '我的退出申请' },
  leaderTransfers: {
    name: 'my-leader-transfers',
    path: '/my-leader-transfers',
    title: '负责人转交',
  },
  teamManagement: {
    name: 'team-management',
    path: '/my-recruitments/:id/team-management',
    title: '队伍管理',
  },
} as const
export type TeamRouteKey = keyof typeof teamRoutes
type PageLoader = () => Promise<{ default: Component }>
/** 在 MainLayout.children 中展开；只注册已提供页面，不创建空页面。 */
export function createTeamRoutes(
  pages: Partial<Record<TeamRouteKey, PageLoader>>,
): RouteRecordRaw[] {
  return (Object.keys(teamRoutes) as TeamRouteKey[]).flatMap((key) => {
    const component = pages[key]
    const route = teamRoutes[key]
    return component
      ? [
          {
            path: route.path,
            name: route.name,
            component,
            meta: { title: route.title, requiresAuth: true },
          },
        ]
      : []
  })
}
export function teamRoutePath(key: TeamRouteKey, id?: number): string {
  const path = teamRoutes[key].path
  if (!path.includes(':id')) return path
  if (!Number.isSafeInteger(id) || id === undefined || id <= 0)
    throw new Error('路由编号必须为正整数')
  return path.replace(':id', String(id))
}
