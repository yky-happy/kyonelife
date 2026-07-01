<template>
  <div class="page">
    <!-- 区域一：检索与操作 -->
    <div class="page-card filter-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索文章标题" clearable style="width: 240px" :prefix-icon="Search" />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="goWrite">写文章</el-button>
      </div>
    </div>

    <!-- 区域二：文章列表 -->
    <div class="page-card list-card">
      <div class="card-head">
        <h3>文章列表</h3>
        <span class="card-sub">共 {{ total }} 篇</span>
      </div>
      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image v-if="row.cover" :src="row.cover"
              style="width: 56px; height: 42px; border-radius: 6px" fit="cover" />
            <div v-else class="img-placeholder">
              <el-icon color="#d1d5db"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="合集" prop="collectionName" width="120" />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t.id" :color="t.color" effect="dark"
              size="small" style="margin-right: 4px; border: none">{{ t.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isStick" type="warning" size="small">置顶</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="阅读" prop="viewCount" width="80" />
        <el-table-column label="发布时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" width="218" fixed="right">
          <template #default="{ row }">
            <div class="table-actions article-actions">
              <el-button text type="primary" size="small" @click="goEdit(row)">编辑</el-button>
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small"
                @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '发布' }}</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Picture } from '@element-plus/icons-vue'
import {
  deleteArticle,
  getArticlePage,
  updateArticleStatus,
  type Article,
} from '@/api/article'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<Article[]>([])

function statusText(s: number) { return ['草稿', '已发布', '已下架'][s] ?? '-' }
function statusType(s: number) { return (['info', 'success', 'danger'] as const)[s] ?? 'info' }

function goWrite() { router.push('/content/article/edit') }
function goEdit(row: Article) { router.push(`/content/article/edit?id=${row.id}`) }

async function loadData() {
  loading.value = true
  try {
    const res = await getArticlePage({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value ?? undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

watch(keyword, () => { page.value = 1; loadData() })
watch(statusFilter, () => { page.value = 1; loadData() })
watch([page, pageSize], loadData)
onMounted(loadData)

async function toggleStatus(row: Article) {
  await ElMessageBox.confirm(`确认${row.status === 1 ? '下架' : '发布'}该文章？`, '提示', { type: 'warning' })
  await updateArticleStatus(row.id, row.status === 1 ? 2 : 1)
  ElMessage.success('操作成功')
  loadData()
}

async function handleDelete(row: Article) {
  await ElMessageBox.confirm(`确认删除文章「${row.title}」？`, '提示', { type: 'warning' })
  await deleteArticle(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; min-height: 100%; }
.filter-card .toolbar { margin-bottom: 0 !important; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; gap: 10px; }
.card-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 16px; }
.card-head h3 { font-size: 16px; font-weight: 750; color: var(--ink-strong); }
.card-sub { font-size: 12px; font-weight: 700; color: var(--text-muted); }
.custom-table { border-radius: var(--radius); overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.img-placeholder { width: 56px; height: 42px; border-radius: 8px; background: var(--main-bg); display: flex; align-items: center; justify-content: center; }
.text-muted { color: var(--text-muted); font-size: 12px; }
</style>
