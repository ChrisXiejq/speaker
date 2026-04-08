<script setup>
import { ref, watch, nextTick, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { http } from '@/api/http'
import { toastError, toastWarning } from '@/utils/toast'
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
  } catch (e) {
    bankSeasons.value = []
    season.value = ''
    toastError(e?.message || '加载题库季节失败')
  }
}

onMounted(() => {
  loadBankSeasons()
})

async function start() {
  const part = PART_API[partIndex.value]
  const topicSource = SOURCES[sourceIndex.value]
  if (topicSource === 'CUSTOM' && !customTopic.value.trim()) {
    toastWarning('请输入自定义话题')
    return
  }
  if (topicSource === 'BANK' && !season.value?.trim()) {
    toastWarning('题库暂无可用季节，请先在管理端导入当季题目')
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
    toastError(e.message || '开始练习失败')
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
    toastError(e.message || '切换话题失败')
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
    ElMessageBox.confirm('模型建议可结束本轮，是否生成完整评价？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
      .then(() => {
        void finish()
      })
      .catch(() => {})
  }
  if (data.strictTopicFinished) {
    ElMessageBox.confirm('本题话题库题已问完，是否进入下一话题？', '提示', {
      confirmButtonText: '进入',
      cancelButtonText: '取消',
      type: 'info',
    })
      .then(() => {
        void nextTopic()
      })
      .catch(() => {})
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
    toastError(e.message || '提交回答失败')
  } finally {
    loading.value = false
  }
}

function listenBrowser() {
  const R = getRecognition()
  if (!R) {
    toastWarning('当前浏览器不支持 Web Speech 识别，请改用「录音上传 ASR」')
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
    toastError('无法启动识别，请稍后重试')
  }
}

async function toggleRecordUpload() {
  if (!recording.value) {
    if (!navigator.mediaDevices?.getUserMedia) {
      toastWarning('无法访问麦克风，请检查权限')
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
      toastWarning('未识别到有效内容，请重试')
    }
  } catch (e) {
    toastError(e.message || '语音识别失败')
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
    toastError(e.message || '生成评价失败')
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
  <div class="page">
    <el-card v-if="!started" class="start-card" shadow="hover">
      <template #header>
        <span class="card-title">开始模拟</span>
      </template>
      <el-form label-position="top" class="start-form">
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
        <el-form-item label="题目来源">
          <el-select v-model="sourceIndex" placeholder="来源" style="width: 100%">
            <el-option v-for="(s, i) in SOURCES" :key="s" :label="s" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="季节（题库模式）">
          <el-select
            v-model="season"
            placeholder="选择季节"
            style="width: 100%"
            :disabled="!bankSeasons.length"
          >
            <el-option v-for="s in bankSeasons" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="SOURCES[sourceIndex] === 'BANK' && !bankSeasons.length"
          title="暂无季节数据，请先在管理端导入题目。"
          type="warning"
          show-icon
          :closable="false"
          class="mb-alert"
        />
        <el-form-item v-if="SOURCES[sourceIndex] === 'BANK'">
          <el-checkbox v-model="allowAiExpand">
            允许 AI 扩展题库（先问完本题话题库题，再扩展提问并标注「此问题为AI扩展」）
          </el-checkbox>
        </el-form-item>
        <el-form-item v-if="SOURCES[sourceIndex] === 'CUSTOM'" label="自定义话题">
          <el-input v-model="customTopic" placeholder="例如 travel / technology" clearable />
        </el-form-item>
        <el-button
          type="primary"
          class="start-btn"
          :loading="loading"
          round
          @click="start"
        >
          开始模拟
        </el-button>
      </el-form>
    </el-card>

    <template v-else>
      <el-card class="session-card" shadow="hover">
        <template #header>
          <span class="card-title">当前题目</span>
        </template>
        <p class="label">Topic</p>
        <p class="block">{{ topic }}</p>
        <el-tag v-if="aiExpandedQuestion" type="warning" effect="dark" class="tag-ai" round>
          此问题为 AI 扩展
        </el-tag>
        <p class="label mt">考官</p>
        <p class="block">{{ examinerLine }}</p>
        <el-collapse
          v-if="bankTopicFlow && (referenceAnswer || parseKeywords(keywordsJson).length)"
          class="ref-collapse"
        >
          <el-collapse-item title="参考答案与关键词" name="ref">
            <p v-if="referenceAnswer" class="answer">{{ referenceAnswer }}</p>
            <ul v-if="parseKeywords(keywordsJson).length" class="kw">
              <li v-for="(k, i) in parseKeywords(keywordsJson)" :key="i">{{ k }}</li>
            </ul>
          </el-collapse-item>
        </el-collapse>
        <template v-if="lastBrief">
          <p class="label mt">上一答简评</p>
          <p class="block">{{ lastBrief }}</p>
        </template>
      </el-card>

      <el-card class="session-card" shadow="hover">
        <template #header>
          <span class="card-title">听说练习</span>
        </template>
        <el-alert
          title="题目更新后会自动朗读。可随时点击「再听一遍」。说完后系统会识别、显示并自动提交，无需手动提交。"
          type="info"
          show-icon
          :closable="false"
          class="mb-alert"
        />
        <div class="row-btns">
          <el-button
            :disabled="loading || !browserTtsOk"
            round
            @click="playExaminerBrowser"
          >
            再听一遍题目
          </el-button>
        </div>
        <div class="row-btns">
          <el-button :disabled="loading || !browserAsrOk" round @click="listenBrowser">
            口语识别（本机）
          </el-button>
          <el-button :disabled="loading" round @click="toggleRecordUpload">
            {{ recording ? '停止并上传 ASR' : '录音上传 ASR' }}
          </el-button>
        </div>
        <p v-if="speechHint" class="speech-hint">{{ speechHint }}</p>

        <div
          v-if="bankTopicFlow && (canAdvanceTopic || practicePhase === 'AWAIT_NEXT_TOPIC')"
          class="row-btns"
        >
          <el-button type="primary" plain :disabled="loading" round @click="nextTopic">
            进入下一话题
          </el-button>
        </div>

        <template v-if="lastRecognizedText">
          <p class="label">识别并提交的回答</p>
          <p class="answer-readonly">{{ lastRecognizedText }}</p>
        </template>

        <div class="footer-btns">
          <el-button :disabled="loading" round @click="finish">结束并生成评价</el-button>
          <el-button round @click="reset">重新开始</el-button>
        </div>
      </el-card>

      <el-card v-if="report" class="report-card" shadow="hover">
        <template #header>
          <span class="card-title">评分与反馈</span>
        </template>
        <el-descriptions :column="1" border size="small" class="band-desc">
          <el-descriptions-item label="Overall">
            {{ report.overallBand }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="row5">
          <el-tag type="success" effect="plain">P {{ report.pronunciationScore }}</el-tag>
          <el-tag type="success" effect="plain">G {{ report.grammarScore }}</el-tag>
          <el-tag type="success" effect="plain">C {{ report.coherenceScore }}</el-tag>
          <el-tag type="success" effect="plain">F {{ report.fluencyScore }}</el-tag>
          <el-tag type="success" effect="plain">I {{ report.ideasScore }}</el-tag>
        </div>
        <p class="feedback">{{ report.detailedFeedback }}</p>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
}

.card-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: #ecfdf5;
}

.start-form {
  max-width: 520px;
}

.start-btn {
  width: 100%;
  margin-top: 0.5rem;
  background: linear-gradient(135deg, #16a34a, #059669) !important;
  border: none !important;
}

.mb-alert {
  margin-bottom: 0.75rem;
}

.label {
  margin: 0.5rem 0 0.25rem;
  font-size: 0.85rem;
  color: rgba(167, 243, 208, 0.75);
}

.label:first-of-type {
  margin-top: 0;
}

.block {
  margin: 0.25rem 0 0;
  line-height: 1.65;
  color: #ecfdf5;
}

.tag-ai {
  margin-top: 0.5rem;
}

.answer-readonly {
  margin: 0.35rem 0 0.75rem;
  padding: 0.65rem 0.85rem;
  border-radius: 10px;
  border: 1px solid rgba(74, 222, 128, 0.35);
  background: rgba(6, 40, 28, 0.45);
  color: #e2e8f0;
  line-height: 1.55;
  white-space: pre-wrap;
}

.mt {
  margin-top: 0.85rem;
}

.row5 {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0.75rem 0;
}

.feedback {
  margin-top: 0.75rem;
  line-height: 1.65;
  color: #d1fae5;
  white-space: pre-wrap;
}

.ref-collapse {
  margin-top: 0.75rem;
  border: none;
  --el-collapse-header-bg-color: transparent;
}

:deep(.ref-collapse .el-collapse-item__header) {
  color: rgba(167, 243, 208, 0.95);
  font-weight: 500;
}

:deep(.ref-collapse .el-collapse-item__wrap) {
  border-bottom: none;
}

.answer {
  margin: 0.5rem 0 0;
  color: #d1fae5;
  line-height: 1.55;
  white-space: pre-wrap;
}

.kw {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
  color: #86efac;
  font-size: 0.88rem;
}

.row-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.row-btns .el-button {
  flex: 1;
  min-width: 140px;
}

.speech-hint {
  font-size: 0.85rem;
  color: #4ade80;
  margin: 0.25rem 0 0.75rem;
}

.footer-btns {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.footer-btns .el-button {
  margin: 0;
}

.band-desc {
  margin-bottom: 0.75rem;
}

.session-card,
.start-card,
.report-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}
</style>
