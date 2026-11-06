# 上传目录（赛事封面等）

运行时由 `marathon.upload.path` 指向本目录（见 `application-local.yml`）。

## 演示用示例图（可提交说明，文件本身可按需保留）

| 文件 | 用途 |
|------|------|
| `demo-cover-wall.jpg` | 城墙晨跑氛围封面示例 |
| `demo-cover-finish.jpg` | 夜赛终点氛围封面示例 |

挂到赛事数据：执行 `sql/seed_demo_covers.sql`，或管理台上传后自动生成时间戳文件名。

浏览器访问：`http://localhost:8081/uploads/demo-cover-wall.jpg`
