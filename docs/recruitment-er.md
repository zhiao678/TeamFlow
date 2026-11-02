# 招募平台 ER 设计

## 1. 核心实体关系

```mermaid
erDiagram
    USER ||--|| USER_PROFILE : has
    USER ||--o{ RECRUITMENT : creates
    CATEGORY ||--o{ RECRUITMENT : classifies
    USER ||--o{ USER_TAG : owns
    TAG ||--o{ USER_TAG : contains
    RECRUITMENT ||--o{ RECRUITMENT_TAG : requires
    TAG ||--o{ RECRUITMENT_TAG : describes
    RECRUITMENT ||--o{ APPLICATION : receives
    USER ||--o{ APPLICATION : submits
    RECRUITMENT ||--o{ TEAM_MEMBER : contains
    USER ||--o{ TEAM_MEMBER : joins

    USER {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar display_name
        varchar avatar_url
        datetime created_at
        datetime updated_at
    }

    USER_PROFILE {
        bigint user_id PK, FK
        varchar department
        varchar grade
        text bio
        boolean contact_allowed
        datetime created_at
        datetime updated_at
    }

    CATEGORY {
        bigint id PK
        varchar name UK
        bigint parent_id FK
        boolean enabled
        int sort_order
        datetime created_at
        datetime updated_at
    }

    TAG {
        bigint id PK
        varchar name UK
        boolean enabled
        datetime created_at
    }

    USER_TAG {
        bigint user_id PK, FK
        bigint tag_id PK, FK
    }

    RECRUITMENT {
        bigint id PK
        bigint owner_id FK
        bigint category_id FK
        varchar title
        varchar course_name
        varchar teacher_name
        text purpose
        text requirements
        int max_members
        datetime deadline_at
        varchar status
        datetime created_at
        datetime updated_at
    }

    RECRUITMENT_TAG {
        bigint recruitment_id PK, FK
        bigint tag_id PK, FK
    }

    APPLICATION {
        bigint id PK
        bigint recruitment_id FK
        bigint applicant_id FK
        text introduction
        text skills_description
        varchar status
        datetime created_at
        datetime processed_at
    }

    TEAM_MEMBER {
        bigint id PK
        bigint recruitment_id FK
        bigint user_id FK
        varchar role
        varchar status
        datetime joined_at
        datetime left_at
    }
```

## 2. 表职责
表	用途
user	登录账号和基础身份
user_profile	院系、年级、简介等公开资料
category	课堂组队、学科竞赛等招募分类
tag	技能标签
user_tag	用户与技能标签的多对多关系
recruitment	一条具体的组队招募
recruitment_tag	招募与技能标签的多对多关系
application	用户提交的加入申请
team_member	已确认加入招募的正式成员

## 2. 关键规则
- recruitment.owner_id 是招募负责人。
- 创建招募时，负责人同时写入 team_member，角色为 LEADER。
- application 只表示申请，不代表正式入队。
- 只有申请被接受后，才创建 team_member 记录。
- application 的状态包括：
  PENDING、ACCEPTED、REJECTED、WITHDRAWN、EXPIRED。
- team_member 的角色包括：
  LEADER、MEMBER。
- team_member 的状态包括：
  ACTIVE、LEFT、REMOVED。
- 同一用户对同一招募最多只能有一条 PENDING 申请。
- 同一用户在同一招募中最多只能有一条有效成员关系。
- 招募的有效成员数量不能超过 max_members。
- 负责人计入正式成员数量。
- 站外成员只作为文本介绍，不创建 team_member。