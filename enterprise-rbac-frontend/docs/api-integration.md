# 后端接口联调文档

本文对应目标仓库当前后端代码。更完整的请求字段与业务规则见后端 [API 设计](../../enterprise-rbac-backend/docs/api-design.md) 和 [API 速查](../../enterprise-rbac-backend/docs/api-reference.md)；最终以 Controller、DTO、VO、Service、Security 配置及 [schema.sql](../../enterprise-rbac-backend/src/main/resources/db/schema.sql) 为准。菜单树、详情、创建、更新、删除以及当前用户菜单和权限接口均已实现。

## 联调前提

- 后端地址：`http://localhost:8080`；服务需要 MySQL 和 Redis。启动与初始化见后端 [README](../../enterprise-rbac-backend/README.md)。
- 后端未配置跨域许可。开发环境由 Vite 将 `/api` 代理到 `http://localhost:8080`；生产环境使用同源反向代理。业务请求统一相对 `/api` 发起。
- 后端请求和响应为 JSON。认证请求标准写法是 `Authorization: Bearer <token>`。不要把 Token 写入 URL、日志或错误提示。
- 开发管理员是 `admin / admin123`；MySQL 默认开发密码是 `123456`；文档其他接口的密码样例 `User@123456` 不是管理员密码。

## 统一响应

接口使用如下外层结构，分页信息也放在 `data` 内：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {},
  "timestamp": 1695628800000
}
```

分页 `data` 的结构为 `records`、`total`、`size`、`current`、`pages`。列表结果在 `records`，不要把 `data` 本身当数组处理。无返回体的写操作仍以统一响应返回，`data` 为 `null`。

后端将 HTTP 状态与响应 `code` 对齐。常见错误为 400 参数/业务输入错误、401 未认证或令牌失效、403 无权限、404 资源不存在、500 服务异常；错误响应仍包含 `code`、`message`、`data`、`timestamp`。HTTP 客户端需同时保留后端 `message`，不能只读取成功响应，也不要将 403 当作登录过期。

日期时间字段由后端 Jackson 格式化为 `yyyy-MM-dd HH:mm:ss`，时区为 `Asia/Shanghai`。数据库 ID 在当前 DTO/VO 中为数值型。菜单类型为 `M` 目录、`C` 菜单、`F` 按钮、`A` 接口；状态和可见性使用 `0/1`。

## 认证流程

### 登录

`POST /api/auth/login`，公开接口，请求体：

```json
{"username":"admin","password":"admin123"}
```

成功时读取 `data.token`、`data.tokenType`、`data.expiresIn` 和 `data.userInfo`。`expiresIn` 单位是秒。`userInfo.roles` 是如 `ROLE_ADMIN` 的角色编码，`userInfo.permissions` 才是 `sys_menu.permission` 权限标识。登录成功后再加载当前用户菜单与完整当前用户信息。注册接口 `POST /api/auth/register` 同样公开，但只创建账号、不签发 Token；注册成功后需要再登录。

### 已登录接口

- 所有除登录、注册及后端显式公开资源以外的 `/api/**` 均要求有效 JWT。
- `POST /api/auth/refresh-token` 要求旧 Token 仍有效；成功后旧 Token 加入黑名单，必须替换为响应中的新 `data.token`。
- `POST /api/auth/logout` 让当前 Token 进入黑名单；客户端无论服务端响应如何，都清理本地登录状态。
- 任一请求返回 401 时清理本地认证/路由/权限数据并跳转登录；403 只提示权限不足，不应无限重试或自动清除登录态。
- 某些接口仅要求已登录而没有独立权限标识，详见接口矩阵；用户改密接口还会在服务端校验路径用户 ID 必须是当前用户。

## 接口矩阵

权限标识与 `sys_menu.permission` 完全一致。`已登录` 表示不要求额外 `system:*` 权限，但不能匿名访问。

### 认证

| 功能 | 方法与路径 | 访问要求 | 前端用途 |
| --- | --- | --- | --- |
| 登录 | `POST /api/auth/login` | 公开 | 创建登录态 |
| 注册 | `POST /api/auth/register` | 公开 | 注册后需单独登录 |
| 退出 | `POST /api/auth/logout` | 已登录 | 服务端注销当前 Token |
| 刷新 Token | `POST /api/auth/refresh-token` | 已登录 | 换发 Token，并立即替换旧值 |
| 当前用户信息 | `GET /api/auth/user-info` | 已登录 | 加载用户资料、角色和权限 |

### 用户管理

| 功能 | 方法与路径 | 访问要求 | 关键请求/响应 |
| --- | --- | --- | --- |
| 用户分页 | `GET /api/users` | `system:user:list` | 查询参数 `page`、`size`、`username`、`status`；`data` 为分页对象 |
| 用户详情 | `GET /api/users/{id}` | `system:user:query` | 响应为 `UserVO`，含角色 ID/编码/名称，不含密码 |
| 创建用户 | `POST /api/users` | `system:user:add` | `username`、`password`、可选资料、`status`、`roleIds` |
| 更新用户资料 | `PUT /api/users/{id}` | `system:user:edit` | `status` 必填；`nickname`、`email`、`phone` 未传、`null` 或空白会清空 |
| 删除用户 | `DELETE /api/users/{id}` | `system:user:delete` | 逻辑删除 |
| 批量删除 | `DELETE /api/users/batch` | `system:user:delete` | JSON 请求体 `{ "ids": [1, 2] }` |
| 修改本人密码 | `PUT /api/users/{id}/password` | 已登录，限本人 | `{ "oldPassword": "…", "newPassword": "…" }` |
| 管理员重置密码 | `PUT /api/users/{id}/reset-password` | `system:user:resetPwd` | `{ "newPassword": "…" }` |
| 分配用户角色 | `PUT /api/users/{id}/roles` | `system:user:role` | `{ "roleIds": [2, 3] }`；空列表可表示清空分配 |

用户列表分页默认 `page=1`、`size=10`，每页最大 100；`username` 为模糊条件，`status` 为 0 或 1。密码仅用于请求，任何情况下都不能保存到页面持久状态或用户对象。

### 角色管理

| 功能 | 方法与路径 | 访问要求 | 关键请求/响应 |
| --- | --- | --- | --- |
| 角色分页 | `GET /api/roles` | `system:role:list` | 查询参数 `page`、`size`、`roleName`；分页对象在 `data` |
| 全部角色选项 | `GET /api/roles/list` | 已登录 | 非分页角色列表，按排序和 ID 升序 |
| 角色详情 | `GET /api/roles/{id}` | `system:role:query` | 响应 `RoleVO` |
| 创建角色 | `POST /api/roles` | `system:role:add` | `roleName`、`roleCode`、`status` 必填；`sort`、`remark` 可选 |
| 更新角色 | `PUT /api/roles/{id}` | `system:role:edit` | `roleName`、`roleCode`、`status` 必填；可选 `sort`、`remark` |
| 删除角色 | `DELETE /api/roles/{id}` | `system:role:delete` | 逻辑删除并清理关联 |
| 查询角色菜单 ID | `GET /api/roles/{id}/permissions` | `system:role:query` | 响应 `{ "menuIds": [1, 2] }` |
| 分配角色菜单 | `PUT /api/roles/{id}/permissions` | `system:role:assign` | 请求 `{ "menuIds": [1, 2] }`；空数组清空分配 |

角色名称和编码唯一，分页 `size` 最大 100。角色权限值是菜单 ID，不是单独的 permission ID；菜单树来自 `GET /api/menus/tree`。

### 菜单权限

> 实现状态：下列管理端与当前用户菜单/权限接口均已实现，可按各自权限要求联调。

| 功能 | 方法与路径 | 访问要求 | 关键请求/响应 |
| --- | --- | --- | --- |
| 管理端菜单树 | `GET /api/menus/tree` | `system:menu:list` | 返回含 M/C/F/A 节点的 `MenuTreeVO[]` |
| 当前用户导航 | `GET /api/menus/my-menus` | 已登录 | 仅已授权、启用的 M/C 树节点 |
| 当前用户权限标识 | `GET /api/menus/my-permissions` | 已登录 | 返回字符串数组，按 `sys_menu.permission` 校验按钮 |
| 菜单详情 | `GET /api/menus/{id}` | `system:menu:query` | 返回 `MenuVO` |
| 创建菜单/权限 | `POST /api/menus` | `system:menu:add` | 请求菜单字段；`parentId`、`menuName`、`menuType` 必填 |
| 更新菜单 | `PUT /api/menus/{id}` | `system:menu:edit` | `parentId`、`menuName`、`menuType` 必填；`sort`、`visible`、`status`、`remark` 缺省保留，其他可选文本缺省清空 |
| 删除菜单 | `DELETE /api/menus/{id}` | `system:menu:delete` | 存在子节点时拒绝；成功后逻辑删除并清理关系 |

菜单字段包括 `parentId`、`menuName`、`menuType`、`path`、`component`、`permission`、`icon`、`sort`、`visible`、`status`、`remark`。创建时 `sort` 默认 0、`visible` 与 `status` 默认 1；父节点必须有效，更新时不能设为自身或后代。更新时 `path`、`component`、`permission`、`icon` 未传或空白会清空，`remark` 为 `null` 时保留、空白时清空。删除有子节点的菜单会失败；菜单删除为逻辑删除，并清除关联角色菜单及受影响用户的权限缓存。菜单表权限字段就是后端接口鉴权使用的权限串；只在按钮或操作需要时按精确权限串控制展示。

## 对接流程建议

1. 确认后端、MySQL、Redis 已启动，并由前端开发服务器代理 `/api`。
2. 登录获取 Token；统一 HTTP 客户端对受保护接口附加 Bearer 头。
3. 加载当前用户、`my-menus` 和 `my-permissions`；根据本地组件白名单映射可访问菜单。
4. 页面数据从分页 `records` 渲染；表单提交字段按 DTO，不假设写操作返回新实体；成功后按需刷新列表/详情。
5. `400` 显示校验/业务消息，`401` 清理登录态，`403` 显示无权限，`404` 提示资源不存在，`500` 显示通用错误并保留安全诊断信息。
6. 联调和问题定位优先查 Knife4j `/doc.html`、后端 Controller/DTO/VO、上述 API 文档；不得因 UI 需要擅自更改路径或权限标识。
