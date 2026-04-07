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
  <div class="card narrow">
    <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
    <form @submit.prevent="submit">
      <label>用户名</label>
      <input v-model="username" type="text" autocomplete="username" placeholder="3–64 字符" />
      <label>密码</label>
      <input v-model="password" type="password" autocomplete="current-password" placeholder="至少 8 位" />
      <p v-if="error" class="err">{{ error }}</p>
      <button type="submit" class="btn primary" :disabled="loading">
        {{ loading ? '提交中…' : mode === 'login' ? '登录' : '注册' }}
      </button>
    </form>
    <p class="muted link" @click="toggle">
      {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
    </p>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 400px;
  margin: 2rem auto;
}

h1 {
  margin: 0 0 1rem;
  font-size: 1.35rem;
}

label {
  display: block;
  margin: 0.75rem 0 0.35rem;
  color: #cbd5e1;
  font-size: 0.9rem;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(2, 6, 23, 0.5);
  color: #e2e8f0;
}

.err {
  color: #f87171;
  font-size: 0.85rem;
  margin: 0.75rem 0 0;
}

.btn {
  width: 100%;
  margin-top: 1rem;
  padding: 0.65rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

.btn.primary {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: #fff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.link {
  margin-top: 1rem;
  text-align: center;
  cursor: pointer;
}

.link:hover {
  color: #38bdf8;
}
</style>
