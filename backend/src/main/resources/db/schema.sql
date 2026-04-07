-- MySQL 建表（时间存 epoch 毫秒 BIGINT，便于 MyBatis 映射）
--
-- 说明：若数据库已建表，再次执行本文件不会修改已有表结构。
-- 后续对表结构的任何变更，请只通过：
--   - resources/db/schema_alter.sql 中记录 ALTER 语句（可复制执行、便于审计）
--   - com.speaker.app.bootstrap.SchemaAlterMigration 中按「列不存在则 ALTER」自动执行
-- 勿依赖修改本文件里的 CREATE TABLE 来更新已存在的库。

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255),
    wechat_openid VARCHAR(64),
    created_at BIGINT NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_wechat UNIQUE (wechat_openid)
);

CREATE TABLE IF NOT EXISTS practice_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    part VARCHAR(16) NOT NULL,
    topic VARCHAR(256),
    topic_prompt TEXT,
    status VARCHAR(16) NOT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT,
    topic_source VARCHAR(16) NULL,
    season_label VARCHAR(64) NULL,
    allow_ai_expand TINYINT(1) NOT NULL DEFAULT 0,
    session_state_json TEXT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS conversation_turns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    seq INT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    brief_eval TEXT
);

CREATE TABLE IF NOT EXISTS session_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    pronunciation_score INT,
    grammar_score INT,
    coherence_score INT,
    fluency_score INT,
    ideas_score INT,
    overall_band VARCHAR(8),
    detailed_feedback TEXT,
    suggestions_json TEXT,
    CONSTRAINT uk_session_reports_session UNIQUE (session_id)
);

CREATE TABLE IF NOT EXISTS question_bank_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    season_label VARCHAR(32) NOT NULL,
    part VARCHAR(8) NOT NULL,
    topic VARCHAR(512) NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT,
    keywords_json TEXT,
    sort_order INT,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
);
