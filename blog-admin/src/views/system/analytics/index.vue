<template>
  <div class="page">
    <div class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="metric-card">
        <div class="metric-icon" :style="{ background: item.bg }">
          <el-icon :size="20" :color="item.color"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <div class="page-card">
        <div class="section-head">
          <h3>最近 7 天访问趋势</h3>
          <el-button :icon="Refresh" size="small" @click="loadData">刷新</el-button>
        </div>
        <div v-loading="loading" class="trend-chart">
          <div v-for="item in trend" :key="item.date" class="trend-item">
            <div class="bars">
              <div class="bar pv" :style="{ height: barHeight(item.pv) }"></div>
              <div class="bar uv" :style="{ height: barHeight(item.uv) }"></div>
            </div>
            <span class="trend-date">{{ item.date.slice(5) }}</span>
            <span class="trend-count">{{ item.pv }} / {{ item.uv }}</span>
          </div>
        </div>
        <div class="legend">
          <span><i class="pv-dot"></i>PV</span>
          <span><i class="uv-dot"></i>UV</span>
        </div>
      </div>

      <div class="page-card">
        <div class="section-head">
          <h3>热门文章 Top 10</h3>
          <span class="sub-text">近 7 天 article_view</span>
        </div>
        <el-table :data="hotArticles" v-loading="loading" class="custom-table" height="360">
          <el-table-column label="排名" width="70">
            <template #default="{ $index }">
              <span class="rank">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="文章" prop="title" min-width="220" show-overflow-tooltip />
          <el-table-column label="浏览" prop="viewCount" width="90" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAnalyticsOverview,
  getAnalyticsTrend,
  getHotArticles,
  type AnalyticsOverview,
  type AnalyticsTrend,
  type HotArticle,
} from '@/api/analytics'

const loading = ref(false)
const overview = ref<AnalyticsOverview>({
  todayPv: 0,
  todayUv: 0,
  totalPv: 0,
  articleViewCount: 0,
})
const trend = ref<AnalyticsTrend[]>([])
const hotArticles = ref<HotArticle[]>([])

const summaryCards = computed(() => [
  { label: '今日 PV', value: overview.value.todayPv, icon: 'View', color: '#2f6df6', bg: '#eef5ff' },
  { label: '今日 UV', value: overview.value.todayUv, icon: 'User', color: '#15b8a6', bg: '#ecfdf8' },
  { label: '累计 PV', value: overview.value.totalPv, icon: 'DataLine', color: '#ff8a5b', bg: '#fff3ec' },
  { label: '文章浏览', value: overview.value.articleViewCount, icon: 'Document', color: '#7c6df2', bg: '#f3f1ff' },
])

const maxTrendValue = computed(() => {
  const max = Math.max(...trend.value.flatMap((item) => [item.pv, item.uv]), 0)
  return max || 1
})

function barHeight(value: number) {
  const percent = Math.max(8, Math.round((value / maxTrendValue.value) * 100))
  return `${percent}%`
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, trendRes, hotRes] = await Promise.all([
      getAnalyticsOverview(),
      getAnalyticsTrend({ days: 7 }),
      getHotArticles({ days: 7, limit: 10 }),
    ])
    overview.value = overviewRes.data
    trend.value = trendRes.data
    hotArticles.value = hotRes.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 18px; }
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 104px;
  padding: 18px 20px;
  border: 1px solid rgba(232, 237, 246, .88);
  border-radius: 16px;
  background: rgba(255, 255, 255, .92);
  box-shadow: var(--shadow);
}
.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  flex-shrink: 0;
}
.metric-card span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}
.metric-card strong {
  display: block;
  margin-top: 8px;
  color: var(--text-main);
  font-size: 28px;
  line-height: 1;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, .8fr);
  gap: 18px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.section-head h3 {
  color: var(--text-main);
  font-size: 16px;
}
.sub-text {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}
.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: end;
  min-height: 320px;
  gap: 12px;
  padding: 12px 4px 0;
}
.trend-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
}
.bars {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 230px;
  padding: 0 8px;
  border-bottom: 1px solid var(--border);
}
.bar {
  width: 18px;
  min-height: 8px;
  border-radius: 8px 8px 0 0;
  transition: height .2s ease;
}
.bar.pv { background: linear-gradient(180deg, #2f6df6 0%, #7aa6ff 100%); }
.bar.uv { background: linear-gradient(180deg, #15b8a6 0%, #7fe0d2 100%); }
.trend-date {
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}
.trend-count {
  margin-top: 4px;
  color: var(--text-main);
  font-size: 12px;
}
.legend {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}
.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}
.pv-dot { background: #2f6df6; }
.uv-dot { background: #15b8a6; }
.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef5ff;
  color: #1d4ed8;
  font-weight: 800;
}
@media (max-width: 1100px) {
  .summary-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
