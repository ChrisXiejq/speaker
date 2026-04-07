import { request, uploadAsr, downloadTtsToTemp } from "../../utils/api";

type StartResp = {
  sessionId: number;
  examinerLine: string;
  part: string;
  topic: string;
};

type ReplyResp = {
  examinerLine: string;
  briefEval: string;
  shouldEnd: boolean;
};

type Report = {
  id: number;
  pronunciationScore: number;
  grammarScore: number;
  coherenceScore: number;
  fluencyScore: number;
  ideasScore: number;
  overallBand: string;
  detailedFeedback: string;
  suggestionsJson: string;
};

const PARTS = ["PART1", "PART2", "PART3"];
const SOURCES = ["BANK", "CUSTOM"];

Page({
  data: {
    partIndex: 0,
    sourceIndex: 0,
    parts: PARTS,
    sources: SOURCES,
    season: "2025Q1",
    customTopic: "",
    sessionId: 0 as number,
    examinerLine: "",
    topic: "",
    userText: "",
    busy: false,
    started: false,
    lastBrief: "",
    report: null as Report | null,
    recording: false,
    speechHint: "",
  },
  innerAudio: null as WechatMiniprogram.InnerAudioContext | null,
  recorder: null as WechatMiniprogram.RecorderManager | null,

  onLoad() {
    this.innerAudio = wx.createInnerAudioContext();
    this.innerAudio.onEnded(() => {
      this.setData({ speechHint: "" });
    });
    this.innerAudio.onError(() => {
      this.setData({ speechHint: "" });
      wx.showToast({ title: "音频播放失败", icon: "none" });
    });
    this.recorder = wx.getRecorderManager();
    this.recorder.onStop((res) => {
      this.setData({ recording: false });
      void this.afterRecordStop(res.tempFilePath);
    });
    this.recorder.onError(() => {
      this.setData({ recording: false });
      wx.showToast({ title: "录音失败", icon: "none" });
    });
  },

  onUnload() {
    this.innerAudio?.stop();
    this.innerAudio?.destroy();
    this.innerAudio = null;
  },

  onSeason(e: WechatMiniprogram.Input) {
    this.setData({ season: e.detail.value });
  },
  onCustomTopic(e: WechatMiniprogram.Input) {
    this.setData({ customTopic: e.detail.value });
  },
  onUserText(e: WechatMiniprogram.Input) {
    this.setData({ userText: e.detail.value });
  },
  onPartChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ partIndex: Number(e.detail.value) });
  },
  onSourceChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ sourceIndex: Number(e.detail.value) });
  },

  async playExaminerTts() {
    if (!this.data.examinerLine?.trim()) return;
    this.setData({ busy: true, speechHint: "加载语音…" });
    try {
      const path = await downloadTtsToTemp(this.data.examinerLine);
      if (!this.innerAudio) return;
      this.innerAudio.stop();
      this.innerAudio.src = path;
      this.setData({ speechHint: "播放题目…" });
      this.innerAudio.play();
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "播放失败", icon: "none" });
      this.setData({ speechHint: "" });
    } finally {
      this.setData({ busy: false });
    }
  },

  toggleRecord() {
    if (!this.recorder) return;
    if (!this.data.recording) {
      this.recorder.start({
        duration: 120000,
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 96000,
        format: "mp3",
      });
      this.setData({ recording: true, speechHint: "录音中，再点一次结束并识别" });
    } else {
      this.recorder.stop();
    }
  },

  async afterRecordStop(tempPath: string) {
    this.setData({ busy: true, speechHint: "识别中…" });
    try {
      const res = await uploadAsr(tempPath);
      this.setData({ userText: res.text, speechHint: "" });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "识别失败", icon: "none" });
      this.setData({ speechHint: "" });
    } finally {
      this.setData({ busy: false });
    }
  },

  async start() {
    const part = PARTS[this.data.partIndex];
    const topicSource = SOURCES[this.data.sourceIndex];
    if (topicSource === "CUSTOM" && !this.data.customTopic.trim()) {
      wx.showToast({ title: "请输入自定义话题", icon: "none" });
      return;
    }
    this.setData({ busy: true });
    try {
      const body: Record<string, unknown> = {
        part,
        topicSource,
        season: this.data.season,
        bankQuestionId: null,
      };
      if (topicSource === "CUSTOM") {
        body.customTopic = this.data.customTopic.trim();
      }
      const res = await request<StartResp>("/api/practice/sessions", "POST", body);
      this.setData({
        sessionId: res.sessionId,
        examinerLine: res.examinerLine,
        topic: res.topic,
        started: true,
        report: null,
        userText: "",
        lastBrief: "",
        speechHint: "",
      });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "开始失败", icon: "none" });
    } finally {
      this.setData({ busy: false });
    }
  },
  async send() {
    if (!this.data.sessionId || !this.data.userText.trim()) {
      wx.showToast({ title: "请先说出或输入回答", icon: "none" });
      return;
    }
    this.setData({ busy: true });
    try {
      const res = await request<ReplyResp>(
        `/api/practice/sessions/${this.data.sessionId}/reply`,
        "POST",
        { userText: this.data.userText.trim() }
      );
      this.setData({
        examinerLine: res.examinerLine,
        lastBrief: res.briefEval,
        userText: "",
      });
      if (res.shouldEnd) {
        wx.showModal({
          title: "提示",
          content: "模型建议可结束本轮，是否生成完整评价？",
          success: async (r) => {
            if (r.confirm) await this.finish();
          },
        });
      }
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "发送失败", icon: "none" });
    } finally {
      this.setData({ busy: false });
    }
  },
  async finish() {
    if (!this.data.sessionId) return;
    this.setData({ busy: true });
    try {
      const rep = await request<Report>(`/api/practice/sessions/${this.data.sessionId}/complete`, "POST");
      this.setData({ report: rep });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "生成失败", icon: "none" });
    } finally {
      this.setData({ busy: false });
    }
  },
  reset() {
    this.innerAudio?.stop();
    this.setData({
      sessionId: 0,
      examinerLine: "",
      topic: "",
      userText: "",
      started: false,
      lastBrief: "",
      report: null,
      recording: false,
      speechHint: "",
    });
  },
});
