<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索合集名" clearable style="width: 220px" :prefix-icon="Search" />
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增合集</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="row.cover"
              style="width: 48px; height: 36px; border-radius: 6px; object-fit: cover"
              fit="cover"
            />
            <div v-else class="img-placeholder">
              <el-icon color="#d1d5db"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="合集名称" prop="name" min-width="160" />
        <el-table-column label="简介" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="文章数" prop="articleCount" width="90" align="center" />
        <el-table-column label="排序" prop="sort" width="80" align="center" />
        <el-table-column label="创建时间" prop="createTime" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next" background />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑合集' : '新增合集'" width="480px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px" class="dialog-form">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入合集名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="form.cover" placeholder="图片地址（暂时手动填写）" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="合集简介" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
import { Search, Plus, Picture } from '@element-plus/icons-vue'
import { getCollectionPage, saveCollection, updateCollection, deleteCollection, type Collection } from '@/api/collection'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableData = ref<Collection[]>([])

const form = reactive({ id: null as null | number, name: '', cover: '', description: '', sort: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入合集名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res = await getCollectionPage({
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

watch(keyword, () => { page.value = 1; loadData() })
watch([page, pageSize], loadData)
onMounted(loadData)

function openDialog(row?: any) {
  row
    ? Object.assign(form, { id: row.id, name: row.name, cover: row.cover, description: row.description, sort: row.sort })
    : Object.assign(form, { id: null, name: '', cover: '', description: '', sort: 0 })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto = { name: form.name, cover: form.cover, description: form.description, sort: form.sort }
    if (form.id) {
      await updateCollection(form.id, dto)
      ElMessage.success('编辑成功')
    } else {
      await saveCollection(dto)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除合集「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCollection(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.img-placeholder { width: 48px; height: 36px; border-radius: 6px; background: var(--main-bg); display: flex; align-items: center; justify-content: center; }
.dialog-form { padding: 8px 0; }
</style>
