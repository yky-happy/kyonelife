<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索评论内容"
            clearable
            style="width: 260px"
            :prefix-icon="Search"
            @keyup.enter="reload"
            @clear="reload"
          />
          <el-button type="primary" @click="reload">查询</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="文章" prop="articleTitle" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.articleTitle || `#${row.articleId}` }}</template>
        </el-table-column>
        <el-table-column label="评论者" prop="nickname" width="120" />
        <el-table-column label="内容" min-width="240">
          <template #default="{ row }">
            <el-tag v-if="row.parentId" size="small" type="info" effect="plain" style="margin-right: 6px">回复</el-tag>
            <span>{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
                {{ row.status === 1 ? '隐藏' : '显示' }}
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
import { getCommentPage, deleteComment, updateCommentStatus, type AdminCommentItem } from '@/api/comment'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<AdminCommentItem[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getCommentPage({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
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

async function toggleStatus(row: AdminCommentItem) {
  const next = row.status === 1 ? 0 : 1
  await updateCommentStatus(row.id, next)
  ElMessage.success('操作成功')
  loadData()
}

async function handleDelete(row: AdminCommentItem) {
  await ElMessageBox.confirm('确认删除该评论？其下回复将一并删除。', '提示', { type: 'warning' })
  await deleteComment(row.id)
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
