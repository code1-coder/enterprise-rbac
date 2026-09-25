# 前端开发指南

## 快速开始

### 环境要求

- Node.js >= 22.14.0
- npm >= 10.9.2

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，会自动将 `/api` 请求代理到后端 `http://localhost:8080`。

**注意**：启动前端前请确保后端服务已启动，详见 [后端 README](../enterprise-rbac-backend/README.md)。

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

### 运行测试

```bash
npm run test
```

### 代码检查

```bash
npm run lint
```

### 类型检查

```bash
npm run type-check
```

## 项目结构

```
src/
├── api/              # API 接口模块
│   ├── auth.ts       # 认证接口
│   ├── users.ts      # 用户管理接口
│   ├── roles.ts      # 角色管理接口
│   └── menus.ts      # 菜单权限接口
├── assets/           # 静态资源
├── components/       # 通用组件
├── layouts/          # 布局组件
│   └── MainLayout.vue
├── router/           # 路由配置
│   └── index.ts
├── stores/           # Pinia 状态管理
│   ├── auth.ts       # 认证状态
│   ├── permission.ts # 权限状态
│   └── index.ts
├── types/            # TypeScript 类型定义
│   ├── common.ts     # 通用类型
│   ├── auth.ts       # 认证类型
│   ├── user.ts       # 用户类型
│   ├── role.ts       # 角色类型
│   └── menu.ts       # 菜单类型
├── utils/            # 工具函数
│   ├── http.ts       # HTTP 客户端
│   └── storage.ts    # 本地存储工具
├── views/            # 页面组件
│   ├── login/        # 登录页
│   └── system/       # 系统管理页面
│       ├── user/     # 用户管理
│       ├── role/     # 角色管理
│       └── menu/     # 菜单管理
├── App.vue           # 根组件
└── main.ts           # 应用入口
```

## 技术栈

- **UI 框架**: Vue 3 (Composition API + `<script setup>`)
- **语言**: TypeScript
- **构建工具**: Vite
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **HTTP 客户端**: Axios
- **UI 组件库**: Element Plus
- **测试**: Vitest + Vue Test Utils

## 开发规范

### 代码组织

- 使用 Composition API 和 `<script setup>` 语法
- 按功能模块组织代码，保持单一职责
- 页面组件负责业务逻辑和用户交互
- API 模块负责接口调用，不包含页面逻辑
- Store 负责全局状态管理

### 类型定义

- 所有 API 请求和响应都应有明确的 TypeScript 类型
- 类型定义按领域划分，放在 `types/` 目录
- 避免使用 `any`，除非确实无法推断类型

### API 调用

- 统一使用 `http` 工具进行 API 调用
- 不要在组件中直接使用 axios 或拼接 URL
- 错误处理在组件层进行，显示友好的错误提示

### 权限控制

- 路由权限由路由守卫统一处理
- 按钮权限使用 `usePermissionStore().hasPermission()` 判断
- 权限标识与后端 `sys_menu.permission` 字段保持一致

### 样式规范

- 使用 scoped 样式避免污染全局
- 优先使用 Element Plus 提供的组件和样式
- 自定义样式保持简洁，避免过度设计

## 认证流程

1. 用户在登录页输入用户名和密码
2. 调用 `/api/auth/login` 接口
3. 成功后保存 Token 到 sessionStorage
4. 加载用户信息、菜单和权限
5. 跳转到主页面

每次刷新页面时：
1. 检查本地是否有 Token
2. 如果有，重新加载用户信息和权限
3. 如果 Token 失效（401），清理登录态并跳转登录页

## 联调说明

### 后端地址

开发环境：`http://localhost:8080`

Vite 开发服务器已配置代理：
- 前端请求 `/api/*` 会自动转发到后端 `http://localhost:8080/api/*`

### 默认账号

- 用户名：`admin`
- 密码：`admin123`

### API 文档

- Knife4j 文档：http://localhost:8080/doc.html
- 后端 API 设计文档：[api-design.md](../enterprise-rbac-backend/docs/api-design.md)
- 前端联调文档：[api-integration.md](./docs/api-integration.md)

## 当前状态

### 已实现（截至 2026-09-25）

- ✅ 项目骨架搭建
- ✅ TypeScript 类型定义
- ✅ 统一 HTTP 客户端封装
- ✅ 认证状态管理（Pinia）
- ✅ 权限状态管理（Pinia）
- ✅ 路由配置和守卫
- ✅ 主布局组件
- ✅ 登录页面
- ✅ 用户管理页面：查询、增删改、批量删除、角色回显/分配、重置密码
- ✅ 角色管理页面：查询、增删改、权限树分配与半选回显修正
- ✅ 菜单管理页面：树形展示和增删改
- ✅ 登录左右分栏与轻量动效；管理页权限刷新
- ✅ API 模块（auth、users、roles、menus）
- ✅ 开发服务器代理配置
- ✅ 角色权限树、当前权限刷新与用户角色显示的纯逻辑测试

### 待完成/验证

- ⏳ 路由级页面权限校验（当前仅认证与权限初始化）
- ⏳ HTTP 客户端、路由守卫和重要表单的测试，以及真实账号端到端联调
- ⏳ 修复 `npm run type-check` 的已有类型错误；当前 `npm run build` 会被类型检查阻断
- ⏳ `vitest.config.ts` 选择 `jsdom`，依赖清单尚未声明 `jsdom`；纯逻辑测试可用 `vitest run --config vite.config.ts --environment node`

## 注意事项

1. **不提交敏感信息**：不要将 Token、密码或密钥提交到代码仓库
2. **环境隔离**：开发环境配置不要影响生产环境
3. **跨域问题**：开发环境使用代理，生产环境使用反向代理，不要放开 CORS
4. **权限校验**：前端权限控制只是 UI 体验，后端接口才是安全边界
5. **Token 管理**：使用 sessionStorage，关闭标签页即失效；不要使用 localStorage 作为默认方案

## 常见问题

### Q: 登录后刷新页面跳回登录页？

A: 检查 sessionStorage 中是否有 Token，确保路由守卫的恢复逻辑正常。

### Q: API 请求 404？

A: 确保后端服务已启动在 8080 端口，检查 Vite 代理配置是否正确。

### Q: 权限按钮不显示？

A: 检查权限标识是否与后端 `sys_menu.permission` 一致，确认用户至少有一个启用角色授予该权限。撤权后重新登录，或让已登录管理页重新获得焦点以刷新权限；后端鉴权始终生效。

### Q: Element Plus 组件样式丢失？

A: 确保已正确导入 `element-plus/dist/index.css`。
