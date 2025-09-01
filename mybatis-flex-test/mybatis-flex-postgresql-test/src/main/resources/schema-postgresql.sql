-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS tb_article;
DROP TABLE IF EXISTS tb_account;

-- 创建账户表
CREATE TABLE tb_account
(
    id        SERIAL PRIMARY KEY,
    user_name VARCHAR(100),
    age       INTEGER,
    sex       INTEGER,
    birthday  TIMESTAMP,
    options   TEXT,
    is_normal INTEGER DEFAULT 1,
    is_delete INTEGER DEFAULT 0
);

-- 创建文章表
CREATE TABLE tb_article
(
    id         SERIAL PRIMARY KEY,
    account_id INTEGER,
    title      VARCHAR(200),
    content    TEXT,
    is_delete  INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_article_account 
        FOREIGN KEY (account_id) REFERENCES tb_account(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_account_user_name ON tb_account(user_name);
CREATE INDEX idx_account_age ON tb_account(age);
CREATE INDEX idx_article_account_id ON tb_article(account_id);
CREATE INDEX idx_article_title ON tb_article(title);

-- 为更新时间添加触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tb_article_updated_at 
    BEFORE UPDATE ON tb_article 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();