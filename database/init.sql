-- TeamFlow development database initialization script.
-- WARNING: Re-running this script drops and recreates the four tables below.
-- Do not run it against a database containing data that must be preserved.

CREATE DATABASE IF NOT EXISTS `teamflow`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `teamflow`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `project_member`;
DROP TABLE IF EXISTS `project`;
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
    CONSTRAINT `uk_user_username` UNIQUE (`username`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `project` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `owner_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_owner_id` (`owner_id`),
    KEY `idx_project_status` (`status`),
    CONSTRAINT `chk_project_status`
        CHECK (`status` IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT `fk_project_owner`
        FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `project_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `project_role` VARCHAR(20) NOT NULL,
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_project_member_project_user` UNIQUE (`project_id`, `user_id`),
    KEY `idx_project_member_user_id` (`user_id`),
    CONSTRAINT `chk_project_member_role`
        CHECK (`project_role` IN ('ADMIN', 'MEMBER')),
    CONSTRAINT `fk_project_member_project`
        FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_project_member_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT UNSIGNED NULL,
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `assignee_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'TODO',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    `due_at` DATETIME NULL,
    `completed_at` DATETIME NULL,
    `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_project_status_deleted` (`project_id`, `status`, `deleted`),
    KEY `idx_task_creator_id` (`creator_id`),
    KEY `idx_task_assignee_id` (`assignee_id`),
    KEY `idx_task_due_at` (`due_at`),
    CONSTRAINT `chk_task_status`
        CHECK (`status` IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT `chk_task_priority`
        CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT `chk_task_deleted`
        CHECK (`deleted` IN (0, 1)),
    CONSTRAINT `fk_task_project`
        FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_task_creator`
        FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_task_assignee`
        FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

START TRANSACTION;

-- All demo users currently use the BCrypt hash for the password "password".
INSERT INTO `user` (`id`, `username`, `password_hash`, `display_name`, `avatar_url`)
VALUES
    (1, 'alice', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice', NULL),
    (2, 'bob', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob', NULL),
    (3, 'carol', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carol', NULL);

INSERT INTO `project` (`id`, `name`, `description`, `owner_id`, `status`)
VALUES
    (1, 'TeamFlow', 'Team collaboration and task management system.', 1, 'ACTIVE'),
    (2, 'Website Redesign', 'Archived website redesign project.', 2, 'ARCHIVED');

INSERT INTO `project_member` (`id`, `project_id`, `user_id`, `project_role`)
VALUES
    (1, 1, 1, 'ADMIN'),
    (2, 1, 2, 'MEMBER'),
    (3, 1, 3, 'MEMBER'),
    (4, 2, 2, 'ADMIN'),
    (5, 2, 1, 'MEMBER');

INSERT INTO `task` (
    `id`,
    `project_id`,
    `creator_id`,
    `assignee_id`,
    `title`,
    `description`,
    `status`,
    `priority`,
    `due_at`,
    `completed_at`
)
VALUES
    (
        1,
        1,
        1,
        2,
        'Implement project APIs',
        'Implement the project list, detail, and creation endpoints.',
        'TODO',
        'HIGH',
        DATE_ADD(NOW(), INTERVAL 3 DAY),
        NULL
    ),
    (
        2,
        1,
        2,
        3,
        'Integrate the task page',
        'Connect the frontend task page to the backend API.',
        'IN_PROGRESS',
        'MEDIUM',
        DATE_ADD(NOW(), INTERVAL 7 DAY),
        NULL
    ),
    (
        3,
        1,
        1,
        1,
        'Design the database schema',
        'Create the initial user, project, project member, and task tables.',
        'DONE',
        'HIGH',
        NOW(),
        DATE_SUB(NOW(), INTERVAL 1 DAY)
    ),
    (
        4,
        1,
        3,
        NULL,
        'Prepare API test data',
        'Prepare data for Swagger and frontend integration testing.',
        'TODO',
        'LOW',
        NULL,
        NULL
    ),
    (
        5,
        2,
        2,
        1,
        'Write project requirements',
        'Document the archived website redesign requirements.',
        'DONE',
        'MEDIUM',
        DATE_SUB(NOW(), INTERVAL 5 DAY),
        DATE_SUB(NOW(), INTERVAL 6 DAY)
    );

COMMIT;
