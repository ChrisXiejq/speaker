import { request } from "../../utils/api";

type Session = {
  id: number;
  part: string;
  topic: string;
  status: string;
  startedAt: string;
};

type PageResp = {
  content: Session[];
  totalElements: number;
};

Page({
  data: {
    list: [] as Session[],
    loading: false,
  },
  onShow() {
    this.load();
  },
  async load() {
    this.setData({ loading: true });
    try {
      const res = await request<PageResp>("/api/practice/sessions?page=0&size=50", "GET");
      this.setData({ list: res.content || [] });
    } catch (e) {
      wx.showToast({ title: e instanceof Error ? e.message : "加载失败", icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },
  openDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    wx.navigateTo({ url: `/pages/session-detail/session-detail?id=${id}` });
  },
});
