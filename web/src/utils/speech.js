/** 浏览器端朗读与识别（Chrome/Edge 效果较好；需 HTTPS 或 localhost） */

/** 口语识别：最后一次听到语音后，再静默这么久才自动提交（毫秒） */
export const SPEECH_SILENCE_MS_BEFORE_SUBMIT = 5000

export function canSpeak() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

export function canRecognize() {
  if (typeof window === 'undefined') return false
  return !!(window.SpeechRecognition || window.webkitSpeechRecognition)
}

/**
 * @param {string} text
 * @param {{ lang?: string, rate?: number, onEnd?: () => void }} [opts]
 * @returns {boolean}
 */
export function speakEnglish(text, opts = {}) {
  if (!canSpeak() || !text?.trim()) return false
  const { lang = 'en-GB', rate = 0.92, onEnd } = opts
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(text)
  u.lang = lang
  u.rate = rate
  if (onEnd) u.onend = onEnd
  window.speechSynthesis.speak(u)
  return true
}

/** @returns {SpeechRecognition | null} */
export function getRecognition() {
  if (typeof window === 'undefined') return null
  const Ctor = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!Ctor) return null
  const r = new Ctor()
  r.lang = 'en-GB'
  r.continuous = false
  r.interimResults = false
  r.maxAlternatives = 1
  return r
}
