DROP TABLE IF EXISTS stock_check_detail;
DROP TABLE IF EXISTS stock_record;
DROP TABLE IF EXISTS stock_check;
DROP TABLE IF EXISTS purchase_order_detail;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS material_category;
DROP TABLE IF EXISTS supplier;

CREATE TABLE supplier (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    name varchar(100) NOT NULL,
    contact varchar(50) DEFAULT NULL,
    phone varchar(20) DEFAULT NULL,
    address varchar(255) DEFAULT NULL,
    status int DEFAULT '1',
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE material_category (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    name varchar(50) NOT NULL,
    sort int DEFAULT '0',
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE material (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    category_id bigint DEFAULT NULL,
    name varchar(100) NOT NULL,
    unit varchar(10) NOT NULL,
    stock_qty decimal(10,2) DEFAULT '0.00',
    min_stock decimal(10,2) DEFAULT '0.00',
    unit_price decimal(10,2) DEFAULT NULL,
    supplier_id bigint DEFAULT NULL,
    barcode varchar(50) DEFAULT NULL,
    status int DEFAULT '1',
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE stock_record (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    material_id bigint NOT NULL,
    type varchar(10) NOT NULL,
    qty decimal(10,2) NOT NULL,
    unit_price decimal(10,2) DEFAULT NULL,
    total_amount decimal(10,2) DEFAULT NULL,
    biz_id bigint DEFAULT NULL,
    remark varchar(255) DEFAULT NULL,
    operator varchar(50) DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE stock_check (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    check_no varchar(64) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    total_diff_amount decimal(10,2) DEFAULT NULL,
    operator varchar(50) DEFAULT NULL,
    remark varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE stock_check_detail (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    check_id bigint NOT NULL,
    material_id bigint NOT NULL,
    book_qty decimal(10,2) NOT NULL,
    actual_qty decimal(10,2) NOT NULL,
    diff_qty decimal(10,2) NOT NULL,
    remark varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE purchase_order (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    order_no varchar(64) NOT NULL,
    supplier_id bigint DEFAULT NULL,
    total_amount decimal(10,2) DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    operator varchar(50) DEFAULT NULL,
    remark varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE purchase_order_detail (
    id bigint NOT NULL AUTO_INCREMENT,
    purchase_order_id bigint NOT NULL,
    material_id bigint NOT NULL,
    qty decimal(10,2) NOT NULL,
    unit_price decimal(10,2) DEFAULT NULL,
    amount decimal(10,2) DEFAULT NULL,
    received_qty decimal(10,2) DEFAULT '0.00',
    remark varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

INSERT INTO supplier (id, tenant_id, name, contact, phone, address, status) VALUES
(1, 1, '测试供应商', '张三', '13800138000', '北京市朝阳区', 1);

INSERT INTO material_category (id, tenant_id, name, sort) VALUES
(1, 1, '蔬菜类', 1),
(2, 1, '肉类', 2);
