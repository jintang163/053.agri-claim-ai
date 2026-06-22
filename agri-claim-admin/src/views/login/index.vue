<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-box">
      <div class="login-title">
        <h1>🌾 农业保险快速定损系统</h1>
        <p>Agri-Claim AI · 基于无人机影像的智能定损平台</p>
      </div>

      <el-card class="login-card" shadow="hover">
        <h2 class="form-title">用户登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码"
              :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input v-model="form.captcha" placeholder="验证码" :prefix-icon="Key" class="flex-1" />
              <img v-if="captcha.base64" :src="captcha.base64" class="captcha-img" @click="loadCaptcha"
                :title="'点击刷新'" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <el-divider>快速登录（演示）</el-divider>
        <div class="quick-login">
          <el-tag v-for="a in accounts" :key="a.username" type="info" class="mr-10 mb-10 pointer"
            effect="plain" size="large" @click="quickLogin(a)">
            {{ a.label }}（{{ a.username }}）
          </el-tag>
        </div>
      </el-card>

      <div class="copyright">
        © 2024 Agri-Claim AI · 农业保险快速定损系统 v1.0.0
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const captcha = ref({})

const form = reactive({
  username: 'admin',
  password: 'admin',
  captcha: '',
  captchaKey: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { label: '超级管理员', username: 'admin', password: 'admin' },
  { label: '查勘员', username: 'surveyor', password: '123456' },
  { label: '定损经理', username: 'manager', password: 'manager' }
]

async function loadCaptcha() {
  try {
    const data = await userStore.GetCaptcha()
    captcha.value = data
    form.captchaKey = data.key
  } catch (e) {}
}

async function handleLogin() {
  try {
    await formRef.value.validate()
    loading.value = true
    await userStore.Login({ ...form })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}

function quickLogin(a) {
  form.username = a.username
  form.password = a.password
  handleLogin()
}

onMounted(loadCaptcha)
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(120, 255, 120, 0.15), transparent 40%),
              radial-gradient(circle at 80% 20%, rgba(255, 255, 120, 0.15), transparent 40%);
}
.login-box {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.login-title { text-align: center; margin-bottom: 24px; }
.login-title h1 { font-size: 34px; margin: 0; letter-spacing: 2px; }
.login-title p { font-size: 14px; opacity: 0.9; margin-top: 8px; }

.login-card {
  width: 420px;
  padding: 16px 24px;
  border-radius: 12px;
}
.form-title { text-align: center; color: #303133; margin: 0 0 20px; }
.captcha-row { display: flex; gap: 10px; }
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  object-fit: cover;
  border: 1px solid #dcdfe6;
}
.quick-login { display: flex; flex-wrap: wrap; }
.pointer { cursor: pointer; }

.copyright {
  position: absolute;
  bottom: 16px;
  font-size: 12px;
  opacity: 0.8;
}
</style>
