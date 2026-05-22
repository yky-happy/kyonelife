<template>
  <div class="page">
    <div class="page-card">
      <div class="toolbar-tip">菜单树由数据库维护，此页面只读查看。修改权限请直接操作数据库或联系超级管理员。</div>
      <el-table :data="tableData" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" class="custom-table">
        <el-table-column label="菜单名称" prop="title" min-width="180" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.type" size="small">{{ typeMap[row.type]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由路径" prop="path" min-width="180" />
        <el-table-column label="权限标识" prop="perm" min-width="160" />
        <el-table-column label="图标" prop="icon" width="100" />
        <el-table-column label="排序" prop="sort" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)
const tableData = ref<any[]>([])

const typeMap: Record<string, { label: string; type: any }> = {
  CATALOG: { label: '目录', type: 'primary' },
  MENU:    { label: '菜单', type: 'success' },
  BUTTON:  { label: '按钮', type: 'warning' },
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; }
.toolbar-tip { font-size: 13px; color: var(--text-muted); margin-bottom: 18px; padding: 10px 14px; background: #fffbeb; border-radius: 8px; border: 1px solid #fef08a; }
.custom-table { border-radius: 8px; overflow: hidden; }
</style>
