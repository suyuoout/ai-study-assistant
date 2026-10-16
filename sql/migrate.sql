-- 现有 study_assistant 库的增量迁移脚本（可重复执行，幂等性以 information_schema 判断为准）
-- 覆盖：role / goal / daily_minutes / ai_api_key 加长 / enabled / must_change_password / ai_usage 表
USE study_assistant;

-- sys_user 增加 role 字段
SET @has_role = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'role'
);
SET @sql = IF(@has_role = 0,
    'ALTER TABLE sys_user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''USER'' COMMENT ''USER/ADMIN'' AFTER email',
    'SELECT ''role already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- study_plan 增加 goal、daily_minutes 字段
SET @has_goal = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'study_plan'
      AND COLUMN_NAME = 'goal'
);
SET @sql = IF(@has_goal = 0,
    'ALTER TABLE study_plan ADD COLUMN goal TEXT NULL COMMENT ''学习目标'' AFTER title',
    'SELECT ''goal already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_daily = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'study_plan'
      AND COLUMN_NAME = 'daily_minutes'
);
SET @sql = IF(@has_daily = 0,
    'ALTER TABLE study_plan ADD COLUMN daily_minutes INT NULL COMMENT ''每日可用分钟'' AFTER end_date',
    'SELECT ''daily_minutes already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 学习任务表
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

-- 对话会话表
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

-- 对话消息表
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

-- 学习资料表
CREATE TABLE IF NOT EXISTS study_document (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    user_id      BIGINT       NOT NULL COMMENT '所属用户',
    title        VARCHAR(200) DEFAULT NULL COMMENT '资料标题',
    file_name    VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) DEFAULT NULL COMMENT '文件保存路径',
    file_type    VARCHAR(20)  DEFAULT NULL COMMENT 'PDF/TXT/DOCX',
    status       VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PROCESSING/READY/FAILED',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '解析失败原因',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_document_user (user_id),
    CONSTRAINT fk_document_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资料表';

-- 文档片段表
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

-- ---------------------------------------------------------------------------
-- 2026-10 新增：个人 AI Key 加密存储、账号启用状态、强制改密、AI 用量统计
-- ---------------------------------------------------------------------------

-- 1. ai_api_key 加长（加密后字符串比原始 Key 长）
SET @has_key = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'ai_api_key'
);
SET @sql = IF(@has_key = 0,
    'ALTER TABLE sys_user ADD COLUMN ai_api_key VARCHAR(512) NULL COMMENT ''个人AI Key（加密存储）''',
    'ALTER TABLE sys_user MODIFY COLUMN ai_api_key VARCHAR(512) NULL COMMENT ''个人AI Key（加密存储）''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 账号启用状态
SET @has_enabled = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'enabled'
);
SET @sql = IF(@has_enabled = 0,
    'ALTER TABLE sys_user ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''1启用/0禁用''',
    'SELECT ''enabled already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 强制改密标记
SET @has_mcp = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'must_change_password'
);
SET @sql = IF(@has_mcp = 0,
    'ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1需要强制改密''',
    'SELECT ''must_change_password already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. AI 用量表
SET @has_doc_err = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'study_assistant'
      AND TABLE_NAME = 'study_document'
      AND COLUMN_NAME = 'error_message'
);
SET @sql = IF(@has_doc_err = 0,
    'ALTER TABLE study_document ADD COLUMN error_message VARCHAR(500) NULL COMMENT ''解析失败原因''',
    'SELECT ''error_message already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_usage (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id           BIGINT      NOT NULL COMMENT '用户ID',
    kind              VARCHAR(20) NOT NULL COMMENT 'CHAT/RAG/PLAN/REPORT',
    model             VARCHAR(50) DEFAULT NULL COMMENT '模型名',
    prompt_tokens     INT         DEFAULT NULL COMMENT '输入token',
    completion_tokens INT         DEFAULT NULL COMMENT '输出token',
    success           TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否成功',
    own_key           TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1表示用的是用户自己的Key',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_usage_user_time (user_id, create_time),
    KEY idx_usage_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用用量表';
