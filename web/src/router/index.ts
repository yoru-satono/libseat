import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    // Auth pages
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/auth/LoginPage.vue'),
      meta: { layout: 'auth', guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/auth/RegisterPage.vue'),
      meta: { layout: 'auth', guest: true },
    },
    {
      path: '/activate',
      name: 'activate',
      component: () => import('@/pages/auth/ActivatePage.vue'),
      meta: { layout: 'auth' },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/pages/auth/ForgotPasswordPage.vue'),
      meta: { layout: 'auth' },
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/pages/auth/ResetPasswordPage.vue'),
      meta: { layout: 'auth' },
    },
    {
      path: '/email/confirm',
      name: 'email-confirm',
      component: () => import('@/pages/auth/EmailConfirmPage.vue'),
      meta: { layout: 'auth' },
    },

    // Public user pages
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/user/HomePage.vue'),
      meta: { layout: 'user' },
    },
    {
      path: '/seats',
      name: 'seats',
      component: () => import('@/pages/user/SeatsPage.vue'),
      meta: { layout: 'user' },
    },

    // Authenticated user pages
    {
      path: '/reservations',
      name: 'reservations',
      component: () => import('@/pages/user/ReservationsPage.vue'),
      meta: { layout: 'user', requiresAuth: true },
    },
    {
      path: '/reservations/:id',
      name: 'reservation-detail',
      component: () => import('@/pages/user/ReservationDetailPage.vue'),
      meta: { layout: 'user', requiresAuth: true },
    },
    {
      path: '/waitlists',
      name: 'waitlists',
      component: () => import('@/pages/user/WaitlistsPage.vue'),
      meta: { layout: 'user', requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/pages/user/NotificationsPage.vue'),
      meta: { layout: 'user', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/pages/user/ProfilePage.vue'),
      meta: { layout: 'user', requiresAuth: true },
    },

    // Admin pages — nested under AdminLayout for persistent sidebar
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/pages/admin/DashboardPage.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/pages/admin/UsersPage.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'users/:id',
          name: 'admin-user-detail',
          component: () => import('@/pages/admin/UserDetailPage.vue'),
          meta: { title: '用户详情' },
        },
        {
          path: 'reservations',
          name: 'admin-reservations',
          component: () => import('@/pages/admin/ReservationsPage.vue'),
          meta: { title: '预约管理' },
        },
        {
          path: 'change-requests',
          name: 'admin-change-requests',
          component: () => import('@/pages/admin/ChangeRequestsPage.vue'),
          meta: { title: '信息修改申请' },
        },
        {
          path: 'libraries',
          name: 'admin-libraries',
          component: () => import('@/pages/admin/LibrariesPage.vue'),
          meta: { title: '图书馆管理' },
        },
        {
          path: 'seats',
          name: 'admin-seats',
          component: () => import('@/pages/admin/SeatsPage.vue'),
          meta: { title: '座位管理' },
        },
        {
          path: 'system-rules',
          name: 'admin-system-rules',
          component: () => import('@/pages/admin/SystemRulesPage.vue'),
          meta: { title: '系统规则' },
        },
        {
          path: 'audit-logs',
          name: 'admin-audit-logs',
          component: () => import('@/pages/admin/AuditLogsPage.vue'),
          meta: { title: '审计日志' },
        },
      ],
    },

    // 404
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Restore user info if we have a token but no currentUser yet
  if (auth.isAuthenticated && !auth.currentUser) {
    try {
      await auth.fetchCurrentUser()
    } catch {
      auth.clearTokens()
      if (to.meta.requiresAuth || to.meta.requiresAdmin) return '/login'
    }
  }

  if (to.meta.requiresAdmin) {
    if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
    if (!auth.isAdmin) return { name: 'home' }
  } else if (to.meta.requiresAuth) {
    if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  } else if (to.meta.guest && auth.isAuthenticated) {
    return auth.isAdmin ? { name: 'admin-dashboard' } : { name: 'home' }
  }
})

export default router
