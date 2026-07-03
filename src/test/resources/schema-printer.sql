DROP TABLE IF EXISTS printer_config;

CREATE TABLE printer_config (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    store_id bigint DEFAULT NULL,
    name varchar(50) NOT NULL,
    type varchar(20) NOT NULL,
    brand varchar(20) DEFAULT NULL,
    device_id varchar(100) DEFAULT NULL,
    ip_address varchar(15) DEFAULT NULL,
    port int DEFAULT NULL,
    paper_size varchar(10) DEFAULT '58mm',
    print_type varchar(20) NOT NULL,
    status int DEFAULT '1',
    sort int DEFAULT '0',
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    system_printer_name varchar(200) DEFAULT NULL,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS printer_log;
CREATE TABLE printer_log (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT '0',
    order_id bigint DEFAULT NULL,
    print_type varchar(20) NOT NULL,
    printer_id bigint DEFAULT NULL,
    content clob DEFAULT NULL,
    status int DEFAULT '0',
    error_msg varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);
