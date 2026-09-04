-- =============================================================
-- 门店 PC 打印代理（本地打印）改造建表脚本
-- 场景：打印机安装在门店收银 PC 上，由打印代理调用本地打印机；
--       后端只负责终端注册/任务队列/状态回执，不再直连服务器打印机。
-- 执行方式：手动执行（参考脚本，无 Flyway 自动迁移）
-- 日期：2026-08-30
-- =============================================================

-- 打印终端表：门店 PC 上运行的打印代理（一台 PC = 一个终端）
CREATE TABLE IF NOT EXISTS print_terminal (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '终端ID',
    tenant_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '门店租户ID',
    store_code      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '门店编码（代理注册时上报，用于匹配门店/租户）',
    terminal_code   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '终端唯一码（代理自生成 UUID）',
    token           VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '代理鉴权 token（注册时后端生成）',
    name            VARCHAR(100) NOT NULL DEFAULT '' COMMENT '终端名称（如：收银台-01）',
    printer_name    VARCHAR(200) NOT NULL DEFAULT '' COMMENT '本地打印机名（Windows 系统打印机名）',
    paper_size      VARCHAR(20)  NOT NULL DEFAULT '80mm' COMMENT '纸张：58mm / 80mm',
    print_types     VARCHAR(50)  NOT NULL DEFAULT 'BILL' COMMENT '绑定打印类型（逗号分隔）：BILL,KITCHEN,DELIVERY',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=停用（不派发任务）1=启用',
    last_heartbeat  DATETIME     DEFAULT NULL COMMENT '最近心跳时间（在线判定依据）',
    client_version  VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '代理客户端版本',
    created_time    DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME     DEFAULT NULL COMMENT '更新时间',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_print_terminal_code (terminal_code),
    KEY idx_print_terminal_tenant (tenant_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='打印终端（门店 PC 打印代理）';

-- 打印任务表：后端构建任务入队，代理心跳拉取执行后回执
CREATE TABLE IF NOT EXISTS print_task (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    tenant_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '门店租户ID',
    store_code    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '门店编码',
    order_id      BIGINT       DEFAULT NULL COMMENT '关联订单ID（测试任务为空）',
    task_type     VARCHAR(20)  NOT NULL DEFAULT 'BILL' COMMENT '任务类型：BILL/KITCHEN/DELIVERY/TEST',
    content       TEXT         COMMENT '打印内容 JSON（PrintLine 数组，由 PrinterTemplate 构建）',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(待领取)/PULLED(已领取)/SUCCESS/FAILED/CANCELLED',
    terminal_id   BIGINT       DEFAULT NULL COMMENT '派发终端ID',
    terminal_code VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '派发终端编码',
    error_msg     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '失败原因',
    retry_count   INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    created_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    pulled_time   DATETIME     DEFAULT NULL COMMENT '代理领取时间',
    done_time     DATETIME     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_print_task_status (status),
    KEY idx_print_task_order (order_id),
    KEY idx_print_task_terminal (terminal_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='打印任务队列（代理领取执行）';
