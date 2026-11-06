-- =============================================================
-- 马拉松赛事管理系统 - 建库建表脚本
-- 依据《马拉松.pdf》设计文档生成
-- MySQL 8.0  /  InnoDB / utf8mb4
-- =============================================================

CREATE DATABASE IF NOT EXISTS marathon_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE marathon_system;

-- -------------------------------------------------------------
-- 1. 赛事表 marathon_event
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS marathon_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '赛事ID',
    event_name       VARCHAR(100) NOT NULL COMMENT '赛事名称',
    event_type       TINYINT      NOT NULL COMMENT '赛事类型:1全程马拉松,2半程马拉松,3迷你跑',
    event_date       DATE         NOT NULL COMMENT '比赛日期',
    start_location   VARCHAR(100) NOT NULL COMMENT '起点地点',
    distance_km      DECIMAL(5,1) NOT NULL COMMENT '距离(公里)',
    quota            INT          NOT NULL COMMENT '总名额',
    registered_count INT          DEFAULT 0 COMMENT '已报名人数',
    entry_fee        DECIMAL(8,2) NOT NULL COMMENT '报名费',
    status           TINYINT      NOT NULL COMMENT '状态:0报名中,1报名截止,2赛事已结束',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事表';

-- -------------------------------------------------------------
-- 2. 选手档案表 runner
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS runner (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '选手ID',
    name              VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender            TINYINT      NOT NULL COMMENT '性别:1男,2女',
    id_card           VARCHAR(18)  NOT NULL COMMENT '身份证号',
    phone             VARCHAR(20)  NOT NULL COMMENT '手机号',
    birth_date        DATE         NOT NULL COMMENT '出生日期',
    emergency_contact VARCHAR(100) NOT NULL COMMENT '紧急联系人及电话',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_idcard (id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选手档案表';

-- -------------------------------------------------------------
-- 3. 报名表 registration
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registration (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报名记录ID',
    event_id      BIGINT      NOT NULL COMMENT '赛事ID',
    runner_id     BIGINT      NOT NULL COMMENT '选手ID',
    bib_number    VARCHAR(10) DEFAULT NULL COMMENT '号码布编号',
    register_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    pay_status    TINYINT     NOT NULL DEFAULT 0 COMMENT '支付状态:0未支付,1已支付',
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '报名状态:1已报名,2已取消,3退赛',
    -- 联合唯一索引：同一个选手不能重复报同一场赛事
    UNIQUE KEY uk_event_runner (event_id, runner_id),
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id)  REFERENCES marathon_event (id),
    CONSTRAINT fk_reg_runner FOREIGN KEY (runner_id) REFERENCES runner (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';

-- -------------------------------------------------------------
-- 4. 比赛成绩表 result
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS result (
    id              BIGINT   AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    registration_id BIGINT   NOT NULL COMMENT '报名记录ID',
    chip_time       INT      NOT NULL COMMENT '枪声成绩(秒)',
    net_time        INT      NOT NULL COMMENT '净成绩(秒)',
    finish_status   TINYINT  NOT NULL COMMENT '完赛状态:1完赛,0未完赛',
    finish_time     DATETIME DEFAULT NULL COMMENT '完赛时间',
    -- 一条报名记录只能录入一次成绩
    UNIQUE KEY uk_reg_result (registration_id),
    CONSTRAINT fk_result_reg FOREIGN KEY (registration_id) REFERENCES registration (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛成绩表';

-- -------------------------------------------------------------
-- 5. 管理员表 admin
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员id',
    username    VARCHAR(50) NOT NULL COMMENT '账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码',
    admin_name  VARCHAR(50) NOT NULL COMMENT '管理员名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- =============================================================
-- 校验：打印建表结果
-- =============================================================
SHOW TABLES;
