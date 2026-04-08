<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { http } from '@/api/http'
import { toastError } from '@/utils/toast'

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
    toastError(e.message || '加载记录失败')
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  router.push({ name: 'session-detail', params: { id: String(id) } })
}

async function removeSession(id, e) {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm('确定删除这条练习记录？删除后列表中不再显示。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await http.delete(`/api/practice/sessions/${id}`)
    list.value = list.value.filter((x) => x.id !== id)
  } catch (err) {
    toastError(err.message || '删除失败')
  }
}

function formatPartLabel(p) {
  if (p === 'PART2_AND_3') return 'Part 2 & 3'
  return p ?? ''
}

/** API 返回 epoch 毫秒（number） */
function formatEpochMs(v) {
  if (v == null || v === '') return ''
  const n = typeof v === 'number' ? v : Number(v)
  if (Number.isNaN(n)) return String(v)
  return new Date(n).toLocaleString()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h2 class="page-title">历史记录</h2>
    <el-skeleton v-if="loading" animated :rows="4" />
    <el-empty v-else-if="!list.length" description="暂无记录" />
    <el-card
      v-for="item in list"
      v-else
      :key="item.id"
      class="item-card"
      shadow="hover"
      role="button"
      tabindex="0"
      @click="openDetail(item.id)"
      @keyup.enter="openDetail(item.id)"
    >
      <div class="row">
        <el-tag type="success" effect="plain" round>{{ formatPartLabel(item.part) }}</el-tag>
        <div class="row-right">
          <span class="status muted">{{ item.status }}</span>
          <el-button type="danger" plain size="small" round @click="removeSession(item.id, $event)">
            删除
          </el-button>
        </div>
      </div>
      <p class="topic">{{ item.topic || '（无主题）' }}</p>
      <p class="muted small">{{ formatEpochMs(item.startedAt) }}</p>
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
}

.item-card {
  margin-bottom: 0.75rem;
  cursor: pointer;
  border-radius: 14px;
  transition:
    border-color 0.2s ease,
    transform 0.15s ease;
}

.item-card:hover {
  border-color: rgba(74, 222, 128, 0.45) !important;
  transform: translateY(-1px);
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.row-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status {
  font-size: 0.8rem;
}

.topic {
  margin: 0.65rem 0 0.25rem;
  color: #ecfdf5;
  font-weight: 500;
}

.small {
  font-size: 0.8rem;
  margin: 0;
}

.muted {
  color: rgba(167, 243, 208, 0.65);
}
</style>
