<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 240px" :prefix-icon="Search" />
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增管理员</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="昵称" prop="nickname" min-width="120" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames" :key="r" type="primary" size="small" style="margin-right: 4px">{{ r }}</el-tag>
            <span v-if="!row.roleNames?.length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" prop="lastLoginTime" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="218" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next" background />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="440px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="dialog-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.id ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="form.id ? '不填则不修改密码' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
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
import {
  getAdminPage, saveAdmin, updateAdmin, updateAdminStatus, deleteAdmin, type AdminItem,
} from '@/api/admin'
import { getRoleList, type Role } from '@/api/role'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<AdminItem[]>([])
const roleOptions = ref<Role[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as null | number,
  username: '',
  nickname: '',
  password: '',
  roleIds: [] as number[],
})
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminPage({ page: page.value, size: pageSize.value, keyword: keyword.value || undefined })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

watch(keyword, () => { page.value = 1; loadData() })
watch([page, pageSize], loadData)
onMounted(async () => {
  roleOptions.value = (await getRoleList()).data
  loadData()
})

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, {
      id: row.id, username: row.username, nickname: row.nickname, password: '',
      roleIds: [...(row.roleIds ?? [])],
    })
  } else {
    Object.assign(form, { id: null, username: '', nickname: '', password: '', roleIds: [] })
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateAdmin(form.id, { nickname: form.nickname, password: form.password || undefined, roleIds: form.roleIds })
      ElMessage.success('编辑成功')
    } else {
      await saveAdmin({ username: form.username, nickname: form.nickname, password: form.password, roleIds: form.roleIds })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminItem) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${next === 0 ? '禁用' : '启用'}该账号？`, '提示', { type: 'warning' })
  await updateAdminStatus(row.id, next)
  ElMessage.success('操作成功')
  loadData()
}

async function handleDelete(row: AdminItem) {
  await ElMessageBox.confirm(`确认删除管理员「${row.username}」？`, '提示', { type: 'warning' })
  await deleteAdmin(row.id)
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
.dialog-form { padding: 8px 0; }
.muted { color: var(--text-muted); }
</style>
