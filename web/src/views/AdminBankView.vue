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

    <el-card class="form-card" shadow="hover">
      <el-form label-position="top">
        <el-form-item label="管理员密钥（请求头 X-Admin-Key）">
          <el-input
            v-model="adminKey"
            type="password"
            autocomplete="off"
            placeholder="与后端 app.admin.api-key 一致"
            show-password
          />
        </el-form-item>
        <el-form-item label="季节标签 seasonLabel">
          <el-input v-model="seasonLabel" placeholder="如 2025-9-12" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="replaceExisting">导入前删除该季节已有题目（同一 seasonLabel）</el-checkbox>
        </el-form-item>
        <el-form-item label="Part 1 · Markdown">
          <el-input
            v-model="part1Markdown"
            type="textarea"
            :rows="12"
            class="mono"
            placeholder="## Topic: 话题名

### Question
题目英文？

### Answer
参考答案段落

### Keywords
- 关键词1
- 关键词2"
          />
        </el-form-item>
        <el-form-item label="Part 2 & 3 · Markdown">
          <el-input
            v-model="part23Markdown"
            type="textarea"
            :rows="16"
            class="mono"
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
        </el-form-item>
        <div class="actions">
          <el-button :disabled="loading" round @click="runPreview">仅预览解析</el-button>
          <el-button type="primary" :loading="loading" round @click="runImport">解析并写入数据库</el-button>
        </div>
      </el-form>
    </el-card>

    <el-alert v-if="loading" title="处理中…" type="info" show-icon :closable="false" class="alert-block" />
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="alert-block" />

    <el-card v-if="preview" class="result-card" shadow="hover">
      <template #header>
        <span class="card-h">预览</span>
      </template>
      <p>
        Part1: {{ preview.part1Count }} 条 · Part2: {{ preview.part2Count }} 条 · Part3:
        {{ preview.part3Count }} 条
      </p>
      <template v-if="preview.warnings?.length">
        <el-alert
          v-for="(w, i) in preview.warnings"
          :key="i"
          :title="w"
          type="warning"
          show-icon
          :closable="false"
          class="warn-item"
        />
      </template>
      <div v-for="(q, i) in preview.sampleItems" :key="i" class="sample">
        <el-tag size="small" effect="plain">{{ q.part }}</el-tag>
        <p class="topic">{{ q.topic }}</p>
        <p class="qtext">{{ q.questionText?.slice(0, 280) }}{{ (q.questionText?.length || 0) > 280 ? '…' : '' }}</p>
      </div>
    </el-card>

    <el-card v-if="importResult" class="result-card ok" shadow="hover">
      <template #header>
        <span class="card-h">导入完成</span>
      </template>
      <p>
        已写入 {{ importResult.inserted }} 条（Part1: {{ importResult.part1Count }} · Part2:
        {{ importResult.part2Count }} · Part3: {{ importResult.part3Count }}）
      </p>
      <template v-if="importResult.warnings?.length">
        <el-alert
          v-for="(w, i) in importResult.warnings"
          :key="i"
          :title="w"
          type="warning"
          show-icon
          :closable="false"
          class="warn-item"
        />
      </template>
    </el-card>
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

.form-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

:deep(.mono textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.85rem;
  line-height: 1.45;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.alert-block {
  margin-bottom: 0.75rem;
}

.result-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.result-card.ok {
  border-color: rgba(74, 222, 128, 0.35) !important;
}

.card-h {
  font-weight: 600;
  color: #ecfdf5;
}

.warn-item {
  margin-top: 0.5rem;
}

.sample {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(74, 222, 128, 0.12);
}

.topic {
  font-weight: 600;
  margin: 0.35rem 0 0.25rem;
  color: #ecfdf5;
}

.qtext {
  margin: 0;
  font-size: 0.88rem;
  color: rgba(209, 250, 229, 0.82);
  line-height: 1.45;
}

.muted {
  color: rgba(167, 243, 208, 0.72);
}
</style>
