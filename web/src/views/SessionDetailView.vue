<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/api/http'
import { toastError } from '@/utils/toast'

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
    toastError(e.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => load(id),
  { immediate: true }
)

function formatPartLabel(p) {
  if (p === 'PART2_AND_3') return 'Part 2 & 3'
  return p ?? ''
}

function formatEpochMs(v) {
  if (v == null || v === '') return ''
  const n = typeof v === 'number' ? v : Number(v)
  if (Number.isNaN(n)) return String(v)
  return new Date(n).toLocaleString()
}
</script>

<template>
  <div class="page">
    <el-skeleton v-if="loading" animated :rows="8" />
    <template v-else-if="detail">
      <el-card class="block-card" shadow="hover">
        <div class="row">
          <el-tag type="success" effect="plain" round>{{ formatPartLabel(detail.session.part) }}</el-tag>
          <span class="muted small">{{ detail.session.status }}</span>
        </div>
        <p class="topic">{{ detail.session.topic || '（无主题）' }}</p>
        <p class="muted small">{{ formatEpochMs(detail.session.startedAt) }}</p>
      </el-card>

      <el-card v-if="detail.report" class="block-card" shadow="hover">
        <template #header>
          <span class="head-title">评价</span>
        </template>
        <el-descriptions :column="1" border size="small" class="mb-desc">
          <el-descriptions-item label="Overall">{{ detail.report.overallBand }}</el-descriptions-item>
        </el-descriptions>
        <div class="row5">
          <el-tag type="success" effect="plain">P {{ detail.report.pronunciationScore }}</el-tag>
          <el-tag type="success" effect="plain">G {{ detail.report.grammarScore }}</el-tag>
          <el-tag type="success" effect="plain">C {{ detail.report.coherenceScore }}</el-tag>
          <el-tag type="success" effect="plain">F {{ detail.report.fluencyScore }}</el-tag>
          <el-tag type="success" effect="plain">I {{ detail.report.ideasScore }}</el-tag>
        </div>
        <p class="feedback">{{ detail.report.detailedFeedback }}</p>
      </el-card>

      <el-card class="block-card" shadow="hover">
        <template #header>
          <span class="head-title">对话</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="t in detail.turns"
            :key="t.id"
            :type="t.role === 'EXAMINER' ? 'success' : 'primary'"
            placement="top"
          >
            <p class="role">{{ t.role === 'EXAMINER' ? '考官' : '你' }}</p>
            <p class="content">{{ t.content }}</p>
            <p v-if="t.briefEval" class="muted small">简评：{{ t.briefEval }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
}

.block-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.head-title {
  font-weight: 600;
  color: #ecfdf5;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic {
  margin: 0.65rem 0 0.25rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #ecfdf5;
}

.row5 {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0.5rem 0;
}

.feedback {
  margin-top: 0.75rem;
  line-height: 1.65;
  white-space: pre-wrap;
  color: #d1fae5;
}

.mb-desc {
  margin-bottom: 0.75rem;
}

.role {
  margin: 0 0 0.25rem;
  color: rgba(167, 243, 208, 0.85);
  font-size: 0.8rem;
  font-weight: 600;
}

.content {
  margin: 0;
  line-height: 1.6;
  color: #ecfdf5;
}

.small {
  font-size: 0.8rem;
  margin: 0.35rem 0 0;
}

.muted {
  color: rgba(167, 243, 208, 0.65);
}

:deep(.el-timeline-item__timestamp) {
  display: none;
}
</style>
