<template>
  <header class="header">
    <div class="header-left">
      <button class="hamburger" type="button" aria-label="展开菜单" @click="emit('toggleSidebar')">
        <span></span><span></span><span></span>
      </button>
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

const emit = defineEmits<{ toggleSidebar: [] }>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const nickname = computed(() => auth.nickname || '管理员')

const titleMap: Record<string, { group: string; title: string }> = {
  '/dashboard':           { group: '概览',     title: '仪表盘' },
  '/content/article':     { group: '内容管理', title: '文章管理' },
  '/content/article/edit': { group: '内容管理', title: '写文章' },
  '/content/agent':        { group: '内容管理', title: 'AI 创作助手' },
  '/content/collection':  { group: '内容管理', title: '合集管理' },
  '/content/tag':         { group: '内容管理', title: '标签管理' },
  '/content/banner':      { group: '内容管理', title: '轮播管理' },
  '/system/admin':        { group: '系统管理', title: '管理员' },
  '/system/role':         { group: '系统管理', title: '角色管理' },
  '/system/menu':         { group: '系统管理', title: '菜单管理' },
  '/system/user':         { group: '系统管理', title: '用户管理' },
  '/system/config':       { group: '系统管理', title: '网站配置' },
  '/system/file':         { group: '系统管理', title: '文件管理' },
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
  background: var(--header-bg);
  border-bottom: 1px solid var(--line-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 22px;
  flex-shrink: 0;
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ☰ 汉堡按钮：默认隐藏，窄屏显示 */
.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 38px;
  height: 38px;
  padding: 0 9px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--glass-bg);
  cursor: pointer;
  transition: all .16s;
}

.hamburger span {
  display: block;
  height: 2px;
  border-radius: 2px;
  background: var(--ink);
  transition: all .16s;
}

.hamburger:hover {
  border-color: var(--accent);
}

.hamburger:hover span {
  background: var(--accent-deep);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1024px) {
  .hamburger { display: flex; }
}

.breadcrumb-root {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}

.breadcrumb-current {
  font-size: 14px;
  font-weight: 800;
  color: var(--ink-strong);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 7px 10px 7px 7px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--line-soft);
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 6px 16px rgba(28, 37, 48, .05);
  transition: all 0.16s;
}

.user-info:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(28, 37, 48, .08);
}

.avatar {
  width: 30px;
  height: 30px;
  background: var(--active-bg);
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
