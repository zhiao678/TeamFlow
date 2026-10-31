CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
