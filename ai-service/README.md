# marathon-ai-service

旁路 AI 服务（**不改动**业务服务 Java 8 / Spring Boot 2.7）。

| 项 | 值 |
| --- | --- |
| 栈 | Java 21 · Spring Boot 3.4.5 · Spring AI 1.0.9 · Nacos Discovery |
| 端口 | `8082` |
| 服务名 | `marathon-ai-service`（注册 Nacos，Gateway `lb://marathon-ai-service`） |
| 赛事数据 | 经 Gateway `http://localhost:8081/api/public/**` |
| 首个能力 | `@Tool` 查赛事 + `entity(EventAssistantReply)` 结构化输出 |

## 启动前

1. Nacos `localhost:8848` 已起
2. Gateway + `marathon-event` 已起（Tool 查赛事）
3. 配置 API Key：复制 `application-local.yml.example` → `application-local.yml`，或环境变量 `DEEPSEEK_API_KEY`
4. 本机 Java ≥ 17（推荐 JDK 21）

## 启动

```bash
cd ai-service
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
mvn spring-boot:run
```

前端默认同源走 Gateway：`POST /api/ai/chat`（`api.js` 中 `aiBase()` 为空）。

## 架构要点

- **旁路隔离**：AI 用 Boot 3 + Spring AI；赛事核心仍在 Boot 2.7。
- **Nacos**：与业务服务同注册中心，Gateway 负载均衡转发 `/api/ai/**`。
- **Tool Calling**：调 Gateway 公开 API，事实不靠模型瞎编。
