<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { sendEmailCode, resetPassword } from '../api/auth'
import { closeLogin, doLogin, doRegister } from '../composables/userAuth'

const mode = ref<'login' | 'register' | 'reset'>('login')

// 登录字段
const identifier = ref('')
const loginPwd = ref('')

// 注册 / 重置 共用：email + code + pwd（+ 注册的 nickname）
const email = ref('')
const code = ref('')
const pwd = ref('')
const nickname = ref('')

const loading = ref(false)
const sending = ref(false)
const error = ref('')
const info = ref('')
const countdown = ref(0)
let timer: number | undefined

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))

function switchMode(m: 'login' | 'register' | 'reset') {
  mode.value = m
  error.value = ''
  info.value = ''
}

async function onSendCode() {
  error.value = ''
  if (!emailValid.value) {
    error.value = '请输入正确的邮箱'
    return
  }
  sending.value = true
  try {
    await sendEmailCode(email.value.trim())
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        window.clearInterval(timer)
        timer = undefined
      }
    }, 1000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  error.value = ''
  info.value = ''
  loading.value = true
  try {
    if (mode.value === 'login') {
      if (!identifier.value.trim() || !loginPwd.value) {
        error.value = '请输入邮箱/账号和密码'
        return
      }
      await doLogin(identifier.value.trim(), loginPwd.value)
      closeLogin()
    } else if (mode.value === 'register') {
      if (!emailValid.value) {
        error.value = '请输入正确的邮箱'
        return
      }
      if (!code.value.trim()) {
        error.value = '请输入验证码'
        return
      }
      if (pwd.value.length < 6) {
        error.value = '密码至少 6 位'
        return
      }
      await doRegister({
        email: email.value.trim(),
        code: code.value.trim(),
        password: pwd.value,
        nickname: nickname.value.trim() || undefined,
      })
      closeLogin()
    } else {
      // 重置密码
      if (!emailValid.value) {
        error.value = '请输入正确的邮箱'
        return
      }
      if (!code.value.trim()) {
        error.value = '请输入验证码'
        return
      }
      if (pwd.value.length < 6) {
        error.value = '新密码至少 6 位'
        return
      }
      await resetPassword(email.value.trim(), code.value.trim(), pwd.value)
      identifier.value = email.value.trim()
      switchMode('login')
      info.value = '密码已重置，请用新密码登录'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="auth-mask" @click.self="closeLogin">
    <div class="auth-modal">
      <button class="auth-close" title="关闭" @click="closeLogin"><i class="fa-solid fa-xmark"></i></button>

      <div v-if="mode !== 'reset'" class="auth-tabs">
        <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>
      <div v-else class="auth-reset-head">
        <button class="auth-back" @click="switchMode('login')"><i class="fa-solid fa-angle-left"></i> 返回登录</button>
        <span>重置密码</span>
      </div>

      <!-- 登录 -->
      <template v-if="mode === 'login'">
        <div class="auth-field">
          <i class="fa-solid fa-user"></i>
          <input v-model="identifier" type="text" placeholder="邮箱或账号" autocomplete="username" />
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-lock"></i>
          <input v-model="loginPwd" type="password" placeholder="密码" autocomplete="current-password" @keyup.enter="onSubmit" />
        </div>
        <div class="auth-row">
          <span v-if="info" class="auth-info">{{ info }}</span>
          <button class="auth-forgot" @click="switchMode('reset')">忘记密码？</button>
        </div>
      </template>

      <!-- 注册 -->
      <template v-else-if="mode === 'register'">
        <div class="auth-field">
          <i class="fa-solid fa-envelope"></i>
          <input v-model="email" type="email" placeholder="邮箱" autocomplete="email" />
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-shield-halved"></i>
          <input v-model="code" type="text" inputmode="numeric" maxlength="6" placeholder="验证码" />
          <button class="auth-code-btn" :disabled="sending || countdown > 0" @click="onSendCode">
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-lock"></i>
          <input v-model="pwd" type="password" placeholder="设置密码（≥6 位）" autocomplete="new-password" />
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-face-smile"></i>
          <input v-model="nickname" type="text" maxlength="20" placeholder="昵称（选填，留空用账号）" />
        </div>
      </template>

      <!-- 重置密码 -->
      <template v-else>
        <div class="auth-field">
          <i class="fa-solid fa-envelope"></i>
          <input v-model="email" type="email" placeholder="注册邮箱" autocomplete="email" />
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-shield-halved"></i>
          <input v-model="code" type="text" inputmode="numeric" maxlength="6" placeholder="验证码" />
          <button class="auth-code-btn" :disabled="sending || countdown > 0" @click="onSendCode">
            {{ countdown > 0 ? `${countdown}s` : sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <div class="auth-field">
          <i class="fa-solid fa-lock"></i>
          <input v-model="pwd" type="password" placeholder="设置新密码（≥6 位）" autocomplete="new-password" />
        </div>
      </template>

      <p v-if="error" class="auth-error">{{ error }}</p>

      <button class="auth-submit" :disabled="loading" @click="onSubmit">
        {{ loading ? '处理中…' : mode === 'login' ? '登录' : mode === 'register' ? '注册并登录' : '重置密码' }}
      </button>
    </div>
  </div>
</template>
