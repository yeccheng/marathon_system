-- 赛事表增加封面图字段
-- 执行前请确认库名：marathon_system
ALTER TABLE marathon_event
    ADD COLUMN cover_image VARCHAR(255) NULL COMMENT '赛事封面图访问路径，如 /uploads/xxx.jpg' AFTER status;
