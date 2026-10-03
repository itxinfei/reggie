-- ============================================================
-- Reggie 外卖系统 — 最小初始化数据（自助建表路径专用）
-- ============================================================
-- 用途：配合 src/test/resources/schema*.sql 建表后，插入最小可登录数据集。
-- 前提：必须用 UTF-8 导入，否则中文乱码：
--   mysql -uroot -p --default-character-set=utf8mb4 reggie < db/seed-minimal.sql
--
-- 说明：
--   1) 密码为 MD5('123456')，系统登录校验通过后会自动升级为 BCrypt。
--      正式部署请务必在首次登录后立刻改密（后台或 PUT /employee/password）。
--   2) 后台左侧菜单写死在 backend/index.html，非数据库驱动；
--      绑定 SUPER_ADMIN 角色后即可看到全部菜单。
--   3) 本脚本可重复执行（先清后插），不会产生重复数据。
-- ============================================================

-- 租户（多租户隔离的根，所有业务数据都挂在某个 tenant_id 下）
INSERT INTO tenant (id, name, status, create_time, update_time)
VALUES (1, '演示租户', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), update_time = NOW();

-- 门店
INSERT INTO store (id, name, address, phone, status, create_time, update_time, is_deleted, tenant_id)
VALUES (1, '瑞吉外卖旗舰店', '北京市朝阳区示例路 1 号', '13900000000', 1, NOW(), NOW(), 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), address = VALUES(address), update_time = NOW();

-- 角色（role_key=SUPER_ADMIN 是 @RequiresAdmin 的放行依据）
INSERT INTO role (id, tenant_id, role_name, role_key, sort, status, create_time, update_time, create_user, update_user, is_deleted)
VALUES (18, 1, '超级管理员', 'SUPER_ADMIN', 1, 1, NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), update_time = NOW();

-- 管理员员工（role=1 为旧字段兜底，employee_role 才是 RBAC 正解）
INSERT INTO employee (id, username, name, phone, status, create_time, update_time,
                      create_user, update_user, password, password_type, tenant_id, role)
VALUES (1, 'admin', '超级管理员', '13901010001', 1, NOW(), NOW(),
        1, 1, 'e10adc3949ba59abbe56e057f20f883e', 'MD5', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), update_time = NOW();

-- 员工↔角色关联
INSERT INTO employee_role (id, employee_id, role_id, create_time)
VALUES (1, 1, 18, NOW())
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
