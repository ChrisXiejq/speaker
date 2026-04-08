<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/session')) return '/history'
  return p
})

function logout() {
  auth.logout()
  router.push('/')
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div class="layout">
      <el-header class="app-header" height="auto">
        <div class="header-inner">
          <RouterLink to="/" class="brand">雅思口语模拟</RouterLink>
          <el-menu
            mode="horizontal"
            router
            :ellipsis="false"
            class="nav-menu"
            :default-active="activeMenu"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/practice">对练</el-menu-item>
            <el-menu-item index="/bank">题库</el-menu-item>
            <el-menu-item index="/history">记录</el-menu-item>
          </el-menu>
          <div class="header-actions">
            <el-button v-if="auth.isLoggedIn" type="primary" plain round size="small" @click="logout">
              退出
            </el-button>
            <el-button v-else type="primary" round size="small" @click="goLogin">登录</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </div>
  </el-config-provider>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  padding: 0;
  background: linear-gradient(
    180deg,
    rgba(8, 55, 40, 0.55) 0%,
    rgba(6, 38, 28, 0.35) 100%
  );
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(74, 222, 128, 0.22);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1040px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.35rem 1.25rem;
  flex-wrap: wrap;
}

.brand {
  font-weight: 700;
  color: #ecfdf5;
  text-decoration: none;
  font-size: 1.05rem;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 12px rgba(34, 197, 94, 0.35);
}

.nav-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none !important;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(34, 197, 94, 0.14);
  --el-menu-active-color: var(--el-color-primary-light-3);
}

:deep(.el-menu--horizontal) {
  border-bottom: none;
}

:deep(.el-menu--horizontal > .el-menu-item) {
  border-bottom: none !important;
  color: rgba(209, 250, 229, 0.82);
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: var(--el-color-primary) !important;
  background: rgba(34, 197, 94, 0.1) !important;
  border-radius: 8px;
}

.header-actions {
  flex-shrink: 0;
}

.main {
  flex: 1;
  padding: 1.5rem 1.25rem 2rem;
  max-width: 1040px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  --el-main-padding: 1.5rem 1.25rem 2rem;
}
</style>
