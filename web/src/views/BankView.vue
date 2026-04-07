<script setup>
import { ref, onMounted, watch } from 'vue'
import { http } from '@/api/http'

const PARTS = ['PART1', 'PART2', 'PART3']

const seasons = ref([])
const seasonIndex = ref(0)
const partIndex = ref(0)
const items = ref([])
const loading = ref(false)

async function loadSeasons() {
  loading.value = true
  try {
    const { data } = await http.get('/api/bank/seasons')
    seasons.value = data?.length ? data : ['2025Q1']
    await loadQuestions()
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

async function loadQuestions() {
  const season = seasons.value[seasonIndex.value] || '2025Q1'
  const part = PARTS[partIndex.value]
  loading.value = true
  try {
    const { data } = await http.get('/api/bank/questions', {
      params: { season, part },
    })
    items.value = data || []
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

watch([seasonIndex, partIndex], () => {
  loadQuestions()
})

onMounted(loadSeasons)
</script>

<template>
  <div>
    <h2 class="title">当季题库</h2>
    <div class="card filters">
      <label>季节</label>
      <select v-model.number="seasonIndex" class="input">
        <option v-for="(s, i) in seasons" :key="s" :value="i">{{ s }}</option>
      </select>
      <label>Part</label>
      <select v-model.number="partIndex" class="input">
        <option v-for="(p, i) in PARTS" :key="p" :value="i">{{ p }}</option>
      </select>
    </div>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted empty">暂无题目</p>
    <div v-for="q in items" :key="q.id" class="card q-item">
      <p class="q-topic">{{ q.topic }}</p>
      <p class="q-text">{{ q.questionText }}</p>
    </div>
  </div>
</template>

<style scoped>
.title {
  margin: 0 0 1rem;
  color: #f8fafc;
}

.filters label {
  display: block;
  margin: 0.5rem 0 0.35rem;
  color: #cbd5e1;
  font-size: 0.9rem;
}

.input {
  width: 100%;
  max-width: 320px;
  box-sizing: border-box;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(2, 6, 23, 0.5);
  color: #e2e8f0;
}

.empty {
  text-align: center;
  padding: 2rem;
}

.q-item {
  margin-bottom: 0.75rem;
}

.q-topic {
  font-weight: 600;
  margin: 0 0 0.35rem;
  color: #f8fafc;
}

.q-text {
  margin: 0;
  line-height: 1.6;
  color: #cbd5e1;
  font-size: 0.95rem;
}
</style>
