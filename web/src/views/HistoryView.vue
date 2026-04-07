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

async function removeSession(id, e) {
  e.stopPropagation()
  if (!confirm('确定删除这条练习记录？删除后列表中不再显示。')) return
  try {
    await http.delete(`/api/practice/sessions/${id}`)
    list.value = list.value.filter((x) => x.id !== id)
  } catch (err) {
    alert(err.message || '删除失败')
  }
}

function formatPartLabel(p) {
  if (p === 'PART2_AND_3') return 'Part 2 & 3'
  return p ?? ''
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
        <span class="tag">{{ formatPartLabel(item.part) }}</span>
        <span class="row-right">
          <span class="muted small">{{ item.status }}</span>
          <button type="button" class="del-btn" @click="removeSession(item.id, $event)">删除</button>
        </span>
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

.row-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.del-btn {
  font-size: 0.75rem;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(127, 29, 29, 0.35);
  color: #fecaca;
  cursor: pointer;
}

.del-btn:hover {
  border-color: rgba(248, 113, 113, 0.75);
  background: rgba(127, 29, 29, 0.55);
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
