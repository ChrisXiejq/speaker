<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { http } from '@/api/http'
import { toastError } from '@/utils/toast'
import PracticeDashboard from '@/components/PracticeDashboard.vue'

const stats = ref(null)
const loadingStats = ref(false)

async function loadDashboard() {
  loadingStats.value = true
  try {
    const { data } = await http.get('/api/practice/stats/dashboard')
    stats.value = data
  } catch (e) {
    toastError(e.message || '加载练习统计失败')
    stats.value = null
  } finally {
    loadingStats.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page">
    <el-card class="hero-card" shadow="hover">
      <template #header>
        <div class="hero-head">
          <span class="hero-title">雅思口语模拟对练</span>
        </div>
      </template>
      <p class="hero-sub">英式考官风格 · Part 1/2/3 · 通义千问驱动</p>
    </el-card>

    <PracticeDashboard
      :stats="stats"
      :loading="loadingStats"
    />

    <el-row :gutter="16" class="grid">
      <el-col :xs="24" :sm="12" :md="8">
        <RouterLink
          to="/practice"
          class="tile-link"
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

.grid {
  margin-top: 0;
}

.tile-link {
  display: block;
  text-decoration: none;
  color: inherit;
  margin-bottom: 1rem;
}

.tile-card {
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
  min-height: 140px;
}

.tile-link:hover .tile-card {
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
