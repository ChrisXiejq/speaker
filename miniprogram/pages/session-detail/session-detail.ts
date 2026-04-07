import { request } from "../../utils/api";

type Turn = {
  id: number;
  seq: number;
  role: string;
  content: string;
  briefEval?: string;
};

type Report = {
  pronunciationScore: number;
  grammarScore: number;
  coherenceScore: number;
  fluencyScore: number;
  ideasScore: number;
  overallBand: string;
  detailedFeedback: string;
  suggestionsJson: string;
};

type Detail = {
  session: {
    id: number;
    part: string;
    topic: string;
    status: string;
    startedAt: string;
    endedAt?: string;
  };
  turns: Turn[];
  report: Report | null;
};

Page({
  data: {
    detail: null as Detail | null,
  },
  onLoad(q: Record<string, string | undefined>) {
    const id = q.id;
    if (!id) return;
    this.load(Number(id));
  },
  async load(id: number) {
    wx.showLoading({ title: "加载中" });
    try {
      const detail = await request<Detail>(`/api/practice/sessions/${id}`, "GET");
      this.setData({ detail });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "加载失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },
});
