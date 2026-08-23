# 可观测性：Prometheus + Grafana

## 架构

1. **Spring Boot** 暴露 `GET /actuator/prometheus`（Micrometer Prometheus），包含 HTTP QPS/延迟、JVM、CPU、Hikari 等指标。
2. **Prometheus** 周期性抓取上述端点（默认 15s）。
3. **Grafana** 使用预置数据源连接 Prometheus，并加载预置看板 **「Speaker API 监控大盘」**。

## 启动顺序

1. 在本机启动后端（默认 `http://127.0.0.1:8080`），确认浏览器或 `curl` 可访问：

   ```bash
   curl -s http://localhost:8080/actuator/prometheus | head
   ```

2. 启动监控栈：

   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```

3. 打开 **Grafana**：<http://localhost:3000>
   - 默认账号：`admin` / `admin`（首次登录可改密码）
   - 左侧 **Dashboards** → **Speaker API 监控大盘**

4. **Prometheus UI**：<http://localhost:9090> → Status → Targets，确认 `speaking-api` 为 **UP**。

## 若 Targets 为 DOWN

- 后端未启动或未监听 `8080`。
- Docker 无法访问宿主机：在 `infra/docker-compose.yml` 中已配置 `extra_hosts: host.docker.internal:host-gateway`；若仍失败，可将 `infra/prometheus/prometheus.yml` 里 `targets` 改为宿主机局域网 IP（如 `192.168.x.x:8080`）。

## 看板无数据或部分面板无数据

- 确认指标带标签 `application="speaking-killer-api"`（与 `spring.application.name` 一致）。若你改过应用名，请在 Grafana 面板里把 PromQL 中的该标签改成你的名称，或暂时去掉 `{application="..."}` 做排查。
- Hikari 面板在未使用数据库连接时可能为空，属正常。

## 安全说明

`/actuator/prometheus` 当前为 **匿名可访问**（便于内网抓取）。生产环境请：

- 仅内网/VPC 可达，或
- 独立 `management.server.port` + 防火墙，或
- 对 Actuator 使用 Spring Security 鉴权 / mTLS。

## 可选：导入社区 JVM 看板

在 Grafana：**Dashboards → Import**，输入 ID **4701**（JVM Micrometer），数据源选 **Prometheus**。
