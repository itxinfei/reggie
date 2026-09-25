-- 瑞吉外卖核心基础数据（幂等，可重复执行）
-- 仅包含必须"确定存在"的基础行：主租户/测试租户 + 超级管理员 admin。
-- 其余业务表的种子数据由反射式 TableSeeder 按"每表 >=10 条"自动补齐，不在此手写。
-- 全部使用 INSERT IGNORE：只保证存在，绝不覆盖运营已修改的数据。

-- 租户：1=主门店（开发联调），999=自动化测试租户
INSERT IGNORE INTO tenant (id, name, contact, phone, status, create_time, update_time, create_user, update_user)
VALUES
    (1,   '瑞吉主门店',       '管理员', '13800000000', 1, NOW(), NOW(), 1, 1),
    (999, '自动化测试租户',   '测试',   '13900000099', 1, NOW(), NOW(), 1, 1);

-- 超级管理员：admin / 123456
-- password_type=MD5，登录校验通过后 EmployeeController 会自动把密码升级为 BCRYPT。
INSERT IGNORE INTO employee
    (id, username, name, password, password_type, phone, status, sex, role,
     tenant_id, create_time, update_time, create_user, update_user)
VALUES
    (1, 'admin', '管理员', 'e10adc3949ba59abbe56e057f20f883e', 'MD5',
     '13800000000', 1, '1', 1, 1, NOW(), NOW(), 1, 1);
