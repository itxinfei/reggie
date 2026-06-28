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
