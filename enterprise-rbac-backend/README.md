# 企业权限角色分配系统

Enterprise RBAC。Maven 工程名是 `enterprise-rbac`。

这是一个 Spring Boot 3.2.5 的权限管理服务。JWT 认证、用户管理九项接口、角色 CRUD 和角色菜单权限分配，以及菜单权限管理接口均已实现。

截至 2026-09-25，Vue 3 管理端已有登录、用户、角色和菜单页面；前端联调、路由页面级权限拦截及类型检查仍待完善。项目整体进度与验证边界见 [根目录 README](../README.md) 和 [前端进度](../enterprise-rbac-frontend/PROJECT_SUMMARY.md)。后端接口实现范围以下文及当前 Controller 为准。

## 技术栈

版本以 [pom.xml](pom.xml) 为准。

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 17 | 可用更高版本的 JDK 编译，语言级别是 17 |
| Spring Boot | 3.2.5 | Web、Security、Validation、AOP、Redis |
| MyBatis-Plus | 3.5.7 | `mybatis-plus-spring-boot3-starter` |
| Druid | 1.2.23 | `druid-spring-boot-3-starter` |
| MySQL | 8.0+ | 驱动由 Spring Boot 管理 |
| jjwt | 0.12.6 | `jjwt-api`、`jjwt-impl`、`jjwt-jackson` |
| Knife4j | 4.5.0 | Jakarta / OpenAPI 3 |
| Hutool | 5.8.29 | 工具库 |
| Fastjson2 | 2.0.52 | JSON |
| Lombok | 由 Spring Boot 管理 | 用于实体、DTO 等模型 |

Redis 用本机实例即可，不锁定小版本。Spring Security、MySQL 驱动、Lombok 不要在 `pom.xml` 里另写版本。

## 目录

```text
├── pom.xml
├── README.md
├── docs
│   ├── api-design.md       认证、用户、角色和菜单权限接口设计
│   ├── api-reference.md    接口速查
│   ├── database-er.md      表字段和关系
│   ├── database-init.md    初始化步骤
│   └── dependencies.md     依赖说明
└── src
    ├── main
    │   ├── java/org/example/rbac
    │   │   ├── common, config
    │   │   ├── controller, dto, vo
    │   │   ├── entity, mapper
    │   │   ├── security
    │   │   └── service/impl
    │   └── resources
    │       ├── application.yml
    │       ├── static/index.html
    │       └── db/schema.sql
    └── test/java/org/example/rbac
        ├── security
        └── service/impl
```

上传前不要包含 `target/`、`.idea/`。这两个目录已在 `.gitignore` 中。

## 环境

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis，默认 `localhost:6379`，无密码，0 号库

## 启动

1. 在仓库根目录初始化数据库。PowerShell 不要直接使用 `<` 重定向：

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < src\main\resources\db\schema.sql"
```

脚本会创建 `rbac_system`，并建 7 张表。详细步骤和校验 SQL 见 [docs/database-init.md](docs/database-init.md)。

2. 确认 [src/main/resources/application.yml](src/main/resources/application.yml)。仓库里的开发默认值是：

- 数据库：`rbac_system`，用户 `root`，密码 `123456`
- Druid 监控：`/druid/`，用户 `admin`，密码 `admin`
- JWT 密钥：`rbacSystemSecretKey2026ChangeThisInProduction`，有效期 `86400000` 毫秒（24 小时）
- 服务端口：`8080`

这是本机开发配置，不是管理员登录密码。生产环境要改数据库密码、Druid 密码和 JWT 密钥。

3. 启动：

```bash
mvn spring-boot:run
```

启动类是 `org.example.rbac.RbacApplication`。

4. 启动后可以访问：

- 首页：http://localhost:8080/
- 接口文档：http://localhost:8080/doc.html
- Druid：http://localhost:8080/druid/

认证接口已由 `AuthController` 提供：登录和注册公开访问，退出、刷新 Token、获取当前用户信息需要有效 JWT。其他受保护的 `/api/**` 请求未携带有效认证信息时返回 401。接口行为和请求示例见 [docs/api-design.md](docs/api-design.md) 与 [docs/api-reference.md](docs/api-reference.md)。

`RbacApplicationTests` 会启动完整容器，需要 MySQL 和 Redis 已按 `application.yml` 运行。

## 初始化数据

只认 [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql)。

| 数据 | 内容 |
| --- | --- |
| 用户 | `admin` / `admin123`。密码是 BCrypt，哈希已用 Spring Security 核对 |
| 角色 | `ROLE_ADMIN`、`ROLE_USER`、`ROLE_GUEST`。后两个只有角色，没有菜单 |
| 菜单 | 19 条。目录 `系统管理` 的 `permission` 为 NULL |
| 权限标识 | 18 条，删除用 `system:user:delete` 这种 `delete`，不是 `remove` |
| 关联 | `admin` 绑定 `ROLE_ADMIN`，该角色拥有全部 19 条菜单 |

`ROLE_USER` 和 `ROLE_GUEST` 的备注如果还写着“普通权限”或“只读”，说明执行的是旧脚本。旧的 `数据库设计.sql` 已删除，里面的 BCrypt 不能还原成 `admin123`。

## 表

| 表 | 作用 | 逻辑删除 |
| --- | --- | --- |
| `sys_user` | 用户 | 有 `deleted` |
| `sys_role` | 角色 | 有 `deleted` |
| `sys_menu` | 目录、菜单、按钮、接口权限 | 有 `deleted` |
| `sys_user_role` | 用户-角色 | 无，外键级联 |
| `sys_role_menu` | 角色-菜单 | 无，外键级联 |
| `sys_operation_log` | 操作日志 | 无，也不建外键 |
| `sys_login_log` | 登录日志 | 无，也不建外键 |

逻辑删除是 `UPDATE deleted=1`，不会触发 `ON DELETE CASCADE`。字段和关系见 [docs/database-er.md](docs/database-er.md)。

## 文档

- [依赖说明](docs/dependencies.md)
- [数据库初始化](docs/database-init.md)
- [表结构说明](docs/database-er.md)
- [接口设计](docs/api-design.md)
- [接口速查](docs/api-reference.md)

接口文档里的 `User@123456` 只是请求样例。管理员密码是 `admin123`，MySQL 密码是 `123456`。

## 当前代码边界

已实现：认证与 JWT 安全链路、用户管理九项接口（含用户角色分配）、角色 CRUD 与角色菜单权限分配、菜单管理 CRUD、当前用户菜单和按钮权限查询。

菜单管理包含管理菜单树、详情、创建、更新和删除；删除有子节点的菜单会被拒绝，成功删除时逻辑删除菜单并清理角色菜单关联。
