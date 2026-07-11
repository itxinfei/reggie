DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_provider_config;
DROP TABLE IF EXISTS ai_user_profile;

CREATE TABLE ai_user_profile (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    taste_tags varchar(500) DEFAULT NULL,
    category_tags varchar(500) DEFAULT NULL,
    disliked_tags varchar(500) DEFAULT NULL,
    allergies varchar(500) DEFAULT NULL,
    price_preference varchar(20) DEFAULT NULL,
    user_tags varchar(500) DEFAULT NULL,
    frequent_dish_ids varchar(500) DEFAULT NULL,
    preferred_dining_type varchar(20) DEFAULT NULL,
    preferred_time_slot varchar(20) DEFAULT NULL,
    confidence decimal(3,2) DEFAULT '0.1',
    total_conversations int DEFAULT 0,
    total_feedbacks int DEFAULT 0,
    last_analyzed_time datetime DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ai_provider_config (
    id bigint NOT NULL AUTO_INCREMENT,
    provider_code varchar(50) DEFAULT NULL,
    provider_name varchar(100) DEFAULT NULL,
    base_url varchar(500) DEFAULT NULL,
    model_name varchar(100) DEFAULT NULL,
    api_key varchar(500) DEFAULT NULL,
    enabled int DEFAULT 0,
    is_active int DEFAULT 0,
    is_deleted int DEFAULT 0,
    timeout int DEFAULT 60,
    max_tokens int DEFAULT 2048,
    temperature decimal(3,2) DEFAULT 0.7,
    api_format varchar(50) DEFAULT 'openai_compatible',
    extra_headers varchar(1000) DEFAULT NULL,
    request_template varchar(2000) DEFAULT NULL,
    response_path varchar(500) DEFAULT NULL,
    icon_url varchar(500) DEFAULT NULL,
    sort int DEFAULT 0,
    last_test_result varchar(20) DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    created_time datetime DEFAULT NULL,
    updated_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ai_conversation (
    id bigint NOT NULL AUTO_INCREMENT,
    conversation_id varchar(64) NOT NULL,
    user_id bigint DEFAULT NULL,
    title varchar(200) DEFAULT NULL,
    scene varchar(50) DEFAULT NULL,
    message_count int DEFAULT 0,
    is_deleted int DEFAULT 0,
    create_time datetime DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ai_message (
    id bigint NOT NULL AUTO_INCREMENT,
    conversation_id varchar(64) NOT NULL,
    user_id bigint DEFAULT NULL,
    role varchar(20) NOT NULL,
    content text DEFAULT NULL,
    message_type varchar(20) DEFAULT 'text',
    feedback varchar(10) DEFAULT NULL,
    dish_ids varchar(500) DEFAULT NULL,
    tokens_used int DEFAULT 0,
    is_deleted int DEFAULT 0,
    create_time datetime DEFAULT NULL,
    PRIMARY KEY (id)
);
