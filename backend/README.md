# TeamFlow 后端说明

## 技术栈

- Java 17
- Spring Boot 3.2.12
- Maven Wrapper
- MySQL 8、H2（自动化测试）
- MyBatis-Plus
- Spring Security、JWT、BCrypt
- Knife4j / OpenAPI

## 环境要求

- JDK 17
- MySQL 8.0.16 及以上
- 无需单独安装 Maven，使用项目自带的 Maven Wrapper

检查 Java 与 Maven 使用的运行时：

```powershell
java -version
cd backend
.\mvnw.cmd -version
```

两处都必须显示 Java 17。`pom.xml` 中的 `java.version` 不会自动切换终端的 JDK。

## 初始化招募平台数据库

在 MySQL 客户端中执行仓库根目录下的脚本：

```sql
SOURCE C:/path/to/TeamFlow/database/recruitment-init.sql;
```

脚本会创建并使用 `teamflow_recruitment` 数据库，包含用户、资料、分类、标签、招募、申请、成员、退出审核和后续数据基础共 19 张表及演示数据。完整字段、约束和事务设计见 ER 文档。

已有招募库本周使用增量升级脚本，在 MySQL 客户端先选择目标数据库再执行。

```sql
USE teamflow_recruitment;
SOURCE C:/path/to/TeamFlow/database/migrations/20261112-application-member-exit.sql;
```

完整初始化脚本 `database/recruitment-init.sql` 会删除并重建数据库中的同名开发表，用于可重置开发库。上述增量脚本保留已有数据。原项目的 `database/init.sql` 使用 `teamflow` 数据库。

## 启动新版招募平台后端

在 PowerShell 中进入 `backend` 目录并设置当前终端环境变量：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的 MySQL 密码'
$env:JWT_SECRET = '至少32字节的本地开发密钥，请勿提交到仓库'
$env:SPRING_PROFILES_ACTIVE = 'recruitment'
```

如 JDK 17 安装位置不同，请修改 `JAVA_HOME`。启动服务：

```powershell
.\mvnw.cmd spring-boot:run
```

默认地址：

- 后端服务：`http://localhost:8080`
- Knife4j：`http://localhost:8080/doc.html`

新版数据库连接可通过 `RECRUITMENT_DB_URL` 覆盖，默认值为：

```text
jdbc:mysql://localhost:3306/teamflow_recruitment
```

数据库密码和 JWT 密钥不得写入配置文件或提交到仓库。

## 运行测试

自动化测试使用 H2 内存数据库，不会修改本地 MySQL：

```powershell
.\mvnw.cmd clean test
```

常用命令：

| 命令 | 用途 |
|---|---|
| `.\mvnw.cmd clean test` | 清理并运行全部测试 |
| `.\mvnw.cmd spring-boot:run` | 启动开发服务 |
| `.\mvnw.cmd clean package` | 编译、测试并打包 |
| `.\mvnw.cmd clean package -DskipTests` | 跳过测试并打包 |

## 用户认证与资料接口

| 方法 | 路径 | 登录要求 | 用途 |
|---|---|---|---|
| `POST` | `/api/auth/register` | 否 | 注册并返回 JWT |
| `POST` | `/api/auth/login` | 否 | 登录并返回 JWT |
| `POST` | `/api/auth/logout` | 是 | 结束客户端登录会话 |
| `GET` | `/api/users/me` | 是 | 查询当前用户资料 |
| `PATCH` | `/api/users/me` | 是 | 修改资料和技能标签 |

需要登录的接口携带：

```text
Authorization: Bearer <token>
```

首版使用无状态 JWT。退出成功后由前端删除本地 Token；后端暂不维护 Token 黑名单，因此旧 Token 在到期前不会被服务端主动撤销。

用户平台角色：`USER / ADMIN`。账号状态：`ACTIVE / RESTRICTED / DISABLED`。

## 招募接口

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/recruitments` | 分页、关键词、分类、课程、教师、技能、缺额筛选及最新、截止、热门排序 |
| `GET` | `/api/recruitments/{id}` | 详情，草稿仅负责人可读 |
| `GET` | `/api/recruitments/mine` | 当前负责人全部状态招募 |
| `GET` | `/api/recruitments/favorites` | 当前用户收藏的招募 |
| `POST` | `/api/recruitments` | 创建草稿或直接发布 |
| `PATCH` | `/api/recruitments/{id}` | 按状态编辑允许字段 |
| `POST` | `/api/recruitments/{id}/publish` | 发布草稿 |
| `POST` | `/api/recruitments/{id}/close` | 关闭招募中记录 |
| `DELETE` | `/api/recruitments/{id}` | 删除草稿 |
| `POST/DELETE` | `/api/recruitments/{id}/favorite` | 收藏或取消收藏 |
| `GET` | `/api/categories`、`/api/tags` | 查询启用分类和技能标签 |
| `GET` | `/api/notifications` | 分页查询当前用户通知和未读数 |
| `PATCH` | `/api/notifications/{id}/read` | 标记单条通知已读 |
| `PATCH` | `/api/notifications/read-all` | 标记全部通知已读 |

大厅返回 `data.records/page/pageSize/total`，我的招募返回数组。所有管理操作均检查 JWT 和负责人权限。参数与返回示例见招募 API 规范。

## 申请与成员

支持入队申请提交、招募负责人查询、我的申请、撤回、接受和拒绝，以及正式成员查询和负责人移除。申请使用 JWT 身份，待处理申请不占名额，接受时锁定招募并再次核对状态与剩余人数，业务数据和通知共同提交。

普通成员退出需要提交申请并由负责人审核，批准前保持正式成员资格。负责人退出先选定本队普通成员，由继任者确认后原子转交并离队，交接通过简短提示沟通。列表、审批及转交接口见 API 规范第 6 节。

### 本周验证与联调

2026-11-12 本地完整验证版本共 89 项测试通过，失败、错误和跳过均为 0。新增测试覆盖申请权限、重复申请、截止和满员、最后名额并发、成员退出审核、继任确认、通知幂等与通知失败时的整体回滚。历史普通负责人转交记录也已验证，确认后原负责人继续作为普通成员留队。本次提交的验证范围和前端待联调事项见交接说明。

真实 MySQL 8.0.18 验证通过。最后名额、重复批准退出、重复确认继任的并发请求均仅一次成功。完整初始化生成 19 张表，从上一版结构执行增量升级两次后保留原有用户、招募和成员数据。

前端需接入入队申请、负责人审核、退出申请及继任确认页面，核对新增通知目标 leaderTransferId 与 handoverHint 提示。我的招募沿用现任负责人查询口径，独立的创建历史展示待小组确认。

## 统一响应

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

认证和权限失败也使用相同 JSON 结构：

```json
{
  "code": 40100,
  "message": "unauthorized",
  "data": null
}
```

常用业务码：`40000 / 40100 / 40300 / 40400 / 40900 / 50000`。

## 代码目录

Java 基础包为 `com.teamflow.backend`：

| 目录 | 内容 |
|---|---|
| `controller` | HTTP 接口 |
| `service`、`service/impl` | 业务接口与实现 |
| `mapper` | MyBatis-Plus 数据访问 |
| `entity` | 数据库实体 |
| `dto` | 请求对象 |
| `vo` | 响应对象 |
| `common` | 认证、统一响应、异常、枚举和配置 |

## 文档与兼容边界

- 新版接口规范：[recruitment-api-spec.md](../docs/recruitment-api-spec.md)
- 新版业务规则：[recruitment-decisions.md](../docs/recruitment-decisions.md)
- 新版 ER 设计：[recruitment-er.md](../docs/recruitment-er.md)
- [申请与成员模块交接说明](../docs/handoff-20261112-applications-members.md)
- 旧项目/任务接口规范：[api-spec.md](../docs/api-spec.md)

旧项目、任务代码和 `database/init.sql` 暂时保留，避免破坏已有演示与测试。新版功能使用 `teamflow_recruitment` 数据库和 `recruitment` Spring Profile，后续按阶段逐步替换旧业务。
