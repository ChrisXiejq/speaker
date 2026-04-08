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
  <div class="page">
    <h2 class="page-title">当季题库</h2>
    <el-card class="filters-card" shadow="hover">
      <el-form label-position="top" class="filters-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="季节">
              <el-select
                v-model="seasonIndex"
                placeholder="选择季节"
                style="width: 100%"
                :disabled="!seasons.length"
              >
                <el-option v-for="(s, i) in seasons" :key="s" :label="s" :value="i" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Part">
              <el-select v-model="partIndex" placeholder="选择 Part" style="width: 100%">
                <el-option
                  v-for="(label, i) in PART_LABEL"
                  :key="PART_API[i]"
                  :label="label"
                  :value="i"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-skeleton v-if="loading" animated :rows="6" class="skel" />
    <el-empty
      v-else-if="!seasons.length"
      description="题库中暂无季节数据，请先在管理端导入题目"
    />
    <el-empty v-else-if="!topicGroups.length" description="暂无题目" />

    <el-card
      v-for="group in topicGroups"
      v-else
      :key="group.topic"
      class="topic-card"
      shadow="hover"
    >
      <template #header>
        <div class="topic-head">
          <el-tag type="success" effect="plain" size="small">{{ PART_LABEL[partIndex] }}</el-tag>
          <h3 class="q-topic">{{ group.topic }}</h3>
        </div>
      </template>

      <template v-if="isPart23">
        <section v-if="group.part2Questions?.length" class="sub-part">
          <h4 class="sub-title">Part 2</h4>
          <div v-for="q in group.part2Questions" :key="'p2-' + q.id" class="q-row">
            <p class="q-text">{{ q.questionText }}</p>
            <el-collapse v-if="q.answerText || q.keywordsJson" class="extra-collapse">
              <el-collapse-item title="参考答案与关键词" :name="'p2-' + q.id">
                <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
                <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
                  <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
                </ul>
              </el-collapse-item>
            </el-collapse>
          </div>
        </section>
        <section v-if="group.part3Questions?.length" class="sub-part">
          <h4 class="sub-title">Part 3</h4>
          <p class="sub-hint muted">基于 Part 2 主题的延伸讨论与社会层面问题</p>
          <div v-for="q in group.part3Questions" :key="'p3-' + q.id" class="q-row">
            <p class="q-text">{{ q.questionText }}</p>
            <el-collapse v-if="q.answerText || q.keywordsJson" class="extra-collapse">
              <el-collapse-item title="参考答案与关键词" :name="'p3-' + q.id">
                <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
                <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
                  <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
                </ul>
              </el-collapse-item>
            </el-collapse>
          </div>
        </section>
      </template>

      <template v-else>
        <div v-for="q in group.questions" :key="q.id" class="q-row">
          <p class="q-text">{{ q.questionText }}</p>
          <el-collapse v-if="q.answerText || q.keywordsJson" class="extra-collapse">
            <el-collapse-item title="参考答案与关键词" :name="'p1-' + q.id">
              <p v-if="q.answerText" class="answer">{{ q.answerText }}</p>
              <ul v-if="parseKeywords(q.keywordsJson).length" class="kw">
                <li v-for="(k, i) in parseKeywords(q.keywordsJson)" :key="i">{{ k }}</li>
              </ul>
            </el-collapse-item>
          </el-collapse>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
}

.page-title {
  margin: 0 0 1rem;
  font-size: 1.35rem;
  font-weight: 700;
  color: #ecfdf5;
  letter-spacing: 0.02em;
}

.filters-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.filters-form {
  margin-bottom: -0.5rem;
}

.skel {
  margin: 1rem 0;
}

.topic-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.topic-head {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.q-topic {
  font-weight: 600;
  font-size: 1.05rem;
  margin: 0;
  color: #ecfdf5;
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
  color: #86efac;
}

.sub-hint {
  margin: 0 0 0.5rem;
  font-size: 0.82rem;
}

.q-row {
  padding: 0.55rem 0;
  border-top: 1px solid rgba(74, 222, 128, 0.12);
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
  line-height: 1.65;
  color: #d1fae5;
  font-size: 0.95rem;
}

.extra-collapse {
  margin-top: 0.65rem;
  border: none;
  --el-collapse-header-bg-color: transparent;
}

:deep(.extra-collapse .el-collapse-item__header) {
  color: rgba(167, 243, 208, 0.9);
  font-size: 0.88rem;
}

:deep(.extra-collapse .el-collapse-item__wrap) {
  border-bottom: none;
}

.answer {
  margin: 0.5rem 0 0;
  white-space: pre-wrap;
  line-height: 1.55;
  color: #d1fae5;
}

.kw {
  margin: 0.35rem 0 0;
  padding-left: 1.2rem;
  color: #86efac;
}

.muted {
  color: rgba(167, 243, 208, 0.65);
}
</style>
