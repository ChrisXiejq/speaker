<script setup>
import { ref, watch, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { http } from '@/api/http'
import { toastSuccess } from '@/utils/toast'

const adminKey = ref(localStorage.getItem('sk_admin_key') ?? '')
const seasons = ref([])
const seasonIndex = ref(0)
/** part1 | part23 */
const segment = ref('part1')
const items = ref([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)

function parseKeywords(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const editOpen = ref(false)
const editForm = ref({
  id: null,
  part: '',
  topic: '',
  questionText: '',
  answerText: '',
  keywordsJson: '',
  sortOrder: null,
})

function saveKey() {
  localStorage.setItem('sk_admin_key', adminKey.value.trim())
}

function adminHeaders() {
  return { 'X-Admin-Key': adminKey.value.trim() }
}

async function loadSeasons() {
  try {
    const { data } = await http.get('/api/bank/seasons')
    seasons.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || String(e)
  }
}

async function loadItems() {
  error.value = ''
  const season = seasons.value[seasonIndex.value]
  if (!season) return
  saveKey()
  loading.value = true
  try {
    const { data } = await http.get('/api/admin/bank/items', {
      params: { seasonLabel: season, segment: segment.value },
      headers: adminHeaders(),
    })
    items.value = data || []
  } catch (e) {
    error.value = e.message || String(e)
    items.value = []
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editForm.value = {
    id: row.id,
    part: row.part,
    topic: row.topic ?? '',
    questionText: row.questionText ?? '',
    answerText: row.answerText ?? '',
    keywordsJson: row.keywordsJson ?? '',
    sortOrder: row.sortOrder ?? null,
  }
  editOpen.value = true
}

function closeEdit() {
  editOpen.value = false
}

async function saveEdit() {
  const f = editForm.value
  if (!f.topic?.trim() || !f.questionText?.trim()) {
    error.value = '话题与题目不能为空'
    return
  }
  saving.value = true
  error.value = ''
  try {
    await http.put(
      `/api/admin/bank/items/${f.id}`,
      {
        topic: f.topic.trim(),
        questionText: f.questionText.trim(),
        answerText: f.answerText?.trim() || null,
        keywordsJson: f.keywordsJson?.trim() || null,
        sortOrder: f.sortOrder != null && f.sortOrder !== '' ? Number(f.sortOrder) : null,
      },
      { headers: adminHeaders() },
    )
    closeEdit()
    await loadItems()
  } catch (e) {
    error.value = e.message || String(e)
  } finally {
    saving.value = false
  }
}

async function removeItem(row) {
  try {
    await ElMessageBox.confirm(`确定软删除题目 #${row.id}？（用户侧将不再显示）`, '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  error.value = ''
  try {
    await http.delete(`/api/admin/bank/items/${row.id}`, { headers: adminHeaders() })
    await loadItems()
  } catch (e) {
    error.value = e.message || String(e)
  }
}

async function softDeleteEntireSeason() {
  const season = seasons.value[seasonIndex.value]
  if (!season) return
  try {
    await ElMessageBox.confirm(
      `确定软删除整个季节「${season}」下的全部题目？此操作不可恢复为可见（仅数据库 is_deleted=1）。`,
      '危险操作',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  error.value = ''
  loading.value = true
  try {
    const { data } = await http.delete('/api/admin/bank/items/by-season', {
      params: { seasonLabel: season },
      headers: adminHeaders(),
    })
    const n = data?.softDeleted ?? 0
    await loadSeasons()
    seasonIndex.value = 0
    await loadItems()
    toastSuccess(n ? `已软删除 ${n} 条题目` : '该季节下没有可删题目（可能已全部删除）')
  } catch (e) {
    error.value = e.message || String(e)
  } finally {
    loading.value = false
  }
}

watch([seasonIndex, segment], () => {
  loadItems()
})

onMounted(async () => {
  await loadSeasons()
  await loadItems()
})
</script>

<template>
  <div class="wrap">
    <p class="admin-nav">
      <RouterLink to="/admin" class="nav-link">← 管理首页</RouterLink>
    </p>
    <h2 class="title">题库管理 · 编辑与删除</h2>
    <p class="muted intro">
      按季节与 Part 范围列出题目，可修改后保存；删除为软删除（<code>is_deleted</code>）。需配置
      <code>app.admin.api-key</code>，并填写与导入页相同的管理员密钥。
    </p>

    <el-card class="form-card" shadow="hover">
      <el-form label-position="top">
        <el-form-item label="管理员密钥">
          <el-input
            v-model="adminKey"
            type="password"
            autocomplete="off"
            placeholder="X-Admin-Key"
            show-password
          />
        </el-form-item>
        <el-form-item label="季节">
          <el-select v-model="seasonIndex" placeholder="选择季节" style="width: 100%; max-width: 400px">
            <el-option v-for="(s, i) in seasons" :key="s" :label="s" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-radio-group v-model="segment">
            <el-radio-button value="part1">Part 1</el-radio-button>
            <el-radio-button value="part23">Part 2 &amp; 3</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="row-actions">
          <el-button round :disabled="loading" @click="loadItems">刷新</el-button>
          <el-button type="danger" plain round :disabled="loading || !seasons.length" @click="softDeleteEntireSeason">
            软删除本季全部
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="err-alert" />

    <el-skeleton v-if="loading" animated :rows="6" />
    <el-empty v-else-if="!items.length" description="该条件下暂无题目" />

    <el-card v-else class="table-card" shadow="hover">
      <el-table :data="items" stripe row-key="id" class="tbl">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="detail-inner">
              <p class="detail-label">参考答案</p>
              <p class="detail-answer">{{ row.answerText?.trim() || '（无）' }}</p>
              <p class="detail-label">关键词</p>
              <ul v-if="parseKeywords(row.keywordsJson).length" class="detail-kw">
                <li v-for="(k, i) in parseKeywords(row.keywordsJson)" :key="i">{{ k }}</li>
              </ul>
              <p v-else class="detail-empty">（无）</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="part" label="Part" width="88" />
        <el-table-column prop="topic" label="话题" min-width="120" show-overflow-tooltip />
        <el-table-column prop="questionText" label="题目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="72" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editOpen"
      :title="`编辑题目 #${editForm.id}`"
      width="560px"
      destroy-on-close
      class="edit-dialog"
    >
      <p class="muted small">Part {{ editForm.part }}（不可在此修改）</p>
      <el-form label-position="top">
        <el-form-item label="话题 topic">
          <el-input v-model="editForm.topic" />
        </el-form-item>
        <el-form-item label="题目 questionText">
          <el-input v-model="editForm.questionText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="参考答案 answerText">
          <el-input v-model="editForm.answerText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="关键词 JSON keywordsJson">
          <el-input v-model="editForm.keywordsJson" type="textarea" :rows="3" placeholder='["a","b"]' class="mono" />
        </el-form-item>
        <el-form-item label="排序 sortOrder">
          <el-input v-model.number="editForm.sortOrder" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="closeEdit">取消</el-button>
        <el-button type="primary" round :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 960px;
}

.admin-nav {
  margin: 0 0 0.75rem;
}

.nav-link {
  font-size: 0.88rem;
  color: #86efac;
  text-decoration: none;
}

.nav-link:hover {
  text-decoration: underline;
}

.title {
  margin: 0 0 0.5rem;
  color: #ecfdf5;
  font-size: 1.35rem;
}

.intro {
  margin: 0 0 1rem;
  line-height: 1.5;
  font-size: 0.9rem;
}

.intro code {
  font-size: 0.85em;
  color: #86efac;
  padding: 0.1em 0.35em;
  background: rgba(34, 197, 94, 0.12);
  border-radius: 4px;
}

.form-card,
.table-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.err-alert {
  margin-bottom: 0.75rem;
}

.detail-inner {
  padding: 0.5rem 0.75rem 0.75rem;
  max-width: 880px;
}

.detail-label {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: rgba(167, 243, 208, 0.75);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.detail-answer {
  margin: 0 0 0.65rem;
  color: #d1fae5;
  line-height: 1.5;
  white-space: pre-wrap;
  font-size: 0.86rem;
}

.detail-kw {
  margin: 0;
  padding-left: 1.1rem;
  color: #86efac;
  font-size: 0.86rem;
  line-height: 1.45;
}

.detail-empty {
  margin: 0;
  color: rgba(167, 243, 208, 0.45);
  font-size: 0.86rem;
}

.small {
  font-size: 0.85rem;
  margin: 0 0 1rem;
}

:deep(.mono textarea) {
  font-family: ui-monospace, monospace;
  font-size: 0.85rem;
}

.tbl {
  width: 100%;
}

.muted {
  color: rgba(167, 243, 208, 0.72);
}
</style>
