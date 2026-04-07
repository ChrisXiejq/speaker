import { request } from "../../utils/api";

type QItem = {
  id: number;
  seasonLabel: string;
  part: string;
  topic: string;
  questionText: string;
};

const PARTS = ["PART1", "PART2", "PART3"];

Page({
  data: {
    seasons: [] as string[],
    seasonIndex: 0,
    partIndex: 0,
    parts: PARTS,
    items: [] as QItem[],
    loading: false,
  },
  onShow() {
    this.loadSeasons();
  },
  async loadSeasons() {
    this.setData({ loading: true });
    try {
      const seasons = await request<string[]>("/api/bank/seasons", "GET");
      this.setData({ seasons: seasons.length ? seasons : ["2025Q1"] });
      await this.loadQuestions();
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "加载失败", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
  onSeasonChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ seasonIndex: Number(e.detail.value) });
    this.loadQuestions();
  },
  onPartChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ partIndex: Number(e.detail.value) });
    this.loadQuestions();
  },
  async loadQuestions() {
    const season = this.data.seasons[this.data.seasonIndex] || "2025Q1";
    const part = PARTS[this.data.partIndex];
    this.setData({ loading: true });
    try {
      const items = await request<QItem[]>(
        `/api/bank/questions?season=${encodeURIComponent(season)}&part=${encodeURIComponent(part)}`,
        "GET"
      );
      this.setData({ items: items || [] });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "加载失败", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
});
