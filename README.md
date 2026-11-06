# 发令台 GUNSTART（marathon_system）

马拉松赛事管理系统 · **Spring Cloud Alibaba** 领域微服务示例。

> **定位**：学习 / 毕设演示用开源示例，**不是生产级赛事平台**。默认配置含明文演示密码与固定 JWT secret，请勿直接用于公网或真实报名业务。

## License

[MIT](./LICENSE)

开源前请自行向学校 / 指导老师确认毕设代码是否允许公开发布。

## 技术栈

- Java 8 · Spring Boot **2.7.14** · Spring Cloud **2021.0.8** · SCA **2021.0.5.0**
- Nacos · Gateway · OpenFeign · MyBatis · MySQL · Redis · JWT
- 旁路 AI：Java 21 · Boot 3.4 · Spring AI（独立 `ai-service`）

## 快速入口（本地跑通后）

| 角色 | 地址 |
|------|------|
| 跑者公开平台 | http://localhost:8081/ |
| 组委会管理台 | http://localhost:8081/admin.html |
| 演示账号 | `admin` / `123456` |

## 仓库里有什么 / 没有什么

| 会提交 | 不会提交（已 `.gitignore`） |
|--------|---------------------------|
| 源码、SQL、文档、`docker-compose.yml` | `application-local.yml`（数据库 / Redis / AI 密钥） |
| `*.example` 本地配置模板 | `.env`、私钥、`uploads` 用户上传 |

**切勿**对本地密钥文件使用 `git add -f`。若密钥曾泄露，请到对应平台作废并换新。

演示库中的选手身份证 / 手机号为**虚构假数据**，仅供联调。

## 已知不足（刻意保留，便于学习对照）

| 点 | 现状 | 生产方向 |
|----|------|----------|
| 管理员密码 | 明文比对 | BCrypt / Argon2 |
| JWT secret | 写在 `application.yml` 示例值 | 环境变量 / 配置中心 |
| 数据边界 | 一库多服务 | 按域拆库或读模型 |
| 名额一致性 | Feign + 补偿 | 锁 / 流水表等 |
| 可观测性 | 日志为主 | 链路追踪、审计 |

## 模块

```text
marathon-parent
├── marathon-common / marathon-api
├── marathon-gateway (8081)
├── marathon-auth (8101)
├── marathon-event (8102)
├── marathon-registration (8103)
├── marathon-result (8104)
└── ai-service (8082，独立工程)
```

## 本地启动（摘要）

1. 中间件：`docker compose up -d`（MySQL `root/123456` · Redis 密码 `redis@2026` · Nacos `8848`）  
   或自行安装 MySQL 8 / Redis / Nacos 2.x
2. 各业务模块：复制 `application-local.yml.example` → `application-local.yml`，填写密码
3. 根目录：`mvn -DskipTests install`
4. 按 [微服务启动说明](docs/微服务启动说明.md) 启动 gateway 与各服务

更多：

- [微服务启动说明](docs/微服务启动说明.md)
- [答辩环境配置说明](docs/答辩环境配置说明.md)
- [技术博客](docs/技术博客-从零搭建马拉松赛事管理系统.md)
- [AI 服务](ai-service/README.md)
