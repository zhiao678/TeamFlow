-- Recruitment platform development database.
-- This script uses a separate database and does not modify the legacy TeamFlow schema.

CREATE DATABASE IF NOT EXISTS `teamflow_recruitment`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `teamflow_recruitment`;

SET FOREIGN_KEY_CHECKS = 0;
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
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_recruitment_user_username` UNIQUE (`username`)
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
    PRIMARY KEY (`id`),
    KEY `idx_application_recruitment_status` (`recruitment_id`, `status`),
    KEY `idx_application_recruitment_applicant` (`recruitment_id`, `applicant_id`),
    CONSTRAINT `chk_application_status` CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT `fk_application_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_application_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
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
    PRIMARY KEY (`id`),
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_member_recruitment_user` (`recruitment_id`, `user_id`),
    KEY `idx_team_member_recruitment_status` (`recruitment_id`, `status`),
    CONSTRAINT `chk_team_member_role` CHECK (`role` IN ('LEADER', 'MEMBER')),
    CONSTRAINT `chk_team_member_status` CHECK (`status` IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT `fk_team_member_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_team_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
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
