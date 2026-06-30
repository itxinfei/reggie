-- 为employee表新增password_type字段
ALTER TABLE employee ADD COLUMN password_type VARCHAR(20) DEFAULT 'MD5' COMMENT '密码加密类型';

-- 为已有数据设置默认值
UPDATE employee SET password_type = 'MD5' WHERE password_type IS NULL;
