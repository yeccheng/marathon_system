# 前端源文件（已归档）

本目录下的 `index.html` 是早期**单文件管理台**草稿（「马拉松赛事管理台 · Marathon Console」），**不参与 Spring Boot 静态资源托管，运行时不会加载。**

## 当前在用（请改这些）

| 角色 | 路径 |
|------|------|
| 公开报名平台 | `src/main/resources/static/index.html` + `assets/js/public-app.js` + `assets/css/site.css` |
| 管理台 | `src/main/resources/static/admin.html` + `assets/js/admin-app.js` |

启动后访问：

- 公开页：`http://localhost:8081/`
- 管理台：`http://localhost:8081/admin.html`

请勿在本目录继续改功能；仅作毕设材料 / 对照备份。若需对照 UI，以 `static/admin.html` 为准。
