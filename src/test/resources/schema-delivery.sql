DROP TABLE IF EXISTS delivery_order;

CREATE TABLE delivery_order (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    platform_order_id varchar(128) NOT NULL,
    platform varchar(20) NOT NULL,
    dish_summary varchar(500) DEFAULT NULL,
    amount decimal(10,2) DEFAULT NULL,
    user_name varchar(50) DEFAULT NULL,
    phone varchar(20) DEFAULT NULL,
    address varchar(255) DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    order_time datetime DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
