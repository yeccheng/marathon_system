ALTER TABLE registration ADD COLUMN shirt_size varchar(8) NULL;
ALTER TABLE registration ADD COLUMN order_no varchar(32) NULL;
ALTER TABLE registration ADD COLUMN pay_time datetime NULL;
ALTER TABLE registration ADD COLUMN pay_voucher varchar(64) NULL;
ALTER TABLE marathon_event ADD COLUMN aid_stations text NULL;
ALTER TABLE marathon_event ADD COLUMN route_profile text NULL;
ALTER TABLE marathon_event ADD COLUMN cancel_rule varchar(500) NULL;
ALTER TABLE result ADD COLUMN split_5k int NULL;
ALTER TABLE result ADD COLUMN split_10k int NULL;
ALTER TABLE result ADD COLUMN split_21k int NULL;
CREATE TABLE IF NOT EXISTS runner_notice (
  id bigint NOT NULL AUTO_INCREMENT,
  runner_id bigint NOT NULL,
  title varchar(100) NOT NULL,
  content varchar(500) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  read_flag tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_runner (runner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
UPDATE result SET split_5k=1080, split_10k=2200, split_21k=4650 WHERE id=4;
UPDATE result SET split_5k=1180, split_10k=2400, split_21k=5050 WHERE id=5;
UPDATE result SET split_5k=1280, split_10k=2600, split_21k=5450 WHERE id=9;
UPDATE result SET split_5k=1350, split_10k=2750, split_21k=5800 WHERE id=6;
UPDATE result SET split_5k=1450, split_10k=2950, split_21k=6200 WHERE id=7;
UPDATE result SET split_5k=1620, split_10k=3300, split_21k=7000 WHERE id=10;
UPDATE result SET split_5k=1800, split_10k=3650, split_21k=7800 WHERE id=8;
