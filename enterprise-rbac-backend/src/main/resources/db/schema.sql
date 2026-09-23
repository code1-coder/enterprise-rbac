-- ============================================================
-- 企业权限角色分配系统 - 数据库表设计
-- RBAC (Role-Based Access Control) 模型
-- 数据库: rbac_system
-- 项目: enterprise-rbac
-- 脚本: src/main/resources/db/schema.sql
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rbac_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rbac_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表 (sys_user)
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 角色表 (sys_role)
-- ============================================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码（如：ROLE_ADMIN）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    sort INT DEFAULT 0 COMMENT '排序号',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_role_code (role_code),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 3. 菜单表 (sys_menu) - 包含菜单、按钮、接口权限
-- ============================================================
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID（0表示顶级菜单）',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type CHAR(1) NOT NULL COMMENT '菜单类型：M-目录 C-菜单 F-按钮 A-接口',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    permission VARCHAR(100) COMMENT '权限标识（如：system:user:list）',
    icon VARCHAR(100) COMMENT '菜单图标',
    sort INT DEFAULT 0 COMMENT '排序号',
    visible TINYINT DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_parent_id (parent_id),
    INDEX idx_menu_type (menu_type),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- ============================================================
-- 4. 用户-角色关联表 (sys_user_role)
-- ============================================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============================================================
-- 5. 角色-菜单关联表 (sys_role_menu)
-- ============================================================
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

-- ============================================================
-- 6. 操作日志表 (sys_operation_log) - 可选扩展
-- ============================================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    operation VARCHAR(50) COMMENT '操作类型',
    method VARCHAR(200) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    time BIGINT COMMENT '执行时长（毫秒）',
    ip VARCHAR(50) COMMENT '操作IP',
    status TINYINT COMMENT '操作状态：0-失败 1-成功',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 7. 登录日志表 (sys_login_log) - 可选扩展
-- ============================================================
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip VARCHAR(50) COMMENT '登录IP',
    location VARCHAR(200) COMMENT '登录地点',
    browser VARCHAR(100) COMMENT '浏览器类型',
    os VARCHAR(100) COMMENT '操作系统',
    status TINYINT COMMENT '登录状态：0-失败 1-成功',
    message VARCHAR(255) COMMENT '提示消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 插入默认超级管理员用户 (密码: admin123, BCrypt加密后)
INSERT INTO sys_user (id, username, password, nickname, email, status, remark) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'admin@example.com', 1, '系统默认管理员账号');

-- 插入默认角色
INSERT INTO sys_role (id, role_name, role_code, status, sort, remark) VALUES
(1, '超级管理员', 'ROLE_ADMIN', 1, 1, '拥有系统所有权限'),
(2, '普通用户', 'ROLE_USER', 1, 2, '预置角色，初始化未分配菜单'),
(3, '访客', 'ROLE_GUEST', 1, 3, '预置角色，初始化未分配菜单');

-- 插入默认菜单（示例）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, visible, status) VALUES
-- 系统管理（目录）
(1, 0, '系统管理', 'M', '/system', NULL, NULL, 'system', 1, 1, 1),
-- 用户管理（菜单）
(2, 1, '用户管理', 'C', '/system/user', 'system/user/index', 'system:user:list', 'user', 1, 1, 1),
(3, 2, '用户查询', 'F', NULL, NULL, 'system:user:query', NULL, 1, 1, 1),
(4, 2, '用户新增', 'F', NULL, NULL, 'system:user:add', NULL, 2, 1, 1),
(5, 2, '用户修改', 'F', NULL, NULL, 'system:user:edit', NULL, 3, 1, 1),
(6, 2, '用户删除', 'F', NULL, NULL, 'system:user:delete', NULL, 4, 1, 1),
-- 角色管理（菜单）
(7, 1, '角色管理', 'C', '/system/role', 'system/role/index', 'system:role:list', 'role', 2, 1, 1),
(8, 7, '角色查询', 'F', NULL, NULL, 'system:role:query', NULL, 1, 1, 1),
(9, 7, '角色新增', 'F', NULL, NULL, 'system:role:add', NULL, 2, 1, 1),
(10, 7, '角色修改', 'F', NULL, NULL, 'system:role:edit', NULL, 3, 1, 1),
(11, 7, '角色删除', 'F', NULL, NULL, 'system:role:delete', NULL, 4, 1, 1),
(12, 7, '分配权限', 'F', NULL, NULL, 'system:role:assign', NULL, 5, 1, 1),
-- 菜单管理（菜单）
(13, 1, '菜单管理', 'C', '/system/menu', 'system/menu/index', 'system:menu:list', 'menu', 3, 1, 1),
(14, 13, '菜单查询', 'F', NULL, NULL, 'system:menu:query', NULL, 1, 1, 1),
(15, 13, '菜单新增', 'F', NULL, NULL, 'system:menu:add', NULL, 2, 1, 1),
(16, 13, '菜单修改', 'F', NULL, NULL, 'system:menu:edit', NULL, 3, 1, 1),
(17, 13, '菜单删除', 'F', NULL, NULL, 'system:menu:delete', NULL, 4, 1, 1),
(18, 2, '重置密码', 'F', NULL, NULL, 'system:user:resetPwd', NULL, 5, 1, 1),
(19, 2, '分配角色', 'F', NULL, NULL, 'system:user:role', NULL, 6, 1, 1);

-- 给超级管理员分配角色。ROLE_USER、ROLE_GUEST 只预置角色，不预置菜单。
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 给超级管理员角色分配所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0;

-- ============================================================
-- 常用查询SQL示例
-- ============================================================

-- 查询用户的所有角色
-- SELECT r.* FROM sys_role r
-- INNER JOIN sys_user_role ur ON r.id = ur.role_id
-- WHERE ur.user_id = 1 AND r.deleted = 0 AND r.status = 1;

-- 查询用户的所有权限（通过角色）
-- SELECT DISTINCT m.* FROM sys_menu m
-- INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
-- INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
-- WHERE ur.user_id = 1 AND m.deleted = 0 AND m.status = 1;

-- 查询用户的所有权限标识（用于Spring Security权限判断）
-- SELECT DISTINCT m.permission FROM sys_menu m
-- INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
-- INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
-- WHERE ur.user_id = 1 AND m.deleted = 0 AND m.status = 1 AND m.permission IS NOT NULL;

-- 查询角色的所有菜单（树形结构）
-- SELECT m.* FROM sys_menu m
-- INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
-- WHERE rm.role_id = 1 AND m.deleted = 0 AND m.status = 1
-- ORDER BY m.parent_id, m.sort;

-- ============================================================
-- 说明：
-- 1. sys_user、sys_role、sys_menu 有 deleted。关联表和两张日志表没有 deleted，不能按 deleted 过滤
-- 2. 使用 InnoDB 引擎，支持事务和外键约束
-- 3. 关联表使用外键 ON DELETE CASCADE，删除主记录时自动删除关联
-- 4. 菜单表支持树形结构（parent_id），可以无限层级
-- 5. 菜单类型：M-目录 C-菜单 F-按钮 A-接口
-- 6. 默认管理员密码：admin123（BCrypt 已核对，实际使用时请修改）
-- 7. 脚本开头关闭外键检查，可重复执行；结尾恢复
-- 8. 逻辑删除是 UPDATE deleted=1，不会触发 ON DELETE CASCADE
-- 9. 删除类权限标识是 delete，不是 remove；另有 resetPwd 与 user:role
-- 10. ROLE_USER、ROLE_GUEST 初始化时没有 sys_role_menu 记录
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
