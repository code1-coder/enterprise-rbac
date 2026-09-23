> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。与 [api-design.md](api-design.md) 和 [schema.sql](../src/main/resources/db/schema.sql) 对齐。接口尚未实现。登录和注册目前是 404，其他 `/api/**` 未认证是 401。示例里的 `User@123456` 不是管理员密码，也不是数据库密码。除 `admin` 以外的用户 id 都是样例。

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

---

## 🎯 HTTP方法使用规则

```
GET     → 查询数据（不改变服务器状态）
POST    → 创建新资源
PUT     → 更新资源（完整替换）
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

3. **批量操作** 统一格式：
   ```json
   {
     "ids": [1, 2, 3]
   }
   ```

4. **时间格式** 统一使用：
   ```
   yyyy-MM-dd HH:mm:ss
   ```

5. **逻辑删除**：所有删除操作都是逻辑删除（设置 deleted=1），不会真正删除数据。

---

**文档版本**: v1.0  
**更新时间**: 2026-09-22  
**在线文档**: http://localhost:8080/doc.html (项目启动后访问)
