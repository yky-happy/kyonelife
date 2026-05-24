<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索标签名"
            clearable
            style="width: 220px"
            :prefix-icon="Search"
          />
        </div>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增标签</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id" class="custom-table">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="标签名" prop="name" min-width="160">
          <template #default="{ row }">
            <el-tag :color="row.color" effect="dark" style="border: none">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="颜色" prop="color" width="140">
          <template #default="{ row }">
            <div class="color-preview">
              <span class="color-dot" :style="{ background: row.color }" />
              <span class="color-text">{{ row.color }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文章数" prop="articleCount" width="100" align="center" />
        <el-table-column label="创建时间" prop="createTime" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑标签' : '新增标签'" width="420px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px" class="dialog-form">
        <el-form-item label="标签名" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <div class="color-row">
            <el-color-picker v-model="form.color" />
            <el-input v-model="form.color" placeholder="#409EFF" style="flex: 1" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getTagPage, saveTag, updateTag, deleteTag, type Tag } from '@/api/tag'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const tableData = ref<Tag[]>([])

const form = reactive({ id: null as null | number, name: '', color: '#14b8a6' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTagPage({
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

// 关键字变化时回到第一页重新搜索
watch(keyword, () => {
  page.value = 1
  loadData()
})

// 翻页或改每页条数时重新加载
watch([page, pageSize], loadData)

onMounted(loadData)

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, color: row.color })
  } else {
    Object.assign(form, { id: null, name: '', color: '#14b8a6' })
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto = { name: form.name, color: form.color }
    if (form.id) {
      await updateTag(form.id, dto)
      ElMessage.success('编辑成功')
    } else {
      await saveTag(dto)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除标签「${row.name}」？`, '提示', { type: 'warning' })
  await deleteTag(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page { height: 100%; }

.page-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.toolbar-left { display: flex; gap: 10px; }

.custom-table { border-radius: 8px; overflow: hidden; }

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.color-preview { display: flex; align-items: center; gap: 8px; }

.color-dot {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
  border: 1px solid rgba(0,0,0,.08);
}

.color-text { font-size: 12.5px; color: var(--text-muted); font-family: monospace; }

.color-row { display: flex; align-items: center; gap: 10px; }

.dialog-form { padding: 8px 0; }
</style>
