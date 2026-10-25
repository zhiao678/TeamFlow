import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'projects',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '项目' },
        },
        {
          path: 'tasks',
          component: () => import('@/views/task/MyTasksView.vue'),
          meta: { title: '我的任务' },
        },
        {
          path: 'notifications',
          component: () => import('@/views/notification/NotificationView.vue'),
          meta: { title: '消息' },
        },
        {
          path: ':pathMatch(.*)*',
          component: () => import('@/views/NotFoundView.vue'),
          meta: { title: '页面未找到' },
        },
      ],
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
