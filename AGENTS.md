# Agent 编码约束

本文件约束在本仓库写代码的 agent。先读代码和 `schema.sql`，再改实现。根 README 与后端 README、`docs/api-design.md` 有过时描述，冲突时以当前代码为准，并在同一次改动里修正被你碰过的文档。

## 范围

- 后端在 `enterprise-rbac-backend`，包名 `org.example.rbac`，Java 17，Spring Boot 3.2.5。
- 前端位于 `enterprise-rbac-frontend`。当前是设计/文档阶段；没有明确实现要求时，不初始化工程、不添加依赖或编写前端代码。
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
- 方法声明处和关键步骤必须添加必要的中文注释，说明方法语义、业务约束或关键设计决策；重点覆盖公开业务方法及权限校验、数据过滤、事务边界、缓存失效等关键步骤。注释只解释非显而易见的约束，不重复代码字面含义。

## 验证

- 行为变更要补或更新聚焦的单元测试，沿用 JUnit 5 与 Mockito。优先覆盖权限拒绝、越权、参数错误和成功路径。
- 不依赖外部服务的测试不能启动完整 Spring 容器。需要 MySQL 或 Redis 的检查要明确说明，不能把环境缺失伪装成通过。
- 修改 Java 代码后运行能定位变更的 Maven 测试；至少说明哪些测试已运行、哪些因环境未运行。
- 接口路径、权限串、字段或状态码变化时，同步更新对应文档。

## 前端开发与后端对接

- 前端技术选型和架构以 `enterprise-rbac-frontend/docs/architecture.md` 为约定；后端联调以 `enterprise-rbac-frontend/docs/api-integration.md`、后端当前 Controller/DTO/VO 和 `enterprise-rbac-backend/docs/api-reference.md` 为准。文档与当前实现冲突时先核对代码，再同步修正文档。
- 前端使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 和 Element Plus。只在明确要求实现前端时初始化依赖；选择兼容版本并提交锁文件，不为文档设计任务安装依赖。
- 页面组件负责展示和用户交互；请求集中在领域 API 模块，经统一 HTTP 客户端访问后端。不要在页面组件中散落 URL、鉴权头或响应错误解析。
- 统一发送 `Authorization: Bearer <token>`。前端路由、菜单和按钮权限只用于用户体验；后端 `@PreAuthorize` 与服务层资源校验才是安全边界。
- 接口权限判断只使用 `sys_menu.permission` 返回的权限标识，不把 `ROLE_ADMIN` 等角色编码当成 `hasAuthority` 权限。菜单组件路径需映射到前端本地白名单，不执行服务端返回的任意模块路径。
- 本地开发通过 Vite 将 `/api` 代理到后端 `http://localhost:8080`；后端当前没有配置跨域许可。部署时由同源反向代理转发，除非任务明确要求，不通过降低安全性或任意放开 CORS 来绕过跨域问题。
- 不将密码、JWT、密钥写入日志、源码或提交的环境文件；不得提交 `.env`、本地环境配置或构建产物。前端令牌生命周期、401/403 处理遵循架构与联调文档。
- 前端实现需按变更补充聚焦测试：至少覆盖路由鉴权/权限可见性、请求头与响应错误处理，以及重要表单成功和校验失败路径。测试应 mock API，不依赖已运行的 MySQL/Redis；文档设计任务不要求启动前端工程或运行不存在的测试。
