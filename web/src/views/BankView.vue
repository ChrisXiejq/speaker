<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { http } from '@/api/http'
import { toastError } from '@/utils/toast'

/** 请求参数：PART1 | PART2_AND_3 */
const PART_API = ['PART1', 'PART2_AND_3']
const PART_LABEL = ['Part 1', 'Part 2 & 3']

function parseKeywords(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const seasons = ref([])
const seasonIndex = ref(0)
const partIndex = ref(0)
/** 后端 GET /api/bank/questions */
const topicGroups = ref([])
const loading = ref(false)

const isPart23 = computed(() => PART_API[partIndex.value] === 'PART2_AND_3')

async function loadSeasons() {
  loading.value = true
  try {
    const { data } = await http.get('/api/bank/seasons')
    seasons.value = Array.isArray(data) ? data : []
    seasonIndex.value = 0
    await loadQuestions()
  } catch (e) {
    toastError(e.message || '加载季节失败')
  } finally {
    loading.value = false
  }
}

async function loadQuestions() {
  const season = seasons.value[seasonIndex.value]
  if (!season) {
    topicGroups.value = []
    return
  }
  const part = PART_API[partIndex.value]
  loading.value = true
  try {
    const { data } = await http.get('/api/bank/questions', {
      params: { season, part },
    })
    topicGroups.value = data || []
  } catch (e) {
    toastError(e.message || '加载题目失败')
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
      <select v-model.number="seasonIndex" class="input" :disabled="!seasons.length">
        <option v-for="(s, i) in seasons" :key="s" :value="i">{{ s }}</option>
      </select>
      <label>Part</label>
      <select v-model.number="partIndex" class="input">
        <option v-for="(label, i) in PART_LABEL" :key="PART_API[i]" :value="i">{{ label }}</option>
      </select>
    </div>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!seasons.length" class="muted empty">题库中暂无季节数据，请先在管理端导入题目</p>
    <p v-else-if="!topicGroups.length" class="muted empty">暂无题目</p>
    <div v-for="group in topicGroups" :key="group.topic" class="card topic-block">
      <span class="part-badge">{{ PART_LABEL[partIndex] }}</span>
      <h3 class="q-topic">{{ group.topic }}</h3>

      <template v-if="isPart23">
        <section v-if="group.part2Questions?.length" class="sub-part">
          <h4 class="sub-title">Part 2</h4>
          <div v-for="q in group.part2Questions" :key="'p2-' + q.id" class="q-row">
            <p class="q-text">{{ q.questionText }}</p>
            <details v-if="q.answerText || q.keywordsJson" class="extra">
              <summary>参考答案与关键词</summary>
              <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
              <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
                <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
              </ul>
            </details>
          </div>
        </section>
        <section v-if="group.part3Questions?.length" class="sub-part">
          <h4 class="sub-title">Part 3</h4>
          <p class="sub-hint muted">基于 Part 2 主题的延伸讨论与社会层面问题</p>
          <div v-for="q in group.part3Questions" :key="'p3-' + q.id" class="q-row">
            <p class="q-text">{{ q.questionText }}</p>
            <details v-if="q.answerText || q.keywordsJson" class="extra">
              <summary>参考答案与关键词</summary>
              <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
              <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
                <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
              </ul>
            </details>
          </div>
        </section>
      </template>

      <template v-else>
        <div v-for="q in group.questions" :key="q.id" class="q-row">
          <p class="q-text">{{ q.questionText }}</p>
          <details v-if="q.answerText || q.keywordsJson" class="extra">
            <summary>参考答案与关键词</summary>
            <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
            <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
              <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
            </ul>
          </details>
        </div>
      </template>
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

.topic-block {
  margin-bottom: 1rem;
}

.part-badge {
  display: inline-block;
  font-size: 0.72rem;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  background: rgba(51, 65, 85, 0.9);
  color: #94a3b8;
  letter-spacing: 0.02em;
  margin-bottom: 0.35rem;
}

.q-topic {
  font-weight: 600;
  font-size: 1.05rem;
  margin: 0 0 0.65rem;
  color: #f8fafc;
}

.sub-part {
  margin-bottom: 1rem;
}

.sub-part:last-child {
  margin-bottom: 0;
}

.sub-title {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #7dd3fc;
}

.sub-hint {
  margin: 0 0 0.5rem;
  font-size: 0.82rem;
}

.q-row {
  padding: 0.55rem 0;
  border-top: 1px solid rgba(51, 65, 85, 0.45);
}

.sub-part .q-row:first-of-type {
  border-top: none;
  padding-top: 0;
}

.q-topic + .sub-part .q-row:first-of-type {
  border-top: none;
}

.q-text {
  margin: 0;
  line-height: 1.6;
  color: #cbd5e1;
  font-size: 0.95rem;
}

.extra {
  margin-top: 0.65rem;
  font-size: 0.88rem;
  color: #94a3b8;
}

.extra summary {
  cursor: pointer;
  color: #7dd3fc;
}

.answer {
  margin: 0.5rem 0 0;
  white-space: pre-wrap;
  line-height: 1.5;
}

.kw {
  margin: 0.35rem 0 0;
  padding-left: 1.2rem;
}

.muted {
  color: #94a3b8;
}
</style>
