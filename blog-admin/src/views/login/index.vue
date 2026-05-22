<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand">
        <div class="brand-icon">
          <el-icon :size="28" color="#14b8a6"><EditPen /></el-icon>
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
  background: var(--main-bg);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #134e4a 100%);
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
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(20,184,166,0.15) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.brand {
  text-align: center;
  z-index: 1;
}

.brand-icon {
  width: 64px;
  height: 64px;
  background: rgba(20,184,166,0.15);
  border: 1px solid rgba(20,184,166,0.3);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.brand-name {
  font-size: 32px;
  font-weight: 800;
  color: #f8fafc;
  letter-spacing: -1px;
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.5);
  letter-spacing: 1px;
}

.decoration {
  position: absolute;
  bottom: 80px;
  display: flex;
  gap: 12px;
}

.deco-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
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
  background: #fff;
}

.login-box {
  width: 100%;
  max-width: 360px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
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
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb;
  padding: 0 14px;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #14b8a6 !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  background: #14b8a6;
  border-color: #14b8a6;
  margin-top: 8px;
  letter-spacing: 4px;
}

.login-btn:hover {
  background: #0d9488;
  border-color: #0d9488;
}
</style>
