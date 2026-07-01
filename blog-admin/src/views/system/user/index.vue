<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索账号/邮箱/昵称"
            clearable
            style="width: 240px"
            :prefix-icon="Search"
            @keyup.enter="reload"
            @clear="reload"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px" @change="reload">
            <el-option label="正常" :value="1" />
            <el-option label="封禁" :value="0" />
          </el-select>
          <el-button type="primary" @click="reload">查询</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="账号" prop="account" width="120" />
        <el-table-column label="邮箱" prop="email" min-width="180" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickname" min-width="120">
          <template #default="{ row }">{{ row.nickname || '—' }}</template>
        </el-table-column>
        <el-table-column label="IP归属地" prop="ipLocation" width="130">
          <template #default="{ row }">{{ row.ipLocation || '—' }}</template>
        </el-table-column>
        <el-table-column label="最后登录" prop="lastLoginTime" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
                {{ row.status === 1 ? '封禁' : '解封' }}
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
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
          @current-change="loadData"
          @size-change="reload"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUserPage, updateUserStatus, deleteUser, type ReaderItem } from '@/api/user'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<ReaderItem[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  loadData()
}

async function toggleStatus(row: ReaderItem) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${next === 0 ? '封禁' : '解封'}读者「${row.nickname || row.account}」？`,
    '提示',
    { type: 'warning' },
  )
  await updateUserStatus(row.id, next)
  ElMessage.success('操作成功')
  loadData()
}

async function handleDelete(row: ReaderItem) {
  await ElMessageBox.confirm(
    `确认删除读者「${row.nickname || row.account}」？其评论将一并删除。`,
    '提示',
    { type: 'warning' },
  )
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.toolbar-left { display: flex; gap: 10px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.table-actions { display: flex; gap: 4px; }
</style>
