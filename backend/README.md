# 后端说明

## 技术栈

后端基于 Spring Boot 3 开发。

主要技术栈：Java 17、Spring Boot 3.2.12、Maven、MySQL 8、MyBatis-Plus、Knife4j、Lombok 和 Jakarta Validation。

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

进入后端目录：

```powershell
cd TeamFlow/backend
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

数据库初始化方式、环境变量和 Knife4j 文档地址将在相关配置完成后补充。
