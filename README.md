# Speaking Test Killer（雅思口语模拟 · 微信小程序 + Spring Boot）

本仓库包含：

- `backend/`：Spring Boot 3 + **MyBatis** + **JWT** + 通义千问（DashScope）+ 限流；**仅连接 MySQL**（通过 `application-local.yml` 配置云库或自建库，密码与密钥写在文件内，不使用环境变量）
- `web/`：**Vue 3 + JavaScript + Vite** 浏览器端 SPA，对接同一套 API（开发时代理到 `localhost:8080`）
- `miniprogram/`：微信小程序前端（TypeScript），对接上述 API

需求来源见根目录 `project idea.txt`。

## 后端运行

1. 安装 **JDK 17**、**Maven**。
2. **必须**：将 `backend/src/main/resources/application-local.yml.example` 复制为同目录下的 `application-local.yml`，填写 **MySQL 连接串、用户名、密码**、`app.jwt.secret`、`app.dashscope.api-key`、微信小程序 `appid`/`secret` 等；无此文件或未配置有效 MySQL 则**无法启动**。该文件已加入 `.gitignore`，不会被提交。默认已激活 `local` profile。
3. 启动：

```bash
cd backend
mvn spring-boot:run
```

4. 健康检查：`GET http://localhost:8080/api/health`
5. **浏览器端联调**：后端默认 `app.cors.allowed-origins` 为 `http://localhost:5173`；若你改了前端端口，请在 `application-local.yml` 中覆盖 `app.cors.allowed-origins`。

## Web 端（Vue）运行

1. 先启动后端（见上）。
2. 在仓库根目录执行：

```bash
cd web
npm install
npm run dev
```

3. 浏览器打开终端里提示的地址（一般为 `http://localhost:5173`）。Vite 会把 `/api` 代理到 `http://localhost:8080`，无需改前端代码即可联调。
4. **生产构建**：复制 `web/.env.production.example` 为 `web/.env.production`，设置 `VITE_API_BASE` 为线上 API 根地址，然后执行 `npm run build`，将 `web/dist` 部署到任意静态站点（Nginx、OSS、Vercel 等），并在后端 `application-local.yml` 的 `app.cors.allowed-origins` 中允许该站点来源。

## 小程序运行

1. 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)。
2. 在 `miniprogram/utils/config.ts` 中将 `API_BASE` 改为你的 **HTTPS** 后端地址（小程序仅允许配置在「request 合法域名」中的域名）。
3. 在 `miniprogram/project.config.json` 中填写你的 `appid`。
4. 在小程序后台「开发 → 开发管理 → 服务器域名」中配置 `request` 域名。
5. 在 `miniprogram/` 执行 `npm install`（仅用于类型定义 `miniprogram-api-typings`）。
6. 用微信开发者工具打开 `miniprogram` 目录。

### 关于「Vue + 小程序」

本仓库小程序端使用 **原生小程序 + TypeScript**，便于零依赖构建与上架。若你更偏好 **Vue 单文件组件**，可使用 **uni-app** 新建工程，将页面逻辑迁移为 `.vue`，并复用 `utils/api.ts` 中的请求封装思路（`uni.request`）。

## 安全与上线建议

- **JWT**：生产环境必须更换 `JWT_SECRET`，并配合 HTTPS；可考虑缩短有效期 + 刷新令牌。
- **限流**：`RateLimitFilter` 按 IP 做粗粒度保护；生产可前置 WAF / API 网关限流。
- **CORS**：小程序主要走 `wx.request`，浏览器 CORS 为次要；仍建议将 `CORS_ORIGINS` 设为固定白名单。
- **密钥**：勿将 `DASHSCOPE_API_KEY` 提交到仓库；使用环境变量或密钥管理。
- **日志**：避免打印用户语音全文与 Token；当前实现已避免在日志中输出敏感字段。
- **ASR**：可在服务端接入阿里云语音识别，将小程序录音文件上传后转写为文本再调用 `/reply`；当前前端支持手打/粘贴，便于先跑通闭环。

## API 摘要

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| POST | `/api/practice/sessions` | 开始会话（Part1/2/3，题库或自定义话题） |
| POST | `/api/practice/sessions/{id}/reply` | 提交用户文本回答 |
| POST | `/api/practice/sessions/{id}/complete` | 生成总结评分 |
| GET | `/api/practice/sessions` | 历史分页 |
| GET | `/api/practice/sessions/{id}` | 会话详情 + 报告 |
| GET | `/api/bank/seasons` | 题库季节列表（可匿名） |
| GET | `/api/bank/questions` | 按季节与 Part 取题（可匿名） |

## 许可

示例项目代码可按需修改与商用，请注意第三方服务（微信、阿里云）各自条款。
