<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索评论内容" clearable style="width: 240px" :prefix-icon="Search" />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="评论内容" prop="content" min-width="260" show-overflow-tooltip />
        <el-table-column label="评论者" prop="nickname" width="120" />
        <el-table-column label="所属文章" prop="articleTitle" min-width="180" show-overflow-tooltip />
        <el-table-column label="IP归属地" prop="ipLocation" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
                {{ row.status === 1 ? '隐藏' : '显示' }}
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])

async function toggleStatus(row: any) {
  await ElMessageBox.confirm(`确认${row.status === 1 ? '隐藏' : '显示'}该评论？`, '提示', { type: 'warning' })
  ElMessage.success('操作成功')
}

async function handleDelete() {
  await ElMessageBox.confirm('确认删除该评论？此操作不可撤销', '提示', { type: 'warning' })
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.toolbar-left { display: flex; gap: 10px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>
