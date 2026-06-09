<template>
  <div class="page">
    <div class="summary-grid">
      <div class="metric-card">
        <span>今日请求</span>
        <strong>{{ summary?.todayRequestCount ?? 0 }}</strong>
      </div>
      <div class="metric-card warn">
        <span>今日 WARN</span>
        <strong>{{ summary?.warnCount ?? 0 }}</strong>
      </div>
      <div class="metric-card error">
        <span>今日 ERROR</span>
        <strong>{{ summary?.errorCount ?? 0 }}</strong>
      </div>
      <div class="metric-card slow">
        <span>慢请求</span>
        <strong>{{ summary?.slowRequestCount ?? 0 }}</strong>
      </div>
    </div>

    <div class="page-card status-card">
      <div>
        <span class="label">最近启动</span>
        <p>{{ summary?.lastStartTime || '-' }}</p>
      </div>
      <div>
        <span class="label">最近关闭</span>
        <p>{{ summary?.lastShutdownTime || '-' }}</p>
      </div>
      <div class="log-path">
        <span class="label">日志文件</span>
        <p>{{ summary?.logFilePath || '-' }}</p>
      </div>
    </div>

    <div class="analysis-grid">
      <div class="page-card">
        <div class="section-head">
          <h3>慢请求</h3>
          <el-input-number v-model="slowThreshold" :min="1" :max="10000" controls-position="right" size="small" />
        </div>
        <el-table :data="slowRequests" v-loading="loading" class="custom-table" height="330">
          <el-table-column label="时间" prop="time" width="170" />
          <el-table-column label="方法" prop="method" width="80" />
          <el-table-column label="路径" prop="path" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" prop="status" width="80" />
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">{{ row.costTime }} ms</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-card">
        <div class="section-head">
          <h3>接口访问排行</h3>
          <el-button :icon="Refresh" size="small" @click="loadAll">刷新</el-button>
        </div>
        <el-table :data="topApis" v-loading="loading" class="custom-table" height="330">
          <el-table-column label="接口" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="method">{{ row.method }}</span>{{ row.path }}
            </template>
          </el-table-column>
          <el-table-column label="次数" prop="requestCount" width="80" />
          <el-table-column label="平均耗时" width="100">
            <template #default="{ row }">{{ row.averageCostTime }} ms</template>
          </el-table-column>
          <el-table-column label="最大耗时" width="100">
            <template #default="{ row }">{{ row.maxCostTime }} ms</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="page-card log-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="keyword" placeholder="关键词搜索" clearable style="width: 240px" @keyup.enter="loadLogs" />
          <el-select v-model="level" placeholder="日志级别" clearable style="width: 130px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
            <el-option label="DEBUG" value="DEBUG" />
          </el-select>
          <el-input-number v-model="lineCount" :min="50" :max="1000" :step="50" controls-position="right" />
        </div>
        <div class="actions">
          <el-button :icon="Search" @click="loadLogs">查询</el-button>
          <el-button :icon="Refresh" type="primary" @click="resetLogs">重置</el-button>
        </div>
      </div>

      <el-table :data="logs" v-loading="logLoading" class="custom-table">
        <el-table-column label="时间" prop="time" width="180" />
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ row.level || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="线程" prop="thread" width="170" show-overflow-tooltip />
        <el-table-column label="日志内容" min-width="420" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.method" class="method">{{ row.method }}</span>
            <span>{{ row.message || row.raw }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costTime ? `${row.costTime} ms` : '-' }}</template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="130" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  getRuntimeLogRecent,
  getRuntimeLogSlowRequests,
  getRuntimeLogSummary,
  getRuntimeLogTopApis,
  searchRuntimeLog,
  type RuntimeLogApiMetric,
  type RuntimeLogLine,
  type RuntimeLogSlowRequest,
  type RuntimeLogSummary,
} from '@/api/runtimeLog'

const loading = ref(false)
const logLoading = ref(false)
const summary = ref<RuntimeLogSummary | null>(null)
const slowRequests = ref<RuntimeLogSlowRequest[]>([])
const topApis = ref<RuntimeLogApiMetric[]>([])
const logs = ref<RuntimeLogLine[]>([])
const keyword = ref('')
const level = ref('')
const lineCount = ref(200)
const slowThreshold = ref(100)

async function loadSummary() {
  const res = await getRuntimeLogSummary({ slowThreshold: slowThreshold.value })
  summary.value = res.data
}

async function loadAnalysis() {
  const [slowRes, topRes] = await Promise.all([
    getRuntimeLogSlowRequests({ threshold: slowThreshold.value, limit: 10 }),
    getRuntimeLogTopApis({ limit: 10 }),
  ])
  slowRequests.value = slowRes.data
  topApis.value = topRes.data
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadAnalysis()])
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      level: level.value || undefined,
      lines: lineCount.value,
    }
    const res = keyword.value ? await searchRuntimeLog(params) : await getRuntimeLogRecent(params)
    logs.value = res.data
  } finally {
    logLoading.value = false
  }
}

function resetLogs() {
  keyword.value = ''
  level.value = ''
  lineCount.value = 200
  loadLogs()
}

function levelTag(value: string) {
  if (value === 'ERROR') return 'danger'
  if (value === 'WARN') return 'warning'
  if (value === 'INFO') return 'success'
  return 'info'
}

watch(slowThreshold, loadAll)
onMounted(() => {
  loadAll()
  loadLogs()
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 18px; }
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.metric-card {
  min-height: 104px;
  padding: 18px 20px;
  border: 1px solid rgba(232, 237, 246, .88);
  border-radius: 16px;
  background: rgba(255, 255, 255, .92);
  box-shadow: var(--shadow);
}
.metric-card span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}
.metric-card strong {
  display: block;
  margin-top: 12px;
  color: var(--text-main);
  font-size: 30px;
  line-height: 1;
}
.metric-card.warn strong { color: #b76b13; }
.metric-card.error strong { color: #d63838; }
.metric-card.slow strong { color: #1d4ed8; }
.status-card {
  display: grid;
  grid-template-columns: 220px 220px minmax(0, 1fr);
  gap: 18px;
}
.label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}
.status-card p {
  color: var(--text-main);
  line-height: 1.5;
  word-break: break-all;
}
.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}
.section-head,
.toolbar,
.filters,
.actions {
  display: flex;
  align-items: center;
}
.section-head {
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-head h3 {
  color: var(--text-main);
  font-size: 16px;
}
.toolbar {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.filters,
.actions {
  flex-wrap: wrap;
  gap: 10px;
}
.method {
  display: inline-flex;
  min-width: 52px;
  margin-right: 8px;
  color: var(--primary);
  font-weight: 800;
}
@media (max-width: 1100px) {
  .summary-grid,
  .analysis-grid,
  .status-card {
    grid-template-columns: 1fr;
  }
}
</style>
