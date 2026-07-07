<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg }">
          <el-icon :size="20" :color="s.color"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ s.value }}</p>
          <p class="stat-label">{{ s.label }}</p>
        </div>
        <div class="stat-trend" :class="s.trend > 0 ? 'up' : 'flat'">
          <el-icon :size="12"><component :is="s.trend > 0 ? 'Top' : 'Minus'" /></el-icon>
          <span>{{ s.trend > 0 ? `+${s.trend}` : '暂无变化' }}</span>
        </div>
      </div>
    </div>

    <div class="cards-row">
      <div class="card">
        <div class="card-header">
          <h3>快捷操作</h3>
        </div>
        <div class="quick-actions">
          <router-link to="/content/article" class="quick-btn">
            <el-icon :size="18" color="#14b8a6"><Document /></el-icon>
            <span>写文章</span>
          </router-link>
          <router-link to="/content/tag" class="quick-btn">
            <el-icon :size="18" color="#8b5cf6"><PriceTag /></el-icon>
            <span>管理标签</span>
          </router-link>
          <router-link to="/content/collection" class="quick-btn">
            <el-icon :size="18" color="#f59e0b"><Collection /></el-icon>
            <span>管理合集</span>
          </router-link>
          <router-link to="/system/config" class="quick-btn">
            <el-icon :size="18" color="#ef4444"><Setting /></el-icon>
            <span>网站配置</span>
          </router-link>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>系统信息</h3>
        </div>
        <div class="sys-info">
          <div class="sys-row">
            <span class="sys-key">项目名称</span>
            <span class="sys-val">kyonelife Blog</span>
          </div>
          <div class="sys-row">
            <span class="sys-key">技术栈</span>
            <span class="sys-val">Spring Boot 3 · Vue 3</span>
          </div>
          <div class="sys-row">
            <span class="sys-key">数据库</span>
            <span class="sys-val">MySQL 8.0 · Redis</span>
          </div>
          <div class="sys-row">
            <span class="sys-key">接口文档</span>
            <a class="sys-link" :href="`${apiBase}/doc.html`" target="_blank">Knife4j 文档 →</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'

// 生产走同源（apiBase 为空 → /doc.html 由 nginx 反代到后端）；开发指向 localhost:8080
const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

const stats = ref([
  { label: '文章总数',  value: '--' as string | number, icon: 'Document',      color: '#2f6df6', bg: '#eef5ff', trend: 0 },
  { label: '标签数量',  value: '--' as string | number, icon: 'PriceTag',      color: '#15b8a6', bg: '#ecfdf8', trend: 0 },
  { label: '总浏览量',  value: '--' as string | number, icon: 'View',          color: '#ff8a5b', bg: '#fff3ec', trend: 0 },
  { label: '注册用户',  value: '--' as string | number, icon: 'User',          color: '#7c6df2', bg: '#f3f1ff', trend: 0 },
])

onMounted(async () => {
  const res = await getDashboardStats()
  const d = res.data
  stats.value[0].value = d.articleCount
  stats.value[1].value = d.tagCount
  stats.value[2].value = d.totalViews
  stats.value[3].value = d.userCount
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 18px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  background: var(--card-bg);
  border: 1px solid rgba(232, 237, 246, .88);
  border-radius: var(--radius);
  padding: 20px;
  box-shadow: var(--shadow);
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info { flex: 1; }

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-main);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-muted);
  white-space: nowrap;
}

.stat-trend {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  padding: 3px 7px;
  border-radius: 20px;
}

.stat-trend.up   { background: #f0fdf4; color: #16a34a; }
.stat-trend.flat { background: #f5f8fc; color: #8a97ad; }

.cards-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.card {
  background: var(--card-bg);
  border: 1px solid rgba(232, 237, 246, .88);
  border-radius: var(--radius);
  padding: 20px 24px;
  box-shadow: var(--shadow);
}

.card-header {
  margin-bottom: 18px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 14px;
}

.card-header h3 {
  font-size: 15px;
  font-weight: 800;
  color: var(--text-main);
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #f8fbff;
  text-decoration: none;
  color: var(--text);
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.15s;
}

.quick-btn:hover {
  background: #eef5ff;
  color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.sys-info { display: flex; flex-direction: column; gap: 12px; }

.sys-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13.5px;
}

.sys-row:last-child { border-bottom: none; }

.sys-key { color: var(--text-muted); }

.sys-val { font-weight: 650; color: var(--text-main); }

.sys-link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
}

.sys-link:hover { color: var(--primary-hover); }

@media (max-width: 960px) {
  .cards-row { grid-template-columns: 1fr; }
}
</style>
