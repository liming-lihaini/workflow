<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-slogan">
        <h1>自定义流程引擎</h1>
        <p>高效、灵活、可扩展的企业级流程管理平台</p>
      </div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <h2 class="login-title">用户登录</h2>
        <a-form
          :model="formState"
          @finish="handleLogin"
          layout="vertical"
          :rules="rules"
        >
          <a-form-item label="用户名" name="username">
            <a-input
              v-model:value="formState.username"
              size="large"
              placeholder="请输入用户名"
              :prefix="h(UserOutlined)"
            />
          </a-form-item>
          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="formState.password"
              size="large"
              placeholder="请输入密码"
              :prefix="h(LockOutlined)"
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>
        <div class="login-hint">
          <span>默认账号：sys_admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const formState = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(formState)
    message.success('登录成功')
    router.push('/dashboard')
  } catch (err) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

.login-left {
  flex: 1;
  background: var(--bg-login);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-slogan {
  color: #fff;
  text-align: center;
}

.login-slogan h1 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
}

.login-slogan p {
  font-size: 16px;
  opacity: 0.85;
}

.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.login-card {
  width: 360px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 32px;
  text-align: center;
}

.login-hint {
  text-align: center;
  color: var(--text-placeholder);
  font-size: 12px;
  margin-top: 16px;
}
</style>
