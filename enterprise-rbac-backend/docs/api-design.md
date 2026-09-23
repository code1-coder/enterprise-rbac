> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。本文是接口设计，Controller 还没写。权限标识以 [schema.sql](../src/main/resources/db/schema.sql) 为准，删除用 `delete`，不用 `remove`。运行时 `SecurityConfig` 放行登录、注册、首页、Knife4j 和 Druid；其他 `/api/**` 未认证返回 401。示例用户密码写 `User@123456`，不要和数据库密码 `123456`、管理员密码 `admin123` 搞混。

# 企业权限角色分配系统 - API接口设计文档

## 📋 RESTful API 设计规范

### HTTP 方法约定

| 方法 | 用途 | 幂等性 | 示例 |
|------|------|--------|------|
| **GET** | 查询数据（单个/列表） | 是 | GET /api/users |
| **POST** | 创建新资源 | 否 | POST /api/users |
| **PUT** | 完整更新资源 | 是 | PUT /api/users/1 |
| **DELETE** | 删除资源 | 是 | DELETE /api/users/1 |

### URL 命名规范
```
基础路径: /api
资源命名: 使用复数名词，小写，用连字符分隔

正确:
  /api/users
  /api/roles
  /api/menus

错误:
  /api/getUsers
  /api/UserList
  /api/user_roles
```

### 统一响应格式

#### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1695628800000
}
```

#### 分页响应
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "timestamp": 1695628800000
}
```

#### 错误响应
```json
{
  "code": 400,
  "message": "参数错误：用户名不能为空",
  "data": null,
  "timestamp": 1695628800000
}
```

### HTTP 状态码规范

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 查询、更新、删除成功 |
| 201 | 已创建 | 创建资源成功 |
| 400 | 请求错误 | 参数校验失败 |
| 401 | 未认证 | Token无效或过期 |
| 403 | 无权限 | 用户无此操作权限 |
| 404 | 未找到 | 资源不存在 |
| 500 | 服务器错误 | 系统异常 |

---

## 认证授权接口

### 1.1 用户登录
```
POST /api/auth/login
```

**请求参数**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "roles": ["ROLE_ADMIN"],
      "permissions": ["system:user:list", "system:user:add"]
    }
  }
}
```

`expiresIn` 的单位是秒。`application.yml` 里的 `jwt.expiration: 86400000` 是毫秒，换成秒就是 `86400`。`roles` 返回 `sys_role.role_code`，例如 `ROLE_ADMIN`，不是中文角色名。

### 1.2 用户注册
```
POST /api/auth/register
```

**请求参数**:
```json
{
  "username": "newuser",
  "password": "User@123456",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

### 1.3 退出登录
```
POST /api/auth/logout
```

**请求头**: `Authorization: Bearer {token}`

### 1.4 刷新Token
```
POST /api/auth/refresh-token
```

**请求头**: `Authorization: Bearer {token}`

### 1.5 获取当前用户信息
```
GET /api/auth/user-info
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "email": "admin@example.com",
    "roles": ["ROLE_ADMIN"],
    "roleNames": ["超级管理员"],
    "permissions": ["system:user:list", "system:user:add"]
  }
}
```

---

## 用户管理接口

### 2.1 获取用户列表（分页）
```
GET /api/users?page=1&size=10&username=admin&status=1
```

**查询参数**:
- `page`: 页码（默认1）
- `size`: 每页数量（默认10）
- `username`: 用户名（模糊查询）
- `status`: 状态（0-禁用 1-启用）

**权限要求**: `system:user:list`

### 2.2 获取单个用户详情
```
GET /api/users/{id}
```

**权限要求**: `system:user:query`

### 2.3 创建用户
```
POST /api/users
```

**请求参数**:
```json
{
  "username": "zhangsan",
  "password": "User@123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138001",
  "status": 1,
  "roleIds": [2, 3]
}
```

**权限要求**: `system:user:add`

### 2.4 更新用户
```
PUT /api/users/{id}
```

**请求参数**:
```json
{
  "nickname": "张三三",
  "email": "zhangsan2@example.com",
  "phone": "13800138002",
  "status": 1
}
```

**权限要求**: `system:user:edit`

### 2.5 删除用户（逻辑删除）
```
DELETE /api/users/{id}
```

**权限要求**: `system:user:delete`

### 2.6 批量删除用户
```
DELETE /api/users/batch
```

**请求参数**:
```json
{
  "ids": [1, 2, 3]
}
```

**权限要求**: `system:user:delete`

### 2.7 修改自己的密码
```
PUT /api/users/{id}/password
```

**请求参数**:
```json
{
  "oldPassword": "admin123",
  "newPassword": "User@123456"
}
```

**权限要求**: 已登录即可，没有单独的菜单权限。路径里的 `id` 必须是当前用户自己。管理员重置别人的密码走下面的 `reset-password`，权限是 `system:user:resetPwd`。

### 2.8 重置用户密码（管理员）
```
PUT /api/users/{id}/reset-password
```

**权限要求**: `system:user:resetPwd`

### 2.9 给用户分配角色
```
PUT /api/users/{id}/roles
```

**请求参数**:
```json
{
  "roleIds": [2, 3]
}
```

**权限要求**: `system:user:role`

---

## 角色管理接口

### 3.1 获取角色列表（分页）
```
GET /api/roles?page=1&size=10&roleName=管理员
```

**权限要求**: `system:role:list`

### 3.2 获取所有角色（不分页，用于下拉框）
```
GET /api/roles/list
```

**权限要求**: 已登录。这是下拉框接口，不要求 `system:role:list`。

### 3.3 获取单个角色详情
```
GET /api/roles/{id}
```

**权限要求**: `system:role:query`

### 3.4 创建角色
```
POST /api/roles
```

**请求参数**:
```json
{
  "roleName": "部门经理",
  "roleCode": "ROLE_MANAGER",
  "status": 1,
  "sort": 10,
  "remark": "部门经理角色"
}
```

**权限要求**: `system:role:add`

### 3.5 更新角色
```
PUT /api/roles/{id}
```

**权限要求**: `system:role:edit`

### 3.6 删除角色
```
DELETE /api/roles/{id}
```

**权限要求**: `system:role:delete`

### 3.7 给角色分配权限（菜单）
```
PUT /api/roles/{id}/permissions
```

**请求参数**:
```json
{
  "menuIds": [1, 2, 3, 4, 5]
}
```

**权限要求**: `system:role:assign`

这里的权限就是菜单 id，写入 `sys_role_menu`。库里没有 `sys_permission` 或 `sys_role_permission`。

### 3.8 获取角色已分配的权限ID列表
```
GET /api/roles/{id}/permissions
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "menuIds": [1, 2, 3, 4, 5]
  }
}
```

---

## 菜单权限管理接口

### 4.1 获取菜单树（树形结构，管理页面用）
```
GET /api/menus/tree
```

**权限要求**: `system:menu:list`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "menuName": "系统管理",
      "menuType": "M",
      "path": "/system",
      "icon": "system",
      "sort": 1,
      "children": [
        {
          "id": 2,
          "menuName": "用户管理",
          "menuType": "C",
          "path": "/system/user",
          "children": []
        }
      ]
    }
  ]
}
```

### 4.2 获取当前用户的动态菜单（前端渲染导航用）
```
GET /api/menus/my-menus
```

只返回 `menuType` 为 M（目录）和 C（菜单）且当前用户有权限的项，不包含按钮(F)。

### 4.3 获取当前用户的按钮权限标识列表
```
GET /api/menus/my-permissions
```

**响应**:
```json
{
  "code": 200,
  "data": ["system:user:add", "system:user:edit", "system:user:delete"]
}
```

### 4.4 获取单个菜单详情
```
GET /api/menus/{id}
```

**权限要求**: `system:menu:query`

### 4.5 创建菜单/按钮
```
POST /api/menus
```

**请求参数**:
```json
{
  "parentId": 1,
  "menuName": "岗位管理",
  "menuType": "C",
  "path": "/system/post",
  "component": "system/post/index",
  "permission": "system:post:list",
  "icon": "post",
  "sort": 4,
  "visible": 1,
  "status": 1
}
```

`岗位管理` 和 `system:post:list` 只是新建菜单的请求样例，`schema.sql` 的初始化数据里没有这条权限。

**权限要求**: `system:menu:add`

### 4.6 更新菜单
```
PUT /api/menus/{id}
```

**权限要求**: `system:menu:edit`

### 4.7 删除菜单
```
DELETE /api/menus/{id}
```

删除前需校验：若存在子菜单则拒绝删除。

**权限要求**: `system:menu:delete`

---

## 接口权限矩阵总览

| 接口 | 方法 | 路径 | 所需权限标识 |
|------|------|------|--------------|
| 用户登录 | POST | /api/auth/login | 无需认证 |
| 用户注册 | POST | /api/auth/register | 无需认证 |
| 退出登录 | POST | /api/auth/logout | 已登录 |
| 刷新 Token | POST | /api/auth/refresh-token | 已登录 |
| 获取当前用户信息 | GET | /api/auth/user-info | 已登录 |
| 获取用户列表 | GET | /api/users | system:user:list |
| 获取用户详情 | GET | /api/users/{id} | system:user:query |
| 创建用户 | POST | /api/users | system:user:add |
| 更新用户 | PUT | /api/users/{id} | system:user:edit |
| 删除用户 | DELETE | /api/users/{id} | system:user:delete |
| 批量删除用户 | DELETE | /api/users/batch | system:user:delete |
| 修改自己的密码 | PUT | /api/users/{id}/password | 已登录 |
| 重置密码 | PUT | /api/users/{id}/reset-password | system:user:resetPwd |
| 分配角色 | PUT | /api/users/{id}/roles | system:user:role |
| 获取角色列表 | GET | /api/roles | system:role:list |
| 获取所有角色 | GET | /api/roles/list | 已登录 |
| 获取角色详情 | GET | /api/roles/{id} | system:role:query |
| 创建角色 | POST | /api/roles | system:role:add |
| 更新角色 | PUT | /api/roles/{id} | system:role:edit |
| 删除角色 | DELETE | /api/roles/{id} | system:role:delete |
| 分配权限 | PUT | /api/roles/{id}/permissions | system:role:assign |
| 获取角色已分配菜单 | GET | /api/roles/{id}/permissions | system:role:query |
| 获取菜单树 | GET | /api/menus/tree | system:menu:list |
| 获取当前用户菜单 | GET | /api/menus/my-menus | 已登录 |
| 获取当前用户按钮权限 | GET | /api/menus/my-permissions | 已登录 |
| 获取菜单详情 | GET | /api/menus/{id} | system:menu:query |
| 创建菜单 | POST | /api/menus | system:menu:add |
| 更新菜单 | PUT | /api/menus/{id} | system:menu:edit |
| 删除菜单 | DELETE | /api/menus/{id} | system:menu:delete |

---

## Controller层设计示例

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> list(UserQueryDTO query) {
        return Result.success(userService.pageList(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Void> create(@RequestBody @Valid UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> batchDelete(@RequestBody BatchIdsDTO dto) {
        userService.batchDeleteUsers(dto.getIds());
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:role')")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return Result.success();
    }
}
```

---

## 命名与设计小结

1. **GET** 用于所有查询：列表查询走 `?page=&size=` 分页参数，详情查询走路径参数 `/{id}`。
2. **POST** 仅用于创建新资源，body携带完整创建信息。
3. **PUT** 用于更新，包括整体更新和子资源关系更新（如分配角色/权限），路径中带资源ID。
4. **DELETE** 用于删除，单个删除走路径参数，批量删除走 `/batch` 子路径 + body传ID数组（因为DELETE请求体在部分网关/代理下支持不佳，用POST语义的batch路径更稳妥，但方法仍保留DELETE以保持语义一致）。
5. 所有需要鉴权的接口通过 `@PreAuthorize("hasAuthority('权限标识')")` 与菜单表中的 `permission` 字段对应。
6. 统一响应体、统一异常处理、统一分页格式，前端处理逻辑保持一致。
