-- V20260930: AI 提示词模板历史版本表
-- 用途：模板更新/重置/回滚前自动快照当前内容，支持运营回滚与追溯。
--      与 ai_prompt_template 同构但无 tenant_id（主表本身无租户列，已加入租户插件白名单）。
-- 注意：本脚本需手动执行（项目无 Flyway 自动迁移）。

CREATE TABLE IF NOT EXISTS ai_prompt_template_history (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  template_id bigint NOT NULL COMMENT '所属模板ID（ai_prompt_template.id）',
  code varchar(64) NOT NULL COMMENT '模板编码（快照时点）',
  scene varchar(50) NOT NULL COMMENT '场景（快照时点）',
  type varchar(16) NOT NULL COMMENT '类型 SYSTEM/WELCOME/QUICK（快照时点）',
  title varchar(100) NOT NULL COMMENT '模板名称（快照时点）',
  content text NULL COMMENT '正文内容（快照时点）',
  quick_questions varchar(1000) DEFAULT NULL COMMENT '快捷问题JSON（快照时点）',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（快照时点）',
  version int NOT NULL COMMENT '该快照对应的模板版本号',
  operator_id bigint DEFAULT NULL COMMENT '触发快照的操作人（员工ID）',
  create_time datetime NOT NULL COMMENT '快照时间',
  PRIMARY KEY (id),
  KEY idx_apt_history_template (template_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板历史版本（更新/重置前自动快照，支持回滚）';
