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
  '/content/article/edit': { group: '内容管理', title: '写文章' },
  '/content/collection':  { group: '内容管理', title: '合集管理' },
  '/content/tag':         { group: '内容管理', title: '标签管理' },
  '/content/comment':     { group: '内容管理', title: '评论管理' },
  '/content/banner':      { group: '内容管理', title: '轮播管理' },
  '/system/admin':        { group: '系统管理', title: '管理员' },
  '/system/role':         { group: '系统管理', title: '角色管理' },
  '/system/menu':         { group: '系统管理', title: '菜单管理' },
  '/system/user':         { group: '系统管理', title: '用户管理' },
  '/system/config':       { group: '系统管理', title: '网站配置' },
  '/system/operation-log': { group: '系统管理', title: '操作日志' },
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
  background: rgba(255, 255, 255, .72);
  border-bottom: 1px solid rgba(232, 237, 246, .8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 22px;
  flex-shrink: 0;
  backdrop-filter: blur(18px);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-root {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}

.breadcrumb-current {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-main);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 7px 10px 7px 7px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(20, 32, 51, .06);
  transition: all 0.16s;
}

.user-info:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(20, 32, 51, .09);
}

.avatar {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #2f6df6, #15b8a6);
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
