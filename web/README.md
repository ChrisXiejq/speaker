# Web 前端（Vue 3 + JavaScript）

## 开发

```bash
npm install
npm run dev
```

确保本机已启动 Spring Boot（默认 `http://localhost:8080`）。`/api` 请求由 Vite 开发服务器代理到后端。

## 生产

1. 复制 `.env.production.example` 为 `.env.production`
2. 设置 `VITE_API_BASE=https://你的API域名`（不要末尾斜杠）
3. `npm run build`，产物在 `dist/`

## 技术栈

- Vue 3（`<script setup>`）
- Vue Router 4
- Pinia
- Axios
