import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../layout/index.vue'),
      children: [
        { path: '', name: 'home', component: () => import('../views/home/index.vue') },
        { path: 'article/:id', name: 'article-detail', component: () => import('../views/article/detail.vue') },
        { path: 'tags', name: 'tags', component: () => import('../views/tag/index.vue') },
        { path: 'tag/:id', name: 'tag-articles', component: () => import('../views/tag/articles.vue') },
        { path: 'collections', name: 'collections', component: () => import('../views/collection/index.vue') },
        { path: 'collection/:id', name: 'collection-detail', component: () => import('../views/collection/detail.vue') },
        { path: 'archives', name: 'archives', component: () => import('../views/archive/index.vue') },
        { path: 'about', name: 'about', component: () => import('../views/about/index.vue') },
        { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('../views/error/404.vue') },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
