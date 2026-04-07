<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http'

const router = useRouter()
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/api/practice/sessions', {
      params: { page: 0, size: 50 },
    })
    list.value = data.content || []
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  router.push({ name: 'session-detail', params: { id: String(id) } })
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="title">历史记录</h2>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!list.length" class="muted empty">暂无记录</p>
    <div
      v-for="item in list"
      :key="item.id"
      class="card item"
      role="button"
      tabindex="0"
      @click="openDetail(item.id)"
      @keyup.enter="openDetail(item.id)"
    >
      <div class="row">
        <span class="tag">{{ item.part }}</span>
        <span class="muted small">{{ item.status }}</span>
      </div>
      <p class="topic">{{ item.topic || '（无主题）' }}</p>
      <p class="muted small">{{ item.startedAt }}</p>
    </div>
  </div>
</template>

<style scoped>
.title {
  margin: 0 0 1rem;
  color: #f8fafc;
}

.empty {
  text-align: center;
  padding: 2rem;
}

.item {
  cursor: pointer;
  margin-bottom: 0.75rem;
  transition: border-color 0.15s;
}

.item:hover {
  border-color: rgba(56, 189, 248, 0.4);
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag {
  font-size: 0.75rem;
  padding: 0.2rem 0.5rem;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.15);
  color: #7dd3fc;
}

.topic {
  margin: 0.5rem 0 0.25rem;
  color: #e2e8f0;
}

.small {
  font-size: 0.8rem;
}
</style>
