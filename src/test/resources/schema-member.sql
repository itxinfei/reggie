DROP TABLE IF EXISTS coupon_user;
DROP TABLE IF EXISTS coupon_template;
DROP TABLE IF EXISTS recharge_record;
DROP TABLE IF EXISTS points_record;
DROP TABLE IF EXISTS member_level;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    user_id bigint DEFAULT NULL,
    level_id bigint DEFAULT NULL,
    name varchar(50) DEFAULT NULL,
    phone varchar(20) NOT NULL,
    points bigint DEFAULT 0,
    balance decimal(10,2) DEFAULT 0.00,
    total_consumption decimal(10,2) DEFAULT 0.00,
    status int DEFAULT 1,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE (phone)
);

CREATE TABLE member_level (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    name varchar(50) NOT NULL,
    min_points bigint DEFAULT 0,
    discount decimal(3,2) DEFAULT 1.00,
    created_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE points_record (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL DEFAULT 0,
    member_id bigint NOT NULL,
    type varchar(10) NOT NULL,
    points int NOT NULL,
    biz_type varchar(50) DEFAULT NULL,
    biz_id bigint DEFAULT NULL,
    remark varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE recharge_record (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL DEFAULT 0,
    member_id bigint NOT NULL,
    amount decimal(10,2) NOT NULL,
    gift_amount decimal(10,2) DEFAULT 0.00,
    payment_method varchar(20) DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE coupon_template (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    name varchar(100) NOT NULL,
    type varchar(20) NOT NULL,
    condition_amount decimal(10,2) DEFAULT NULL,
    discount_amount decimal(10,2) DEFAULT NULL,
    discount_rate decimal(3,2) DEFAULT NULL,
    total_count int DEFAULT 0,
    remain_count int DEFAULT 0,
    valid_days int DEFAULT NULL,
    status int DEFAULT 1,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE coupon_user (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL DEFAULT 0,
    member_id bigint NOT NULL,
    template_id bigint NOT NULL,
    code varchar(32) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'UNUSED',
    used_time datetime DEFAULT NULL,
    order_id bigint DEFAULT NULL,
    expire_time datetime DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);
