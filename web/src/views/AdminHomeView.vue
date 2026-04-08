<script setup>
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="wrap">
    <el-page-header class="page-h" @back="router.push('/')">
      <template #content>
        <span class="title">管理员后台</span>
      </template>
    </el-page-header>
    <p class="muted intro">仅通过直接访问 <code>/admin</code> 进入；首页不展示管理入口。</p>
    <el-row :gutter="16" class="grid">
      <el-col :xs="24" :sm="12">
        <RouterLink to="/admin/bank" class="tile-link">
          <el-card class="tile-card" shadow="hover">
            <div class="tile-icon">📥</div>
            <h2>Markdown 导入</h2>
            <p class="muted">粘贴题目与答案入库</p>
          </el-card>
        </RouterLink>
      </el-col>
      <el-col :xs="24" :sm="12">
        <RouterLink to="/admin/bank/manage" class="tile-link">
          <el-card class="tile-card" shadow="hover">
            <div class="tile-icon">✏️</div>
            <h2>题库编辑</h2>
            <p class="muted">按季浏览、修改与软删除</p>
          </el-card>
        </RouterLink>
      </el-col>
    </el-row>
    <p class="back">
      <RouterLink to="/" class="link">← 返回首页</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 720px;
}

.page-h {
  margin-bottom: 0.75rem;
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ecfdf5;
}

.intro {
  margin: 0 0 1.25rem;
  font-size: 0.9rem;
  line-height: 1.5;
}

.intro code {
  font-size: 0.85em;
  color: #86efac;
  padding: 0.1em 0.35em;
  background: rgba(34, 197, 94, 0.12);
  border-radius: 4px;
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
  min-height: 130px;
  border-radius: 14px;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
}

.tile-link:hover .tile-card {
  transform: translateY(-2px);
  border-color: rgba(74, 222, 128, 0.45) !important;
}

.tile-icon {
  font-size: 1.75rem;
  margin-bottom: 0.35rem;
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

.back {
  margin: 1.5rem 0 0;
}

.link {
  color: #86efac;
  text-decoration: none;
  font-size: 0.9rem;
}

.link:hover {
  text-decoration: underline;
}

.muted {
  color: rgba(167, 243, 208, 0.72);
}
</style>
