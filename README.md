# 企业权限角色分配系统

个人作品集项目。一个面向中后台的 RBAC 权限服务：用户、角色、菜单和按钮权限，JWT 登录，Redis 会话控制，以及基于权限标识的接口鉴权。

截至 2026-09-25，Spring Boot 后端与 Vue 3 管理端均已有可运行代码。前端已实现登录、用户/角色/菜单管理页面和权限交互；完整联调、类型检查与测试覆盖仍需完善，不能视为生产就绪。

## 仓库结构

| 目录 | 说明 |
| --- | --- |
| [enterprise-rbac-backend](enterprise-rbac-backend) | Spring Boot 3.2.5 后端，含数据库脚本和接口文档 |
| [enterprise-rbac-frontend](enterprise-rbac-frontend) | Vue 3 管理端，含登录与用户、角色、菜单页面 |

后端怎么启动、默认账号和表结构，见 [enterprise-rbac-backend/README.md](enterprise-rbac-backend/README.md)。

## 技术栈

- Java 17，Spring Boot 3.2.5，Spring Security
- MyBatis-Plus 3.5.7，MySQL 8，Druid
- JWT（jjwt 0.12.6）+ Redis
- Knife4j / OpenAPI 3
- Lombok、Hutool、Fastjson2
- Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus

版本以 [pom.xml](enterprise-rbac-backend/pom.xml) 为准。

## 已实现

- 登录、注册、退出、刷新 Token、当前用户
- 用户管理接口：分页查询、详情、创建、更新、删除、批量删除、本人改密、管理员重置密码、分配角色
- 角色管理接口：分页查询、下拉列表、详情、创建、更新、删除、分配菜单权限
- JWT 过滤器、Redis 登录态和退出黑名单
- `@PreAuthorize` 权限标识，例如 `system:user:delete`
- 初始化脚本：管理员、三种角色、菜单和权限

菜单权限接口已实现，包括管理菜单树、详情、创建、更新、删除，以及当前用户菜单和按钮权限查询。

管理端已实现左右分栏登录页、用户列表与角色分配、角色权限树、菜单树管理；按钮依据当前用户的 `sys_menu.permission` 显示。角色权限树重新打开时仅回填已授权叶节点，避免半选父节点误选整支；保存授权后及管理页重新获得焦点时会刷新当前账号权限。后端仍独立执行接口鉴权；多个启用角色的权限取并集。

**待完成/验证**：前端路由目前仅校验登录并初始化权限，尚未按页面权限阻止手动访问路由；缺少路由守卫、HTTP 客户端及关键表单的系统化测试。前端 `npm run type-check` 仍有类型错误，`npm run build` 因类型检查而未确认通过；已单独验证 Vite 资源构建。真实账号跨会话撤权和完整 MySQL/Redis 联调尚未在本次文档更新中验证。详见 [前端进度](enterprise-rbac-frontend/PROJECT_SUMMARY.md)。

本地演示账号是 `admin` / `admin123`。这是脚本里的种子数据，不是生产密码。

## 本地启动

需要 JDK 17+、Maven 3.6+、MySQL 8 和本机 Redis。

```powershell
cd enterprise-rbac-backend
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < src\main\resources\db\schema.sql"
mvn spring-boot:run
```

前端另开终端：

```powershell
cd enterprise-rbac-frontend
npm install
npm run dev
```

管理端：`http://localhost:5173`，开发服务器将 `/api` 代理至后端 `http://localhost:8080`。

- 首页：http://localhost:8080/
- 接口文档：http://localhost:8080/doc.html
- Druid：http://localhost:8080/druid/

仓库里的数据库密码、Druid 账号和 JWT 密钥都是本机开发默认值。公开仓库不要改成真实环境的密码；上线前改掉 `application.yml` 里的这三项。

## 上传说明

本仓库忽略 `target/`、`.idea/`、`node_modules/` 和本地覆盖配置。不要把 `application-local.yml` 或 `.env` 提交进来。
