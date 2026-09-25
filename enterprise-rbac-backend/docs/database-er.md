> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。字段以 [schema.sql](../src/main/resources/db/schema.sql) 为准。

# 企业权限角色分配系统 - 数据库ER图说明

> 进度（2026-09-25）：后端接口与前端管理页已有实现，完整联调仍待验证；详见 [项目总览](../../README.md)。

## 📊 核心表关系图

```
┌─────────────────┐
│   sys_user      │ 用户表
│─────────────────│
│ id (PK)         │
│ username        │◄────────┐
│ password        │         │
│ nickname        │         │
│ email           │         │
│ phone           │         │
│ status          │         │
│ deleted         │         │
└─────────────────┘         │
         │                  │
         │ 1                │
         │                  │
         │ N                │
         ▼                  │
┌─────────────────┐         │
│ sys_user_role   │ 用户-角色关联表
│─────────────────│         │
│ id (PK)         │         │
│ user_id (FK)    │─────────┘
│ role_id (FK)    │─────────┐
└─────────────────┘         │
         │                  │
         │ N                │
         │                  │
         │ 1                │
         ▼                  │
┌─────────────────┐         │
│   sys_role      │ 角色表   │
│─────────────────│         │
│ id (PK)         │◄────────┘
│ role_name       │
│ role_code       │
│ status          │
│ sort            │
│ deleted         │
└─────────────────┘
         │
         │ 1
         │
         │ N
         ▼
┌─────────────────┐
│ sys_role_menu   │ 角色-菜单关联表
│─────────────────│
│ id (PK)         │
│ role_id (FK)    │
│ menu_id (FK)    │─────────┐
└─────────────────┘         │
         │                  │
         │ N                │
         │                  │
         │ 1                │
         ▼                  │
┌─────────────────┐         │
│   sys_menu      │ 菜单表   │
│─────────────────│         │
│ id (PK)         │◄────────┘
│ parent_id       │ 自关联，0 表示顶级
│ menu_name       │
│ menu_type       │
│ path            │
│ permission      │
│ icon            │
│ sort            │
│ status          │
│ deleted         │
└─────────────────┘
```

## 🔗 表关系说明

### 1. 用户 ↔ 角色（多对多）
**关联表**: `sys_user_role`
- 一个用户可以拥有多个角色
- 一个角色可以分配给多个用户
- 例如：张三既是"部门经理"又是"项目负责人"

### 2. 角色 ↔ 菜单（多对多）
**关联表**: `sys_role_menu`
- 一个角色可以拥有多个菜单权限
- 一个菜单可以分配给多个角色
- 例如："管理员"角色拥有所有菜单权限

### 3. 菜单自关联（树形结构）
**字段**: `parent_id`
- 菜单可以有父菜单和子菜单
- 支持无限层级嵌套
- parent_id = 0 表示顶级菜单

```
系统管理 (id=1, parent_id=0)        ← 目录(M)
├── 用户管理 (id=2, parent_id=1)     ← 菜单(C)
│   ├── 用户查询 (id=3, parent_id=2)  ← 按钮(F)
│   ├── 用户新增 (id=4, parent_id=2)  ← 按钮(F)
│   └── 用户删除 (id=6, parent_id=2)  ← 按钮(F)
├── 角色管理 (id=7, parent_id=1)     ← 菜单(C)
└── 菜单管理 (id=13, parent_id=1)    ← 菜单(C)
```

上图只画主干。初始化共 19 条菜单。删除类权限标识是 `delete`，另外还有 `system:user:resetPwd`（id=18）和 `system:user:role`（id=19）。

## 📋 核心表详解

### 1️⃣ sys_user (用户表)
**用途**: 存储系统用户基本信息

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | BIGINT | 主键 | 1 |
| username | VARCHAR(50) | 登录账号(唯一) | admin |
| password | VARCHAR(255) | BCrypt加密密码 | $2a$10$... |
| nickname | VARCHAR(50) | 显示名称 | 超级管理员 |
| email | VARCHAR(100) | 邮箱 | admin@example.com |
| phone | VARCHAR(20) | 手机号 | 13800138000 |
| avatar | VARCHAR(255) | 头像 URL | |
| status | TINYINT | 状态: 0-禁用 1-启用 | 1 |
| deleted | TINYINT | 逻辑删除: 0-未删除 1-已删除 | 0 |
| create_time | DATETIME | 创建时间 | |
| update_time | DATETIME | 更新时间 | |
| last_login_time | DATETIME | 最后登录时间 | |
| last_login_ip | VARCHAR(50) | 最后登录 IP | |
| remark | VARCHAR(500) | 备注 | |

**关键索引**:
- `username` (UNIQUE) - 快速登录查询
- `status` - 筛选启用用户
- `deleted` - 过滤已删除

---

### 2️⃣ sys_role (角色表)
**用途**: 定义系统角色

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | BIGINT | 主键 | 1 |
| role_name | VARCHAR(50) | 角色名称(唯一) | 超级管理员 |
| role_code | VARCHAR(50) | 角色编码(唯一) | ROLE_ADMIN |
| status | TINYINT | 状态: 0-禁用 1-启用 | 1 |
| sort | INT | 排序号 | 1 |
| deleted | TINYINT | 逻辑删除: 0-未删除 1-已删除 | 0 |
| create_time | DATETIME | 创建时间 | |
| update_time | DATETIME | 更新时间 | |
| remark | VARCHAR(500) | 备注 | |

**关键点**:
- `role_code` 保存的是完整权限字符串，例如 `ROLE_ADMIN`。`hasRole('ROLE_ADMIN')` 会再补一个 `ROLE_` 前缀，实际匹配 `ROLE_ROLE_ADMIN`。角色判断用 `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` 或 `hasRole('ADMIN')`。接口权限用 `hasAuthority('system:user:list')`，对应 `sys_menu.permission`
- `role_name` 用于前端展示

**预设角色**:
1. `ROLE_ADMIN` - 超级管理员（所有权限）
2. `ROLE_USER` - 普通用户（初始化未分配菜单）
3. `ROLE_GUEST` - 访客（初始化未分配菜单）

---

### 3️⃣ sys_menu (菜单权限表)
**用途**: 统一管理菜单、按钮、接口权限

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | BIGINT | 主键 | 1 |
| parent_id | BIGINT | 父菜单ID (0=顶级) | 0 |
| menu_name | VARCHAR(50) | 菜单名称 | 用户管理 |
| menu_type | CHAR(1) | 类型: M-目录 C-菜单 F-按钮 A-接口 | C |
| path | VARCHAR(200) | 路由路径 | /system/user |
| component | VARCHAR(255) | 前端组件路径 | system/user/index |
| permission | VARCHAR(100) | 权限标识 | system:user:list |
| icon | VARCHAR(100) | 图标 | user |
| sort | INT | 排序号 | 1 |
| visible | TINYINT | 是否可见: 0-隐藏 1-显示 | 1 |
| status | TINYINT | 状态: 0-禁用 1-启用 | 1 |
| deleted | TINYINT | 逻辑删除: 0-未删除 1-已删除 | 0 |
| create_time | DATETIME | 创建时间 | |
| update_time | DATETIME | 更新时间 | |
| remark | VARCHAR(500) | 备注 | |

**菜单类型说明**:
- **M (目录)**: 一级分类，如"系统管理"
- **C (菜单)**: 实际页面，如"用户管理"
- **F (按钮)**: 页面内操作按钮，如"新增"、"删除"
- **A (接口)**: 后端API权限（可选）

**权限标识格式**: `模块:功能:操作`
- `system:user:list` - 查看用户列表
- `system:user:add` - 新增用户
- `system:role:edit` - 编辑角色（菜单名称是“角色修改”）
- `system:user:delete` - 删除用户（不是 `remove`）
- `system:user:resetPwd` - 重置密码，菜单 id=18
- `system:user:role` - 给用户分配角色，菜单 id=19

---

### 4️⃣ sys_user_role (用户-角色关联表)
**用途**: 实现用户与角色的多对多关系

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID (外键) |
| role_id | BIGINT | 角色ID (外键) |
| create_time | DATETIME | 创建时间 |

**约束**:
- `UNIQUE(user_id, role_id)` - 防止重复分配
- `ON DELETE CASCADE` - 删除用户/角色时自动清理关联

---

### 5️⃣ sys_role_menu (角色-菜单关联表)
**用途**: 实现角色与菜单权限的多对多关系

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| role_id | BIGINT | 角色ID (外键) |
| menu_id | BIGINT | 菜单ID (外键) |
| create_time | DATETIME | 创建时间 |

**约束**:
- `UNIQUE(role_id, menu_id)` - 防止重复分配
- `ON DELETE CASCADE` - 删除角色/菜单时自动清理关联

---

## 扩展表

`sys_operation_log` 和 `sys_login_log` 也在初始化脚本中，不建外键，避免写日志受用户删除影响。

### sys_operation_log（操作日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 操作用户 ID，可空 |
| username | VARCHAR(50) | 操作用户名 |
| operation | VARCHAR(50) | 操作类型 |
| method | VARCHAR(200) | 请求方法 |
| params | TEXT | 请求参数 |
| time | BIGINT | 执行时长（毫秒） |
| ip | VARCHAR(50) | 操作 IP |
| status | TINYINT | 0-失败 1-成功 |
| error_msg | TEXT | 错误信息 |
| create_time | DATETIME | 创建时间 |

### sys_login_log（登录日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID，可空 |
| username | VARCHAR(50) | 用户名 |
| ip | VARCHAR(50) | 登录 IP |
| location | VARCHAR(200) | 登录地点 |
| browser | VARCHAR(100) | 浏览器 |
| os | VARCHAR(100) | 操作系统 |
| status | TINYINT | 0-失败 1-成功 |
| message | VARCHAR(255) | 提示消息 |
| login_time | DATETIME | 登录时间 |

---

## 🔐 RBAC权限验证流程

```
1. 用户登录
   ↓
2. 根据 user_id 查询 sys_user_role，获取所有 role_id
   ↓
3. 根据 role_id 查询 sys_role_menu，获取所有 menu_id
   ↓
4. 根据 menu_id 查询 sys_menu，获取所有 permission
   ↓
5. 将权限列表缓存到 Redis (key: user:permissions:{userId})
   ↓
6. 每次请求时，从 Redis 读取权限，判断是否有访问权限
```

### 核心查询SQL

**查询用户的所有权限标识**:
```sql
SELECT DISTINCT m.permission 
FROM sys_menu m
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
WHERE ur.user_id = ? 
  AND m.deleted = 0 
  AND m.status = 1 
  AND m.permission IS NOT NULL;
```

**查询用户的所有角色**:
```sql
SELECT r.* 
FROM sys_role r
INNER JOIN sys_user_role ur ON r.id = ur.role_id
WHERE ur.user_id = ? 
  AND r.deleted = 0 
  AND r.status = 1;
```

**查询角色的菜单树**:
```sql
SELECT m.* 
FROM sys_menu m
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
WHERE rm.role_id = ? 
  AND m.deleted = 0 
  AND m.status = 1
ORDER BY m.parent_id, m.sort;
```

---

## 🎯 数据流转示例

### 场景：用户"张三"访问"用户管理"页面

这是权限判断示例，不是初始化数据。库里没有 `zhangsan`，也没有 id=5 的角色。删除按钮对应 `system:user:delete`（菜单 id=6）。

1. **张三登录** (username: zhangsan)
   - 数据库查询: `SELECT * FROM sys_user WHERE username='zhangsan' AND deleted=0`

2. **加载角色**
   - `sys_user_role` 查询得知张三有角色: [部门经理(id=5)]

3. **加载权限**
   - `sys_role_menu` 查询得知"部门经理"角色拥有菜单: [1, 2, 3, 4, 5, 7...]
   - `sys_menu` 查询得知权限包括:
     - `system:user:list` (查看用户)
     - `system:user:add` (新增用户)
     - `system:user:edit` (编辑用户)
     - ❌ **没有** `system:user:delete` (删除用户)

4. **权限缓存到Redis**
   ```
   Key: user:permissions:123
   Value: ["system:user:list", "system:user:add", "system:user:edit", ...]
   TTL: 24小时
   ```

5. **前端展示**
   - 用户管理页面正常显示
   - "新增"按钮显示（有 `system:user:add` 权限）
   - "删除"按钮隐藏（无 `system:user:delete` 权限）

6. **后端验证**
   ```java
   @PreAuthorize("hasAuthority('system:user:add')")
   public Result addUser(@RequestBody User user) {
       // 新增用户逻辑
   }
   ```

---

## ⚙️ 数据库特性说明

### 1. 逻辑删除 (Soft Delete)
- **字段**: `deleted` (0-未删除, 1-已删除)
- **优点**: 
  - 数据可恢复
  - 保留历史记录
  - 外键关联不会断裂
- **使用**: 
  ```sql
  -- 删除用户（实际只是标记）
  UPDATE sys_user SET deleted = 1 WHERE id = 123;
  
  -- 查询时过滤已删除
  SELECT * FROM sys_user WHERE deleted = 0;
  ```

### 2. 外键级联删除 (ON DELETE CASCADE)
- **作用**: 删除主记录时，自动删除关联记录
- **示例**: 
  - 删除用户 → 自动删除 `sys_user_role` 中该用户的所有角色关联
  - 删除角色 → 自动删除 `sys_role_menu` 中该角色的所有权限关联

逻辑删除只执行 `UPDATE deleted=1`，不会触发 `ON DELETE CASCADE`。级联只在物理 `DELETE` 主表记录时发生。

### 3. 唯一约束 (UNIQUE)
- `sys_user.username` - 防止用户名重复
- `sys_role.role_name` - 防止角色名称重复
- `sys_role.role_code` - 防止角色编码重复
- `sys_user_role(user_id, role_id)` - 防止重复分配角色
- `sys_role_menu(role_id, menu_id)` - 防止重复分配权限

### 4. 索引优化
所有关联表都建立了索引，加速查询:
- `idx_user_id` - 快速查询某用户的角色
- `idx_role_id` - 快速查询某角色的权限
- `idx_parent_id` - 快速构建菜单树

---

## 🚀 初始化数据说明

执行 SQL 文件后，系统会自动创建:

### 默认用户
- 用户名: `admin`
- 密码: `admin123`
- 角色: 超级管理员

### 默认角色
1. **超级管理员** (ROLE_ADMIN) - 已分配全部 19 个菜单
2. **普通用户** (ROLE_USER) - 已创建，未分配菜单
3. **访客** (ROLE_GUEST) - 已创建，未分配菜单

### 默认菜单

共 19 条，其中目录“系统管理”没有权限标识：

- 用户管理：查询、新增、修改、删除、重置密码（id=18）、分配角色（id=19）
- 角色管理：查询、新增、修改、删除、分配权限
- 菜单管理：查询、新增、修改、删除

非空权限标识是 18 条。完整树见 [database-init.md](database-init.md)。

---

## 📝 执行步骤

1. **创建数据库**
   ```bash
   mysql -u root -p
   ```

2. **执行SQL文件**
   ```bash
   mysql -u root -p < src/main/resources/db/schema.sql
   ```
   或在MySQL客户端中:
   ```sql
   source src/main/resources/db/schema.sql
   ```

3. **验证数据**
   ```sql
   USE rbac_system;
   
   -- 查看所有表
   SHOW TABLES;
   
   -- 查看默认用户
   SELECT * FROM sys_user;
   
   -- 查看默认角色
   SELECT * FROM sys_role;
   
   -- 查看菜单树
   SELECT * FROM sys_menu ORDER BY parent_id, sort;
   ```

4. **修改 `src/main/resources/application.yml`**
   确保数据库连接配置正确

---

**设计完成时间**: 2026-09-22  
**下一步**: 根据表结构生成实体类和Mapper
