const TOKEN_KEY = "sk_token";

export function getToken(): string | undefined {
  try {
    return wx.getStorageSync(TOKEN_KEY) as string | undefined;
  } catch {
    return undefined;
  }
}

export function setToken(token: string) {
  wx.setStorageSync(TOKEN_KEY, token);
}

export function clearToken() {
  wx.removeStorageSync(TOKEN_KEY);
}
