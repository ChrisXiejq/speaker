<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="wrap">
    <h1 class="title">管理员后台</h1>
    <p class="muted intro">仅通过直接访问 <code>/admin</code> 进入；首页不展示管理入口。</p>
    <div class="grid">
      <RouterLink to="/admin/bank" class="tile card">
        <h2>Markdown 导入</h2>
        <p class="muted">粘贴题目与答案入库</p>
      </RouterLink>
      <RouterLink to="/admin/bank/manage" class="tile card">
        <h2>题库编辑</h2>
        <p class="muted">按季浏览、修改与软删除</p>
      </RouterLink>
    </div>
    <p class="back">
      <RouterLink to="/" class="link">← 返回首页</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 720px;
}

.title {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  color: #f8fafc;
}

.intro {
  margin: 0 0 1.25rem;
  font-size: 0.9rem;
  line-height: 1.5;
}

.intro code {
  font-size: 0.85em;
  color: #7dd3fc;
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

@media (min-width: 560px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.tile {
  text-decoration: none;
  color: inherit;
  display: block;
  transition: border-color 0.15s;
}

.tile:hover {
  border-color: rgba(56, 189, 248, 0.45);
}

.tile h2 {
  margin: 0 0 0.35rem;
  font-size: 1.1rem;
  color: #f1f5f9;
}

.back {
  margin: 1.5rem 0 0;
}

.link {
  color: #7dd3fc;
  text-decoration: none;
  font-size: 0.9rem;
}

.link:hover {
  text-decoration: underline;
}
</style>
