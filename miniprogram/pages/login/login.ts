import { request } from "../../utils/api";
import { setToken } from "../../utils/storage";

type TokenResponse = {
  token: string;
  tokenType: string;
  expiresInMs: number;
};

Page({
  data: {
    username: "",
    password: "",
    mode: "login" as "login" | "register",
  },
  onUsername(e: WechatMiniprogram.Input) {
    this.setData({ username: e.detail.value });
  },
  onPassword(e: WechatMiniprogram.Input) {
    this.setData({ password: e.detail.value });
  },
  toggleMode() {
    this.setData({ mode: this.data.mode === "login" ? "register" : "login" });
  },
  async submit() {
    const { username, password, mode } = this.data;
    if (!username || !password) {
      wx.showToast({ title: "请填写完整", icon: "none" });
      return;
    }
    wx.showLoading({ title: "提交中" });
    try {
      const path = mode === "login" ? "/api/auth/login" : "/api/auth/register";
      const res = await request<TokenResponse>(path, "POST", { username, password });
      setToken(res.token);
      wx.showToast({ title: "成功", icon: "success" });
      setTimeout(() => wx.navigateBack(), 400);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "失败";
      wx.showToast({ title: msg, icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },
});
