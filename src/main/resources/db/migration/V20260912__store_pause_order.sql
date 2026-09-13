-- 暂停接单字段：0=正常接单 1=暂停接单
ALTER TABLE store_info ADD COLUMN pause_order INT NOT NULL DEFAULT 0 COMMENT '暂停接单 0:正常 1:暂停';
