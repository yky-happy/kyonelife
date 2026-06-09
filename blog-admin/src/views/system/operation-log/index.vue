<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="filters.module" placeholder="模块" clearable style="width: 150px" />
          <el-input v-model="filters.operation" placeholder="操作类型" clearable style="width: 160px" />
          <el-select v-model="filters.success" placeholder="执行结果" clearable style="width: 130px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            style="width: 360px"
          />
        </div>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="时间" prop="createTime" min-width="170" />
        <el-table-column label="管理员" min-width="110">
          <template #default="{ row }">
            {{ row.adminName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="模块" prop="module" width="110" />
        <el-table-column label="操作" prop="operation" width="130" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costTime }} ms</template>
        </el-table-column>
        <el-table-column label="请求" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="method">{{ row.requestMethod }}</span>
            <span>{{ row.requestPath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="130" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="560px">
      <div v-if="current" class="detail">
        <div class="detail-row"><span>管理员</span><p>{{ current.adminName || '-' }}</p></div>
        <div class="detail-row"><span>模块</span><p>{{ current.module }}</p></div>
        <div class="detail-row"><span>操作</span><p>{{ current.operation }}</p></div>
        <div class="detail-row"><span>请求路径</span><p>{{ current.requestMethod }} {{ current.requestPath }}</p></div>
        <div class="detail-row"><span>响应</span><p>{{ current.responseCode }} {{ current.responseMessage }}</p></div>
        <div class="detail-row"><span>User-Agent</span><p>{{ current.userAgent || '-' }}</p></div>
        <div class="detail-block">
          <span>请求参数</span>
          <pre>{{ formatJson(current.requestParams) }}</pre>
        </div>
        <div v-if="current.errorMessage" class="detail-block">
          <span>异常信息</span>
          <pre>{{ current.errorMessage }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOperationLogPage, type OperationLog } from '@/api/operationLog'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<OperationLog[]>([])
const detailVisible = ref(false)
const current = ref<OperationLog | null>(null)
const timeRange = ref<[string, string] | null>(null)
const filters = reactive({
  module: '',
  operation: '',
  success: null as number | null,
})

async function loadData() {
  loading.value = true
  try {
    const res = await getOperationLogPage({
      page: page.value,
      size: pageSize.value,
      module: filters.module || undefined,
      operation: filters.operation || undefined,
      success: filters.success ?? undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1],
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.module = ''
  filters.operation = ''
  filters.success = null
  timeRange.value = null
  page.value = 1
  loadData()
}

function openDetail(row: OperationLog) {
  current.value = row
  detailVisible.value = true
}

function formatJson(value: string) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

watch([() => filters.module, () => filters.operation, () => filters.success, timeRange], () => {
  page.value = 1
  loadData()
})
watch([page, pageSize], loadData)
onMounted(loadData)
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.filters { display: flex; flex-wrap: wrap; gap: 10px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.method { display: inline-flex; min-width: 46px; margin-right: 8px; color: var(--primary); font-weight: 700; }
.detail { display: flex; flex-direction: column; gap: 14px; }
.detail-row span,
.detail-block span { display: block; margin-bottom: 6px; color: var(--text-muted); font-size: 12px; font-weight: 700; }
.detail-row p { margin: 0; color: var(--text-main); line-height: 1.6; word-break: break-all; }
pre {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.55;
}
</style>
