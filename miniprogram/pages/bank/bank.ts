import { request } from "../../utils/api";

type QItem = {
  id: number;
  seasonLabel: string;
  part: string;
  topic: string;
  questionText: string;
  answerText?: string;
  keywordsJson?: string;
};

type TopicGroup = {
  topic: string;
  questions?: QItem[];
  part2Questions?: QItem[];
  part3Questions?: QItem[];
};

const PART_API = ["PART1", "PART2_AND_3"];
const PART_LABELS = ["Part 1", "Part 2 & 3"];

Page({
  data: {
    seasons: [] as string[],
    seasonIndex: 0,
    partIndex: 0,
    partLabels: PART_LABELS,
    topicGroups: [] as TopicGroup[],
    loading: false,
  },
  onShow() {
    this.loadSeasons();
  },
  async loadSeasons() {
    this.setData({ loading: true });
    try {
      const seasons = await request<string[]>("/api/bank/seasons", "GET");
      this.setData({ seasons: seasons.length ? seasons : [], seasonIndex: 0 });
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
    const season = this.data.seasons[this.data.seasonIndex];
    if (!season) {
      this.setData({ loading: false, topicGroups: [] });
      return;
    }
    const part = PART_API[this.data.partIndex];
    this.setData({ loading: true });
    try {
      const topicGroups = await request<TopicGroup[]>(
        `/api/bank/questions?season=${encodeURIComponent(season)}&part=${encodeURIComponent(part)}`,
        "GET"
      );
      this.setData({ topicGroups: topicGroups || [] });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "加载失败", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
});
