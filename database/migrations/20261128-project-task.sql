-- Apply to the selected recruitment database after 20261127-project-foundation.sql.
-- MySQL 8.0.16+ is required for enforced CHECK constraints.
-- Repeated execution preserves existing project tasks.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `project_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT UNSIGNED NOT NULL,
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `assignee_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(150) NOT NULL,
    `description` VARCHAR(2000) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'TODO',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    `due_at` DATETIME NULL,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_task_project_created` (`project_id`, `created_at`, `id`),
    KEY `idx_project_task_project_status` (`project_id`, `status`, `created_at`, `id`),
    KEY `idx_project_task_project_assignee` (`project_id`, `assignee_id`, `created_at`, `id`),
    KEY `idx_project_task_project_priority` (`project_id`, `priority`, `created_at`, `id`),
    KEY `idx_project_task_project_due` (`project_id`, `due_at`, `id`),
    KEY `idx_project_task_project_creator` (`project_id`, `creator_id`),
    CONSTRAINT `chk_project_task_title` CHECK (CHAR_LENGTH(TRIM(`title`)) > 0),
    CONSTRAINT `chk_project_task_status` CHECK (`status` IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT `chk_project_task_priority` CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT `chk_project_task_completed_at` CHECK (
        (`status` = 'DONE' AND `completed_at` IS NOT NULL) OR
        (`status` <> 'DONE' AND `completed_at` IS NULL)),
    CONSTRAINT `fk_project_task_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_project_task_creator_member` FOREIGN KEY (`project_id`, `creator_id`)
        REFERENCES `project_member` (`project_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_project_task_assignee_member` FOREIGN KEY (`project_id`, `assignee_id`)
        REFERENCES `project_member` (`project_id`, `user_id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
