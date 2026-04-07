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
  async onDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const modal = await wx.showModal({
      title: "确认删除",
      content: "删除后列表中不再显示该记录",
    });
    if (!modal.confirm) return;
    try {
      await request(`/api/practice/sessions/${id}`, "DELETE");
      wx.showToast({ title: "已删除", icon: "success" });
      await this.load();
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "删除失败", icon: "none" });
    }
  },
});
