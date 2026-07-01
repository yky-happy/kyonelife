import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/dashboard/index.vue') },
        { path: 'content/article', component: () => import('@/views/content/article/index.vue') },
        { path: 'content/article/edit', component: () => import('@/views/content/article/edit.vue') },
        { path: 'content/agent', component: () => import('@/views/content/agent/index.vue') },
        { path: 'content/collection', component: () => import('@/views/content/collection/index.vue') },
        { path: 'content/tag', component: () => import('@/views/content/tag/index.vue') },
        { path: 'content/banner', component: () => import('@/views/content/banner/index.vue') },
        { path: 'system/admin', component: () => import('@/views/system/admin/index.vue') },
        { path: 'system/role', component: () => import('@/views/system/role/index.vue') },
        { path: 'system/menu', component: () => import('@/views/system/menu/index.vue') },
        { path: 'system/user', component: () => import('@/views/system/user/index.vue') },
        { path: 'system/comment', component: () => import('@/views/system/comment/index.vue') },
        { path: 'system/config', component: () => import('@/views/system/config/index.vue') },
        { path: 'system/file', component: () => import('@/views/system/file/index.vue') },
        { path: 'system/operation-log', component: () => import('@/views/system/operation-log/index.vue') },
        { path: 'system/runtime-log', component: () => import('@/views/system/runtime-log/index.vue') },
        { path: 'system/analytics', component: () => import('@/views/system/analytics/index.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
})

export default router
