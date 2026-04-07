<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/api/http'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)

async function load(id) {
  if (!id) return
  loading.value = true
  detail.value = null
  try {
    const { data } = await http.get(`/api/practice/sessions/${id}`)
    detail.value = data
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => load(id),
  { immediate: true }
)
</script>

<template>
  <div>
    <p v-if="loading" class="muted">加载中…</p>
    <template v-else-if="detail">
      <div class="card">
        <div class="row">
          <span class="tag">{{ detail.session.part }}</span>
          <span class="muted small">{{ detail.session.status }}</span>
        </div>
        <p class="topic">{{ detail.session.topic || '（无主题）' }}</p>
        <p class="muted small">{{ detail.session.startedAt }}</p>
      </div>

      <div v-if="detail.report" class="card">
        <h3>评价</h3>
        <p class="muted">Overall: {{ detail.report.overallBand }}</p>
        <div class="row5">
          <span>P {{ detail.report.pronunciationScore }}</span>
          <span>G {{ detail.report.grammarScore }}</span>
          <span>C {{ detail.report.coherenceScore }}</span>
          <span>F {{ detail.report.fluencyScore }}</span>
          <span>I {{ detail.report.ideasScore }}</span>
        </div>
        <p class="feedback">{{ detail.report.detailedFeedback }}</p>
      </div>

      <div class="card">
        <h3>对话</h3>
        <div v-for="t in detail.turns" :key="t.id" class="turn">
          <p class="role">{{ t.role === 'EXAMINER' ? '考官' : '你' }}</p>
          <p class="content">{{ t.content }}</p>
          <p v-if="t.briefEval" class="muted small">简评：{{ t.briefEval }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
h3 {
  margin: 0 0 0.75rem;
  color: #f8fafc;
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
  font-size: 1.1rem;
  font-weight: 600;
}

.row5 {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin: 0.5rem 0;
  color: #cbd5e1;
  font-size: 0.85rem;
}

.feedback {
  margin-top: 0.75rem;
  line-height: 1.65;
  white-space: pre-wrap;
  color: #e2e8f0;
}

.turn {
  padding: 0.75rem 0;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}

.turn:first-of-type {
  border-top: none;
  padding-top: 0;
}

.role {
  margin: 0 0 0.25rem;
  color: #94a3b8;
  font-size: 0.8rem;
}

.content {
  margin: 0;
  line-height: 1.6;
  color: #e2e8f0;
}

.small {
  font-size: 0.8rem;
}
</style>
