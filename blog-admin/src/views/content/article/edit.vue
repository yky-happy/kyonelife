<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>{{ articleId ? '编辑文章' : '写文章' }}</h2>
        <p>保存 Markdown 原文，HTML 内容当前与 Markdown 同步保存。</p>
      </div>
      <div class="actions">
        <el-button @click="goBack">返回</el-button>
        <el-button :loading="saving" @click="submit(0)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="submit(1)">发布文章</el-button>
      </div>
    </div>

    <div class="editor-layout" v-loading="loading">
      <div class="main-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="200" show-word-limit />
          </el-form-item>

          <el-form-item label="摘要">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="请输入文章摘要"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="正文" prop="contentMd">
            <MdEditor
              v-model="form.contentMd"
              placeholder="请输入 Markdown 原文"
              :toolbars-exclude="['github']"
              class="markdown-editor"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="side-panel">
        <el-form label-position="top">
          <el-form-item label="封面图地址">
            <el-input v-model="form.cover" placeholder="https://..." />
          </el-form-item>

          <el-form-item label="合集">
            <el-select v-model="form.collectionId" clearable placeholder="不加入合集" style="width: 100%">
              <el-option v-for="item in collections" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="标签">
            <el-select v-model="form.tagIds" multiple clearable placeholder="请选择标签" style="width: 100%">
              <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="SEO 关键词">
            <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" maxlength="200" />
          </el-form-item>

          <el-form-item label="原创类型">
            <el-radio-group v-model="form.isOriginal">
              <el-radio-button :label="1">原创</el-radio-button>
              <el-radio-button :label="0">转载</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="form.isOriginal === 0" label="原文链接">
            <el-input v-model="form.originalUrl" placeholder="请输入转载原文链接" />
          </el-form-item>

          <el-form-item label="展示设置">
            <div class="switch-list">
              <el-switch v-model="form.isStick" :active-value="1" :inactive-value="0" active-text="置顶" />
              <el-switch v-model="form.isCarousel" :active-value="1" :inactive-value="0" active-text="轮播" />
            </div>
          </el-form-item>

          <el-form-item v-if="form.isCarousel === 1" label="轮播排序">
            <el-input-number v-model="form.carouselSort" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getCollectionPage, type Collection } from '@/api/collection'
import { getTagPage, type Tag } from '@/api/tag'
import {
  getArticleDetail,
  saveArticle,
  updateArticle,
  type ArticleSaveDTO,
} from '@/api/article'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const tags = ref<Tag[]>([])
const collections = ref<Collection[]>([])
const articleId = computed(() => route.query.id ? Number(route.query.id) : null)

const form = reactive({
  title: '',
  cover: '',
  summary: '',
  contentMd: '',
  keywords: '',
  collectionId: null as number | null,
  tagIds: [] as number[],
  isStick: 0,
  isCarousel: 0,
  carouselSort: 0,
  isOriginal: 1,
  originalUrl: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  contentMd: [{ required: true, message: '请输入文章正文', trigger: 'blur' }],
}

async function loadOptions() {
  const [collectionRes, tagRes] = await Promise.all([
    getCollectionPage({ page: 1, size: 100 }),
    getTagPage({ page: 1, size: 100 }),
  ])
  collections.value = collectionRes.data.records
  tags.value = tagRes.data.records
}

async function loadDetail() {
  if (!articleId.value) return
  loading.value = true
  try {
    const res = await getArticleDetail(articleId.value)
    Object.assign(form, {
      title: res.data.title || '',
      cover: res.data.cover || '',
      summary: res.data.summary || '',
      contentMd: res.data.contentMd || '',
      keywords: res.data.keywords || '',
      collectionId: res.data.collectionId || null,
      tagIds: res.data.tagIds || [],
      isStick: res.data.isStick || 0,
      isCarousel: res.data.isCarousel || 0,
      carouselSort: res.data.carouselSort || 0,
      isOriginal: res.data.isOriginal ?? 1,
      originalUrl: res.data.originalUrl || '',
    })
  } finally {
    loading.value = false
  }
}

function buildDTO(status: number): ArticleSaveDTO {
  return {
    title: form.title,
    cover: form.cover,
    summary: form.summary,
    content: form.contentMd,
    contentMd: form.contentMd,
    keywords: form.keywords,
    collectionId: form.collectionId,
    tagIds: form.tagIds,
    status,
    isStick: form.isStick,
    isCarousel: form.isCarousel,
    carouselSort: form.carouselSort,
    isOriginal: form.isOriginal,
    originalUrl: form.isOriginal === 0 ? form.originalUrl : '',
  }
}

async function submit(status: number) {
  await formRef.value?.validate()
  if (form.isOriginal === 0 && !form.originalUrl) {
    ElMessage.warning('转载文章请填写原文链接')
    return
  }

  saving.value = true
  try {
    const dto = buildDTO(status)
    if (articleId.value) {
      await updateArticle(articleId.value, dto)
      ElMessage.success(status === 1 ? '发布成功' : '保存成功')
    } else {
      await saveArticle(dto)
      ElMessage.success(status === 1 ? '发布成功' : '保存成功')
    }
    router.push('/content/article')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/content/article')
}

onMounted(async () => {
  await loadOptions()
  await loadDetail()
})
</script>

<style scoped>
.page { min-height: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.page-header h2 { margin: 0 0 6px; font-size: 24px; font-weight: 800; color: var(--text-main); }
.page-header p { margin: 0; color: var(--text-muted); font-size: 13px; }
.actions { display: flex; gap: 10px; }
.editor-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; align-items: start; }
.main-panel,
.side-panel {
  background: var(--card-bg);
  border: 1px solid rgba(232, 237, 246, .88);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 20px 24px;
  backdrop-filter: blur(18px);
}
.markdown-editor { height: 520px; }
.switch-list { display: flex; flex-direction: column; gap: 12px; align-items: flex-start; }

@media (max-width: 960px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .editor-layout { grid-template-columns: 1fr; }
  .side-panel { order: -1; }
}
</style>
