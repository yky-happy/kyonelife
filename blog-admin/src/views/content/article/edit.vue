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
            <div class="field-with-ai">
              <el-input
                v-model="form.summary"
                type="textarea"
                :rows="3"
                placeholder="请输入文章摘要，或点击右侧按钮由 AI 生成"
                maxlength="500"
                show-word-limit
              />
              <el-button
                class="ai-btn"
                :icon="MagicStick"
                :loading="summaryLoading"
                @click="handleAiSummary"
              >AI 生成摘要</el-button>
            </div>
          </el-form-item>

          <!-- 文章设置：原右侧栏内容整体移至摘要与正文之间，正文区改为整行全宽 -->
          <div class="settings-section">
            <div class="settings-title">文章设置</div>
            <div class="settings-grid">
              <el-form-item label="封面图" class="cover-item">
                <div class="cover-upload">
                  <el-image v-if="form.cover" :src="form.cover" class="cover-preview" fit="cover" />
                  <div v-else class="cover-empty">暂无封面</div>
                  <el-upload
                    :show-file-list="false"
                    :http-request="handleCoverUpload"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                  >
                    <el-button :loading="coverUploading" :icon="Upload">上传封面</el-button>
                  </el-upload>
                  <el-input v-model="form.cover" placeholder="上传后自动回填，也可手动填写图片 URL" />
                </div>
              </el-form-item>

              <div class="settings-fields">
                <el-form-item label="合集">
                  <el-select v-model="form.collectionId" clearable placeholder="不加入合集" style="width: 100%">
                    <el-option v-for="item in collections" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="标签">
                  <el-select v-model="form.tagIds" multiple clearable placeholder="请选择标签" style="width: 100%">
                    <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                  <el-button
                    class="ai-tag-btn"
                    text
                    :icon="MagicStick"
                    :loading="tagLoading"
                    @click="handleAiTags"
                  >AI 推荐标签</el-button>
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
              </div>
            </div>
          </div>

          <el-form-item label="正文" prop="contentMd">
            <div class="editor-tools">
              <el-upload
                :show-file-list="false"
                :http-request="handleVideoUpload"
                accept="video/mp4,video/webm,video/quicktime"
              >
                <el-button :loading="videoUploading" :icon="VideoPlay">上传视频并插入正文</el-button>
              </el-upload>
            </div>
            <MdEditor
              v-model="form.contentMd"
              placeholder="请输入 Markdown 原文"
              :toolbars-exclude="['github', 'fullscreen']"
              class="markdown-editor"
              @onUploadImg="handleMarkdownImageUpload"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Upload, VideoPlay, MagicStick } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getCollectionPage, type Collection } from '@/api/collection'
import { getTagPage, type Tag } from '@/api/tag'
import { uploadImage, uploadVideo } from '@/api/file'
import { aiGenerateSummary, aiRecommendTags } from '@/api/ai'
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
const coverUploading = ref(false)
const videoUploading = ref(false)
const summaryLoading = ref(false)
const tagLoading = ref(false)
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

async function handleCoverUpload(options: UploadRequestOptions) {
  coverUploading.value = true
  try {
    const res = await uploadImage(options.file, 'article')
    form.cover = res.data.url
    ElMessage.success('封面上传成功')
  } finally {
    coverUploading.value = false
  }
}

async function handleMarkdownImageUpload(files: File[], callback: (urls: string[]) => void) {
  const results = await Promise.all(files.map((file) => uploadImage(file, 'article')))
  callback(results.map((item) => item.data.url))
  ElMessage.success('图片上传成功')
}

async function handleVideoUpload(options: UploadRequestOptions) {
  videoUploading.value = true
  try {
    const res = await uploadVideo(options.file, 'article')
    form.contentMd += `\n\n<video src="${res.data.url}" controls style="max-width: 100%; border-radius: 8px;"></video>\n`
    ElMessage.success('视频上传成功')
  } finally {
    videoUploading.value = false
  }
}

async function handleAiSummary() {
  if (!form.contentMd.trim()) {
    ElMessage.warning('请先填写正文，再生成摘要')
    return
  }
  summaryLoading.value = true
  try {
    const res = await aiGenerateSummary({ title: form.title, contentMd: form.contentMd })
    form.summary = res.data.summary
    if (res.data.degraded) {
      ElMessage.warning('AI 暂不可用，已用本地规则生成摘要，请按需修改')
    } else {
      ElMessage.success(res.data.fromCache ? '已复用缓存的 AI 摘要' : 'AI 摘要已生成')
    }
  } finally {
    summaryLoading.value = false
  }
}

async function handleAiTags() {
  if (!form.contentMd.trim()) {
    ElMessage.warning('请先填写正文，再推荐标签')
    return
  }
  tagLoading.value = true
  try {
    const res = await aiRecommendTags({ title: form.title, contentMd: form.contentMd })
    const recommended: string[] = res.data.tags || []
    const matchedIds: number[] = []
    const unmatched: string[] = []
    recommended.forEach((name) => {
      const hit = tags.value.find((t) => t.name === name)
      if (hit) matchedIds.push(hit.id)
      else unmatched.push(name)
    })
    form.tagIds = Array.from(new Set([...form.tagIds, ...matchedIds]))

    if (res.data.degraded) {
      ElMessage.warning('AI 暂不可用，已用关键词匹配已有标签')
    } else if (matchedIds.length || unmatched.length) {
      ElMessage.success('AI 标签推荐完成')
    } else {
      ElMessage.info('未匹配到合适标签')
    }
    if (unmatched.length) {
      ElMessage.info(`建议新建标签：${unmatched.join('、')}`)
    }
  } finally {
    tagLoading.value = false
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
.editor-layout { display: block; }
.main-panel {
  background: var(--card-bg);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 20px 24px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}
.markdown-editor { height: 560px; }
.editor-tools {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
  width: 100%;
}

/* 文章设置：摘要与正文之间的一体化设置区 */
.settings-section {
  margin: 2px 0 18px;
  padding: 16px 18px 2px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: var(--glass-bg);
}
.settings-title {
  margin-bottom: 14px;
}

/* 区块标题：标题 / 摘要 / 正文（顶层表单项）+ 文章设置 —— 宋体并放大字号 */
.main-panel :deep(.el-form > .el-form-item > .el-form-item__label),
.settings-title {
  font-family: "Songti SC", "STSong", SimSun, "宋体", serif;
  font-size: 20px;
  font-weight: 700 !important;
  color: var(--ink-strong);
  line-height: 1.7;
}
.settings-grid {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 0 22px;
  align-items: start;
}
.settings-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.settings-grid :deep(.el-form-item),
.settings-fields :deep(.el-form-item) {
  margin-bottom: 14px;
}

/* 摘要 + AI 生成按钮：输入框占满，按钮靠右下 */
.field-with-ai { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; width: 100%; }
.field-with-ai :deep(.el-textarea) { width: 100%; }
.ai-btn { align-self: flex-end; }
.ai-tag-btn { margin-top: 6px; padding: 0; height: auto; }

.switch-list { display: flex; flex-direction: row; gap: 24px; align-items: center; }
.cover-upload { display: flex; flex-direction: column; gap: 10px; width: 100%; }
.cover-preview,
.cover-empty {
  width: 100%;
  height: 132px;
  border-radius: 8px;
  border: 1px solid var(--line-soft);
}
.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--main-bg);
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 960px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .settings-grid { grid-template-columns: 1fr; }
  .settings-fields { grid-template-columns: 1fr; }
}
</style>
