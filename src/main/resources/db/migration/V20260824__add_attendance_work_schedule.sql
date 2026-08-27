-- ==================== 考勤 / 排班模块表结构（补全） ====================
-- 背景：Attendance / WorkSchedule 实体已存在，但项目内从未有对应建表脚本，
-- 导致模块接口运行时报 "Table 'reggie.attendance' doesn't exist"。
-- 本脚本按实体字段 + 项目建表惯例补齐，幂等可重复执行。

-- 考勤记录表
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) DEFAULT NULL COMMENT '员工姓名',
  `date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '考勤状态：0=缺勤,1=正常,2=迟到,3=早退,4=请假,5=出差',
  `work_hours` decimal(5,2) DEFAULT '0.00' COMMENT '工时（小时）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_attendance_emp_date` (`employee_id`, `date`),
  KEY `idx_attendance_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- 排班记录表
CREATE TABLE IF NOT EXISTS `work_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) DEFAULT NULL COMMENT '员工姓名',
  `schedule_date` date NOT NULL COMMENT '排班日期',
  `shift` tinyint DEFAULT '3' COMMENT '班次：0=早班,1=中班,2=晚班,3=全天',
  `shift_start` time DEFAULT NULL COMMENT '班次开始时间',
  `shift_end` time DEFAULT NULL COMMENT '班次结束时间',
  `work_date_str` varchar(20) DEFAULT NULL COMMENT '工作日期字符串',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_emp_date` (`employee_id`, `schedule_date`),
  KEY `idx_schedule_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班记录表';
