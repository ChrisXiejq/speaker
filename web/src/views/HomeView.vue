<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div>
    <section class="hero card">
      <h1>雅思口语模拟对练</h1>
      <p class="muted">英式考官风格 · Part 1/2/3 · 通义千问驱动</p>
      <p v-if="auth.isLoggedIn" class="status">已登录</p>
      <p v-else class="status muted">未登录 — 请先登录以使用对练与记录</p>
    </section>

    <div class="grid">
      <RouterLink to="/practice" class="tile card" :class="{ disabled: !auth.isLoggedIn }">
        <h2>口语对练</h2>
        <p class="muted">模拟考官提问与即时反馈</p>
      </RouterLink>
      <RouterLink to="/history" class="tile card" :class="{ disabled: !auth.isLoggedIn }">
        <h2>历史记录</h2>
        <p class="muted">会话与评分报告</p>
      </RouterLink>
      <RouterLink to="/bank" class="tile card">
        <h2>当季题库</h2>
        <p class="muted">浏览内置题库</p>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.hero h1 {
  margin: 0 0 0.5rem;
  font-size: 1.75rem;
  color: #f8fafc;
}

.hero .muted {
  margin: 0 0 1rem;
}

.status {
  margin: 0;
  font-size: 0.9rem;
}

.grid {
  display: grid;
  gap: 1rem;
  margin-top: 1.25rem;
}

@media (min-width: 640px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.tile {
  text-decoration: none;
  color: inherit;
  display: block;
  transition: border-color 0.15s;
}

.tile:hover:not(.disabled) {
  border-color: rgba(56, 189, 248, 0.45);
}

.tile h2 {
  margin: 0 0 0.35rem;
  font-size: 1.1rem;
  color: #f1f5f9;
}

.tile.disabled {
  pointer-events: none;
  opacity: 0.45;
}
</style>
