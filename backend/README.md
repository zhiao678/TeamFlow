# 后端说明

## 技术栈

后端基于 Spring Boot 3 开发。

主要技术栈：Java 17、Spring Boot 3.2.12、Maven、MySQL 8、MyBatis-Plus、Lombok 和 Jakarta Validation。Knife4j 将在任务接口阶段接入。

## 环境要求

- JDK 17
- MySQL 8.0
- 使用项目自带的 Maven Wrapper，无需单独安装 Maven

检查 Java 版本：

```powershell
java -version
```

输出应为 Java 17。

## 首次启动

在 MySQL 客户端中执行仓库根目录下的初始化脚本：

```sql
SOURCE C:/path/to/TeamFlow/database/init.sql;
```

该脚本会创建 `teamflow` 数据库、四张基础表和演示数据。重复执行会删除并重建这四张表，请勿用于需要保留数据的数据库。

进入后端目录，并设置当前终端使用的数据库账号：

```powershell
cd TeamFlow/backend
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的MySQL密码'
```

运行测试：

```powershell
.\mvnw.cmd clean test
```

启动后端：

```powershell
.\mvnw.cmd spring-boot:run
```

默认服务地址：`http://localhost:8080`。

数据库连接地址也可以通过 `DB_URL` 覆盖，默认值为 `jdbc:mysql://localhost:3306/teamflow`。数据库密码不得写入配置文件或提交到仓库。

## 数据库

初始化脚本位于 `database/init.sql`，包含以下数据表：

| 表 | 用途 |
|---|---|
| `user` | 用户基础信息 |
| `project` | 项目信息和创建者 |
| `project_member` | 项目成员及项目内角色 |
| `task` | 项目任务及个人任务预留字段 |

脚本提供 `alice`、`bob`、`carol` 三个虚构演示用户，以及演示项目、成员关系和任务数据。

## 常用命令

以下命令均在 `backend/` 目录下执行：

| 命令 | 用途 |
|---|---|
| `.\mvnw.cmd clean test` | 清理项目并运行测试 |
| `.\mvnw.cmd spring-boot:run` | 启动后端开发服务 |
| `.\mvnw.cmd clean package` | 编译、测试并打包 |
| `.\mvnw.cmd clean package -DskipTests` | 跳过测试并打包 |

## 代码目录约定

Java 基础包为 `com.teamflow.backend`，业务代码按以下目录组织：

| 目录 | 内容 |
|---|---|
| `controller` | HTTP 接口和请求参数接收 |
| `service` | 业务逻辑接口 |
| `service/impl` | 业务逻辑实现 |
| `mapper` | MyBatis-Plus 数据访问接口 |
| `entity` | 数据库实体类 |
| `dto` | 接收请求的数据对象 |
| `vo` | 返回给前端的数据对象 |
| `common` | 统一返回、异常、枚举和公共配置 |

## 接口约定

所有业务接口统一使用 `/api` 前缀。

完整的前后端接口契约见 [API 规范](../docs/api-spec.md)。

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

当前已实现的项目接口：

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/projects` | 查询项目列表 |
| `GET` | `/api/projects/{id}` | 查询项目详情 |
| `POST` | `/api/projects` | 创建项目 |

创建项目请求示例：

```json
{
  "name": "TeamFlow",
  "description": "团队协作系统",
  "ownerId": 1
}
```

创建项目时，后端会在同一事务中将创建者写入 `project_member`，角色设置为 `ADMIN`。当前尚未接入登录，因此暂时由请求传递 `ownerId`。

## 当前开发边界

现在不实现：

- 登录接口。
- 密码校验。
- Token。
- 系统管理员。
- 个人任务接口。
- 用户禁用。
- 评论和消息。
