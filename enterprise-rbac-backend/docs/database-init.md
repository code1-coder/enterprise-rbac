> 项目：[企业权限角色分配系统](../README.md)（`enterprise-rbac`）。建表脚本：[schema.sql](../src/main/resources/db/schema.sql)。下面的命令都在仓库根目录执行。

# 数据库初始化指南

> 进度（2026-09-25）：数据库脚本与前后端已有实现，真实环境联调待验证；详见 [项目总览](../../README.md)。

## 📋 前置准备

### 1. 确保MySQL已安装并运行
```bash
# 检查MySQL服务状态
# Windows: 打开服务管理器，查看 MySQL 服务是否运行

# 或使用命令行测试连接
mysql -u root -p
```

### 2. 准备工作
- MySQL版本: 8.0+
- 字符集: utf8mb4
- 排序规则: utf8mb4_unicode_ci

---

## 🚀 方法一：使用MySQL客户端执行（推荐）

### Step 1: 连接MySQL
```bash
mysql -u root -p
# 输入密码
```

### Step 2: 执行SQL文件
```sql
-- 在MySQL命令行中执行
source src/main/resources/db/schema.sql

```

### Step 3: 验证数据
```sql
-- 切换到数据库
USE rbac_system;

-- 查看所有表
SHOW TABLES;

-- 应该看到以下7张表：
-- sys_login_log
-- sys_menu
-- sys_operation_log
-- sys_role
-- sys_role_menu
-- sys_user
-- sys_user_role

-- 查看用户表数据
SELECT id, username, nickname FROM sys_user;

-- 查看角色表数据
SELECT id, role_name, role_code FROM sys_role;

-- 查看菜单表数据（查看前5条）
SELECT id, parent_id, menu_name, menu_type FROM sys_menu LIMIT 5;
```

---

## 🚀 方法二：使用Navicat/DataGrip等图形化工具

### 使用Navicat
1. 连接到MySQL服务器
2. 右键选择"运行SQL文件"
3. 选择 `src/main/resources/db/schema.sql`
4. 点击"开始"执行

### 使用DataGrip
1. 打开 `src/main/resources/db/schema.sql` 文件
2. 点击工具栏的"执行"按钮（或按 Ctrl+Enter）
3. 等待执行完成

---

## 🚀 方法三：使用命令行直接执行

在仓库根目录执行。`source` 的相对路径取决于 mysql 客户端的当前目录。

```bash
mysql -u root -p --default-character-set=utf8mb4 -e "source src/main/resources/db/schema.sql"
```

Windows PowerShell 不能直接使用 `<` 重定向，改用 cmd：

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < src\main\resources\db\schema.sql"
```

---

## 📊 初始化后的数据

### 默认用户账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | 超级管理员（ROLE_ADMIN） | 已分配全部 19 个菜单 |

⚠️ **生产环境必须修改默认密码！**

### 默认角色

| 角色ID | 角色名称 | 角色编码 | 说明 |
|--------|----------|----------|------|
| 1 | 超级管理员 | ROLE_ADMIN | 所有权限 |
| 2 | 普通用户 | ROLE_USER | 只创建角色，未分配菜单 |
| 3 | 访客 | ROLE_GUEST | 只创建角色，未分配菜单 |

### 默认菜单结构
```
系统管理 (id=1, M, /system)
├── 用户管理 (id=2, C, system:user:list)
│   ├── 用户查询 (id=3, F, system:user:query)
│   ├── 用户新增 (id=4, F, system:user:add)
│   ├── 用户修改 (id=5, F, system:user:edit)
│   ├── 用户删除 (id=6, F, system:user:delete)
│   ├── 重置密码 (id=18, F, system:user:resetPwd)
│   └── 分配角色 (id=19, F, system:user:role)
├── 角色管理 (id=7, C, system:role:list)
│   ├── 角色查询 (id=8, F, system:role:query)
│   ├── 角色新增 (id=9, F, system:role:add)
│   ├── 角色修改 (id=10, F, system:role:edit)
│   ├── 角色删除 (id=11, F, system:role:delete)
│   └── 分配权限 (id=12, F, system:role:assign)
└── 菜单管理 (id=13, C, system:menu:list)
    ├── 菜单查询 (id=14, F, system:menu:query)
    ├── 菜单新增 (id=15, F, system:menu:add)
    ├── 菜单修改 (id=16, F, system:menu:edit)
    └── 菜单删除 (id=17, F, system:menu:delete)
```

---

## ✅ 验证步骤

### 1. 验证表结构
```sql
USE rbac_system;

-- 查看用户表结构
DESC sys_user;

-- 查看角色表结构
DESC sys_role;

-- 查看菜单表结构
DESC sys_menu;
```

### 2. 验证数据完整性
```sql
-- 统计各表记录数
SELECT 'sys_user' AS table_name, COUNT(*) AS count FROM sys_user
UNION ALL
SELECT 'sys_role', COUNT(*) FROM sys_role
UNION ALL
SELECT 'sys_menu', COUNT(*) FROM sys_menu
UNION ALL
SELECT 'sys_user_role', COUNT(*) FROM sys_user_role
UNION ALL
SELECT 'sys_role_menu', COUNT(*) FROM sys_role_menu;

-- 预期结果：
-- sys_user: 1 条（admin用户）
-- sys_role: 3 条（3个角色）
-- sys_menu: 19 条（菜单和按钮）
-- sys_user_role: 1 条（admin的角色关联）
-- sys_role_menu: 19 条（超级管理员的全部菜单）
```

### 3. 验证关联关系
```sql
-- 查询 admin 用户的角色
SELECT u.username, r.role_name, r.role_code
FROM sys_user u
INNER JOIN sys_user_role ur ON u.id = ur.user_id
INNER JOIN sys_role r ON ur.role_id = r.id
WHERE u.username = 'admin';

-- 预期结果：admin | 超级管理员 | ROLE_ADMIN

-- 查询超级管理员的权限数量
SELECT COUNT(*) AS permission_count
FROM sys_role_menu
WHERE role_id = 1;

-- 预期结果：19（全部菜单和按钮）
```

### 4. 测试权限查询SQL
```sql
-- 查询 admin 用户的所有权限标识
SELECT DISTINCT m.permission
FROM sys_menu m
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
WHERE ur.user_id = 1 
  AND m.deleted = 0 
  AND m.status = 1 
  AND m.permission IS NOT NULL;

-- 应返回 18 条非空权限标识，顺序不保证：
-- system:user:list
-- system:user:query
-- system:user:add
-- system:user:edit
-- system:user:delete
-- system:user:resetPwd
-- system:user:role
-- system:role:list
-- system:role:query
-- system:role:add
-- system:role:edit
-- system:role:delete
-- system:role:assign
-- system:menu:list
-- system:menu:query
-- system:menu:add
-- system:menu:edit
-- system:menu:delete
-- 目录 id=1 的 permission 为 NULL，所以菜单 19 条、权限标识 18 条
```

---

## 🔧 配置Spring Boot连接

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rbac_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的MySQL密码
```

仓库里的 [application.yml](../src/main/resources/application.yml) 目前写的是 `123456`。这是数据库密码，不是 `admin` 的登录密码。

---

## 🐛 常见问题

### 问题1: 连接失败
```
错误: Access denied for user 'root'@'localhost'
```
**解决方法**:
- 检查MySQL服务是否启动
- 确认用户名密码是否正确
- 尝试重置MySQL密码

### 问题2: 找不到数据库
```
错误: Unknown database 'rbac_system'
```
**解决方法**:
```sql
-- 手动创建数据库
CREATE DATABASE rbac_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 问题3: 字符集问题
```
错误: Incorrect string value
```
**解决方法**:
```sql
-- 检查数据库字符集
SHOW CREATE DATABASE rbac_system;

-- 修改字符集
ALTER DATABASE rbac_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 问题4: 外键约束失败
```
错误: Cannot add foreign key constraint
```
**解决方法**:
- 完整的 schema.sql 开头有 `SET FOREIGN_KEY_CHECKS = 0`，整文件重跑即可
- 不要只执行后半段。关联表在主表之前被单独执行时才会触发这个错误

```sql
-- 删除数据库重新创建
DROP DATABASE IF EXISTS rbac_system;
CREATE DATABASE rbac_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rbac_system;
source src/main/resources/db/schema.sql;
```

---

## 📝 登录验证

初始化数据包含可用于验证登录的管理员账号。启动服务后，可按以下请求登录：

```bash
# 启动类是 org.example.rbac.RbacApplication。
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

# 登录成功后响应包含 JWT Token。登录和注册无需认证；退出、刷新 Token、当前用户信息及其他受保护接口需要有效 Token。
# 完整认证接口契约见 api-design.md 和 api-reference.md。
```

---

## 🔐 安全提示

1. **修改默认密码**
   ```sql
   -- 生产环境必须修改！
   UPDATE sys_user SET password = '$2a$10$新的BCrypt密码' WHERE username = 'admin';
   ```

2. **定期备份数据库**
   ```bash
   # 备份命令
   mysqldump -u root -p rbac_system > backup_20260922.sql
   ```

---

## ✨ 下一步

数据库初始化完成后：

1. 把 `application.yml` 里的数据库密码改成你本机的密码
2. 启动 `org.example.rbac.RbacApplication`
3. 打开 http://localhost:8080/ 和 http://localhost:8080/doc.html 。文档页可以打开，里面还没有业务接口
4. 不要把 `/api/auth/login` 当成已经可用
5. 下一步再写实体、Mapper 和 JWT

---

**初始化完成时间**: 2026-09-22  
**下一步**: 生成实体类和Mapper接口
