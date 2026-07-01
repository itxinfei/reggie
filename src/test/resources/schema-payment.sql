DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS payment_order;

CREATE TABLE payment_order (
    id bigint NOT NULL AUTO_INCREMENT,
    order_id bigint NOT NULL,
    tenant_id bigint DEFAULT NULL,
    trade_no varchar(64) NOT NULL,
    channel_trade_no varchar(128) DEFAULT NULL,
    channel varchar(20) NOT NULL,
    amount decimal(10,2) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    paid_time datetime DEFAULT NULL,
    notify_time datetime DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (trade_no)
);

CREATE TABLE refund_record (
    id bigint NOT NULL AUTO_INCREMENT,
    payment_order_id bigint NOT NULL,
    tenant_id bigint DEFAULT NULL,
    refund_no varchar(64) NOT NULL,
    amount decimal(10,2) NOT NULL,
    reason varchar(255) DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (refund_no)
);
