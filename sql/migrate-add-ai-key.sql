-- 已有 MySQL 数据库升级脚本：新增「个人 AI Key」字段
-- 全新部署不需要执行（建表脚本里已包含该字段）
ALTER TABLE sys_user ADD COLUMN ai_api_key VARCHAR(255) NULL;
