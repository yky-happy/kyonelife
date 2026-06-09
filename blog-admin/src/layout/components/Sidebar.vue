<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      <div class="logo-icon">
        <el-icon :size="22" color="#ffffff"><EditPen /></el-icon>
      </div>
      <div class="logo-text">
        <span class="logo-title">kyonelife</span>
        <span class="logo-sub">Content Studio</span>
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
      { path: '/system/operation-log', title: '操作日志', icon: 'Tickets' },
      { path: '/system/runtime-log', title: '运行日志', icon: 'Monitor' },
      { path: '/system/analytics', title: '数据看板', icon: 'DataAnalysis' },
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
  border-right: 1px solid rgba(232, 237, 246, .82);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
  box-shadow: 18px 0 50px rgba(20, 32, 51, .05);
  backdrop-filter: blur(18px);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 14px 16px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #2f6df6 0%, #15b8a6 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 14px 28px rgba(47, 109, 246, .22);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: 0;
}

.logo-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #6f7f96;
  letter-spacing: 0;
}

.sidebar-nav {
  padding: 8px 14px 20px;
  flex: 1;
}

.nav-group {
  margin-bottom: 4px;
}

.nav-group-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  letter-spacing: 0;
  padding: 16px 8px 7px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 9px 11px;
  border-radius: 11px;
  color: #64728a;
  text-decoration: none;
  font-size: 13px;
  font-weight: 650;
  transition: all 0.18s ease;
  margin-bottom: 3px;
}

.nav-item:hover {
  background: rgba(47, 109, 246, .08);
  color: var(--primary-hover);
}

.nav-item:hover .el-icon {
  color: var(--primary);
}

.nav-item.active {
  background: var(--active-bg);
  color: var(--active-text);
  box-shadow: 0 12px 26px rgba(47, 109, 246, .22);
}

.nav-item.active .el-icon {
  color: var(--active-text) !important;
}
</style>
