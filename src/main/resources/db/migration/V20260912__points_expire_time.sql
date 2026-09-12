-- 积分过期字段：points_record 新增 expire_time 列
-- 积分有效期为获取之日起 1 年，定时任务每天凌晨扫描过期记录自动扣减

ALTER TABLE points_record ADD COLUMN expire_time datetime DEFAULT NULL COMMENT '积分过期时间' AFTER remark;

-- 索引：定时任务按 expire_time 范围扫描
CREATE INDEX idx_points_expire ON points_record (tenant_id, expire_time, is_deleted);

-- 历史数据补刷：已有 IN 类型积分记录设置过期时间为创建时间 + 1 年
UPDATE points_record
   SET expire_time = DATE_ADD(created_time, INTERVAL 1 YEAR)
 WHERE type = 'IN' AND expire_time IS NULL AND is_deleted = 0;
