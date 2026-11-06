ALTER TABLE registration ADD COLUMN pay_channel varchar(20) NULL;
ALTER TABLE registration ADD COLUMN refund_status tinyint NOT NULL DEFAULT 0;
ALTER TABLE registration ADD COLUMN refund_no varchar(64) NULL;
ALTER TABLE registration ADD COLUMN refund_amount decimal(8,2) NULL;
ALTER TABLE registration ADD COLUMN refund_time datetime NULL;

ALTER TABLE marathon_event ADD COLUMN route_gpx varchar(255) NULL;

CREATE TABLE IF NOT EXISTS sms_log (
  id bigint NOT NULL AUTO_INCREMENT,
  phone varchar(20) NOT NULL,
  scene varchar(40) NOT NULL,
  content varchar(500) NOT NULL,
  provider varchar(40) NOT NULL,
  success tinyint NOT NULL DEFAULT 0,
  provider_msg varchar(200) NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_log (
  id bigint NOT NULL AUTO_INCREMENT,
  registration_id bigint NOT NULL,
  order_no varchar(32) NOT NULL,
  action varchar(20) NOT NULL,
  channel varchar(20) NULL,
  amount decimal(8,2) NULL,
  voucher_no varchar(64) NULL,
  remark varchar(200) NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reg (registration_id),
  KEY idx_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE marathon_event SET route_gpx='/routes/event-1.gpx' WHERE id=1;
UPDATE marathon_event SET route_gpx='/routes/event-2.gpx' WHERE id=2;
UPDATE marathon_event SET route_gpx='/routes/event-3.gpx' WHERE id=3;
