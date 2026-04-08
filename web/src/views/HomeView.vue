<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="page">
    <el-card class="hero-card" shadow="hover">
      <template #header>
        <div class="hero-head">
          <span class="hero-title">雅思口语模拟对练</span>
          <el-tag v-if="auth.isLoggedIn" type="success" effect="dark" round size="small">已登录</el-tag>
          <el-tag v-else type="info" effect="plain" round size="small">未登录</el-tag>
        </div>
      </template>
      <p class="hero-sub">英式考官风格 · Part 1/2/3 · 通义千问驱动</p>
      <p v-if="!auth.isLoggedIn" class="hint">请先登录以使用对练与记录</p>
    </el-card>

    <el-row :gutter="16" class="grid">
      <el-col :xs="24" :sm="12" :md="8">
        <RouterLink
          to="/practice"
          class="tile-link"
          :class="{ disabled: !auth.isLoggedIn }"
        >
          <el-card class="tile-card" shadow="hover">
            <div class="tile-icon">🎙</div>
            <h2>口语对练</h2>
            <p class="muted">模拟考官提问与即时反馈</p>
          </el-card>
        </RouterLink>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <RouterLink
          to="/history"
          class="tile-link"
          :class="{ disabled: !auth.isLoggedIn }"
        >
          <el-card class="tile-card" shadow="hover">
            <div class="tile-icon">📋</div>
            <h2>历史记录</h2>
            <p class="muted">会话与评分报告</p>
          </el-card>
        </RouterLink>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <RouterLink to="/bank" class="tile-link">
          <el-card class="tile-card" shadow="hover">
            <div class="tile-icon">📚</div>
            <h2>当季题库</h2>
            <p class="muted">浏览内置题库</p>
          </el-card>
        </RouterLink>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
}

.hero-card {
  margin-bottom: 1.25rem;
}

.hero-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.hero-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #ecfdf5;
  letter-spacing: 0.02em;
}

.hero-sub {
  margin: 0;
  color: rgba(209, 250, 229, 0.85);
  font-size: 0.95rem;
}

.hint {
  margin: 0.75rem 0 0;
  font-size: 0.88rem;
  color: rgba(167, 243, 208, 0.65);
}

.grid {
  margin-top: 0;
}

.tile-link {
  display: block;
  text-decoration: none;
  color: inherit;
  margin-bottom: 1rem;
}

.tile-link.disabled {
  pointer-events: none;
  opacity: 0.42;
}

.tile-card {
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
  min-height: 140px;
}

.tile-link:not(.disabled):hover .tile-card {
  transform: translateY(-2px);
  border-color: rgba(74, 222, 128, 0.45) !important;
}

.tile-icon {
  font-size: 1.75rem;
  margin-bottom: 0.35rem;
  filter: grayscale(0.2);
}

.tile-card h2 {
  margin: 0 0 0.35rem;
  font-size: 1.1rem;
  color: #ecfdf5;
}

.tile-card .muted {
  margin: 0;
  line-height: 1.45;
}
</style>
