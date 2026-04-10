-- ============================================================
-- Smart Expense Splitter  |  Neev Cloud Internship Assignment
-- Database: expense_splitter   |  Currency: INR (₹)
-- ============================================================

CREATE DATABASE IF NOT EXISTS expense_splitter
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE expense_splitter;

-- ── Users ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)   UNIQUE NOT NULL,
    email      VARCHAR(100)  UNIQUE NOT NULL,
    password   VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ── Groups (back-ticked — reserved word in MySQL) ───────────
CREATE TABLE IF NOT EXISTS `groups` (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by BIGINT       NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ── Group Members ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS group_members (
    id        BIGINT    AUTO_INCREMENT PRIMARY KEY,
    group_id  BIGINT    NOT NULL,
    user_id   BIGINT    NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT uq_group_user UNIQUE (group_id, user_id)
);

-- ── Expenses ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    group_id    BIGINT          NOT NULL,
    description VARCHAR(255)    NOT NULL,
    amount      DECIMAL(12, 2)  NOT NULL COMMENT 'Amount in INR',
    paid_by     BIGINT          NOT NULL,
    category    VARCHAR(50)     DEFAULT 'Other' COMMENT 'AI-assigned: Food|Travel|Rent|Shopping|Other',
    split_type  ENUM('EQUAL','CUSTOM') NOT NULL DEFAULT 'EQUAL',
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_group  FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_payer  FOREIGN KEY (paid_by)  REFERENCES users(id)    ON DELETE CASCADE
);

-- ── Splits ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS splits (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY,
    expense_id  BIGINT         NOT NULL,
    user_id     BIGINT         NOT NULL,
    amount_owed DECIMAL(12, 2) NOT NULL COMMENT 'Amount this user owes for this expense, in INR',
    CONSTRAINT fk_split_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_split_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
);

-- ── Indexes for performance ───────────────────────────────────
CREATE INDEX idx_gm_user        ON group_members(user_id);
CREATE INDEX idx_gm_group       ON group_members(group_id);
CREATE INDEX idx_expense_group  ON expenses(group_id);
CREATE INDEX idx_expense_payer  ON expenses(paid_by);
CREATE INDEX idx_split_expense  ON splits(expense_id);
CREATE INDEX idx_split_user     ON splits(user_id);
