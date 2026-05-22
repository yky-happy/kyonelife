<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      <div class="logo-icon">
        <el-icon :size="22" color="#14b8a6"><EditPen /></el-icon>
      </div>
      <div class="logo-text">
        <span class="logo-title">kyonelife</span>
        <span class="logo-sub">博客管理</span>
      </div>
    </div>

    <nav class="sidebar-nav">
      <div v-for="group in menus" :key="group.group" class="nav-group">
        <p class="nav-group-label">{{ group.group }}</p>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon :size="17"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </router-link>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const menus = [
  {
    group: '概览',
    items: [
      { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
    ],
  },
  {
    group: '内容管理',
    items: [
      { path: '/content/article', title: '文章管理', icon: 'Document' },
      { path: '/content/collection', title: '合集管理', icon: 'Collection' },
      { path: '/content/tag', title: '标签管理', icon: 'PriceTag' },
      { path: '/content/comment', title: '评论管理', icon: 'ChatDotRound' },
      { path: '/content/banner', title: '轮播管理', icon: 'Picture' },
    ],
  },
  {
    group: '系统管理',
    items: [
      { path: '/system/admin', title: '管理员', icon: 'UserFilled' },
      { path: '/system/role', title: '角色管理', icon: 'Avatar' },
      { path: '/system/menu', title: '菜单管理', icon: 'Menu' },
      { path: '/system/user', title: '用户管理', icon: 'User' },
      { path: '/system/config', title: '网站配置', icon: 'Setting' },
    ],
  },
]

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--border);
}

.logo-icon {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #f0fdfa 0%, #ccfbf1 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid #99f6e4;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.3px;
}

.logo-sub {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.5px;
}

.sidebar-nav {
  padding: 12px 12px 20px;
  flex: 1;
}

.nav-group {
  margin-bottom: 4px;
}

.nav-group-label {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 14px 8px 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: #6b7280;
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.15s ease;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: var(--primary-light);
  color: var(--primary-hover);
}

.nav-item:hover .el-icon {
  color: var(--primary);
}

.nav-item.active {
  background: var(--active-bg);
  color: var(--active-text);
}

.nav-item.active .el-icon {
  color: var(--active-text) !important;
}
</style>
