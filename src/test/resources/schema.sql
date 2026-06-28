DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS category;

CREATE TABLE dish (
  id bigint NOT NULL,
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
