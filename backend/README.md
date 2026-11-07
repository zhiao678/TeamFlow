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
- MySQL 8.0
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

脚本会创建并使用 `teamflow_recruitment` 数据库，包含用户、资料、分类、标签、招募、申请和成员等基础表及演示数据。

脚本会删除并重建该数据库中的同名开发表，不得用于需要保留真实数据的环境。原项目的 `database/init.sql` 和 `teamflow` 数据库暂时保留，两套脚本互不覆盖。

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
- 旧项目/任务接口规范：[api-spec.md](../docs/api-spec.md)

旧项目、任务代码和 `database/init.sql` 暂时保留，避免破坏已有演示与测试。新版功能使用 `teamflow_recruitment` 数据库和 `recruitment` Spring Profile，后续按阶段逐步替换旧业务。
