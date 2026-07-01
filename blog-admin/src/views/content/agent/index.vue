<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>AI 创作助手</h2>
        <p>基于平台埋点数据（热搜词 / 热门文章 / 标签）让模型自主调用工具做选题与草稿；AI 产物统一落草稿，人工确认后再发布。</p>
      </div>
    </div>

    <!-- ============ 第一步：选题助手 ============ -->
    <div class="page-card">
      <div class="card-title">
        <el-icon><MagicStick /></el-icon>
        <span>选题助手</span>
        <span class="card-sub">读取近 N 天平台数据，生成可落地的选题</span>
      </div>

      <div class="topic-form">
        <div class="field">
          <label>统计天数</label>
          <el-input-number v-model="topicForm.days" :min="1" :max="90" controls-position="right" />
        </div>
        <div class="field">
          <label>选题数量</label>
          <el-input-number v-model="topicForm.count" :min="1" :max="10" controls-position="right" />
        </div>
        <div class="field grow">
          <label>偏好方向（可空）</label>
          <el-input v-model="topicForm.direction" placeholder="如：Redis / 后端方向，留空则按综合热度" maxlength="50" />
        </div>
        <el-button type="primary" :icon="MagicStick" :loading="topicLoading" @click="handleSuggestTopics">
          生成选题
        </el-button>
      </div>

      <!-- 运行元信息 -->
      <div v-if="topicMeta" class="run-meta">
        <el-tag size="small" type="info" effect="plain">迭代 {{ topicMeta.rounds }} 轮</el-tag>
        <el-tag v-if="topicMeta.capped" size="small" type="warning" effect="plain">已达迭代上限</el-tag>
        <el-tag v-if="topicMeta.degraded" size="small" type="danger" effect="plain">已降级（AI/工具不可用）</el-tag>
        <el-tag v-else size="small" type="success" effect="plain">真实数据驱动</el-tag>
      </div>

      <!-- 选题卡片 -->
      <div v-if="topics.length" class="topic-grid">
        <div v-for="(t, i) in topics" :key="i" class="topic-card">
          <h4 class="topic-title">{{ t.title }}</h4>
          <p class="topic-reason">{{ t.reason }}</p>
          <div v-if="t.refKeywords?.length || t.refArticles?.length" class="topic-refs">
            <el-tag v-for="kw in t.refKeywords" :key="'k' + kw" size="small" effect="light" type="warning">
              热词：{{ kw }}
            </el-tag>
            <el-tag v-for="(a, ai) in t.refArticles" :key="'a' + ai" size="small" effect="light">
              参考：{{ a }}
            </el-tag>
          </div>
          <div class="topic-actions">
            <el-button type="primary" text :icon="EditPen" @click="openDraftDialog(t)">
              用这个选题写草稿
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else-if="topicAsked && !topicLoading" description="没有生成选题，换个方向或天数再试" />
    </div>

    <!-- ============ 创作弹窗 ============ -->
    <el-dialog v-model="draftDialogVisible" title="生成文章草稿" width="560px" align-center :close-on-click-modal="false">
      <el-form :model="draftForm" label-position="top" class="draft-form">
        <el-form-item label="选题">
          <el-input v-model="draftForm.topic" placeholder="选题标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="要点 / 提纲（可空）">
          <el-input
            v-model="draftForm.points"
            type="textarea"
            :rows="4"
            placeholder="希望文章覆盖的要点，留空则由模型自行展开"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="风格（可空）">
          <el-input v-model="draftForm.style" placeholder="如：务实、技术向" maxlength="50" />
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="生成整篇文章约需 20~40 秒，请耐心等待；产物只落草稿（status=0），不会直接发布。"
      />

      <template #footer>
        <el-button @click="draftDialogVisible = false" :disabled="draftLoading">取 消</el-button>
        <el-button type="primary" :loading="draftLoading" @click="handleGenerateDraft">
          {{ draftLoading ? '生成中…' : '生成草稿' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, MagicStick } from '@element-plus/icons-vue'
import {
  agentSuggestTopics,
  agentGenerateDraft,
  type TopicSuggestion,
  type AgentTopicsVO,
} from '@/api/agent'

const router = useRouter()
const sessionId = createSessionId()

// ===== 选题助手 =====
const topicForm = reactive({ days: 7, count: 5, direction: '' })
const topicLoading = ref(false)
const topicAsked = ref(false)
const topics = ref<TopicSuggestion[]>([])
const topicMeta = ref<Pick<AgentTopicsVO, 'rounds' | 'capped' | 'degraded'> | null>(null)

async function handleSuggestTopics() {
  topicLoading.value = true
  topicAsked.value = true
  try {
    const res = await agentSuggestTopics({
      days: topicForm.days,
      count: topicForm.count,
      direction: topicForm.direction || undefined,
      sessionId,
    })
    const data: AgentTopicsVO = res.data
    topics.value = data.topics || []
    topicMeta.value = { rounds: data.rounds, capped: data.capped, degraded: data.degraded }
    if (data.degraded) {
      ElMessage.warning('AI/工具暂不可用，已降级为按热搜词直接拼选题')
    } else if (topics.value.length) {
      ElMessage.success(`已生成 ${topics.value.length} 个选题`)
    }
  } catch {
    // 拦截器已弹错误提示
  } finally {
    topicLoading.value = false
  }
}

// ===== 创作助手 =====
const draftDialogVisible = ref(false)
const draftLoading = ref(false)
const draftForm = reactive({ topic: '', points: '', style: '' })

function openDraftDialog(t: TopicSuggestion) {
  draftForm.topic = t.title
  draftForm.points = t.reason || ''
  draftForm.style = '务实、技术向'
  draftDialogVisible.value = true
}

async function handleGenerateDraft() {
  if (!draftForm.topic.trim()) {
    ElMessage.warning('请填写选题')
    return
  }
  draftLoading.value = true
  try {
    const res = await agentGenerateDraft({
      topic: draftForm.topic,
      points: draftForm.points || undefined,
      style: draftForm.style || undefined,
      sessionId,
    })
    const data = res.data
    if (data.degraded || !data.draftId) {
      ElMessage.error('草稿生成失败（AI/工具不可用或超时），请稍后重试或手动撰写')
      return
    }
    draftDialogVisible.value = false
    ElMessage.success(`草稿已生成（#${data.draftId}），迭代 ${data.rounds} 轮`)
    await ElMessageBox.confirm(
      `草稿「${data.title}」已保存为草稿，是否现在去编辑、补充并发布？`,
      '生成成功',
      { confirmButtonText: '去编辑', cancelButtonText: '留在本页', type: 'success' },
    )
    router.push(`/content/article/edit?id=${data.draftId}`)
  } catch (e) {
    // 取消跳转 / 拦截器已提示，均无需额外处理
  } finally {
    draftLoading.value = false
  }
}

function createSessionId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}

.page-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-muted);
  max-width: 760px;
  line-height: 1.6;
}

.page-card {
  background: var(--glass-bg, #fff);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius, 14px);
  padding: 20px;
  margin-bottom: 18px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
}

.card-title .card-sub {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 6px;
}

.topic-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 14px;
}

.topic-form .field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.topic-form .field.grow {
  flex: 1;
  min-width: 220px;
}

.topic-form .field label {
  font-size: 12px;
  font-weight: 650;
  color: var(--text-muted);
}

.run-meta {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.topic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.topic-card {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm, 10px);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(43, 50, 61, .015);
  transition: all .18s ease;
}

.topic-card:hover {
  border-color: var(--accent, #2b323d);
  box-shadow: 0 10px 26px rgba(28, 37, 48, .08);
}

.topic-title {
  margin: 0;
  font-size: 15px;
  font-weight: 750;
  color: var(--text-main);
  line-height: 1.4;
}

.topic-reason {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  flex: 1;
}

.topic-refs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.topic-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px dashed var(--line-soft);
  padding-top: 8px;
}

.draft-form {
  margin-bottom: 4px;
}
</style>
