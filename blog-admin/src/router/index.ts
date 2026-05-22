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
        { path: 'content/collection', component: () => import('@/views/content/collection/index.vue') },
        { path: 'content/tag', component: () => import('@/views/content/tag/index.vue') },
        { path: 'content/comment', component: () => import('@/views/content/comment/index.vue') },
        { path: 'content/banner', component: () => import('@/views/content/banner/index.vue') },
        { path: 'system/admin', component: () => import('@/views/system/admin/index.vue') },
        { path: 'system/role', component: () => import('@/views/system/role/index.vue') },
        { path: 'system/menu', component: () => import('@/views/system/menu/index.vue') },
        { path: 'system/user', component: () => import('@/views/system/user/index.vue') },
        { path: 'system/config', component: () => import('@/views/system/config/index.vue') },
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
