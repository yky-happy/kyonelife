<template>
  <div class="page">
    <div class="page-card">
      <div class="card-header">
        <h3>网站基础配置</h3>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
      </div>

      <el-form :model="form" label-width="110px" class="config-form">
        <div class="section">
          <p class="section-title">基本信息</p>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="网站名称">
                <el-input v-model="form.siteName" placeholder="kyonelife" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="ICP备案号">
                <el-input v-model="form.icpNumber" placeholder="可选填写" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="网站 Logo">
                <div class="avatar-upload">
                  <el-image
                    v-if="form.logo"
                    :src="form.logo"
                    fit="contain"
                    class="logo-preview"
                  />
                  <div class="avatar-actions">
                    <el-upload
                      :show-file-list="false"
                      :http-request="handleLogoUpload"
                      accept="image/jpeg,image/png,image/webp,image/gif,image/svg+xml"
                    >
                      <el-button :loading="logoUploading" :icon="Upload">上传 Logo</el-button>
                    </el-upload>
                    <el-input v-model="form.logo" placeholder="上传后自动回填，也可手动填写图片 URL" />
                  </div>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="网站简介">
                <el-input v-model="form.summary" placeholder="一句话介绍" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="section">
          <p class="section-title">作者信息</p>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="作者名">
                <el-input v-model="form.author" placeholder="你的名字" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="个性签名">
                <el-input v-model="form.signature" placeholder="个性签名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="作者头像">
                <div class="avatar-upload">
                  <el-avatar :size="64" :src="form.authorAvatar">
                    {{ form.author ? form.author.slice(0, 1) : 'A' }}
                  </el-avatar>
                  <div class="avatar-actions">
                    <el-upload
                      :show-file-list="false"
                      :http-request="handleAuthorAvatarUpload"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                    >
                      <el-button :loading="avatarUploading" :icon="Upload">上传头像</el-button>
                    </el-upload>
                    <el-input v-model="form.authorAvatar" placeholder="上传后自动回填，也可手动填写头像 URL" />
                  </div>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="GitHub">
                <el-input v-model="form.github" placeholder="GitHub 主页链接" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系邮箱">
                <el-input v-model="form.email" placeholder="联系邮箱" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="section">
          <p class="section-title">公告内容</p>
          <el-form-item label="">
            <el-input v-model="form.bulletin" type="textarea" :rows="3"
              placeholder="首页公告内容，留空则不显示" maxlength="500" show-word-limit />
          </el-form-item>
        </div>

        <div class="section">
          <p class="section-title">关于我</p>
          <el-form-item label="">
            <el-input v-model="form.aboutMe" type="textarea" :rows="8"
              placeholder="Markdown 格式内容" />
          </el-form-item>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/file'
import { getConfig, updateConfig } from '@/api/config'

const saving = ref(false)
const avatarUploading = ref(false)
const logoUploading = ref(false)

const form = reactive({
  siteName: '', logo: '', summary: '', author: '', authorAvatar: '',
  signature: '', github: '', email: '', aboutMe: '', icpNumber: '',
  bulletin: '',
})

onMounted(async () => {
  const res = await getConfig()
  if (res.data) {
    Object.keys(form).forEach((k) => {
      const v = (res.data as Record<string, unknown>)[k]
      if (v !== null && v !== undefined) {
        (form as Record<string, unknown>)[k] = v
      }
    })
  }
})

async function handleSave() {
  saving.value = true
  try {
    await updateConfig({ ...form })
    ElMessage.success('配置已保存')
  } finally {
    saving.value = false
  }
}

async function handleAuthorAvatarUpload(options: UploadRequestOptions) {
  avatarUploading.value = true
  try {
    const res = await uploadImage(options.file, 'avatar')
    form.authorAvatar = res.data.url
    ElMessage.success('头像上传成功')
  } finally {
    avatarUploading.value = false
  }
}

async function handleLogoUpload(options: UploadRequestOptions) {
  logoUploading.value = true
  try {
    const res = await uploadImage(options.file, 'config')
    form.logo = res.data.url
    ElMessage.success('Logo 上传成功')
  } finally {
    logoUploading.value = false
  }
}
</script>

<style scoped>
.page { height: 100%; }
.page-card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 24px 28px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 28px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
.card-header h3 { font-size: 15px; font-weight: 600; color: #111827; }
.config-form { max-width: 900px; }
.section { margin-bottom: 28px; }
.section-title { font-size: 12.5px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 18px; padding-bottom: 8px; border-bottom: 1px solid var(--border); }
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}
.avatar-actions {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}
.logo-preview {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: #fff;
  flex-shrink: 0;
}
</style>
