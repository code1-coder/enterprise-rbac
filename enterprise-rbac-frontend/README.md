# enterprise-rbac-frontend

企业权限角色分配系统 - 管理端前端

## 当前状态

✅ **前端骨架已完成**

- 项目结构完整，技术栈符合架构设计
- 类型定义、API 模块、状态管理已就绪
- 路由、认证守卫、主布局和登录页已实现
- 开发环境配置完成，可与后端联调

⏳ **待实现的业务页面**

- 用户管理（列表、创建、编辑、删除、角色分配）
- 角色管理（列表、创建、编辑、删除、权限分配）
- 菜单管理（树形展示、创建、编辑、删除）

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器运行在 `http://localhost:5173`，自动将 `/api` 请求代理到后端 `http://localhost:8080`。

**前提条件**：确保后端服务已启动，详见 [后端 README](../enterprise-rbac-backend/README.md)。

### 默认登录账号

- 用户名：`admin`
- 密码：`admin123`

## 技术栈

- Vue 3 + TypeScript + Vite
- Vue Router 4 + Pinia
- Axios + Element Plus
- Vitest + Vue Test Utils

详细说明见 [开发指南](DEVELOPMENT.md)。

## 设计文档

- [前端架构与技术选型](docs/architecture.md)：技术栈、分层、路由、权限与页面逻辑
- [后端接口联调文档](docs/api-integration.md)：认证流程、统一响应、错误处理和完整接口矩阵
- [开发指南](DEVELOPMENT.md)：项目结构、开发规范、常见问题

## 后端

- 后端地址：`http://localhost:8080`
- 接口文档：http://localhost:8080/doc.html
- 后端启动和数据库初始化：[后端 README](../enterprise-rbac-backend/README.md)
- API 设计文档：[api-design.md](../enterprise-rbac-backend/docs/api-design.md)、[api-reference.md](../enterprise-rbac-backend/docs/api-reference.md)

## 项目结构

```
src/
├── api/              # API 接口模块（auth、users、roles、menus）
├── assets/           # 静态资源
├── components/       # 通用组件（待实现）
├── layouts/          # 布局组件（MainLayout 已实现）
├── router/           # 路由配置和守卫
├── stores/           # Pinia 状态管理（auth、permission）
├── types/            # TypeScript 类型定义
├── utils/            # 工具函数（http、storage）
├── views/            # 页面组件（login 已实现，业务页面待实现）
├── App.vue           # 根组件
└── main.ts           # 应用入口
```

## 开发说明

### 认证流程

1. 登录成功后 Token 保存在 `sessionStorage`
2. 路由守卫检查登录态，未登录跳转登录页
3. 已登录但未加载权限时，自动加载用户信息和菜单权限
4. API 请求自动注入 `Authorization: Bearer <token>`
5. 401 响应自动清理登录态并跳转登录页

### 权限控制

- 路由守卫统一处理认证和权限初始化
- 按钮权限使用 `usePermissionStore().hasPermission(permission)` 判断
- 权限标识与后端 `sys_menu.permission` 字段一致

### API 调用

统一使用 `http` 工具，不直接使用 axios：

```typescript
import { http } from '@/utils/http'

// GET 请求
const data = await http.get('/users', { params: { page: 1 } })

// POST 请求
await http.post('/users', { username: 'test' })
```

### 状态管理

```typescript
import { useAuthStore, usePermissionStore } from '@/stores'

const authStore = useAuthStore()
const permissionStore = usePermissionStore()

// 检查权限
if (permissionStore.hasPermission('system:user:add')) {
  // 显示新增按钮
}
```

## 注意事项

1. **Token 管理**：使用 sessionStorage，关闭标签页即失效
2. **跨域处理**：开发环境使用 Vite 代理，生产环境使用反向代理
3. **权限校验**：前端权限只是 UI 体验，后端接口才是安全边界
4. **敏感信息**：不要提交 Token、密码或环境变量到代码仓库

## 相关链接

- [项目根 README](../README.md)
- [后端 README](../enterprise-rbac-backend/README.md)
- [开发指南](DEVELOPMENT.md)
