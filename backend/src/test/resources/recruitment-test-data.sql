INSERT INTO category (id, name, enabled, sort_order) VALUES
    (1, '课堂组队', 1, 1),
    (2, '学科竞赛', 1, 2);

INSERT INTO tag (id, name, enabled) VALUES
    (1, 'Java', 1),
    (2, 'Vue', 1);

INSERT INTO `user` (id, username, password_hash, display_name) VALUES
    (1, 'alice', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice');

INSERT INTO user_profile (user_id, department, grade, bio) VALUES
    (1, '计算机学院', '2025级', '后端开发');
