-- 演示：为示例赛事挂上 uploads 目录中的封面图（先保证文件已放在 uploads/）
-- 执行前请已跑过 sql/add_cover_image.sql
SET NAMES utf8mb4;

UPDATE marathon_event SET cover_image = '/uploads/demo-cover-wall.jpg' WHERE id = 1;
UPDATE marathon_event SET cover_image = '/uploads/demo-cover-finish.jpg' WHERE id = 2;
-- id=3 可现场在管理台上传，或同样指定一张图
