<template>
  <header class="header">
    <div class="header-left">
      <nav class="breadcrumb">
        <span class="breadcrumb-root">{{ currentGroup }}</span>
        <el-icon :size="12" color="#d1d5db"><ArrowRight /></el-icon>
        <span class="breadcrumb-current">{{ currentTitle }}</span>
      </nav>
    </div>

    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <div class="avatar">{{ nickname.charAt(0) }}</div>
          <span class="username">{{ nickname }}</span>
          <el-icon :size="13" color="#9ca3af"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const nickname = computed(() => auth.nickname || '管理员')

const titleMap: Record<string, { group: string; title: string }> = {
  '/dashboard':           { group: '概览',     title: '仪表盘' },
  '/content/article':     { group: '内容管理', title: '文章管理' },
  '/content/collection':  { group: '内容管理', title: '合集管理' },
  '/content/tag':         { group: '内容管理', title: '标签管理' },
  '/content/comment':     { group: '内容管理', title: '评论管理' },
  '/content/banner':      { group: '内容管理', title: '轮播管理' },
  '/system/admin':        { group: '系统管理', title: '管理员' },
  '/system/role':         { group: '系统管理', title: '角色管理' },
  '/system/menu':         { group: '系统管理', title: '菜单管理' },
  '/system/user':         { group: '系统管理', title: '用户管理' },
  '/system/config':       { group: '系统管理', title: '网站配置' },
}

const currentGroup = computed(() => titleMap[route.path]?.group ?? '博客管理')
const currentTitle = computed(() => titleMap[route.path]?.title ?? '')

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    auth.clearAuth()
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  height: var(--header-height);
  background: var(--card-bg);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-root {
  font-size: 13px;
  color: var(--text-muted);
}

.breadcrumb-current {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background 0.15s;
}

.user-info:hover {
  background: var(--main-bg);
}

.avatar {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  text-transform: uppercase;
}

.username {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text);
}
</style>
