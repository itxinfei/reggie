-- 排队取号 / 到店预订记录关联 C 端顾客用户
-- 员工在后台代录的记录 user_id 可为 NULL；顾客端自助取号/预订必带
ALTER TABLE dining_queue
    ADD COLUMN user_id BIGINT NULL COMMENT '取号顾客用户ID' AFTER phone;

ALTER TABLE dining_reservation
    ADD COLUMN user_id BIGINT NULL COMMENT '预订顾客用户ID' AFTER phone;
