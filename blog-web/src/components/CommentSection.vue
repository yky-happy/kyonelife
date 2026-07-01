<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { addComment, getComments, likeComment, type CommentItem } from '../api/comment'
import { uploadImage } from '../api/upload'
import { isLoggedIn, openLogin } from '../composables/userAuth'
import { renderMarkdown } from '../utils/markdown'

const props = defineProps<{ articleId: number }>()

const comments = ref<CommentItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const content = ref('')
const submitting = ref(false)
const uploading = ref(false)
const error = ref('')
const editorRef = ref<HTMLTextAreaElement | null>(null)
const fileRef = ref<HTMLInputElement | null>(null)
const expanded = ref<Set<number>>(new Set())
const replyTo = ref<{ parentId: number; nickname: string } | null>(null)

const hasMore = computed(() => page.value * pageSize < total.value)

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}
function render(text: string) {
  return renderMarkdown(text)
}

async function load(reset = false) {
  if (reset) {
    page.value = 1
    comments.value = []
  }
  loading.value = true
  try {
    const res = await getComments(props.articleId, page.value, pageSize)
    total.value = res.total
    comments.value = reset ? res.records : [...comments.value, ...res.records]
  } catch {
    if (reset) comments.value = []
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value += 1
  await load()
}

/* ---- 工具栏 ---- */
function insert(before: string, after = before, placeholder = '文字') {
  const el = editorRef.value
  if (!el) {
    content.value += before + placeholder + after
    return
  }
  const s = el.selectionStart
  const e = el.selectionEnd
  const val = content.value
  const sel = val.slice(s, e) || placeholder
  content.value = val.slice(0, s) + before + sel + after + val.slice(e)
  nextTick(() => {
    el.focus()
    el.selectionStart = s + before.length
    el.selectionEnd = s + before.length + sel.length
  })
}
function insertText(text: string) {
  const el = editorRef.value
  const s = el ? el.selectionStart : content.value.length
  content.value = content.value.slice(0, s) + text + content.value.slice(s)
}

/* ---- 图片上传 ---- */
function pickImage() {
  if (!isLoggedIn.value) {
    openLogin()
    return
  }
  fileRef.value?.click()
}
async function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    error.value = '图片不能超过 5MB'
    return
  }
  uploading.value = true
  error.value = ''
  try {
    const res = await uploadImage(file)
    insertText(`![](${res.url})`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '图片上传失败'
  } finally {
    uploading.value = false
  }
}

function setReply(c: CommentItem) {
  replyTo.value = { parentId: c.parentId ?? c.id, nickname: c.nickname }
  nextTick(() => editorRef.value?.focus())
}
function cancelReply() {
  replyTo.value = null
}

async function submit() {
  if (!isLoggedIn.value) {
    openLogin()
    return
  }
  error.value = ''
  if (!content.value.trim()) {
    error.value = '说点什么吧'
    return
  }
  submitting.value = true
  try {
    const pid = replyTo.value?.parentId ?? null
    await addComment(props.articleId, content.value.trim(), pid)
    content.value = ''
    if (pid) expanded.value.add(pid)
    replyTo.value = null
    await load(true)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发表失败，请重试'
  } finally {
    submitting.value = false
  }
}

function findComment(id: number): CommentItem | undefined {
  for (const c of comments.value) {
    if (c.id === id) return c
    for (const r of c.replies || []) if (r.id === id) return r
  }
  return undefined
}
async function like(c: CommentItem) {
  try {
    const res = await likeComment(c.id)
    const target = findComment(c.id)
    if (target) {
      target.liked = res.liked
      target.likeCount = res.likeCount
    }
  } catch {
    // 忽略
  }
}

function toggleExpand(id: number) {
  if (expanded.value.has(id)) expanded.value.delete(id)
  else expanded.value.add(id)
  expanded.value = new Set(expanded.value)
}

onMounted(() => load(true))
watch(() => props.articleId, () => load(true))
</script>

<template>
  <section class="comment-section">
    <h2 class="comment-title">评论 <span class="comment-total">{{ total }} 条</span></h2>

    <input ref="fileRef" type="file" accept="image/*" hidden @change="onFile" />

    <!-- 编辑器 -->
    <div class="comment-editor">
      <div v-if="replyTo" class="reply-hint">
        回复 <b>@{{ replyTo.nickname }}</b>
        <button @click="cancelReply"><i class="fa-solid fa-xmark"></i></button>
      </div>
      <textarea
        ref="editorRef"
        v-model="content"
        rows="4"
        maxlength="1000"
        placeholder="写下你的评论…支持 Markdown"
      ></textarea>
      <div class="editor-bar">
        <div class="editor-tools">
          <button title="加粗" @click="insert('**')"><b>B</b></button>
          <button title="斜体" @click="insert('*')"><i>I</i></button>
          <button title="下划线" @click="insert('<u>', '</u>')"><u>U</u></button>
          <span class="tool-sep"></span>
          <button title="链接" @click="insertText('[链接](https://)')"><i class="fa-solid fa-link"></i></button>
          <button title="插入图片" :disabled="uploading" @click="pickImage">
            <i :class="uploading ? 'fa-solid fa-spinner fa-spin' : 'fa-regular fa-image'"></i>
          </button>
          <button title="代码" @click="insert('`')"><i class="fa-solid fa-code"></i></button>
        </div>
        <div class="editor-right">
          <span v-if="error" class="editor-err">{{ error }}</span>
          <button
            class="comment-submit"
            :class="{ 'login-tip': !isLoggedIn }"
            :disabled="submitting"
            @click="submit"
          >
            {{ isLoggedIn ? (submitting ? '发表中…' : replyTo ? '回复' : '发表评论') : '请先登录' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 列表 -->
    <div v-if="loading && !comments.length" class="ky-state">加载评论中…</div>
    <template v-else-if="comments.length">
      <ul class="cmt-list">
        <li v-for="c in comments" :key="c.id" class="cmt">
          <div class="cmt-avatar">
            <img v-if="c.avatar" :src="c.avatar" alt="" />
            <span v-else>{{ (c.nickname || 'U').slice(0, 1).toUpperCase() }}</span>
          </div>
          <div class="cmt-main">
            <div class="cmt-head">
              <span class="cmt-name">{{ c.nickname }}</span>
              <button class="cmt-reply" @click="setReply(c)">回复</button>
            </div>
            <div class="cmt-date">{{ fmt(c.createTime) }}</div>
            <div class="cmt-content" v-html="render(c.content)"></div>

            <button v-if="c.replies?.length" class="cmt-expand" @click="toggleExpand(c.id)">
              <span class="line"></span>
              {{ expanded.has(c.id) ? '收起回复' : `展开 ${c.replies.length} 条回复` }}
            </button>

            <ul v-if="expanded.has(c.id) && c.replies?.length" class="cmt-replies">
              <li v-for="r in c.replies" :key="r.id" class="cmt">
                <div class="cmt-avatar small">
                  <img v-if="r.avatar" :src="r.avatar" alt="" />
                  <span v-else>{{ (r.nickname || 'U').slice(0, 1).toUpperCase() }}</span>
                </div>
                <div class="cmt-main">
                  <div class="cmt-head">
                    <span class="cmt-name">{{ r.nickname }}</span>
                    <button class="cmt-reply" @click="setReply(r)">回复</button>
                  </div>
                  <div class="cmt-date">{{ fmt(r.createTime) }}</div>
                  <div class="cmt-content" v-html="render(r.content)"></div>
                </div>
                <button class="cmt-like" :class="{ liked: r.liked }" @click="like(r)">
                  <i class="fa-solid fa-heart"></i><span>{{ r.likeCount }}</span>
                </button>
              </li>
            </ul>
          </div>
          <button class="cmt-like" :class="{ liked: c.liked }" @click="like(c)">
            <i class="fa-solid fa-heart"></i><span>{{ c.likeCount }}</span>
          </button>
        </li>
      </ul>

      <div class="load-more" v-if="hasMore">
        <button class="ky-btn ghost" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '加载更多评论' }}
        </button>
      </div>
    </template>
    <div v-else class="ky-state">还没有评论，来抢沙发～</div>
  </section>
</template>
