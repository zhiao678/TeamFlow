import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/auth/AuthView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      component: () => import('@/views/auth/AuthView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'recruitments',
          name: 'recruitment-hall',
          component: () => import('@/views/recruitment/RecruitmentHallView.vue'),
          meta: { title: '招募大厅' },
        },
        {
          path: 'recruitments/new',
          name: 'recruitment-new',
          component: () => import('@/views/recruitment/RecruitmentEditorView.vue'),
          meta: { title: '发布招募', requiresAuth: true },
        },
        {
          path: 'recruitments/:id/edit',
          name: 'recruitment-edit',
          component: () => import('@/views/recruitment/RecruitmentEditorView.vue'),
          meta: { title: '编辑招募', requiresAuth: true },
        },
        {
          path: 'recruitments/:id',
          name: 'recruitment-detail',
          component: () => import('@/views/recruitment/RecruitmentDetailView.vue'),
          meta: { title: '招募详情' },
        },
        {
          path: 'my-recruitments',
          name: 'my-recruitments',
          component: () => import('@/views/recruitment/MyRecruitmentsView.vue'),
          meta: { title: '我的招募', requiresAuth: true },
        },
        {
          path: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台', requiresAuth: true },
        },
        {
          path: 'projects',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '项目', projectData: true },
        },
        {
          path: 'projects/:projectId',
          component: () => import('@/views/project/ProjectDetailView.vue'),
          meta: { title: '项目详情', projectData: true },
          children: [
            { path: '', redirect: (to) => ({ name: 'project-overview', params: to.params }) },
            {
              path: 'overview',
              name: 'project-overview',
              component: () => import('@/views/project/ProjectOverviewView.vue'),
              meta: { title: '项目概览' },
            },
            {
              path: 'members',
              name: 'project-members',
              component: () => import('@/views/project/ProjectMembersView.vue'),
              meta: { title: '项目成员' },
            },
            {
              path: 'tasks',
              name: 'project-tasks',
              component: () => import('@/views/task/ProjectTasksView.vue'),
              meta: { title: '项目任务' },
            },
          ],
        },
        {
          path: 'tasks',
          component: () => import('@/views/task/MyTasksView.vue'),
          meta: { title: '我的任务', requiresAuth: true },
        },
        {
          path: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '个人资料', requiresAuth: true },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/notification/NotificationView.vue'),
          meta: { title: '通知', requiresAuth: true },
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

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.restore()
  if (to.meta.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
