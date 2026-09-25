# 前端架构与技术选型

## 当前阶段

本文件最初是设计约定；截至 2026-09-25，前端工程、依赖清单及登录、用户、角色、菜单页面均已落地。以下有些条目仍是目标设计，未实现的部分在下文明确标出；当前进度见 [前端 README](../README.md)。

后端与前端均有可运行代码。接口和权限设计以当前后端 Controller、DTO、VO、`schema.sql` 及 [API 联调文档](api-integration.md) 为准；文档冲突时先核对代码。

## 技术选型

| 层次 | 选择 | 用途与理由 |
| --- | --- | --- |
| UI 框架 | Vue 3 | 适合中后台单页应用，组件化承载查询表格、表单、树和弹窗 |
| 语言 | TypeScript | 为请求/响应、菜单树和权限状态建立类型边界，减少接口字段误用 |
| 构建与开发服务器 | Vite | 提供开发服务器，并通过 `/api` 代理连接现有 Spring Boot 服务 |
| 路由 | Vue Router | 登录页、受保护的管理页面和按权限装配的导航路由 |
| 全局状态 | Pinia | 集中维护登录用户、JWT、权限标识和当前菜单状态 |
| HTTP 客户端 | Axios | 通过单一实例注入 Authorization，集中处理 `Result`、401/403 和网络异常 |
| UI 组件库 | Element Plus | 复用适合管理端的表格、表单、树选择、对话框和反馈组件 |

前端已有 `package.json` 与 `package-lock.json`，版本以这两个文件为准，不再按原规划重新初始化项目。

官方文档： [Vue](https://vuejs.org/guide/introduction.html)、[TypeScript with Vue](https://vuejs.org/guide/typescript/overview.html)、[Vite](https://vite.dev/guide/)、[Vue Router](https://router.vuejs.org/)、[Pinia](https://pinia.vuejs.org/)、[Element Plus](https://element-plus.org/)、[Axios](https://axios-http.com/docs/intro)。

## 应用分层

当前工程按职责组织（以下为层次概览，不要求机械照搬目录名）：

```text
src/
  api/          按 auth、users、roles、menus 划分的请求函数
  components/   预留可复用组件目录；目前主要复用 Element Plus
  layouts/      登录后主框架、侧边导航和内容区
  router/       静态路由、登录守卫和权限初始化
  stores/       auth、permission Pinia 状态
  types/        Result、PageResult、DTO 和 VO 对应的前端类型
  utils/        统一 HTTP 客户端及纯工具
  views/        login、user、role、menu 等业务页面
```

- `views` 负责页面状态与用户交互，调用领域 API 和 composable，不直接拼接接口 URL。
- `api` 负责路径、HTTP 方法、参数和后端 DTO/VO 类型，不在其中实现页面行为。
- 统一 HTTP 客户端负责 `/api` 基础路径、Bearer 请求头、业务错误呈现及认证失效回调。
- `stores` 维护登录态和权限快照；页面刷新后从当前有效 JWT 恢复会话，并重新加载用户、菜单和权限数据。
- 通用组件保持领域无关；用户、角色、菜单专属规则留在相应页面或领域模块。

## 页面与路由逻辑

| 页面/路由 | 主要职责 | 后端数据来源 |
| --- | --- | --- |
| `/login` | 登录；显示校验和认证错误 | `POST /api/auth/login` |
| `/system/user` | 用户分页、详情、创建、编辑、删除、角色分配和密码操作 | `/api/users`、`/api/roles/list` |
| `/system/role` | 角色分页、详情、创建、编辑、删除、菜单权限分配 | `/api/roles`、`/api/menus/tree`、`/api/roles/{id}/permissions` |
| `/system/menu` | 菜单权限树、详情、创建、更新、删除 | `/api/menus/tree`、`/api/menus/{id}`、`POST/PUT/DELETE /api/menus` |

> 菜单管理与当前用户导航/权限接口均已实现。管理端接口分别要求 `system:menu:list/query/add/edit/delete`；当前用户的 `my-menus` 和 `my-permissions` 仅要求已登录。

主框架使用 `/api/menus/my-menus` 的当前用户导航。后端只返回已授权、启用的 M（目录）和 C（菜单）节点；F（按钮）和 A（接口权限）不作为导航项。当前路由表只注册 `/system/user`、`/system/role`、`/system/menu` 三个本地组件，不执行服务端返回的 `component` 路径；未知路径的显式 404/无权页尚未实现。

按钮显示逻辑根据 `/api/menus/my-permissions` 返回的 `sys_menu.permission` 集合判断，例如 `system:user:add`。角色编码 `ROLE_ADMIN` 是角色资料，不是 `hasAuthority` 权限。前端隐藏按钮和路由仅用于界面体验，后端仍须独立校验每个受保护接口。

当前路由守卫负责未登录跳 `/login`、从 `sessionStorage` 恢复登录态和首次加载用户/菜单/权限。**尚未实现**按页面权限拒绝手动访问管理路由或展示无权限页；后端 API 仍是最终授权来源。按钮显隐取 `permission` Store，保存角色权限后与管理页重新获得焦点时会刷新当前账号权限；多个启用角色的权限取并集。

## 登录态与权限初始化

1. 登录表单提交用户名和密码至公开的 `/api/auth/login`。
2. 成功后保存 `data.token` 和 `data.tokenType`，使用 `data.userInfo` 初始化当前用户摘要；密码立即丢弃，不写入状态持久化、日志或浏览器存储。
3. 使用标准请求头 `Authorization: Bearer <token>`；登录后加载 `my-menus` 和 `my-permissions`，页面刷新后路由守卫还会请求 `/api/auth/user-info`。导航来自服务端菜单，页面组件来自静态路由表。
4. 页面刷新时若本地仍有令牌，重新调用上述已登录接口验证；任何鉴权 401 都清除本地认证状态和权限快照，并跳转登录页。
5. 退出时调用 `/api/auth/logout`，无论请求成功与否都清除本地 token、用户、路由与权限状态。刷新 Token 时，后端会使旧 Token 进入黑名单，前端必须立即用响应中的新 Token 替换旧值。

建议令牌仅在当前浏览器标签页使用 `sessionStorage` 持久化，以支持同标签页刷新恢复；该方式仍可被同源脚本读取，不防御 XSS。不要将 Token 写入 `localStorage` 作为默认方案，也不要记录 Token。页面内容应默认转义，不渲染不可信 HTML，并保持依赖及时更新。

## 联调与环境边界

- 后端本地地址为 `http://localhost:8080`，依赖 MySQL 与 Redis；默认管理员账号 `admin / admin123`。MySQL 开发密码 `123456`、接口样例密码 `User@123456` 均非管理员密码。
- 后端当前没有配置 CORS 许可。开发服务器应将 `/api` 同源代理到 `http://localhost:8080`；生产环境由同源反向代理转发，不在前端绕过浏览器安全策略。
- HTTP 客户端当前使用相对 `/api`；Vite 开发代理目标为 `http://localhost:8080`。部署时由同源反向代理转发，不在代码里放生产域名、JWT 密钥、数据库密码或 Druid 凭据。
- 分页、查询、完整更新、逻辑删除和权限串按 [API 联调文档](api-integration.md) 处理。服务端校验失败时将后端 `message` 转成可读提示；不要假设写请求都返回资源详情。

## 验证策略

目前已加入用户角色显示、角色权限树回填、权限 Store 刷新的纯逻辑测试；统一客户端请求头和错误解析、路由守卫、关键表单、用户/角色/菜单页面成功与失败状态仍需补测。现有 `vitest.config.ts` 使用 `jsdom`，但依赖清单未声明该包；纯逻辑测试可用 Node 环境运行。`npm run type-check` 尚有错误；单独的 Vite 构建成功不等于完整 `npm run build` 通过。真实联调需另行确认后端、MySQL、Redis 和数据均已启动。
