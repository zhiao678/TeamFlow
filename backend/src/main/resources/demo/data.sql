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
        DATEADD('DAY', 3, CURRENT_TIMESTAMP),
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
        DATEADD('DAY', 7, CURRENT_TIMESTAMP),
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
        DATEADD('DAY', -1, CURRENT_TIMESTAMP)
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
        DATEADD('DAY', -5, CURRENT_TIMESTAMP),
        DATEADD('DAY', -6, CURRENT_TIMESTAMP)
    );
