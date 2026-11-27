CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    platform_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (platform_role IN ('USER', 'ADMIN')),
    CHECK (account_status IN ('ACTIVE', 'RESTRICTED', 'DISABLED'))
);

CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY,
    department VARCHAR(100),
    grade VARCHAR(30),
    bio VARCHAR(500),
    contact_allowed INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT,
    enabled INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    enabled INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_tag (
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, tag_id)
);

CREATE TABLE recruitment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    course_name VARCHAR(100),
    teacher_name VARCHAR(100),
    purpose VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    requirements VARCHAR(2000),
    max_members INT NOT NULL,
    deadline_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    close_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recruitment_tag (
    recruitment_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (recruitment_id, tag_id)
);

CREATE TABLE application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    rejection_reason VARCHAR(255),
    introduction VARCHAR(1000),
    skills_description VARCHAR(1000),
    available_time VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE TABLE team_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    UNIQUE (recruitment_id, user_id)
);

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id BIGINT NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(50000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruitment_id) REFERENCES recruitment(id),
    FOREIGN KEY (owner_id) REFERENCES `user`(id)
);

CREATE TABLE project_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(20) NOT NULL CHECK (project_role IN ('ADMIN', 'MEMBER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    UNIQUE (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES project(id),
    FOREIGN KEY (user_id) REFERENCES `user`(id),
    CHECK ((status = 'ACTIVE' AND left_at IS NULL) OR (status <> 'ACTIVE' AND left_at IS NOT NULL))
);

CREATE TABLE recruitment_favorite (
    user_id BIGINT NOT NULL,
    recruitment_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, recruitment_id),
    FOREIGN KEY (user_id) REFERENCES `user` (id),
    FOREIGN KEY (recruitment_id) REFERENCES recruitment (id)
);

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT,
    event_type VARCHAR(40) NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    recruitment_id BIGINT,
    application_id BIGINT,
    leader_transfer_id BIGINT,
    report_id BIGINT,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (CHAR_LENGTH(TRIM(event_key)) > 0),
    CHECK (event_type IN ('APPLICATION_SUBMITTED', 'APPLICATION_ACCEPTED', 'APPLICATION_REJECTED', 'APPLICATION_WITHDRAWN', 'APPLICATION_EXPIRED', 'RECRUITMENT_TEAMED', 'RECRUITMENT_CLOSED', 'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_REMOVED', 'MEMBER_EXIT_REQUESTED', 'MEMBER_EXIT_APPROVED', 'MEMBER_EXIT_REJECTED', 'LEADER_TRANSFER_REQUESTED', 'LEADER_TRANSFER_ACCEPTED', 'LEADER_TRANSFER_REJECTED', 'LEADER_TRANSFER_CANCELLED', 'LEADER_TRANSFER_EXPIRED', 'REPORT_RESOLVED', 'REPORT_DISMISSED')),
    CHECK (target_type IN ('RECRUITMENT', 'APPLICATION', 'LEADER_TRANSFER', 'REPORT')),
    CHECK (
        (target_type = 'RECRUITMENT' AND recruitment_id IS NOT NULL AND application_id IS NULL AND leader_transfer_id IS NULL AND report_id IS NULL) OR
        (target_type = 'APPLICATION' AND recruitment_id IS NULL AND application_id IS NOT NULL AND leader_transfer_id IS NULL AND report_id IS NULL) OR
        (target_type = 'LEADER_TRANSFER' AND recruitment_id IS NULL AND application_id IS NULL AND leader_transfer_id IS NOT NULL AND report_id IS NULL) OR
        (target_type = 'REPORT' AND recruitment_id IS NULL AND application_id IS NULL AND leader_transfer_id IS NULL AND report_id IS NOT NULL)
    ),
    UNIQUE (recipient_id, event_key),
    FOREIGN KEY (recipient_id) REFERENCES `user` (id),
    FOREIGN KEY (actor_id) REFERENCES `user` (id),
    FOREIGN KEY (recruitment_id) REFERENCES recruitment (id),
    FOREIGN KEY (application_id) REFERENCES application (id)
);

CREATE TABLE conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    recruitment_id BIGINT,
    direct_user_low_id BIGINT,
    direct_user_high_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (recruitment_id),
    UNIQUE (direct_user_low_id, direct_user_high_id),
    CHECK (type IN ('DIRECT', 'TEAM')),
    CHECK (status IN ('ACTIVE', 'READ_ONLY')),
    CHECK ((type = 'TEAM' AND recruitment_id IS NOT NULL AND direct_user_low_id IS NULL AND direct_user_high_id IS NULL)
        OR (type = 'DIRECT' AND recruitment_id IS NULL AND direct_user_low_id IS NOT NULL
            AND direct_user_high_id IS NOT NULL AND direct_user_low_id < direct_user_high_id)),
    FOREIGN KEY (recruitment_id) REFERENCES recruitment (id),
    FOREIGN KEY (direct_user_low_id) REFERENCES `user` (id),
    FOREIGN KEY (direct_user_high_id) REFERENCES `user` (id)
);

CREATE TABLE conversation_member (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    last_read_message_id BIGINT,
    PRIMARY KEY (conversation_id, user_id),
    CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CHECK ((status = 'ACTIVE' AND left_at IS NULL) OR (status <> 'ACTIVE' AND left_at IS NOT NULL)),
    FOREIGN KEY (conversation_id) REFERENCES conversation (id),
    FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    client_message_id VARCHAR(64) NOT NULL,
    source_recruitment_id BIGINT,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (conversation_id, id),
    UNIQUE (conversation_id, sender_id, client_message_id),
    CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CHECK (CHAR_LENGTH(TRIM(client_message_id)) > 0),
    CHECK (status IN ('VISIBLE', 'HIDDEN')),
    FOREIGN KEY (conversation_id, sender_id) REFERENCES conversation_member (conversation_id, user_id),
    FOREIGN KEY (source_recruitment_id) REFERENCES recruitment (id)
);

ALTER TABLE conversation_member
    ADD CONSTRAINT fk_conversation_member_read_message
    FOREIGN KEY (conversation_id, last_read_message_id) REFERENCES message (conversation_id, id);

CREATE TABLE user_block (
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES `user` (id),
    FOREIGN KEY (blocked_id) REFERENCES `user` (id)
);


CREATE TABLE member_exit_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255),
    rejection_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    pending_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN user_id ELSE NULL END),
    UNIQUE (recruitment_id, pending_user_id),
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    CHECK ((status = 'PENDING' AND processed_at IS NULL) OR (status <> 'PENDING' AND processed_at IS NOT NULL)),
    FOREIGN KEY (recruitment_id, user_id) REFERENCES team_member (recruitment_id, user_id)
);

CREATE TABLE leader_transfer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    exit_after_transfer TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    pending_recruitment_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN recruitment_id ELSE NULL END),
    UNIQUE (pending_recruitment_id),
    CHECK (from_user_id <> to_user_id),
    CHECK (exit_after_transfer IN (0, 1)),
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    CHECK ((status = 'PENDING' AND processed_at IS NULL) OR (status <> 'PENDING' AND processed_at IS NOT NULL)),
    FOREIGN KEY (recruitment_id) REFERENCES recruitment (id),
    FOREIGN KEY (recruitment_id, from_user_id) REFERENCES team_member (recruitment_id, user_id),
    FOREIGN KEY (recruitment_id, to_user_id) REFERENCES team_member (recruitment_id, user_id)
);
