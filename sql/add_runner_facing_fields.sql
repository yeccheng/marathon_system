-- Runner-facing fields for marathon_event
-- Database: marathon_system

ALTER TABLE marathon_event
    ADD COLUMN signup_start  DATETIME     NULL COMMENT 'signup start',
    ADD COLUMN signup_end    DATETIME     NULL COMMENT 'signup end',
    ADD COLUMN gun_time      DATETIME     NULL COMMENT 'gun time',
    ADD COLUMN cut_off_time  VARCHAR(50)  NULL COMMENT 'cut off time text',
    ADD COLUMN signup_rule   VARCHAR(200) NULL COMMENT 'signup rule',
    ADD COLUMN pickup_info   VARCHAR(300) NULL COMMENT 'pickup info',
    ADD COLUMN route_desc    VARCHAR(500) NULL COMMENT 'route description';

-- Seed: 2026 Xian International Marathon (id=1)
UPDATE marathon_event SET
    signup_start = '2026-09-01 10:00:00',
    signup_end   = '2026-10-15 18:00:00',
    gun_time     = '2026-11-15 07:30:00',
    cut_off_time = '6小时',
    signup_rule  = '先到先得',
    pickup_info  = '西安奥体中心南广场，赛前两天 09:00-18:00',
    route_desc   = '起终点设于奥体中心，途经大雁塔、钟楼等城市地标，全程起伏适中。'
WHERE id = 1;

-- Seed: half marathon (id=2)
UPDATE marathon_event SET
    signup_start = '2026-07-01 09:00:00',
    signup_end   = '2026-08-20 18:00:00',
    gun_time     = '2026-10-25 07:00:00',
    cut_off_time = '3小时',
    signup_rule  = '先到先得',
    pickup_info  = '城墙起点附近酒店大堂，赛前一天 10:00-20:00',
    route_desc   = '半程沿古城墙外侧平路为主，适合首次参赛跑者。'
WHERE id = 2;

-- Seed: mini run (id=3)
UPDATE marathon_event SET
    signup_start = '2026-05-01 09:00:00',
    signup_end   = '2026-06-10 18:00:00',
    gun_time     = '2026-09-20 08:00:00',
    cut_off_time = '1.5小时',
    signup_rule  = '抽签录取',
    pickup_info  = '曲江池现场临时领物点，比赛当天 06:30-07:30',
    route_desc   = '迷你跑环曲江池绿道，亲子友好。'
WHERE id = 3;

-- Fallback for other rows still null
UPDATE marathon_event SET
    signup_start = TIMESTAMP(DATE_SUB(event_date, INTERVAL 60 DAY), '10:00:00'),
    signup_end   = TIMESTAMP(DATE_SUB(event_date, INTERVAL 20 DAY), '18:00:00'),
    gun_time     = TIMESTAMP(event_date, '07:30:00'),
    cut_off_time = IFNULL(cut_off_time, '6小时'),
    signup_rule  = IFNULL(signup_rule, '先到先得'),
    pickup_info  = IFNULL(pickup_info, '赛事官方指定地点，赛前两天领取'),
    route_desc   = IFNULL(route_desc, '详见赛事官方赛道图，沿城市主干道往返。')
WHERE signup_start IS NULL;
