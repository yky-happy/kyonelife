<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <span class="toolbar-tip">维护后台菜单/权限树（目录 / 菜单 / 按钮三级）。</span>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增菜单</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id" default-expand-all
        :tree-props="{ children: 'children' }" class="custom-table">
        <el-table-column label="菜单名称" prop="title" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.type" size="small">{{ typeMap[row.type]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由路径" prop="path" min-width="170" />
        <el-table-column label="权限标识" prop="perm" min-width="150" />
        <el-table-column label="图标" prop="icon" width="110" />
        <el-table-column label="排序" prop="sort" width="70" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="480px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="dialog-form">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" placeholder="顶级菜单" style="width: 100%">
            <el-option label="顶级菜单" :value="0" />
            <el-option v-for="m in flatMenus" :key="m.id" :label="m.label" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="title">
          <el-input v-model="form.title" placeholder="菜单名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="CATALOG">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type !== 'BUTTON'" label="路由路径">
          <el-input v-model="form.path" placeholder="如 /system/role" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="组件路径">
          <el-input v-model="form.component" placeholder="如 system/role/index" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.perm" placeholder="如 role:add（按钮类必填）" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMenuTree, saveMenu, updateMenu, deleteMenu, type MenuNode } from '@/api/menu'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableData = ref<MenuNode[]>([])

const typeMap: Record<string, { label: string; type: any }> = {
  CATALOG: { label: '目录', type: 'primary' },
  MENU: { label: '菜单', type: 'success' },
  BUTTON: { label: '按钮', type: 'warning' },
}

const form = reactive({
  id: null as null | number,
  parentId: 0,
  title: '',
  type: 'MENU',
  path: '',
  component: '',
  perm: '',
  icon: '',
  sort: 0,
})
const rules: FormRules = {
  title: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

// 把树拍平成"带缩进的下拉选项"，供选父级用
const flatMenus = computed(() => {
  const out: { id: number; label: string }[] = []
  const walk = (nodes: MenuNode[], depth: number) => {
    for (const n of nodes) {
      out.push({ id: n.id, label: `${'　'.repeat(depth)}${n.title}` })
      if (n.children?.length) walk(n.children, depth + 1)
    }
  }
  walk(tableData.value, 0)
  return out
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, {
      id: row.id, parentId: row.parentId ?? 0, title: row.title, type: row.type,
      path: row.path ?? '', component: row.component ?? '', perm: row.perm ?? '',
      icon: row.icon ?? '', sort: row.sort ?? 0,
    })
  } else {
    Object.assign(form, { id: null, parentId: 0, title: '', type: 'MENU', path: '', component: '', perm: '', icon: '', sort: 0 })
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto = {
      parentId: form.parentId, title: form.title, type: form.type, path: form.path,
      component: form.component, perm: form.perm, icon: form.icon, sort: form.sort,
    }
    if (form.id) {
      await updateMenu(form.id, dto)
      ElMessage.success('编辑成功')
    } else {
      await saveMenu(dto)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除菜单「${row.title}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.toolbar-tip { font-size: 13px; color: var(--text-muted); }
.custom-table { border-radius: 8px; overflow: hidden; }
.dialog-form { padding: 8px 0; }
</style>
