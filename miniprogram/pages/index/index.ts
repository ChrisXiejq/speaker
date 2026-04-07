import { getToken, clearToken } from "../../utils/storage";

Page({
  data: {
    loggedIn: false,
  },
  onShow() {
    this.setData({ loggedIn: !!getToken() });
  },
  goLogin() {
    wx.navigateTo({ url: "/pages/login/login" });
  },
  goPractice() {
    if (!getToken()) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/pages/practice/practice" });
  },
  goBank() {
    wx.navigateTo({ url: "/pages/bank/bank" });
  },
  goHistory() {
    if (!getToken()) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/pages/history/history" });
  },
  logout() {
    clearToken();
    this.setData({ loggedIn: false });
    wx.showToast({ title: "已退出", icon: "none" });
  },
});
