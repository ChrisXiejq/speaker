<script setup>
import { ref, watch, nextTick, computed, onMounted } from 'vue'
import { http } from '@/api/http'
import {
  speakEnglish,
  getRecognition,
  canSpeak,
  canRecognize,
  SPEECH_SILENCE_MS_BEFORE_SUBMIT,
} from '@/utils/speech'

/** 与后端 PracticeSession.Part 一致：PART1 | PART2_AND_3 */
const PART_API = ['PART1', 'PART2_AND_3']
const PART_LABEL = ['Part 1', 'Part 2 & 3']
const SOURCES = ['BANK', 'CUSTOM']

function parseKeywords(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const partIndex = ref(0)
const sourceIndex = ref(0)
/** 与 /api/bank/seasons 一致，首项为当前日期下的默认季 */
const bankSeasons = ref([])
const season = ref('')
const customTopic = ref('')
/** 仅题库模式：是否允许问完本题话题库题后由 AI 扩展提问 */
const allowAiExpand = ref(false)
const bankTopicFlow = ref(false)
const practicePhase = ref('')
const referenceAnswer = ref('')
const keywordsJson = ref('')
const canAdvanceTopic = ref(false)
const aiExpandedQuestion = ref(false)

const sessionId = ref(0)
const examinerLine = ref('')
const topic = ref('')
const started = ref(false)
const lastBrief = ref('')
const report = ref(null)
const loading = ref(false)

/** 最近一次识别并提交的回答（只读展示） */
const lastRecognizedText = ref('')

const speechHint = ref('')
const recording = ref(false)
let mediaRecorder = null
let mediaChunks = []
let recordStream = null

const browserTtsOk = computed(() => canSpeak())
const browserAsrOk = computed(() => canRecognize())

watch(examinerLine, (line) => {
  if (!sessionId.value || !line?.trim()) return
  if (!canSpeak()) return
  nextTick(() => {
    speakEnglish(line)
  })
})

async function loadBankSeasons() {
  try {
    const { data } = await http.get('/api/bank/seasons')
    bankSeasons.value = Array.isArray(data) ? data : []
    if (bankSeasons.value.length) {
      season.value = bankSeasons.value[0]
    } else {
      season.value = ''
    }
  } catch {
    bankSeasons.value = []
    season.value = ''
  }
}

onMounted(() => {
  loadBankSeasons()
})

async function start() {
  const part = PART_API[partIndex.value]
  const topicSource = SOURCES[sourceIndex.value]
  if (topicSource === 'CUSTOM' && !customTopic.value.trim()) {
    alert('请输入自定义话题')
    return
  }
  if (topicSource === 'BANK' && !season.value?.trim()) {
    alert('题库暂无可用季节，请先在管理端导入当季题目')
    return
  }
  loading.value = true
  report.value = null
  lastRecognizedText.value = ''
  try {
    const body = {
      part,
      topicSource,
      season: season.value,
      bankQuestionId: null,
    }
    if (topicSource === 'CUSTOM') body.customTopic = customTopic.value.trim()
    if (topicSource === 'BANK') body.allowAiExpand = allowAiExpand.value
    const { data } = await http.post('/api/practice/sessions', body)
    sessionId.value = data.sessionId
    examinerLine.value = data.examinerLine
    topic.value = data.topic
    bankTopicFlow.value = !!data.bankTopicFlow
    practicePhase.value = data.practicePhase ?? ''
    referenceAnswer.value = data.referenceAnswer ?? ''
    keywordsJson.value = data.keywordsJson ?? ''
    canAdvanceTopic.value = false
    aiExpandedQuestion.value = false
    lastBrief.value = ''
    started.value = true
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function playExaminerBrowser() {
  if (!examinerLine.value) return
  speakEnglish(examinerLine.value)
}

async function nextTopic() {
  if (!sessionId.value || loading.value) return
  loading.value = true
  try {
    const { data } = await http.post(`/api/practice/sessions/${sessionId.value}/next-topic`)
    examinerLine.value = data.examinerLine
    topic.value = data.topic
    referenceAnswer.value = data.referenceAnswer ?? ''
    keywordsJson.value = data.keywordsJson ?? ''
    practicePhase.value = data.practicePhase ?? ''
    canAdvanceTopic.value = false
    aiExpandedQuestion.value = false
    lastRecognizedText.value = ''
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function applyReplyMeta(data) {
  examinerLine.value = data.examinerLine
  lastBrief.value = data.briefEval
  referenceAnswer.value = data.referenceAnswer ?? ''
  keywordsJson.value = data.keywordsJson ?? ''
  practicePhase.value = data.practicePhase ?? ''
  canAdvanceTopic.value = !!data.canAdvanceTopic
  aiExpandedQuestion.value = !!data.aiExpandedQuestion
  if (data.shouldEnd) {
    const ok = window.confirm('模型建议可结束本轮，是否生成完整评价？')
    if (ok) void finish()
  }
  if (data.strictTopicFinished) {
    const ok = window.confirm('本题话题库题已问完，是否进入下一话题？')
    if (ok) void nextTopic()
  }
}

/**
 * 识别结果展示后自动提交；同一轮识别内只提交一次。
 */
async function submitReply(text) {
  if (!sessionId.value || !text?.trim()) return
  if (loading.value) return
  const trimmed = text.trim()
  lastRecognizedText.value = trimmed
  loading.value = true
  try {
    const { data } = await http.post(`/api/practice/sessions/${sessionId.value}/reply`, {
      userText: trimmed,
    })
    applyReplyMeta(data)
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function listenBrowser() {
  const R = getRecognition()
  if (!R) {
    alert('当前浏览器不支持 Web Speech 识别，请改用「录音上传 ASR」')
    return
  }
  if (loading.value) return

  R.continuous = true
  R.interimResults = true

  let submitted = false
  let accumulated = ''
  let silenceTimer = null

  const clearSilenceTimer = () => {
    if (silenceTimer != null) {
      clearTimeout(silenceTimer)
      silenceTimer = null
    }
  }

  const finalizeFromSilence = () => {
    if (submitted) return
    const text = accumulated.trim()
    if (!text) {
      clearSilenceTimer()
      try {
        R.stop()
      } catch {
        /* ignore */
      }
      speechHint.value = ''
      return
    }
    submitted = true
    clearSilenceTimer()
    try {
      R.stop()
    } catch {
      /* ignore */
    }
    speechHint.value = ''
    void submitReply(text)
  }

  const bumpSilenceTimer = () => {
    clearSilenceTimer()
    silenceTimer = window.setTimeout(finalizeFromSilence, SPEECH_SILENCE_MS_BEFORE_SUBMIT)
  }

  R.onresult = (ev) => {
    const parts = []
    for (let i = 0; i < ev.results.length; i++) {
      if (ev.results[i].isFinal) {
        parts.push(ev.results[i][0].transcript.trim())
      }
    }
    const joined = parts.join(' ').trim()
    if (joined) {
      accumulated = joined
    }

    // 任意识别活动（含 interim）都视为仍在说话，重置静默计时
    bumpSilenceTimer()
    speechHint.value = `聆听中…说完后停顿约 ${SPEECH_SILENCE_MS_BEFORE_SUBMIT / 1000} 秒将自动提交`
  }

  R.onerror = (ev) => {
    clearSilenceTimer()
    speechHint.value = ''
    if (ev.error === 'aborted' || ev.error === 'no-speech') return
  }

  R.onend = () => {
    // 浏览器主动结束会话时，若仍有待处理的静默定时器则保留，由定时器提交
    speechHint.value = ''
  }

  speechHint.value = `聆听中…说完后停顿约 ${SPEECH_SILENCE_MS_BEFORE_SUBMIT / 1000} 秒将自动提交`
  try {
    R.start()
  } catch {
    clearSilenceTimer()
    speechHint.value = ''
    alert('无法启动识别，请稍后重试')
  }
}

async function toggleRecordUpload() {
  if (!recording.value) {
    if (!navigator.mediaDevices?.getUserMedia) {
      alert('无法访问麦克风，请检查权限')
      return
    }
    recordStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaChunks = []
    mediaRecorder = new MediaRecorder(recordStream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size) mediaChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      recordStream?.getTracks().forEach((t) => t.stop())
      recordStream = null
      const blob = new Blob(mediaChunks, { type: mediaRecorder.mimeType || 'audio/webm' })
      mediaRecorder = null
      await uploadAsrBlob(blob)
    }
    mediaRecorder.start()
    recording.value = true
    speechHint.value = '录音中，再次点击结束并识别'
  } else {
    mediaRecorder?.stop()
    recording.value = false
    speechHint.value = ''
  }
}

async function uploadAsrBlob(blob) {
  loading.value = true
  try {
    const fd = new FormData()
    const ext = blob.type.includes('webm') ? 'webm' : blob.type.includes('wav') ? 'wav' : 'mp3'
    fd.append('file', blob, `rec.${ext}`)
    const { data } = await http.post('/api/practice/asr', fd)
    loading.value = false
    if (data.text?.trim()) {
      await submitReply(data.text)
    } else {
      alert('未识别到有效内容，请重试')
    }
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

async function finish() {
  if (!sessionId.value) return
  loading.value = true
  try {
    const { data } = await http.post(`/api/practice/sessions/${sessionId.value}/complete`)
    report.value = data
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function reset() {
  sessionId.value = 0
  examinerLine.value = ''
  topic.value = ''
  started.value = false
  lastBrief.value = ''
  lastRecognizedText.value = ''
  report.value = null
  speechHint.value = ''
  recording.value = false
  bankTopicFlow.value = false
  practicePhase.value = ''
  referenceAnswer.value = ''
  keywordsJson.value = ''
  canAdvanceTopic.value = false
  aiExpandedQuestion.value = false
}
</script>

<template>
  <div>
    <div v-if="!started" class="card">
      <h2>开始模拟</h2>
      <label>Part</label>
      <select v-model.number="partIndex" class="input">
        <option v-for="(label, i) in PART_LABEL" :key="PART_API[i]" :value="i">{{ label }}</option>
      </select>
      <label>题目来源</label>
      <select v-model.number="sourceIndex" class="input">
        <option v-for="(s, i) in SOURCES" :key="s" :value="i">{{ s }}</option>
      </select>
      <label>季节（题库模式）</label>
      <select v-model="season" class="input" :disabled="!bankSeasons.length">
        <option v-if="!bankSeasons.length" disabled value="">暂无季节</option>
        <option v-for="s in bankSeasons" :key="s" :value="s">{{ s }}</option>
      </select>
      <p v-if="SOURCES[sourceIndex] === 'BANK' && !bankSeasons.length" class="muted small">
        暂无季节数据，请先在管理端导入题目。
      </p>
      <label v-if="SOURCES[sourceIndex] === 'BANK'" class="switch-row">
        <input v-model="allowAiExpand" type="checkbox" />
        <span>允许 AI 扩展题库（先问完本题话题库题，再扩展提问并标注「此问题为AI扩展」）</span>
      </label>
      <template v-if="SOURCES[sourceIndex] === 'CUSTOM'">
        <label>自定义话题</label>
        <input v-model="customTopic" class="input" type="text" placeholder="例如 travel / technology" />
      </template>
      <button type="button" class="btn primary" :disabled="loading" @click="start">
        {{ loading ? '…' : '开始模拟' }}
      </button>
    </div>

    <template v-else>
      <div class="card">
        <p class="muted">Topic</p>
        <p class="block">{{ topic }}</p>
        <p v-if="aiExpandedQuestion" class="badge-ai">此问题为 AI 扩展</p>
        <p class="muted mt">考官</p>
        <p class="block">{{ examinerLine }}</p>
        <details v-if="bankTopicFlow && (referenceAnswer || parseKeywords(keywordsJson).length)" class="ref-box">
          <summary>参考答案与关键词</summary>
          <p v-if="referenceAnswer" class="answer">{{ referenceAnswer }}</p>
          <ul v-if="parseKeywords(keywordsJson).length" class="kw">
            <li v-for="(k, i) in parseKeywords(keywordsJson)" :key="i">{{ k }}</li>
          </ul>
        </details>
        <template v-if="lastBrief">
          <p class="muted mt">上一答简评</p>
          <p class="block">{{ lastBrief }}</p>
        </template>
      </div>

      <div class="card">
        <p class="muted">听说练习</p>
        <p class="hint">
          题目更新后会自动朗读。可随时点击「再听一遍」。说完后系统会识别、显示并自动提交，无需手动提交。
        </p>
        <div class="row-btns">
          <button
            type="button"
            class="btn outline row-btn"
            :disabled="loading || !browserTtsOk"
            @click="playExaminerBrowser"
          >
            再听一遍题目
          </button>
        </div>
        <div class="row-btns">
          <button type="button" class="btn outline row-btn" :disabled="loading || !browserAsrOk" @click="listenBrowser">
            口语识别（本机）
          </button>
          <button type="button" class="btn outline row-btn" :disabled="loading" @click="toggleRecordUpload">
            {{ recording ? '停止并上传 ASR' : '录音上传 ASR' }}
          </button>
        </div>
        <p v-if="speechHint" class="speech-hint">{{ speechHint }}</p>

        <div
          v-if="bankTopicFlow && (canAdvanceTopic || practicePhase === 'AWAIT_NEXT_TOPIC')"
          class="row-btns"
        >
          <button type="button" class="btn outline row-btn" :disabled="loading" @click="nextTopic">
            进入下一话题
          </button>
        </div>

        <template v-if="lastRecognizedText">
          <p class="muted">识别并提交的回答</p>
          <p class="answer-readonly">{{ lastRecognizedText }}</p>
        </template>

        <button type="button" class="btn outline" :disabled="loading" @click="finish">结束并生成评价</button>
        <button type="button" class="btn outline" @click="reset">重新开始</button>
      </div>

      <div v-if="report" class="card">
        <h3>评分与反馈</h3>
        <p class="muted">Overall: {{ report.overallBand }}</p>
        <div class="row5">
          <span>P {{ report.pronunciationScore }}</span>
          <span>G {{ report.grammarScore }}</span>
          <span>C {{ report.coherenceScore }}</span>
          <span>F {{ report.fluencyScore }}</span>
          <span>I {{ report.ideasScore }}</span>
        </div>
        <p class="feedback">{{ report.detailedFeedback }}</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
h2,
h3 {
  margin: 0 0 1rem;
  color: #f8fafc;
}

label {
  display: block;
  margin: 0.75rem 0 0.35rem;
  color: #cbd5e1;
  font-size: 0.9rem;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(2, 6, 23, 0.5);
  color: #e2e8f0;
  margin-bottom: 0.25rem;
}

.block {
  margin: 0.25rem 0 0;
  line-height: 1.6;
  color: #e2e8f0;
}

.answer-readonly {
  margin: 0.35rem 0 0.75rem;
  padding: 0.65rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.25);
  background: rgba(2, 6, 23, 0.45);
  color: #e2e8f0;
  line-height: 1.55;
  white-space: pre-wrap;
}

.mt {
  margin-top: 0.75rem;
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
  color: #e2e8f0;
  white-space: pre-wrap;
}

.switch-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin: 0.75rem 0 0;
  font-size: 0.88rem;
  color: #cbd5e1;
  line-height: 1.45;
}

.switch-row input {
  margin-top: 0.2rem;
}

.badge-ai {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: #fbbf24;
}

.ref-box {
  margin-top: 0.75rem;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(2, 6, 23, 0.35);
}

.ref-box summary {
  cursor: pointer;
  color: #94a3b8;
  font-size: 0.88rem;
}

.ref-box .answer {
  margin: 0.5rem 0 0;
  color: #cbd5e1;
  line-height: 1.55;
  white-space: pre-wrap;
}

.ref-box .kw {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
  color: #a5b4fc;
  font-size: 0.88rem;
}

.small {
  font-size: 0.85rem;
  margin: 0.35rem 0 0;
  color: #94a3b8;
}

.hint {
  font-size: 0.85rem;
  color: #94a3b8;
  line-height: 1.5;
  margin: 0 0 0.75rem;
}

.row-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.speech-hint {
  font-size: 0.85rem;
  color: #38bdf8;
  margin: 0.25rem 0 0.75rem;
}

.btn {
  display: block;
  width: 100%;
  margin-top: 0.75rem;
  padding: 0.6rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
}

.row-btns .row-btn {
  width: auto;
  flex: 1;
  min-width: 140px;
  margin-top: 0;
}

.btn.primary {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: #fff;
}

.btn.outline {
  background: transparent;
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
