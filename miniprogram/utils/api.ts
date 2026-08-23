import { API_BASE } from "./config";

export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

function getApiBase(): string {
  return API_BASE;
}

export function uploadAsr(filePath: string): Promise<{ text: string }> {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${getApiBase()}/api/practice/asr`,
      filePath,
      name: "file",
      success(res) {
        const status = res.statusCode || 0;
        if (status >= 400) {
          try {
            const body = JSON.parse(res.data as string) as { error?: string };
            reject(new Error(body?.error || `HTTP ${status}`));
          } catch {
            reject(new Error(`HTTP ${status}`));
          }
          return;
        }
        try {
          const data = JSON.parse(res.data as string) as { text: string };
          resolve(data);
        } catch {
          reject(new Error("解析 ASR 响应失败"));
        }
      },
      fail: reject,
    });
  });
}

/** 服务端 TTS，返回临时文件路径供 InnerAudioContext 播放 */
export function downloadTtsToTemp(text: string): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${getApiBase()}/api/practice/tts`,
      method: "POST",
      data: { text },
      header: {
        "Content-Type": "application/json",
      },
      responseType: "arraybuffer",
      success(res) {
        const status = res.statusCode || 0;
        if (status >= 400) {
          reject(new Error(`TTS HTTP ${status}`));
          return;
        }
        const fs = wx.getFileSystemManager();
        const path = `${wx.env.USER_DATA_PATH}/examiner-tts.mp3`;
        try {
          fs.writeFileSync(path, res.data as ArrayBuffer);
          resolve(path);
        } catch (e) {
          reject(e instanceof Error ? e : new Error("写入临时音频失败"));
        }
      },
      fail: reject,
    });
  });
}

export function request<T>(
  path: string,
  method: HttpMethod = "GET",
  data?: Record<string, unknown>
): Promise<T> {
  const header: Record<string, string> = {
    "Content-Type": "application/json",
  };
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${getApiBase()}${path}`,
      method,
      data,
      header,
      timeout: 60000,
      success(res) {
        const status = res.statusCode || 0;
        if (status >= 400) {
          const body = res.data as { error?: string };
          reject(new Error(body?.error || `HTTP ${status}`));
          return;
        }
        resolve(res.data as T);
      },
      fail(err) {
        reject(err);
      },
    });
  });
}
