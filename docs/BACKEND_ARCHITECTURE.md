# 后端架构与接口说明

> 基于 `backend/` 模块当前实现整理：技术栈、分层、安全、缓存与 **HTTP API** 一览。

## 1. 技术栈

| 类别 | 选型 |
|------|------|
| 运行时 | Java 17，Spring Boot 3.2.x |
| Web | Spring MVC，Spring Validation |
| 安全 | Spring Security（无 Session；普通业务 API 免登录，管理端使用独立 API Key） |
| 持久化 | MySQL，MyBatis-Plus（Mapper 注解 SQL，无 XML） |
| 缓存 | Spring Cache；实现为 **Caffeine 同类：ConcurrentMap** 或 **Redis**（`app.cache.use-redis`） |
| HTTP 客户端 | 通义 DashScope / OpenAI 兼容 ASR·TTS·Chat（见配置 `app.dashscope`） |

---

## 2. 分层结构

```mermaid
flowchart TB
    subgraph edge["接入层"]
        F[Filter: RateLimit / RequestLogging / AdminApiKey]
        C[Controller: REST]
    end
    subgraph app["应用层"]
        S[Service: impl + intf]
    end
    subgraph data["数据与集成"]
        M[Mapper: MyBatis-Plus BaseMapper + @Select/@Update]
        DB[(MySQL)]
        R[(Redis 可选)]
        AI[DashScope 客户端]
    end
    subgraph cross["横切"]
        G[GlobalExceptionHandler]
        AOP[ControllerLoggingAspect]
        Cache[@Cacheable / @CacheEvict]
    end

    F --> C
    C --> S
    S --> M
    M --> DB
    S -.-> Cache
    Cache -.-> R
    S --> AI
    C --> G
    C --> AOP
```

| 包路径（主要） | 职责 |
|----------------|------|
| `com.speaker.app.controller` | REST 入口、DTO 出入参 |
| `com.speaker.app.service.impl` / `intf` | 业务编排、事务边界、调用 AI |
| `com.speaker.app.repository` | Mapper 接口，SQL 以注解形式写在接口内 |
| `com.speaker.app.config` | Security、Cache、Redis、CORS、AOP 日志等 |
| `com.speaker.app.security` | 管理员 Key 过滤器、限流过滤器 |
| `com.speaker.app.bootstrap` | 启动时数据初始化（若启用） |

---

## 3. 安全模型（摘要）

| 路径模式 | 说明 |
|----------|------|
| `/api/health` | **匿名** |
| `/api/admin/**` | **匿名** 但须通过 **`X-Admin-Key`**（与 `app.admin.api-key` 一致；未配置 Key 时返回 503） |
| 其他 `/api/**` | **单用户免登录** |

普通业务请求统一使用数据库中最早创建的账号作为数据归属；空库会自动创建 `default-user`。因此公网部署时须在网关、防火墙或反向代理层限制普通业务接口的访问范围。

---

## 4. 缓存说明

| 缓存名 | 内容 | 失效时机 |
|--------|------|----------|
| `bankSeasons` | 季节列表 API 结果 | 管理端题库导入/编辑/软删除等（`@CacheEvict allEntries`） |
| `bankTopicGroups` | 按「季节 + Part 键」聚合的题目分组 | 同上 |

`app.cache.use-redis=false` 时使用 JVM **ConcurrentMap**；`true` 且 Redis 可达时使用 **Redis**（值 JSON 序列化，TTL 见 `app.cache.bank-ttl-ms`）。

---

## 5. HTTP API 一览

**Base URL**：开发环境常为 `http://localhost:8080`；前端经 Vite 代理时浏览器只访问 `/api`。

### 5.1 健康

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/health` | 无 | 探活 |

### 5.2 题库（公开）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/bank/seasons` | 无 | 未删除题目中的季节标签列表（含默认季排序） |
| GET | `/api/bank/questions` | 无 | Query：`season`（可选）、`part`（PART1 / PART2_AND_3 等） |
| GET | `/api/bank/search` | 无 | Query：`q`，topic 模糊搜 |

### 5.3 练习与语音（免登录）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/practice/sessions` | 无 | 开始会话，body：Part、话题来源、季节、自定义话题等 |
| POST | `/api/practice/sessions/{id}/reply` | 无 | 提交用户回答 |
| POST | `/api/practice/sessions/{id}/next-topic` | 无 | 题库模式下进入下一话题 |
| POST | `/api/practice/sessions/{id}/complete` | 无 | 结束并生成报告 |
| GET | `/api/practice/sessions/{id}` | 无 | 会话详情 |
| GET | `/api/practice/sessions` | 无 | 分页列表，Query：`page`、`size` |
| DELETE | `/api/practice/sessions/{id}` | 无 | 软删除本会话 |
| POST | `/api/practice/asr` | 无 | `multipart/form-data` 上传音频 → 转写文本 |
| POST | `/api/practice/tts` | 无 | JSON 文本 → TTS 音频 |

### 5.4 管理端题库

**请求头**：`X-Admin-Key: <与 app.admin.api-key 一致>`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/bank/items` | Query：`seasonLabel`、`segment`（part1 / part23） |
| PUT | `/api/admin/bank/items/{id}` | 更新题目字段 |
| DELETE | `/api/admin/bank/items/{id}` | 单题软删除 |
| DELETE | `/api/admin/bank/items/by-season` | Query：`seasonLabel`，整季软删除 |
| POST | `/api/admin/bank/preview-markdown` | 预览 Markdown 解析 |
| POST | `/api/admin/bank/import-markdown` | 导入 Markdown 题库 |

---

## 6. 统一错误响应

业务失败时响应体多为 JSON：`{ "error": "可读错误信息" }`。
HTTP 状态码与 `GlobalExceptionHandler` 绑定（如 400 参数错误、409 数据冲突、503 Redis 不可用、500 未捕获异常等）。

---

## 7. 配置要点（环境）

| 配置前缀 | 用途 |
|----------|------|
| `spring.datasource.*` | MySQL 连接（通常放在 `application-local.yml`） |
| `spring.data.redis.*` | Redis（启用 Redis 缓存时） |
| `app.admin.api-key` | 管理端 Key |
| `app.dashscope.*` | 通义 API Key、模型名、TTS 参数等 |
| `app.cache.*` | `use-redis`、`bank-ttl-ms` |
| `app.cors.allowed-origins` | 允许的前端源 |
| `app.security.rate-limit.*` | 各桶每分钟请求上限 |

---

## 8. 文档版本

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-04-08 | 首版，与当前仓库实现同步 |
