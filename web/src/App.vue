<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <RouterLink to="/" class="brand">雅思口语模拟</RouterLink>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/practice">对练</RouterLink>
        <RouterLink to="/bank">题库</RouterLink>
        <RouterLink to="/history">记录</RouterLink>
        <template v-if="auth.isLoggedIn">
          <button type="button" class="link-btn" @click="logout">退出</button>
        </template>
        <RouterLink v-else to="/login">登录</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  font-weight: 700;
  color: #f8fafc;
  text-decoration: none;
  font-size: 1.05rem;
}

.nav {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.nav a {
  color: #94a3b8;
  text-decoration: none;
  font-size: 0.9rem;
}

.nav a.router-link-active {
  color: #38bdf8;
}

.link-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 0;
}

.link-btn:hover {
  color: #38bdf8;
}

.main {
  flex: 1;
  padding: 1.25rem;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
</style>
