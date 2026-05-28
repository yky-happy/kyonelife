<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <span />
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
              <el-button text type="warning" size="small" @click="assignMenu(row)">分配菜单</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableData = ref<any[]>([])
const form = reactive({ id: null as null | number, code: '', name: '', remarks: '' })
const rules: FormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function openDialog(row?: any) {
  row ? Object.assign(form, row) : Object.assign(form, { id: null, code: '', name: '', remarks: '' })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function assignMenu(_row: any) { ElMessage.info('分配菜单功能待接入后端实现') }

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try { ElMessage.success(form.id ? '编辑成功' : '新增成功'); dialogVisible.value = false }
  finally { saving.value = false }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.dialog-form { padding: 8px 0; }
</style>
