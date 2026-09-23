# Agent 编码约束

本文件约束在本仓库写代码的 agent。先读代码和 `schema.sql`，再改实现。根 README 与后端 README、`docs/api-design.md` 有过时描述，冲突时以当前代码为准，并在同一次改动里修正被你碰过的文档。

## 范围

- 后端在 `enterprise-rbac-backend`，包名 `org.example.rbac`，Java 17，Spring Boot 3.2.5。
- 前端目录只是预留。没有明确要求时，不初始化工程，不添加依赖。
- 只改完成当前任务所需的文件。不顺手重排格式、重命名无关符号或升级依赖。
- 不提交 `target/`、`.idea/`、`node_modules/`、`application-local.yml`、`.env`。

## 先核对事实

- 权限标识以 `src/main/resources/db/schema.sql` 为准。删除用 `system:*:delete`，不用 `remove`。
- `hasAuthority` 比对 `sys_menu.permission`。角色编码如 `ROLE_ADMIN` 只出现在登录用户信息和角色数据里。
- 三个密码不要混用：管理员 `admin123`，MySQL 开发默认 `123456`，接口样例 `User@123456`。
- 仓库里的数据库密码、Druid 账号和 JWT 密钥是本机开发默认值。不要替换成真实环境的秘密，也不要新增生产密钥。

## 后端分层

- Controller 只做入参校验、调用 Service 和返回 `Result`。不写 SQL，不直接操作 Mapper 或 Redis。
- 请求体用 `dto.<领域>`，响应用 `vo.<领域>`。禁止把 `entity` 当接口入参或出参。
- Service 负责业务规则、事务和对象转换。实现放在 `service.impl`。
- Mapper 只声明持久化操作。单表 CRUD 优先用 MyBatis-Plus；确需手写 SQL 时使用参数绑定，不拼接用户输入。
- 多表写入、角色分配、菜单分配使用事务。逻辑删除走 `@TableLogic`，不要物理删除带 `deleted` 的表，也不要假设逻辑删除会触发外键级联。
- 业务失败抛 `BusinessException`，由 `GlobalExceptionHandler` 统一转换。Controller 不吞异常，不返回另一套错误结构。

## 接口与安全

- 沿用 `/api` 加复数资源名，例如 `/api/users`。新增接口先对照 `docs/api-design.md` 和现有 Controller，不另造路径。
- 查询返回 `Result.query`，写操作返回 `Result.success`。响应字段保持 `code`、`message`、data`、`timestamp`。
- 除登录、注册和既有公开资源外，新接口必须经过认证。需要权限的方法添加对应 `@PreAuthorize("hasAuthority('...')")`。
- 已登录即可访问的接口必须在代码注释里说明理由。只允许用户操作自己的资源时，在 Service 核对当前用户 id。
- 密码只保存 BCrypt 哈希，使用已注册的 `PasswordEncoder`。禁止把密码、Token 或密钥写入日志和 VO。
- 登录态保持无 Session。JWT 与 Redis 黑名单、权限缓存沿用现有 `security` 包，不并行引入另一套认证机制。

## 实现要求

- 复用现有 `Result`、异常处理、MyBatis-Plus 和 Security 配置。版本由父 POM 或 Spring Boot 管理的依赖，不重复声明版本。
- MyBatis-Plus 使用 Boot 3 构件 `mybatis-plus-spring-boot3-starter`。不要引入 Boot 2 starter 或代码生成器。
- DTO 校验使用 Jakarta Validation，中文错误信息保持“字段 + 原因”。
- 配置项走 `application.yml` 和现有 `@ConfigurationProperties`。新增可调参数要有开发默认值，但不能弱化认证。
- 注释只解释非显而易见的业务约束，不重复代码字面含义。

## 验证

- 行为变更要补或更新聚焦的单元测试，沿用 JUnit 5 与 Mockito。优先覆盖权限拒绝、越权、参数错误和成功路径。
- 不依赖外部服务的测试不能启动完整 Spring 容器。需要 MySQL 或 Redis 的检查要明确说明，不能把环境缺失伪装成通过。
- 修改 Java 代码后运行能定位变更的 Maven 测试；至少说明哪些测试已运行、哪些因环境未运行。
- 接口路径、权限串、字段或状态码变化时，同步更新对应文档。
