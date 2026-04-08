<script setup>
import { ref, watch, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
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

/** 展开显示答案与关键词的题目 id */
const expandedIds = ref({})

function parseKeywords(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function isExpanded(id) {
  return !!expandedIds.value[id]
}

function toggleExpand(id) {
  expandedIds.value = { ...expandedIds.value, [id]: !expandedIds.value[id] }
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
    expandedIds.value = {}
  } catch (e) {
    error.value = e.message || String(e)
    items.value = []
    expandedIds.value = {}
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
  if (!window.confirm(`确定软删除题目 #${row.id}？（用户侧将不再显示）`)) return
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
  if (
    !window.confirm(
      `确定软删除整个季节「${season}」下的全部题目？\n此操作不可恢复为可见（仅数据库 is_deleted=1）。`,
    )
  ) {
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

    <div class="card form">
      <label>管理员密钥</label>
      <input v-model="adminKey" type="password" class="input" autocomplete="off" placeholder="X-Admin-Key" />

      <label>季节</label>
      <select v-model.number="seasonIndex" class="input">
        <option v-for="(s, i) in seasons" :key="s" :value="i">{{ s }}</option>
      </select>

      <label>范围</label>
      <div class="seg">
        <label class="inline"
          ><input v-model="segment" type="radio" value="part1" /> Part 1</label
        >
        <label class="inline"
          ><input v-model="segment" type="radio" value="part23" /> Part 2 &amp; 3</label
        >
      </div>

      <div class="row-actions">
        <button type="button" class="btn secondary" :disabled="loading" @click="loadItems">刷新</button>
        <button
          type="button"
          class="btn danger"
          :disabled="loading || !seasons.length"
          @click="softDeleteEntireSeason"
        >
          软删除本季全部
        </button>
      </div>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-if="error" class="err">{{ error }}</p>

    <div v-if="!loading && items.length" class="table-wrap card">
      <table class="tbl">
        <thead>
          <tr>
            <th>ID</th>
            <th>Part</th>
            <th>话题</th>
            <th>题目</th>
            <th>排序</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <template v-for="row in items" :key="row.id">
            <tr>
              <td>{{ row.id }}</td>
              <td>{{ row.part }}</td>
              <td class="topic">{{ row.topic }}</td>
              <td class="q">
                <div class="q-cell">
                  <span class="q-text">{{ row.questionText }}</span>
                  <button
                    type="button"
                    class="expand-btn"
                    :aria-expanded="isExpanded(row.id)"
                    :title="isExpanded(row.id) ? '收起答案与关键词' : '展开答案与关键词'"
                    @click="toggleExpand(row.id)"
                  >
                    <span class="caret" :class="{ open: isExpanded(row.id) }">▼</span>
                  </button>
                </div>
              </td>
              <td>{{ row.sortOrder }}</td>
              <td class="actions">
                <button type="button" class="link" @click="openEdit(row)">编辑</button>
                <button type="button" class="link danger" @click="removeItem(row)">删除</button>
              </td>
            </tr>
            <tr v-if="isExpanded(row.id)" class="detail-row">
              <td colspan="6" class="detail-cell">
                <div class="detail-inner">
                  <p class="detail-label">参考答案</p>
                  <p class="detail-answer">{{ row.answerText?.trim() || '（无）' }}</p>
                  <p class="detail-label">关键词</p>
                  <ul v-if="parseKeywords(row.keywordsJson).length" class="detail-kw">
                    <li v-for="(k, i) in parseKeywords(row.keywordsJson)" :key="i">{{ k }}</li>
                  </ul>
                  <p v-else class="detail-empty">（无）</p>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
    <p v-else-if="!loading && !items.length" class="muted empty">该条件下暂无题目</p>

    <div v-if="editOpen" class="modal-mask" @click.self="closeEdit">
      <div class="modal card">
        <h3>编辑题目 #{{ editForm.id }}</h3>
        <p class="muted small">Part {{ editForm.part }}（不可在此修改）</p>
        <label>话题 topic</label>
        <input v-model="editForm.topic" class="input" />
        <label>题目 questionText</label>
        <textarea v-model="editForm.questionText" class="textarea" rows="4" />
        <label>参考答案 answerText</label>
        <textarea v-model="editForm.answerText" class="textarea" rows="4" />
        <label>关键词 JSON keywordsJson</label>
        <textarea v-model="editForm.keywordsJson" class="textarea mono" rows="3" placeholder='["a","b"]' />
        <label>排序 sortOrder</label>
        <input v-model.number="editForm.sortOrder" type="number" class="input" />
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="closeEdit">取消</button>
          <button type="button" class="btn" :disabled="saving" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
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
  color: #7dd3fc;
  text-decoration: none;
}

.nav-link:hover {
  text-decoration: underline;
}

.title {
  margin: 0 0 0.5rem;
  color: #f8fafc;
}

.intro {
  margin: 0 0 1rem;
  line-height: 1.5;
  font-size: 0.9rem;
}

.intro code {
  font-size: 0.85em;
  color: #7dd3fc;
}

.form label {
  display: block;
  margin: 0.75rem 0 0.35rem;
  color: #cbd5e1;
  font-size: 0.9rem;
}

.seg {
  display: flex;
  gap: 1.25rem;
  margin: 0.5rem 0 1rem;
}

.inline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: #e2e8f0;
  cursor: pointer;
}

.input {
  width: 100%;
  max-width: 400px;
  box-sizing: border-box;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(2, 6, 23, 0.5);
  color: #e2e8f0;
}

.textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(2, 6, 23, 0.5);
  color: #e2e8f0;
  font-size: 0.9rem;
}

.textarea.mono {
  font-family: ui-monospace, monospace;
  font-size: 0.85rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.5);
  background: rgba(14, 165, 233, 0.25);
  color: #e0f2fe;
  cursor: pointer;
}

.btn.secondary {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(30, 41, 59, 0.6);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn.danger {
  border-color: rgba(248, 113, 113, 0.55);
  background: rgba(127, 29, 29, 0.35);
  color: #fecaca;
}

.err {
  color: #fca5a5;
}

.empty {
  text-align: center;
  padding: 2rem;
}

.table-wrap {
  overflow-x: auto;
}

.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.tbl th,
.tbl td {
  padding: 0.5rem 0.65rem;
  text-align: left;
  vertical-align: top;
  border-bottom: 1px solid rgba(51, 65, 85, 0.6);
}

.tbl th {
  color: #94a3b8;
  font-weight: 600;
}

.topic {
  max-width: 140px;
  color: #f1f5f9;
}

.q {
  max-width: 420px;
  color: #cbd5e1;
  line-height: 1.45;
}

.q-cell {
  display: flex;
  align-items: flex-start;
  gap: 0.35rem;
}

.q-text {
  flex: 1;
  min-width: 0;
}

.expand-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: rgba(30, 41, 59, 0.8);
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}

.expand-btn:hover {
  color: #e2e8f0;
  background: rgba(51, 65, 85, 0.9);
}

.caret {
  display: inline-block;
  font-size: 0.65rem;
  transition: transform 0.15s ease;
}

.caret.open {
  transform: rotate(-180deg);
}

.detail-row .detail-cell {
  padding: 0 0.65rem 0.75rem 0.65rem;
  border-bottom: 1px solid rgba(51, 65, 85, 0.6);
  background: rgba(15, 23, 42, 0.45);
}

.detail-inner {
  padding: 0.5rem 0.65rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(71, 85, 105, 0.5);
}

.detail-label {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.detail-answer {
  margin: 0 0 0.65rem;
  color: #cbd5e1;
  line-height: 1.5;
  white-space: pre-wrap;
  font-size: 0.86rem;
}

.detail-kw {
  margin: 0;
  padding-left: 1.1rem;
  color: #a5b4fc;
  font-size: 0.86rem;
  line-height: 1.45;
}

.detail-empty {
  margin: 0;
  color: #64748b;
  font-size: 0.86rem;
}

.actions {
  white-space: nowrap;
}

.link {
  background: none;
  border: none;
  color: #7dd3fc;
  cursor: pointer;
  margin-right: 0.75rem;
  padding: 0;
  font-size: inherit;
}

.link.danger {
  color: #f87171;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  padding: 1rem;
}

.modal {
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal h3 {
  margin: 0 0 0.25rem;
  color: #f8fafc;
}

.small {
  font-size: 0.85rem;
  margin: 0 0 1rem;
}

.modal label {
  display: block;
  margin: 0.65rem 0 0.35rem;
  color: #cbd5e1;
  font-size: 0.88rem;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
