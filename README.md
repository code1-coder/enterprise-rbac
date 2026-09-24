# 企业权限角色分配系统

个人作品集项目。一个面向中后台的 RBAC 权限服务：用户、角色、菜单和按钮权限，JWT 登录，Redis 会话控制，以及基于权限标识的接口鉴权。

当前可运行的是 Spring Boot 后端。前端目录已预留，管理端界面还没开始。

## 仓库结构

| 目录 | 说明 |
| --- | --- |
| [enterprise-rbac-backend](enterprise-rbac-backend) | Spring Boot 3.2.5 后端，含数据库脚本和接口文档 |
| [enterprise-rbac-frontend](enterprise-rbac-frontend) | 管理端前端，尚未初始化 |

后端怎么启动、默认账号和表结构，见 [enterprise-rbac-backend/README.md](enterprise-rbac-backend/README.md)。

## 技术栈

- Java 17，Spring Boot 3.2.5，Spring Security
- MyBatis-Plus 3.5.7，MySQL 8，Druid
- JWT（jjwt 0.12.6）+ Redis
- Knife4j / OpenAPI 3
- Lombok、Hutool、Fastjson2

版本以 [pom.xml](enterprise-rbac-backend/pom.xml) 为准。

## 已实现

- 登录、注册、退出、刷新 Token、当前用户
- 用户管理接口：分页查询、详情、创建、更新、删除、批量删除、本人改密、管理员重置密码、分配角色
- 角色管理接口：分页查询、下拉列表、详情、创建、更新、删除、分配菜单权限
- JWT 过滤器、Redis 登录态和退出黑名单
- `@PreAuthorize` 权限标识，例如 `system:user:delete`
- 初始化脚本：管理员、三种角色、菜单和权限

菜单权限接口已实现，包括管理菜单树、详情、创建、更新、删除，以及当前用户菜单和按钮权限查询。

本地演示账号是 `admin` / `admin123`。这是脚本里的种子数据，不是生产密码。

## 本地启动

需要 JDK 17+、Maven 3.6+、MySQL 8 和本机 Redis。

```powershell
cd enterprise-rbac-backend
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < src\main\resources\db\schema.sql"
mvn spring-boot:run
```

- 首页：http://localhost:8080/
- 接口文档：http://localhost:8080/doc.html
- Druid：http://localhost:8080/druid/

仓库里的数据库密码、Druid 账号和 JWT 密钥都是本机开发默认值。公开仓库不要改成真实环境的密码；上线前改掉 `application.yml` 里的这三项。

## 上传说明

本仓库忽略 `target/`、`.idea/`、`node_modules/` 和本地覆盖配置。不要把 `application-local.yml` 或 `.env` 提交进来。
