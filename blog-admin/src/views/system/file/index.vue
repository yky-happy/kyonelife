<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="dir" placeholder="全部目录" clearable style="width: 160px" @change="loadData">
            <el-option v-for="d in dirs" :key="d" :label="d" :value="d" />
          </el-select>
          <span class="tip">MinIO 对象存储文件管理</span>
        </div>
        <el-upload :show-file-list="false" :http-request="handleUpload"
          accept="image/jpeg,image/png,image/webp,image/gif">
          <el-button type="primary" :icon="Upload" :loading="uploading">上传图片</el-button>
        </el-upload>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <el-image :src="row.url" style="width: 64px; height: 44px; border-radius: 6px"
              fit="cover" :preview-src-list="[row.url]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="对象名" prop="objectName" min-width="260" show-overflow-tooltip />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="修改时间" prop="lastModified" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" size="small" @click="copyUrl(row.url)">复制链接</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getFileList, deleteFileObject, uploadImage, type FileItem } from '@/api/file'

const dirs = ['article', 'collection', 'banner', 'avatar', 'config', 'video']
const dir = ref('')
const loading = ref(false)
const uploading = ref(false)
const tableData = ref<FileItem[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getFileList({ dir: dir.value || undefined, limit: 200 })
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    await uploadImage(options.file, dir.value || 'article')
    ElMessage.success('上传成功')
    loadData()
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row: FileItem) {
  await ElMessageBox.confirm(`确认删除「${row.objectName}」？删除后引用此文件的内容将失效。`, '提示', { type: 'warning' })
  await deleteFileObject(row.objectName)
  ElMessage.success('删除成功')
  loadData()
}

function copyUrl(url: string) {
  navigator.clipboard?.writeText(url)
  ElMessage.success('链接已复制')
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.tip { font-size: 13px; color: var(--text-muted); }
.custom-table { border-radius: 8px; overflow: hidden; }
</style>
