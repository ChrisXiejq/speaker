<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { http } from '@/api/http'

const adminKey = ref(localStorage.getItem('sk_admin_key') ?? '')
const seasonLabel = ref('2025-9-12')
const replaceExisting = ref(false)
const part1Markdown = ref('')
const part23Markdown = ref('')
const loading = ref(false)
const preview = ref(null)
const importResult = ref(null)
const error = ref('')

function saveKey() {
  localStorage.setItem('sk_admin_key', adminKey.value.trim())
}

async function runPreview() {
  error.value = ''
  importResult.value = null
  if (!part1Markdown.value.trim() && !part23Markdown.value.trim()) {
    error.value = '请至少填写 Part1 或 Part2&3 其中一段'
    return
  }
  saveKey()
  loading.value = true
  try {
    const { data } = await http.post(
      '/api/admin/bank/preview-markdown',
      {
        seasonLabel: seasonLabel.value.trim(),
        part1Markdown: part1Markdown.value,
        part23Markdown: part23Markdown.value,
        replaceExisting: false,
      },
      { headers: { 'X-Admin-Key': adminKey.value.trim() } },
    )
    preview.value = data
  } catch (e) {
    error.value = e.message || String(e)
    preview.value = null
  } finally {
    loading.value = false
  }
}

async function runImport() {
  error.value = ''
  preview.value = null
  if (!seasonLabel.value.trim()) {
    error.value = '请填写季节标签 seasonLabel'
    return
  }
  if (!part1Markdown.value.trim() && !part23Markdown.value.trim()) {
    error.value = '请至少填写 Part1 或 Part2&3 其中一段'
    return
  }
  saveKey()
  loading.value = true
  try {
    const { data } = await http.post(
      '/api/admin/bank/import-markdown',
      {
        seasonLabel: seasonLabel.value.trim(),
        part1Markdown: part1Markdown.value,
        part23Markdown: part23Markdown.value,
        replaceExisting: replaceExisting.value,
      },
      { headers: { 'X-Admin-Key': adminKey.value.trim() } },
    )
    importResult.value = data
  } catch (e) {
    error.value = e.message || String(e)
    importResult.value = null
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <p class="admin-nav">
      <RouterLink to="/admin" class="nav-link">← 管理首页</RouterLink>
    </p>
    <h2 class="title">题库管理 · Markdown 导入</h2>
    <p class="muted intro">
      在下方两个文本框中按约定格式粘贴 Markdown，后端会解析为题目、参考答案与关键词。请在
      <code>application-local.yml</code> 中配置 <code>app.admin.api-key</code>，并在此填写相同密钥。
    </p>

    <div class="card form">
      <label>管理员密钥（请求头 X-Admin-Key）</label>
      <input v-model="adminKey" type="password" class="input" autocomplete="off" placeholder="与后端 app.admin.api-key 一致" />

      <label>季节标签 seasonLabel</label>
      <input v-model="seasonLabel" class="input" placeholder="如 2025-9-12" />

      <label class="row">
        <input v-model="replaceExisting" type="checkbox" />
        导入前删除该季节已有题目（同一 seasonLabel）
      </label>

      <label>Part 1 · Markdown</label>
      <textarea
        v-model="part1Markdown"
        class="textarea"
        rows="12"
        placeholder="## Topic: 话题名

### Question
题目英文？

### Answer
参考答案段落

### Keywords
- 关键词1
- 关键词2"
      />

      <label>Part 2 &amp; 3 · Markdown</label>
      <textarea
        v-model="part23Markdown"
        class="textarea"
        rows="16"
        placeholder="## Topic: 话题名

### Part 2

#### Cue card
Describe ...（含 You should say 要点）

#### Answer
范文

#### Keywords
- a

### Part 3

#### Question
讨论题？

#### Answer
参考

#### Keywords
- x"
      />

      <div class="actions">
        <button type="button" class="btn secondary" :disabled="loading" @click="runPreview">仅预览解析</button>
        <button type="button" class="btn" :disabled="loading" @click="runImport">解析并写入数据库</button>
      </div>
    </div>

    <p v-if="loading" class="muted">处理中…</p>
    <p v-if="error" class="err">{{ error }}</p>

    <div v-if="preview" class="card result">
      <h3>预览</h3>
      <p>
        Part1: {{ preview.part1Count }} 条 · Part2: {{ preview.part2Count }} 条 · Part3:
        {{ preview.part3Count }} 条
      </p>
      <ul v-if="preview.warnings?.length" class="warn-list">
        <li v-for="(w, i) in preview.warnings" :key="i">{{ w }}</li>
      </ul>
      <div v-for="(q, i) in preview.sampleItems" :key="i" class="sample">
        <span class="badge">{{ q.part }}</span>
        <p class="topic">{{ q.topic }}</p>
        <p class="qtext">{{ q.questionText?.slice(0, 280) }}{{ (q.questionText?.length || 0) > 280 ? '…' : '' }}</p>
      </div>
    </div>

    <div v-if="importResult" class="card result ok">
      <h3>导入完成</h3>
      <p>
        已写入 {{ importResult.inserted }} 条（Part1: {{ importResult.part1Count }} · Part2:
        {{ importResult.part2Count }} · Part3: {{ importResult.part3Count }}）
      </p>
      <ul v-if="importResult.warnings?.length" class="warn-list">
        <li v-for="(w, i) in importResult.warnings" :key="i">{{ w }}</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 720px;
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

.row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.input {
  width: 100%;
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
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.85rem;
  line-height: 1.45;
  resize: vertical;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.5);
  background: rgba(14, 165, 233, 0.25);
  color: #e0f2fe;
  cursor: pointer;
  font-size: 0.95rem;
}

.btn:hover:not(:disabled) {
  background: rgba(14, 165, 233, 0.4);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.secondary {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(30, 41, 59, 0.6);
}

.err {
  color: #fca5a5;
  margin: 0.75rem 0 0;
}

.result h3 {
  margin: 0 0 0.5rem;
  color: #f1f5f9;
}

.result.ok {
  border-color: rgba(34, 197, 94, 0.35);
}

.warn-list {
  margin: 0.5rem 0 0;
  padding-left: 1.25rem;
  color: #fcd34d;
  font-size: 0.9rem;
}

.sample {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(51, 65, 85, 0.6);
}

.badge {
  display: inline-block;
  font-size: 0.75rem;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  background: rgba(51, 65, 85, 0.9);
  color: #94a3b8;
}

.topic {
  font-weight: 600;
  margin: 0.35rem 0 0.25rem;
  color: #f8fafc;
}

.qtext {
  margin: 0;
  font-size: 0.88rem;
  color: #94a3b8;
  line-height: 1.45;
}
</style>
