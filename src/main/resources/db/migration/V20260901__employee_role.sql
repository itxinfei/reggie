-- ============================================================
-- 员工-角色关联表（多对多，补全 RBAC 闭环：用户→角色→权限）
-- 历史缺陷：仅有 role_permission（角色→权限），缺 employee→role 关联，
--           导致权限管理"分配权限时找不到用户"——Employee.role 仅 1/2 单值枚举装不下自定义角色。
-- 设计：tenant_id 列由 MP TenantLineInnerInterceptor 自动隔离；uk(employee_id,role_id) 保证幂等。
-- 兼容：保留 Employee.role(1/2) 映射 SUPER_ADMIN/STORE_MANAGER 的登录超管判断，不动。
-- ============================================================
CREATE TABLE IF NOT EXISTS employee_role (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  employee_id bigint       NOT NULL COMMENT '员工ID',
  role_id     bigint       NOT NULL COMMENT '角色ID',
  tenant_id   bigint       NULL DEFAULT NULL COMMENT '租户ID（MP 租户拦截器自动注入）',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_employee_role (employee_id, role_id),
  KEY idx_employee_role_employee (employee_id),
  KEY idx_employee_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工-角色关联表（多对多）';
