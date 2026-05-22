<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar">
        <span class="table-tip">轮播图数量为 0 时，首页自动使用图片轮播</span>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增轮播</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="custom-table">
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl"
              style="width: 80px; height: 50px; border-radius: 6px" fit="cover" />
            <div v-else class="img-placeholder"><el-icon color="#d1d5db"><Picture /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="160" />
        <el-table-column label="链接" prop="linkUrl" min-width="200" show-overflow-tooltip />
        <el-table-column label="排序" prop="sort" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播' : '新增轮播'" width="480px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="dialog-form">
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="图片 URL" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="可叠加显示在图片上（可选）" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="点击轮播跳转的链接（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="form-tip">数字越大越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
const form = reactive({ id: null as null | number, imageUrl: '', title: '', linkUrl: '', sort: 0, status: 1 })
const rules: FormRules = { imageUrl: [{ required: true, message: '请填写图片地址', trigger: 'blur' }] }

function openDialog(row?: any) {
  row ? Object.assign(form, row) : Object.assign(form, { id: null, imageUrl: '', title: '', linkUrl: '', sort: 0, status: 1 })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try { ElMessage.success(form.id ? '编辑成功' : '新增成功'); dialogVisible.value = false }
  finally { saving.value = false }
}

async function handleDelete(_row: any) {
  await ElMessageBox.confirm('确认删除该轮播图？', '提示', { type: 'warning' })
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.table-tip { font-size: 13px; color: var(--text-muted); }
.custom-table { border-radius: 8px; overflow: hidden; }
.img-placeholder { width: 80px; height: 50px; border-radius: 6px; background: var(--main-bg); display: flex; align-items: center; justify-content: center; }
.dialog-form { padding: 8px 0; }
.form-tip { font-size: 12px; color: var(--text-muted); margin-left: 10px; }
</style>
