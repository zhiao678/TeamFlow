-- Apply after selecting the existing teamflow_recruitment database.
-- Creates the project collaboration foundation without migrating the old teamflow database.

CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id` BIGINT UNSIGNED NOT NULL,
    `owner_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `description` MEDIUMTEXT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_recruitment` (`recruitment_id`),
    KEY `idx_project_owner_status` (`owner_id`, `status`),
    CONSTRAINT `chk_project_status` CHECK (`status` IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT `fk_project_recruitment` FOREIGN KEY (`recruitment_id`) REFERENCES `recruitment` (`id`),
    CONSTRAINT `fk_project_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `project_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `project_role` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `left_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_member_project_user` (`project_id`, `user_id`),
    KEY `idx_project_member_user_status` (`user_id`, `status`),
    CONSTRAINT `chk_project_member_role` CHECK (`project_role` IN ('ADMIN', 'MEMBER')),
    CONSTRAINT `chk_project_member_status` CHECK (`status` IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT `chk_project_member_left_at` CHECK (
        (`status` = 'ACTIVE' AND `left_at` IS NULL) OR (`status` <> 'ACTIVE' AND `left_at` IS NOT NULL)),
    CONSTRAINT `fk_project_member_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_project_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
