DROP TABLE IF EXISTS dining_queue;
DROP TABLE IF EXISTS dining_reservation;
DROP TABLE IF EXISTS dining_table;
DROP TABLE IF EXISTS dining_area;

CREATE TABLE dining_area (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    name varchar(50) NOT NULL,
    sort int DEFAULT 0,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE dining_table (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    area_id bigint DEFAULT NULL,
    name varchar(20) NOT NULL,
    seat_count int DEFAULT 4,
    status varchar(20) NOT NULL DEFAULT 'FREE',
    min_amount decimal(10,2) DEFAULT NULL,
    qr_code_url varchar(255) DEFAULT NULL,
    sort int DEFAULT 0,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE dining_queue (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    queue_no varchar(20) NOT NULL,
    phone varchar(20) DEFAULT NULL,
    seat_count int DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'WAITING',
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE dining_reservation (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    table_id bigint DEFAULT NULL,
    customer_name varchar(50) NOT NULL,
    phone varchar(20) NOT NULL,
    reserved_time datetime NOT NULL,
    seat_count int DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    remark varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);
