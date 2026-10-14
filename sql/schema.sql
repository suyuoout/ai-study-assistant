-- 智能学习助手系统 - 完整建表脚本
-- 数据库：smart_learning（现有库名 study_assistant 亦可，脚本默认用 study_assistant）

CREATE DATABASE IF NOT EXISTS smart_learning
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_learning;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    ai_api_key  VARCHAR(255) DEFAULT NULL COMMENT '个人AI Key',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 学习资料表
CREATE TABLE IF NOT EXISTS study_document (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    user_id      BIGINT       NOT NULL COMMENT '所属用户',
    title        VARCHAR(200) DEFAULT NULL COMMENT '资料标题',
    file_name    VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) DEFAULT NULL COMMENT '文件保存路径',
    file_type    VARCHAR(20)  DEFAULT NULL COMMENT 'PDF/TXT/DOCX',
    status       VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PROCESSING/READY/FAILED',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_document_user (user_id),
    CONSTRAINT fk_document_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资料表';

-- 3. 文档片段表
CREATE TABLE IF NOT EXISTS document_chunk (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '片段ID',
    document_id BIGINT       NOT NULL COMMENT '资料ID',
    chunk_index INT          NOT NULL COMMENT '片段序号',
    content     TEXT         NOT NULL COMMENT '片段文本',
    page_number INT          DEFAULT NULL COMMENT '页码',
    vector_id   VARCHAR(100) DEFAULT NULL COMMENT '向量存储记录ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_chunk_document (document_id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES study_document (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档片段表';

-- 4. 对话会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    chat_type   VARCHAR(20)  NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/RAG',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_session_user (user_id),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

-- 5. 对话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id   BIGINT      NOT NULL COMMENT '会话ID',
    role         VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT',
    content      TEXT        NOT NULL COMMENT '消息正文',
    sources_json JSON        DEFAULT NULL COMMENT 'RAG引用',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_message_session (session_id),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 6. 学习计划表
CREATE TABLE IF NOT EXISTS study_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    title         VARCHAR(200) NOT NULL COMMENT '计划名称',
    goal          TEXT         DEFAULT NULL COMMENT '学习目标',
    content       TEXT         DEFAULT NULL COMMENT '计划内容（AI生成）',
    start_date    DATE         DEFAULT NULL COMMENT '开始日期',
    end_date      DATE         DEFAULT NULL COMMENT '截止日期',
    daily_minutes INT          DEFAULT NULL COMMENT '每日可用分钟',
    status        VARCHAR(20)  DEFAULT '进行中' COMMENT '计划状态',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY fk_plan_user (user_id),
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';

-- 7. 学习任务表
CREATE TABLE IF NOT EXISTS study_task (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    plan_id           BIGINT       NOT NULL COMMENT '所属计划',
    title             VARCHAR(200) NOT NULL COMMENT '任务标题',
    description       TEXT         DEFAULT NULL COMMENT '任务说明',
    task_date         DATE         DEFAULT NULL COMMENT '计划完成日期',
    estimated_minutes INT          DEFAULT NULL COMMENT '预计用时',
    status            VARCHAR(20)  NOT NULL DEFAULT 'TODO' COMMENT 'TODO/DOING/DONE',
    actual_minutes    INT          DEFAULT NULL COMMENT '实际用时',
    completed_time    DATETIME     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_task_plan (plan_id),
    KEY idx_task_date (task_date),
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES study_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习任务表';
