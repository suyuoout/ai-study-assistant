-- MySQL 初始化脚本（docker-compose 首次启动时自动执行）
-- 数据库由环境变量 MYSQL_DATABASE 创建，这里只建表

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50)  DEFAULT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS study_document (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(200) DEFAULT NULL,
    file_name    VARCHAR(255) DEFAULT NULL,
    storage_path VARCHAR(500) DEFAULT NULL,
    file_type    VARCHAR(20)  DEFAULT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_document_user (user_id),
    CONSTRAINT fk_document_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_chunk (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    document_id BIGINT       NOT NULL,
    chunk_index INT          NOT NULL,
    content     TEXT         NOT NULL,
    page_number INT          DEFAULT NULL,
    vector_id   VARCHAR(100) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chunk_document (document_id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES study_document (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) DEFAULT NULL,
    chat_type   VARCHAR(20)  NOT NULL DEFAULT 'GENERAL',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_user (user_id),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    session_id   BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    content      TEXT        NOT NULL,
    sources_json JSON        DEFAULT NULL,
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_message_session (session_id),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS study_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    goal          TEXT         DEFAULT NULL,
    content       TEXT         DEFAULT NULL,
    start_date    DATE         DEFAULT NULL,
    end_date      DATE         DEFAULT NULL,
    daily_minutes INT          DEFAULT NULL,
    status        VARCHAR(20)  DEFAULT '进行中',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_plan_user (user_id),
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS study_task (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    plan_id           BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT         DEFAULT NULL,
    task_date         DATE         DEFAULT NULL,
    estimated_minutes INT          DEFAULT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    actual_minutes    INT          DEFAULT NULL,
    completed_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_task_plan (plan_id),
    KEY idx_task_date (task_date),
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES study_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
