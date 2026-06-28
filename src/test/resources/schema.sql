DROP TABLE IF EXISTS tenant;
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
  id bigint(20) NOT NULL,
  username varchar(64) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  password varchar(64) DEFAULT NULL,
  phone varchar(16) DEFAULT NULL,
  sex varchar(8) DEFAULT NULL,
  id_number varchar(32) DEFAULT NULL,
  status int(11) DEFAULT '1',
  tenant_id bigint(20) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE tenant (
  id bigint(20) NOT NULL,
  name varchar(64) DEFAULT NULL,
  phone varchar(16) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  status int(11) DEFAULT '1',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE dish (
  id bigint NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  category_id bigint DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  code varchar(64) NOT NULL,
  image varchar(200) DEFAULT NULL,
  description varchar(400) DEFAULT NULL,
  status int DEFAULT '1',
  sort int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE category (
  id bigint NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  type int DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  sort int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS setmeal;
CREATE TABLE setmeal (
  id bigint(20) NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  category_id bigint(20) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  status int(11) DEFAULT '1',
  code varchar(32) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  image varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS setmeal_dish;
CREATE TABLE setmeal_dish (
  id bigint(20) NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  setmeal_id bigint(20) DEFAULT NULL,
  dish_id bigint(20) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  copies int(11) DEFAULT NULL,
  sort int(11) DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
  id bigint(20) NOT NULL,
  number varchar(50) DEFAULT NULL,
  status int(11) DEFAULT '2',
  user_id bigint(20) DEFAULT NULL,
  address_book_id bigint(20) DEFAULT NULL,
  order_time datetime DEFAULT NULL,
  checkout_time datetime DEFAULT NULL,
  pay_method int(11) DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  phone varchar(32) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  user_name varchar(64) DEFAULT NULL,
  consignee varchar(32) DEFAULT NULL,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS address_book;
CREATE TABLE address_book (
  id bigint(20) NOT NULL,
  user_id bigint(20) DEFAULT NULL,
  consignee varchar(32) DEFAULT NULL,
  sex varchar(8) DEFAULT NULL,
  phone varchar(16) DEFAULT NULL,
  province_code varchar(16) DEFAULT NULL,
  province_name varchar(32) DEFAULT NULL,
  city_code varchar(16) DEFAULT NULL,
  city_name varchar(32) DEFAULT NULL,
  district_code varchar(16) DEFAULT NULL,
  district_name varchar(32) DEFAULT NULL,
  detail varchar(255) DEFAULT NULL,
  label varchar(32) DEFAULT NULL,
  is_default int(11) DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS shopping_cart;
CREATE TABLE shopping_cart (
  id bigint(20) NOT NULL,
  name varchar(50) DEFAULT NULL,
  user_id bigint(20) DEFAULT NULL,
  dish_id bigint(20) DEFAULT NULL,
  setmeal_id bigint(20) DEFAULT NULL,
  dish_flavor varchar(50) DEFAULT NULL,
  number int(11) DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  image varchar(200) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS dish_flavor;
CREATE TABLE dish_flavor (
  id bigint(20) NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  dish_id bigint(20) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  value varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS order_detail;
CREATE TABLE order_detail (
  id bigint(20) NOT NULL,
  name varchar(50) DEFAULT NULL,
  image varchar(200) DEFAULT NULL,
  order_id bigint(20) DEFAULT NULL,
  dish_id bigint(20) DEFAULT NULL,
  setmeal_id bigint(20) DEFAULT NULL,
  dish_flavor varchar(50) DEFAULT NULL,
  number int(11) DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (id)
);
