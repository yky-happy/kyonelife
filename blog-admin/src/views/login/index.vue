<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand">
        <div class="brand-icon">
          <el-icon :size="28" color="#ffffff"><EditPen /></el-icon>
        </div>
        <h1 class="brand-name">kyonelife</h1>
        <p class="brand-desc">个人博客管理系统</p>
      </div>
      <div class="decoration">
        <div class="deco-card deco-card-1">
          <div class="deco-dot green" />
          <div class="deco-line" style="width: 80px" />
          <div class="deco-line short" style="width: 50px" />
        </div>
        <div class="deco-card deco-card-2">
          <div class="deco-dot teal" />
          <div class="deco-line" style="width: 100px" />
          <div class="deco-line short" style="width: 60px" />
        </div>
        <div class="deco-card deco-card-3">
          <div class="deco-dot blue" />
          <div class="deco-line" style="width: 70px" />
          <div class="deco-line short" style="width: 40px" />
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-box">
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-sub">登录你的博客管理后台</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              class="custom-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              class="custom-input"
            />
          </el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res = await request.post('/admin/auth/login', form) as any
    auth.setAuth(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // 错误已由 request.ts 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  background:
    linear-gradient(135deg, rgba(255,255,255,.82), rgba(240,246,253,.72)),
    var(--main-bg);
}

.login-left {
  flex: 1;
  background:
    linear-gradient(135deg, rgba(47,109,246,.92), rgba(21,184,166,.9)),
    #2f6df6;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px),
    linear-gradient(180deg, rgba(255,255,255,.1) 1px, transparent 1px);
  background-size: 44px 44px;
  pointer-events: none;
}

.brand {
  text-align: center;
  z-index: 1;
}

.brand-icon {
  width: 64px;
  height: 64px;
  background: rgba(255,255,255,0.16);
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.brand-name {
  font-size: 32px;
  font-weight: 800;
  color: #f8fafc;
  letter-spacing: 0;
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.72);
  letter-spacing: 0;
}

.decoration {
  position: absolute;
  bottom: 80px;
  display: flex;
  gap: 12px;
}

.deco-card {
  background: rgba(255,255,255,0.14);
  border: 1px solid rgba(255,255,255,0.18);
  border-radius: 14px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  backdrop-filter: blur(10px);
}

.deco-card-1 { transform: translateY(8px); }
.deco-card-3 { transform: translateY(8px); }

.deco-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.deco-dot.green { background: #4ade80; }
.deco-dot.teal  { background: #2dd4bf; }
.deco-dot.blue  { background: #60a5fa; }

.deco-line {
  height: 6px;
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
}
.deco-line.short {
  height: 4px;
  background: rgba(255,255,255,0.06);
}

.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: rgba(255, 255, 255, .82);
  backdrop-filter: blur(18px);
}

.login-box {
  width: 100%;
  max-width: 360px;
}

.login-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 6px;
}

.login-sub {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 36px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px var(--border);
  padding: 0 14px;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset, var(--ring) !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #2f6df6 0%, #15b8a6 100%);
  border: none;
  margin-top: 8px;
  letter-spacing: 0;
}

.login-btn:hover {
  filter: brightness(1.03);
}
</style>
