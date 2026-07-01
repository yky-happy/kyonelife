<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索角色编码/名称" clearable style="width: 240px" :prefix-icon="Search" />
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="角色编码" prop="code" width="150" />
        <el-table-column label="角色名称" prop="name" min-width="150" />
        <el-table-column label="描述" prop="remarks" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
              <el-button text type="warning" size="small" @click="openAssign(row)">分配菜单</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="420px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="dialog-form">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如 EDITOR" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如 编辑员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限 -->
    <el-dialog v-model="assignVisible" :title="`分配菜单权限 - ${assignRole?.name ?? ''}`" width="420px" align-center>
      <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox check-strictly node-key="id"
        default-expand-all :props="{ label: 'title', children: 'children' }" v-loading="assignLoading"
        style="max-height: 420px; overflow: auto" />
      <template #footer>
        <el-button @click="assignVisible = false">取 消</el-button>
        <el-button type="primary" :loading="assignSaving" @click="handleAssign">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  getRolePage, saveRole, updateRole, deleteRole, getRoleMenus, assignRoleMenus, type Role,
} from '@/api/role'
import { getMenuTree, type MenuNode } from '@/api/menu'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<Role[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: null as null | number, code: '', name: '', remarks: '' })
const rules: FormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

// 分配菜单
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignSaving = ref(false)
const assignRole = ref<Role | null>(null)
const menuTree = ref<MenuNode[]>([])
const menuTreeRef = ref<any>()

async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage({ page: page.value, size: pageSize.value, keyword: keyword.value || undefined })
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
  row ? Object.assign(form, { id: row.id, code: row.code, name: row.name, remarks: row.remarks })
      : Object.assign(form, { id: null, code: '', name: '', remarks: '' })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto = { code: form.code, name: form.name, remarks: form.remarks }
    if (form.id) { await updateRole(form.id, dto); ElMessage.success('编辑成功') }
    else { await saveRole(dto); ElMessage.success('新增成功') }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openAssign(row: Role) {
  assignRole.value = row
  assignVisible.value = true
  assignLoading.value = true
  try {
    if (menuTree.value.length === 0) {
      menuTree.value = (await getMenuTree()).data
    }
    const checked = (await getRoleMenus(row.id)).data as number[]
    // 等树渲染后回填勾选
    setTimeout(() => menuTreeRef.value?.setCheckedKeys(checked, false), 0)
  } finally {
    assignLoading.value = false
  }
}

async function handleAssign() {
  if (!assignRole.value) return
  assignSaving.value = true
  try {
    const menuIds = menuTreeRef.value?.getCheckedKeys(false) as number[]
    await assignRoleMenus(assignRole.value.id, menuIds)
    ElMessage.success('权限已更新')
    assignVisible.value = false
  } finally {
    assignSaving.value = false
  }
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
