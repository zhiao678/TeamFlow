-- Recruitment platform development database.
-- MySQL 8.0.16+ is required for enforced CHECK constraints.
-- Development reset only: this script drops all recruitment data.
-- This script uses a separate database and does not modify the legacy TeamFlow schema.

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `teamflow_recruitment`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `teamflow_recruitment`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `report_action`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `user_block`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `conversation_member`;
DROP TABLE IF EXISTS `conversation`;
DROP TABLE IF EXISTS `leader_transfer`;
DROP TABLE IF EXISTS `recruitment_favorite`;
DROP TABLE IF EXISTS `team_member`;
DROP TABLE IF EXISTS `application`;
DROP TABLE IF EXISTS `recruitment_tag`;
DROP TABLE IF EXISTS `user_tag`;
DROP TABLE IF EXISTS `recruitment`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user_profile`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `display_name` VARCHAR(50) NOT NULL,
    `avatar_url` VARCHAR(255) NULL,
    `platform_role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `account_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_recruitment_user_username` UNIQUE (`username`),
    CONSTRAINT `chk_user_platform_role` CHECK (`platform_role` IN ('USER', 'ADMIN')),
    CONSTRAINT `chk_user_account_status` CHECK (`account_status` IN ('ACTIVE', 'RESTRICTED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_profile` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `department` VARCHAR(100) NULL,
    `grade` VARCHAR(30) NULL,
    `bio` VARCHAR(500) NULL,
    `contact_allowed` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_recruitment_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT UNSIGNED NULL,
    `enabled` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_recruitment_category_name` UNIQUE (`name`),
    CONSTRAINT `fk_recruitment_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tag` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `enabled` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_recruitment_tag_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_tag` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `tag_id` BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`user_id`, `tag_id`),
    CONSTRAINT `fk_recruitment_user_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_recruitment_user_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recruitment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT UNSIGNED NOT NULL,
    `category_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `course_name` VARCHAR(100) NULL,
    `teacher_name` VARCHAR(100) NULL,
    `purpose` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `requirements` TEXT NULL,
    `max_members` INT NOT NULL,
    `deadline_at` DATETIME NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `close_reason` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_recruitment_status_category` (`status`, `category_id`),
    KEY `idx_recruitment_owner` (`owner_id`),
    KEY `idx_recruitment_deadline` (`deadline_at`),
    KEY `idx_recruitment_status_created` (`status`, `created_at`, `id`),
    KEY `idx_recruitment_owner_created` (`owner_id`, `created_at`, `id`),
    KEY `idx_recruitment_course` (`course_name`),
    KEY `idx_recruitment_teacher` (`teacher_name`),
    CONSTRAINT `chk_recruitment_max_members` CHECK (`max_members` >= 2),
    CONSTRAINT `chk_recruitment_status` CHECK (`status` IN ('DRAFT', 'RECRUITING', 'TEAMED', 'CLOSED')),
    CONSTRAINT `fk_recruitment_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_recruitment_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recruitment_tag` (
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `tag_id` BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`recruitment_id`, `tag_id`),
    CONSTRAINT `fk_recruitment_tag_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_recruitment_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `application` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `applicant_id` BIGINT UNSIGNED NOT NULL,
    `introduction` VARCHAR(1000) NULL,
    `skills_description` VARCHAR(1000) NULL,
    `available_time` VARCHAR(255) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME NULL,
    `pending_applicant_id` BIGINT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN `status` = 'PENDING' THEN `applicant_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_pending` (`recruitment_id`, `pending_applicant_id`),
    KEY `idx_application_recruitment_status` (`recruitment_id`, `status`),
    KEY `idx_application_recruitment_applicant` (`recruitment_id`, `applicant_id`),
    CONSTRAINT `chk_application_status` CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT `fk_application_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_application_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `left_at` DATETIME NULL,
    `active_leader_recruitment_id` BIGINT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN `role` = 'LEADER' AND `status` = 'ACTIVE' THEN `recruitment_id` ELSE NULL END) VIRTUAL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_member_recruitment_user` (`recruitment_id`, `user_id`),
    UNIQUE KEY `uk_team_member_active_leader` (`active_leader_recruitment_id`),
    KEY `idx_team_member_recruitment_status` (`recruitment_id`, `status`),
    CONSTRAINT `chk_team_member_role` CHECK (`role` IN ('LEADER', 'MEMBER')),
    CONSTRAINT `chk_team_member_status` CHECK (`status` IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT `fk_team_member_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_team_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- P0 data foundation. These tables do not imply that their APIs are implemented.
CREATE TABLE `recruitment_favorite` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `recruitment_id`),
    KEY `idx_favorite_user_created` (`user_id`, `created_at`, `recruitment_id`),
    KEY `idx_favorite_recruitment` (`recruitment_id`, `created_at`),
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `leader_transfer` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `from_user_id` BIGINT UNSIGNED NOT NULL,
    `to_user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME NULL,
    `pending_recruitment_id` BIGINT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN `status` = 'PENDING' THEN `recruitment_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transfer_pending` (`pending_recruitment_id`),
    KEY `idx_transfer_rec_created` (`recruitment_id`, `created_at`, `id`),
    KEY `idx_transfer_recipient_status` (`to_user_id`, `status`, `created_at`),
    CONSTRAINT `chk_transfer_users` CHECK (`from_user_id` <> `to_user_id`),
    CONSTRAINT `chk_transfer_status` CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT `chk_transfer_processed` CHECK (
        (`status` = 'PENDING' AND `processed_at` IS NULL) OR
        (`status` <> 'PENDING' AND `processed_at` IS NOT NULL)),
    CONSTRAINT `fk_transfer_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_transfer_from_member` FOREIGN KEY (`recruitment_id`, `from_user_id`)
        REFERENCES `team_member` (`recruitment_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_transfer_to_member` FOREIGN KEY (`recruitment_id`, `to_user_id`)
        REFERENCES `team_member` (`recruitment_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `conversation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `type` VARCHAR(20) NOT NULL,
    `recruitment_id` BIGINT UNSIGNED NULL,
    `direct_user_low_id` BIGINT UNSIGNED NULL,
    `direct_user_high_id` BIGINT UNSIGNED NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_recruitment` (`recruitment_id`),
    UNIQUE KEY `uk_conversation_direct_pair` (`direct_user_low_id`, `direct_user_high_id`),
    CONSTRAINT `chk_conversation_type` CHECK (`type` IN ('DIRECT', 'TEAM')),
    CONSTRAINT `chk_conversation_status` CHECK (`status` IN ('ACTIVE', 'READ_ONLY')),
    CONSTRAINT `chk_conversation_identity` CHECK (
        (`type` = 'TEAM' AND `recruitment_id` IS NOT NULL AND
            `direct_user_low_id` IS NULL AND `direct_user_high_id` IS NULL) OR
        (`type` = 'DIRECT' AND `recruitment_id` IS NULL AND
            `direct_user_low_id` IS NOT NULL AND `direct_user_high_id` IS NOT NULL AND
            `direct_user_low_id` < `direct_user_high_id`)),
    CONSTRAINT `fk_conversation_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_conversation_low_user` FOREIGN KEY (`direct_user_low_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_conversation_high_user` FOREIGN KEY (`direct_user_high_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `conversation_member` (
    `conversation_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `left_at` DATETIME NULL,
    `last_read_message_id` BIGINT UNSIGNED NULL,
    PRIMARY KEY (`conversation_id`, `user_id`),
    KEY `idx_conversation_member_user_status` (`user_id`, `status`, `conversation_id`),
    CONSTRAINT `chk_conversation_member_status` CHECK (`status` IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT `chk_conversation_member_left` CHECK (
        (`status` = 'ACTIVE' AND `left_at` IS NULL) OR
        (`status` <> 'ACTIVE' AND `left_at` IS NOT NULL)),
    CONSTRAINT `fk_conversation_member_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_conversation_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT UNSIGNED NOT NULL,
    `sender_id` BIGINT UNSIGNED NOT NULL,
    `client_message_id` VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    `source_recruitment_id` BIGINT UNSIGNED NULL,
    `content` VARCHAR(2000) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_conversation_id` (`conversation_id`, `id`),
    UNIQUE KEY `uk_message_client` (`conversation_id`, `sender_id`, `client_message_id`),
    KEY `idx_message_conversation_created` (`conversation_id`, `created_at`, `id`),
    CONSTRAINT `chk_message_content` CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),
    CONSTRAINT `chk_message_client_id` CHECK (CHAR_LENGTH(TRIM(`client_message_id`)) > 0),
    CONSTRAINT `chk_message_status` CHECK (`status` IN ('VISIBLE', 'HIDDEN')),
    CONSTRAINT `fk_message_sender_member` FOREIGN KEY (`conversation_id`, `sender_id`)
        REFERENCES `conversation_member` (`conversation_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_message_source_recruitment` FOREIGN KEY (`source_recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The composite reference prevents a read position from pointing to another conversation.
ALTER TABLE `conversation_member`
    ADD CONSTRAINT `fk_conversation_member_read_message` FOREIGN KEY (`conversation_id`, `last_read_message_id`)
        REFERENCES `message` (`conversation_id`, `id`) ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE TABLE `user_block` (
    `blocker_id` BIGINT UNSIGNED NOT NULL,
    `blocked_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`blocker_id`, `blocked_id`),
    KEY `idx_block_blocked` (`blocked_id`, `blocker_id`),
    CONSTRAINT `chk_block_different_users` CHECK (`blocker_id` <> `blocked_id`),
    CONSTRAINT `fk_block_blocker` FOREIGN KEY (`blocker_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_block_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `report` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `reporter_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `target_user_id` BIGINT UNSIGNED NULL,
    `target_recruitment_id` BIGINT UNSIGNED NULL,
    `target_message_id` BIGINT UNSIGNED NULL,
    `reason` VARCHAR(1000) NOT NULL,
    `evidence_snapshot` TEXT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `assigned_admin_id` BIGINT UNSIGNED NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `resolved_at` DATETIME NULL,
    `open_target_id` BIGINT UNSIGNED GENERATED ALWAYS AS
        (CASE WHEN `status` IN ('PENDING', 'PROCESSING')
            THEN COALESCE(`target_user_id`, `target_recruitment_id`, `target_message_id`) ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_open_target` (`reporter_id`, `target_type`, `open_target_id`),
    KEY `idx_report_status_created` (`status`, `created_at`, `id`),
    KEY `idx_report_reporter_created` (`reporter_id`, `created_at`, `id`),
    KEY `idx_report_admin_status` (`assigned_admin_id`, `status`, `created_at`),
    CONSTRAINT `chk_report_target_type` CHECK (`target_type` IN ('USER', 'RECRUITMENT', 'MESSAGE')),
    CONSTRAINT `chk_report_target` CHECK (
        (`target_type` = 'USER' AND `target_user_id` IS NOT NULL AND `target_recruitment_id` IS NULL AND `target_message_id` IS NULL) OR
        (`target_type` = 'RECRUITMENT' AND `target_user_id` IS NULL AND `target_recruitment_id` IS NOT NULL AND `target_message_id` IS NULL) OR
        (`target_type` = 'MESSAGE' AND `target_user_id` IS NULL AND `target_recruitment_id` IS NULL AND `target_message_id` IS NOT NULL)),
    CONSTRAINT `chk_report_reason` CHECK (CHAR_LENGTH(TRIM(`reason`)) > 0),
    CONSTRAINT `chk_report_status` CHECK (`status` IN ('PENDING', 'PROCESSING', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT `chk_report_resolved` CHECK (
        (`status` IN ('PENDING', 'PROCESSING') AND `resolved_at` IS NULL) OR
        (`status` IN ('RESOLVED', 'DISMISSED') AND `resolved_at` IS NOT NULL)),
    CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_report_target_user` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_report_target_recruitment` FOREIGN KEY (`target_recruitment_id`) REFERENCES `recruitment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_report_target_message` FOREIGN KEY (`target_message_id`) REFERENCES `message` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_report_admin` FOREIGN KEY (`assigned_admin_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `report_action` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `report_id` BIGINT UNSIGNED NOT NULL,
    `admin_id` BIGINT UNSIGNED NOT NULL,
    `action_type` VARCHAR(30) NOT NULL,
    `reason` VARCHAR(1000) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_report_action_report_created` (`report_id`, `created_at`, `id`),
    KEY `idx_report_action_admin_created` (`admin_id`, `created_at`, `id`),
    CONSTRAINT `chk_report_action_type` CHECK (`action_type` IN
        ('ASSIGN', 'NOTE', 'HIDE_CONTENT', 'RESTORE_CONTENT', 'RESTRICT_USER', 'DISABLE_USER', 'RESTORE_USER', 'RESOLVE', 'DISMISS')),
    CONSTRAINT `chk_report_action_reason` CHECK (CHAR_LENGTH(TRIM(`reason`)) > 0),
    CONSTRAINT `fk_report_action_report` FOREIGN KEY (`report_id`) REFERENCES `report` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_report_action_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recipient_id` BIGINT UNSIGNED NOT NULL,
    `actor_id` BIGINT UNSIGNED NULL,
    `event_type` VARCHAR(40) NOT NULL,
    `event_key` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    `target_type` VARCHAR(20) NOT NULL,
    `recruitment_id` BIGINT UNSIGNED NULL,
    `application_id` BIGINT UNSIGNED NULL,
    `leader_transfer_id` BIGINT UNSIGNED NULL,
    `report_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` VARCHAR(1000) NOT NULL,
    `read_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event` (`recipient_id`, `event_key`),
    KEY `idx_notification_recipient_created` (`recipient_id`, `created_at`, `id`),
    KEY `idx_notification_recipient_read` (`recipient_id`, `read_at`, `created_at`, `id`),
    CONSTRAINT `chk_notification_event_key` CHECK (CHAR_LENGTH(TRIM(`event_key`)) > 0),
    CONSTRAINT `chk_notification_event_type` CHECK (`event_type` IN
        ('APPLICATION_SUBMITTED', 'APPLICATION_ACCEPTED', 'APPLICATION_REJECTED', 'APPLICATION_WITHDRAWN', 'APPLICATION_EXPIRED',
         'RECRUITMENT_TEAMED', 'RECRUITMENT_CLOSED', 'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_REMOVED',
         'LEADER_TRANSFER_REQUESTED', 'LEADER_TRANSFER_ACCEPTED', 'LEADER_TRANSFER_REJECTED', 'LEADER_TRANSFER_CANCELLED', 'LEADER_TRANSFER_EXPIRED',
         'REPORT_RESOLVED', 'REPORT_DISMISSED')),
    CONSTRAINT `chk_notification_target_type` CHECK (`target_type` IN ('RECRUITMENT', 'APPLICATION', 'LEADER_TRANSFER', 'REPORT')),
    CONSTRAINT `chk_notification_target` CHECK (
        (`target_type` = 'RECRUITMENT' AND `recruitment_id` IS NOT NULL AND `application_id` IS NULL AND `leader_transfer_id` IS NULL AND `report_id` IS NULL) OR
        (`target_type` = 'APPLICATION' AND `recruitment_id` IS NULL AND `application_id` IS NOT NULL AND `leader_transfer_id` IS NULL AND `report_id` IS NULL) OR
        (`target_type` = 'LEADER_TRANSFER' AND `recruitment_id` IS NULL AND `application_id` IS NULL AND `leader_transfer_id` IS NOT NULL AND `report_id` IS NULL) OR
        (`target_type` = 'REPORT' AND `recruitment_id` IS NULL AND `application_id` IS NULL AND `leader_transfer_id` IS NULL AND `report_id` IS NOT NULL)),
    CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_notification_actor` FOREIGN KEY (`actor_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_notification_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_notification_application` FOREIGN KEY (`application_id`) REFERENCES `application` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_notification_transfer` FOREIGN KEY (`leader_transfer_id`) REFERENCES `leader_transfer` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_notification_report` FOREIGN KEY (`report_id`) REFERENCES `report` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO `category` (`id`, `name`, `sort_order`) VALUES
    (1, '课堂组队', 1),
    (2, '学科竞赛', 2),
    (3, '校园活动', 3),
    (4, '其他', 4);

INSERT INTO `tag` (`id`, `name`) VALUES
    (1, 'Java'),
    (2, 'Vue'),
    (3, '数据分析'),
    (4, '文案整理');

-- Demo users use the BCrypt hash for the password "password".
INSERT INTO `user` (`id`, `username`, `password_hash`, `display_name`) VALUES
    (1, 'alice', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice'),
    (2, 'bob', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob'),
    (3, 'carol', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carol');

INSERT INTO `user_profile` (`user_id`, `department`, `grade`, `bio`) VALUES
    (1, '计算机学院', '2025级', '擅长前端开发'),
    (2, '软件学院', '2025级', '擅长 Java 开发'),
    (3, '数学学院', '2024级', '擅长数据分析');

INSERT INTO `user_tag` (`user_id`, `tag_id`) VALUES
    (1, 2), (2, 1), (3, 3);

INSERT INTO `recruitment` (`id`, `owner_id`, `category_id`, `title`, `course_name`, `teacher_name`, `purpose`, `description`, `requirements`, `max_members`, `deadline_at`, `status`) VALUES
    (1, 1, 1, '寻找 Java 课程汇报队友', 'Java 程序设计', '张老师', '完成课程汇报', '已有一名负责人，希望寻找 Java 和文档方向的队友。', '愿意参与讨论并按时完成分工。', 3, DATE_ADD(NOW(), INTERVAL 14 DAY), 'RECRUITING'),
    (2, 2, 2, '数学建模竞赛组队', NULL, NULL, '参加本学期数学建模竞赛', '寻找擅长数据分析和论文整理的同学。', NULL, 4, DATE_ADD(NOW(), INTERVAL 7 DAY), 'DRAFT');

INSERT INTO `recruitment_tag` (`recruitment_id`, `tag_id`) VALUES
    (1, 1), (1, 4), (2, 3);

INSERT INTO `team_member` (`recruitment_id`, `user_id`, `role`, `status`) VALUES
    (1, 1, 'LEADER', 'ACTIVE'),
    (2, 2, 'LEADER', 'ACTIVE');

COMMIT;
