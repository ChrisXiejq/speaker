<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const mode = ref('login')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请填写用户名与密码'
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(username.value.trim(), password.value)
    } else {
      await auth.register(username.value.trim(), password.value)
    }
    let redirect = route.query.redirect || '/'
    if (Array.isArray(redirect)) redirect = redirect[0] || '/'
    router.replace(typeof redirect === 'string' ? redirect : '/')
  } catch (e) {
    error.value = e.message || '失败'
  } finally {
    loading.value = false
  }
}

function toggle() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  error.value = ''
}
</script>

<template>
  <div class="wrap">
    <el-card class="login-card" shadow="always">
      <template #header>
        <h1 class="title">{{ mode === 'login' ? '登录' : '注册' }}</h1>
      </template>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input
            v-model="username"
            autocomplete="username"
            placeholder="3–64 字符"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="至少 8 位"
            show-password
          />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="err-alert" />
        <el-button
          type="primary"
          native-type="submit"
          class="submit-btn"
          :loading="loading"
          round
        >
          {{ mode === 'login' ? '登录' : '注册' }}
        </el-button>
      </el-form>
      <el-button text type="primary" class="switch-link" @click="toggle">
        {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
      </el-button>
    </el-card>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 420px;
  margin: 2rem auto;
}

.login-card {
  border-radius: 16px;
}

.title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 600;
  color: #ecfdf5;
}

.err-alert {
  margin-bottom: 1rem;
}

.submit-btn {
  width: 100%;
  margin-top: 0.25rem;
}

.switch-link {
  width: 100%;
  margin-top: 1rem;
}
</style>
