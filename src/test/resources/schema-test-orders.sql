DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id bigint NOT NULL,
    number varchar(50) DEFAULT NULL,
    status int DEFAULT '2',
    user_id bigint DEFAULT NULL,
    address_book_id bigint DEFAULT NULL,
    order_time datetime DEFAULT NULL,
    checkout_time datetime DEFAULT NULL,
    pay_method int DEFAULT '1',
    amount decimal(10,2) DEFAULT NULL,
    remark varchar(255) DEFAULT NULL,
    phone varchar(32) DEFAULT NULL,
    address varchar(255) DEFAULT NULL,
    user_name varchar(64) DEFAULT NULL,
    consignee varchar(32) DEFAULT NULL,
    table_id bigint DEFAULT NULL,
    dining_type varchar(20) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE order_detail (
    id bigint NOT NULL,
    name varchar(50) DEFAULT NULL,
    image varchar(200) DEFAULT NULL,
    order_id bigint DEFAULT NULL,
    dish_id bigint DEFAULT NULL,
    setmeal_id bigint DEFAULT NULL,
    dish_flavor varchar(50) DEFAULT NULL,
    number int DEFAULT '1',
    amount decimal(10,2) DEFAULT NULL,
    PRIMARY KEY (id)
);
