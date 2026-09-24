> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。认证、用户、角色和菜单权限接口已实现。其他 `/api/**` 未认证返回 401。示例里的 `User@123456` 不是管理员密码，也不是数据库密码。除 `admin` 以外的用户 id 都是样例。

# API接口快速参考表

## 🔐 认证授权模块

| 功能 | 方法 | 路径 | 权限 |
|------|------|------|------|
| 用户登录 | POST | /api/auth/login | 公开 |
| 用户注册 | POST | /api/auth/register | 公开 |
| 退出登录 | POST | /api/auth/logout | 已登录 |
| 刷新Token | POST | /api/auth/refresh-token | 已登录 |
| 获取当前用户信息 | GET | /api/auth/user-info | 已登录 |

---

## 👥 用户管理模块

| 功能 | 方法 | 路径 | 权限标识 |
|------|------|------|----------|
| 获取用户列表（分页） | GET | /api/users | system:user:list |
| 获取用户详情 | GET | /api/users/{id} | system:user:query |
| 创建用户 | POST | /api/users | system:user:add |
| 更新用户信息 | PUT | /api/users/{id} | system:user:edit |
| 删除用户 | DELETE | /api/users/{id} | system:user:delete |
| 批量删除用户 | DELETE | /api/users/batch | system:user:delete |
| 修改自己密码 | PUT | /api/users/{id}/password | 已登录 |
| 重置用户密码（管理员） | PUT | /api/users/{id}/reset-password | system:user:resetPwd |
| 给用户分配角色 | PUT | /api/users/{id}/roles | system:user:role |

修改本人密码只允许已登录用户操作，Service 会校验路径中的用户 ID 与当前 JWT 身份一致。更新用户是完整更新：`status` 必填，`nickname`、`email`、`phone` 未传、传 `null` 或空白字符串都会清空对应字段。

---

## 🎭 角色管理模块

| 功能 | 方法 | 路径 | 权限标识 |
|------|------|------|----------|
| 获取角色列表（分页） | GET | /api/roles | system:role:list |
| 获取所有角色（下拉框） | GET | /api/roles/list | 已登录 |
| 获取角色详情 | GET | /api/roles/{id} | system:role:query |
| 创建角色 | POST | /api/roles | system:role:add |
| 更新角色信息 | PUT | /api/roles/{id} | system:role:edit |
| 删除角色 | DELETE | /api/roles/{id} | system:role:delete |
| 给角色分配权限 | PUT | /api/roles/{id}/permissions | system:role:assign |
| 获取角色的权限ID列表 | GET | /api/roles/{id}/permissions | system:role:query |

角色分页支持 `page`（默认 1）、`size`（默认 10，最大 100）和 `roleName` 模糊查询，结果按 `sort`、`id` 升序；`/api/roles/list` 返回同序的全部角色，仅要求已登录。创建时 `roleName`、`roleCode`、`status` 必填，`sort` 缺省为 0；角色名称和编码会去除首尾空白且必须唯一，逻辑删除角色占用过的名称或编码不能复用。更新时上述三个字段仍必填，未传 `sort` 或 `remark` 保留原值，`remark` 传空白字符串会清空。

权限分配请求体使用 `menuIds`（菜单 ID 列表），空数组表示清除已有分配；菜单 ID 必须存在且未逻辑删除。删除角色会逻辑删除角色并在同一事务清理用户角色、角色菜单关联；角色停用、删除或菜单分配变更后，受影响用户的 Redis 权限缓存会在事务提交后失效。

---

## 📋 菜单权限管理模块

| 功能 | 方法 | 路径 | 权限标识 |
|------|------|------|----------|
| 获取菜单树（管理用） | GET | /api/menus/tree | system:menu:list |
| 获取当前用户的菜单 | GET | /api/menus/my-menus | 已登录 |
| 获取当前用户的按钮权限 | GET | /api/menus/my-permissions | 已登录 |
| 获取菜单详情 | GET | /api/menus/{id} | system:menu:query |
| 创建菜单/按钮 | POST | /api/menus | system:menu:add |
| 更新菜单 | PUT | /api/menus/{id} | system:menu:edit |
| 删除菜单 | DELETE | /api/menus/{id} | system:menu:delete |

菜单管理树包含 M（目录）、C（菜单）、F（按钮）和 A（接口）全部类型；当前用户导航树仅包含已分配且启用的 M、C。创建默认 `sort=0`、`visible=1`、`status=1`；更新未传 `sort`、`visible`、`status`、`remark` 时保留原值。删除有子菜单的节点会失败；成功删除会逻辑删除菜单、清理角色菜单关联，并在事务提交后清除相关用户的权限缓存。

---

## 🎯 HTTP方法使用规则

```
GET     → 查询数据（不改变服务器状态）
POST    → 创建新资源
PUT     → 更新资源；未传字段的处理规则以对应接口 DTO 说明为准
DELETE  → 删除资源
```

---

## 📝 请求示例

### 登录
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 获取用户列表（带Token）
```bash
GET /api/users?page=1&size=10&username=张三
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 创建用户
```bash
POST /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "username": "zhangsan",
  "password": "User@123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "roleIds": [2]
}
```

### 更新用户
```bash
PUT /api/users/5
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "nickname": "张三三",
  "email": "zhangsan@example.com",
  "status": 1
}
```

### 删除用户
```bash
DELETE /api/users/5
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

---

## 🔒 权限标识对照表

| 权限标识 | 含义 | 对应菜单ID |
|----------|------|-----------|
| system:user:list | 查看用户列表 | 2 |
| system:user:query | 查看用户详情 | 3 |
| system:user:add | 新增用户 | 4 |
| system:user:edit | 编辑用户 | 5 |
| system:user:delete | 删除用户 | 6 |
| system:user:resetPwd | 重置密码 | 18 |
| system:user:role | 分配角色 | 19 |
| system:role:list | 查看角色列表 | 7 |
| system:role:query | 查看角色详情 | 8 |
| system:role:add | 新增角色 | 9 |
| system:role:edit | 编辑角色 | 10 |
| system:role:delete | 删除角色 | 11 |
| system:role:assign | 分配权限 | 12 |
| system:menu:list | 查看菜单列表 | 13 |
| system:menu:query | 查看菜单详情 | 14 |
| system:menu:add | 新增菜单 | 15 |
| system:menu:edit | 编辑菜单 | 16 |
| system:menu:delete | 删除菜单 | 17 |

---

## 📊 统一响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1695628800000
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null,
  "timestamp": 1695628800000
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 🚦 业务状态码

| Code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证/Token失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 📌 开发提示

1. **所有需要认证的接口** 都需要在请求头携带：
   ```
   Authorization: Bearer {token}
   ```

2. **分页查询** 统一使用参数：
   - `page`: 页码（从1开始）
   - `size`: 每页数量（默认10）

3. **批量/关联操作**：字段按接口区分，用户批量删除使用 `ids`，用户角色分配使用 `roleIds`，角色菜单分配使用 `menuIds`；空数组语义以对应接口说明为准。

4. **时间格式** 统一使用：
   ```
   yyyy-MM-dd HH:mm:ss
   ```

5. **删除方式**：用户、角色和菜单主记录使用逻辑删除；用户角色、角色菜单等关联表没有 `deleted` 字段，删除或调整分配时会清理关联记录。

---

**文档版本**: v1.0  
**更新时间**: 2026-09-23

**在线文档**: http://localhost:8080/doc.html (项目启动后访问)
