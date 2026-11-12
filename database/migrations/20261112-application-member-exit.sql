-- Apply after selecting the existing recruitment database.
-- Existing business data is preserved. This upgrade can be rerun after interruption.
SET NAMES utf8mb4;
SET @upgrade_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'application' AND column_name = 'rejection_reason'),
    'SELECT 1', 'ALTER TABLE application ADD COLUMN rejection_reason VARCHAR(255) NULL');
PREPARE upgrade_statement FROM @upgrade_sql;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;

SET @upgrade_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'leader_transfer' AND column_name = 'exit_after_transfer'),
    'SELECT 1', 'ALTER TABLE leader_transfer ADD COLUMN exit_after_transfer TINYINT UNSIGNED NOT NULL DEFAULT 0');
PREPARE upgrade_statement FROM @upgrade_sql;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;

SET @upgrade_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()
        AND table_name = 'leader_transfer' AND constraint_name = 'chk_transfer_exit'),
    'SELECT 1', 'ALTER TABLE leader_transfer ADD CONSTRAINT chk_transfer_exit CHECK (exit_after_transfer IN (0, 1))');
PREPARE upgrade_statement FROM @upgrade_sql;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;

CREATE TABLE IF NOT EXISTS `member_exit_request` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reason` VARCHAR(255) NULL,
    `rejection_reason` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME NULL,
    `pending_user_id` BIGINT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN `status` = 'PENDING' THEN `user_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_exit_pending` (`recruitment_id`, `pending_user_id`),
    KEY `idx_member_exit_recruitment_status` (`recruitment_id`, `status`, `created_at`, `id`),
    KEY `idx_member_exit_user_created` (`user_id`, `created_at`, `id`),
    CONSTRAINT `chk_member_exit_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT `chk_member_exit_processed` CHECK (
        (`status` = 'PENDING' AND `processed_at` IS NULL) OR
        (`status` <> 'PENDING' AND `processed_at` IS NOT NULL)),
    CONSTRAINT `fk_member_exit_member` FOREIGN KEY (`recruitment_id`, `user_id`)
        REFERENCES `team_member` (`recruitment_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @upgrade_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()
        AND table_name = 'notification' AND constraint_name = 'chk_notification_event_type'),
    'ALTER TABLE notification DROP CHECK chk_notification_event_type', 'SELECT 1');
PREPARE upgrade_statement FROM @upgrade_sql;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;

ALTER TABLE notification ADD CONSTRAINT `chk_notification_event_type` CHECK (`event_type` IN
        ('APPLICATION_SUBMITTED', 'APPLICATION_ACCEPTED', 'APPLICATION_REJECTED', 'APPLICATION_WITHDRAWN', 'APPLICATION_EXPIRED',
         'RECRUITMENT_TEAMED', 'RECRUITMENT_CLOSED', 'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_REMOVED',
         'MEMBER_EXIT_REQUESTED', 'MEMBER_EXIT_APPROVED', 'MEMBER_EXIT_REJECTED',
         'LEADER_TRANSFER_REQUESTED', 'LEADER_TRANSFER_ACCEPTED', 'LEADER_TRANSFER_REJECTED', 'LEADER_TRANSFER_CANCELLED', 'LEADER_TRANSFER_EXPIRED',
         'REPORT_RESOLVED', 'REPORT_DISMISSED'));
