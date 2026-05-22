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
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" type="primary" size="small" style="margin-right: 4px">{{ r }}</el-tag>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.id ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="form.id ? '不填则不修改密码' : '请输入密码'" show-password />
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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableData = ref<any[]>([])
const form = reactive({ id: null as null | number, username: '', nickname: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function openDialog(row?: any) {
  row ? Object.assign(form, { ...row, password: '' }) : Object.assign(form, { id: null, username: '', nickname: '', password: '' })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try { ElMessage.success(form.id ? '编辑成功' : '新增成功'); dialogVisible.value = false }
  finally { saving.value = false }
}

async function toggleStatus(row: any) {
  await ElMessageBox.confirm(`确认${row.status === 1 ? '禁用' : '启用'}该账号？`, '提示', { type: 'warning' })
  ElMessage.success('操作成功')
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除管理员「${row.username}」？`, '提示', { type: 'warning' })
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
.dialog-form { padding: 8px 0; }
</style>
